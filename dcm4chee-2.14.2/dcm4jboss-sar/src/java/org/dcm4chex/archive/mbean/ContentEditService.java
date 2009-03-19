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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Tags;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.InstancesAccessedMessage;
import org.dcm4che2.audit.message.ParticipantObjectDescription;
import org.dcm4che2.audit.message.PatientRecordMessage;
import org.dcm4che2.audit.message.StudyDeletedMessage;
import org.dcm4che2.audit.message.ParticipantObjectDescription.SOPClass;
import org.dcm4chex.archive.ejb.interfaces.ContentEdit;
import org.dcm4chex.archive.ejb.interfaces.ContentEditHome;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.PrivateManager;
import org.dcm4chex.archive.ejb.interfaces.PrivateManagerHome;
import org.dcm4chex.archive.notif.PatientUpdated;
import org.dcm4chex.archive.notif.SeriesUpdated;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@tiani.com
 * @version $Revision: 7596 $ $Date: 2008-10-16 16:53:05 +0200 (Thu, 16 Oct 2008) $
 * @since 17.02.2005
 */
public class ContentEditService extends ServiceMBeanSupport {

    private static final int DELETED = 1;

    private AuditLoggerDelegate auditLogger = new AuditLoggerDelegate(this);

    private boolean auditEnabled;

    private ContentEdit contentEdit;

    private ObjectName hl7SendServiceName;
    private String sendingApplication;
    private String sendingFacility;
    private String receivingApplication;
    private String receivingFacility;

    private ObjectName studyMgtScuServiceName;
    private ObjectName storeScpServiceName;
    private ObjectName mppsScpServiceName;

    private String callingAET;
    private String calledAET;

    private ContentManager contentMgr;

    private PrivateManager privateMgr;

    public ContentEditService() {
    }

    protected void startService() throws Exception {
    }

    protected void stopService() throws Exception {
    }

    public ObjectName getAuditLoggerName() {
        return auditLogger.getAuditLoggerName();
    }

    public void setAuditLoggerName(ObjectName auditLogName) {
        this.auditLogger.setAuditLoggerName(auditLogName);
    }

    public void setAuditEnabled(boolean auditEnabled) {
        this.auditEnabled = auditEnabled;
    }

    public boolean getAuditEnabled() {
        return auditEnabled;
    }

    public final ObjectName getHL7SendServiceName() {
        return hl7SendServiceName;
    }

    public final void setHL7SendServiceName(ObjectName name) {
        this.hl7SendServiceName = name;
    }

    public final ObjectName getStudyMgtScuServiceName() {
        return studyMgtScuServiceName;
    }

    public final void setStudyMgtScuServiceName(ObjectName name) {
        this.studyMgtScuServiceName = name;
    }

    /**
     * @return Returns the storeScpServiceName.
     */
    public ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    /**
     * @param storeScpServiceName
     *            The storeScpServiceName to set.
     */
    public void setStoreScpServiceName(ObjectName storeScpServiceName) {
        this.storeScpServiceName = storeScpServiceName;
    }

    /**
     * @return the mppsScpServiceName
     */
    public ObjectName getMppsScpServiceName() {
        return mppsScpServiceName;
    }

    /**
     * @param mppsScpServiceName
     *            the mppsScpServiceName to set
     */
    public void setMppsScpServiceName(ObjectName mppsScpServiceName) {
        this.mppsScpServiceName = mppsScpServiceName;
    }

    /**
     * @return Returns the receivingApplication.
     */
    public String getReceivingApplication() {
        return receivingApplication;
    }

    /**
     * @param receivingApplication
     *            The receivingApplication to set.
     */
    public void setReceivingApplication(String receivingApplication) {
        this.receivingApplication = receivingApplication;
    }

    /**
     * @return Returns the receivingFacility.
     */
    public String getReceivingFacility() {
        return receivingFacility;
    }

    /**
     * @param receivingFacility
     *            The receivingFacility to set.
     */
    public void setReceivingFacility(String receivingFacility) {
        this.receivingFacility = receivingFacility;
    }

    /**
     * @return Returns the sendingApplication.
     */
    public String getSendingApplication() {
        return sendingApplication;
    }

