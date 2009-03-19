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

package org.dcm4chex.rid.mbean;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.ejb.FinderException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.Driver;
import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.DictionaryFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.DataSource;
import org.dcm4che.util.ISO8601DateFormat;
import org.dcm4che2.audit.message.ActiveParticipant;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.DataExportMessage;
import org.dcm4che2.audit.message.ParticipantObjectDescription;
import org.dcm4che2.audit.message.ParticipantObjectDescription.SOPClass;
import org.dcm4chee.docstore.Availability;
import org.dcm4chee.docstore.BaseDocument;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.jdbc.RetrieveStudyDatesCmd;
import org.dcm4chex.archive.mbean.HttpUserInfo;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileDataSource;
import org.dcm4chex.rid.common.RIDRequestObject;
import org.dcm4chex.rid.common.RIDResponseObject;
import org.dcm4chex.rid.mbean.xml.IHEDocumentList;
import org.jboss.mx.util.MBeanServerLocator;
import org.xml.sax.SAXException;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class RIDSupport {
    private static final String CARDIOLOGY = "Cardiology";
    private static final String RADIOLOGY = "Radiology";
    private static final String ENCAPSED_PDF_CARDIOLOGY = "encapsed_PDF_Cardiology";
    private static final String ENCAPSED_PDF_RADIOLOGY = "encapsed_PDF_Radiology";
    public static final String ENCAPSED_PDF_ECG = "encapsed_PDF_ECG";

    public static final String SUMMARY = "SUMMARY";
    public static final String SUMMARY_RADIOLOGY = "SUMMARY-RADIOLOGY";
    public static final String SUMMARY_CARDIOLOGY = "SUMMARY-CARDIOLOGY";
    public static final String SUMMARY_CARDIOLOGY_ECG = "SUMMARY-CARDIOLOGY-ECG";
    public static final String SUMMARY_LABORATORY = "SUMMARY-LABORATORY";
    public static final String SUMMARY_SURGERY = "SUMMARY-SURGERY";
    public static final String SUMMARY_EMERGENCY = "SUMMARY-EMERGENCY";
    public static final String SUMMARY_DISCHARGE = "SUMMARY-DISCHARGE";
    public static final String SUMMARY_ICU = "SUMMARY-ICU";//Intensive Care Report
    public static final String SUMMARY_RX = "SUMMARY-RX";//Prescription Report
    
    public static final String CONTENT_TYPE_XHTML = "text/xhtml";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_JPEG = "image/jpeg";
    public static final String CONTENT_TYPE_PDF = "application/pdf";
    public static final String CONTENT_TYPE_DICOM = "application/dicom";

    public static final String CUID_GRP_SR = "SR";
    public static final String CUID_GRP_ECG = "ECG";
    public static final String CUID_GRP_DOC = "DOC";
    
    private static Logger log = Logger.getLogger( RIDSupport.class.getName() );
    private static final DcmObjectFactory factory = DcmObjectFactory.getInstance();

    private static final String FOBSR_XSL_URI = "resource:xsl/fobsr.xsl";
    private final Driver fop = new Driver();

    private static MBeanServer server;
    private Map<String, String> ecgSopCuids = new TreeMap<String, String>();
    private Map<String, String> srSopCuids = new TreeMap<String, String>();
    private Map<String, String> docSopCuids = new TreeMap<String, String>();
    private Map<String, String> cuidsForSummaryAll;
    
    private Map<String, Map<String, String>> sopCuidGroups = new HashMap<String, Map<String, String>>();
    
    private Map<String, String> docTitlePatterns;
    private Map<String, String> summaryTitles;
    
    private String ridSummaryXsl;

    private boolean useXSLInstruction;
    private String wadoURL;

    private ObjectName queryRetrieveScpName;
    private ObjectName auditLogName;
    private Boolean auditLogIHEYr4;

    private ECGSupport ecgSupport = null;

    private Dataset patientDS = null;
    private RIDService service;
    private ConceptNameCodeConfig conceptNameCodeConfig = new ConceptNameCodeConfig();

    private boolean useOrigFile = false;
    private RIDStorageDelegate storage;
    private String srImageRows;
    private String codeCfgString;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    public RIDSupport( RIDService service ) {
        this.service = service;
        RIDSupport.server = service.getServer();
        if ( server == null ) {
            server = MBeanServerLocator.locate();
        }
    }

    private ECGSupport getECGSupport() {
        if ( ecgSupport == null )
            ecgSupport = new ECGSupport( this );
        return ecgSupport;
    }

    /**
     * @return Returns the conceptNameCodeConfig.
     */
    public ConceptNameCodeConfig getConceptNameCodeConfig() {
        return conceptNameCodeConfig;
    }
    public Map<String, String> getDocTitlePatterns() {
        return docTitlePatterns;
    }

    public void setDocTitlePatterns(Map<String,String> docTitlePatterns) {
        this.docTitlePatterns = docTitlePatterns;
    }

    /**
     * @return Returns the sopCuids.
     */
    public Map getECGSopCuids() {
        if ( ecgSopCuids == null ) setDefaultECGSopCuids();
        return ecgSopCuids;
    }
    /**
     * @param sopCuids The sopCuids to set.
     */
    public void setECGSopCuids(Map sopCuids) {
        if ( sopCuids != null && ! sopCuids.isEmpty() )
            ecgSopCuids = sopCuids;
        else {
            setDefaultECGSopCuids();
        }
        sopCuidGroups.put(CUID_GRP_ECG, ecgSopCuids);
    }

    /**
     * 
     */
    private void setDefaultECGSopCuids() {
        ecgSopCuids.clear();
        ecgSopCuids.put( "TwelveLeadECGWaveformStorage", UIDs.TwelveLeadECGWaveformStorage );
        ecgSopCuids.put( "GeneralECGWaveformStorage", UIDs.GeneralECGWaveformStorage );
        ecgSopCuids.put( "AmbulatoryECGWaveformStorage", UIDs.AmbulatoryECGWaveformStorage );
        ecgSopCuids.put( "HemodynamicWaveformStorage", UIDs.HemodynamicWaveformStorage );
        ecgSopCuids.put( "CardiacElectrophysiologyWaveformStorage", UIDs.CardiacElectrophysiologyWaveformStorage );
    }

    /**
     * @return Returns the sopCuids.
     */
    public Map getSRSopCuids() {
        return srSopCuids;
    }
    /**
     * @param sopCuids The sopCuids to set.
     */
    public void setSRSopCuids(Map cuids) {
        if ( cuids != null && ! cuids.isEmpty() ) {
            srSopCuids = cuids;
            sopCuidGroups.put(CUID_GRP_SR, srSopCuids);
        }
    }

    /**
     * @return Returns the sopCuids.
     */
    public Map<String, String> getEncapsulatedDocumentSopCuids() {
        return docSopCuids;
    }
    /**
     * @param sopCuids The sopCuids to set.
     */
    public void setEncapsulatedDocumentSopCuids(Map<String, String> cuids) {
        if ( cuids == null ) {
            docSopCuids.clear();
        } else {
            docSopCuids = cuids;
        }
        sopCuidGroups.put(CUID_GRP_DOC, docSopCuids);
    }

    public Map<String, Map<String, String>> getSopCuidGroups() {
        return sopCuidGroups;
    }

    public void updateSopCuidGroups(Map<String, Map<String, String>> grps) {
        this.sopCuidGroups.putAll(grps);
    }

    public Map<String, String> getCuidsForSummaryAll() {
        return cuidsForSummaryAll;
    }

    public void setCuidsForSummaryAll(Map<String, String> cuidsForSummaryAll) {
        this.cuidsForSummaryAll = cuidsForSummaryAll;
    }

    public Map<String, String> getSummaryTitles() {
        if ( summaryTitles == null ) {
            setDefaultSummaryTitles();
        }
        return summaryTitles;
    }

    public void setSummaryTitles(Map<String, String> summaryTitles) {
        this.summaryTitles = summaryTitles;
    }

    /**
     * @return Returns the wadoURL.
     */
    public String getWadoURL() {
        return wadoURL;
    }
    /**
     * @param wadoURL The wadoURL to set.
     */
    public void setWadoURL(String wadoURL) {
        this.wadoURL = wadoURL;
    }
    /**
     * @return Returns the ridSummaryXsl.
     */
    public String getRIDSummaryXsl() {
        return ridSummaryXsl;
    }
    /**
     * @param ridSummaryXsl The ridSummaryXsl to set.
     */
    public void setRIDSummaryXsl(String ridSummaryXsl) {
        this.ridSummaryXsl = ridSummaryXsl;
    }
    /**
     * @return Returns the useXSLInstruction.
     */
    public boolean isUseXSLInstruction() {
        return useXSLInstruction;
    }
    /**
     * @param useXSLInstruction The useXSLInstruction to set.
     */
    public void setUseXSLInstruction(boolean useXSLInstruction) {
        this.useXSLInstruction = useXSLInstruction;
    }

    /**
     * @return Returns the useOrigFile.
     */
    public boolean isUseOrigFile() {
        return useOrigFile;
    }
    /**
     * @param useOrigFile The useOrigFile to set.
     */
    public void setUseOrigFile(boolean useOrigFile) {
        this.useOrigFile = useOrigFile;
    }

    public String getSrImageRows() {
        return srImageRows;
    }

    public void setSrImageRows(String srImageRows) {
        if ( srImageRows != null )
            Integer.parseInt(srImageRows);
        this.srImageRows = srImageRows;
    }

    public ObjectName getQueryRetrieveScpName() {
        return queryRetrieveScpName;
    }

    public void setQueryRetrieveScpName(ObjectName name) {
        this.queryRetrieveScpName = name;
    }

    protected MBeanServer getMBeanServer() {
        return server;
    }

    /**
     * Set the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @param name The Audit Logger Name to set.
     */
    public void setAuditLoggerName(ObjectName name) {
        auditLogName = name;
    }
    /**
     * Get the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @return Returns the name of the Audit Logger MBean.
     */
    public ObjectName getAuditLoggerName() {
        return auditLogName;
    }


    /**
     * @param reqObj
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerConfigurationException
     */
    public RIDResponseObject getRIDSummary(RIDRequestObject reqObj) throws SQLException, IOException, TransformerConfigurationException, SAXException {
        String contentType = checkContentType( reqObj, new String[]{CONTENT_TYPE_HTML,CONTENT_TYPE_XML } );
        if ( contentType == null ) {
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_NOT_ACCEPTABLE, "Client doesnt support text/xml, text/html or text/xhtml !");
        }
        String reqType = reqObj.getRequestType();
        if (log.isDebugEnabled() ) log.debug(" Summary request type:"+reqObj.getRequestType());
        String[] pat = splitPatID( reqObj.getParam( "patientID" ));
        Dataset patDS = null;
        try {
            patDS = this.getContentManager().getPatientByID(pat[0], pat[1]);
        } catch (Exception ignore) {
        }
        if ( patDS == null )
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_NOT_FOUND, "Patient with patientID="+reqObj.getParam("patientID")+ " not found!");
        IHEDocumentList docList= new IHEDocumentList();
        initDocList( docList, reqObj, patDS );
        try {
            Map<String, List<Dataset>> conceptNames = this.conceptNameCodeConfig.getConceptNameCodes(reqType);
            if ( conceptNames == null ) {
                log.warn("No conceptCodeNames for info request type!:"+reqType);
            } else {
                fillDocList( pat[0], pat[1], docList, conceptNames);
            }
            if ( useXSLInstruction ) docList.setXslFile( ridSummaryXsl );

            if ( SUMMARY_CARDIOLOGY_ECG.equals( reqType ) ) {
                fillDocListWithCuids( pat[0], pat[1], docList, ecgSopCuids, null);
                return new RIDTransformResponseObjectImpl(docList, CONTENT_TYPE_XML, HttpServletResponse.SC_OK, null);
            } else if ( SUMMARY.equals( reqType ) && cuidsForSummaryAll != null) {
                if (log.isDebugEnabled()) log.debug("Add SOP Class UIDs for SUMMARY (all):"+cuidsForSummaryAll.keySet());
                fillDocListWithCuids( pat[0], pat[1], docList, cuidsForSummaryAll, null);
            }
            if ( docTitlePatterns != null) {
                String pattern = docTitlePatterns.get(ConceptNameCodeConfig.getSummaryID(reqType));
                if ( pattern != null ) {
                    fillDocListWithCuids( pat[0], pat[1], docList, docSopCuids, pattern);
                }
            }
            if ( docList.size() < 1 ) {
                log.info("No documents found: patientDS:");log.info(patientDS);
                if ( patientDS != null ) {
                    PersonName pn = patientDS.getPersonName(Tags.PatientName );
                    if ( pn != null ) {
                        log.info("family:"+ pn.get( PersonName.FAMILY ));
                        log.info("givenName:"+ pn.get( PersonName.GIVEN ));
                    }
                    docList.setQueryDS( patientDS );
                }
            }

            if ( ! contentType.equals(CONTENT_TYPE_XML) ) { // transform to (x)html only if client supports (x)html.
                docList.setXslt( ridSummaryXsl );
            }
            if (log.isDebugEnabled()) log.debug("ContentType:"+contentType);
            return new RIDTransformResponseObjectImpl(docList, contentType, HttpServletResponse.SC_OK, null);
        } catch ( Exception x ) {
            log.error("Building RID Summary failed!", x);
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Creation of Summary failed! Reason:"+x.getMessage());
        }
    }

    /**
     * Checks if one of the given content types are allowed.
     * <p>
     * 
     * 
     * @param reqObj	The RID request object.
     * @param types		Array of content types that can be used.
     * 
     * @return The content type that is allowed by the request or null.
     */
    protected String checkContentType(RIDRequestObject reqObj, String[] types) {
        List<String> allowed = reqObj.getAllowedContentTypes();
        if ( log.isDebugEnabled() ) log.debug(" check against:"+allowed);
        if ( allowed == null ) {
            log.debug("No accept header field! use content type:"+types[0]);
            return types[0];
        }
        String s;
        for ( int i = 0, len = types.length ; i < len ; i++ ) {
            s = types[i];
            if ( log.isDebugEnabled() ) log.debug(" check "+s+":"+allowed.contains( s )+" ,"+s.substring( 0, s.indexOf( "/") )+"/*: "+allowed.contains( s.substring( 0, s.indexOf( "/") )+"/*" ) );
            if ( allowed.contains( s ) || allowed.contains( s.substring( 0, s.indexOf( "/") )+"/*" ) ) {
                return s;
            }
        }
        if ( log.isDebugEnabled() ) log.debug(" check */*:"+allowed.contains("*/*") );
        if ( allowed.contains("*/*") ) {
            return types[0];
        }
        return null;
    }

    private void initDocList( IHEDocumentList docList, RIDRequestObject reqObj, Dataset patDS ) {
        String reqURL = reqObj.getRequestURL();
        docList.setReqURL(reqURL);
        docList.setDocSopCuids(this.docSopCuids.values());
        int pos = reqURL.indexOf('?');
        if ( pos != -1 ) reqURL = reqURL.substring(0, pos);
        docList.setDocRIDUrl( reqURL.substring( 0, reqURL.lastIndexOf("/") ) );//last path element should be the servlet name! 
        docList.setQueryDS( patDS );
        String docCode = reqObj.getRequestType();
        docList.setDocCode( docCode );
        docList.setDocDisplayName( getSummaryTitles().get(ConceptNameCodeConfig.getSummaryID(docCode)));
        String mrr = reqObj.getParam("mostRecentResults");
        if (mrr != null) {
            docList.setMostRecentResults(Integer.parseInt(mrr));
        }
        Date ldt = toDate(reqObj.getParam("lowerDateTime"));
        if (ldt != null) {
            docList.setLowerDateTime(ldt);
        }
        Date udt = toDate(reqObj.getParam("upperDateTime"));
        if (udt != null) {
            docList.setUpperDateTime(udt);
        }
    }
    
    private void setDefaultSummaryTitles() {
        summaryTitles = new HashMap<String, String>();
        summaryTitles.put("A", "List of all reports" );
        summaryTitles.put("R", "List of radiology reports" );
        summaryTitles.put("C", "List of cardiology reports" );
        summaryTitles.put("ECG", "List of ECG" );
        summaryTitles.put("L", "List of laboratory reports" );
        summaryTitles.put("S", "List of surgery reports" );
        summaryTitles.put("E", "List of emergency reports" );
        summaryTitles.put("D", "List of discharge reports" );
        summaryTitles.put("I", "List of intensive care reports" );
        summaryTitles.put("P", "List of prescription reports" );
    }
    

    private void fillDocList( String pid, String issuer, IHEDocumentList docList, Map<String, List<Dataset>> conceptCodes ) throws RemoteException, FinderException, Exception {
        Collection<String> cuids = null, allCuids = null;
        for ( Map.Entry entry : conceptCodes.entrySet() ) {
            if (entry.getKey() == null) {
                if ( allCuids == null ) {
                    allCuids = new HashSet<String>();
                    for ( Map<String, String> map : sopCuidGroups.values()) {
                        allCuids.addAll(map.values());
                    }
                }
                cuids = allCuids;
            } else {
                Map<String, String> map = sopCuidGroups.get(entry.getKey());
                if ( map == null || map.isEmpty() ) {
                    log.warn("SOPClassUID Group not defined or empty! cuidGrpId:"+entry.getKey());
                    continue;
                }
                cuids = map.values();
            }
            List docs = getContentManager().listInstanceInfosByPatientAndSRCode(pid, issuer, 
                    (List<Dataset>)entry.getValue(), cuids);
            for ( Iterator iter = docs.iterator() ; iter.hasNext() ; ) {
                docList.add( (Dataset) iter.next() );
            }
        }
    }

    private void fillDocListWithCuids( String pid, String issuer, IHEDocumentList docList, Map<String, String> cuids, String docTitlePattern ) throws RemoteException, FinderException, Exception {
        List docs = getContentManager().listInstanceInfosByPatientAndSRCode(pid, issuer, null, cuids.values());
        for ( Dataset ds : (List<Dataset>) docs ) {
            if ( docTitlePattern == null) {
                docList.add( ds );
            } else {
                String code = ds.getString(Tags.DocumentTitle);
                if (code != null && code.matches(docTitlePattern)) {
                    docList.add( ds );
                }
            }
            
        }
    }
    
    static Date toDate(String dt) {
        if (dt == null || dt.length() == 0)
            return null;
        try {
            return new ISO8601DateFormat().parse( dt );
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * @param reqObj
     * @return
     */
    public RIDResponseObject getRIDDocument(RIDRequestObject reqObj) {
        String uid = reqObj.getParam("documentUID");
        try {
            Dataset ds = getDicomObjectForDocument(uid);
            if ( ds != null ) {
                RIDResponseObject response;
                log.debug("Found Dataset:");log.debug(ds);
                String cuid = ds.getString( Tags.SOPClassUID );
                if ( getECGSopCuids().values().contains( cuid ) ) {
                    response = getECGSupport().getECGDocument( reqObj, ds );
                } else if ( this.getEncapsulatedDocumentSopCuids().values().contains( cuid )) {
                    response = getEncapsulatedDocument( reqObj );
                    logExport(reqObj, ds, "XDS Document Retrieve");
                } else {
                    response = getDocument( reqObj );
                }
                logExport(reqObj, ds, "RID Request");
                return response;
            } else {
                BaseDocument doc = retrieveDocument(uid,reqObj.getParam("preferredContentType"));//Document is not stored in PACS but maybe in DocumentStore (e.g. XDS)
                if ( doc != null && doc.getAvailability().compareTo(Availability.UNAVAILABLE) < 0) {
                    //logExport(reqObj, getXDSPatientInfo(reqObj, is), "XDS Document Retrieve");
                    return new RIDStreamResponseObjectImpl( doc.getInputStream(), 
                            reqObj.getParam("preferredContentType"), HttpServletResponse.SC_OK, null);
                }
                return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_NOT_FOUND, "Object with documentUID="+uid+ " not found!");
            }
        } catch (Exception x) {
            log.error("Cant get RIDDocument:", x);
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cant get Document! Reason: unexpected error:"+x.getMessage() );
        }
    }

    private Dataset getDicomObjectForDocument(String uid) throws FinderException,
    RemoteException, Exception {
        List instances = getContentManager().listInstanceInfos( new String[] {uid}, false);
        return instances.isEmpty() ? null : (Dataset) instances.get(0);
    }

    private boolean isAuditLogIHEYr4() {
        if (auditLogName == null) {
            return false;
        }
        if (auditLogIHEYr4 == null) {
            try {
                this.auditLogIHEYr4 = (Boolean) server.getAttribute(
                        auditLogName, "IHEYr4");
            } catch (Exception e) {
                log.warn("JMX failure: ", e);
                this.auditLogIHEYr4 = Boolean.FALSE;
            }
        }
        return auditLogIHEYr4.booleanValue();
    }

    private void logExport(RIDRequestObject reqObj, Dataset ds, String mediaType ) {
        if (isAuditLogIHEYr4()) return;
        try {
            HttpUserInfo userInfo = new HttpUserInfo(reqObj.getRequest(), AuditMessage.isEnableDNSLookups());
            String user = userInfo.getUserId();
            String host = userInfo.getHostName();
            DataExportMessage msg = new DataExportMessage();
            msg.setOutcomeIndicator( AuditEvent.OutcomeIndicator.SUCCESS );
            msg.addExporterProcess(AuditMessage.getProcessID(), 
                    AuditMessage.getLocalAETitles(),
                    AuditMessage.getProcessName(), false,
                    AuditMessage.getLocalHostName());
            msg.addDataRepository(reqObj.getRequest().getRequestURL().toString());
            msg.addDestinationMedia(host, null, mediaType, user == null, host );
            if (user != null) {
                ActiveParticipant ap = ActiveParticipant.createActivePerson(user, null, user, null, true);
                msg.addActiveParticipant(ap);

            }
            msg.addPatient(ds.getString(Tags.PatientID), ds.getString(Tags.PatientName));
            ParticipantObjectDescription desc = new ParticipantObjectDescription();
            SOPClass sopClass = new SOPClass(ds.getString(Tags.SOPClassUID));
            sopClass.setNumberOfInstances(1);
            desc.addSOPClass(sopClass);
            msg.addStudy(ds.getString(Tags.StudyInstanceUID), desc);
            msg.validate();
            Logger.getLogger("auditlog").info(msg);
        } catch (Exception e) {
            log.warn("Audit Log failed:", e);
        }		
    }

    /**
     * @param reqObj
     * @param f 
     * @return
     */
    private Dataset getXDSPatientInfo(RIDRequestObject reqObj, InputStream is) {
        String pct = reqObj.getParam("preferredContentType");
        Dataset ds = dof.newDataset();
        if ( CONTENT_TYPE_DICOM.equals(pct) ) {
            try {
                DataInputStream dis = new DataInputStream(is);
                DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
                parser.setDcmHandler(ds.getDcmHandler());
                parser.parseDcmFile(FileFormat.DICOM_FILE, -1);
                log.debug("parsed Dicom File ds:");log.debug(ds);
                return ds;
            } catch (Exception x) {
                log.error("Cant parse dicom stream to get XDSPatientInfo!", x);
            }
        }
        log.debug("Not a Dicom file! try to get patient infos!");
        //TODO REAL stuff (need metadata here!)
        if ( ! ds.containsValue(Tags.StudyInstanceUID) ) ds.putUI(Tags.StudyInstanceUID);
        if ( ! ds.containsValue(Tags.PatientID) ) ds.putLO(Tags.PatientID,"UNKNOWN");
        if ( ! ds.containsValue(Tags.PatientName) ) ds.putPN(Tags.PatientName,"UNKNOWN");
        return ds;
    }

    public BaseDocument retrieveDocument(String objectUID, String contentType) throws IOException {
        BaseDocument doc = getStorage().getDocument( objectUID, contentType );
        return doc;
    }

    public BaseDocument getOrCreateDocument(String objectUID, String contentType) throws IOException {
        BaseDocument doc = getStorage().getDocument( objectUID, contentType );
        return doc != null ? doc : getStorage().createDocument(objectUID, contentType);
    }

    private RIDResponseObject getDocument( RIDRequestObject reqObj ) {
        String docUID = reqObj.getParam("documentUID");
        if ( log.isDebugEnabled() ) log.debug(" Document UID:"+docUID);
        String contentType = reqObj.getParam("preferredContentType");
        if ( contentType == null ) {
            contentType = CONTENT_TYPE_PDF;
        } else {
            if ( contentType.equals( CONTENT_TYPE_JPEG )) {
                if ( this.checkContentType( reqObj, new String[]{ CONTENT_TYPE_JPEG } ) == null ) {
                    return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_BAD_REQUEST, "Display actor doesnt accept preferred content type!");
                }
                RIDResponseObject resp = handleJPEG( reqObj );
                if ( resp != null ) return resp; 
                contentType = CONTENT_TYPE_PDF; //cant be rendered as image (SR) make PDF instead.
            } else if ( ! contentType.equals( CONTENT_TYPE_PDF) ) {
                if ( getEncapsulatedDocumentSopCuids().isEmpty() ) //if no encapsulated document cuid defined, we accept only jpeg and pdf.
                    return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_NOT_ACCEPTABLE, "preferredContentType '"+contentType+"' is not supported! Only 'application/pdf' and 'image/jpeg' are supported !");
            }
        }
        if ( this.checkContentType( reqObj, new String[]{ CONTENT_TYPE_PDF } ) == null ) {
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_BAD_REQUEST, "Display actor doesnt accept preferred content type!");
        }
        BaseDocument doc = getStorage().getDocument( docUID, contentType );
        OutputStream out = null;
        try {
            if ( doc == null ) {
                doc = getStorage().createDocument( docUID, contentType );
                out = doc.getOutputStream();
                File inFile = getDICOMFile( docUID );
                if ( inFile == null ) {
                    return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_NOT_FOUND, "Object with documentUID="+docUID+ "not found!");
                }
                if ( ! useOrigFile   ) {
                    FileDataSource ds = null;
                    try {
                        ds = (FileDataSource) server.invoke(queryRetrieveScpName,
                                "getDatasourceOfInstance",
                                new Object[] { docUID },
                                new String[] { String.class.getName() } );

                    } catch (Exception e) {
                        log.error("Failed to get updated DICOM file", e);
                        return new RIDStreamResponseObjectImpl( null, contentType, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error! Cant get updated dicom object");
                    }
                    File tmpFile = File.createTempFile("coerced", "dcm");
                    tmpFile.deleteOnExit();
                    log.debug("Temporary coerced dicom file:"+tmpFile);
                    OutputStream os = new BufferedOutputStream( new FileOutputStream( tmpFile ));
                    ds.setWriteFile(true);
                    ds.writeTo( os, null);
                    os.close();
                    renderSRFile( new FileInputStream(tmpFile), out );
                    tmpFile.delete();
                } else {
                    renderSRFile( new FileInputStream(inFile), out );
                }
                out.close();
            }
            InputStream in = doc.getInputStream();
            return new RIDStreamResponseObjectImpl( in,in.available(), contentType, HttpServletResponse.SC_OK, null);
        } catch (Exception x) {
            log.error("Cant get document! docUID:"+docUID, x);
            if ( out != null) try {
                out.close();
            } catch (IOException ignore) {}
            getStorage().removeDocument( docUID );
        }
        return null;
    }

    /**
     * @param outFile
     * @return
     */
    protected boolean isOutdated(File outFile, String docUID) {
        Date fileDate = new Date( outFile.lastModified() );
        Dataset dsQ = factory.newDataset();
        dsQ.putUI(Tags.SOPInstanceUID, docUID);
        dsQ.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        RetrieveStudyDatesCmd cmd = null;
        try {
            cmd = RetrieveStudyDatesCmd.create(dsQ);
            Date[] dates = cmd.getUpdatedTimes(1);//IMAGE should contain only one result set
            if (dates != null) {
//              check only patient and instance (study and series maybe changed without influence of this instance!) 
                return dates[0].after(fileDate) || dates[3].after(fileDate);
            } else {
                return true;
            }
        } catch (SQLException e) {
            log.error("Cant get Study date/times! mark "+outFile+" as outdated!",e);
            return true;
        } finally {
            if ( cmd != null ) cmd.close();
        }
    }

    /**
     * @param reqObj
     * @return
     */
    private RIDResponseObject handleJPEG(final RIDRequestObject reqObj) {
        File file;
        try {
            String sn = String.class.getName();
            file = (File) server.invoke(null, "getJpgFile", 
                    new Object[] { "rid", "rid", reqObj.getParam("documentUID") }, 
                    new String[] { sn, sn, sn,});
            if ( file != null ) {
                return new RIDStreamResponseObjectImpl( new FileInputStream( file ), CONTENT_TYPE_JPEG, HttpServletResponse.SC_OK, null );
            } else {
                return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_JPEG, HttpServletResponse.SC_NOT_FOUND, "Requested Document not found! documentID:"+reqObj.getParam("documentUID") );
            }
        } catch (Exception e) {
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_JPEG, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error:"+e.getMessage() );
        }
    }

    /**
     * @param f
     * @return
     * @throws IOException
     * @throws TransformerException
     */
    private void renderSRFile(InputStream input, OutputStream out ) throws IOException, TransformerException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(input));
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
        fop.setRenderer(Driver.RENDER_PDF);
        fop.setOutputStream( out );
        Templates template = tf.newTemplates(new StreamSource(FOBSR_XSL_URI));
        TransformerHandler th = tf.newTransformerHandler(template);
        if ( srImageRows != null ) {
            Transformer t = th.getTransformer();
            t.setParameter("srImageRows", srImageRows);
        }
        th.getTransformer().setParameter("wadoURL", wadoURL);
        th.setResult(new SAXResult( fop.getContentHandler() ));
        DcmParser parser = DcmParserFactory.getInstance().newDcmParser(in);
        parser.setSAXHandler2(th,DictionaryFactory.getInstance().getDefaultTagDictionary(), null, 4000, null );//4000 ~ one text page.
        parser.parseDcmFile(null, -1);
    }

    /**
     * Returns the DICOM file for given arguments.
     * <p>
     * Use the FileSystemMgtService MBean to localize the DICOM file.
     * 
     * @param instanceUID	Unique identifier of the instance.
     * 
     * @return The File object or null if not found.
     * 
     * @throws IOException
     */
    public File getDICOMFile( String instanceUID ) throws IOException {
        File file;
        Object dicomObject = null;
        try {
            dicomObject = server.invoke(queryRetrieveScpName,
                    "locateInstance",
                    new Object[] { instanceUID },
                    new String[] { String.class.getName() } );

        } catch (Exception e) {
            log.error("Failed to get DICOM file", e);
        }
        if ( dicomObject == null ) return null; //not found!
        if ( dicomObject instanceof File ) return (File) dicomObject; //We have the File!
        if ( dicomObject instanceof String ) {
            log.info("Requested DICOM file is not local! You can retrieve it from:"+dicomObject);
        }
        return null;
    }

    private RIDResponseObject getEncapsulatedDocument( RIDRequestObject reqObj ) {

        try {
            FileDataSource fds = (FileDataSource) server.invoke(queryRetrieveScpName,
                    "getDatasourceOfInstance",
                    new Object[] { reqObj.getParam("documentUID") },
                    new String[] { String.class.getName() } );

            InputStream is = new BufferedInputStream( new FileInputStream(fds.getFile()) );
            Dataset ds = fds.getMergeAttrs();
            String mime = ds.getString(Tags.MIMETypeOfEncapsulatedDocument);
            if (mime == null) {
                mime = "application.octet-stream";
            }
            log.debug("Mime type of encapsulated document:"+mime);
            if ( checkContentType( reqObj, new String[]{mime}) == null ) 
                return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_BAD_REQUEST, "The Display actor doesnt accept mime type of requested document:"+mime+"!");
            DataInputStream dis = new DataInputStream(is);
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            Dataset attrs = dof.newDataset();
            parser.setDcmHandler( attrs.getDcmHandler() );
            parser.parseDcmFile(null,Tags.EncapsulatedDocument);
            long len = parser.getReadLength();
            log.debug("read length of encapsulated document:"+len);
            return new RIDStreamResponseObjectImpl(is, len, mime, HttpServletResponse.SC_OK, null);
        } catch ( Exception x ) {
            log.error("Error getting encapsulated PDF!", x);
            return new RIDStreamResponseObjectImpl( null, CONTENT_TYPE_HTML, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cant get encapsulated PDF document! Reason:"+x.getMessage());
        }
    }

    /**
     * @param patientID patient id in form patientID^^^issuerOfPatientID
     * @return String[] 0..patientID 1.. issuerOfPatientID
     */
    private String[] splitPatID(String patientID) {
        String[] sa = StringUtils.split(patientID, '^');
        String[] pat = new String[]{sa[0],null};
        if ( sa.length > 3 && sa[3] != null && sa[3].trim().length() > 0 ) {
            pat[1] = sa[3];
        }
        return pat;
    }

    public float getWaveformCorrection() {
        // TODO Auto-generated method stub
        return service.getWaveformCorrection();
    }
    
    public void setConceptNameCodes( String cfg ) {
        conceptNameCodeConfig.setConceptNameCodes(cfg);
        codeCfgString = cfg;
    }
    public String getConceptNameCodes() {
        return conceptNameCodeConfig.getConceptNameCodes();
    }

    private ContentManager getContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory.getFactory()
        .lookup(ContentManagerHome.class, ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    public File getDICOMFile(String string, String string2, String iuid) {
        // TODO Auto-generated method stub
        return null;
    }

    public RIDStorageDelegate getStorage() {
        if ( storage == null)
            storage= RIDStorageDelegate.getInstance();
        return storage;
    }
}
