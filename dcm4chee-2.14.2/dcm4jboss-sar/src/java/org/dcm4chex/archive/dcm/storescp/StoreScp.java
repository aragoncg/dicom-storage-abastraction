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
 * Fuad Ibrahimov <fuad@ibrahimov.de>
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

package org.dcm4chex.archive.dcm.storescp;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.management.ObjectName;
import javax.security.auth.Subject;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.data.DcmParserFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationListener;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.PDU;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.codec.CompressCmd;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.config.CompressionRules;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StorageHome;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.jdbc.QueryFilesCmd;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfMonDelegate;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 8038 $
 * @since 03.08.2003
 */
public class StoreScp extends DcmServiceBase implements AssociationListener {

    private static final int MISSING_USER_ID_ERR_STATUS = 0xCE10;

    private static final int NO_APPEND_PERMISSION_ERR_STATUS = 0xCE24;

    private static final String MISSING_USER_ID_ERR_MSG = "Missing user identification for appending existing Study";

    private static final String NO_APPEND_PERMISSION_ERR_MSG = "No permission to append existing Study";

    private static final String STORE_XSL = "cstorerq.xsl";

    private static final String STORE_XML = "-cstorerq.xml";

    private static final String MWL2STORE_XSL = "mwl-cfindrsp2cstorerq.xsl";

    private static final String STORE2MWL_XSL = "cstorerq2mwl-cfindrq.xsl";

    private static final String RECEIVE_BUFFER = "RECEIVE_BUFFER";

    private static final String SERIES_STORED = "SERIES_STORED";

//    private static final String SOP_IUIDS = "SOP_IUIDS";

    final StoreScpService service;

    private final Logger log;

    private boolean studyDateInFilePath = false;
    
    private boolean sourceAETInFilePath = false;
    
    private boolean yearInFilePath = true;

    private boolean monthInFilePath = true;

    private boolean dayInFilePath = true;

    private boolean hourInFilePath = false;

    private boolean acceptMissingPatientID = true;

    private boolean acceptMissingPatientName = true;

    private boolean serializeDBUpdate = false;

    private int updateDatabaseMaxRetries = 2;

    private int maxCountUpdateDatabaseRetries = 0;

    private boolean storeDuplicateIfDiffMD5 = true;

    private boolean storeDuplicateIfDiffHost = true;

    private long updateDatabaseRetryInterval = 0L;

    private CompressionRules compressionRules = new CompressionRules("");

    private String[] coerceWarnCallingAETs = {};

    private String[] acceptMismatchIUIDCallingAETs = {};

    private boolean checkIncorrectWorklistEntry = true;

    private String referencedDirectoryPath;

    private String referencedDirectoryURI;

    private String refFileSystemGroupID;

    private boolean readReferencedFile = true;

    private boolean md5sumReferencedFile = true;

    private boolean coerceBeforeWrite = false;
    
    private PerfMonDelegate perfMon;

    public StoreScp(StoreScpService service) {
        this.service = service;
        this.log = service.getLog();
        perfMon = new PerfMonDelegate(this.service);
    }

    public final ObjectName getPerfMonServiceName() {
        return perfMon.getPerfMonServiceName();
    }

    public final void setPerfMonServiceName(ObjectName perfMonServiceName) {
        perfMon.setPerfMonServiceName(perfMonServiceName);
    }

    public final boolean isAcceptMissingPatientID() {
        return acceptMissingPatientID;
    }

    public final void setAcceptMissingPatientID(boolean accept) {
        this.acceptMissingPatientID = accept;
    }

    public final boolean isAcceptMissingPatientName() {
        return acceptMissingPatientName;
    }

    public final void setAcceptMissingPatientName(boolean accept) {
        this.acceptMissingPatientName = accept;
    }

    public final boolean isSerializeDBUpdate() {
        return serializeDBUpdate;
    }

    public final void setSerializeDBUpdate(boolean serialize) {
        this.serializeDBUpdate = serialize;
    }

    public final String getCoerceWarnCallingAETs() {
        return StringUtils.toString(coerceWarnCallingAETs, '\\');
    }

    public final void setCoerceWarnCallingAETs(String aets) {
        coerceWarnCallingAETs = StringUtils.split(aets, '\\');
    }

    public final String getAcceptMismatchIUIDCallingAETs() {
        return StringUtils.toString(acceptMismatchIUIDCallingAETs, '\\');
    }

    public final void setAcceptMismatchIUIDCallingAETs(String aets) {
        acceptMismatchIUIDCallingAETs = StringUtils.split(aets, '\\');
    }

    public final boolean isStudyDateInFilePath() {
        return studyDateInFilePath;
    }

    public final void setStudyDateInFilePath(boolean studyDateInFilePath) {
        this.studyDateInFilePath = studyDateInFilePath;
    }

    public final boolean isSourceAETInFilePath() {
        return sourceAETInFilePath;
    }

