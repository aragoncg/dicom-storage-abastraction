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
 * ACG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Lin Yang <YangLin@cn-arg.com>
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.security.auth.Subject;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmDecodeParam;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmParser;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.codec.CompressCmd;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.dcm.DBConUtil;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.ejb.interfaces.MPPSManager;
import org.dcm4chex.archive.ejb.interfaces.MPPSManagerHome;
import org.dcm4chex.archive.ejb.interfaces.Storage;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.jdbc.QueryFilesCmd;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.16.2009
 * 
 * This is the main class that implements the storage of Dicom image
 * in Oracle 11g database. Most of the code comes from the original
 * StoreScp.java. The modification mainly happens in the doActualCStore 
 * method and storeToDB method.
 */
public class DBStoreScp extends StoreScp {

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
    
    private StSCPDBImpl dbStore;
    
    /**
     * Constructor of DBStoreScp. 
     */
    public DBStoreScp(StoreScpService service) {
        super(service);
        dbStore = new StSCPDBImpl();
    }
    
    /**
     * Store a Dicom image and its metadata into database.
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

            //Validate sop class id, sop instance id, serial id, study id
            String iuid = checkSOPInstanceUID(rqCmd, ds, callingAET);

            //Validate append permission of the same association
            checkAppendPermission(assoc, ds);

            List duplicates = new QueryFilesCmd(iuid).getFileDTOs();
            if (!(duplicates.isEmpty() || isStoreDuplicateIfDiffMD5() || isStoreDuplicateIfDiffHost()
                    && !containsLocal(duplicates))) {
                service.getLog().info(
                        "Received Instance[uid=" + iuid
                                + "] already exists - ignored");
                return;
            }

            service.preProcess(ds);

            if (service.getLog().isDebugEnabled()) {
                service.getLog().debug("Dataset:\n");
                service.getLog().debug(ds);
            }

            getPerfMon().setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET,
                    ds);

            service.logDIMSE(assoc, STORE_XML, ds);

            if (isCheckIncorrectWorklistEntry()
                    && checkIncorrectWorklistEntry(ds)) {
                service
                        .getLog()
                        .info(
                                "Received Instance[uid="
                                        + iuid
                                        + "] ignored! Reason: Incorrect Worklist entry selected!");
                return;
            }

            FileSystemDTO fsDTO = null;

            String filePath = null;
            
            long fileLength = new Long(0);

            byte[] md5sum = null;
            
            Dataset coerced = service.getCoercionAttributesFor(assoc,
                    STORE_XSL, ds);
            if ( isCoerceBeforeWrite() ) {
                if (coerced != null) {
                    service.coerceAttributes(ds, coerced);
                }
                service.postCoercionProcessing(ds);
            }

            if (dcm4cheeURIReferenced) {
                //Acquire storage URI
                String uri = ds.getString(Tags.RetrieveURI);
                if (uri == null) {
                    throw new DcmServiceException(
                            Status.DataSetDoesNotMatchSOPClassError,
                            "Missing (0040,E010) Retrieve URI - required for Dcm4che Retrieve URI Transfer Syntax");
                }
                if (!uri.startsWith(getReferencedDirectoryURI())) {
                    throw new DcmServiceException(
                            Status.DataSetDoesNotMatchSOPClassError,
                            "(0040,E010) Retrieve URI: "
                                    + uri
                                    + " does not match with configured Referenced Directory Path: "
                                    + getReferencedDirectoryPath());
                }

                //Acquire storage path
                filePath = uri.substring(getReferencedDirectoryURI().length());

                //Validate if the file specified by the URI is a real file
                if (uri.startsWith("file:/")) {
                    file = new File(new URI(uri));
                    if (!file.isFile()) {
                        throw new DcmServiceException(Status.ProcessingFailure,
                                "File referenced by (0040,E010) Retrieve URI: "
                                        + uri + " not found!");
                    }
                }

                fsDTO = getFileSystemMgt().getFileSystemOfGroup(
                		getReferencedFileSystemGroupID(),getReferencedDirectoryPath());

                if (file != null && isReadReferencedFile()) {
                    service.getLog().info("M-READ " + file);
                    
                    fileLength = file.length();
                    
                    //Create a new empty dataset
                    Dataset fileDS = objFact.newDataset();

                    FileInputStream fis = new FileInputStream(file);

                    try {
                        if (isMd5sumReferencedFile()) {
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
                } else {//If reading referenced file is not required generate
                        //metaInfo individually
                    ds.setPrivateCreatorID(PrivateTags.CreatorID);
                    String tsuid = ds.getString(
                            PrivateTags.Dcm4cheURIReferencedTransferSyntaxUID,
                            UIDs.ImplicitVRLittleEndian);
                    ds.setFileMetaInfo(objFact.newFileMetaInfo(rqCmd
                            .getAffectedSOPClassUID(), rqCmd
                            .getAffectedSOPInstanceUID(), tsuid));
                }
            } else {//If not tianiURIReferenced
            	String fsgrpid = service.selectFileSystemGroup(callingAET, ds);
                fsDTO = service.selectStorageFileSystem(fsgrpid);
                
                File baseDir = FileUtils.toFile(fsDTO.getDirectoryPath());
                // create output file
                file = makeFile(baseDir, ds, callingAET);

                filePath = file.getPath().substring(
                        baseDir.getPath().length() + 1).replace(
                        File.separatorChar, '/');

                //Generate metaInfo
                String compressTSUID = (parser.getReadTag() == Tags.PixelData && parser
                        .getReadLength() != -1) ? getCompressionRules()
                        .getTransferSyntaxFor(assoc, ds) : null;
                String tsuid = (compressTSUID != null) ? compressTSUID : rq
                        .getTransferSyntaxUID();
                ds.setFileMetaInfo(objFact.newFileMetaInfo(rqCmd
                        .getAffectedSOPClassUID(), rqCmd
                        .getAffectedSOPInstanceUID(), tsuid));

                getPerfMon().start(activeAssoc, rq,
                        PerfCounterEnum.C_STORE_SCP_OBJ_STORE);

                getPerfMon().setProperty(activeAssoc, rq,
                        PerfPropertyEnum.DICOM_FILE, getStoreItem());
                
                StringBuilder tempPath = new StringBuilder(); 
                tempPath.append(filePath);
                
                StringBuilder tempLength = new StringBuilder();
                
                //Store image data to database
                md5sum = storeToDB(parser, ds, file, tempPath, tempLength, getByteBuffer(assoc));
                
                filePath = tempPath.toString();
                
                fileLength = Long.valueOf(tempLength.toString()); 
                
                //Keep the file not null in case to delete it
                file = new File(baseDir.getPath() + filePath);
                
                getPerfMon().stop(activeAssoc, rq,
                        PerfCounterEnum.C_STORE_SCP_OBJ_STORE);
            }

            if (md5sum != null && ignoreDuplicate(duplicates, md5sum)) {
                service.getLog().info(
                        "Received Instance[uid=" + iuid
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

            if ( ! isCoerceBeforeWrite() ) {
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
                service.getLog().debug("Send SeriesStoredNotification - series changed");
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

            getPerfMon().start(activeAssoc, rq,
                    PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);

            //Update metaInfo of image in database
            Dataset coercedElements = updateDB(store, ds, fsDTO.getPk(),
                    filePath, fileLength, md5sum,
                    newSeries);

            ds.putAll(coercedElements, Dataset.MERGE_ITEMS);
            coerced = merge(coerced, coercedElements);
            getPerfMon().setProperty(activeAssoc, rq, PerfPropertyEnum.REQ_DATASET,
                    ds);
            getPerfMon().stop(activeAssoc, rq,
                    PerfCounterEnum.C_STORE_SCP_OBJ_REGISTER_DB);
            if (coerced.isEmpty()
                    || !contains(
                            splitStringToArray(getCoerceWarnCallingAETs()),
                            callingAET)) {
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
            service.getLog().warn(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw e;
        } catch (Throwable e) {
            service.getLog().error(e.getMessage(), e);
            if (!dcm4cheeURIReferenced) {
                deleteFailedStorage(file);
            }
            throw new DcmServiceException(Status.ProcessingFailure, e);
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
            service.getLog().warn(prompt);
            if (!contains(
                    splitStringToArray(getAcceptMismatchIUIDCallingAETs()), aet)) {
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

    private boolean contains(Object[] a, Object e) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].equals(e)) {
                return true;
            }
        }
        return false;
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

    private boolean containsLocal(List duplicates) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (service.isFileSystemGroupLocalAccessable(
                    dto.getFileSystemGroupID()))
                return true;
        }
        return false;
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

    private FileSystemMgt2 getFileSystemMgt() throws RemoteException,
            CreateException, HomeFactoryException {
        return ((FileSystemMgt2Home) EJBHomeFactory.getFactory().lookup(
                FileSystemMgt2Home.class, FileSystemMgt2Home.JNDI_NAME)).create();
    }

    private MPPSManager getMPPSManager() throws CreateException,
            RemoteException, HomeFactoryException {
        return ((MPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                MPPSManagerHome.class, MPPSManagerHome.JNDI_NAME)).create();
    }

    private byte[] getByteBuffer(Association assoc) {
        byte[] buf = (byte[]) assoc.getProperty(RECEIVE_BUFFER);
        if (buf == null) {
            buf = new byte[service.getBufferSize()];
            assoc.putProperty(RECEIVE_BUFFER, buf);
        }
        return buf;
    }
    
    /**
     * Write a Dicom image into database and update its metadata.
     */
    private byte[] storeToDB(DcmParser parser, Dataset ds, File file,
            StringBuilder filePath, StringBuilder fileLength, byte[] buffer) throws Exception {
        service.getLog().info("M-WRITE to Database Oracle.11g");
        
        //Acquire OutputStream of the new inserted empty Dicom object in database
        int id = dbStore.getNewId();
        OutputStream ops = dbStore.getOutputStream(id);
        
        MessageDigest md = null;
        BufferedOutputStream bos = null;
        if (service.isMd5sum()) {
            md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(ops, md);
            bos = new BufferedOutputStream(dos, buffer);
        } else {
            bos = new BufferedOutputStream(ops, buffer);
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
            if (ops != null)
                ops.close();
        }
        
        //Extract and update the metadata of the new inserted Dicom image in database
        fileLength.append(dbStore.setProperties(id));
        filePath.append(DBConUtil.DBSTORE_MARK + id);
        file.delete();
        
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
    
    private boolean ignoreDuplicate(List duplicates, byte[] md5sum) {
        for (int i = 0, n = duplicates.size(); i < n; ++i) {
            FileDTO dto = (FileDTO) duplicates.get(i);
            if (isStoreDuplicateIfDiffMD5()
                    && !Arrays.equals(md5sum, dto.getFileMd5()))
                continue;
            if (isStoreDuplicateIfDiffHost()
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
        FileUtils.delete(file, true);
    }

    private void checkPatientIdAndName(Dataset ds, String aet)
            throws DcmServiceException, HomeFactoryException, RemoteException,
            CreateException, FinderException {
        String pid = ds.getString(Tags.PatientID);
        String pname = ds.getString(Tags.PatientName);
        if (pid == null && !isAcceptMissingPatientID()) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError,
                    "Acceptance of objects without Patient ID is disabled");
        }
        if (pname == null && !isAcceptMissingPatientName()) {
            throw new DcmServiceException(
                    Status.DataSetDoesNotMatchSOPClassError,
                    "Acceptance of objects without Patient Name is disabled");
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
        service.getLog().info(
                "Query for matching worklist entries for received Series["
                        + seriuid + "]");
        try {
            mwlItems = service.findMWLEntries(mwlFilter);
        } catch (Exception e) {
            service.getLog().error(
                    "Query for matching worklist entries for received Series["
                            + seriuid + "] failed:", e);
            return null;
        }
        int size = mwlItems.size();
        service.getLog().info(
                        "" + size
                        + " matching worklist entries found for received Series[ "
                        + seriuid + "]");
        if (size == 0) {
            return null;
        }
        Dataset coerce = service.getCoercionAttributesFor(assoc, MWL2STORE_XSL,
                (Dataset) mwlItems.get(0));
        if (coerce == null) {
            service.getLog().error(
                            "Failed to find or load stylesheet "
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
                    service.getLog().warn(
                                    "Several ("
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
    
    private Object getStoreItem() {
        return "StoreIntoDB";
    }
    
    private String[] splitStringToArray(String aets) {
        return StringUtils.split(aets, '\\');
    }
}
