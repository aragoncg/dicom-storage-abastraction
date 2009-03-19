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
 * Agfa HealthCare.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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
package org.dcm4chex.archive.ejb.session;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.DeleteStudyOrder;
import org.dcm4chex.archive.common.FileStatus;
import org.dcm4chex.archive.common.FileSystemStatus;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileLocal;
import org.dcm4chex.archive.ejb.interfaces.FileLocalHome;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocal;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocalHome;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Local;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivateFileLocal;
import org.dcm4chex.archive.ejb.interfaces.PrivateFileLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyOnFileSystemLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyOnFileSystemLocalHome;
import org.dcm4chex.archive.exceptions.ConcurrentStudyStorageException;
import org.dcm4chex.archive.exceptions.NoSuchStudyException;

/**
 * @ejb.bean name="FileSystemMgt2" type="Stateless" view-type="both"
 *     jndi-name="ejb/FileSystemMgt2"
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="FileSystem" view-type="local"
 *              ref-name="ejb/FileSystem"
 * @ejb.ejb-ref ejb-name="File" view-type="local" ref-name="ejb/File"
 * @ejb.ejb-ref ejb-name="PrivateFile" view-type="local"
 *              ref-name="ejb/PrivateFile"
 * @ejb.ejb-ref ejb-name="Study" ref-name="ejb/Study" view-type="local"
 * @ejb.ejb-ref ejb-name="Series" ref-name="ejb/Series" view-type="local"
 * @ejb.ejb-ref ejb-name="Instance" ref-name="ejb/Instance" view-type="local"
 * @ejb.ejb-ref ejb-name="StudyOnFileSystem" ref-name="ejb/StudyOnFileSystem"
 *              view-type="local"
 * 
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 13, 2008
 */
public abstract class FileSystemMgt2Bean implements SessionBean {

    private static Logger log = Logger.getLogger(FileSystemMgt2Bean.class);

    private static final int[] IAN_PAT_TAGS = { Tags.SpecificCharacterSet,
            Tags.PatientName, Tags.PatientID };

