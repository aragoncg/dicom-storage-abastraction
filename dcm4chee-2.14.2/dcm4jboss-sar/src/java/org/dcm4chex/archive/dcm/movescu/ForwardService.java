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

package org.dcm4chex.archive.dcm.movescu;

import java.util.HashMap;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.config.ForwardingRules;
import org.dcm4chex.archive.notif.PatientUpdated;
import org.dcm4chex.archive.notif.SeriesUpdated;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7596 $ $Date: 2008-10-16 16:53:05 +0200 (Thu, 16 Oct 2008) $
 * @since 27.08.2004
 */
public class ForwardService extends ServiceMBeanSupport {

    private static final NotificationFilterSupport seriesUpdatedFilter =
            new NotificationFilterSupport();

    private static final NotificationFilterSupport patientUpdatedFilter =
            new NotificationFilterSupport();

    private final NotificationListener seriesStoredListener =
            new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            SeriesStored seriesStored = (SeriesStored) notif.getUserData();
            Map<String, String[]> param = new HashMap<String, String[]>();
            param.put("calling", new String[] { seriesStored.getSourceAET() });
            String[] destAETs = forwardingRules
                    .getForwardDestinationsFor(param);
            for (int i = 0; i < destAETs.length; i++) {
                final String destAET = ForwardingRules.toAET(destAETs[i]);
                final long scheduledTime = ForwardingRules
                        .toScheduledTime(destAETs[i]);
                scheduleMove(seriesStored.getRetrieveAET(), destAET,
                        forwardPriority, null,
                        seriesStored.getStudyInstanceUID(),
                        seriesStored.getSeriesInstanceUID(),
                        sopIUIDsOrNull(seriesStored),
                        scheduledTime);
            }
        }

    };

    private String[] sopIUIDsOrNull(SeriesStored seriesStored) {
        int numI = seriesStored.getNumberOfInstances();
        if (numI > 1 && !isForwardOnInstanceLevelFromAET(
                        seriesStored.getSourceAET())) {
            return null;
        }
        String[] iuids = new String[numI];
        DcmElement sq = seriesStored.getIAN().getItem(Tags.RefSeriesSeq)
                .get(Tags.RefSOPSeq);
        for (int i = 0; i < iuids.length; i++) {
            iuids[i] = sq.getItem(i).getString(Tags.RefSOPInstanceUID);
        }
        return iuids;
    }

    private final NotificationListener seriesUpdatedListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            SeriesUpdated seriesUpdated = (SeriesUpdated) notif.getUserData();
            log.info("seriesUpdatedListener: series updated:"
                    + seriesUpdated.getSeriesIUID() + " Description:"
                    + seriesUpdated.getDescription());
            sendUpdatedNotification(null, null, seriesUpdated.getSeriesIUID());
        }
    };

    private final NotificationListener patientUpdatedListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            PatientUpdated patientUpdated = (PatientUpdated) notif
                    .getUserData();
            log.info("patientUpdatedListener: updated for patient "
                    + patientUpdated.getPatientID() + " Description:"
                    + patientUpdated.getDescription());
            sendUpdatedNotification(patientUpdated.getPatientID(), null, null);
        }
    };

    private static final String ALL = "ALL";

    private static final String NONE = "NONE";

    private static final String[] EMPTY = {};

    private ObjectName storeScpServiceName;

    private ObjectName moveScuServiceName;

    private ObjectName editContentServiceName;

    private String[] forwardOnInstanceLevelFromAETs = EMPTY;

    private String[] forwardModifiedToAETs = EMPTY;

    private int forwardPriority = 0;

    private ForwardingRules forwardingRules = new ForwardingRules("");

    public final String getForwardingRules() {
        return forwardingRules.toString();
    }

    public final void setForwardingRules(String forwardingRules) {
        this.forwardingRules = new ForwardingRules(forwardingRules);
    }

    public final String getForwardPriority() {
        return DicomPriority.toString(forwardPriority);
    }

    public final void setForwardPriority(String forwardPriority) {
        this.forwardPriority = DicomPriority.toCode(forwardPriority);
    }

    public final ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public final void setStoreScpServiceName(ObjectName storeScpServiceName) {
        this.storeScpServiceName = storeScpServiceName;
    }

    public final ObjectName getMoveScuServiceName() {
        return moveScuServiceName;
    }

    public final void setMoveScuServiceName(ObjectName moveScuServiceName) {
        this.moveScuServiceName = moveScuServiceName;
    }

    /**
     * @return Returns the editContentServiceName.
     */
    public final ObjectName getEditContentServiceName() {
        return editContentServiceName;
    }

    public String getForwardOnInstanceLevelFromAETs() {
        return forwardOnInstanceLevelFromAETs == null ? ALL 
                : forwardOnInstanceLevelFromAETs.length == 0 ? NONE 
                : StringUtils.toString(forwardOnInstanceLevelFromAETs, ',');
    }

    public void setForwardOnInstanceLevelFromAETs(String s) {
        forwardOnInstanceLevelFromAETs = ALL.equals(s) ? null
                : NONE.equals(s) ? EMPTY : StringUtils.split(s, ',');
    }

    private boolean isForwardOnInstanceLevelFromAET(String aet) {
        if (forwardOnInstanceLevelFromAETs == null) {
            return true;
        }
        for (int i = 0; i < forwardOnInstanceLevelFromAETs.length; i++) {
            if (aet.equals(forwardOnInstanceLevelFromAETs[i]))
                return true;
        }
        return false;
    }

    /**
     * @param editContentServiceName
     *            The editContentServiceName to set.
     */
    public final void setEditContentServiceName(
            ObjectName editContentServiceName) {
        this.editContentServiceName = editContentServiceName;
    }

    /**
     * @return Returns the enablePatientUpdated.
     */
    public boolean isEnablePatientUpdated() {
        return !patientUpdatedFilter.getEnabledTypes().isEmpty();
    }

    /**
     * @param enablePatientUpdated
     *            The enablePatientUpdated to set.
     * @throws InstanceNotFoundException
     */
    public void setEnablePatientUpdated(boolean enablePatientUpdated) {
        if (enablePatientUpdated) {
            patientUpdatedFilter.enableType(PatientUpdated.class.getName());
        } else {
            patientUpdatedFilter.disableAllTypes();
        }
    }

    /**
     * @return Returns the enableSeriesUpdated.
     */
    public boolean isEnableSeriesUpdated() {
        return !seriesUpdatedFilter.getEnabledTypes().isEmpty();
    }

    /**
     * @param enableSeriesUpdated
     *            The enableSeriesUpdated to set.
     */
    public void setEnableSeriesUpdated(boolean enableSeriesUpdated) {
        if (enableSeriesUpdated) {
            seriesUpdatedFilter.enableType(SeriesUpdated.class.getName());
        } else {
            seriesUpdatedFilter.disableAllTypes();
        }
    }

    public String getForwardModifiedToAETs() {
        return forwardModifiedToAETs.length == 0 ? NONE
                : StringUtils.toString(forwardModifiedToAETs, ',');
    }

    public void setForwardModifiedToAETs(String s) {
        forwardModifiedToAETs = NONE.equals(s) ? EMPTY
                : StringUtils.split(s, ',');
    }

    protected void startService() throws Exception {
        server.addNotificationListener(storeScpServiceName,
                seriesStoredListener, SeriesStored.NOTIF_FILTER, null);
        server.addNotificationListener(editContentServiceName,
                seriesUpdatedListener, seriesUpdatedFilter, null);
        server.addNotificationListener(editContentServiceName,
                patientUpdatedListener, patientUpdatedFilter, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(storeScpServiceName,
                seriesStoredListener, SeriesStored.NOTIF_FILTER, null);
        server.removeNotificationListener(editContentServiceName,
                seriesUpdatedListener, seriesUpdatedFilter, null);
        server.removeNotificationListener(editContentServiceName,
                patientUpdatedListener, patientUpdatedFilter, null);
    }

    private void scheduleMove(String retrieveAET, String destAET, int priority,
            String pid, String studyIUID, String seriesIUID, String[] sopIUIDs,
            long scheduledTime) {
        try {
            server.invoke(moveScuServiceName, "scheduleMove", new Object[] {
                    retrieveAET, destAET, new Integer(priority), pid,
                    studyIUID, seriesIUID, sopIUIDs, new Long(scheduledTime) },
                    new String[] { String.class.getName(),
                            String.class.getName(), int.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), String[].class.getName(),
                            long.class.getName() });
        } catch (Exception e) {
            log.error("Schedule Move failed:", e);
        }
    }

    private void sendUpdatedNotification(String patientID, String studyIUID,
            String seriesIUID) {
        for (int i = 0; i < forwardModifiedToAETs.length; i++) {
            scheduleMove(null, forwardModifiedToAETs[i], forwardPriority,
                    patientID, studyIUID, seriesIUID, null, 0l);
        }
    }

}