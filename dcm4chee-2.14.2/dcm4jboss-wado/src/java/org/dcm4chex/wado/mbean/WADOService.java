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

package org.dcm4chex.wado.mbean;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che2.audit.message.ActiveParticipant;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.InstancesTransferredMessage;
import org.dcm4che2.audit.message.ParticipantObject;
import org.dcm4che2.audit.message.ParticipantObjectDescription;
import org.dcm4che2.audit.message.ParticipantObject.IDTypeCode;
import org.dcm4che2.audit.message.ParticipantObject.TypeCode;
import org.dcm4che2.audit.message.ParticipantObject.TypeCodeRole;
import org.dcm4che2.audit.message.ParticipantObjectDescription.SOPClass;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.dcm.mppsscp.MPPSScpService;
import org.dcm4chex.archive.mbean.HttpUserInfo;
import org.dcm4chex.archive.notif.StudyDeleted;
import org.dcm4chex.wado.common.WADORequestObject;
import org.dcm4chex.wado.common.WADOResponseObject;
import org.dcm4chex.wado.mbean.cache.WADOCacheImpl;
import org.dcm4chex.wado.mbean.factory.WADOSupportFactoryUtil;

/**
 * @author franz.willer
 * 
 * The MBean to manage the WADO service.
 * <p>
 * This class use WADOSupport for the WADO methods and WADOCache for caching jpg
 * images.
 */
public class WADOService extends AbstractCacheService {

    private static final String NONE = "NONE";
    
    //Modified by YangLin@cn-arg.com on 03.03.2009
    //Acquire WADOSupport instance according to Dicom image storage way
//  private WADOSupport support = new WADOSupport(this.server);
    private WADOSupport support = WADOSupportFactoryUtil.getWADOSupportFactory().
                                  getWADOSupport(this.server); 
    
