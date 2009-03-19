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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.transform.Templates;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.dcm.ianscu.IANScuService;
import org.dcm4chex.archive.ejb.interfaces.GPWLManager;
import org.dcm4chex.archive.ejb.interfaces.GPWLManagerHome;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.XSLTUtils;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 3340 $ $Date: 2007-05-12 18:00:31 +0200 (Sat, 12 May 2007) $
 * @since Mar 18, 2007
 */
public class GPWLFeedService2 extends ServiceMBeanSupport {

    private static final String MPPS2GPWL_XSL = "mpps2gpwl.xsl";

    private static final String LOGFILE_PATTERN = "yyyyMMddHHmmss.SSS'-mpps.xml'";

    private String[] logAETs = {};

    private String[] pgpAETs = {};
    
    private String[] appendAETs = {};
    
    private File logDir;

    private TemplatesDelegate templates = new TemplatesDelegate(this);
    
    private ObjectName ianScuServiceName;
    
    private final NotificationListener ianListener = new NotificationListener(){

        public void handleNotification(Notification notif, Object handback) {
            GPWLFeedService2.this.onIAN((Dataset) notif.getUserData());           
        }
    };

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    
    public final String getWorkItemConfigDir() {
        return templates.getConfigDir();
    }

    public final void setWorkItemConfigDir(String path) {
        templates.setConfigDir(path);
    }
    
    public final String getLogStationAETs() {
        return StringUtils.toString(logAETs, '\\');
    }

    public final void setLogStationAETs(String aets) {
        logAETs = StringUtils.split(aets, '\\');
    }

    public final String getPGPStationAETs() {
        return StringUtils.toString(pgpAETs, '\\');
    }

    public final void setPGPStationAETs(String aets) {
        pgpAETs = StringUtils.split(aets, '\\');
    }

    public final String getAppendCaseStationAETs() {
        return StringUtils.toString(appendAETs, '\\');
    }

    public final void setAppendCaseStationAETs(String aets) {
        appendAETs = StringUtils.split(aets, '\\');
    }

    public final ObjectName getIANScuServiceName() {
        return ianScuServiceName;
    }

    public final void setIANScuServiceName(ObjectName ianScuServiceName) {
        this.ianScuServiceName = ianScuServiceName;
    }

    protected void startService() throws Exception {
        logDir = new File(ServerConfigLocator.locate().getServerHomeDir(), "log");
        server.addNotificationListener(ianScuServiceName,
                ianListener , IANScuService.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(ianScuServiceName,
                ianListener, IANScuService.NOTIF_FILTER, null);
    }

    private void onIAN(Dataset mpps) {
        String aet = mpps.getString(Tags.PerformedStationAET);
        boolean logMPPS = contains(logAETs, aet);
        if (logMPPS) {
            logMPPS(aet, mpps);
        }
        Templates stylesheet = templates.getTemplatesForAET(aet, MPPS2GPWL_XSL);
        if (stylesheet == null) {
            log.info("No mpps2gpwl.xsl found for " + aet);
            return;
        }
        Dataset wkitems = DcmObjectFactory.getInstance().newDataset();
        try {
            XSLTUtils.xslt(mpps, stylesheet, wkitems);
        } catch (Exception e) {
            log.error("Failed to create work items triggered by MPPS from " + aet, e);
            return;
        }
        DcmElement wkitemSeq = wkitems.get(PrivateTags.WorkItemSeq);
        log.info("Creating " + wkitemSeq.countItems() + " work item(s) triggered by MPPS from " + aet);
        try {
            createWorkItems(wkitemSeq, contains(pgpAETs, aet), contains(appendAETs, aet));
        } catch (Exception e) {
            log.error("Failed to create work item triggered by MPPS from " + aet, e);
        }
    }

    private synchronized void createWorkItems(DcmElement wkitemSeq, 
            boolean checkPGP, boolean checkAppend) throws Exception {
        GPWLManager gpwlmgr = getGPWLManager();
        for (int i = 0, n = wkitemSeq.countItems(); i < n; i++) {
            gpwlmgr.addWorklistItem(wkitemSeq.getItem(i), checkPGP, checkAppend);
        }
    }

    private static boolean contains(Object[] a, Object e) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(e)) {
                return true;
            }
        }
        return false;
    }

    private GPWLManager getGPWLManager() throws Exception {
        return ((GPWLManagerHome) EJBHomeFactory.getFactory().lookup(
                GPWLManagerHome.class, GPWLManagerHome.JNDI_NAME)).create();
    }

    private void logMPPS(String aet, Dataset mpps) {
        File dir = new File(logDir, aet);
        SimpleDateFormat df = new SimpleDateFormat(LOGFILE_PATTERN);
        File f = new File(dir, df.format(new Date()));
        dir.mkdir();
        try {
            log.info("Log MPPS attributes to " + f);
            XSLTUtils.writeTo(mpps, f);
        } catch (Exception e) {
            log.warn("Log MPPS attributes failed:", e);
        }
    }
}