    private SessionContext ctx;
    private FileSystemLocalHome fileSystemHome;
    private FileLocalHome fileHome;
    private PrivateFileLocalHome privFileHome;
    private StudyLocalHome studyHome;
    private SeriesLocalHome seriesHome;
    private InstanceLocalHome instHome;
    private StudyOnFileSystemLocalHome sofHome;

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            this.fileSystemHome = (FileSystemLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/FileSystem");
            this.fileHome = (FileLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/File");
            this.privFileHome = (PrivateFileLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/PrivateFile");
            this.studyHome = (StudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Study");
            this.seriesHome = (SeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Series");
            this.instHome = (InstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Instance");
            this.sofHome = (StudyOnFileSystemLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/StudyOnFileSystem");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    public void unsetSessionContext() {
       ctx = null;
       fileSystemHome = null;
       fileHome = null;
       privFileHome = null;
       studyHome = null;
       seriesHome = null;
       instHome = null;
       sofHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO addFileSystem(FileSystemDTO dto)
            throws CreateException {
        return fileSystemHome.create(dto).toDTO();
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO removeFileSystem(String groupID, String dirPath)
            throws FinderException, RemoveException {
        FileSystemLocal fs = fileSystemHome
                .findByGroupIdAndDirectoryPath(groupID, dirPath);
        FileSystemDTO dto = fs.toDTO();
        removeFileSystem(fs);
        return dto;
    }

    private void removeFileSystem(FileSystemLocal fs)
            throws RemoveException, FinderException {
        FileSystemLocal next = fs.getNextFileSystem();
        if (next != null && fs.isIdentical(next)) {
            next = null;
        }
        Collection c = fs.getPreviousFileSystems();
        FileSystemLocal[] prevs = (FileSystemLocal[])
                c.toArray(new FileSystemLocal[c.size()]);
        for (int i = 0; i < prevs.length; i++) {
            prevs[i].setNextFileSystem(next);
        }
        if (fs.getStatus() == FileSystemStatus.DEF_RW && next != null
                && next.getStatus() == FileSystemStatus.RW) {
            next.setStatus(FileSystemStatus.DEF_RW);
        }
        fs.remove();
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO getFileSystem(long pk) throws FinderException {
        return fileSystemHome.findByPrimaryKey(new Long(pk)).toDTO();
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO getFileSystemOfGroup(String groupID, String path)
            throws FinderException {
        return toDTO(
                fileSystemHome.findByGroupIdAndDirectoryPath(groupID, path));
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO[] getAllFileSystems() throws FinderException {
        return toDTO(fileSystemHome.findAll());
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO[] getFileSystemsOfGroup(String groupId)
            throws FinderException {
        return toDTO(fileSystemHome.findByGroupId(groupId));
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO[] getRWFileSystemsOfGroup(String groupId)
            throws FinderException {
        return toDTO(fileSystemHome.findRWByGroupId(groupId));
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO getDefRWFileSystemsOfGroup(String groupId)
            throws FinderException {
        return toDTO(selectDefRWFileSystemsOfGroup(groupId));
    }

    private FileSystemLocal selectDefRWFileSystemsOfGroup(String groupId)
            throws FinderException {
        Collection c = fileSystemHome.findByGroupIdAndStatus(groupId,
                FileSystemStatus.DEF_RW);
        if (!c.isEmpty()) {
            return (FileSystemLocal) c.iterator().next();
        }
        c = fileSystemHome.findByGroupIdAndStatus(groupId, FileSystemStatus.RW);
        if (c.isEmpty()) {
            return null;
        }
        FileSystemLocal fs = (FileSystemLocal) c.iterator().next();
        log.info("Update status of " + fs.asString() + " to RW+");
        fs.setStatus(FileSystemStatus.DEF_RW);
        return fs;
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO updateFileSystemStatus(long pk, int status)
            throws FinderException {
        return updateFileSystemStatus(
                fileSystemHome.findByPrimaryKey(new Long(pk)), status);
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO updateFileSystemStatus(String groupID, String dirPath,
            int status) throws FinderException {
        return updateFileSystemStatus(
                fileSystemHome.findByGroupIdAndDirectoryPath(groupID, dirPath),
                status);
    }

    private FileSystemDTO updateFileSystemStatus(FileSystemLocal fs, int status)
            throws FinderException {
        if (status != fs.getStatus()) {
            if (status == FileSystemStatus.DEF_RW) {
                // set status of previous default RW file system(s) to RW
                Collection c = fileSystemHome.findByGroupIdAndStatus(
                        fs.getGroupID(), FileSystemStatus.DEF_RW);
                for (Iterator iterator = c.iterator(); iterator.hasNext();) {
                    ((FileSystemLocal) iterator.next())
                            .setStatus(FileSystemStatus.RW);
                }
            }
            fs.setStatus(status);
        }
        return fs.toDTO();
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="NotSupported"
     */
    public FileSystemDTO updateFileSystemRetrieveAET(String groupID,
            String dirPath, String retrieveAET, int limit)
            throws FinderException {
        FileSystemLocal fs =
                fileSystemHome.findByGroupIdAndDirectoryPath(groupID, dirPath);
        String oldAET = fs.getRetrieveAET();
        if (!retrieveAET.equals(oldAET)) {
            fs.setRetrieveAET(retrieveAET);
            updateRetrieveAETForStudyOnFileSystem(fs, oldAET, retrieveAET, limit);
        }
        return fs.toDTO();
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="NotSupported"
     */
    public int updateFileSystemRetrieveAET(String oldAET, String newAET,
            int limit) throws FinderException {
        if (oldAET.equals(newAET)) {
            return 0;
        }
        Collection fss = fileSystemHome.findByRetrieveAET(oldAET);
        for (Iterator it = fss.iterator(); it.hasNext();) {
            FileSystemLocal fs = (FileSystemLocal) it.next();
            fs.setRetrieveAET(newAET);
            updateRetrieveAETForStudyOnFileSystem(fs, oldAET, newAET, limit);
        }
        return fss.size();
    }

    private void updateRetrieveAETForStudyOnFileSystem(FileSystemLocal fs,
            String oldAET, String newAET, int batchsize) throws FinderException {
        for (int offset = 0; ; offset += batchsize) {
            Collection studies = studyHome.findStudiesWithFilesOnFileSystem(fs,
                    offset, batchsize);
            for (Iterator it = studies.iterator(); it.hasNext();) {
                StudyLocal study = (StudyLocal) it.next();
                FileSystemMgt2Local ejb =
                    (FileSystemMgt2Local) ctx.getEJBLocalObject();
                study.updateRetrieveAETs(oldAET, newAET);
            }
            if (studies.size() < batchsize) {
                break;
            }
        }
    }

    /**
     * @ejb.interface-method
     * @ejb.transaction type="NotSupported"
     */
    public FileSystemDTO updateFileSystemAvailability(String groupID,
            String dirPath, int availability, int availabilityOfExtRetr,
            int limit) throws FinderException {
        FileSystemLocal fs =
                fileSystemHome.findByGroupIdAndDirectoryPath(groupID, dirPath);
        if (fs.getAvailability() != availability) {
            fs.setAvailability(availability);
            updateAvailabilityForStudyOnFileSystem(fs, availabilityOfExtRetr,
                    limit);
        }
        return fs.toDTO();
    }

    private void updateAvailabilityForStudyOnFileSystem(FileSystemLocal fs,
            int availabilityOfExtRetr, int batchsize) throws FinderException {
        for (int offset = 0; ; offset += batchsize) {
            Collection studies = studyHome.findStudiesWithFilesOnFileSystem(fs,
                    offset, batchsize);
            for (Iterator it = studies.iterator(); it.hasNext();) {
                StudyLocal study = (StudyLocal) it.next();
                FileSystemMgt2Local ejb =
                    (FileSystemMgt2Local) ctx.getEJBLocalObject();
                ejb.updateAvailabilityForStudy(study, availabilityOfExtRetr);
            }
            if (studies.size() < batchsize) {
                break;
            }
        }
    }

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.transaction type="RequiresNew"
     */
    public boolean updateAvailabilityForStudy(StudyLocal study,
            int availabilityOfExtRetr) throws FinderException {
        Collection series = study.getSeries();
        boolean updated = false;
        boolean updateStudy = false;
        for (Iterator serit = series.iterator(); serit.hasNext();) {
            SeriesLocal ser = (SeriesLocal) serit.next();
            Collection insts = ser.getInstances();
            boolean updateSeries = false;
            for (Iterator instit = insts.iterator(); instit.hasNext();) {
                InstanceLocal inst = (InstanceLocal) instit.next();
                if (inst.updateAvailability(availabilityOfExtRetr)) {
                    updateSeries = updated = true;
                }
            }
            if (updateSeries) {
                if (ser.updateAvailability()) {
                    updateStudy = true;
                }
            }
        }
        if (updateStudy) {
            study.updateAvailability();
        }
        return updated;
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO linkFileSystems(String groupID, String dirPath,
            String next) throws FinderException {
        FileSystemLocal prevfs = fileSystemHome
                .findByGroupIdAndDirectoryPath(groupID, dirPath);
        FileSystemLocal nextfs = (next != null && next.length() != 0)
                ? fileSystemHome.findByGroupIdAndDirectoryPath(groupID, next)
                : null;
        prevfs.setNextFileSystem(nextfs);
        return prevfs.toDTO();
    }

    /**
     * @ejb.interface-method
     */
    public FileSystemDTO addAndLinkFileSystem(FileSystemDTO dto)
            throws FinderException, CreateException {
        FileSystemLocal prev = selectDefRWFileSystemsOfGroup(dto.getGroupID());
        if (prev == null) {
            dto.setStatus(FileSystemStatus.DEF_RW);
        }
        FileSystemLocal fs = fileSystemHome.create(dto);
        if (prev != null) {
            FileSystemLocal prev0 = prev;
            FileSystemLocal next;
            while ((next = prev.getNextFileSystem()) != null
                    && !next.isIdentical(prev0)) {
                prev = next;
            }
            prev.setNextFileSystem(fs);
            fs.setNextFileSystem(next);
        }
        return fs.toDTO();
    }

    /**
     * @ejb.interface-method
     */
    public long sizeOfFilesCreatedAfter(long pk, long after)
            throws FinderException {
        return fileSystemHome
                .sizeOfFilesCreatedAfter(new Long(pk), new Timestamp(after));
    }

    private static FileSystemDTO toDTO(FileSystemLocal fs) {
        return fs != null ? fs.toDTO() : null;
    }

    private static FileSystemDTO[] toDTO(Collection c) {
        FileSystemDTO[] dto = new FileSystemDTO[c.size()];
        Iterator it = c.iterator();
        for (int i = 0; i < dto.length; i++) {
            dto[i] = ((FileSystemLocal) it.next()).toDTO();
        }
        return dto;
    }

    /**
     * @ejb.interface-method
     */
    public long getStudySize(DeleteStudyOrder order) throws FinderException {
        return studyHome.selectStudySize(order.getStudyPk(), order.getFsPk());
    }

    /**
     * @ejb.interface-method
     */
    public boolean removeStudyOnFSRecord(DeleteStudyOrder order)
            throws RemoveException, FinderException {
        StudyOnFileSystemLocal sof = sofHome.findByPrimaryKey(order.getSoFsPk());
        if (sof.getAccessTime().getTime() > order.getAccessTime()) {
            log.info("Study[pk=" + order.getStudyPk() + "] on FileSystem[pk="
                    + order.getFsPk()
                    + "] may have updated after check of deletion constraints"
                    + " -> do not schedule study for deletion.");
            return false;
        }
        sof.remove();
        return true;
    }

    /**
     * @ejb.interface-method
     */
    public void createStudyOnFSRecord(DeleteStudyOrder order)
            throws CreateException, FinderException {
        sofHome.create(studyHome.findByPrimaryKey(order.getStudyPk()),
                fileSystemHome.findByPrimaryKey(order.getFsPk()));
    }

    /**    
     * @ejb.interface-method
     */
    public Collection createDeleteOrdersForStudiesOnFSGroup(
            String fsGroup, long minAccessTime, long maxAccessTime, int limit,
            boolean externalRetrieveable, boolean storageNotCommited,
            boolean copyOnMedia, String copyOnFSGroup, boolean copyArchived,
            boolean copyOnReadOnlyFS) throws FinderException {
        return createDeleteOrders(sofHome.findByFSGroupAndAccessedBetween(fsGroup,
                new Timestamp(minAccessTime), new Timestamp(maxAccessTime), limit),
                externalRetrieveable, storageNotCommited, copyOnMedia,
                copyOnFSGroup, copyArchived, copyOnReadOnlyFS);
    }

    /**    
     * @ejb.interface-method
     */
    public Collection createDeleteOrdersForStudyOnFSGroup(String suid,
            String fsGroup) throws FinderException {
        Collection sofs = sofHome.findByStudyIUIDAndFSGroup(suid, fsGroup);
        Collection orders = new ArrayList(sofs.size());
        for (Iterator iter = sofs.iterator(); iter.hasNext();) {
            StudyOnFileSystemLocal sof = (StudyOnFileSystemLocal) iter.next();
            orders.add(new DeleteStudyOrder(sof.getPk(),
                    sof.getStudy().getPk(), sof.getFileSystem().getPk(),
                    sof.getAccessTime().getTime()));
        }
        return orders;
    }

    private Collection createDeleteOrders(
            Collection sofs, boolean externalRetrieveable,
            boolean storageNotCommited, boolean copyOnMedia,
            String copyOnFSGroup, boolean copyArchived,
            boolean copyOnReadOnlyFS) throws FinderException {
        Collection orders = new ArrayList(sofs.size());
        for (Iterator iter = sofs.iterator(); iter.hasNext();) {
            StudyOnFileSystemLocal sof = (StudyOnFileSystemLocal) iter.next();
            if (sof.matchDeleteConstrains(externalRetrieveable,
                    storageNotCommited, copyOnMedia, copyOnFSGroup,
                    copyArchived, copyOnReadOnlyFS)) {
                orders.add(new DeleteStudyOrder(sof.getPk(),
                        sof.getStudy().getPk(), sof.getFileSystem().getPk(),
                        sof.getAccessTime().getTime()));
            }
        }
        return orders;
    }

    /**    
     * @ejb.interface-method
     */
    public Dataset createIAN(DeleteStudyOrder order, boolean delStudyFromDB)
            throws FinderException, NoSuchStudyException {
        StudyLocal study;
        try {
            study = studyHome.findByPrimaryKey(order.getStudyPk());
        } catch (ObjectNotFoundException e) {
            throw new NoSuchStudyException(e);
        }
        PatientLocal pat = study.getPatient();
        Dataset ian = DcmObjectFactory.getInstance().newDataset();
        ian.putAll(pat.getAttributes(false).subSet(IAN_PAT_TAGS));
        ian.putSH(Tags.StudyID, study.getStudyId());
        ian.putUI(Tags.StudyInstanceUID, study.getStudyIuid());
        DcmElement refPPSSeq = ian.putSQ(Tags.RefPPSSeq);
        HashSet ppsuids = new HashSet();
        DcmElement refSerSeq = ian.putSQ(Tags.RefSeriesSeq);
        Collection seriess = study.getSeries();
        for (Iterator siter = seriess.iterator(); siter.hasNext();) {
            SeriesLocal series = (SeriesLocal) siter.next();
            Dataset serAttrs = series.getAttributes(false);
            Dataset refPPS = serAttrs.getItem(Tags.RefPPSSeq);
            if (refPPS != null
                    && ppsuids.add(refPPS.getString(Tags.RefSOPInstanceUID))) {
                refPPSSeq.addItem(refPPS);
            }
            Dataset refSer = refSerSeq.addNewItem();
            refSer.putUI(Tags.SeriesInstanceUID, series.getSeriesIuid());
            DcmElement refSopSeq = refSer.putSQ(Tags.RefSOPSeq);
            Collection insts = series.getInstances();
            for (Iterator iiter = insts.iterator(); iiter.hasNext();) {
                InstanceLocal inst = (InstanceLocal) iiter.next();
                Dataset refSOP = refSopSeq.addNewItem();
                refSOP.putUI(Tags.RefSOPClassUID, inst.getSopCuid());
                refSOP.putUI(Tags.RefSOPInstanceUID, inst.getSopIuid());
                DatasetUtils.putRetrieveAET(refSOP, inst.getRetrieveAETs(),
                        inst.getExternalRetrieveAET());
                refSOP.putCS(Tags.InstanceAvailability, Availability.toString(
                        delStudyFromDB ? Availability.UNAVAILABLE
                                       : inst.getAvailabilitySafe()));
             }
        }
        return ian;
    }

    /**    
     * @ejb.interface-method
     */
    public String[] deleteStudy(DeleteStudyOrder order,
            int availabilityOfExtRetr, boolean delStudyFromDB,
            boolean delPatientWithoutObjects)
            throws ConcurrentStudyStorageException {
        try {
            long fsPk = order.getFsPk();
            StudyLocal study = studyHome.findByPrimaryKey(order.getStudyPk());
            FileSystemLocal fs = fileSystemHome.findByPrimaryKey(fsPk);
            Collection files = study.getFiles(fsPk);
            try {
                // check if new objects belonging to the study were stored
                // after this DeleteStudyOrder was scheduled
                sofHome.findByStudyAndFileSystem(study, fs);
                throw new ConcurrentStudyStorageException(
                        "Concurrent storage of study[uid="
                        + study.getStudyIuid() + "] on file system[dir="
                        + fs.getDirectoryPath() + "] - do not delete study");
            } catch (ObjectNotFoundException onfe) {
            }
            String fsPath = fs.getDirectoryPath();
            String[] fpaths = new String[files.size()];
            int i = 0;
            for (Iterator iter = files.iterator(); iter.hasNext(); i++) {
                FileLocal f = (FileLocal) iter.next();
                fpaths[i] = fsPath + '/' + f.getFilePath();
                f.remove();
            }
            if (delStudyFromDB && study.getAllFiles().isEmpty()) {
                PatientLocal pat = study.getPatient();
                study.remove();
                if (delPatientWithoutObjects) {
                    deletePatientWithoutObjects(pat);
                }
            } else {
                Collection seriess = study.getSeries();
                for (Iterator siter = seriess.iterator(); siter.hasNext();) {
                    SeriesLocal series = (SeriesLocal) siter.next();
                    Collection insts = series.getInstances();
                    for (Iterator iiter = insts.iterator(); iiter.hasNext();) {
                        InstanceLocal inst = (InstanceLocal) iiter.next();
                        inst.updateRetrieveAETs();
                        inst.updateAvailability(availabilityOfExtRetr);
                    }
                    series.updateRetrieveAETs();
                    series.updateAvailability();
                }
                study.updateRetrieveAETs();
                study.updateAvailability();
             }
            return fpaths;
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    private void deletePatientWithoutObjects(PatientLocal patient)
            throws RemoveException {
        if ( patient.getStudies().isEmpty() &&
                patient.getMwlItems().isEmpty() &&
                patient.getGsps().isEmpty() &&
                patient.getMpps().isEmpty() &&
                patient.getGppps().isEmpty() ) {
            patient.remove();
        }
    }

    /**
     * @ejb.interface-method
     */
    public FileDTO[] findFilesToCompress(FileSystemDTO fsDTO, String cuid,
            Timestamp before, int limit) throws FinderException {
        if (log.isDebugEnabled())
            log.debug("Querying for files to compress in "
                    + fsDTO.getDirectoryPath());
        Collection c = fileHome.findFilesToCompress(fsDTO.getPk(), cuid,
                before, limit);
        if (log.isDebugEnabled())
            log.debug("Found " + c.size() + " files to compress in "
                    + fsDTO.getDirectoryPath());
        return toFileDTOs(c);
    }

    /**
     * @ejb.interface-method
     */
    public FileDTO[] findFilesForMD5Check(String dirPath, Timestamp before,
            int limit) throws FinderException {
        if (log.isDebugEnabled())
            log.debug("Querying for files to check md5 in " + dirPath);
        Collection c = fileHome.findToCheckMd5(dirPath, before, limit);
        if (log.isDebugEnabled())
            log.debug("Found " + c.size() + " files to check md5 in "
                    + dirPath);
        return toFileDTOs(c);
    }

    /**
     * @ejb.interface-method
     */
    public FileDTO[] findFilesByStatusAndFileSystem(String dirPath, int status,
            Timestamp before, int limit) throws FinderException {
        if (log.isDebugEnabled())
            log.debug("Querying for files with status " + status + " in "
                    + dirPath);
        Collection c = fileHome.findByStatusAndFileSystem(dirPath, status,
                before, limit);
        if (log.isDebugEnabled())
            log.debug("Found " + c.size() + " files with status " + status
                    + " in " + dirPath);
        return toFileDTOs(c);
    }

    /**
     * @ejb.interface-method
     */
    public void updateTimeOfLastMd5Check(long pk) throws FinderException {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        if (log.isDebugEnabled())
            log.debug("update time of last md5 check to " + ts);
        FileLocal fl = fileHome.findByPrimaryKey(new Long(pk));
        fl.setTimeOfLastMd5Check(ts);
    }

    /**
     * @ejb.interface-method
     */
    public void replaceFile(long pk, String path, String tsuid, long size,
            byte[] md5, int status) throws FinderException {
        FileLocal oldFile = fileHome.findByPrimaryKey(pk);
        oldFile.setFilePath(path);
        oldFile.setFileTsuid(tsuid);
        oldFile.setFileSize(size);
        oldFile.setFileMd5(md5);
        oldFile.setFileStatus(status);
    }

    /**
     * @ejb.interface-method
     */
    public void setFileStatus(long pk, int status) throws FinderException {
        fileHome.findByPrimaryKey(pk).setFileStatus(status);
    }

    private FileDTO[] toFileDTOs(Collection c) {
        FileDTO[] dto = new FileDTO[c.size()];
        Iterator it = c.iterator();
        for (int i = 0; i < dto.length; ++i) {
            dto[i] = ((FileLocal) it.next()).getFileDTO();
        }
        return dto;
    }

    /**
     * @ejb.interface-method
     */
    public FileDTO[] getOrphanedPrivateFilesOnFSGroup(String groupID, int limit)
            throws FinderException {
        return toFileDTOsPrivate(
                privFileHome.findOrphanedOnFSGroup(groupID, limit));
    }

    private FileDTO[] toFileDTOsPrivate(Collection c) {
        FileDTO[] dto = new FileDTO[c.size()];
        Iterator it = c.iterator();
        for (int i = 0; i < dto.length; ++i) {
            dto[i] = ((PrivateFileLocal) it.next()).getFileDTO();
        }
        return dto;
    }

    /**
     * @ejb.interface-method
     */
    public void deletePrivateFile(long file_pk) throws RemoveException {
        privFileHome.remove(file_pk);
    }

    /**
     * @ejb.interface-method
     */
    public void touchStudyOnFileSystem(String siud, String dirPath)
            throws FinderException, CreateException {
        try {
            sofHome.findByStudyAndFileSystem(siud, dirPath).touch();
        } catch (ObjectNotFoundException e) {
            try {
                sofHome.create(studyHome.findByStudyIuid(siud), fileSystemHome
                        .findByDirectoryPath(dirPath));
            } catch (CreateException ignore) {
                // Check if concurrent create
                sofHome.findByStudyAndFileSystem(siud, dirPath).touch();
            }
        }
    }

    /**
     * @ejb.interface-method
     */
    public FileDTO[] deleteStoredSeries(SeriesStored seriesStored) {
        try {
            SeriesLocal series = seriesHome.findBySeriesIuid(
                    seriesStored.getSeriesInstanceUID());
            StudyLocal study = series.getStudy();
            Collection instances = series.getInstances();
            ArrayList fileDTOs = new ArrayList(instances.size());
            DcmElement refSopSeq = seriesStored.getIAN()
                .getItem(Tags.RefSeriesSeq).get(Tags.RefSOPSeq);
            int numRefInst = refSopSeq.countItems();
            HashSet iuids = new HashSet(numRefInst * 4 / 3 + 1);
            for (int i = 0; i < numRefInst; i++) {
                iuids.add(refSopSeq.getItem(i)
                        .getString(Tags.RefSOPInstanceUID));
            }
            ArrayList toRemove = new ArrayList(numRefInst);
            for (Iterator itInst = instances.iterator(); itInst.hasNext();) {
                InstanceLocal inst = (InstanceLocal) itInst.next();
                if (iuids.contains(inst.getSopIuid())) {
                    Collection files = inst.getFiles();
                    for (Iterator itFiles = files.iterator();
                            itFiles.hasNext();) {
                        FileLocal file = (FileLocal) itFiles.next();
                        fileDTOs.add(file.getFileDTO());
                    }
                    toRemove.add(inst);
                }
            }
            if (toRemove.size() == instances.size()) {
                series.remove();
            } else {
                for (Iterator itInst = toRemove.iterator(); itInst.hasNext();) {
                    InstanceLocal inst = (InstanceLocal) itInst.next();
                    inst.remove();
                }
                UpdateDerivedFieldsUtils.updateDerivedFieldsOf(series);
            }
            UpdateDerivedFieldsUtils.updateDerivedFieldsOf(study);
            return (FileDTO[]) fileDTOs.toArray(new FileDTO[fileDTOs.size()]);
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    private static final Comparator DESC_FILE_PK = new Comparator() {

        /**
         * This will make sure the most available file will be listed first
         */
        public int compare(Object o1, Object o2) {
            FileDTO dto1 = (FileDTO) o1;
            FileDTO dto2 = (FileDTO) o2;
            int diffAvail = dto1.getAvailability() - dto2.getAvailability();
            long diffPk = dto2.getPk() - dto1.getPk();
            return diffAvail != 0 ? diffAvail 
                        : diffPk == 0 ? 0 : diffPk < 0 ? -1 : 1;
        }
    };

    /**
     * @ejb.interface-method
     */
    public FileDTO[] getFilesOfInstance(String iuid) throws FinderException {
        FileDTO[] dtos = toFileDTOs(instHome.findBySopIuid(iuid).getFiles());
        Arrays.sort(dtos, DESC_FILE_PK);
                return dtos;
    }

    /**
     * @ejb.interface-method
     */
    public String getExternalRetrieveAET(String iuid) throws FinderException {
        return instHome.findBySopIuid(iuid).getExternalRetrieveAET();
    }
}
