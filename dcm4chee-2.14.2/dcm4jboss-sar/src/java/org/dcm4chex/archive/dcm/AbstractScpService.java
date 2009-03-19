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

package org.dcm4chex.archive.dcm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import javax.management.Attribute;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.Notification;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.TransformerHandler;

import org.dcm4che.auditlog.AuditLoggerFactory;
import org.dcm4che.auditlog.RemoteNode;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObject;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmService;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4che.net.PDataTF;
import org.dcm4che.net.UserIdentityNegotiator;
import org.dcm4che.server.DcmHandler;
import org.dcm4che.util.DTFormat;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.QueryMessage;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.AEManager;
import org.dcm4chex.archive.ejb.interfaces.AEManagerHome;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManager;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManagerHome;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.dcm4chex.archive.mbean.AuditLoggerDelegate;
import org.dcm4chex.archive.mbean.TemplatesDelegate;
import org.dcm4chex.archive.notif.AetChanged;
import org.dcm4chex.archive.notif.CallingAetChanged;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.XSLTUtils;
import org.jboss.logging.Logger;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 7896 $ $Date: 2008-10-16 10:02:19 +0200 (Thu, 16 Oct
 *          2008) $
 * @since 31.08.2003
 */
public abstract class AbstractScpService extends ServiceMBeanSupport {

    protected static final String ANY = "ANY";
    protected static final String CONFIGURED_AETS = "CONFIGURED_AETS";

    protected static final String NONE = "NONE";

    private static int sequenceInt = new Random().nextInt();

    protected ObjectName dcmServerName;
    protected ObjectName aeServiceName;

    protected AuditLoggerDelegate auditLogger = new AuditLoggerDelegate(this);

    protected DcmHandler dcmHandler;

    protected UserIdentityNegotiator userIdentityNegotiator;

    protected String[] calledAETs;

    /**
     * List of allowed calling AETs. <p /> <code>null</code> means ANY<br /> An
     * empty list (length=0) means CONFIGURED_AETS.
     */
    protected String[] callingAETs;

    protected String[] generatePatientID = null;

    protected String issuerOfGeneratedPatientID;

    protected boolean supplementIssuerOfPatientID;

    protected String[] generatePatientIDForUnscheduledFromAETs;

    protected boolean invertGeneratePatientIDForUnscheduledFromAETs;

    /**
     * Map containing accepted Transfer Syntax UIDs. key is name (as in config
     * string), value is real uid)
     */
    protected Map<String, String> tsuidMap = new LinkedHashMap<String, String>();

    protected int maxPDULength = PDataTF.DEF_MAX_PDU_LENGTH;

    protected int maxOpsInvoked = 1;

    protected int maxOpsPerformed = 1;

    protected String[] logCallingAETs = {};

    protected File logDir;
    private boolean writeCoercionXmlLog;

    protected TemplatesDelegate templates = new TemplatesDelegate(this);

    private static final NotificationFilterSupport callingAETsChangeFilter = new NotificationFilterSupport();
    static {
        callingAETsChangeFilter.enableType(CallingAetChanged.class.getName());
    }
    private static final NotificationFilterSupport aetChangeFilter = new NotificationFilterSupport();
    static {
        aetChangeFilter.enableType(AetChanged.class.getName());
    }