    public final void setSourceAETInFilePath(boolean sourceAETInFilePath) {
        this.sourceAETInFilePath = sourceAETInFilePath;
    }
    
    public final boolean isYearInFilePath() {
        return yearInFilePath;
    }

    public final void setYearInFilePath(boolean yearInFilePath) {
        this.yearInFilePath = yearInFilePath;
    }

    public final boolean isMonthInFilePath() {
        return monthInFilePath;
    }

    public final void setMonthInFilePath(boolean monthInFilePath) {
        this.monthInFilePath = monthInFilePath;
    }

    public final boolean isDayInFilePath() {
        return dayInFilePath;
    }

    public final void setDayInFilePath(boolean dayInFilePath) {
        this.dayInFilePath = dayInFilePath;
    }

    public final boolean isHourInFilePath() {
        return hourInFilePath;
    }

    public final void setHourInFilePath(boolean hourInFilePath) {
        this.hourInFilePath = hourInFilePath;
    }

    public final String getReferencedDirectoryPath() {
        return referencedDirectoryPath;
    }

    public final void setReferencedDirectoryPath(String pathOrURI) {
        String trimmed = pathOrURI.trim();
        referencedDirectoryURI = isURI(trimmed) ? (trimmed + '/') :
                FileUtils.toFile(trimmed).toURI().toString();
        referencedDirectoryPath = trimmed;
    }

    public void setReferencedFileSystemGroupID(String groupID) {
        this.refFileSystemGroupID = groupID;
    }

    public String getReferencedFileSystemGroupID() {
        return refFileSystemGroupID;
    }

    private static boolean isURI(String pathOrURI) {
        return pathOrURI.indexOf(':') > 1 ;
    }

    public final boolean isMd5sumReferencedFile() {
        return md5sumReferencedFile;
    }

    public final void setMd5sumReferencedFile(boolean md5ReferencedFile) {
        this.md5sumReferencedFile = md5ReferencedFile;
    }

    public final boolean isCoerceBeforeWrite() {
        return this.coerceBeforeWrite;
    }
    
    public final void setCoerceBeforeWrite(boolean coerceBeforeWrite) {
        this.coerceBeforeWrite = coerceBeforeWrite;
    }
    
    public final boolean isReadReferencedFile() {
        return readReferencedFile;
    }

    public final void setReadReferencedFile(boolean readReferencedFile) {
        this.readReferencedFile = readReferencedFile;
    }

    public final boolean isStoreDuplicateIfDiffHost() {
        return storeDuplicateIfDiffHost;
    }

    public final void setStoreDuplicateIfDiffHost(boolean storeDuplicate) {
        this.storeDuplicateIfDiffHost = storeDuplicate;
    }

    public final boolean isStoreDuplicateIfDiffMD5() {
        return storeDuplicateIfDiffMD5;
    }

    public final void setStoreDuplicateIfDiffMD5(boolean storeDuplicate) {
        this.storeDuplicateIfDiffMD5 = storeDuplicate;
    }

    public final CompressionRules getCompressionRules() {
        return compressionRules;
    }

    public final void setCompressionRules(CompressionRules compressionRules) {
        this.compressionRules = compressionRules;
    }

    public final int getUpdateDatabaseMaxRetries() {
        return updateDatabaseMaxRetries;
    }

    public final void setUpdateDatabaseMaxRetries(int updateDatabaseMaxRetries) {
        this.updateDatabaseMaxRetries = updateDatabaseMaxRetries;
    }

    public final int getMaxCountUpdateDatabaseRetries() {
        return maxCountUpdateDatabaseRetries;
    }

    public final void setMaxCountUpdateDatabaseRetries(int count) {
        this.maxCountUpdateDatabaseRetries = count;
    }

    public final long getUpdateDatabaseRetryInterval() {
        return updateDatabaseRetryInterval;
    }

    public final void setUpdateDatabaseRetryInterval(long interval) {
        this.updateDatabaseRetryInterval = interval;
    }

    /**
     * @return Returns the checkIncorrectWorklistEntry.
     */
    public boolean isCheckIncorrectWorklistEntry() {
        return checkIncorrectWorklistEntry;
    }

    /**
     * @param checkIncorrectWorklistEntry
     *                The checkIncorrectWorklistEntry to set.
     */
    public void setCheckIncorrectWorklistEntry(
            boolean checkIncorrectWorklistEntry) {
        this.checkIncorrectWorklistEntry = checkIncorrectWorklistEntry;
    }

    protected void doCStore(ActiveAssociation activeAssoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        InputStream in = rq.getDataAsStream();
        perfMon.start(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_IN);
        perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DIMSE, rq);

