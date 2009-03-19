/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), available at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * TIANI Medgraph AG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunter.zeilinger@tiani.com>
 * Franz Willer <franz.willer@gwi-ag.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.dcm4chex.archive.mbean;

import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.FinderException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.FutureRSP;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.StudyReconciliation;
import org.dcm4chex.archive.ejb.interfaces.StudyReconciliationHome;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7902 $ $Date: 2008-11-03 14:57:39 +0100 (Mon, 03 Nov 2008) $
 * @since Juli 2006
 * 
 */
public class StudyReconciliationService extends AbstractScuService {

    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);

    private long taskInterval = 0L;

    private int disabledStartHour;

    private int disabledEndHour;

    private int limitNumberOfStudiesPerTask;

    private long minStudyAge;

    private int scheduledStatus = 1;

    private int successStatus = 0;

    private int failureStatus = -1;

    private String calledAET = "TIANI_ARCHIVE";

    private Integer listenerID;

    private final static AssociationFactory af = AssociationFactory
            .getInstance();

    private final static DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private final static int PCID_FIND = 1;

    private int priority = Command.MEDIUM;

    private String timerIDCheckStudyReconciliation = "CheckStudyReconciliation";

    private static final int[] UPDATE_ATTRIBUTES = new int[] {
            Tags.OtherPatientIDs, Tags.OtherPatientNames,
            Tags.StudyDescription, Tags.StudyID, Tags.AccessionNumber,
            Tags.StudyDate, Tags.StudyTime, Tags.ReferringPhysicianName,
            Tags.SeriesDescription, Tags.SeriesNumber, Tags.Modality,
            Tags.BodyPartExamined, Tags.SeriesDate, Tags.SeriesTime,
            Tags.Laterality, Tags.PerformingPhysicianName };

    private static final Logger log = Logger
            .getLogger(StudyReconciliationService.class);

    private final NotificationListener updateCheckListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (isDisabled(hour)) {
                if (log.isDebugEnabled())
                    log.debug("Study Reconciliation ignored in time between "
                            + disabledStartHour + " and " + disabledEndHour
                            + " !");
            } else {
                try {
                    log.info(check());
                } catch (Exception e) {
                    log.error("Study Reconciliation failed!", e);
                }
            }
        }
    };

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }

    public String getCalledAET() {
        return calledAET;
    }

    public void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }

    public final String getTaskInterval() {
        String s = RetryIntervalls.formatIntervalZeroAsNever(taskInterval);
        return (disabledEndHour == -1) ? s : s + "!" + disabledStartHour + "-"
                + disabledEndHour;
    }

    public void setTaskInterval(String interval) throws Exception {
        long oldInterval = taskInterval;
        int pos = interval.indexOf('!');
        if (pos == -1) {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval);
            disabledEndHour = -1;
        } else {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval
                    .substring(0, pos));
            int pos1 = interval.indexOf('-', pos);
            disabledStartHour = Integer.parseInt(interval.substring(pos + 1,
                    pos1));
            disabledEndHour = Integer.parseInt(interval.substring(pos1 + 1));
        }
        if (getState() == STARTED && oldInterval != taskInterval) {
            scheduler.stopScheduler(timerIDCheckStudyReconciliation,
                    listenerID, updateCheckListener);
            listenerID = scheduler.startScheduler(
                    timerIDCheckStudyReconciliation, taskInterval,
                    updateCheckListener);
        }
    }

    /**
     * Getter for minStudyAge.
     * <p>
     * This value is used to ensure that consistent check is not performed on
     * studies that are not completed (store is not completed).
     * 
     * @return Returns ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMinStudyAge() {
        return RetryIntervalls.formatInterval(minStudyAge);
    }

    /**
     * Setter for minStudyAge.
     * <p>
     * This value is used to ensure that consistent check is not performed on
     * studies that are not completed (store is not completed).
     * 
     * @param age
     *            ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public void setMinStudyAge(String age) {
        this.minStudyAge = RetryIntervalls.parseInterval(age);
    }

    public int getLimitNumberOfStudiesPerTask() {
        return limitNumberOfStudiesPerTask;
    }

    public void setLimitNumberOfStudiesPerTask(int limit) {
        this.limitNumberOfStudiesPerTask = limit;
    }

    public int getScheduledStatus() {
        return scheduledStatus;
    }

    public void setScheduledStatus(int status) {
        this.scheduledStatus = status;
    }

    public int getSuccessStatus() {
        return successStatus;
    }

    public void setSuccessStatus(int status) {
        this.successStatus = status;
    }

    public int getFailureStatus() {
        return failureStatus;
    }

    public void setFailureStatus(int failureStatus) {
        this.failureStatus = failureStatus;
    }

    public String check() throws Exception {
        long start = System.currentTimeMillis();
        StudyReconciliation studyReconciliation = newStudyReconciliation();
        Timestamp createdBefore = new Timestamp(System.currentTimeMillis()
                - this.minStudyAge);
        Collection col = studyReconciliation.getStudyIuidsWithStatus(
                scheduledStatus, createdBefore, limitNumberOfStudiesPerTask);
        log.info("Check " + col.size()
                + " Studies for Reconcilitiation (status:"
                + this.scheduledStatus + ")");
        if (col.isEmpty()) {
            log.debug("No Studies with status " + scheduledStatus
                    + " found for Reconcilitiation!");
            return "Nothing to do!";
        }
        Map archiveStudy;
        Dataset qrSeriesDS = getSeriesQueryDS();
        Dataset qrInstanceDS = getInstanceQueryDS();
        ActiveAssociation aa = openAssociation(calledAET, 
                UIDs.StudyRootQueryRetrieveInformationModelFIND);        
        int numPatUpdt = 0, numPatMerge = 0, numFailures = 0;
        try {
            String studyIuid;
            for (Iterator iter = col.iterator(); iter.hasNext();) {
                studyIuid = (String) iter.next();
                qrSeriesDS.putUI(Tags.StudyInstanceUID, studyIuid);
                archiveStudy = query(aa, qrSeriesDS, Tags.SeriesInstanceUID);
                Dataset patDS = checkStudy(qrSeriesDS, qrInstanceDS,
                        archiveStudy, aa);
                if (patDS != null) {
                    log.debug("check Patient info of study " + studyIuid);
                    Dataset origPatDS = (Dataset) archiveStudy.values().iterator().next();
                    if (compare(patDS, origPatDS, Tags.PatientID)
                            && compare(patDS, origPatDS,
                                    Tags.IssuerOfPatientID)) {
                        if (!compare(patDS, origPatDS, Tags.PatientName)
                                || !compare(patDS, origPatDS,
                                        Tags.PatientBirthDate)
                                        || !compare(patDS, origPatDS,
                                                Tags.PatientSex)) {
                            log.info("UPDATE patient: "
                                    + origPatDS.getString(Tags.PatientID));
                            studyReconciliation.updatePatient(origPatDS);
                            numPatUpdt++;
                        }
                    } else {
                        log.info("MERGE patient: "
                                + patDS.getString(Tags.PatientID)
                                + " with "
                                + origPatDS.getString(Tags.PatientID));
                        studyReconciliation.mergePatient(origPatDS, patDS);
                        numPatMerge++;
                    }
                    studyReconciliation.updateStudyAndSeries(studyIuid,
                            successStatus, archiveStudy);
                } else {
                    numFailures++;
                    studyReconciliation.updateStatus(studyIuid,
                            failureStatus);
                }
            }
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn(
                        "Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append(col.size()).append(
                " studies checked for Reconciliation!(updt:")
                .append(numPatUpdt);
        sb.append(";merged:").append(numPatMerge).append(";failed:").append(
                numFailures);
        sb.append(") in ").append(System.currentTimeMillis() - start).append(
                " ms");
        return sb.toString();
    }

    /**
     * @param ds1
     * @param ds2
     * @param tag
     * @return
     */
    private boolean compare(Dataset ds1, Dataset ds2, int tag) {
        String s1 = ds1.getString(tag);
        String s2 = ds2.getString(tag);
        if (log.isDebugEnabled())
            log.debug("compare '" + s1 + "'=?'" + s2 + "'");
        return s1 == null ? s2 == null : s1.equals(s2);
    }

    /**
     * @return
     */
    private Dataset getSeriesQueryDS() {
        Dataset qrDS = dof.newDataset();
        qrDS.putCS(Tags.QueryRetrieveLevel, "SERIES");
        qrDS.putUI(Tags.SeriesInstanceUID);
        qrDS.putLO(Tags.PatientID);
        qrDS.putPN(Tags.IssuerOfPatientID);
        qrDS.putPN(Tags.PatientName);
        qrDS.putPN(Tags.PatientBirthDate);
        qrDS.putPN(Tags.PatientSex);
        for (int i = 0; i < UPDATE_ATTRIBUTES.length; i++) {
            if (!qrDS.contains(UPDATE_ATTRIBUTES[i]))
                qrDS.putXX(UPDATE_ATTRIBUTES[i]);
        }
        return qrDS;
    }

    private Dataset getInstanceQueryDS() {
        Dataset qrDS = dof.newDataset();
        qrDS.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        qrDS.putUI(Tags.SOPInstanceUID);
        return qrDS;
    }

    /**
     * @param string
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private Map query(ActiveAssociation aassoc, Dataset keys, int tag)
            throws InterruptedException, IOException {
        if (aassoc == null) {
            throw new IllegalStateException("No Association established");
        }
        Command rqCmd = dof.newCommand();
        rqCmd.initCFindRQ(aassoc.getAssociation().nextMsgID(),
                UIDs.StudyRootQueryRetrieveInformationModelFIND, priority);
        Dimse findRq = af.newDimse(PCID_FIND, rqCmd, keys);
        FutureRSP future = aassoc.invoke(findRq);
        Dimse findRsp = future.get();
        List findRspList = future.listPending();
        String iuid;
        Dataset ds;
        Map map = new HashMap(findRspList.size());
        for (int i = 0, len = findRspList.size(); i < len; i++) {
            ds = ((Dimse) findRspList.get(i)).getDataset();
            iuid = ds.getString(tag);
            removeEmptyUpdateAttributes(ds);
            if (map.put(iuid, ds) != null) {
                throw new IllegalArgumentException(
                        "C-FIND contains two items with same uid ("
                                + Tags.toString(tag) + ") ! :" + iuid);
            }
        }
        return map;
    }

    /**
     * @param ds
     */
    private void removeEmptyUpdateAttributes(Dataset ds) {
        String value;
        for (int i = 0; i < UPDATE_ATTRIBUTES.length; i++) {
            value = ds.getString(UPDATE_ATTRIBUTES[i]);
            if (value == null || value.length() < 1) {
                log.info("remove empty update attribute:"
                        + UPDATE_ATTRIBUTES[i]);
                ds.remove(UPDATE_ATTRIBUTES[i]);
            }
        }

    }

    /**
     * @param qrSeriesDS
     * @param archiveStudy
     * @param aa
     * @throws SQLException
     * @throws IOException
     * @throws InterruptedException
     */
    private Dataset checkStudy(Dataset qrSeriesDS, Dataset qrInstanceDS,
            Map archiveStudy, ActiveAssociation aa) throws SQLException,
            InterruptedException, IOException {
        QueryCmd queryCmd = QueryCmd.create(qrSeriesDS, false, false, false, null);
        Map map = new HashMap();
        try {
            queryCmd.execute();
            Dataset ds = null;
            String seriesIUID = null;
            while (queryCmd.next()) {
                try {
                    ds = queryCmd.getDataset();
                    seriesIUID = ds.getString(Tags.SeriesInstanceUID);
                    if (archiveStudy.get(seriesIUID) == null) {
                        log
                                .warn("Study Reconciliation failed! Series "
                                        + seriesIUID
                                        + " not available at reference FIND SCP! study:"
                                        + qrSeriesDS
                                                .getString(Tags.StudyInstanceUID));
                        return null;
                    }
                    if (map.put(seriesIUID, ds) != null) {
                        log
                                .error("Local result contains two series with same iuid! :"
                                        + seriesIUID);
                        return null;
                    }
                } catch (Exception x) {
                    log.error("Study Reconciliation failed! (study:"
                            + qrSeriesDS.getString(Tags.StudyInstanceUID)
                            + ")! Internal error:" + x.getMessage(), x);
                    return null;
                }
            }
            if (archiveStudy.size() != map.size()) {
                log
                        .warn("Study Reconciliation failed! Number of Series mismatch! study:"
                                + qrSeriesDS.getString(Tags.StudyInstanceUID));
                return null;
            }
            boolean checked = true;
            qrInstanceDS.putUI(Tags.StudyInstanceUID, qrSeriesDS
                    .getString(Tags.StudyInstanceUID));
            for (Iterator iter = archiveStudy.keySet().iterator(); iter
                    .hasNext()
                    && checked;) {
                qrInstanceDS
                        .putUI(Tags.SeriesInstanceUID, (String) iter.next());
                checked = checkSeries(qrInstanceDS, aa);
            }
            return checked ? ds : null;
        } finally {
            queryCmd.close();
        }
    }

    /**
     * @param map
     * @param qrInstanceDS
     * @param aa
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws SQLException
     */
    private boolean checkSeries(Dataset qrInstanceDS, ActiveAssociation aa)
            throws InterruptedException, IOException, SQLException {
        Map archiveSeries = query(aa, qrInstanceDS, Tags.SOPInstanceUID);

        QueryCmd queryCmd = QueryCmd.create(qrInstanceDS, false, false, false, null);
        try {
            queryCmd.execute();
            Dataset ds;
            String iuid = null;
            Map map = new HashMap();
            while (queryCmd.next()) {
                try {
                    ds = queryCmd.getDataset();
                    iuid = ds.getString(Tags.SOPInstanceUID);
                    if (archiveSeries.get(iuid) == null) {
                        log
                                .warn("Study Reconciliation failed! Instance "
                                        + iuid
                                        + " not available at reference FIND SCP! study:"
                                        + qrInstanceDS
                                                .getString(Tags.StudyInstanceUID)
                                        + " series:"
                                        + qrInstanceDS
                                                .getString(Tags.SeriesInstanceUID));
                        return false;
                    }
                    if (map.put(iuid, ds) != null) {
                        log
                                .error("Local result contains two instances with same iuid! :"
                                        + iuid);
                        return false;
                    }
                } catch (Exception x) {
                    log.error("Study Reconciliation failed! (Series IUID:"
                            + qrInstanceDS.getString(Tags.SeriesInstanceUID)
                            + ")! Internal error:" + x.getMessage(), x);
                    return false;
                }
            }
            if (archiveSeries.size() != map.size()) {
                log
                        .warn("Study Reconciliation failed! Number of Instances mismatch! study:"
                                + qrInstanceDS.getString(Tags.StudyInstanceUID)
                                + " series:"
                                + qrInstanceDS
                                        .getString(Tags.SeriesInstanceUID));
                return false;
            }
        } finally {
            queryCmd.close();
        }
        return true;
    }

    public String reschedule() throws RemoteException, FinderException,
            DcmServiceException {
        StudyReconciliation studyReconciliation = newStudyReconciliation();
        Timestamp createdBefore = new Timestamp(System.currentTimeMillis());
        int total = 0;
        Collection col;
        while (true) {
            col = studyReconciliation.getStudyIuidsWithStatus(failureStatus,
                    createdBefore, limitNumberOfStudiesPerTask);
            if (col.isEmpty())
                break;
            studyReconciliation.updateStatus(col, scheduledStatus);
            total += col.size();
        }
        return total + " studies rescheduled for Reconciliation!";
    }
    private boolean isDisabled(int hour) {
        if (disabledEndHour == -1)
            return false;
        boolean sameday = disabledStartHour <= disabledEndHour;
        boolean inside = hour >= disabledStartHour && hour < disabledEndHour;
        return sameday ? inside : !inside;
    }

    protected void startService() throws Exception {
        listenerID = scheduler.startScheduler(timerIDCheckStudyReconciliation,
                taskInterval, updateCheckListener);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDCheckStudyReconciliation, listenerID,
                updateCheckListener);
        super.stopService();
    }

    private StudyReconciliation newStudyReconciliation() {
        try {
            StudyReconciliationHome home = (StudyReconciliationHome) EJBHomeFactory
                    .getFactory().lookup(StudyReconciliationHome.class,
                            StudyReconciliationHome.JNDI_NAME);
            return home.create();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to access Study Reconciliation SessionBean:", e);
        }
    }

    public String getTimerIDCheckStudyReconciliation() {
        return timerIDCheckStudyReconciliation;
    }

    public void setTimerIDCheckStudyReconciliation(
            String timerIDCheckStudyReconciliation) {
        this.timerIDCheckStudyReconciliation = timerIDCheckStudyReconciliation;
    }

}