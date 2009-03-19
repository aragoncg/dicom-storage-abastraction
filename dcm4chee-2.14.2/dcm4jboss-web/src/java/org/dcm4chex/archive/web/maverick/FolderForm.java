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

package org.dcm4chex.archive.web.maverick;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.model.StudyFilterModel;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7916 $ $Date: 2008-11-05 11:16:10 +0100 (Wed, 05 Nov 2008) $
 * @since 28.01.2004
 */
public class FolderForm extends BasicFolderForm {

    public static final String FOLDER_ATTRNAME = "folderFrom";

    private String patientID;

    private String patientName;

    private String accessionNumber;

    private String studyID;

    private String studyUID;

    private String seriesUID;

    private String studyDateRange;

    private String modality;

    private StudyFilterModel studyFilter = null;

    private List aets;

    private Map grantedStudyActions;

    private String destination;

    private boolean webViewer;
    private String webViewerWindowName = "webView";
    private boolean xdsConsumer;

    /** Base URL for WADO service. Used for image view */
    private String wadoBaseURL;

    private boolean showStudyIUID;

    private boolean showSeriesIUID;

    private boolean filterAET = false;

    private boolean noMatchForNoValue = false;
    
    private boolean showIssuerOfPID;

    protected static Logger log = Logger.getLogger(FolderForm.class);

    public static FolderForm getFolderForm(ControllerContext ctx) {
        HttpServletRequest request = ctx.getRequest();
        FolderForm form = (FolderForm) request.getSession()
        .getAttribute(FOLDER_ATTRNAME);
        if (form == null) {
            form = new FolderForm(request);
            String wadoBase = ctx.getServletConfig().getInitParameter("wadoBaseURL");
            try {
                URL wadoURL = null;
                if ( wadoBase != null ) {
                    try {
                        wadoURL = new URL(wadoBase);
                    } catch (MalformedURLException x){
                        log.warn("Invalid Servlet Init Parameter wadoBaseURL:"+wadoBase+"! Ignored");
                    }
                }
                if ( wadoURL == null ) {
                    wadoURL = new URL( request.isSecure() ? "https" : "http", request.getServerName(),
                            request.getServerPort(), "/");
                }
                form.setWadoBaseURL( wadoURL.toString() );
            } catch (MalformedURLException e) {
                log.error("Init Parameter 'wadoBaseURL' is invalid:"+wadoBase);
            }
            checkWebViewer(ctx, form);
            checkXDSQuery(ctx, form);
            request.getSession().setAttribute(FOLDER_ATTRNAME, form);
            initLimit(ctx.getServletConfig().getInitParameter("limitNrOfStudies"), form);
            form.setShowIssuerOfPID("true".equals(ctx.getServletConfig().getInitParameter("showIssuerOfPID")));
            String defaultQueryHasIssuer = ctx.getServletConfig().getInitParameter("defaultQueryHasIssuer");
            if ( "true".equals(defaultQueryHasIssuer)) {
                form.setHideHasNoIssuerOfPID(true);
            } else if ("false".equals(defaultQueryHasIssuer)) {
                form.setHideHasIssuerOfPID(true);
            }
            try {
                ObjectName qrscpServiceName = new ObjectName(ctx.getServletConfig().getInitParameter("qrscpServiceName"));
                MBeanServer server = MBeanServerLocator.locate();
                Boolean b = (Boolean) server.getAttribute(qrscpServiceName,"NoMatchForNoValue");
                form.setNoMatchForNoValue(b.booleanValue());

            }catch (Exception x) {
                log.warn("Cant initialize noMatchForNoValue! set to true (NON standard conform!!!)");
                form.setNoMatchForNoValue(true);
            }
        }
        initLimit(request.getParameter("limitNrOfStudies"), form);
        form.clearPopupMsg();

        return form;
    }

    private static void checkWebViewer(ControllerContext ctx, FolderForm form) {
        try {
            ObjectName webviewServiceName = new ObjectName(ctx.getServletConfig().getInitParameter("webviewServiceName"));
            MBeanServer server = MBeanServerLocator.locate();
            if ( server.isRegistered(webviewServiceName) ) {
                log.info("Webviewer is enabled!");
                form.enableWebViewer();
                form.setWebViewerWindowName(ctx.getServletConfig().getInitParameter("webViewerWindowName"));
            } else {
                log.debug("Webviewer is disabled!");
            }
        } catch (Exception ignore) {
            log.debug("Failure while check if Webviewer Service is available! Disabled!",ignore);
        }
    }

    private static void checkXDSQuery(ControllerContext ctx, FolderForm form) {
        try {
            ObjectName name = new ObjectName(ctx.getServletConfig().getInitParameter("xdsQueryServiceName"));
            MBeanServer server = MBeanServerLocator.locate();
            if ( server.isRegistered(name) ) {
                log.info("XDS-I Consumer is enabled!");
                form.enableXDSConsumer();
            } else {
                log.debug("XDS-I Consumer is disabled!");
            }
        } catch (Exception ignore) {
            log.debug("Failure while check if XDSQuery Service is available! Disabled!",ignore);
        }
    }

    private FolderForm( HttpServletRequest request ) {
        super(request);
    }

    public String getModelName() { return "FOLDER"; }


    /**
     * 
     */
    private void enableWebViewer() {
        webViewer = true;

    }
    /**
     * @return Returns the webViewer.
     */
    public boolean isWebViewer() {
        return webViewer;
    }

    public String getWebViewerWindowName() {
        return webViewerWindowName;
    }