    /**
     * @param sendingApplication
     *            The sendingApplication to set.
     */
    public void setSendingApplication(String sendingApplication) {
        this.sendingApplication = sendingApplication;
    }

    /**
     * @return Returns the sendingFacility.
     */
    public String getSendingFacility() {
        return sendingFacility;
    }

    /**
     * @param sendingFacility
     *            The sendingFacility to set.
     */
    public void setSendingFacility(String sendingFacility) {
        this.sendingFacility = sendingFacility;
    }

    /**
     * @return Returns the calledAET.
     */
    public String getCalledAET() {
        return calledAET;
    }

    /**
     * @param calledAET
     *            The calledAET to set.
     */
    public void setCalledAET(String calledAET) {
        this.calledAET = calledAET;
    }

    /**
     * @return Returns the callingAET.
     */
    public String getCallingAET() {
        return callingAET;
    }

    /**
     * @param callingAET
     *            The callingAET to set.
     */
    public void setCallingAET(String callingAET) {
        this.callingAET = callingAET;
    }

    public Dataset createPatient(Dataset ds) throws RemoteException,
            CreateException, HomeFactoryException, FinderException {
        if (log.isDebugEnabled())
            log.debug("create Partient");
        String pid = ds.getString(Tags.PatientID);
        String issuer = ds.getString(Tags.IssuerOfPatientID);
        Dataset ds1 = lookupContentManager().getPatientByID(pid, issuer);
        if (ds1 != null) {
            throw new CreateException("Patient with ID(pid:" + pid + " issuer:"
                    + issuer + ") already exists!");
        }
        ds1 = lookupContentEdit().createPatient(ds);
        sendHL7PatientXXX(ds, "ADT^A04");// use update to create patient, msg
                                         // type is 'Register a patient'
        logPatientRecord(ds, PatientRecordMessage.CREATE);
        return ds1;
    }

