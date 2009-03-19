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

package org.dcm4chex.archive.dcm.mwlscp;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4chex.archive.dcm.AbstractScpService;
import org.dcm4chex.archive.dcm.mppsscp.MPPSScpService;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 6312 $ $Date: 2008-05-15 15:06:29 +0200 (Thu, 15 May 2008) $
 * @since 31.01.2004
 */
public class MWLFindScpService extends AbstractScpService implements
        NotificationListener {

    private static final String SPS_STATUS_STARTED = "STARTED";

    private static final String PPS_STATUS_IN_PROGRESS = "IN PROGRESS";

    private static final NotificationFilterSupport mppsFilter = 
            new NotificationFilterSupport();
    
    static {
        mppsFilter.enableType(MPPSScpService.EVENT_TYPE_MPPS_RECEIVED);
        mppsFilter.enableType(MPPSScpService.EVENT_TYPE_MPPS_LINKED);
    }

    private ObjectName mppsScpServiceName;
    
    private ObjectName mwlScuServiceName;

    private boolean useProxy = false;
    
    private boolean checkMatchingKeySupported = true;

    private MWLFindScp mwlFindScp = new MWLFindScp(this);

    /**
     * @return the useProxy
     */
    public boolean isUseProxy() {
        return useProxy;
    }

    /**
     * @param useProxy the useProxy to set
     */
    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
        if ( useProxy && server != null ) {
            checkMWLScuConfig();
        }
    }

    /**
     * @return Returns the checkMatchingKeySupport.
     */
    public boolean isCheckMatchingKeySupported() {
        return checkMatchingKeySupported;
    }

    /**
     * @param checkMatchingKeySupport
     *            The checkMatchingKeySupport to set.
     */
    public void setCheckMatchingKeySupported(boolean checkMatchingKeySupport) {
        this.checkMatchingKeySupported = checkMatchingKeySupport;
    }

    public final ObjectName getMppsScpServiceName() {
        return mppsScpServiceName;
    }

    public final void setMppsScpServiceName(ObjectName mppsScpServiceName) {
        this.mppsScpServiceName = mppsScpServiceName;
    }

    public final ObjectName getMwlScuServiceName() {
        return mwlScuServiceName;
    }

    public final void setMwlScuServiceName(ObjectName mwlScuServiceName) {
        this.mwlScuServiceName = mwlScuServiceName;
    }
   
    protected void startService() throws Exception {
        server.addNotificationListener(mppsScpServiceName, this, mppsFilter,
                null);
        super.startService();
    }

    protected void stopService() throws Exception {
        super.stopService();
        server.removeNotificationListener(mppsScpServiceName, this, mppsFilter,
                null);
    }

    protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.ModalityWorklistInformationModelFIND, mwlFindScp);
    }

    protected void unbindDcmServices(DcmServiceRegistry services) {
        services.unbind(UIDs.ModalityWorklistInformationModelFIND);
    }

    protected void enablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.ModalityWorklistInformationModelFIND,
                valuesToStringArray(tsuidMap));
    }

    protected void disablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.ModalityWorklistInformationModelFIND, null);
    }

    private MWLManagerHome getMWLManagerHome() throws HomeFactoryException {
        return (MWLManagerHome) EJBHomeFactory.getFactory().lookup(
                MWLManagerHome.class, MWLManagerHome.JNDI_NAME);
    }

    private MPPSManagerHome getMPPSManagerHome() throws HomeFactoryException {
        return (MPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME);
    }

    private Dataset getMPPS(String iuid) throws Exception {
        MPPSManager mgr = getMPPSManagerHome().create();
        try {
            return mgr.getMPPS(iuid);
        } finally {
            try {
                mgr.remove();
            } catch (Exception ignore) {
            }
        }
    }

    public void handleNotification(Notification notif, Object handback) {
        Dataset mpps = (Dataset) notif.getUserData();
        final String iuid = mpps.getString(Tags.SOPInstanceUID);
        final String status = mpps.getString(Tags.PPSStatus);
        DcmElement sq = mpps.get(Tags.ScheduledStepAttributesSeq);
        if (sq == null) {
            // MPPS N-SET can be ignored for status == IN PROGRESS
            if (PPS_STATUS_IN_PROGRESS.equals(status))
                return;
            try {
                mpps = getMPPS(iuid);
                sq = mpps.get(Tags.ScheduledStepAttributesSeq);
            } catch (Exception e) {
                log.error("Failed to load MPPS - " + iuid, e);
                return;
            }
        }
        MWLManager mgr;
        try {
            mgr = getMWLManagerHome().create();
        } catch (Exception e) {
            log.error("Failed to access MWL Manager:", e);
            return;
        }
        try {
            final String spsStatus = PPS_STATUS_IN_PROGRESS.equals(status) 
                    ? SPS_STATUS_STARTED
                    : status;
            for (int i = 0, n = sq.countItems(); i < n; ++i) {
                Dataset item = sq.getItem(i);
                String spsid = item.getString(Tags.SPSID);
                String rpid = item.getString(Tags.RequestedProcedureID);
                if (spsid != null) {
                    try {
                        if (mgr.updateSPSStatus(rpid, spsid, spsStatus)) {
                            log.info("Update MWL item[spsid=" + spsid
                                    + ", status=" + spsStatus + "]");
                        } else {
                            log.info("No such MWL item[spsid=" + spsid + "]");
                        }
                    } catch (Exception e) {
                        log.error("Failed to update MWL item[spsid=" + spsid
                                + "]", e);
                    }
                }
            }
        } finally {
            try {
                mgr.remove();
            } catch (Exception ignore) {
            }
        }
    }

    public boolean checkMWLScuConfig() {
        String proxyAET;
        try {
            proxyAET = (String) server.getAttribute(mwlScuServiceName, "CalledAETitle");
        } catch (Exception x) {
            log.warn("Cant check MWL SCU AET configuration! Continue with assumption that configuration is valid!",x);
            return true;
        }
        if ( "LOCAL".equals(proxyAET)){
            log.warn("Check MWL Proxy Settings: Called AET in MWLScu Service is LOCAL! -> Use local DB access!");
            return false;
        }
        for ( int i = 0 ; i < calledAETs.length ; i++ ) {
            if ( proxyAET.equals(calledAETs[i])) {
                log.warn("Check MWL Proxy Settings: Called AET ("+proxyAET+") in MWLScu is also configured as accepted AET here! -> Disable forwarding MWL C-FIND requests to avoid infinite loop!");
                return false;
            }
        }
        return true;
    }
    
    public int findMWLEntries( Dataset rqData, List l, boolean forceLocal ) throws InstanceNotFoundException, MBeanException, ReflectionException {
        return ((Integer) server.invoke(this.mwlScuServiceName, forceLocal ? "findMWLEntriesLocal":"findMWLEntries", 
                new Object[] {rqData, l}, 
                new String[]{Dataset.class.getName(), List.class.getName()})).intValue();
    }
}
