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

package org.dcm4chex.archive.dcm.gpwlscp;

import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.management.Notification;
import javax.management.NotificationFilter;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4chex.archive.dcm.AbstractScpService;
import org.dcm4chex.archive.ejb.jdbc.GPWLQueryCmd;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-05-15 15:06:29 +0200 (Thu, 15 May 2008) $
 * @since 03.04.2005
 */

public class GPWLScpService extends AbstractScpService {

    public static final String ON_PPS_NOTIF = "org.dcm4chex.archive.dcm.gpwlscp.onpps";

    public static final NotificationFilter ON_PPS_NOTIF_FILTER = new NotificationFilter() {

        private static final long serialVersionUID = -1969818592538255743L;

        public boolean isNotificationEnabled(Notification notif) {
            return ON_PPS_NOTIF.equals(notif.getType());
        }
    };

    public static final String ON_SPS_ACTION_NOTIF = "org.dcm4chex.archive.dcm.gpwlscp.onspsaction";

    public static final NotificationFilter ON_SPS_ACTION_NOTIF_FILTER = new NotificationFilter() {

        private static final long serialVersionUID = 5671824066766098134L;

        public boolean isNotificationEnabled(Notification notif) {
            return ON_SPS_ACTION_NOTIF.equals(notif.getType());
        }
    };
    
    /** Map containing accepted SOP Class UIDs.
     * key is name (as in config string), value is real uid) */
    private Map cuidMap = new LinkedHashMap();
    private GPWLFindScp gpwlFindScp = new GPWLFindScp(this);
    private GPSPSScp spspsScp = new GPSPSScp(this);
    private PPSScp ppsScp = new PPSScp(this);

    void sendActionNotification(String iuid) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(ON_SPS_ACTION_NOTIF, this, eventID);
        notif.setUserData(iuid);
        super.sendNotification(notif);
    }
 
    void sendPPSNotification(Dataset pps) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(ON_PPS_NOTIF, this, eventID);
        notif.setUserData(pps);
        super.sendNotification(notif);
    }
    
    public String getAcceptedSOPClasses() {
        return toString(cuidMap);
    }

    public void setAcceptedSOPClasses(String s) {
        updateAcceptedSOPClass(cuidMap, s, null);
    }
    
    public final boolean getAccessBlobAsLongVarBinary() {
        return GPWLQueryCmd.blobAccessType == Types.LONGVARBINARY;
    }

    public final void setAccessBlobAsLongVarBinary(boolean enable) {
        GPWLQueryCmd.blobAccessType = enable ? Types.LONGVARBINARY : Types.BLOB;
    }

    public final String getTransactionIsolationLevel() {
        return GPWLQueryCmd.transactionIsolationLevelAsString(
                GPWLQueryCmd.transactionIsolationLevel);
    }

    public final void setTransactionIsolationLevel(String level) {
        GPWLQueryCmd.transactionIsolationLevel = 
            GPWLQueryCmd.transactionIsolationLevelOf(level);
    }

    protected void startService() throws Exception {
        super.startService();
    }

    protected void stopService() throws Exception {
        super.stopService();
    }
    
    protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.GeneralPurposeWorklistInformationModelFIND, gpwlFindScp);
        services.bind(UIDs.GeneralPurposeScheduledProcedureStepSOPClass, spspsScp);
        services.bind(UIDs.GeneralPurposePerformedProcedureStepSOPClass, ppsScp);
    }

    protected void unbindDcmServices(DcmServiceRegistry services) {
        services.unbind(UIDs.GeneralPurposeWorklistInformationModelFIND);
        services.unbind(UIDs.GeneralPurposeScheduledProcedureStepSOPClass);
        services.unbind(UIDs.GeneralPurposePerformedProcedureStepSOPClass);
    }

    protected void enablePresContexts(AcceptorPolicy policy) {
        putPresContexts(policy, valuesToStringArray(cuidMap),
                valuesToStringArray(tsuidMap));
    }

    protected void disablePresContexts(AcceptorPolicy policy) {
        putPresContexts(policy, valuesToStringArray(cuidMap),null);
    }
}