    private final NotificationListener callingAETChangeListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            try {
                CallingAetChanged userData = (CallingAetChanged) notif
                        .getUserData();
                if (areCalledAETsAffected(userData.getAffectedCalledAETs())) {
                    String[] newCallingAets = userData.getNewCallingAETs();
                    String newCallingAETs = newCallingAets == null ? ANY
                            : newCallingAets.length == 0 ? CONFIGURED_AETS
                                    : StringUtils
                                            .toString(newCallingAets, '\\');
                    log.debug("newCallingAETs:" + newCallingAETs);
                    server.setAttribute(serviceName, new Attribute(
                            "CallingAETitles", newCallingAETs));
                }
            } catch (Throwable th) {
                log.warn("Failed to process callingAET change notification: ",
                        th);
            }
        }

        private boolean areCalledAETsAffected(String[] affectedCalledAETs) {
            if (calledAETs == null)
                return true;
            if (affectedCalledAETs != null) {
                for (int i = 0; i < affectedCalledAETs.length; i++) {
                    for (int j = 0; j < calledAETs.length; j++) {
                        if (affectedCalledAETs[i].equals(calledAETs[j]))
                            return true;
                    }
                }
            }
            return false;
        }
    };
    private final NotificationListener aetChangeListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            if (callingAETs != null || callingAETs.length == 0) {
                try {
                    log.debug("Handle AE Title change notification!");
                    AetChanged userData = (AetChanged) notif.getUserData();
                    String removeAET = userData.getOldAET();
                    String addAET = userData.getNewAET();
                    AcceptorPolicy policy = dcmHandler.getAcceptorPolicy();
                    for (int i = 0; i < calledAETs.length; ++i) {
                        AcceptorPolicy policy1 = policy
                                .getPolicyForCalledAET(calledAETs[i]);
                        if (removeAET != null) {
                            policy1.removeCallingAET(removeAET);
                        }
                        if (addAET != null) {
                            policy1.addCallingAET(addAET);
                        }
                    }

                } catch (Throwable th) {
                    log.warn(
                            "Failed to process AE Title change notification: ",
                            th);
                }
            }
        }

    };

    public final ObjectName getDcmServerName() {
        return dcmServerName;
    }

    public final void setDcmServerName(ObjectName dcmServerName) {
        this.dcmServerName = dcmServerName;
    }

    public final ObjectName getAuditLoggerName() {
        return auditLogger.getAuditLoggerName();
    }

    public final void setAuditLoggerName(ObjectName auditLogName) {
        this.auditLogger.setAuditLoggerName(auditLogName);
    }

    public final ObjectName getTemplatesServiceName() {
        return templates.getTemplatesServiceName();
    }

    public final void setTemplatesServiceName(ObjectName serviceName) {
        templates.setTemplatesServiceName(serviceName);
    }

    public ObjectName getAEServiceName() {
        return aeServiceName;
    }

    public void setAEServiceName(ObjectName aeServiceName) {
        this.aeServiceName = aeServiceName;
    }

    public final String getCalledAETs() {
        return calledAETs == null ? "" : StringUtils.toString(calledAETs, '\\');
    }

    public final void setCalledAETs(String calledAETs) {
        if (getCalledAETs().equals(calledAETs))
            return;
        disableService();
        this.calledAETs = StringUtils.split(calledAETs, '\\');
        enableService();
    }

    public final String getLogCallingAETs() {
        return StringUtils.toString(logCallingAETs, '\\');
    }

    public final void setLogCallingAETs(String aets) {
        logCallingAETs = StringUtils.split(aets, '\\');
    }

    public final String getGeneratePatientID() {
        if (generatePatientID == null) {
            return NONE;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < generatePatientID.length; i++) {
            sb.append(generatePatientID[i]);
        }
        return sb.toString();
    }

    public final void setGeneratePatientID(String pattern) {
        if (pattern.equalsIgnoreCase(NONE)) {
            this.generatePatientID = null;
            return;
        }
        int pl = pattern.indexOf('#');
        int pr = pl != -1 ? pattern.lastIndexOf('#') : -1;
        int sl = pattern.indexOf('$');
        int sr = sl != -1 ? pattern.lastIndexOf('$') : -1;
        if (pl == -1 && sl == -1) {
            this.generatePatientID = new String[] { pattern };
        } else if (pl != -1 && sl != -1) {
            this.generatePatientID = pl < sl ? split(pattern, pl, pr, sl, sr)
                    : split(pattern, sl, sr, pl, pr);

        } else {
            this.generatePatientID = pl != -1 ? split(pattern, pl, pr) : split(
                    pattern, sl, sr);
        }
    }

    private static String[] split(String pattern, int l1, int r1) {
        return new String[] { pattern.substring(0, l1),
                pattern.substring(l1, r1 + 1), pattern.substring(r1 + 1), };
    }

    private static String[] split(String pattern, int l1, int r1, int l2, int r2) {
        if (r1 > l2) {
            throw new IllegalArgumentException(pattern);
        }
        return new String[] { pattern.substring(0, l1),
                pattern.substring(l1, r1 + 1), pattern.substring(r1 + 1, l2),
                pattern.substring(l2, r2 + 1), pattern.substring(r2 + 1) };
    }

    public final String getIssuerOfGeneratedPatientID() {
        return issuerOfGeneratedPatientID;
    }

    public final void setIssuerOfGeneratedPatientID(
            String issuerOfGeneratedPatientID) {
        this.issuerOfGeneratedPatientID = issuerOfGeneratedPatientID;
    }

    public final boolean isSupplementIssuerOfPatientID() {
        return supplementIssuerOfPatientID;
    }

    public final void setSupplementIssuerOfPatientID(
            boolean supplementIssuerOfPatientID) {
        this.supplementIssuerOfPatientID = supplementIssuerOfPatientID;
    }

    public final String getGeneratePatientIDForUnscheduledFromAETs() {
        return invertGeneratePatientIDForUnscheduledFromAETs ? "!\\" : ""
            + (generatePatientIDForUnscheduledFromAETs == null ? "NONE"
                    : StringUtils.toString(
                            generatePatientIDForUnscheduledFromAETs, '\\'));
    }

    public final void setGeneratePatientIDForUnscheduledFromAETs(String aets) {
        if (invertGeneratePatientIDForUnscheduledFromAETs = aets.startsWith("!\\")) {
            aets = aets.substring(2);
        }
        generatePatientIDForUnscheduledFromAETs = aets.equalsIgnoreCase("NONE")
                ? null : StringUtils.split(aets, '\\');
    }

    protected boolean isGeneratePatientIDForUnscheduledFromAET(String callingAET) {
        if (generatePatientIDForUnscheduledFromAETs != null) {
            for (String aet : generatePatientIDForUnscheduledFromAETs) {
                if (aet.equals(callingAET)) {
                    return !invertGeneratePatientIDForUnscheduledFromAETs;
                }
            }
        }
        return invertGeneratePatientIDForUnscheduledFromAETs;
    }

    public final int getMaxPDULength() {
        return maxPDULength;
    }

    public final void setMaxPDULength(int maxPDULength) {
        if (this.maxPDULength == maxPDULength)
            return;
        this.maxPDULength = maxPDULength;
        enableService();
    }

    public final int getMaxOpsInvoked() {
        return maxOpsInvoked;
    }

    public final void setMaxOpsInvoked(int maxOpsInvoked) {
        if (this.maxOpsInvoked == maxOpsInvoked)
            return;
        this.maxOpsInvoked = maxOpsInvoked;
        enableService();
    }

    public final int getMaxOpsPerformed() {
        return maxOpsPerformed;
    }

    public final void setMaxOpsPerformed(int maxOpsPerformed) {
        if (this.maxOpsPerformed == maxOpsPerformed)
            return;
        this.maxOpsPerformed = maxOpsPerformed;
        enableService();
    }

    public final String getCoerceConfigDir() {
        return templates.getConfigDir();
    }

    public final void setCoerceConfigDir(String path) {
        templates.setConfigDir(path);
    }

    public boolean isWriteCoercionXmlLog() {
        return writeCoercionXmlLog;
    }

    public void setWriteCoercionXmlLog(boolean writeCoercionXmlLog) {
        this.writeCoercionXmlLog = writeCoercionXmlLog;
    }

    protected boolean enableService() {
        if (dcmHandler == null)
            return false;
        boolean changed = false;
        String[] callingAETs = getCallingAETsForPolicy();
        AcceptorPolicy policy = dcmHandler.getAcceptorPolicy();
        for (int i = 0; i < calledAETs.length; ++i) {
            AcceptorPolicy policy1 = policy
                    .getPolicyForCalledAET(calledAETs[i]);
            if (policy1 == null) {
                policy1 = AssociationFactory.getInstance().newAcceptorPolicy();
                policy1.setCallingAETs(callingAETs);
                policy1.setUserIdentityNegotiator(userIdentityNegotiator);
                policy.putPolicyForCalledAET(calledAETs[i], policy1);
                policy.addCalledAET(calledAETs[i]);
                changed = true;
            } else {
                String[] aets = policy1.getCallingAETs();
                if (aets.length == 0) {
                    if (callingAETs != null) {
                        policy1.setCallingAETs(callingAETs);
                        changed = true;
                    }
                } else {
                    if (!haveSameItems(aets, callingAETs)) {
                        policy1.setCallingAETs(callingAETs);
                        changed = true;
                    }
                }
            }
            policy1.setMaxPDULength(maxPDULength);
            policy1.setAsyncOpsWindow(maxOpsInvoked, maxOpsPerformed);
            enablePresContexts(policy1);
        }
        return changed;
    }

    @SuppressWarnings("unchecked")
    private String[] getCallingAETsForPolicy() {
        if (callingAETs == null)
            return null;
        if (callingAETs.length != 0)
            return callingAETs;
        log.debug("Use 'CONFIGURED_AETS' for list of calling AETs");
        try {
            List<AEDTO> l = aeMgr().findAll();
            if (l.size() == 0) {
                log.warn("No AETs configured! No calling AET is allowed!");
                return callingAETs;
            }
            List<String> dicomAEs = new ArrayList<String>(l.size());
            String aet;
            for (Iterator<AEDTO> iter = l.iterator(); iter.hasNext();) {
                aet = iter.next().getTitle();
                if (aet.indexOf('^') == -1) {// filter 'HL7' AETs
                    dicomAEs.add(aet);
                }
            }
            log
                    .debug("Use 'CONFIGURED_AETS'. Current list of configured (dicom) AETs"
                            + dicomAEs);
            String[] sa = new String[dicomAEs.size()];
            return dicomAEs.toArray(sa);
        } catch (Exception e) {
            log
                    .error(
                            "Failed to query configured AETs! No calling AET is allowed!",
                            e);
            return callingAETs;
        }
    }

    // Only check if all items in o1 are also in o2! (and same length)
    // e.g. {"a","a","d"}, {"a","d","d"} will also return true!
    private boolean haveSameItems(Object[] o1, Object[] o2) {
        if (o1 == null || o2 == null || o1.length != o2.length)
            return false;
        if (o1.length == 1)
            return o1[0].equals(o2[0]);
        iloop: for (int i = 0, len = o1.length; i < len; i++) {
            for (int j = 0; j < len; j++) {
                if (o1[i].equals(o2[j]))
                    continue iloop;
            }
            return false;
        }
        return true;
    }

    private void disableService() {
        if (dcmHandler == null)
            return;
        AcceptorPolicy policy = dcmHandler.getAcceptorPolicy();
        for (int i = 0; i < calledAETs.length; ++i) {
            AcceptorPolicy policy1 = policy
                    .getPolicyForCalledAET(calledAETs[i]);
            if (policy1 != null) {
                disablePresContexts(policy1);
                if (policy1.listPresContext().isEmpty()) {
                    policy.putPolicyForCalledAET(calledAETs[i], null);
                    policy.removeCalledAET(calledAETs[i]);
                }
            }
        }
    }

    public final String getCallingAETs() {
        return callingAETs == null ? ANY
                : callingAETs.length == 0 ? CONFIGURED_AETS : StringUtils
                        .toString(callingAETs, '\\');
    }

    public final void setCallingAETs(String callingAETs)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException {
        if (getCallingAETs().equals(callingAETs))
            return;
        this.callingAETs = ANY.equalsIgnoreCase(callingAETs) ? null
                : CONFIGURED_AETS.equalsIgnoreCase(callingAETs) ? new String[0]
                        : StringUtils.split(callingAETs, '\\');
        if (enableService()) {
            server.invoke(dcmServerName, "notifyCallingAETchange",
                    new Object[] { calledAETs, this.callingAETs },
                    new String[] { String[].class.getName(),
                            String[].class.getName() });
        }
    }

    protected void updateAcceptedSOPClass(Map<String, String> cuidMap,
            String newval, DcmService scp) {
        Map<String, String> tmp = parseUIDs(newval);
        if (cuidMap.keySet().equals(tmp.keySet()))
            return;
        disableService();
        if (scp != null)
            unbindAll(valuesToStringArray(cuidMap));
        cuidMap.clear();
        cuidMap.putAll(tmp);
        if (scp != null)
            bindAll(valuesToStringArray(cuidMap), scp);
        enableService();
    }

    // protected String[] getTransferSyntaxUIDs() {
    // return valuesToStringArray(tsuids);
    // }

    protected static String[] valuesToStringArray(Map<String, String> tsuid) {
        return tsuid.values().toArray(new String[tsuid.size()]);
    }

    protected void bindAll(String[] cuids, DcmService scp) {
        if (dcmHandler == null)
            return; // nothing to do!
        DcmServiceRegistry services = dcmHandler.getDcmServiceRegistry();
        for (int i = 0; i < cuids.length; i++) {
            services.bind(cuids[i], scp);
        }
    }

    protected void unbindAll(String[] cuids) {
        if (dcmHandler == null)
            return; // nothing to do!
        DcmServiceRegistry services = dcmHandler.getDcmServiceRegistry();
        for (int i = 0; i < cuids.length; i++) {
            services.unbind(cuids[i]);
        }
    }

    public String getAcceptedTransferSyntax() {
        return toString(tsuidMap);
    }

    public void setAcceptedTransferSyntax(String s) {
        updateAcceptedTransferSyntax(tsuidMap, s);
    }

    protected void updateAcceptedTransferSyntax(Map<String, String> tsuidMap,
            String newval) {
        Map<String, String> tmp = parseUIDs(newval);
        if (tsuidMap.keySet().equals(tmp.keySet()))
            return;
        tsuidMap.clear();
        tsuidMap.putAll(tmp);
        enableService();
    }

    protected String toString(Map<String, String> uids) {
        if (uids == null || uids.isEmpty())
            return "";
        String nl = System.getProperty("line.separator", "\n");
        StringBuffer sb = new StringBuffer();
        Iterator<String> iter = uids.keySet().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(nl);
        }
        return sb.toString();
    }

    protected static Map<String, String> parseUIDs(String uids) {
        StringTokenizer st = new StringTokenizer(uids, " \t\r\n;");
        String uid, name;
        Map<String, String> map = new LinkedHashMap<String, String>();
        while (st.hasMoreTokens()) {
            uid = st.nextToken().trim();
            name = uid;

            if (isDigit(uid.charAt(0))) {
                if (!UIDs.isValid(uid))
                    throw new IllegalArgumentException("UID " + uid
                            + " isn't a valid UID!");
            } else {
                uid = UIDs.forName(name);
            }
            map.put(name, uid);
        }
        return map;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    protected void startService() throws Exception {
        logDir = new File(ServerConfigLocator.locate().getServerHomeDir(),
                "log");
        userIdentityNegotiator = (UserIdentityNegotiator) server.invoke(
                dcmServerName, "userIdentityNegotiator", null, null);
        dcmHandler = (DcmHandler) server.invoke(dcmServerName, "dcmHandler",
                null, null);
        bindDcmServices(dcmHandler.getDcmServiceRegistry());
        server.addNotificationListener(dcmServerName, callingAETChangeListener,
                callingAETsChangeFilter, null);
        server.addNotificationListener(aeServiceName, aetChangeListener,
                aetChangeFilter, null);
        enableService();
    }

    protected void stopService() throws Exception {
        disableService();
        unbindDcmServices(dcmHandler.getDcmServiceRegistry());
        dcmHandler = null;
        userIdentityNegotiator = null;
        server.removeNotificationListener(dcmServerName,
                callingAETChangeListener);
        server.removeNotificationListener(aeServiceName, aetChangeListener);
    }

    protected abstract void bindDcmServices(DcmServiceRegistry services);

    protected abstract void unbindDcmServices(DcmServiceRegistry services);

    protected abstract void enablePresContexts(AcceptorPolicy policy);

    protected abstract void disablePresContexts(AcceptorPolicy policy);

    protected void putPresContexts(AcceptorPolicy policy, String[] cuids,
            String[] tsuids) {
        for (int i = 0; i < cuids.length; i++) {
            policy.putPresContext(cuids[i], tsuids);
        }
    }

    protected void putRoleSelections(AcceptorPolicy policy, String[] cuids,
            boolean scu, boolean scp) {
        for (int i = 0; i < cuids.length; i++) {
            policy.putRoleSelection(cuids[i], scu, scp);
        }
    }

    protected void removeRoleSelections(AcceptorPolicy policy, String[] cuids) {
        for (int i = 0; i < cuids.length; i++) {
            policy.removeRoleSelection(cuids[i]);
        }
    }

    public File getLogFile(Date now, String callingAET, String suffix) {
        File dir = new File(logDir, callingAET);
        dir.mkdirs();
        return new File(dir, new DTFormat().format(now) + suffix);
    }

    private boolean contains(Object[] a, Object e) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(e)) {
                return true;
            }
        }
        return false;
    }

    public void logDIMSE(Association a, String suffix, Dataset ds) {
        String callingAET = a.getCallingAET();
        if (contains(logCallingAETs, callingAET)) {
            try {
                XSLTUtils.writeTo(ds,
                        getLogFile(new Date(), callingAET, suffix));
            } catch (Exception e) {
                log.warn("Logging of attributes failed:", e);
            }
        }
    }

    public Dataset getCoercionAttributesFor(Association a, String xsl,
            Dataset in) {
        return getCoercionAttributesFor(a, xsl, in,
                getCoercionTemplates(a, xsl));
    }

    public Templates getCoercionTemplates(Association a, String xsl) {
        return templates.getTemplatesForAET(a.getCallingAET(), xsl);
    }

    public TransformerHandler getCoercionTransformerHandler(Association a,
            String xsl) throws TransformerConfigurationException {
        Templates tpl = getCoercionTemplates(a, xsl);
        return tpl != null ? XSLTUtils.getTransformerHandler(tpl, a) : null;
    }

    public Dataset getCoercionAttributesFor(Association a, String xsl,
            Dataset in, Templates stylesheet) {
        if (stylesheet == null) {
            return null;
        }
        Dataset out = DcmObjectFactory.getInstance().newDataset();
        try {
            XSLTUtils.xslt(in, stylesheet, a, out);
            logCoercion(a, xsl, in, out);
        } catch (Exception e) {
            log.error("Attribute coercion failed:", e);
            return null;
        }
        return out;
    }

    public Dataset getCoercionAttributesFor(Association a, String xsl,
            Dataset in, TransformerHandler th) {
        if (th == null) {
            return null;
        }
        Dataset out = DcmObjectFactory.getInstance().newDataset();
        try {
            XSLTUtils.xslt(in, th, out);
            logCoercion(a, xsl, in, out);
        } catch (Exception e) {
            log.error("Attribute coercion failed:", e);
            return null;
        }
        return out;
    }

    private void logCoercion(Association a, String xsl, Dataset in, Dataset out)
            throws TransformerConfigurationException, IOException {
        if (writeCoercionXmlLog && contains(logCallingAETs, a.getCallingAET())) {
            Date now = new Date();
            XSLTUtils.writeTo(in,
                    getLogFile(now, "coercion", "." + xsl + ".in"));
            XSLTUtils.writeTo(out, getLogFile(now, "coercion", "." + xsl
                    + ".out"));
        }
    }

    public void coerceAttributes(DcmObject ds, DcmObject coerce) {
        coerceAttributes(ds, coerce, null);
    }

    @SuppressWarnings( { "fallthrough", "unchecked" })
    private void coerceAttributes(DcmObject ds, DcmObject coerce,
            DcmElement parent) {
        boolean coerced = false;
        for (Iterator<DcmElement> it = coerce.iterator(); it.hasNext();) {
            DcmElement el = it.next();
            DcmElement oldEl = ds.get(el.tag());
            if (el.isEmpty()) {
                coerced = oldEl != null && !oldEl.isEmpty();
                if (oldEl == null || coerced) {
                    ds.putXX(el.tag(), el.vr());
                }
            } else {
                Dataset item;
                DcmElement sq = oldEl;
                switch (el.vr()) {
                case VRs.SQ:
                    coerced = oldEl != null && sq.vr() != VRs.SQ;
                    if (oldEl == null || coerced) {
                        sq = ds.putSQ(el.tag());
                    }
                    for (int i = 0, n = el.countItems(); i < n; ++i) {
                        item = sq.getItem(i);
                        if (item == null) {
                            item = sq.addNewItem();
                        }
                        Dataset coerceItem = el.getItem(i);
                        coerceAttributes(item, coerceItem, el);
                        if (!coerceItem.isEmpty()) {
                            coerced = true;
                        }
                    }
                    break;
                case VRs.OB:
                case VRs.OF:
                case VRs.OW:
                case VRs.UN:
                    if (el.hasDataFragments()) {
                        coerced = true;
                        sq = ds.putXXsq(el.tag(), el.vr());
                        for (int i = 0, n = el.countItems(); i < n; ++i) {
                            sq.addDataFragment(el.getDataFragment(i));
                        }
                        break;
                    }
                    // fall through
                default:
                    coerced = oldEl != null && !oldEl.equals(el);
                    if (oldEl == null || coerced) {
                        ds.putXX(el.tag(), el.vr(), el.getByteBuffer());
                    }
                    break;
                }
            }
            if (coerced) {
                log
                        .info(parent == null ? ("Coerce " + oldEl + " to " + el)
                                : ("Coerce " + oldEl + " to " + el
                                        + " in item of " + parent));
            } else {
                if (oldEl == null && log.isDebugEnabled()) {
                    log.debug(parent == null ? ("Add " + el) : ("Add " + el
                            + " in item of " + parent));
                }
                it.remove();
            }
        }
    }

    public void sendJMXNotification(Object o) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(o.getClass().getName(), this,
                eventID);
        notif.setUserData(o);
        super.sendNotification(notif);
    }

    public void logDicomQuery(Association assoc, String cuid, Dataset keys) {
        try {
            if (auditLogger.isAuditLogIHEYr4()) {
                RemoteNode rnode = AuditLoggerFactory
                        .getInstance()
                        .newRemoteNode(assoc.getSocket(), assoc.getCallingAET());
                server.invoke(auditLogger.getAuditLoggerName(),
                        "logDicomQuery", new Object[] { keys, rnode, cuid },
                        new String[] { Dataset.class.getName(),
                                RemoteNode.class.getName(),
                                String.class.getName() });
            } else {
                QueryMessage msg = new QueryMessage();
                msg.addDestinationProcess(AuditMessage.getProcessID(),
                        calledAETs, AuditMessage.getProcessName(), AuditMessage
                                .getLocalHostName(), false);

                String srcHost = AuditMessage.hostNameOf(assoc.getSocket()
                        .getInetAddress());
                msg.addSourceProcess(srcHost, new String[] { assoc
                        .getCallingAET() }, null, srcHost, true);
                byte[] query = DatasetUtils.toByteArray(keys);
                msg.addQuerySOPClass(cuid, UIDs.ExplicitVRLittleEndian, query);
                msg.validate();
                Logger.getLogger("auditlog").info(msg);
            }
        } catch (Exception e) {
            log.warn("Audit Log failed:", e);
        }
    }

    public static String formatPN(String pname) {
        if (pname == null || pname.length() == 0) {
            return null;
        }
        return DcmObjectFactory.getInstance().newPersonName(pname).format();
    }

    public void supplementIssuerOfPatientID(Dataset ds, String callingAET) {
        if (supplementIssuerOfPatientID
                && !ds.containsValue(Tags.IssuerOfPatientID)) {
            String pid = ds.getString(Tags.PatientID);
            if (pid != null) {
                try {
                    AEDTO ae = aeMgr().findByAET(callingAET);
                    String issuer = ae.getIssuerOfPatientID();
                    ds.putLO(Tags.IssuerOfPatientID, issuer);
                    if (log.isInfoEnabled()) {
                        log.info("Add missing Issuer Of Patient ID " + issuer
                                + " for Patient ID " + pid);
                    }
                } catch (UnknownAETException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Missing AE configuration for " + callingAET
                                + " - no supplement of Issuer Of Patient ID");
                    }
                } catch (Exception e) {
                    log.warn("Failed to supplement Issuer Of Patient ID: ", e);
                }
            }
        }
    }

    public void generatePatientID(Dataset pat, Dataset sty) {
        if (generatePatientID == null) {
            return;
        }
        String pid = pat.getString(Tags.PatientID);
        if (pid != null) {
            return;
        }
        String pname = pat.getString(Tags.PatientName);
        if (generatePatientID.length == 1) {
            pid = generatePatientID[0];
        } else {
            String suid = sty != null ? sty.getString(Tags.StudyInstanceUID)
                    : null;
            int suidHash = suid != null ? suid.hashCode() : ++sequenceInt;
            // generate different Patient IDs for different studies
            // if no Patient Name
            int pnameHash = pname == null ? suidHash : pname.hashCode() * 37
                    + pat.getString(Tags.PatientBirthDate, "").hashCode();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < generatePatientID.length; i++) {
                String s = generatePatientID[i];
                int l = s.length();
                if (l == 0)
                    continue;
                char c = s.charAt(0);
                if (c != '#' && c != '$') {
                    sb.append(s);
                    continue;
                }
                String v = Long
                        .toString((c == '#' ? pnameHash : suidHash) & 0xffffffffL);
                for (int j = v.length() - l; j < 0; j++) {
                    sb.append('0');
                }
                sb.append(v);
            }
            pid = sb.toString();
        }
        pat.putLO(Tags.PatientID, pid);
        pat.putLO(Tags.IssuerOfPatientID, issuerOfGeneratedPatientID);
        if (log.isInfoEnabled()) {
            StringBuffer prompt = new StringBuffer("Generate Patient ID: ");
            prompt.append(pid);
            if (issuerOfGeneratedPatientID != null) {
                prompt.append("^^^").append(issuerOfGeneratedPatientID);
            }
            prompt.append(" for Patient: ").append(pname);
            log.info(prompt.toString());
        }
    }

    public boolean ignorePatientIDForUnscheduled(Dataset ds,
            int requestAttrsSeqTag, String callingAET) {
        String pid = ds.getString(Tags.PatientID);
        Dataset requestAttrs = ds.getItem(requestAttrsSeqTag);
        if (pid != null
                && (requestAttrs == null
                        || !requestAttrs.containsValue(Tags.SPSID))
                && isGeneratePatientIDForUnscheduledFromAET(callingAET)) {
            String issuer = ds.getString(Tags.IssuerOfPatientID);
            ds.putLO(Tags.PatientID);
            ds.remove(Tags.IssuerOfPatientID);
            if (log.isInfoEnabled()) {
                StringBuffer prompt = new StringBuffer("Ignore Patient ID: ");
                prompt.append(pid);
                if (issuer != null) {
                    prompt.append("^^^").append(issuer);
                }
                prompt.append(" for Patient: ")
                    .append(ds.getString(Tags.PatientName));
                log.info(prompt.toString());
            }
            return true;
        }
        return false;
    }

    protected AEManager aeMgr() throws Exception {
        AEManagerHome home = (AEManagerHome) EJBHomeFactory.getFactory()
                .lookup(AEManagerHome.class, AEManagerHome.JNDI_NAME);
        return home.create();
    }

    public StudyPermissionManager getStudyPermissionManager(Association a)
            throws Exception {
        StudyPermissionManager mgt = (StudyPermissionManager) a
                .getProperty(StudyPermissionManagerHome.JNDI_NAME);
        if (mgt == null) {
            mgt = ((StudyPermissionManagerHome) EJBHomeFactory.getFactory()
                    .lookup(StudyPermissionManagerHome.class,
                            StudyPermissionManagerHome.JNDI_NAME)).create();
            a.putProperty(StudyPermissionManagerHome.JNDI_NAME, mgt);
        }
        return mgt;
    }

}
