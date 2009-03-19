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

package org.dcm4chex.archive.hl7;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.DictionaryFactory;
import org.dcm4che.dict.TagDictionary;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.dcm.mppsscp.MPPSScpService;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;
import org.regenstrief.xhl7.HL7XMLWriter;
import org.regenstrief.xhl7.XMLWriter;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4934 $ $Date: 2007-08-30 10:45:46 +0200 (Thu, 30 Aug 2007) $
 * @since Oct 3, 2005
 */
public class MPPS2ORMService extends ServiceMBeanSupport implements
        NotificationListener {

    private static final String ISO_8859_1 = "ISO-8859-1";

    private static final int INIT_BUFFER_SIZE = 512;

    private ObjectName mppsScpServiceName;

    private ObjectName hl7SendServiceName;
    
    private TemplatesDelegate templates = new TemplatesDelegate(this);

    private String xslPath;

    private String sendingApplication;

    private String sendingFacility;

    private String receivingApplication;

    private String receivingFacility;
    
    private boolean enabled;

    private boolean ignoreUnscheduled;

    private boolean ignoreInProgress;

    private boolean oneORMperSPS;

    private File logDir;

    private boolean logXSLT;

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }
    
    public final String getStylesheet() {
        return xslPath;
    }

    public void setStylesheet(String path) {
        this.xslPath = path;
    }
    
    public final String getSendingApplication() {
        return sendingApplication;
    }

    public final void setSendingApplication(String sendingApplication) {
        this.sendingApplication = sendingApplication;
    }

    public final String getSendingFacility() {
        return sendingFacility;
    }

    public final void setSendingFacility(String sendingFacility) {
        this.sendingFacility = sendingFacility;
    }

    public final String getReceivingApplication() {
        return receivingApplication;
    }

    public final void setReceivingApplication(String receivingApplication) {
        this.receivingApplication = receivingApplication;
    }

    public final String getReceivingFacility() {
        return receivingFacility;
    }

    public final void setReceivingFacility(String receivingFacility) {
        this.receivingFacility = receivingFacility;
    }

    public final boolean isEnabled() {
        return enabled;
    }

    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public final boolean isIgnoreUnscheduled() {
        return ignoreUnscheduled;
    }

    public final void setIgnoreUnscheduled(boolean ignoreUnscheduled) {
        this.ignoreUnscheduled = ignoreUnscheduled;
    }

    public final boolean isIgnoreInProgress() {
        return ignoreInProgress;
    }

    public final void setIgnoreInProgress(boolean ignoreInProgress) {
        this.ignoreInProgress = ignoreInProgress;
    }

    public final boolean isOneORMperSPS() {
        return oneORMperSPS;
    }

    public final void setOneORMperSPS(boolean splitMPPS) {
        this.oneORMperSPS = splitMPPS;
    }

    public final boolean isLogXSLT() {
        return logXSLT;
    }

    public final void setLogXSLT(boolean logXSLT) {
        this.logXSLT = logXSLT;
    }

    private MPPSManagerHome getMPPSManagerHome() throws HomeFactoryException {
        return (MPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME);
    }

    public final ObjectName getMppsScpServiceName() {
        return mppsScpServiceName;
    }

    public final void setMppsScpServiceName(ObjectName mppsScpServiceName) {
        this.mppsScpServiceName = mppsScpServiceName;
    }

    public final ObjectName getHl7SendServiceName() {
        return hl7SendServiceName;
    }

    public final void setHl7SendServiceName(ObjectName hl7SendServiceName) {
        this.hl7SendServiceName = hl7SendServiceName;
    }

    protected void startService() throws Exception {
        server.addNotificationListener(mppsScpServiceName, this,
                MPPSScpService.NOTIF_FILTER, null);
        logDir = new File(ServerConfigLocator.locate().getServerHomeDir(),
                "log");
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(mppsScpServiceName, this,
                MPPSScpService.NOTIF_FILTER, null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.management.NotificationListener#handleNotification(javax.management.Notification,
     *      java.lang.Object)
     */
    public void handleNotification(Notification notif, Object handback) {
        if (!enabled) {
            return;
        }
        Dataset mpps = (Dataset) notif.getUserData();
        if (ignoreInProgress
                && "IN PROGRESS".equals(mpps.getString(Tags.PPSStatus)))
            return;

        final String iuid = mpps.getString(Tags.SOPInstanceUID);
        mpps = getMPPS(iuid);
        DcmElement sq = mpps.get(Tags.ScheduledStepAttributesSeq);
        if (sq == null || sq.isEmpty()) {
            log
                    .error("Missing Scheduled Step Attributes Seq in MPPS - "
                            + iuid);
            return;
        }
        if (ignoreUnscheduled
                && sq.getItem().getString(Tags.AccessionNumber) == null) {
            return;
        }
        if (oneORMperSPS) {
            for (int i = 0, n = sq.countItems(); i < n; i++) {
                mpps.putSQ(Tags.ScheduledStepAttributesSeq).addItem(
                        sq.getItem(i));
                scheduleORM(makeORM(mpps));
            }
        } else {
            scheduleORM(makeORM(mpps));
        }
    }

    private void scheduleORM(byte[] bs) {
        if (bs == null)
            return;
        try {
            server.invoke(hl7SendServiceName, "forward", new Object[] { bs },
                    new String[] { byte[].class.getName() });
        } catch (Exception e) {
            log.error("Failed to schedule ORM", e);
        }
    }

    private byte[] makeORM(Dataset mpps) {
        if (mpps == null)
            return null;
        try {
            if (logXSLT)
                try {
                    logXSLT(mpps);
                } catch (Exception e) {
                    log.warn("Failed to log XSLT:", e);
                }
            ByteArrayOutputStream out = new ByteArrayOutputStream(
                    INIT_BUFFER_SIZE);
            TransformerHandler th = getTransformerHandler();
            XMLWriter xmlWriter = new HL7XMLWriter(
            		new OutputStreamWriter(out, ISO_8859_1));
            th.setResult(new SAXResult(xmlWriter.getContentHandler()));
            mpps.writeDataset2(th, null, null, 64, null);
            log.info(new String(out.toByteArray()));
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to convert MPPS to ORM", e);
            log.error(mpps);
            return null;
        }
    }

    private void logXSLT(Dataset mpps) throws Exception {
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
                .newInstance();
        String uid = mpps.getString(Tags.SOPInstanceUID);
        logXSLT(mpps, tf.newTransformerHandler(), new File(logDir, "mpps-"
                + uid + ".xml"));
        logXSLT(mpps, getTransformerHandler(), new File(logDir, "mpps-" + uid
                + ".orm.xml"));
    }

    private void logXSLT(Dataset mpps, TransformerHandler th, File logFile)
            throws Exception {
        TagDictionary dict = DictionaryFactory.getInstance()
                .getDefaultTagDictionary();
        FileOutputStream out = new FileOutputStream(logFile);
        try {
            th.setResult(new StreamResult(out));
            mpps.writeDataset2(th, dict, null, 64, null);
        } finally {
            out.close();
        }
    }

    private TransformerHandler getTransformerHandler() throws Exception {
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
                .newInstance();
        File xslFile = FileUtils.toExistingFile(xslPath);
        TransformerHandler th = tf.newTransformerHandler(
                templates.getTemplates(xslFile));
        Transformer t = th.getTransformer();
        t.setParameter("SendingApplication", sendingApplication);
        t.setParameter("SendingFacility", sendingFacility);
        t.setParameter("ReceivingApplication", receivingApplication);
        t.setParameter("ReceivingFacility", receivingFacility);
        return th;
    }

    private Dataset getMPPS(String iuid) {
        try {
            MPPSManager mgr = getMPPSManagerHome().create();
            try {
                return mgr.getMPPS(iuid);
            } finally {
                try {
                    mgr.remove();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            log.error("Failed to load MPPS - " + iuid, e);
            return null;
        }
    }
}