    private final NotificationListener seriesStoredListener =
        new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            SeriesStored seriesStored = (SeriesStored) notif.getUserData();
            onSeriesStored(seriesStored);
        }
    };

    public WADOService() {
        cache = WADOCacheImpl.getWADOCache();
    }

    /**
     * @return Returns the clientRedirect.
     */
    public boolean isClientRedirect() {
        return cache.isClientRedirect();
    }

    /**
     * @param clientRedirect
     *            The clientRedirect to set.
     */
    public void setClientRedirect(boolean clientRedirect) {
        cache.setClientRedirect(clientRedirect);
    }

    /**
     * @return Returns the redirectCaching.
     */
    public boolean isRedirectCaching() {
        return cache.isRedirectCaching();
    }

    /**
     * @param redirectCaching
     *            The redirectCaching to set.
     */
    public void setRedirectCaching(boolean redirectCaching) {
        cache.setRedirectCaching(redirectCaching);
    }

    public String getImageQuality() {
        return cache.getImageQuality();
    }

    public void setImageQuality(String imageQuality) {
        cache.setImageQuality(imageQuality);
    }

    public String getImageWriterClass() {
        return cache.getImageWriterClass();
    }

    public void setImageWriterClass(String imageWriterClass) {
        cache.setImageWriterClass(imageWriterClass);
    }

    /**
     * @return Returns the useTransferSyntaxOfFileAsDefault.
     */
    public boolean isUseTransferSyntaxOfFileAsDefault() {
        return support.isUseTransferSyntaxOfFileAsDefault();
    }

    /**
     * Set default transfer syntax option.
     * <p>
     * If true use the TS from file.<br>
     * If false use Explicit VR littlle Endian (as defined in part 18)
     * 
     * @param b
     *            If true use TS from file.
     */
    public void setUseTransferSyntaxOfFileAsDefault(boolean b) {
        support.setUseTransferSyntaxOfFileAsDefault(b);
    }


    public String getSrImageRows() {
        String rows = support.getSrImageRows();
        return rows == null ? NONE : support.getSrImageRows();
    }

    public void setSrImageRows(String srImageRows) {
        support.setSrImageRows( NONE.equals(srImageRows) ? null : srImageRows );
    }
    /**
     * Set URL to XSLT stylesheet that should be used to transform DICOM SR to
     * HTML document.
     * 
     * @return
     */
    public String getHtmlXslURL() {
        return support.getHtmlXslURL();
    }

    public void setHtmlXslURL(String htmlXslURL) {
        support.setHtmlXslURL(htmlXslURL);
    }

    /**
     * Set URL to XSLT stylesheet that should be used to transform DICOM SR to
     * XHTML document.
     * 
     * @return
     */
    public String getXHtmlXslURL() {
        return support.getXHtmlXslURL();
    }

    public void setXHtmlXslURL(String htmlXslURL) {
        support.setXHtmlXslURL(htmlXslURL);
    }

    /**
     * Set URL to XSLT stylesheet that should be used to transform DICOM SR to
     * xml document.
     * 
     * @return
     */
    public String getXmlXslURL() {
        return support.getXmlXslURL();
    }

    public void setXmlXslURL(String xslURL) {
        support.setXmlXslURL(xslURL);
    }

    /**
     * Set URL to XSLT stylesheet that should be used to transform DICOM SR to
     * xml document.
     * 
     * @return
     */
    public String getDicomXslURL() {
        return support.getDicomXslURL();
    }

    public void setDicomXslURL(String xslURL) {
        support.setDicomXslURL(xslURL);
    }

    public void clearTemplateCache() {
        support.clearTemplateCache();
    }

    public String getContentTypeDicomXML() {
        return support.getContentTypeDicomXML();
    }

    public void setContentTypeDicomXML(String contentTypeDicomXML) {
        support.setContentTypeDicomXML(contentTypeDicomXML);
    }

    public String getImageSopCuids() throws Exception{
        return map2string(support.getImageSopCuids());
    }

    /**
     * Returns a String with all defined SOP Class UIDs that are used to find
     * text (SR) documents.
     * <p>
     * The uids are separated with line separator.
     * 
     * @return SOP Class UIDs to find ECG related files.
     */
    public String getTextSopCuids() {
        Map uids = support.getTextSopCuids();
        return map2string(uids);
    }

    /**
     * Set a list of SOP Class UIDs that are used to find text (SR) documents.
     * <p>
     * The UIDs are separated with line separator.
     * 
     * @param sopCuids
     *            String with SOP class UIDs separated with ';'
     */
    public void setTextSopCuids(String sopCuids) {

        support.setTextSopCuids(sopCuids);
    }

    /**
     * Returns a String with all defined SOP Class UIDs that are used to support
     * Video (mpeg2) DICOM objects.
     * <p>
     * The uids are separated with line separator.
     * 
     * @return SOP Class UIDs to find ECG related files.
     */
    public String getVideoSopCuids() {
        Map uids = support.getVideoSopCuids();
        return uids.isEmpty() ? WADOSupport.NONE : map2string(uids);
    }

    /**
     * Set a list of SOP Class UIDs that are used to support
     * Video (mpeg2) DICOM objects.
     * <p>
     * The UIDs are separated with line separator.
     * 
     * @param sopCuids
     *            String with SOP class UIDs separated with ';'
     */
    public void setVideoSopCuids(String sopCuids) {

        support.setVideoSopCuids(sopCuids);
    }

    public String getEncapsulatedSopCuids() {
        Map uids = support.getEncapsulatedSopCuids();
        return uids.isEmpty() ? WADOSupport.NONE : map2string(uids);
    }

    public void setEncapsulatedSopCuids(String sopCuids) {
        support.setEncapsulatedSopCuids(sopCuids);
    }
    
    private String map2string(Map map) {
        if (map == null || map.isEmpty())
            return "";
        StringBuffer sb = new StringBuffer(map.size() << 5);// StringBuffer
        // initial size:
        // nrOfUIDs x 32
        Iterator iter = map.keySet().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(
                    System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    /**
     * Getter for the name of the StoreScp Service Name.
     * <p>
     * This bean is used to get list of Image SOP Classs UID.
     * 
     * @return Name of the MBean
     */
    public ObjectName getStoreScpServiceName() {
        return support.getStoreScpServiceName();
    }
    public void setStoreScpServiceName(ObjectName name) {
        support.setStoreScpServiceName(name);
    }

    /**
     * Set the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @param name
     *            The Audit Logger Name to set.
     */
    public void setAuditLoggerName(ObjectName name) {
        support.setAuditLoggerName(name);
    }

    /**
     * Get the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @return Returns the name of the Audit Logger MBean.
     */
    public ObjectName getAuditLoggerName() {
        return support.getAuditLoggerName();
    }

    public ObjectName getQueryRetrieveScpName() {
        return support.getQueryRetrieveScpName();
    }

    public void setQueryRetrieveScpName(ObjectName name) {
        support.setQueryRetrieveScpName(name);
    }

    public String getDisabledAuditLogHosts() {
        Set s = support.getDisabledAuditLogHosts();
        if (s == null)
            return "ALL";
        if (s.isEmpty())
            return NONE;
        StringBuffer sb = new StringBuffer(s.size() << 4);
        for (Iterator it = s.iterator(); it.hasNext();) {
            sb.append(it.next()).append(
                    System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    public void setDisabledAuditLogHosts(String disabledAuditLogHosts) {
        if ("ALL".equals(disabledAuditLogHosts)) {
            support.setDisabledAuditLogHosts(null);
        } else {
            Set disabledHosts = new HashSet();
            if (!NONE.equals(disabledAuditLogHosts)) {
                StringTokenizer st = new StringTokenizer(disabledAuditLogHosts,
                "\r\n;");
                while (st.hasMoreTokens()) {
                    disabledHosts.add(st.nextElement());
                }
            }
            support.setDisabledAuditLogHosts(disabledHosts);
        }
    }

    public boolean isDisableDNS() {
        return support.isDisableDNS();
    }

    /**
     * @param disableDNS
     *            the disableDNS to set
     */
    public void setDisableDNS(boolean disableDNS) {
        support.setDisableDNS(disableDNS);
    }

    /**
     * Get the requested DICOM object as File packed in a WADOResponseObject.
     * <p>
     * 
     * @param reqVO
     *            The request parameters packed in an value object.
     * 
     * @return The value object containing the retrieved object or an error.
     * @throws Exception 
     */
    public WADOResponseObject getWADOObject(WADORequestObject reqVO) throws Exception {
        WADOResponseObject resp = support.getWADOObject(reqVO);
        if (support.isAuditLogEnabled(reqVO)) {
            if (support.isAuditLogIHEYr4() && resp.getPatInfo() != null) {
                support.logInstancesSent(reqVO, resp);
            } else {
                log.debug("Suppress (IHEYr4) audit log! No patient info available!");
            }
            logExport(reqVO, resp);
        } else {
            log.debug("Suppress audit log! Disabled for host:"
                    + reqVO.getRemoteHost());
        }
        return resp;
    }

    private void logExport(WADORequestObject reqObj, WADOResponseObject resp) {
        if (support.isAuditLogIHEYr4())
            return;
        try {
            HttpUserInfo userInfo = new HttpUserInfo(reqObj.getRequest(),
                    AuditMessage.isEnableDNSLookups());
            String user = userInfo.getUserId();
            String destHost = userInfo.getHostName();
            InstancesTransferredMessage msg = new InstancesTransferredMessage(
                    InstancesTransferredMessage.EXECUTE);
            msg.setOutcomeIndicator(resp.getReturnCode() == HttpServletResponse.SC_OK ? AuditEvent.OutcomeIndicator.SUCCESS
                    : AuditEvent.OutcomeIndicator.MINOR_FAILURE);
            msg.addSourceProcess(AuditMessage.getProcessID(), AuditMessage
                    .getLocalAETitles(), AuditMessage.getProcessName(),
                    AuditMessage.getLocalHostName(), false);
            ParticipantObject obj = new ParticipantObject(reqObj.getRequest()
                    .getRequestURL().toString(), IDTypeCode.URI);
            obj.setParticipantObjectTypeCode(TypeCode.SYSTEM);
            obj.setParticipantObjectTypeCodeRole(TypeCodeRole.DATA_REPOSITORY);
            msg.addParticipantObject(obj);
            msg.addDestinationProcess(destHost, null, null, destHost,
                    user == null);
            if (user != null) {
                ActiveParticipant ap = ActiveParticipant.createActivePerson(
                        user, null, user, null, true);
                msg.addActiveParticipant(ap);

            }
            Dataset ds = resp.getPatInfo();
            if (ds != null) {
                msg.addPatient(ds.getString(Tags.PatientID), ds
                        .getString(Tags.PatientName));
                ParticipantObjectDescription desc = new ParticipantObjectDescription();
                SOPClass sopClass = new SOPClass(ds.getString(Tags.SOPClassUID));
                sopClass.setNumberOfInstances(1);
                desc.addSOPClass(sopClass);
                msg.addStudy(ds.getString(Tags.StudyInstanceUID), desc);
            } else {
                msg.addPatient("unknown_patid", "unknown_pn");
                msg.addStudy(reqObj.getStudyUID(), null);
            }
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        } catch (Exception e) {
            log.warn("Audit Log failed:", e);
        }
    }

    protected void startService() throws Exception {
        server.addNotificationListener(getStoreScpServiceName(),
                seriesStoredListener, SeriesStored.NOTIF_FILTER, null);

    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(getStoreScpServiceName(),
                seriesStoredListener, SeriesStored.NOTIF_FILTER, null);
    }
    
    private void onSeriesStored(SeriesStored seriesStored) {
        Dataset ian = seriesStored.getIAN();
        String studyIUID = ian.getString(Tags.StudyInstanceUID);
        log.info("SeriesStored! remove cached entries for seriesStored:"+seriesStored);
        cache.purgeStudy(studyIUID);
    }

}
