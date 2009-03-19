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

import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dcm4che.auditlog.AuditLoggerFactory;
import org.dcm4che.auditlog.InstancesAction;
import org.dcm4che.auditlog.Patient;
import org.dcm4che.auditlog.RemoteNode;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.dict.DictionaryFactory;
import org.dcm4che.dict.TagDictionary;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.imageio.plugins.DcmMetadata;
import org.dcm4cheri.imageio.plugins.SimpleYBRColorSpace;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManager;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManagerHome;
import org.dcm4chex.archive.ejb.jdbc.QueryCmd;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileDataSource;
import org.dcm4chex.wado.common.WADORequestObject;
import org.dcm4chex.wado.common.WADOResponseObject;
import org.dcm4chex.wado.mbean.cache.WADOCache;
import org.dcm4chex.wado.mbean.cache.WADOCacheImpl;
import org.dcm4chex.wado.mbean.xml.DatasetXMLResponseObject;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class WADOSupport {

    public static final String NONE = "NONE";

    private static final String REDIRECT_PARAM = "redir";

    public static final String CONTENT_TYPE_JPEG = "image/jpeg";

    public static final String CONTENT_TYPE_DICOM = "application/dicom";

    public static final String CONTENT_TYPE_DICOM_XML = "application/dicom+xml";

    public static final String CONTENT_TYPE_HTML = "text/html";

    private static final String CONTENT_TYPE_XHTML = "application/xhtml+xml";

    public static final String CONTENT_TYPE_XML = "text/xml";

    public static final String CONTENT_TYPE_PLAIN = "text/plain";

    public static final String CONTENT_TYPE_MPEG = "video/mpeg";

    private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";

    private static final String ERROR_INVALID_SIMPLE_FRAME_LIST =
        "Error: simpleFrameList parameter is invalid! Must be a comma " +
        "separated list of positive integer strings in ascending order.";

    private static final String ERROR_INVALID_CALCULATED_FRAME_LIST =
        "Error: calculatedFrameList parameter is invalid! Must be a comma " +
        "separated list of triples of integer strings, defining " +
        "non-overlapping ranges of frame numbers.";

    private static final String ERROR_SIMPLE_AND_CALCULATED_FRAME_LIST =
        "Error: use of simpleFrameList and calculatedFrameList parameter" +
        " are mutually exclusive.";

    private static Logger log = Logger.getLogger(WADOService.class.getName());

    private static final AuditLoggerFactory alf = AuditLoggerFactory
    .getInstance();

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private ObjectName queryRetrieveScpName = null;

    private ObjectName auditLogName = null;
    private ObjectName storeScpServiceName = null;

    private Boolean auditLogIHEYr4 = null;

    /**
     * List of Hosts where audit log is disabled.
     * <p>
     * <code>null</code> means ALL; an empty list means NONE
     */
    private Set disabledAuditLogHosts;

    private boolean useTransferSyntaxOfFileAsDefault = true;

    private boolean disableDNS = false;

    private Map textSopCuids = null;
    private Map imageSopCuids = null;
    private Map videoSopCuids = new TreeMap();
    private Map encapsSopCuids = new TreeMap();

    private static MBeanServer server;

    private static TagDictionary dict = null;

    private String htmlXslURL = "resource:xsl/sr_html.xsl";

    private String xhtmlXslURL = "resource:xsl/sr_html.xsl";

    private String xmlXslURL = "resource:xsl/sr_xml_style.xsl";

    private String dicomXslURL = "resource:xsl/dicom_html.xsl";

    private String contentTypeDicomXML;

    private Map mapTemplates = new HashMap();

    private String srImageRows;

    public WADOSupport(MBeanServer mbServer) {
        if (server != null) {
            server = mbServer;
        } else {
            server = MBeanServerLocator.locate();
        }
    }

    /**
     * Handles a WADO request and returns a WADO response object.
     * <p>
     * </DL>
     * <DT>If the request was successfull:</DT>
     * <DD> The WADO response object contains a File and a corresponding content
     * type.</DD>
     * <DD> the return code was OK and the error message <code>null</code>.</DD>
     * <DT>If the requested object is not local:</DT>
     * <DD> If <code>WADOCacheImpl.isClientRedirect() is false</code> the
     * server tries to connect via WADO to the remote WADO server to get the
     * object.</DD>
     * <DD> if clientRedirect is enabled, the WADOResponse object return code is
     * set to <code>HttpServletResponse.SC_TEMPORARY_REDIRECT</code> and error
     * message is set to the aedto to redirect.</DD>
     * <DT>If the request was not successfull (not found or an error):</DT>
     * <DD> The return code of the WADO response object is set to a http error
     * code and an error message was set.</DD>
     * <DD> The file of the WADO response is <code>null</code>. The content
     * type is not specified for this case.</DD>
     * </DL>
     * 
     * @param req
     *                The WADO request object.
     * 
     * @return The WADO response object.
     * @throws PolicyContextException
     */
    public WADOResponseObject getWADOObject(WADORequestObject req)
    throws Exception {
        log.info("Get WADO object for " + req.getObjectUID());
        Dataset objectDs = null;
        QueryCmd cmd = null;
        log.debug("isStudyPermissionCheckDisabled:"+req.isStudyPermissionCheckDisabled());
        if (!hasPermission(req)) {
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_HTML,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Permission denied for:" + req.getObjectUID());
        }
        try {
            Dataset dsQ = dof.newDataset();
            dsQ.putUI(Tags.SOPInstanceUID, req.getObjectUID());
            dsQ.putUI(Tags.SOPClassUID);
            dsQ.putLO(Tags.PatientID);
            dsQ.putPN(Tags.PatientName);
            dsQ.putUI(Tags.StudyInstanceUID);
            dsQ.putUI(Tags.SeriesInstanceUID);
            dsQ.putUI(Tags.MIMETypeOfEncapsulatedDocument);
            dsQ.putCS(Tags.QueryRetrieveLevel, "IMAGE");
            cmd = QueryCmd.create(dsQ, true, true, false, null);
            cmd.execute();
            if (cmd.next()) {
                objectDs = cmd.getDataset();
            }
        } catch (SQLException x) {
            log.error("Cant get DICOM Object file reference for "
                    + req.getObjectUID(), x);
        } finally {
            if (cmd != null)
                cmd.close();
        }
        log.debug("Found object:" + req.getObjectUID() + ":");
        log.debug(objectDs);
        if (objectDs == null) {
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_HTML,
                    HttpServletResponse.SC_NOT_FOUND,
                    "DICOM object not found! objectUID:" + req.getObjectUID());
        }
        String contentType = getPrefContentType(req, objectDs);
        log.debug("preferred ContentType:" + contentType);
        WADOResponseObject resp = null;
        if (contentType == null) {
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_HTML,
                    HttpServletResponse.SC_NOT_ACCEPTABLE,
                    "Requested object can not be served as requested content type! Requested contentType(s):"
                    + req.getRequest().getParameter("contentType"));
        }
        req.setObjectInfo(objectDs);
        if (CONTENT_TYPE_JPEG.equals(contentType)) {
            return this.handleJpg(req);
        } else if (CONTENT_TYPE_DICOM.equals(contentType)) {
            return handleDicom(req); // audit log is done in handleDicom to
            // avoid extra query.
        }
        File file = null;
        try {
            file = this.getDICOMFile(req.getStudyUID(), req.getSeriesUID(), req
                    .getObjectUID());
            if (file == null) {
                if (log.isDebugEnabled())
                    log.debug("Dicom object not found: " + req);
                return new WADOStreamResponseObjectImpl(null, contentType,
                        HttpServletResponse.SC_NOT_FOUND,
                "DICOM object not found!");
            }
        } catch (IOException x) {
            log.error("Exception in getWADOObject: " + x.getMessage(), x);
            return new WADOStreamResponseObjectImpl(null, contentType,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unexpected error! Cant get file");
        } catch (NeedRedirectionException nre) {
            return handleNeedRedirectException(req, contentType, nre);
        }
        String sopCuid = objectDs.getString(Tags.SOPClassUID);
        if (CONTENT_TYPE_DICOM_XML.equals(contentType)) {
            if (dict == null)
                dict = DictionaryFactory.getInstance().getDefaultTagDictionary();
            resp = handleTextTransform(req, file, contentTypeDicomXML,
                    getDicomXslURL(), dict);
        } else if ( this.getEncapsulatedSopCuids().containsValue(sopCuid)) {
            resp = handleEncaps(file, contentType);
        } else if ( this.getVideoSopCuids().containsValue(sopCuid)){
            resp = handleVideo(file);
        } else if (CONTENT_TYPE_HTML.equals(contentType)) {
            resp = handleTextTransform(req, file, contentType, getHtmlXslURL(),
                    null);
        } else if (CONTENT_TYPE_XHTML.equals(contentType)) {
            resp = handleTextTransform(req, file, contentType,
                    getXHtmlXslURL(), null);
        } else if (CONTENT_TYPE_XML.equals(contentType)) {
            resp = handleTextTransform(req, file, CONTENT_TYPE_XML,
                    getXmlXslURL(), null);
        } else {
            log.debug("Content type not supported! :" + contentType
                    + "\nrequested contentType(s):" + req.getContentTypes()
                    + " SOP Class UID:" + objectDs.getString(Tags.SOPClassUID));
            resp = new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_DICOM,
                    HttpServletResponse.SC_NOT_IMPLEMENTED,
                    "This method is not implemented for requested (preferred) content type!"
                    + contentType);
        }
        return resp;
    }

    private WADOResponseObject handleNeedRedirectException(
            WADORequestObject req, String contentType,
            NeedRedirectionException nre) {
        if ( nre.getExternalRetrieveAET() == null ) {
            return new WADOStreamResponseObjectImpl(null, contentType,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, nre.getMessage());
        }
        if (req.getRequest().getParameter(REDIRECT_PARAM) != null) {
            log.warn("WADO request is already redirected! Return 'NOT FOUND' to avoid circular redirect!\n(Maybe a filesystem was removed from filesystem management but already exists in database!)");
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_JPEG,
                    HttpServletResponse.SC_NOT_FOUND,
            "Object not found (Circular redirect found)!");
        }
        if (!WADOCacheImpl.getWADOCache().isClientRedirect()) {
            return getRemoteWADOObject(nre.getExternalRetrieveAET(), req);
        } else {
            return new WADOStreamResponseObjectImpl(null, contentType,
                    HttpServletResponse.SC_TEMPORARY_REDIRECT,
                    getRedirectURL(nre.getExternalRetrieveAET(), req).toString());
        }
    }

    private boolean hasPermission(WADORequestObject req)
    throws PolicyContextException, RemoteException, Exception {
        if ( req.getRemoteUser() == null || req.isStudyPermissionCheckDisabled() ) {
            log.debug("StudyPermission check disabled!");
            return true;
        }
        Subject subject = (Subject) PolicyContext
        .getContext(SUBJECT_CONTEXT_KEY);
        return getStudyPermissionManager().hasPermission(req.getStudyUID(),
                StudyPermissionDTO.READ_ACTION, subject);
    }

    /**
     * @param contentTypes
     * @return
     */
    private String getPrefContentType(WADORequestObject req, Dataset objectDs) {
        List contentTypes = req.getContentTypes();
        List supportedContentTypes = getSupportedContentTypes(objectDs);
        if (log.isDebugEnabled()) {
            log.debug("Requested content Types:" + contentTypes);
            log.debug("supported content Types:" + supportedContentTypes);
        }
        if (contentTypes == null) {
            return supportedContentTypes.get(0).toString();
        }
        contentTypes.retainAll(supportedContentTypes);// remove all
        // unsupported content
        // types
        if (!contentTypes.isEmpty()) {
            return contentTypes.get(0).toString(); // return the first item
            // (the most accurate)
        } else {
            return null;
        }
    }

    /**
     * @param objectUID
     * @return
     */
    private List getSupportedContentTypes(Dataset objectDs) {
        List types = new ArrayList();
        String sopCuid = objectDs.getString(Tags.SOPClassUID);
        if (getTextSopCuids().containsValue(sopCuid)) {
            types.add(CONTENT_TYPE_HTML);
            types.add(CONTENT_TYPE_XHTML);
            types.add(CONTENT_TYPE_XML);
            types.add(CONTENT_TYPE_PLAIN);
        } else if ( getImageSopCuids().containsValue(sopCuid) ){
            types.add(CONTENT_TYPE_JPEG);
        } else if ( getEncapsulatedSopCuids().containsValue(sopCuid) ){
            String mime = objectDs.getString(Tags.MIMETypeOfEncapsulatedDocument);
            if (mime == null) {
                mime = "application/octet-stream";
            }
            log.info("Mime type of encapsulated document:"+mime);
            types.add(mime);
        } else if ( this.getVideoSopCuids().containsValue(sopCuid) ){
            types.add(CONTENT_TYPE_MPEG);
        }
        types.add(CONTENT_TYPE_DICOM);
        if (!NONE.equals(contentTypeDicomXML))
            types.add(CONTENT_TYPE_DICOM_XML);
        return types;
    }

    public WADOResponseObject handleDicom(WADORequestObject req) {
        File file = null;
        try {
            file = this.getDICOMFile(req.getStudyUID(), req.getSeriesUID(), req
                    .getObjectUID());
            if (file == null) {
                if (log.isDebugEnabled())
                    log.debug("Dicom object not found: " + req);
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_DICOM, HttpServletResponse.SC_NOT_FOUND,
                "DICOM object not found!");
            }
        } catch (IOException x) {
            log.error("Exception in handleDicom: " + x.getMessage(), x);
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_DICOM,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unexpected error! Cant get dicom object");
        } catch (NeedRedirectionException nre) {
            return handleNeedRedirectException(req, CONTENT_TYPE_DICOM, nre);
        }

        if ("true".equals(req.getRequest().getParameter("useOrig"))) {
            try {
                WADOStreamResponseObjectImpl resp = new WADOStreamResponseObjectImpl(
                        new FileInputStream(file), CONTENT_TYPE_DICOM,
                        HttpServletResponse.SC_OK, null);
                log.info("Original Dicom object file retrieved (useOrig=true) objectUID:"
                        + req.getObjectUID());
                Dataset ds = req.getObjectInfo();
                ds.putPN(Tags.PatientName, ds.getString(Tags.PatientName)
                        + " (orig)");
                resp.setPatInfo(ds);
                return resp;
            } catch (FileNotFoundException e) {
                log.error("Dicom File not found (useOrig=true)! file:" + file);
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_DICOM,
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Unexpected error! Cant get dicom object");
            }
        }
        return getUpdatedInstance(req, checkTransferSyntax(req
                .getTransferSyntax()));
    }
    
    private WADOResponseObject handleEncaps(File file, String contentType) {
        try {
            InputStream is = new BufferedInputStream( new FileInputStream(file) );
            DataInputStream dis = new DataInputStream(is);
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            parser.parseDcmFile(null,Tags.EncapsulatedDocument);
            long len = parser.getReadLength();
            log.debug("read length of encapsulated document:"+len);
            return new WADOStreamResponseObjectImpl(is, len, contentType, HttpServletResponse.SC_OK, null);
        } catch (Exception x) {
            log.error("Cant get content from encapsulated DICOM storage object! file:" + file);
            return new WADOStreamResponseObjectImpl(null, contentType,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error! Cant get content from encapsulated DICOM storage object!");
        }
    }
    private WADOResponseObject handleVideo(File file) {
        try {
            InputStream is = new BufferedInputStream( new FileInputStream(file) );
            DataInputStream dis = new DataInputStream(is);
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(dis);
            parser.parseDcmFile(null,Tags.PixelData);
            parser.parseHeader();
            int offsetTableLen = parser.getReadLength();
            if ( offsetTableLen != 0 ) {
                log.warn("OffsetTable len is not 0!");
            }
            parser.parseHeader();
            long len = parser.getReadLength();
            log.debug("read length of mpeg2 data:"+len);
            return new WADOStreamResponseObjectImpl(is, len, CONTENT_TYPE_MPEG, HttpServletResponse.SC_OK, null);
        } catch (Exception x) {
            log.error("Cant get mpeg2 data! file:" + file);
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_MPEG,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error! Cant get mpeg2 data!");
        }
    }

    /**
     * @param transferSyntax
     * @return
     */
    private String checkTransferSyntax(String transferSyntax) {
        if (transferSyntax == null) {
            if (!this.useTransferSyntaxOfFileAsDefault)
                return UIDs.ExplicitVRLittleEndian;
            else
                return null;
        }
        if (!UIDs.isValid(transferSyntax)) {
            log
            .warn("WADO parameter transferSyntax is not a valid UID! Use Explicit VR little endian instead! transferSyntax:"
                    + transferSyntax);
            return UIDs.ExplicitVRLittleEndian;
        }
        if (transferSyntax.equals(UIDs.ImplicitVRLittleEndian)
                || transferSyntax.equals(UIDs.ExplicitVRBigEndian)) {
            log
            .warn("WADO parameter transferSyntax should neither Implicit VR, nor Big Endian! Use Explicit VR little endian instead! transferSyntax:"
                    + transferSyntax);
            return UIDs.ExplicitVRLittleEndian;
        }
        return transferSyntax;
    }

    private static int[] parseInts(String s) {
        if (s == null) {
            return null;
        }
        String[] ss = s.split(",");
        if (ss.length == 0) {
            throw new IllegalArgumentException();
        }
        int[] frameList = new int[ss.length];
        for (int i = 0; i < frameList.length; i++) {
            frameList[i] = Integer.parseInt(ss[i].trim());
        }
        return frameList;
    }

    private WADOResponseObject getUpdatedInstance(WADORequestObject req,
            String transferSyntax) {
        String iuid = req.getObjectUID();
        FileDataSource ds = null;
        try {
            ds = (FileDataSource) server.invoke(queryRetrieveScpName,
                    "getDatasourceOfInstance", new Object[] { iuid },
                    new String[] { String.class.getName() });
            Dataset d = ds.getMergeAttrs();
            ds.setWriteFile(true);
            ds.setExcludePrivate(req.isExcludePrivate());
            try {
                ds.setSimpleFrameList(parseInts(req.getSimpleFrameList()));
            } catch (IllegalArgumentException iae) {
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_DICOM,
                        HttpServletResponse.SC_BAD_REQUEST,
                        ERROR_INVALID_SIMPLE_FRAME_LIST);
            }
            try {
                ds.setCalculatedFrameList(
                        parseInts(req.getCalculatedFrameList()));
            } catch (IllegalStateException ise) {
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_DICOM,
                        HttpServletResponse.SC_BAD_REQUEST,
                        ERROR_SIMPLE_AND_CALCULATED_FRAME_LIST);
            } catch (IllegalArgumentException iae) {
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_DICOM,
                        HttpServletResponse.SC_BAD_REQUEST,
                        ERROR_INVALID_CALCULATED_FRAME_LIST);
            }
            WADODatasourceResponseObjectImpl resp = new WADODatasourceResponseObjectImpl(
                    ds, transferSyntax, CONTENT_TYPE_DICOM,
                    HttpServletResponse.SC_OK, null);
            resp.setPatInfo(d);
            return resp;
        } catch (Exception e) {
            log.error("Failed to get updated DICOM file", e);
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_DICOM,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unexpected error! Cant get updated dicom object");
        }
    }

    /**
     * Handles a request for content type image/jpeg.
     * <p>
     * Use this method first if content type jpeg is possible to get advantage
     * of the cache.
     * <p>
     * 
     * @param studyUID
     *                The unique id of a study.
     * @param seriesUID
     *                The unique id of a series.
     * @param instanceUID
     *                The unique id of an instance.
     * @param rows
     *                The number of pixel rows (integer String)
     * @param columns
     *                the number of pixel columns (integer String)
     * 
     * @return The WADO response object containing the file of the image.
     */
    public WADOResponseObject handleJpg(WADORequestObject req) {
        String studyUID = req.getStudyUID();
        String seriesUID = req.getSeriesUID();
        String instanceUID = req.getObjectUID();
        String rows = req.getRows();
        String columns = req.getColumns();
        String frameNumber = req.getFrameNumber();
        String region = req.getRegion();
        String windowWidth = req.getWindowWidth();
        String windowCenter = req.getWindowCenter();
        String imageQuality = req.getImageQuality();

        try {
            File file = getJpg(studyUID, seriesUID, instanceUID, rows, columns,
                    frameNumber, region, windowWidth, windowCenter,
                    imageQuality);
            if (file != null) {
                WADOStreamResponseObjectImpl resp = new WADOStreamResponseObjectImpl(
                        new FileInputStream(file), CONTENT_TYPE_JPEG,
                        HttpServletResponse.SC_OK, null);
                resp.setPatInfo(req.getObjectInfo());
                return resp;
            } else {
                return new WADOStreamResponseObjectImpl(null,
                        CONTENT_TYPE_JPEG, HttpServletResponse.SC_NOT_FOUND,
                "DICOM object not found!");
            }
        } catch (NeedRedirectionException nre) {
            return handleNeedRedirectException(req, CONTENT_TYPE_JPEG, nre);
        } catch (NoImageException x1) {
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_JPEG,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Cant get jpeg from requested object");
        } catch (ImageCachingException x1) {
            return new WADOImageResponseObjectImpl(x1.getImage(),
                    CONTENT_TYPE_JPEG, HttpServletResponse.SC_OK,
            "Warning: Caching failed!");
        } catch (Exception x) {
            log.error("Exception in handleJpg: " + x.getMessage(), x);
            return new WADOStreamResponseObjectImpl(null, CONTENT_TYPE_JPEG,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unexpected error! Cant get jpeg");
        }
    }

    /**
     * 
     * @param studyUID
     * @param seriesUID
     * @param instanceUID
     * @param rows
     * @param columns
     * @param frameNumber
     * @param region
     *                String representing a rectangular region of the image
     * @param windowWidth
     *                Decimal string indicating the contrast of the image.
     * @param windowCenter
     *                Decimal string indicating the luminosity of the image.
     * @param readDicom
     *                Used to indicate if the image is read from the dicom file
     *                for this request.
     * @return
     * @throws IOException
     * @throws NeedRedirectionException
     * @throws NoImageException
     * @throws ImageCachingException
     */
    public File getJpg(String studyUID, String seriesUID, String instanceUID,
            String rows, String columns, String frameNumber, String region,
            String windowWidth, String windowCenter, String imageQuality)
    throws IOException, NeedRedirectionException, NoImageException,
    ImageCachingException {
        int frame = 0;
        String suffix = null;
        if (frameNumber != null) {
            frame = Integer.parseInt(frameNumber)-1;
            if (frame > 0)
                suffix = "-" + frameNumber;
            else
                frame = 0;
        }
        WADOCache cache = WADOCacheImpl.getWADOCache();
        File file;
        BufferedImage bi = null;

        file = cache.getImageFile(studyUID, seriesUID, instanceUID, rows,
                columns, region, windowWidth, windowCenter, imageQuality,
                suffix);

        if (file == null) {
            File dicomFile = getDICOMFile(studyUID, seriesUID, instanceUID);
            if (dicomFile != null) {
                bi = getImage(dicomFile, frame, rows, columns, region,
                        windowWidth, windowCenter);
            } else {
                return null;
            }
            if (bi != null) {
                try {
                    file = cache.putImage(bi, studyUID, seriesUID, instanceUID,
                            rows, columns, region, windowWidth, windowCenter,
                            imageQuality, suffix);
                } catch (Exception x) {
                    log.warn("Error caching image file! Return image without caching (Enable DEBUG for stacktrace)!");
                    log.debug("Stacktrace for caching error:",x);
                    throw new ImageCachingException(bi);
                }
            } else {
                throw new NoImageException();
            }
        }
        return file;
    }

    /* _ */

    private WADOResponseObject handleTextTransform(WADORequestObject req,
            File file, String contentType, String xslURL, TagDictionary dict) {
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(
                    new FileInputStream(file)));
            DcmParser parser = DcmParserFactory.getInstance().newDcmParser(in);
            Dataset ds = dof.newDataset();
            parser.setDcmHandler(ds.getDcmHandler());
            parser.parseDcmFile(null, Tags.PixelData);
            Dataset dsCoerce = null;
            if (!"true".equals(req.getRequest().getParameter("useOrig"))) {
                dsCoerce = this.getContentManager().getInstanceInfo(
                        req.getObjectUID(), true);
                ds.putAll(dsCoerce);
            }
            if (log.isDebugEnabled()) {
                log.debug("Dataset for XSLT Transformation:");
                log.debug(ds);
                log.debug("Use XSLT stylesheet:" + xslURL);
            }
            TransformerHandler th = getTransformerHandler(xslURL);
            if ( srImageRows != null ) {
                Transformer t = th.getTransformer();
                t.setParameter("srImageRows", srImageRows);
            }
            DatasetXMLResponseObject res = new DatasetXMLResponseObject(ds, th,
                    dict);
            WADOTransformResponseObjectImpl resp = new WADOTransformResponseObjectImpl(
                    res, contentType, HttpServletResponse.SC_OK, null);
            resp.setPatInfo(dsCoerce != null ? dsCoerce : ds);
            return resp;
        } catch (Exception e) {
            log.error("Failed to get DICOM file", e);
            return new WADOStreamResponseObjectImpl(null, contentType,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Unexpected error! Cant get dicom object");
        }
    }

    public String getSrImageRows() {
        return srImageRows;
    }

    public void setSrImageRows(String srImageRows) {
        if ( srImageRows != null )
            Integer.parseInt(srImageRows);
        this.srImageRows = srImageRows;
    }

    /**
     * @return
     */
    public String getHtmlXslURL() {
        return htmlXslURL;
    }

    /**
     * @param htmlXslURL
     *                The htmlXslURL to set.
     */
    public void setHtmlXslURL(String htmlXslURL) {
        this.htmlXslURL = htmlXslURL;
    }

    /**
     * @return
     */
    public String getXHtmlXslURL() {
        return xhtmlXslURL;
    }

    /**
     * @param htmlXslURL
     *                The htmlXslURL to set.
     */
    public void setXHtmlXslURL(String xslURL) {
        this.xhtmlXslURL = xslURL;
    }

    /**
     * @return Returns the xmlXslURL.
     */
    public String getXmlXslURL() {
        return xmlXslURL;
    }

    /**
     * @param xmlXslURL
     *                The xmlXslURL to set.
     */
    public void setXmlXslURL(String xmlXslURL) {
        this.xmlXslURL = xmlXslURL;
    }

    /**
     * @return the dicomXslURL
     */
    public String getDicomXslURL() {
        return dicomXslURL;
    }

    /**
     * @param dicomXslURL
     *                the dicomXslURL to set
     */
    public void setDicomXslURL(String dicomXslURL) {
        this.dicomXslURL = dicomXslURL;
    }

    /**
     * @return the contentTypeDicomXML
     */
    public String getContentTypeDicomXML() {
        return contentTypeDicomXML;
    }

    /**
     * @param contentTypeDicomXML
     *                the contentTypeDicomXML to set
     */
    public void setContentTypeDicomXML(String contentTypeDicomXML) {
        this.contentTypeDicomXML = contentTypeDicomXML;
    }

    private TransformerHandler getTransformerHandler(String xslt)
    throws TransformerConfigurationException {
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
        .newInstance();
        TransformerHandler th;
        if (xslt != null && !xslt.equalsIgnoreCase(NONE)) {
            Templates stylesheet = (Templates) mapTemplates.get(xslt);
            if (stylesheet == null) {
                stylesheet = tf.newTemplates(new StreamSource(xslt));
                mapTemplates.put(xslt, stylesheet);
            }
            th = tf.newTransformerHandler(stylesheet);
        } else {
            th = tf.newTransformerHandler();
        }
        return th;
    }

    public void clearTemplateCache() {
        mapTemplates.clear();
    }

    /**
     * Returns the DICOM file for given arguments.
     * <p>
     * Use the FileSystemMgtService MBean to localize the DICOM file.
     * 
     * @param studyUID
     *                Unique identifier of the study.
     * @param seriesUID
     *                Unique identifier of the series.
     * @param instanceUID
     *                Unique identifier of the instance.
     * 
     * @return The File object or null if not found.
     * 
     * @throws IOException
     */
    public File getDICOMFile(String studyUID, String seriesUID,
            String instanceUID) throws IOException, NeedRedirectionException {
        Object dicomObject = null;
        try {
            dicomObject = server.invoke(queryRetrieveScpName, "locateInstance",
                    new Object[] { instanceUID }, new String[] { String.class
                    .getName() });

        } catch (Exception e) {
            if (e.getCause() instanceof UnknownAETException) {
                //Indicate NeedRedirect with unknown external retrieve AET
                throw new NeedRedirectionException(
                        "Can't redirect WADO request to external retrieve AET! Unknown AET:"
                        +e.getCause().getMessage(), null);
            }
            log.error("Failed to get DICOM file:" + instanceUID, e);
        }
        if (dicomObject == null)
            return null; // not found!
        if (dicomObject instanceof File)
            return (File) dicomObject; // We have the File!
        if (dicomObject instanceof AEDTO) {
            throw new NeedRedirectionException(null, (AEDTO) dicomObject);
        }
        return null;
    }

    /**
     * Returns the WADO URL to remote server which serves the object.
     * <p>
     * the remote server have to be a dcm4chee-wado server on the same port as
     * this WADO server!
     * 
     * @param aedto
     * @param req
     * @return
     */
    private URL getRedirectURL(AEDTO aedto, WADORequestObject req) {
        URL url = null;
        try {
            URL reqURL =  new URL( req.getRequestURL());
            if ( aedto.getWadoUrl() != null ) {
                URL baseURL = new URL(aedto.getWadoUrl());
                StringBuffer sbQuery = new StringBuffer();
                sbQuery.append('?').append(REDIRECT_PARAM).append("=true");
                sbQuery.append('&').append(reqURL.getQuery());
                String baseQuery = baseURL.getQuery();
                if ( baseQuery != null && !baseQuery.equals("requestType=WADO")) {
                    sbQuery.append('&').append(baseQuery);
                }
                url = new URL(baseURL.getProtocol(), baseURL.getHost(), 
                        baseURL.getPort(), baseURL.getPath()+sbQuery);
            } else {
                url = new URL(reqURL.getProtocol(), aedto.getHostName(), 
                        reqURL.getPort(), reqURL.getFile());
            }
        } catch (MalformedURLException e) {
            log.error("Malformed redirect URL to remote AET:" + aedto + " wadoURL:"
                    + aedto.getWadoUrl(), e);
        }
        if (log.isDebugEnabled())
            log.debug("redirect url:" + url);
        return url;
    }

    /**
     * Tries to get the WADO object from an external WADO service.
     * 
     * @param aedto
     *                External Retrieve AE to reference remote WADO service.
     * @param req
     *                The original WADO request.
     * 
     * @return A WADOResponseObject containing the result of a remote WADO request.
     */
    private WADOResponseObject getRemoteWADOObject(AEDTO aedto,
            WADORequestObject req) {
        if (log.isInfoEnabled())
            log.info("WADO request redirected to aedto:" + aedto.getHostName()+" WADO URL:"+aedto.getWadoUrl());
        URL url = null;
        try {
            url = getRedirectURL(aedto, req);
            if (log.isDebugEnabled())
                log.debug("redirect url:" + url);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            String authHeader = (String) req.getRequestHeaders().get(
            "Authorization");
            if (authHeader != null) {
                conn.addRequestProperty("Authorization", authHeader);
            }
            conn.connect();
            if (log.isDebugEnabled())
                log.debug("conn.getResponseCode():" + conn.getResponseCode());
            if (conn.getResponseCode() != HttpServletResponse.SC_OK) {
                if (log.isInfoEnabled())
                    log.info("Remote WADO server responses with:"
                            + conn.getResponseMessage());
                return new WADOStreamResponseObjectImpl(null, conn
                        .getContentType(), conn.getResponseCode(), conn
                        .getResponseMessage());
            }
            InputStream is = conn.getInputStream();
            if (WADOCacheImpl.getWADOCache().isRedirectCaching()
                    && CONTENT_TYPE_JPEG.equals(conn.getContentType())) {
                String suffix = req.getFrameNumber();
                if (suffix != null && suffix.equals("0"))
                    suffix = null;// 0 is default -> dont use suffix!
                File file = WADOCacheImpl.getWADOCache().putStream(is,
                        req.getStudyUID(), req.getSeriesUID(),
                        req.getObjectUID(), req.getRows(), req.getColumns(),
                        req.getRegion(), req.getWindowWidth(),
                        req.getWindowCenter(), req.getImageQuality(), suffix);
                is = new FileInputStream(file);
            }
            return new WADOStreamResponseObjectImpl(is, conn.getContentType(),
                    HttpServletResponse.SC_OK, null);
        } catch (Exception e) {
            log.error("Can't connect to remote WADO service:" + url, e);
            e.printStackTrace();
            return new WADOStreamResponseObjectImpl(
                    null,
                    CONTENT_TYPE_JPEG,
                    HttpServletResponse.SC_NOT_FOUND,
                    "Redirect to find requested object failed! (Can't connect to remote WADO service:"
                    + url + ")!");
        }
    }

    /**
     * Get the image from DICOM file.
     * <p>
     * If <code>rows or columns</code> not null, the original image will be
     * scaled.
     * 
     * @param file
     *                A DICOM file.
     * @param frame
     * @param rows
     *                Image height in pixel.
     * @param columns
     *                Image width in pixel.
     * @param region
     *                String representing a rectangular region of the image
     * @param windowWidth
     *                Decimal string representing the contrast of the image.
     * @param windowCenter
     *                Decimal string representing the luminosity of the image.
     * 
     * @return
     * @throws IOException
     */
    private BufferedImage getImage(File file, int frame, String rows,
            String columns, String region, String windowWidth,
            String windowCenter) throws IOException {
        Iterator it = ImageIO.getImageReadersByFormatName("DICOM");
        if (!it.hasNext())
            return null; // TODO more useful stuff
        ImageReader reader = (ImageReader) it.next();
        ImageInputStream in = new FileImageInputStream(file);
        reader.setInput(in);
        BufferedImage bi = null;
        Rectangle regionRectangle = null;
        try {
            ImageReadParam param = reader.getDefaultReadParam();
            if (region != null) {
                String[] ss = StringUtils.split(region, ',');
                int totWidth = reader.getWidth(0);
                int totHeight = reader.getHeight(0);

                int topX = (int) Math.round(Double.parseDouble(ss[0])
                        * totWidth); // top left X value
                int topY = (int) Math.round(Double.parseDouble(ss[1])
                        * totHeight); // top left Y value
                int botX = (int) Math.round(Double.parseDouble(ss[2])
                        * totWidth); // bottom right X value
                int botY = (int) Math.round(Double.parseDouble(ss[3])
                        * totHeight); // bottom right Y value

                int w = botX - topX;
                int h = botY - topY;

                regionRectangle = new Rectangle(topX, topY, w, h);
                param.setSourceRegion(regionRectangle);
            }
            if (windowWidth != null && windowCenter != null) {
                Dataset data = ((DcmMetadata) reader.getStreamMetadata())
                .getDataset();
                data.putDS(Tags.WindowWidth, windowWidth);
                data.putDS(Tags.WindowCenter, windowCenter);
            }

            bi = reader.read(frame, param);
        } catch (Exception x) {
            log.error("Can't read image:", x);
            return null;
        }
        float aspectRatio = reader.getAspectRatio(frame);
        if (rows != null || columns != null || aspectRatio != 1.0f) {
            bi = resize(bi, rows, columns, aspectRatio);
        }
        return bi;
    }

    /**
     * Resize the given image.
     * 
     * @param bi
     *                The image as BufferedImage.
     * @param rows
     *                Image height in pixel.
     * @param columns
     *                Image width in pixel.
     * @param aspectRatio 
     * 
     * @return
     */
    private BufferedImage resize(BufferedImage bi, String rows, String columns, float aspectRatio) {
        int h0 = bi.getHeight();
        int w0 = bi.getWidth();
        double ratio = (double) aspectRatio;
        int newH = -1, newW = -1;
        if (rows != null) {
            newH = Integer.parseInt(rows);
        }
        if (columns != null) {
            newW = Integer.parseInt(columns);
        }
        if (newW == -1) {
            if ( newH == -1)
                newH = h0;
            newW = (int) (newH * ratio);
        } else if (newH == -1)
            newH = (int) (newW / ratio);
        int w = newW;
        int h = newH;
        w = (int) Math.min(w, h * ratio);
        h = (int) Math.min(h, w / ratio);
        if (w != w0 || h != h0) {
            ColorSpace cs = bi.getColorModel().getColorSpace();
            if (cs instanceof SimpleYBRColorSpace) {
                // convert YBR to RGB, otherwise scaleOp.filter(bi, null) will
                // throw ImagingOpException("Unable to transform src image")
                ColorConvertOp colorConvetOp = new ColorConvertOp(
                        ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
                bi = colorConvetOp.filter(bi, null);
            }
            AffineTransform scale = AffineTransform.getScaleInstance((double) w
                    / w0, (double) h / h0);
            AffineTransformOp scaleOp = new AffineTransformOp(scale,
                    AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage biDest = scaleOp.filter(bi, null);
            return biDest;
        } else {
            return bi;
        }
    }

    public ObjectName getQueryRetrieveScpName() {
        return queryRetrieveScpName;
    }

    public void setQueryRetrieveScpName(ObjectName name) {
        this.queryRetrieveScpName = name;
    }

    /**
     * Set the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @param name
     *                The Audit Logger Name to set.
     */
    public void setAuditLoggerName(ObjectName name) {
        this.auditLogName = name;
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

    public ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public void setStoreScpServiceName(ObjectName storeScpServiceName) {
        this.storeScpServiceName = storeScpServiceName;
    }

    /**
     * @return Returns if audit log is enabled for the host of the given
     *         request.
     */
    public boolean isAuditLogEnabled(WADORequestObject req) {
        return disabledAuditLogHosts == null ? false : !disabledAuditLogHosts
                .contains(req.getRemoteHost());
    }

    /**
     * Set the list of host where audit log is disabled.
     * <p>
     * An empty list means 'NONE'<br>
     * <code>null</code> means 'ALL'
     * 
     * @param disabledLogHosts
     *                The disabledLogHosts to set.
     */
    public void setDisabledAuditLogHosts(Set disabledLogHosts) {
        this.disabledAuditLogHosts = disabledLogHosts;
    }

    public Set getDisabledAuditLogHosts() {
        return disabledAuditLogHosts;
    }

    /**
     * @return the disableDNS
     */
    public boolean isDisableDNS() {
        return disableDNS;
    }

    /**
     * @param disableDNS
     *                the disableDNS to set
     */
    public void setDisableDNS(boolean disableDNS) {
        this.disableDNS = disableDNS;
    }

    /**
     * @return Returns the useTransferSyntaxOfFileAsDefault.
     */
    public boolean isUseTransferSyntaxOfFileAsDefault() {
        return useTransferSyntaxOfFileAsDefault;
    }

    /**
     * @param useTransferSyntaxOfFileAsDefault
     *                The useTransferSyntaxOfFileAsDefault to set.
     */
    public void setUseTransferSyntaxOfFileAsDefault(
            boolean useTransferSyntaxOfFileAsDefault) {
        this.useTransferSyntaxOfFileAsDefault = useTransferSyntaxOfFileAsDefault;
    }

    public Map getImageSopCuids() {
        if ( imageSopCuids == null ) {
            try {
                imageSopCuids = uidsString2map((String) server.getAttribute(
                        storeScpServiceName, "AcceptedImageSOPClasses") );
            } catch ( Exception x ) {
                log.error("Cant get list of image SOP Class UIDs!",x);
            }
        }
        return imageSopCuids;
    }

    /**
     * @return Returns the sopCuids.
     */
    public Map getTextSopCuids() {
        if (textSopCuids == null)
            setDefaultTextSopCuids();
        return textSopCuids;
    }

    /**
     * @param sopCuids
     *                The sopCuids to set.
     */
    public void setTextSopCuids(String sopCuids) {
        if (sopCuids != null && sopCuids.trim().length() > 0)
            textSopCuids = uidsString2map(sopCuids);
        else {
            setDefaultTextSopCuids();
        }
    }

    /**
     * @return Returns the sopCuids for mpeg2 support.
     */
    public Map getEncapsulatedSopCuids() {
        return encapsSopCuids;
    }

    /**
     * @param sopCuids
     *                The sopCuids to set.
     */
    public void setEncapsulatedSopCuids(String sopCuids) {
        if (sopCuids != null && !NONE.equalsIgnoreCase(sopCuids.trim()))
            encapsSopCuids = uidsString2map(sopCuids);
        else {
            encapsSopCuids.clear();
        }
    }
  
    /**
     * @return Returns the sopCuids for mpeg2 support.
     */
    public Map getVideoSopCuids() {
        return videoSopCuids;
    }

    /**
     * @param sopCuids
     *                The sopCuids to set.
     */
    public void setVideoSopCuids(String sopCuids) {
        if (sopCuids != null && !NONE.equalsIgnoreCase(sopCuids.trim()))
            videoSopCuids = uidsString2map(sopCuids);
        else {
            videoSopCuids.clear();
        }
    }
    
    protected boolean isAuditLogIHEYr4() {
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

    protected void logInstancesSent(WADORequestObject req,
            WADOResponseObject resp) {
        if (!isAuditLogIHEYr4())
            return;
        Dataset ds = resp.getPatInfo();
        Patient patient = alf.newPatient(ds.getString(Tags.PatientID), ds
                .getString(Tags.PatientName));
        String remoteHost = disableDNS ? req.getRemoteAddr() : req
                .getRemoteHost();
        RemoteNode rnode = alf.newRemoteNode(req.getRemoteAddr(), remoteHost,
                null);
        String suid = ds.getString(Tags.StudyInstanceUID);
        try {
            server.invoke(auditLogName, "logInstancesSent", new Object[] {
                    rnode, alf.newInstancesAction("Access", suid, patient) },
                    new String[] { RemoteNode.class.getName(),
                    InstancesAction.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to log Instances Sent:", e);
        }

    }

    /**
     * 
     */
    private void setDefaultTextSopCuids() {
        if (textSopCuids == null) {
            textSopCuids = new TreeMap();
        } else {
            textSopCuids.clear();
        }
        textSopCuids.put("BasicTextSR", UIDs.BasicTextSR);
        textSopCuids.put("ChestCADSR", UIDs.ChestCADSR);
        textSopCuids.put("ComprehensiveSR", UIDs.ComprehensiveSR);
        textSopCuids.put("EnhancedSR", UIDs.EnhancedSR);
        textSopCuids.put("KeyObjectSelectionDocument",
                UIDs.KeyObjectSelectionDocument);
        textSopCuids.put("MammographyCADSR", UIDs.MammographyCADSR);
    }

    public Map uidsString2map(String uids) {
        StringTokenizer st = new StringTokenizer(uids, "\r\n;");
        String uid, name;
        Map map = new TreeMap();
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

    private ContentManager getContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
        .getFactory().lookup(ContentManagerHome.class,
                ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    private StudyPermissionManager getStudyPermissionManager() throws Exception {
        StudyPermissionManagerHome home = (StudyPermissionManagerHome) EJBHomeFactory
        .getFactory().lookup(StudyPermissionManagerHome.class,
                StudyPermissionManagerHome.JNDI_NAME);
        return home.create();
    }

    /**
     * Inner exception class to handle WADO redirection.
     * 
     * @author franz.willer
     * 
     * Holds the aedto of the WADO server that have direct access of the
     * requested object.
     */
    class NeedRedirectionException extends Exception {

        /** Comment for <code>serialVersionUID</code> */
        private static final long serialVersionUID = 1L;

        /** holds the AEDTO to redirect */
        private AEDTO aedto;

        /**
         * Creates a NeedRedirectionException instance.
         * 
         * @param aedto
         *                the target of redirection.
         */
        public NeedRedirectionException(String msg, AEDTO aedto) {
            super(msg);
            this.aedto = aedto;
        }

        public AEDTO getExternalRetrieveAET() {
            return aedto;
        }

        public String getWadoUrl() {
            return aedto == null ? null : aedto.getWadoUrl();
        }
    }

    /**
     * Inner exception class to handle error if DCM object is not a image.
     * 
     * @author franz.willer
     * 
     */
    class NoImageException extends Exception {

        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 1L;

    }

    /**
     * Inner exception class to handle error in caching image files.
     * <p>
     * The image can be sent directly to the WEB client.
     * 
     * @author franz.willer
     * 
     */
    class ImageCachingException extends IOException {

        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 1L;

        private BufferedImage bi;

        public ImageCachingException(BufferedImage bi) {
            this.bi = bi;
        }

        /**
         * @return Returns the bi.
         */
        public BufferedImage getImage() {
            return bi;
        }
    }
}