    public Map mergePatients(Long patPk, long[] mergedPks)
            throws RemoteException, HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("merge Partient");
        Map map = lookupContentEdit().mergePatients(patPk.longValue(),
                mergedPks);
        if (!map.containsKey("ERROR")) {
            sendHL7PatientMerge((Dataset) map.get("DOMINANT"), (Dataset[]) map
                    .get("MERGED"));
            String patID = ((Dataset) map.get("DOMINANT"))
                    .getString(Tags.PatientID);
            sendJMXNotification(new PatientUpdated(patID, "Patient merge"));
        }
        return map;
    }

    public Dataset createStudy(Dataset ds, Long patPk) throws CreateException,
            RemoteException, HomeFactoryException {
        if (log.isDebugEnabled())
            log.debug("create study:");
        log.debug(ds);
        Dataset ds1 = lookupContentEdit().createStudy(ds, patPk.longValue());
        sendStudyMgt(ds1.getString(Tags.StudyInstanceUID), Command.N_CREATE_RQ,
                0, ds1);
        return ds1;
    }

    public Dataset createSeries(Dataset ds, Long studyPk)
            throws CreateException, RemoteException, HomeFactoryException,
            FinderException {
        Dataset ds1 = lookupContentEdit().createSeries(ds, studyPk.longValue());
        log.debug("create Series ds1:");
        log.debug(ds1);
        sendStudyMgt(ds1.getString(Tags.StudyInstanceUID), Command.N_SET_RQ, 0,
                ds1);
        String seriesIUID = ds1.get(Tags.RefSeriesSeq).getItem().getString(
                Tags.SeriesInstanceUID);
        return lookupContentManager().getSeriesByIUID(seriesIUID);
    }

    public void updatePatient(Dataset ds) throws RemoteException,
            HomeFactoryException, CreateException {
        log.debug("update Patient");
        Collection col = lookupContentEdit().updatePatient(ds);
        sendHL7PatientXXX(ds, "ADT^A08");

        String patID = ds.getString(Tags.PatientID);
        sendJMXNotification(new PatientUpdated(patID, "Patient update"));
        logPatientRecord(ds, PatientRecordMessage.UPDATE);
    }

    public Map linkMppsToMwl(String[] spsIDs, String[] mppsIUIDs) {
        return doLinkMppsToMwl(spsIDs, mppsIUIDs);
    }

    public Map linkMppsToMwl(Dataset[] spsAttrs, String[] mppsIUIDs) {
        return doLinkMppsToMwl(spsAttrs, mppsIUIDs);
    }

    private Map doLinkMppsToMwl(Object o, String[] mppsIUIDs) {
        try {
            Map map = (Map) server.invoke(mppsScpServiceName, "linkMppsToMwl",
                    new Object[] { o, mppsIUIDs }, new String[] {
                            o.getClass().getName(), String[].class.getName() });
            List studyDsN = (List) map.get("StudyMgtDS");
            Dataset dsN;
            for (Iterator iter = studyDsN.iterator(); iter.hasNext();) {
                dsN = (Dataset) iter.next();
                sendStudyMgt(dsN.getString(Tags.StudyInstanceUID),
                        Command.N_SET_RQ, 0, dsN);
                sendSeriesUpdatedNotifications(dsN, "Series update");
            }
            return map;
        } catch (Exception x) {
            log.error("Exception occured in linkMppsToMwl: " + x.getMessage(),
                    x);
            return null;
        }

    }

    private void sendStudyMgt(String iuid, int commandField, int actionTypeID,
            Dataset dataset) {
        String infoStr = "iuid: " + iuid + ", cmd:" + commandField
                + ", action: " + actionTypeID + ", ds:";
        if (log.isDebugEnabled()) {
            log.debug("send StudyMgt command: " + infoStr);
            log.debug(dataset);
        }

        try {
            server.invoke(this.studyMgtScuServiceName, "forward", new Object[] {
                    this.callingAET, this.calledAET, iuid,
                    new Integer(commandField), new Integer(actionTypeID),
                    dataset }, new String[] { String.class.getName(),
                    String.class.getName(), String.class.getName(),
                    int.class.getName(), int.class.getName(),
                    Dataset.class.getName() });
        } catch (InstanceNotFoundException infe) {
            log.warn("The MBean service [" + studyMgtScuServiceName
                    + "] is not registered. Ignore sending StudyMgt command: "
                    + infoStr);
        } catch (Exception e) {
            log.error("Failed to send StudyMgt command:" + infoStr, e);
        }
    }

    /**
     * @param ds
     */
    private void sendHL7PatientXXX(Dataset ds, String msgType) {
        try {
            server.invoke(this.hl7SendServiceName, "sendHL7PatientXXX",
                    new Object[] {
                            ds,
                            msgType,
                            getSendingApplication() + "^"
                                    + getSendingFacility(),
                            getReceivingApplication() + "^"
                                    + getReceivingFacility(), Boolean.TRUE },
                    new String[] { Dataset.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), boolean.class.getName() });
        } catch (InstanceNotFoundException infe) {
            log.warn("The MBean service [" + hl7SendServiceName
                    + "] is not registered. Ignore sending HL7 message: "
                    + msgType);
        } catch (Exception e) {
            log.error("Failed to send HL7 message:" + msgType, e);
            log.error(ds);
        }
    }

    private void sendHL7PatientMerge(Dataset dsDominant, Dataset[] priorPats) {
        try {
            server.invoke(this.hl7SendServiceName, "sendHL7PatientMerge",
                    new Object[] {
                            dsDominant,
                            priorPats,
                            getSendingApplication() + "^"
                                    + getSendingFacility(),
                            getReceivingApplication() + "^"
                                    + getReceivingFacility(), Boolean.TRUE },
                    new String[] { Dataset.class.getName(),
                            Dataset[].class.getName(), String.class.getName(),
                            String.class.getName(), boolean.class.getName() });
        } catch (InstanceNotFoundException infe) {
            log
                    .warn("The MBean service ["
                            + hl7SendServiceName
                            + "] is not registered. Ignore sending HL7 patient merge message.");
        } catch (Exception e) {
            log.error("Failed to send HL7 patient merge message:", e);
            log.error(dsDominant);
        }

    }

    public void updateStudy(Dataset ds) throws RemoteException,
            HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("update Study");
        Dataset dsN = lookupContentEdit().updateStudy(ds);
        sendStudyMgt(dsN.getString(Tags.StudyInstanceUID), Command.N_SET_RQ, 0,
                dsN);
        sendSeriesUpdatedNotifications(dsN, "Study update");
    }

    public void updateSeries(Dataset ds) throws RemoteException,
            HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("update Series");
        Dataset dsN = lookupContentEdit().updateSeries(ds);
        if (log.isDebugEnabled()) {
            log.debug("update series: dsN:");
            log.debug(dsN);
        }
        sendStudyMgt(dsN.getString(Tags.StudyInstanceUID), Command.N_SET_RQ, 0,
                dsN);
        sendSeriesUpdatedNotifications(dsN, "Series update");
    }

    public void movePatientToTrash(long pk) throws RemoteException,
            HomeFactoryException, CreateException {
        Collection col = lookupPrivateManager().movePatientToTrash(pk);
        Dataset ds = null;
        for (Iterator iter = col.iterator(); iter.hasNext();) {
            ds = (Dataset) iter.next();
            if (ds.containsValue(Tags.StudyInstanceUID)) // patient without
                                                         // study?
                logStudyDeleted(ds);
        }
        sendHL7PatientXXX(ds, "ADT^A23");// Send Patient delete message
        logPatientRecord(ds, PatientRecordMessage.DELETE);
        if (log.isDebugEnabled()) {
            log.debug("Patient moved to trash. ds:");
            log.debug(ds);
        }
    }

    public void moveStudyToTrash(long pk) throws RemoteException,
            HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("Move Study (pk=" + pk + ") to trash.");
        Dataset ds = lookupPrivateManager().moveStudyToTrash(pk);
        if (log.isDebugEnabled())
            log.debug("sendStudyMgt N-DELETE Study (pk=" + pk + ").");
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_DELETE_RQ,
                0, ds);
        logStudyDeleted(ds);
        if (log.isDebugEnabled()) {
            log.debug("Study moved to trash. ds:");
            log.debug(ds);
        }
    }

    public void moveSeriesToTrash(long pk) throws RemoteException,
            HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("Move Series (pk=" + pk + ") to trash.");
        Dataset ds = lookupPrivateManager().moveSeriesToTrash(pk);
        if (log.isDebugEnabled())
            log.debug("sendStudyMgt N-ACTION Series (pk=" + pk + ").");
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_ACTION_RQ,
                1, ds);
        Collection c = new ArrayList();
        c.add(ds);
        logInstancesAccessed(c, InstancesAccessedMessage.DELETE);
        if (log.isDebugEnabled()) {
            log.debug("Series moved to trash. ds:");
            log.debug(ds);
        }
    }

    public void moveInstanceToTrash(long pk) throws RemoteException,
            HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("Move Instance (pk=" + pk + ") to trash.");
        Dataset ds = lookupPrivateManager().moveInstanceToTrash(pk);
        if (log.isDebugEnabled())
            log.debug("sendStudyMgt N-ACTION Instance (pk=" + pk + ").");
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_ACTION_RQ,
                2, ds);
        Collection c = new ArrayList();
        c.add(ds);
        logInstancesAccessed(c, InstancesAccessedMessage.DELETE);
        if (log.isDebugEnabled()) {
            log.debug("Instance moved to trash. ds:");
            log.debug(ds);
        }
    }

    public Dataset moveInstancesToTrash(String[] iuids) throws Exception {
        if (log.isDebugEnabled())
            log.debug("Move Instances to trash: " + iuids);
        Collection dss = lookupPrivateManager().moveInstancesToTrash(iuids,
                true);
        if (dss.size() != 1)
            throw new Exception("moveInstancesToTrash failed");
        Dataset ds = (Dataset) dss.iterator().next();
        if (log.isDebugEnabled())
            log.debug("sendStudyMgt N-ACTION Instances: " + iuids);
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_ACTION_RQ,
                2, ds);
        if (log.isDebugEnabled()) {
            log.debug("Instances moved to trash. ds:");
            log.debug(ds);
        }
        return ds;
    }

    public List undeletePatient(long privPatPk) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("undelete Patient from trash. pk:" + privPatPk);
        List[] files = lookupContentManager().listPatientFilesToRecover(
                privPatPk);
        List failed = recoverFiles(files);
        if (failed.isEmpty()) {
            lookupPrivateManager().deletePrivateFiles(files[0]);
            lookupPrivateManager().deletePrivatePatient(privPatPk);
        }
        return failed;
    }

    public List undeleteStudy(long privStudyPk) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("undelete Study from trash. pk:" + privStudyPk);
        List[] files = lookupContentManager().listStudyFilesToRecover(
                privStudyPk);
        List failed = recoverFiles(files);
        if (failed.isEmpty()) {
            lookupPrivateManager().deletePrivateFiles(files[0]);
            lookupPrivateManager().deletePrivateStudy(privStudyPk);
        }
        return failed;
    }

    public List undeleteSeries(long privSeriesPk) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("undelete Series from trash. pk:" + privSeriesPk);
        List[] files = lookupContentManager().listSeriesFilesToRecover(
                privSeriesPk);
        List failed = recoverFiles(files);
        if (failed.isEmpty()) {
            lookupPrivateManager().deletePrivateFiles(files[0]);
            lookupPrivateManager().deletePrivateSeries(privSeriesPk);
        }
        return failed;
    }

    public List undeleteInstance(long privInstancePk) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        if (log.isDebugEnabled())
            log.debug("undelete Instance from trash. pk:" + privInstancePk);
        List[] files = lookupContentManager().listInstanceFilesToRecover(
                privInstancePk);
        List failed = recoverFiles(files);
        if (failed.isEmpty()) {
            lookupPrivateManager().deletePrivateFiles(files[0]);
            lookupPrivateManager().deletePrivateInstance(privInstancePk);
        }
        return failed;
    }

    /**
     * @param files
     */
    private List recoverFiles(List[] files) {
        if (files == null || files.length != 2 || files[0] == null
                || files[1] == null || files[0].size() != files[1].size()) {
            throw new IllegalArgumentException(
                    "List array for files to recover is illegal:" + files);
        }
        List failed = new ArrayList();
        Iterator iterFileDTO = files[0].iterator();
        Iterator iterDS = files[1].iterator();
        String prevseriuid = null;
        while (iterFileDTO.hasNext()) {
            FileDTO fileDTO = (FileDTO) iterFileDTO.next();
            Dataset ds = (Dataset) iterDS.next();
            try {
                importFile(fileDTO, ds, prevseriuid, !iterFileDTO.hasNext());
                prevseriuid = ds.getString(Tags.SeriesInstanceUID);
            } catch (Exception e) {
                failed.add(fileDTO);
                log.warn("Undelete failed for file " + fileDTO + " ds:", e);
                log.warn(ds);
            }
        }
        return failed;
    }

    public void deletePatient(long patPk) throws RemoteException,
            HomeFactoryException, CreateException, FinderException {
        if (log.isDebugEnabled())
            log.debug("delete Patient from trash. pk:" + patPk);
        lookupPrivateManager().deletePrivatePatient(patPk);
    }

    public void deleteStudy(long studyPk) throws RemoteException,
            HomeFactoryException, CreateException, FinderException {
        if (log.isDebugEnabled())
            log.debug("delete Study from trash. pk:" + studyPk);
        lookupPrivateManager().deletePrivateStudy(studyPk);
    }

    public void deleteSeries(long seriesPk) throws RemoteException,
            HomeFactoryException, CreateException, FinderException {
        if (log.isDebugEnabled())
            log.debug("delete Series from trash. pk:" + seriesPk);
        lookupPrivateManager().deletePrivateSeries(seriesPk);
    }

    public void deleteInstance(long pk) throws RemoteException,
            HomeFactoryException, CreateException, FinderException {
        if (log.isDebugEnabled())
            log.debug("delete Instance from trash. pk:" + pk);
        lookupPrivateManager().deletePrivateInstance(pk);// dont delete files of
                                                         // instance (needed for
                                                         // purge process)
    }

    public void emptyTrash() throws RemoteException, HomeFactoryException,
            CreateException, FinderException {
        if (log.isDebugEnabled())
            log
                    .debug("EMPTY TRASH! (delete all entries of 'private' tables of privateType:"
                            + DELETED + ")");
        lookupPrivateManager().deleteAll(DELETED);
    }

    public void moveStudies(long[] study_pks, Long patient_pk)
            throws FinderException, HomeFactoryException, CreateException,
            RemoteException {
        if (log.isDebugEnabled())
            log.debug("move Studies");
        Collection col = lookupContentEdit().moveStudies(study_pks,
                patient_pk.longValue());
        Iterator iter = col.iterator();
        Dataset ds;
        while (iter.hasNext()) {
            ds = (Dataset) iter.next();
            sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_SET_RQ,
                    0, ds);
            sendSeriesUpdatedNotifications(ds, "Move studies");
        }
    }

    public void moveSeries(long[] series_pks, Long study_pk)
            throws RemoteException, HomeFactoryException, CreateException,
            FinderException {
        if (log.isDebugEnabled())
            log.debug("move Series");
        Dataset ds = lookupContentEdit().moveSeries(series_pks,
                study_pk.longValue());
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_SET_RQ, 0,
                ds);
        sendSeriesUpdatedNotifications(ds, "Move series");
    }

    public void moveInstances(long[] instance_pks, Long series_pk)
            throws RemoteException, HomeFactoryException, CreateException,
            FinderException {
        if (log.isDebugEnabled())
            log.debug("move Instances");
        Dataset ds = lookupContentEdit().moveInstances(instance_pks,
                series_pk.longValue());
        sendStudyMgt(ds.getString(Tags.StudyInstanceUID), Command.N_SET_RQ, 0,
                ds);
        this.sendSeriesUpdatedNotifications(ds, "Move instances");
    }

    private ContentEdit lookupContentEdit() throws HomeFactoryException,
            RemoteException, CreateException {
        if (contentEdit != null)
            return contentEdit;
        ContentEditHome home = (ContentEditHome) EJBHomeFactory.getFactory()
                .lookup(ContentEditHome.class, ContentEditHome.JNDI_NAME);
        contentEdit = home.create();
        return contentEdit;
    }

    private ContentManager lookupContentManager() throws HomeFactoryException,
            RemoteException, CreateException {
        if (contentMgr != null)
            return contentMgr;
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
                .getFactory().lookup(ContentManagerHome.class,
                        ContentManagerHome.JNDI_NAME);
        contentMgr = home.create();
        return contentMgr;
    }

    private PrivateManager lookupPrivateManager() throws HomeFactoryException,
            RemoteException, CreateException {
        if (privateMgr != null)
            return privateMgr;
        PrivateManagerHome home = (PrivateManagerHome) EJBHomeFactory
                .getFactory().lookup(PrivateManagerHome.class,
                        PrivateManagerHome.JNDI_NAME);
        privateMgr = home.create();
        return privateMgr;
    }

    /**
     * Send a SeriesUpdated JMX notification for each series referenced in
     * studyMgt dataset.
     * 
     * This notification is used in ForwardService to forward the series
     * referenced in SeriesUpdated.
     * 
     * @param studyMgtDS
     *            StudyMgt dataset (Dataset with Referenced Series Sequence)
     */
    private void sendSeriesUpdatedNotifications(Dataset studyMgtDS,
            String description) {
        DcmElement sq = studyMgtDS.get(Tags.RefSeriesSeq);
        for (int i = 0, len = sq.countItems(); i < len; i++) {
            sendJMXNotification(new SeriesUpdated(sq.getItem(i).getString(
                    Tags.SeriesInstanceUID), description));
        }

    }

    protected void sendJMXNotification(Object o) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(o.getClass().getName(), this,
                eventID);
        notif.setUserData(o);
        super.sendNotification(notif);
    }

    private void importFile(FileDTO fileDTO, Dataset ds, String prevseriuid,
            boolean last) throws Exception {
        server.invoke(this.storeScpServiceName, "importFile", new Object[] {
                fileDTO, ds, prevseriuid, new Boolean(last) }, new String[] {
                FileDTO.class.getName(), Dataset.class.getName(),
                String.class.getName(), boolean.class.getName() });
    }

    private void logPatientRecord(Dataset ds, AuditEvent.ActionCode actionCode) {
        if (auditLogger.isAuditLogIHEYr4() || !auditEnabled)
            return;
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage
                .isEnableDNSLookups());
        log.debug("log Patient Record! actionCode:" + actionCode);
        try {
            PatientRecordMessage msg = new PatientRecordMessage(actionCode);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            PersonName pn = ds.getPersonName(Tags.PatientName);
            String pname = pn != null ? pn.format() : null;
            msg.addPatient(ds.getString(Tags.PatientID), pname);
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        } catch (Exception x) {
            log.warn("Audit Log 'Patient Record' (actionCode:" + actionCode
                    + ") failed:", x);
        }
    }

    private void logInstancesAccessed(Collection studies,
            AuditEvent.ActionCode actionCode) {
        if (auditLogger.isAuditLogIHEYr4())
            return;
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage
                .isEnableDNSLookups());
        log.debug("log instances Accessed! actionCode:" + actionCode);
        try {
            InstancesAccessedMessage msg = new InstancesAccessedMessage(
                    actionCode);
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            Iterator iter = studies.iterator();
            Dataset studyMgtDs = (Dataset) iter.next();
            PersonName pn = studyMgtDs.getPersonName(Tags.PatientName);
            String pname = pn != null ? pn.format() : null;
            msg.addPatient(studyMgtDs.getString(Tags.PatientID), pname);
            while (studyMgtDs != null) {
                msg.addStudy(studyMgtDs.getString(Tags.StudyInstanceUID),
                        getStudyDescription(studyMgtDs));
                studyMgtDs = iter.hasNext() ? (Dataset) iter.next() : null;
            }
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        } catch (Exception x) {
            log.warn("Audit Log 'Instances Accessed' (actionCode:" + actionCode
                    + ") failed:", x);
        }
    }

    private void logStudyDeleted(Dataset studyMgtDs) {
        if (auditLogger.isAuditLogIHEYr4())
            return;
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage
                .isEnableDNSLookups());
        try {
            StudyDeletedMessage msg = new StudyDeletedMessage();
            msg.addUserPerson(userInfo.getUserId(), null, null, userInfo
                    .getHostName(), true);
            PersonName pn = studyMgtDs.getPersonName(Tags.PatientName);
            String pname = pn != null ? pn.format() : null;
            msg.addPatient(studyMgtDs.getString(Tags.PatientID), pname);
            msg.addStudy(studyMgtDs.getString(Tags.StudyInstanceUID),
                    getStudyDescription(studyMgtDs));
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        } catch (Exception x) {
            log.warn("Audit Log 'Study Deleted' failed:", x);
        }
    }

    private ParticipantObjectDescription getStudyDescription(Dataset studyMgtDs) {
        ParticipantObjectDescription desc = new ParticipantObjectDescription();
        String accNr = studyMgtDs.getString(Tags.AccessionNumber);
        if (accNr != null)
            desc.addAccession(accNr);
        addSOPClassInfo(desc, studyMgtDs.get(Tags.RefSeriesSeq));
        return desc;
    }

    private void addSOPClassInfo(ParticipantObjectDescription desc,
            DcmElement refSeries) {
        if (refSeries == null)
            return;
        Dataset ds;
        DcmElement refSopSeq;
        String cuid;
        HashMap map = new HashMap();
        int[] noi;
        for (int i = 0, len = refSeries.countItems(); i < len; i++) {
            refSopSeq = refSeries.getItem(i).get(Tags.RefSOPSeq);
            if (refSopSeq != null) {
                for (int j = 0, jlen = refSopSeq.countItems(); j < jlen; j++) {
                    ds = refSopSeq.getItem(j);
                    cuid = ds.getString(Tags.RefSOPClassUID);
                    noi = (int[]) map.get(cuid);
                    if (noi == null) {
                        map.put(cuid, new int[] { 1 });
                    } else {
                        noi[0]++;
                    }
                }
            }
        }
        Map.Entry entry;
        for (Iterator iter = map.entrySet().iterator(); iter.hasNext();) {
            entry = (Map.Entry) iter.next();
            desc.addSOPClass(new SOPClass((String) entry.getKey())
                    .setNumberOfInstances(((int[]) entry.getValue())[0]));
        }
    }

}
