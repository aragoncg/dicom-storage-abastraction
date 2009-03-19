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
package org.dcm4chex.archive.xdsi;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.UIDGenerator;
import org.dcm4che2.audit.message.AuditEvent;
import org.dcm4che2.audit.message.AuditMessage;
import org.dcm4che2.audit.message.DataExportMessage;
import org.dcm4che2.audit.message.ParticipantObjectDescription;
import org.dcm4che2.audit.util.InstanceSorter;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.dcm.ianscu.IANScuService;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.jdbc.QueryFilesCmd;
import org.dcm4chex.archive.mbean.AuditLoggerDelegate;
import org.dcm4chex.archive.mbean.HttpUserInfo;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;
import org.jboss.system.server.ServerConfigLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import com.sun.xml.messaging.saaj.util.JAXMStreamSource;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 8715 $ $Date: 2008-12-12 14:29:49 +0100 (Fri, 12 Dec 2008) $
 * @since Feb 15, 2006
 */
public class XDSIService extends ServiceMBeanSupport {

    private static final String DEFAULT_XDSB_SOURCE_SERVICE = "dcm4chee.xds:service=XDSbSourceService";
    public static final String DOCUMENT_ID = "doc_1";
    public static final String PDF_DOCUMENT_ID = "pdf_doc_1";
    public static final String AUTHOR_SPECIALITY = "authorSpeciality";
    public static final String AUTHOR_PERSON = "authorPerson";
    public static final String AUTHOR_ROLE = "authorRole";
    public static final String AUTHOR_ROLE_DISPLAYNAME = "authorRoleDisplayName";
    public  static final String AUTHOR_INSTITUTION = "authorInstitution";
    public  static final String SOURCE_ID = "sourceId";

    private static final String NONE = "NONE";

    protected AuditLoggerDelegate auditLogger = new AuditLoggerDelegate(this);

    private ObjectName ianScuServiceName;
    private ObjectName keyObjectServiceName;
    private ObjectName xdsbSourceServiceName;
    private ObjectName xdsHttpCfgServiceName;
    private Boolean httpCfgServiceAvailable;

    protected String[] autoPublishAETs;
    private String autoPublishDocTitle;

    private static Logger log = Logger.getLogger(XDSIService.class.getName());

    private Map usr2author = new TreeMap();


//  http attributes to document repository actor (synchron) 	
    private String docRepositoryURI;
    private String docRepositoryAET;

//  Metadata attributes
    private File propertyFile;
    private File docTitleCodeListFile;
    private File classCodeListFile;
    private File contentTypeCodeListFile;
    private File eventCodeListFile;
    private File healthCareFacilityCodeListFile;
    private File autoPublishPropertyFile;

    private List authorRoles = new ArrayList();
    private List confidentialityCodes;

    private Properties metadataProps = new Properties();

    private Map mapCodeLists = new HashMap();

    private ObjectName pixQueryServiceName;
    private String sourceID;
    private String localDomain;
    private String affinityDomain;

    private String ridURL;

    private boolean logSOAPMessage = true;
    private boolean indentSOAPLog = true;
    private boolean useXDSb = false;