    public void setWebViewerWindowName(String webViewerWindowName) {
        this.webViewerWindowName = webViewerWindowName;
    }

    private void enableXDSConsumer() {
        xdsConsumer = true;

    }
    public boolean isXDSConsumer() {
        return xdsConsumer;
    }

    /**
     * @return Returns the wadoBaseURL.
     */
    public String getWadoBaseURL() {
        return wadoBaseURL;
    }
    /**
     * @param wadoBaseURL The wadoBaseURL to set.
     */
    public void setWadoBaseURL(String wadoBaseURL) {
        this.wadoBaseURL = wadoBaseURL;
    }

    public final String getAccessionNumber() {
        return accessionNumber;
    }

    public final void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public final String getModality() {
        return modality;
    }

    public final void setModality(String modality) {
        this.modality = modality;
    }

    public final String getPatientID() {
        return patientID;
    }

    public final void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public final String getPatientName() {
        return patientName;
    }

    public final void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public final String getStudyDateRange() {
        return studyDateRange;
    }

    public final void setStudyDateRange(String studyDateRange) {
        this.studyDateRange = studyDateRange;
    }

    public final String getStudyID() {
        return studyID;
    }

    public final void setStudyID(String studyID) {
        this.studyID = studyID;
    }

    /**
     * @return Returns the studyUID.
     */
    public String getStudyUID() {
        return studyUID;
    }
    /**
     * @param studyUID The studyUID to set.
     */
    public void setStudyUID(String studyUID) {
        this.studyUID = studyUID;
        if ( studyUID != null && studyUID.trim().length() > 0 ) {
            patientID = "";
            patientName = "";
            accessionNumber = "";
            studyID = "";
            seriesUID = "";
            studyDateRange = "";
            modality = "";
            filterAET = false;
            this.showStudyIUID = true;
        }
    }

    public String getSeriesUID() {
        return seriesUID;
    }
    /**
     * @param studyUID The studyUID to set.
     */
    public void setSeriesUID(String seriesUID) {
        this.seriesUID = seriesUID;
        if ( seriesUID != null && seriesUID.trim().length() > 0 ) {
            patientID = "";
            patientName = "";
            accessionNumber = "";
            studyID = "";
            studyUID = "";
            studyDateRange = "";
            modality = "";
            filterAET = false;
            this.showSeriesIUID = true;
        }
    }

    public final List getAets() {
        return aets;
    }

    public final void setAets(List aets) {
        this.aets = aets;
    }

    public final Map getGrantedStudyActions() {
        return this.grantedStudyActions;
    }
    public final void setGrantedStudyActions(Map granted) {
        this.grantedStudyActions = granted;
    }

    public final String getDestination() {
        return destination;
    }

    public final void setDestination(String destination) {
        this.destination = destination;
    }

    public final void setFilter(String filter) {
        resetOffset();
        studyFilter = null;
    }

    public final StudyFilterModel getStudyFilter() {
        if (studyFilter == null) {
            studyFilter = new StudyFilterModel();
            studyFilter.setPatientID(patientID);
            studyFilter.setPatientName(patientName);
            studyFilter.setAccessionNumber(accessionNumber);
            studyFilter.setStudyID(studyID);
            studyFilter.setStudyUID( studyUID );
            studyFilter.setSeriesUID( seriesUID );
            studyFilter.setStudyDateRange(studyDateRange);
            studyFilter.setModality(modality);
            studyFilter.setCallingAET(this.filterAET ? destination:null);
        }
        return studyFilter;
    }

    /**
     * @param b
     */
    public void setShowStudyIUID(boolean b) {
        showStudyIUID = b;

    }
    /**
     * @return Returns the showStudyIUID.
     */
    public boolean isShowStudyIUID() {
        return showStudyIUID;
    }
    /**
     * @param b
     */
    public void setShowSeriesIUID(boolean b) {
        showSeriesIUID = b;

    }
    /**
     * @return Returns the showStudyIUID.
     */
    public boolean isShowSeriesIUID() {
        return showSeriesIUID;
    }


    /**
     * @return Returns the filterAET.
     */
    public boolean isFilterAET() {
        return filterAET;
    }
    /**
     * @param filterAET The filterAET to set.
     */
    public void setFilterAET(boolean filterAET) {
        this.filterAET = filterAET;
    }
    /**
     * @return Returns the noMatchForNoValue.
     */
    public boolean isNoMatchForNoValue() {
        return noMatchForNoValue;
    }
    /**
     * @param noMatchForNoValue The noMatchForNoValue to set.
     */
    public void setNoMatchForNoValue(boolean noMatchForNoValue) {
        this.noMatchForNoValue = noMatchForNoValue;
    }

    public boolean isShowIssuerOfPID() {
        return showIssuerOfPID;
    }

    public void setShowIssuerOfPID(boolean showIssuerOfPID) {
        this.showIssuerOfPID = showIssuerOfPID;
    }

    /* (non-Javadoc)
     * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
     */
    public void gotoCurrentPage() {
        //We doesnt need this method here. FolderSubmitCtrl does not use performPrevious/performNext!
    }

    /**
     * @param ctx
     * @param string
     */
    public static void setExternalPopupMsg(ControllerContext ctx, String msgId, String[] args) {
        getFolderForm(ctx).setExternalPopupMsg(msgId, args);
    }

    public boolean hasPermission(String suid, String action) {
        if ( grantedStudyActions == null ) return false;
        Collection l = (Collection) this.grantedStudyActions.get(suid);
        log.info("hasPermission: studyIUID:"+suid+" actions:"+l);
        return l == null ? false : l.contains(action);
    }
}