        DcmDecodeParam decParam = DcmDecodeParam.valueOf(rq
                .getTransferSyntaxUID());
        Dataset ds = objFact.newDataset();
        DcmParser parser = DcmParserFactory.getInstance().newDcmParser(in);
        parser.setDcmHandler(ds.getDcmHandler());
        parser.parseDataset(decParam, Tags.PixelData);
        if (!parser.hasSeenEOF() && parser.getReadTag() != Tags.PixelData) {
            parser.unreadHeader();
            parser.parseDataset(decParam, -1);
        }
        doActualCStore(activeAssoc, rq, rspCmd, ds, parser);

        perfMon.stop(activeAssoc, rq, PerfCounterEnum.C_STORE_SCP_OBJ_IN);
    }

    /**
     * Actual CStore request handling. Allows for subclasses to do some
     * preliminary work with the rq Dataset before reading and handling the
     * pixel data.
     * 
     * This method expects that the Dataset has already been parsed from the
     * Dimse InputStream, and the DcmParser is initialized already with the
     * Dataset.
     * 
     * @param activeAssoc
     *                The ActiveAssociation
     * @param rq
     *                The Dimse request
     * @param rspCmd
     *                The response Command
     * @param ds
     *                The parsed Dataset from the Dimse rq
     * @param parser
     *                The DcmParser initialized with the InputStream from the
     */
    protected void doActualCStore(ActiveAssociation activeAssoc, Dimse rq,
            Command rspCmd, Dataset ds, DcmParser parser) throws IOException,
            DcmServiceException {
        File file = null;
        boolean dcm4cheeURIReferenced = rq.getTransferSyntaxUID().equals(
                UIDs.Dcm4cheURIReferenced);
        try {
            Command rqCmd = rq.getCommand();
            Association assoc = activeAssoc.getAssociation();
            String callingAET = assoc.getCallingAET();
            
            String iuid = checkSOPInstanceUID(rqCmd, ds, callingAET);
            
            checkAppendPermission(assoc, ds);
            
            List duplicates = new QueryFilesCmd(iuid).getFileDTOs();
            if (!(duplicates.isEmpty() || storeDuplicateIfDiffMD5 || storeDuplicateIfDiffHost
                    && !containsLocal(duplicates))) {
                log.info("Received Instance[uid=" + iuid
                        + "] already exists - ignored");
                return;
            }

            service.preProcess(ds);

            if (log.isDebugEnabled()) {
                log.debug("Dataset:\n");
                log.debug(ds);
            }

            // Set original dataset
            perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET,
                    ds);

            service.logDIMSE(assoc, STORE_XML, ds);
            
            if (isCheckIncorrectWorklistEntry()
                    && checkIncorrectWorklistEntry(ds)) {
                log
                        .info("Received Instance[uid="
                                + iuid
                                + "] ignored! Reason: Incorrect Worklist entry selected!");
                return;
            }
            
            FileSystemDTO fsDTO;
            
            String filePath;
            
            byte[] md5sum = null;
            
            Dataset coerced = service.getCoercionAttributesFor(assoc,
                    STORE_XSL, ds);
            if ( coerceBeforeWrite ) {
                if (coerced != null) {
                    service.coerceAttributes(ds, coerced);
                }
                service.postCoercionProcessing(ds);
            }
            
            if (dcm4cheeURIReferenced) {
                String uri = ds.getString(Tags.RetrieveURI);
                if (uri == null) {
                    throw new DcmServiceException(
                            Status.DataSetDoesNotMatchSOPClassError,
                            "Missing (0040,E010) Retrieve URI - required for Dcm4che Retrieve URI Transfer Syntax");
                }
                if (!uri.startsWith(referencedDirectoryURI)) {
                    throw new DcmServiceException(
                            Status.DataSetDoesNotMatchSOPClassError,
                            "(0040,E010) Retrieve URI: " + uri
                            + " does not match with configured Referenced Directory Path: "
                            + referencedDirectoryPath);
                }
                filePath = uri.substring(referencedDirectoryURI.length());
                if (uri.startsWith("file:/")) {
                    file = new File(new URI(uri));
                    if (!file.isFile()) {
                        throw new DcmServiceException(Status.ProcessingFailure,
                                "File referenced by (0040,E010) Retrieve URI: "
                                        + uri + " not found!");
                    }
                }
                fsDTO = getFileSystemMgt().getFileSystemOfGroup(
                        refFileSystemGroupID, referencedDirectoryPath);
                if (file != null && readReferencedFile) {
                    log.info("M-READ " + file);
                    
                    Dataset fileDS = objFact.newDataset();
                    FileInputStream fis = new FileInputStream(file);
                    
                    try {
                        if (md5sumReferencedFile) {
                            MessageDigest digest = MessageDigest
                                    .getInstance("MD5");
                            DigestInputStream dis = new DigestInputStream(fis,
                                    digest);
                            BufferedInputStream bis = new BufferedInputStream(
                                    dis);
                            fileDS.readFile(bis, null, Tags.PixelData);
                            byte[] buf = getByteBuffer(assoc);
                            while (bis.read(buf) != -1)
                                ;
                            md5sum = digest.digest();
                        } else {
                            BufferedInputStream bis = new BufferedInputStream(
                                    fis);
                            fileDS.readFile(bis, null, Tags.PixelData);
                        }
                    } finally {
                        fis.close();
                    }
                    fileDS.putAll(ds, Dataset.REPLACE_ITEMS);
                    ds = fileDS;
                } else {
                    ds.setPrivateCreatorID(PrivateTags.CreatorID);
                    String tsuid = ds.getString(
                            PrivateTags.Dcm4cheURIReferencedTransferSyntaxUID,
                            UIDs.ImplicitVRLittleEndian);
                    ds.setFileMetaInfo(objFact.newFileMetaInfo(rqCmd
                            .getAffectedSOPClassUID(), rqCmd
                            .getAffectedSOPInstanceUID(), tsuid));
                }
            } else {
                String fsgrpid = service.selectFileSystemGroup(callingAET, ds);
                fsDTO = service.selectStorageFileSystem(fsgrpid);
                
                File baseDir = FileUtils.toFile(fsDTO.getDirectoryPath());
                
                file = makeFile(baseDir, ds, callingAET);
                
                filePath = file.getPath().substring(
                        baseDir.getPath().length() + 1).replace(
                        File.separatorChar, '/');
                
                String compressTSUID = (parser.getReadTag() == Tags.PixelData && parser
                        .getReadLength() != -1) ? compressionRules
                        .getTransferSyntaxFor(assoc, ds) : null;
                String tsuid = (compressTSUID != null) ? compressTSUID : rq
                        .getTransferSyntaxUID();
                ds.setFileMetaInfo(objFact.newFileMetaInfo(rqCmd
                        .getAffectedSOPClassUID(), rqCmd
                        .getAffectedSOPInstanceUID(), tsuid));

                perfMon.start(activeAssoc, rq,
                        PerfCounterEnum.C_STORE_SCP_OBJ_STORE);
                perfMon.setProperty(activeAssoc, rq,
                        PerfPropertyEnum.DICOM_FILE, file);
                md5sum = storeToFile(parser, ds, file, getByteBuffer(assoc));
                perfMon.stop(activeAssoc, rq,
                        PerfCounterEnum.C_STORE_SCP_OBJ_STORE);
            }
            if (md5sum != null && ignoreDuplicate(duplicates, md5sum)) {
                log.info("Received Instance[uid=" + iuid
                        + "] already exists - ignored");
                if (!dcm4cheeURIReferenced) {
                    deleteFailedStorage(file);
                }
                return;
            }
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putAE(PrivateTags.CallingAET, callingAET);
            ds.putAE(PrivateTags.CalledAET, assoc.getCalledAET());
            ds.putAE(Tags.RetrieveAET, fsDTO.getRetrieveAET());
            
            if ( ! coerceBeforeWrite ) {
                if (coerced != null) {
                    service.coerceAttributes(ds, coerced);
                }
                service.postCoercionProcessing(ds);
            }
            checkPatientIdAndName(ds, callingAET);
            Storage store = getStorage(assoc);
            String seriuid = ds.getString(Tags.SeriesInstanceUID);
            SeriesStored seriesStored = (SeriesStored) assoc.getProperty(SERIES_STORED);
            if (seriesStored != null
                    && !seriuid.equals(seriesStored.getSeriesInstanceUID())) {
                log.debug("Send SeriesStoredNotification - series changed");
                doAfterSeriesIsStored(store, assoc, seriesStored);
                seriesStored = null;
            }
            boolean newSeries = seriesStored == null;
            if (newSeries) {
                seriesStored = initSeriesStored(ds, callingAET,
                        fsDTO.getRetrieveAET());
                assoc.putProperty(SERIES_STORED, seriesStored);
                Dataset mwlFilter = service.getCoercionAttributesFor(assoc,
                        STORE2MWL_XSL, ds);
                if (mwlFilter != null) {
                    coerced = merge(coerced, mergeMatchingMWLItem(assoc, ds,
                            seriuid, mwlFilter));
                }
                service.ignorePatientIDForUnscheduled(ds,
                        Tags.RequestAttributesSeq, callingAET);
                service.supplementIssuerOfPatientID(ds, callingAET);
                service.generatePatientID(ds, ds);
            }
            appendInstanceToSeriesStored(seriesStored, ds, fsDTO);
            perfMon.start(activeAssoc, rq,
                    PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);
            
            long fileLength = file != null ? file.length() : 0L;
            Dataset coercedElements = updateDB(store, ds, fsDTO.getPk(),
                    filePath, fileLength, md5sum,
                    newSeries);
            
            ds.putAll(coercedElements, Dataset.MERGE_ITEMS);
            coerced = merge(coerced, coercedElements);
            perfMon.setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET,
                    ds);
            perfMon.stop(activeAssoc, rq,
                    PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);
            if (coerced.isEmpty()
                    || !contains(coerceWarnCallingAETs, callingAET)) {
                rspCmd.putUS(Tags.Status, Status.Success);
            } else {
                int[] coercedTags = new int[coerced.size()];
                Iterator it = coerced.iterator();
                for (int i = 0; i < coercedTags.length; i++) {
                    coercedTags[i] = ((DcmElement) it.next()).tag();
                }
                rspCmd.putAT(Tags.OffendingElement, coercedTags);
                rspCmd.putUS(Tags.Status, Status.CoercionOfDataElements);
            }
            service.postProcess(ds);
        } catch (DcmServiceException e) {
            log.warn(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw e;
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    private SeriesStored initSeriesStored(Dataset ds, String callingAET,
            String retrieveAET) {
        Dataset patAttrs = AttributeFilter.getPatientAttributeFilter().filter(ds);
        Dataset studyAttrs = AttributeFilter.getStudyAttributeFilter().filter(ds);
        Dataset seriesAttrs = AttributeFilter.getSeriesAttributeFilter().filter(ds);
        Dataset ian = DcmObjectFactory.getInstance().newDataset();
        ian.putUI(Tags.StudyInstanceUID, ds.getString(Tags.StudyInstanceUID));
        Dataset refSeries = ian.putSQ(Tags.RefSeriesSeq).addNewItem();
        refSeries.putUI(Tags.SeriesInstanceUID, ds.getString(Tags.SeriesInstanceUID));
        refSeries.putSQ(Tags.RefSOPSeq);
        Dataset pps = seriesAttrs.getItem(Tags.RefPPSSeq);
        DcmElement refPPSSeq = ian.putSQ(Tags.RefPPSSeq);
        if (pps != null) {
            if (!pps.contains(Tags.PerformedWorkitemCodeSeq)) {
                pps.putSQ(Tags.PerformedWorkitemCodeSeq);
            }
            refPPSSeq.addItem(pps);
        }
        return new SeriesStored(callingAET, retrieveAET, patAttrs, studyAttrs,
                seriesAttrs, ian);
    }

    private void appendInstanceToSeriesStored(SeriesStored seriesStored,
            Dataset ds, FileSystemDTO fsDTO) {
        Dataset refSOP = seriesStored.getIAN()
                .get(Tags.RefSeriesSeq).getItem()
                .get(Tags.RefSOPSeq).addNewItem();
        refSOP.putUI(Tags.RefSOPClassUID, ds.getString(Tags.SOPClassUID));
        refSOP.putUI(Tags.RefSOPInstanceUID, ds.getString(Tags.SOPInstanceUID));
        refSOP.putAE(Tags.RetrieveAET, fsDTO.getRetrieveAET());
        refSOP.putCS(Tags.InstanceAvailability,
                Availability.toString(fsDTO.getAvailability()));
    }

    private void checkAppendPermission(Association a, Dataset ds)
            throws Exception {
        if (service.hasUnrestrictedAppendPermissions(a.getCallingAET())) {
            return;
        }
        // only check on first instance of a series received in the same
        // association
        String seriuid = ds.getString(Tags.SeriesInstanceUID);
        SeriesStored seriesStored = (SeriesStored) a.getProperty(SERIES_STORED);
        if (seriesStored != null
                && seriuid.equals(seriesStored.getSeriesInstanceUID())) {
            return;
        }
        String suid = ds.getString(Tags.StudyInstanceUID);
        if (getStorage(a).numberOfStudyRelatedInstances(suid) == -1) {
            return;
        }

        Subject subject = (Subject) a.getProperty("user");
        if (subject == null) {
            throw new DcmServiceException(MISSING_USER_ID_ERR_STATUS,
                    MISSING_USER_ID_ERR_MSG);
        }
        if (!service.getStudyPermissionManager(a).hasPermission(suid,
                StudyPermissionDTO.APPEND_ACTION, subject)) {
            throw new DcmServiceException(NO_APPEND_PERMISSION_ERR_STATUS,
                    NO_APPEND_PERMISSION_ERR_MSG);
        }
    }

    private Dataset merge(Dataset ds, Dataset merge) {
        if (ds == null) {
            return merge;
        }
        if (merge == null) {
            return ds;
        }
        ds.putAll(merge, Dataset.MERGE_ITEMS);
        return ds;
    }

    private Dataset mergeMatchingMWLItem(Association assoc, Dataset ds,
            String seriuid, Dataset mwlFilter) {
        List mwlItems;
        log.info("Query for matching worklist entries for received Series["
                + seriuid + "]");
        try {
            mwlItems = service.findMWLEntries(mwlFilter);
        } catch (Exception e) {
            log.error(
                    "Query for matching worklist entries for received Series["
                            + seriuid + "] failed:", e);
            return null;
        }
        int size = mwlItems.size();
        log.info("" + size
                + " matching worklist entries found for received Series[ "
                + seriuid + "]");
        if (size == 0) {
            return null;
        }
        Dataset coerce = service.getCoercionAttributesFor(assoc, MWL2STORE_XSL,
                (Dataset) mwlItems.get(0));
        if (coerce == null) {
            log
                    .error("Failed to find or load stylesheet "
                            + MWL2STORE_XSL
                            + " for "
                            + assoc.getCallingAET()
                            + ". Cannot coerce object attributes with request information.");
            return null;
        }
        if (size > 1) {
            DcmElement rqAttrsSq = coerce.get(Tags.RequestAttributesSeq);
            Dataset coerce0 = coerce
                    .exclude(new int[] { Tags.RequestAttributesSeq });
            for (int i = 1; i < size; i++) {
                Dataset coerce1 = service.getCoercionAttributesFor(assoc,
                        MWL2STORE_XSL, (Dataset) mwlItems.get(i));
                if (!coerce1.match(coerce0, true, true)) {
                    log
                            .warn("Several ("
                                    + size
                                    + ") matching worklist entries "
                                    + "found for received Series[ "
                                    + seriuid
                                    + "], which differs also in attributes NOT mapped to the Request Attribute Sequence item "
                                    + "- Do not coerce object attributes with request information.");
                    return null;
                }
                if (rqAttrsSq != null) {
                    Dataset item = coerce1.getItem(Tags.RequestAttributesSeq);
                    if (item != null) {
                        rqAttrsSq.addItem(item);
                    }
                }
            }
        }
        service.coerceAttributes(ds, coerce);
        return coerce;
    }

    private boolean checkIncorrectWorklistEntry(Dataset ds) throws Exception {
        Dataset refPPS = ds.getItem(Tags.RefPPSSeq);
        if (refPPS == null) {
            return false;
        }
        String ppsUID = refPPS.getString(Tags.RefSOPInstanceUID);
        if (ppsUID == null) {
            return false;
        }
        Dataset mpps;
        try {
            mpps = getMPPSManager().getMPPS(ppsUID);
        } catch (ObjectNotFoundException e) {
            return false;
        }
        Dataset item = mpps.getItem(Tags.PPSDiscontinuationReasonCodeSeq);
        return item != null && "110514".equals(item.getString(Tags.CodeValue))
                && "DCM".equals(item.getString(Tags.CodingSchemeDesignator));
    }

    private MPPSManager getMPPSManager() throws CreateException,
            RemoteException, HomeFactoryException {
        return ((MPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME)).create();
    }

    private FileSystemMgt2 getFileSystemMgt() throws RemoteException,
            CreateException, HomeFactoryException {
        return ((FileSystemMgt2Home) EJBHomeFactory.getFactory().lookup(
                FileSystemMgt2Home.class, FileSystemMgt2Home.JNDI_NAME)).create();
    }

    private byte[] getByteBuffer(Association assoc) {
        byte[] buf = (byte[]) assoc.getProperty(RECEIVE_BUFFER);
        if (buf == null) {
            buf = new byte[service.getBufferSize()];
            assoc.putProperty(RECEIVE_BUFFER, buf);
        }
        return buf;
    }

    private boolean containsLocal(List duplicates) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (service.isFileSystemGroupLocalAccessable(
                    dto.getFileSystemGroupID()))
                return true;
        }
        return false;
    }

    private boolean ignoreDuplicate(List duplicates, byte[] md5sum) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (storeDuplicateIfDiffMD5
                    && !Arrays.equals(md5sum, dto.getFileMd5()))
                continue;
            if (storeDuplicateIfDiffHost
                    && !service.isFileSystemGroupLocalAccessable(
                            dto.getFileSystemGroupID()))
                continue;
            return true;
        }
        return false;
    }

    private void deleteFailedStorage(File file) {
        if (file == null) {
            return;
        }
        log.info("M-DELETE file:" + file);
        file.delete();
        // purge empty series and study directory
        File seriesDir = file.getParentFile();
        if (seriesDir.delete()) {
            seriesDir.getParentFile().delete();
        }
    }

    protected Dataset updateDB(Storage storage, Dataset ds, long fspk,
            String filePath, long fileLength, byte[] md5,
            boolean updateStudyAccessTime) throws DcmServiceException,
            CreateException, HomeFactoryException, IOException {
        int retry = 0;
        for (;;) {
            try {
                if (serializeDBUpdate) {
                    synchronized (storage) {
                        return storage.store(ds, fspk, filePath, fileLength,
                                md5, updateStudyAccessTime);
                    }
                } else {
                    return storage.store(ds, fspk, filePath, fileLength,
                            md5, updateStudyAccessTime);
                }
            } catch (Exception e) {
                ++retry;
                if (retry > updateDatabaseMaxRetries) {
                    service.getLog().error(
                            "failed to update DB with entries for received "
                                    + filePath, e);
                    throw new DcmServiceException(Status.ProcessingFailure, e);
                }
                maxCountUpdateDatabaseRetries = Math.max(retry,
                        maxCountUpdateDatabaseRetries);
                service.getLog().warn(
                        "failed to update DB with entries for received "
                                   + filePath + " - retry", e);
                try {
                    Thread.sleep(updateDatabaseRetryInterval);
                } catch (InterruptedException e1) {
                    log.warn("update Database Retry Interval interrupted:", e1);
                }
            }
        }
    }

    Storage getStorage(Association assoc) throws RemoteException,
            CreateException, HomeFactoryException {
        Storage store = (Storage) assoc.getProperty(StorageHome.JNDI_NAME);
        if (store == null) {
            store = service.getStorage();
            assoc.putProperty(StorageHome.JNDI_NAME, store);
        }
        return store;
    }

    File makeFile(File basedir, Dataset ds, String callingAET) throws Exception {
        Calendar date = Calendar.getInstance();
        StringBuffer filePath = new StringBuffer();
        if( sourceAETInFilePath && callingAET != null) {
        	filePath.append(callingAET);
            filePath.append(File.separatorChar);
        }
        if (studyDateInFilePath) {
            Date studyDate = ds.getDateTime(Tags.StudyDate, Tags.StudyTime);
            if (studyDate != null)
                date.setTime(studyDate);
        }
        if (yearInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.YEAR)));
            filePath.append(File.separatorChar);
        }
        if (monthInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.MONTH) + 1));
            filePath.append(File.separatorChar);
        }
        if (dayInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.DAY_OF_MONTH)));
            filePath.append(File.separatorChar);
        }
        if (hourInFilePath) {
            filePath.append(String.valueOf(date.get(Calendar.HOUR_OF_DAY)));
            filePath.append(File.separatorChar);
        }
        filePath.append(FileUtils.toHex(ds.getString(Tags.StudyInstanceUID)
                .hashCode()));
        filePath.append(File.separatorChar);
        filePath.append(FileUtils.toHex(ds.getString(Tags.SeriesInstanceUID)
                .hashCode()));
        File dir = new File(basedir, filePath.toString());
        return FileUtils.createNewFile(dir, ds.getString(Tags.SOPInstanceUID)
                .hashCode());
    }

    private byte[] storeToFile(DcmParser parser, Dataset ds, File file,
            byte[] buffer) throws Exception {
        log.info("M-WRITE file:" + file);
        MessageDigest md = null;
        BufferedOutputStream bos = null;
        if (service.isMd5sum()) {
            md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(
                    new FileOutputStream(file), md);
            bos = new BufferedOutputStream(dos, buffer);
        } else {
            bos = new BufferedOutputStream(new FileOutputStream(file), buffer);
        }
        try {
            DcmDecodeParam decParam = parser.getDcmDecodeParam();
            String tsuid = ds.getFileMetaInfo().getTransferSyntaxUID();
            DcmEncodeParam encParam = DcmEncodeParam.valueOf(tsuid);
            CompressCmd compressCmd = null;
            if (!decParam.encapsulated && encParam.encapsulated) {
                compressCmd = CompressCmd.createCompressCmd(ds, tsuid);
                compressCmd.coerceDataset(ds);
            }
            ds.writeFile(bos, encParam);
            if (parser.getReadTag() == Tags.PixelData) {
                int len = parser.getReadLength();
                InputStream in = parser.getInputStream();
                if (encParam.encapsulated) {
                    ds.writeHeader(bos, encParam, Tags.PixelData, VRs.OB, -1);
                    if (decParam.encapsulated) {
                        parser.parseHeader();
                        while (parser.getReadTag() == Tags.Item) {
                            len = parser.getReadLength();
                            ds.writeHeader(bos, encParam, Tags.Item, VRs.NONE,
                                    len);
                            bos.copyFrom(in, len);
                            parser.parseHeader();
                        }
                    } else {
                        int read = compressCmd.compress(decParam.byteOrder,
                                parser.getInputStream(), bos);
                        skipFully(in, parser.getReadLength() - read);
                    }
                    ds.writeHeader(bos, encParam, Tags.SeqDelimitationItem,
                            VRs.NONE, 0);
                } else {
                    ds.writeHeader(bos, encParam, Tags.PixelData, parser
                            .getReadVR(), len);
                    bos.copyFrom(in, len);
                }
                parser.parseDataset(decParam, -1);
                ds.subSet(Tags.PixelData, -1).writeDataset(bos, encParam);
            }
        } finally {
            // We don't want to ignore the IOException since in rare cases the
            // close() may cause
            // exception due to running out of physical space while the File
            // System still holds
            // some internally cached data. In this case, we do want to fail
            // this C-STORE.
            bos.close();
        }
        return md != null ? md.digest() : null;
    }

    private static void skipFully(InputStream in, int n) throws IOException {
        int remaining = n;
        int skipped = 0;
        while (remaining > 0) {
            if ((skipped = (int) in.skip(remaining)) == 0) {
                throw new EOFException();
            }
            remaining -= skipped;
        }
    }

    private String checkSOPInstanceUID(Command rqCmd, Dataset ds, String aet)
            throws DcmServiceException {
        String cuid = checkNotNull(ds.getString(Tags.SOPClassUID),
                "Missing SOP Class UID (0008,0016)");
        String iuid = checkNotNull(ds.getString(Tags.SOPInstanceUID),
                "Missing SOP Instance UID (0008,0018)");
        checkNotNull(ds.getString(Tags.StudyInstanceUID),
                "Missing Study Instance UID (0020,000D)");
        checkNotNull(ds.getString(Tags.SeriesInstanceUID),
                "Missing Series Instance UID (0020,000E)");
        if (!rqCmd.getAffectedSOPInstanceUID().equals(iuid)) {
            String prompt = "SOP Instance UID in Dataset [" + iuid
                    + "] differs from Affected SOP Instance UID["
                    + rqCmd.getAffectedSOPInstanceUID() + "]";
            log.warn(prompt);
            if (!contains(acceptMismatchIUIDCallingAETs, aet)) {
                throw new DcmServiceException(
                        Status.DataSetDoesNotMatchSOPClassError, prompt);
            }
        }
        if (!rqCmd.getAffectedSOPClassUID().equals(cuid)) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError,
                    "SOP Class UID in Dataset differs from Affected SOP Class UID");
        }
        return iuid;
    }

    private static String checkNotNull(String val, String msg)
            throws DcmServiceException {
        if (val == null) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError, msg);
        }
        return val;
    }

    private void checkPatientIdAndName(Dataset ds, String aet)
            throws DcmServiceException, HomeFactoryException, RemoteException,
            CreateException, FinderException {
        String pid = ds.getString(Tags.PatientID);
        String pname = ds.getString(Tags.PatientName);
        if (pid == null && !acceptMissingPatientID) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError,
                    "Acceptance of objects without Patient ID is disabled");
        }
        if (pname == null && !acceptMissingPatientName) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError,
                    "Acceptance of objects without Patient Name is disabled");
        }
    }

    private boolean contains(Object[] a, Object e) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(e)) {
                return true;
            }
        }
        return false;
    }

    // Implementation of AssociationListener

    public void write(Association src, PDU pdu) {
        if (pdu instanceof AAssociateAC)
            perfMon.assocEstEnd(src, Command.C_STORE_RQ);
    }

    public void received(Association src, PDU pdu) {
        if (pdu instanceof AAssociateRQ)
            perfMon.assocEstStart(src, Command.C_STORE_RQ);
    }

    public void write(Association src, Dimse dimse) {
    }

    public void received(Association src, Dimse dimse) {
    }

    public void error(Association src, IOException ioe) {
    }

    public void closing(Association assoc) {
        if (assoc.getAAssociateAC() != null)
            perfMon.assocRelStart(assoc, Command.C_STORE_RQ);

        SeriesStored seriesStored = (SeriesStored) assoc.getProperty(SERIES_STORED);
        if (seriesStored != null) {
            try {
                log.debug("Send SeriesStoredNotification - association closed");
                doAfterSeriesIsStored(getStorage(assoc), assoc, seriesStored);
            } catch (Exception e) {
                log.error("Clean up on Association close failed:", e);
            }
        }
    }

    public void closed(Association assoc) {
        if (assoc.getAAssociateAC() != null)
            perfMon.assocRelEnd(assoc, Command.C_STORE_RQ);
    }

    /**
     * Finalize a stored series.
     * <p>
     * <dl>
     * <dd>1) Update derived Study and Series fields in DB</dd>
     * <dd>1) Create Audit log entries for instances stored</dd>
     * <dd>2) send SeriesStored JMX notification</dd>
     * <dd>3) Set Series/Instance status in DB from RECEIVED to STORED</dd>
     * </dl>
     */
    protected void doAfterSeriesIsStored(Storage store, Association assoc,
            SeriesStored seriesStored) throws RemoteException, FinderException {
        store.updateDerivedStudyAndSeriesFields(
                seriesStored.getSeriesInstanceUID());
        service.logInstancesStored(assoc == null ? null : assoc.getSocket(), seriesStored);
        service.sendJMXNotification(seriesStored);
        store.commitSeriesStored(seriesStored);
    }
    
    //The two methods are added by YangLin@cn-arg.com on 01.20.2009
    //For accessing private fields from sub-type
    protected String getReferencedDirectoryURI() {
        return referencedDirectoryURI;
    }
   
    protected PerfMonDelegate getPerfMon() {
        return perfMon;
    }

}