    private final NotificationListener ianListener = 
        new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            log.info("ianListener called!");
            onIAN((Dataset) notif.getUserData());
        }

    };

    /**
     * @return Returns the property file path.
     */
    public String getPropertyFile() {
        return propertyFile.getPath();
    }
    public void setPropertyFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) return;
        propertyFile = new File(file.replace('/', File.separatorChar));
        try {
            readPropertyFile();
        } catch ( Throwable ignore ) {
            log.warn("Property file "+file+" cant be read!");
        }
    }

    public String getDocTitleCodeListFile() {
        return docTitleCodeListFile == null ? null : docTitleCodeListFile.getPath();
    }
    public void setDocTitleCodeListFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) {
            docTitleCodeListFile = null;
        } else {
            docTitleCodeListFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getClassCodeListFile() {
        return classCodeListFile == null ? null : classCodeListFile.getPath();
    }
    public void setClassCodeListFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) {
            classCodeListFile = null;
        } else {
            classCodeListFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getContentTypeCodeListFile() {
        return contentTypeCodeListFile == null ? null : contentTypeCodeListFile.getPath();
    }
    public void setContentTypeCodeListFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) {
            contentTypeCodeListFile = null;
        } else {
            contentTypeCodeListFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getHealthCareFacilityCodeListFile() {
        return healthCareFacilityCodeListFile == null ? null : healthCareFacilityCodeListFile.getPath();
    }
    public void setHealthCareFacilityCodeListFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) {
            healthCareFacilityCodeListFile = null;
        } else {
            healthCareFacilityCodeListFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getEventCodeListFile() {
        return eventCodeListFile == null ? null : eventCodeListFile.getPath();
    }
    public void setEventCodeListFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1) {
            eventCodeListFile = null;
        } else {
            eventCodeListFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getConfidentialityCodes() {
        return getListString(confidentialityCodes);
    }
    public void setConfidentialityCodes(String codes) throws IOException {
        confidentialityCodes = setListString( codes );;
    }

    /**
     * @return Returns the authorPerson or a user to authorPerson mapping.
     */
    public String getAuthorPersonMapping() {
        if ( usr2author.isEmpty() ) {
            return metadataProps.getProperty(AUTHOR_PERSON);
        } else {
            return this.getMappingString(usr2author);
        }
    }

    /**
     * Set either a fix authorPerson or a mapping user to authorPerson.
     * <p>
     * Mapping format: &lt;user&gt;=&lt;authorPerson&gt;<br>
     * Use either newline or semicolon to seperate mappings.
     * <p>
     * If '=' is ommited, a fixed autorPerson is set in <code>metadataProps</code>
     * 
     * @param s The authorPerson(-mapping) to set.
     */
    public void setAuthorPersonMapping(String s) {
        if ( s == null || s.trim().length() < 1) return;
        usr2author.clear();
        if ( s.indexOf('=') == -1) {
            metadataProps.setProperty(AUTHOR_PERSON, s); //NO mapping user -> authorPerson; use fix authorPerson instead
        } else {
            this.addMappingString(s, usr2author);
        }
    }

    /**
     * get the authorPerson value for given user.
     * 
     * @param user
     * @return
     */
    public String getAuthorPerson( String user ) {
        String person = (String)usr2author.get(user);
        if ( person == null ) {
            person = metadataProps.getProperty(AUTHOR_PERSON);
        }
        return person;
    }


    public String getSourceID() {
        if ( sourceID == null ) sourceID = metadataProps.getProperty(SOURCE_ID);
        return sourceID;
    }

    public void setSourceID(String id) {
        sourceID = id;
        if ( sourceID != null )
            metadataProps.setProperty(SOURCE_ID, sourceID);
    }

    /**
     * @return Returns a list of authorRoles (with displayName) as String.
     */
    public String getAuthorRoles() {
        return getListString(authorRoles);
    }

    /**
     * Set authorRoles (with displayName).
     * <p>
     * Format: &lt;role&gt;^&lt;displayName&gt;<br>
     * Use either newline or semicolon to seperate roles.
     * <p>
     * @param s The roles to set.
     */
    public void setAuthorRoles(String s) {
        if ( s == null || s.trim().length() < 1) return;
        authorRoles = setListString(s);
    }

    public Properties joinMetadataProperties(Properties props) {
        Properties p = new Properties();//we should not change metadataProps!
        p.putAll(metadataProps);
        if ( props == null )
            p.putAll(props);
        return p;
    }

//  http
    /**
     * @return Returns the docRepositoryURI.
     */
    public String getDocRepositoryURI() {
        return docRepositoryURI;
    }
    /**
     * @param docRepositoryURI The docRepositoryURI to set.
     */
    public void setDocRepositoryURI(String docRepositoryURI) {
        this.docRepositoryURI = docRepositoryURI;
    }

    /**
     * @return Returns the docRepositoryAET.
     */
    public String getDocRepositoryAET() {
        return docRepositoryAET == null ? "NONE" : docRepositoryAET;
    }
    /**
     * @param docRepositoryAET The docRepositoryAET to set.
     */
    public void setDocRepositoryAET(String docRepositoryAET) {
        if ( "NONE".equals(docRepositoryAET))
            this.docRepositoryAET = null;
        else
            this.docRepositoryAET = docRepositoryAET;
    }

    public final ObjectName getAuditLoggerName() {
        return auditLogger.getAuditLoggerName();
    }

    public final void setAuditLoggerName(ObjectName auditLogName) {
        this.auditLogger.setAuditLoggerName(auditLogName);
    }

    public final ObjectName getPixQueryServiceName() {
        return pixQueryServiceName;
    }

    public final void setPixQueryServiceName(ObjectName name) {
        this.pixQueryServiceName = name;
    }

    public final ObjectName getIANScuServiceName() {
        return ianScuServiceName;
    }

    public final void setIANScuServiceName(ObjectName ianScuServiceName) {
        this.ianScuServiceName = ianScuServiceName;
    }

    public final ObjectName getKeyObjectServiceName() {
        return keyObjectServiceName;
    }

    public final void setKeyObjectServiceName(ObjectName keyObjectServiceName) {
        this.keyObjectServiceName = keyObjectServiceName;
    }

    public String getXdsbSourceServiceName() {
        return xdsbSourceServiceName == null ? NONE : xdsbSourceServiceName.toString();
    }
    public void setXdsbSourceServiceName(String name) throws MalformedObjectNameException, NullPointerException {
        this.xdsbSourceServiceName = NONE.equals(name) ? null : ObjectName.getInstance(name);
    }

    public String getXdsHttpCfgServiceName() {
        return xdsHttpCfgServiceName == null ? NONE : xdsHttpCfgServiceName.toString();
    }
    public void setXdsHttpCfgServiceName(String name) throws MalformedObjectNameException, NullPointerException {
        try {
            xdsHttpCfgServiceName = NONE.equals(name) ? null : ObjectName.getInstance(name);
        } finally {
            //Set available flag to false when ObjectName is not set or null ('unchecked') otherwise because we have
            //no dependency to the optional configuration service.
            httpCfgServiceAvailable = xdsHttpCfgServiceName == null ? Boolean.FALSE : null;
        }
    }

    public boolean isHttpCfgServiceAvailable() {
        if (httpCfgServiceAvailable==null) {
            if ( xdsHttpCfgServiceName != null ) {
                if ( server.isRegistered(xdsHttpCfgServiceName) ) {
                    this.httpCfgServiceAvailable = Boolean.TRUE;
                } else {
                    this.httpCfgServiceAvailable = Boolean.FALSE;
                }
            } else {
                return false;
            }
        }
        return httpCfgServiceAvailable.booleanValue();
    }

    public boolean isUseXDSb() {
        return useXDSb;
    }
    public void setUseXDSb(boolean useXDSb) {
        if ( this.useXDSb != useXDSb ) {
            this.useXDSb = useXDSb;
            if ( useXDSb && xdsbSourceServiceName == null ) {
                try {
                    setXdsbSourceServiceName(DEFAULT_XDSB_SOURCE_SERVICE);
                } catch (Exception x) {
                    log.warn("Cant set default XDS.b Service ("+DEFAULT_XDSB_SOURCE_SERVICE+")!",x);
                }
            }
        }
    }
    public final String getAutoPublishAETs() {
        return autoPublishAETs.length > 0 ? StringUtils.toString(autoPublishAETs,
        '\\') : NONE;
    }

    public final void setAutoPublishAETs(String autoPublishAETs) {
        this.autoPublishAETs = NONE.equalsIgnoreCase(autoPublishAETs)
        ? new String[0]
                     : StringUtils.split(autoPublishAETs, '\\');
    }

    public final String getAutoPublishDocTitle() {
        return autoPublishDocTitle;
    }
    public final void setAutoPublishDocTitle(String autoPublishDocTitle ) {
        this.autoPublishDocTitle = autoPublishDocTitle;
    }

    public String getAutoPublishPropertyFile() {
        return autoPublishPropertyFile == null ? "NONE" : autoPublishPropertyFile.getPath();
    }
    public void setAutoPublishPropertyFile(String file) throws IOException {
        if ( file == null || file.trim().length() < 1 || file.equalsIgnoreCase("NONE")) {
            autoPublishPropertyFile = null;
        } else {
            autoPublishPropertyFile = new File(file.replace('/', File.separatorChar));
        }
    }

    public String getLocalDomain() {
        return localDomain == null ? "NONE" : localDomain;
    }
    public void setLocalDomain(String domain) {
        localDomain = ( domain==null || 
                domain.trim().length()<1 || 
                domain.equalsIgnoreCase("NONE") ) ? null : domain;
    }

    public String getAffinityDomain() {
        return affinityDomain;
    }
    public void setAffinityDomain(String domain) {
        affinityDomain = domain;
    }

    /**
     * Adds a 'mappingString' (format:&lt;key&gt;=&lt;value&gt;...) to a map.
     * 
     * @param s
     */
    private void addMappingString(String s, Map map) {
        StringTokenizer st = new StringTokenizer( s, ",;\n\r\t ");
        String t;
        int pos;
        while ( st.hasMoreTokens() ) {
            t = st.nextToken();
            pos = t.indexOf('=');
            if ( pos == -1) {
                map.put(t,t);
            } else {
                map.put(t.substring(0,pos), t.substring(++pos));
            }
        }
    }
    /**
     * Returns the String representation of a map
     * @return
     */
    private String getMappingString(Map map) {
        if ( map == null || map.isEmpty() ) return null;
        StringBuffer sb = new StringBuffer();
        String key;
        for ( Iterator iter = map.keySet().iterator() ; iter.hasNext() ; ) {
            key = iter.next().toString();
            sb.append(key).append('=').append(map.get(key)).append( System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    private List setListString(String s) {
        List l = new ArrayList();
        if ( NONE.equals(s) ) return l;
        StringTokenizer st = new StringTokenizer( s, ";\n\r");
        while ( st.hasMoreTokens() ) {
            l.add(st.nextToken());
        }
        return l;
    }

    private String getListString(List l) {
        if ( l == null || l.isEmpty() ) return NONE;
        StringBuffer sb = new StringBuffer();
        for ( Iterator iter = l.iterator() ; iter.hasNext() ; ) {
            sb.append(iter.next()).append( System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    /**
     * @return Returns the ridURL.
     */
    public String getRidURL() {
        return ridURL;
    }
    /**
     * @param ridURL The ridURL to set.
     */
    public void setRidURL(String ridURL) {
        this.ridURL = ridURL;
    }
    /**
     * @return Returns the logSOAPMessage.
     */
    public boolean isLogSOAPMessage() {
        return logSOAPMessage;
    }
    /**
     * @param logSOAPMessage The logSOAPMessage to set.
     */
    public void setLogSOAPMessage(boolean logSOAPMessage) {
        this.logSOAPMessage = logSOAPMessage;
    }

    public boolean isIndentSOAPLog() {
        return indentSOAPLog;
    }
    public void setIndentSOAPLog(boolean indentSOAPLog) {
        this.indentSOAPLog = indentSOAPLog;
    }

//  Operations	

    /**
     * @throws IOException
     */
    public void readPropertyFile() throws IOException {
        File propFile = FileUtils.resolve(this.propertyFile);
        BufferedInputStream bis= new BufferedInputStream( new FileInputStream( propFile ));
        try {
            metadataProps.clear();
            metadataProps.load(bis);
            if ( sourceID != null ) {
                metadataProps.setProperty(SOURCE_ID, sourceID);
            }
        } finally {
            bis.close();
        }
    }

    public List listAuthorRoles() throws IOException {
        return this.authorRoles;
    }
    public List listDocTitleCodes() throws IOException {
        return readCodeFile(docTitleCodeListFile);
    }
    public List listEventCodes() throws IOException {
        return readCodeFile(eventCodeListFile);
    }
    public List listClassCodes() throws IOException {
        return readCodeFile(classCodeListFile);
    }
    public List listContentTypeCodes() throws IOException {
        return readCodeFile(contentTypeCodeListFile);
    }
    public List listHealthCareFacilityTypeCodes() throws IOException {
        return readCodeFile(healthCareFacilityCodeListFile);
    }
    public List listConfidentialityCodes() throws IOException {
        return confidentialityCodes;
    }

    /**
     * @throws IOException
     * 
     */
    public List readCodeFile(File codeFile) throws IOException {
        if ( codeFile == null ) return new ArrayList();
        List l = (List) mapCodeLists.get(codeFile);
        if ( l == null ) {
            l = new ArrayList();
            File file = FileUtils.resolve(codeFile);
            if ( file.exists() ) {
                BufferedReader r = new BufferedReader( new FileReader(file));
                String line;
                while ( (line = r.readLine()) != null ) {
                    if ( ! (line.charAt(0) == '#') ) {
                        l.add( line );
                    }
                }
                log.debug("Codes read from code file "+codeFile);
                log.debug("Codes:"+l);
                mapCodeLists.put(codeFile,l);
            } else {
                log.warn("Code File "+file+" does not exist! return empty code list!");
            }
        }
        return l;
    }

    public void clearCodeFileCache() {
        mapCodeLists.clear();
    }

    public boolean sendSOAP( String metaDataFilename, String docNames, String url ) {
        File metaDataFile = new File( metaDataFilename);

        XDSIDocument[] docFiles = null;
        if ( docNames != null && docNames.trim().length() > 0) {
            StringTokenizer st = new StringTokenizer( docNames, "," );
            docFiles = new XDSIDocument[ st.countTokens() ];
            for ( int i=0; st.hasMoreTokens(); i++ ) {
                docFiles[i] = XDSIFileDocument.valueOf( st.nextToken() );
            }
        }
        return sendSOAP( readXMLFile(metaDataFile), docFiles, url );
    }

    public boolean sendSOAP( Document metaData, XDSIDocument[] docs, String url ) {
        if ( isUseXDSb() ) {
            if ( xdsbSourceServiceName != null ) {
                return exportXDSb(metaData, docs);
            } else {
                log.warn("UseXDSb is enabled but XdsbSourceServiceName is not configured! Use XDS.a instead!");
            }
        }
        if ( url == null ) url = this.docRepositoryURI;
        log.info("Send 'Provide and Register Document Set' request to "+url);
        SOAPConnection conn = null;
        try {
            configTLS(url);
            MessageFactory messageFactory = MessageFactory.newInstance();
            SOAPMessage message = messageFactory.createMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
            SOAPBody soapBody = envelope.getBody();
            SOAPElement bodyElement = soapBody.addDocument(metaData);
            if ( docs != null ) {
                for (int i = 0; i < docs.length; i++) {
                    DataHandler dhAttachment = docs[i].getDataHandler();
                    AttachmentPart part = message.createAttachmentPart(dhAttachment);
                    part.setMimeHeader("Content-Type", docs[i].getMimeType());
                    String docId = docs[i].getDocumentID();
                    if ( docId.charAt(0) != '<' ) {//Wrap with < >
                        docId = "<"+docId+">";
                    }

                    part.setContentId(docId);
                    if ( log.isDebugEnabled()){
                        log.debug("Add Attachment Part ("+(i+1)+"/"+docs.length+")! Document ID:"+part.getContentId()+" mime:"+docs[i].getMimeType());
                    }
                    message.addAttachmentPart(part);
                }
            }
            SOAPConnectionFactory connFactory = SOAPConnectionFactory.newInstance();

            conn = connFactory.createConnection();
            log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            log.info("send request to "+url);
            if ( logSOAPMessage ) log.info("-------------------------------- request  ----------------------------------");
            dumpSOAPMessage(message);
            SOAPMessage response = conn.call(message, url);
            if ( ! logSOAPMessage ) log.info("-------------------------------- response ----------------------------------");
            dumpSOAPMessage(response);
            if ( logSOAPMessage ) log.info("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            return checkResponse( response );
        } catch ( Throwable x ) {
            log.error("Cant send SOAP message! Reason:", x);
            return false;
        } finally {
            if ( conn != null ) try {
                conn.close();
            } catch (SOAPException ignore) {}
        }

    }


    private void configTLS(String url) {
        if ( isHttpCfgServiceAvailable() ) {
            try {
                server.invoke(xdsHttpCfgServiceName,
                        "configTLS",
                        new Object[] { url },
                        new String[] { String.class.getName() } );
            } catch ( Exception x ) {
                log.error( "Exception occured in configTLS: "+x.getMessage(), x );
            }
        }
    }
    private boolean exportXDSb(Document metaData, XDSIDocument[] docs) {
        log.info("export Document(s) as XDS.b Document Source!");
        Node rsp = null;
        try {
            Map mapDocs = new HashMap();
            if ( docs != null) {
                for ( int i = 0 ; i < docs.length ; i++) {
                    mapDocs.put(docs[i].getDocumentID(), docs[i].getDataHandler() ); 
                }
            }
            log.info("call xds.b exportDocuments");
            rsp = (Node) server.invoke(this.xdsbSourceServiceName,
                    "exportDocuments",
                    new Object[] { metaData, mapDocs },
                    new String[] { Node.class.getName(), Map.class.getName() });
            log.info("response from xds.b exportDocuments:"+rsp);
            return checkResponse(rsp.getFirstChild());
        } catch (Exception x) {
            log.error("Export Documents failed via XDS.b transaction",x);
            return false;
        }
    }
    public static String resolvePath(String fn) {
        File f = new File(fn);
        if (f.isAbsolute()) return f.getAbsolutePath();
        File serverHomeDir = ServerConfigLocator.locate().getServerHomeDir();
        return new File(serverHomeDir, f.getPath()).getAbsolutePath();
    }

    public boolean sendSOAP(String kosIuid, Properties mdProps) throws SQLException {
        Dataset kos = queryInstance( kosIuid );
        if ( kos == null ) return false;
        if ( mdProps == null ) mdProps = this.metadataProps;
        List files = new QueryFilesCmd(kosIuid).getFileDTOs();
        if ( files == null || files.size() == 0 ) {
            return false;
        }
        FileDTO fileDTO = (FileDTO) files.iterator().next();
        File file = FileUtils.toFile(fileDTO.getDirectoryPath(), fileDTO.getFilePath());
        XDSIDocument[] docs = new XDSIFileDocument[]
                                                   {new XDSIFileDocument(file,"application/dicom",DOCUMENT_ID,kosIuid)};
        XDSMetadata md = new XDSMetadata(kos, mdProps, docs);
        Document metadata = md.getMetadata();
        return sendSOAP(metadata, docs, null);
    }


    private Dataset queryInstance(String iuid) {
        try {
            return getContentManager().getInstanceInfo(iuid, true);
        } catch (Exception e) {
            log.error("Query for SOP Instance UID:" + iuid + " failed!", e);
        }
        return null;
    }

    public boolean sendSOAP(String kosIuid) throws SQLException {
        Dataset kos = queryInstance( kosIuid );
        return sendSOAP(kos,null);
    }
    public boolean sendSOAP(Dataset kos, Properties mdProps) throws SQLException {
        if ( log.isDebugEnabled()) {
            log.debug("Key Selection Object:");log.debug(kos);
        }
        if ( kos == null ) return false;
        if ( mdProps == null ) mdProps = this.metadataProps;
        String user = mdProps.getProperty("user");
        mdProps.setProperty("xadPatientID", getAffinityDomainPatientID(kos));
        XDSIDocument[] docs;
        String pdfIUID = mdProps.getProperty("pdf_iuid");
        if ( pdfIUID == null || !UIDs.isValid(pdfIUID) ) {
            docs = new XDSIDocument[]{ new XDSIDatasetDocument(kos,"application/dicom",DOCUMENT_ID)};
        } else {
            String pdfUID = UIDGenerator.getInstance().createUID();
            log.info("Add PDF document with IUID "+pdfIUID+" to this submission set!");
            try {
                docs = new XDSIDocument[]{ new XDSIDatasetDocument(kos,"application/dicom",DOCUMENT_ID),
                        new XDSIURLDocument(new URL(ridURL+pdfIUID),"application/pdf",PDF_DOCUMENT_ID,pdfUID)};
            } catch (Exception x) {
                log.error("Cant attach PDF document! :"+x.getMessage(), x);
                return false;
            }
        }
        addAssociations(docs, mdProps);
        XDSMetadata md = new XDSMetadata(kos, mdProps, docs);
        Document metadata = md.getMetadata();
        boolean b = sendSOAP(metadata, docs , null);
        logExport(kos, user, b);
        if ( b ) {
            logIHEYr4Export( kos.getString(Tags.PatientID), 
                    kos.getString(Tags.PatientName),
                    this.getDocRepositoryURI(), 
                    docRepositoryAET,
                    getSUIDs(kos));
        }
        return b;
    }

    private void addAssociations(XDSIDocument[] docs, Properties mdProps) {
        if ( mdProps.getProperty("nrOfAssociations") == null) return; 
        int len = Integer.parseInt(mdProps.getProperty("nrOfAssociations"));
        String assoc, uuid, type, status;
        StringTokenizer st;
        for ( int i=0; i < len ; i++ ) {
            assoc = mdProps.getProperty("association_"+i);
            st = new StringTokenizer(assoc,"|");
            uuid = st.nextToken();
            type = st.nextToken();
            status = st.nextToken();
            for ( int j = 0 ; j < docs.length ; j++) {
                docs[j].addAssociation(uuid, type, status);
            }
        }

    }
    public boolean exportPDF(String iuid) throws SQLException, MalformedURLException {
        return exportPDF(iuid,null);
    }
    public boolean exportPDF(String iuid, Properties mdProps) throws SQLException, MalformedURLException {
        log.debug("export PDF to XDS Instance UID:"+iuid);
        Dataset ds = queryInstance(iuid);
        if ( ds == null ) return false;
        String pdfUID = UIDGenerator.getInstance().createUID();
        log.info("Document UID of exported PDF:"+pdfUID);
        ds.putUI(Tags.SOPInstanceUID,pdfUID);
        if ( mdProps == null ) mdProps = this.metadataProps;
        String user = mdProps.getProperty("user");
        mdProps.setProperty("mimetype", "application/pdf");
        mdProps.setProperty("xadPatientID", getAffinityDomainPatientID(ds));
        XDSIDocument[] docs = new XDSIURLDocument[]
                                                  {new XDSIURLDocument(new URL(ridURL+iuid),"application/pdf",PDF_DOCUMENT_ID,pdfUID)};
        addAssociations(docs, mdProps);
        XDSMetadata md = new XDSMetadata(ds, mdProps, docs);
        Document metadata = md.getMetadata();
        boolean b = sendSOAP(metadata,docs , null);
        logExport(ds, user, b);
        if ( b ) {
            logIHEYr4Export( ds.getString(Tags.PatientID), 
                    ds.getString(Tags.PatientName),
                    this.getDocRepositoryURI(), 
                    docRepositoryAET,
                    getSUIDs(ds));
        }
        return b;
    }

    public boolean createFolder( Properties mdProps ) {
        String patDsIUID = mdProps.getProperty("folder.patDatasetIUID");
        Dataset ds = queryInstance(patDsIUID);
        log.info("create XDS Folder for patient:"+ds.getString(Tags.PatientID));
        mdProps.setProperty("xadPatientID", getAffinityDomainPatientID(ds));
        log.info("XAD patient:"+mdProps.getProperty("xadPatientID"));
        XDSMetadata md = new XDSMetadata(null, mdProps, null);
        Document metadata = md.getMetadata();
        boolean b = sendSOAP(metadata, null , null);
        return b;
    }

    /**
     * @param kos
     * @return
     */
    private Set getSUIDs(Dataset kos) {
        Set suids = null;
        DcmElement sq = kos.get(Tags.CurrentRequestedProcedureEvidenceSeq);
        if ( sq != null ) {
            suids = new LinkedHashSet();
            for ( int i = 0,len=sq.countItems() ; i < len ; i++ ) {
                suids.add(sq.getItem(i).getString(Tags.StudyInstanceUID));
            }
        }
        return suids;
    }

    private void logIHEYr4Export(String patId, String patName, String node, String aet, Set suids) {
        if (!auditLogger.isAuditLogIHEYr4()) return;
        try {
            URL url = new URL(node);
            InetAddress inet = InetAddress.getByName(url.getHost());
            server.invoke(auditLogger.getAuditLoggerName(),
                    "logExport",
                    new Object[] { patId, patName, "XDSI Export", 
                suids,
                inet.getHostAddress(), inet.getHostName(), aet},
                new String[] { String.class.getName(), String.class.getName(), String.class.getName(), 
                Set.class.getName(),
                String.class.getName(), String.class.getName(), String.class.getName()});
            /*_*/
        } catch (Exception e) {
            log.warn("Audit Log failed:", e);
        }		
    }
    private void logExport(Dataset dsKos, String user, boolean success) {
        String requestHost = null;
        HttpUserInfo userInfo = new HttpUserInfo(AuditMessage.isEnableDNSLookups());
        user = userInfo.getUserId();
        requestHost = userInfo.getHostName();
        DataExportMessage msg = new DataExportMessage();
        msg.setOutcomeIndicator(success ? AuditEvent.OutcomeIndicator.SUCCESS:
            AuditEvent.OutcomeIndicator.MINOR_FAILURE);
        msg.addExporterProcess(AuditMessage.getProcessID(), 
                AuditMessage.getLocalAETitles(),
                AuditMessage.getProcessName(), user == null,
                AuditMessage.getLocalHostName());
        if (user != null) {
            msg.addExporterPerson(user, null, null, true, requestHost);
        }
        String host = "unknown";
        try {
            host = new URL(docRepositoryURI).getHost();
        } catch (MalformedURLException ignore) {
        }
        msg.addDestinationMedia(docRepositoryURI, null, "XDS-I Export", false, host );
        msg.addPatient(dsKos.getString(Tags.PatientID), dsKos.getString(Tags.PatientName));
        InstanceSorter sorter = getInstanceSorter(dsKos);
        for (String suid : sorter.getSUIDs()) {
            ParticipantObjectDescription desc = new ParticipantObjectDescription();
            for (String cuid : sorter.getCUIDs(suid)) {
                ParticipantObjectDescription.SOPClass sopClass =
                    new ParticipantObjectDescription.SOPClass(cuid);
                sopClass.setNumberOfInstances(
                        sorter.countInstances(suid, cuid));
                desc.addSOPClass(sopClass);
            }
            msg.addStudy(suid, desc);
        }
        msg.validate();
        Logger.getLogger("auditlog").info(msg);
    }

    private InstanceSorter getInstanceSorter(Dataset dsKos) {
        InstanceSorter sorter = new InstanceSorter();
        DcmObjectFactory df = DcmObjectFactory.getInstance();
        DcmElement sq = dsKos.get(Tags.CurrentRequestedProcedureEvidenceSeq);
        if ( sq != null ) {
            for (int i = 0, n = sq.countItems(); i < n; i++) {
                Dataset refStudyItem = sq.getItem(i);
                String suid = refStudyItem.getString(Tags.StudyInstanceUID);
                DcmElement refSerSeq = refStudyItem.get(Tags.RefSeriesSeq);
                for (int j = 0, m = refSerSeq.countItems(); j < m; j++) {
                    Dataset refSer = refSerSeq.getItem(j);
                    DcmElement srcRefSOPSeq = refSer.get(Tags.RefSOPSeq);
                    for (int k = 0, l = srcRefSOPSeq.countItems(); k < l; k++) {
                        Dataset srcRefSOP = srcRefSOPSeq.getItem(k);
                        Dataset refSOP = df.newDataset();
                        String cuid = srcRefSOP.getString(Tags.RefSOPClassUID);
                        refSOP.putUI(Tags.RefSOPClassUID, cuid);
                        String iuid = srcRefSOP.getString(Tags.RefSOPInstanceUID);
                        refSOP.putUI(Tags.RefSOPInstanceUID, iuid);
                        sorter.addInstance(suid, cuid, iuid, null);
                    }
                }
            }
        } else { //not a manifest! (PDF)
            sorter.addInstance(dsKos.getString(Tags.StudyInstanceUID), dsKos.getString(Tags.SOPClassUID),
                    dsKos.getString(Tags.SOPInstanceUID), null);
        }
        return sorter;
    }

    /**
     * @param kos
     * @return
     */
    public String getAffinityDomainPatientID(Dataset kos) {
        String patID = kos.getString(Tags.PatientID);
        String issuer = kos.getString(Tags.IssuerOfPatientID);
        if ( affinityDomain.charAt(0) == '=') {
            if ( affinityDomain.length() == 1 ) {
                patID+="^^^";
                if ( issuer == null ) return patID;
                return patID+issuer;
            } else if (affinityDomain.charAt(1)=='?') {
                log.info("PIX Query disabled: replace issuer with affinity domain! ");
                log.debug("patID changed! ("+patID+"^^^"+issuer+" -> "+patID+"^^^"+affinityDomain.substring(2)+")");
                return patID+"^^^"+affinityDomain.substring(2);
            } else {
                log.info("PIX Query disabled: replace configured patient ID! :"+affinityDomain.substring(1));
                return affinityDomain.substring(1);
            }
        }
        if ( this.pixQueryServiceName == null ) {
            log.info("PIX Query disabled: use source patient ID!");
            patID+="^^^";
            if ( issuer == null ) return patID;
            return patID+issuer;
        } else {
            try {
                if ( localDomain != null ) {
                    if ( localDomain.charAt(0) == '=') {
                        String oldIssuer = issuer;
                        issuer = localDomain.substring(1);
                        log.info("PIX Query: Local affinity domain changed from "+oldIssuer+" to "+issuer);
                    } else if ( issuer == null ) {
                        log.info("PIX Query: Unknown local affinity domain changed to "+issuer);
                        issuer = localDomain;
                    }
                } else if ( issuer == null ) {
                    issuer = "";
                }
                List pids = (List) server.invoke(this.pixQueryServiceName,
                        "queryCorrespondingPIDs",
                        new Object[] { patID, issuer, new String[]{affinityDomain} },
                        new String[] { String.class.getName(), String.class.getName(), String[].class.getName() });
                String pid, affPid;
                for ( Iterator iter = pids.iterator() ; iter.hasNext() ; ) {
                    pid = toPIDString((String[]) iter.next());
                    log.debug("Check if from domain! PatientID:"+pid);
                    if ( (affPid = isFromDomain(pid)) != null ) {
                        log.debug("found domain patientID:"+affPid);
                        return affPid;
                    }
                }
                log.error("Patient ID is not known in Affinity domain:"+affinityDomain);
                return null;
            } catch (Exception e) {
                log.error("Failed to get patientID for Affinity Domain:", e);
                return null;
            }
        }
    }
    /**
     * @param pid
     * @return
     */
    private String isFromDomain(String pid) {
        int pos = 0;
        for ( int i = 0 ; i < 3 ; i++) {
            pos = pid.indexOf('^', pos);
            if ( pos == -1 ) {
                log.warn("patient id does not contain domain (issuer)! :"+pid);
                return null;
            }
            pos++;
        }
        String s = pid.substring(pos);
        if ( !s.endsWith(this.affinityDomain) )
            return null;
        if (s.length() == affinityDomain.length()) 
            return pid;
        return pid.substring(0,pos)+affinityDomain;
    }

    private String toPIDString(String[] pid) {
        if (pid == null || pid.length < 1) return "";
        StringBuffer sb = new StringBuffer(pid[0]);
        log.debug("pid[0]:"+pid[0]);
        if ( pid.length > 1 ) {
            sb.append("^^^").append(pid[1]);
            log.debug("pid[1]:"+pid[1]);
        }
        for (int i = 2 ; i < pid.length; i++) {
            sb.append('&').append(pid[i]);
            log.debug("pid["+i+"]:"+pid[i]);
        }
        return sb.toString();
    }
    /**
     * @param response
     * @return
     * @throws SOAPException
     */
    private boolean checkResponse(Node n) throws SOAPException {
        log.info("checkResponse node:"+n.getLocalName()+" in NS:"+n.getNamespaceURI());
        String status = n.getAttributes().getNamedItem("status").getNodeValue();
        log.info("XDSI: SOAP response status."+status);
        if ("Success".equals(status)) {
            return true;
        } else {
            StringBuffer sb = new StringBuffer();
            try {
                NodeList errList = n.getChildNodes().item(0).getChildNodes();
                Node errNode;
                for ( int j = 0, lenj = errList.getLength() ; j < lenj ; j++ ) {
                    sb.setLength(0); 
                    sb.append("Error (").append(j).append("):");
                    if ( (errNode = errList.item(j)) != null && errNode.getFirstChild() != null ) {
                        sb.append( errNode.getFirstChild().getNodeValue());
                    }
                    log.info(sb.toString());
                }
            } catch (Exception ignoreMissingErrorList){}
            return false;
        }
    }
    private boolean checkResponse(SOAPMessage response) throws SOAPException {
        log.info("checkResponse:"+response);
        try {
            SOAPBody body = response.getSOAPBody();
            log.debug("SOAPBody:"+body );
            NodeList nl = body.getChildNodes();
            if ( nl.getLength() > 0  ) {
                for ( int i = 0, len = nl.getLength() ; i < len ; i++ ) {
                    Node n = nl.item(i);
                    if ( n.getNodeType() == Node.ELEMENT_NODE &&
                            "RegistryResponse".equals(n.getLocalName() ) ) {
                        return checkResponse(n);
                    }
                }
            } else {
                log.warn("XDSI: Empty SOAP response!");
            }
        } catch ( Exception x ) {
            log.error("Cant check response!", x);
        }
        return false;
    }
    /**
     * @param message
     * @return
     * @throws IOException
     * @throws SOAPException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private void dumpSOAPMessage(SOAPMessage message) throws SOAPException, IOException, ParserConfigurationException, SAXException {
        if ( ! logSOAPMessage ) return;
        Source s = message.getSOAPPart().getContent();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write("SOAP message:".getBytes());
            Transformer t = TransformerFactory.newInstance().newTransformer();
            if (indentSOAPLog)
                t.setOutputProperty("indent", "yes");
            t.transform(s, new StreamResult(out));
            log.info(out.toString());
        } catch (Exception e) {
            log.warn("Failed to log SOAP message", e);
        }
    }

    private Document readXMLFile(File xmlFile){
        Document document = null;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            dbFactory.setNamespaceAware(true);
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            document = builder.parse(xmlFile);
        } catch (Exception x) {
            log.error("Cant read xml file:"+xmlFile, x);
        }
        return document;
    }

    protected void startService() throws Exception {
        server.addNotificationListener(ianScuServiceName,
                ianListener, IANScuService.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(ianScuServiceName,
                ianListener, IANScuService.NOTIF_FILTER, null);
    }

    private void onIAN(Dataset mpps) {
        log.debug("Received mpps");log.debug(mpps);
        if (Arrays.asList(autoPublishAETs).indexOf(
                mpps.getString(Tags.PerformedStationAET)) != -1) {
            List iuids = getIUIDS(mpps);
            log.debug("iuids:"+iuids);
            Dataset manifest = getKeyObject(iuids, getAutoPublishRootInfo(mpps), null);
            log.debug("Created manifest KOS:");
            log.debug(manifest);
            try {
                sendSOAP(manifest, getAutoPublishMetadataProperties(mpps));
            } catch (SQLException x) {
                log.error("XDS-I Autopublish failed! Reason:",x );
            }
            return;
        }
        // TODO        
    }

    private List getIUIDS(Dataset mpps) {
        List l = new ArrayList();
        DcmElement refSerSQ = mpps.get(Tags.PerformedSeriesSeq);
        if ( refSerSQ != null ) {
            Dataset item;
            DcmElement refSopSQ;
            for ( int i = 0 ,len = refSerSQ.countItems() ; i < len ; i++){
                refSopSQ = refSerSQ.getItem(i).get(Tags.RefImageSeq);
                for ( int j = 0 ,len1 = refSerSQ.countItems() ; j < len1 ; j++){
                    item = refSopSQ.getItem(j);
                    l.add( item.getString(Tags.RefSOPInstanceUID));
                }
            }
        }
        return l;
    }
    private Dataset getAutoPublishRootInfo(Dataset mpps) {
        Dataset rootInfo = DcmObjectFactory.getInstance().newDataset();
        DcmElement sq = rootInfo.putSQ(Tags.ConceptNameCodeSeq);
        Dataset item = sq.addNewItem();
        StringTokenizer st = new StringTokenizer(autoPublishDocTitle,"^");
        item.putSH(Tags.CodeValue,st.hasMoreTokens() ? st.nextToken():"autoPublish");
        item.putLO(Tags.CodeMeaning, st.hasMoreTokens() ? st.nextToken():"default doctitle for autopublish");
        item.putSH(Tags.CodingSchemeDesignator,st.hasMoreTokens() ? st.nextToken():null);
        return rootInfo;
    }

    private Properties getAutoPublishMetadataProperties(Dataset mpps) {
        Properties props = new Properties();
        BufferedInputStream bis = null;
        try {
            if (autoPublishPropertyFile == null ) return props;
            File propFile = FileUtils.resolve(this.autoPublishPropertyFile);
            bis= new BufferedInputStream( new FileInputStream( propFile ));
            props.load(bis);
            if ( sourceID != null ) {
                props.setProperty(SOURCE_ID, sourceID);
            }
        } catch (IOException x) {
            log.error("Cant read Metadata Properties for AutoPublish!",x);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ignore) {}
            }
        }
        return props;
    }

    private Dataset getKeyObject(Collection iuids, Dataset rootInfo, List contentItems) {
        Object o = null;
        try {
            o = server.invoke(keyObjectServiceName,
                    "getKeyObject",
                    new Object[] { iuids, rootInfo, contentItems },
                    new String[] { Collection.class.getName(), Dataset.class.getName(), Collection.class.getName() });
        } catch (RuntimeMBeanException x) {
            log.warn("RuntimeException thrown in KeyObject Service:"+x.getCause());
            throw new IllegalArgumentException(x.getCause().getMessage());
        } catch (Exception e) {
            log.warn("Failed to create Key Object:", e);
            throw new IllegalArgumentException("Error: KeyObject Service cant create manifest Key Object! Reason:"+e.getClass().getName());
        }
        return (Dataset) o;
    }

    private ContentManager getContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory.getFactory()
        .lookup(ContentManagerHome.class, ContentManagerHome.JNDI_NAME);
        return home.create();
    }
}
