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
package org.dcm4chex.archive.mbean;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.management.Attribute;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.DeleteStudyOrder;
import org.dcm4chex.archive.common.FileSystemStatus;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.config.DeleterThresholds;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.notif.StorageFileSystemSwitched;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileSystemUtils;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 8, 2008
 */
public class FileSystemMgt2Service extends ServiceMBeanSupport {

    private static final String NONE = "NONE";

    private static final String GROUP = "group";

    private static final long MIN_FREE_DISK_SPACE = 20 * FileUtils.MEGA;

    private final DeleteStudyDelegate deleteStudy =
            new DeleteStudyDelegate(this);

    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);

    private String timerIDScheduleStudiesForDeletion;

    private String timerIDDeleteOrphanedPrivateFiles;

    private long scheduleStudiesForDeletionInterval;

    private long deleteOrphanedPrivateFilesInterval;

    private String defRetrieveAET;

    private int defAvailability;

    private String defUserInfo;

    private String defStorageDir;

    private boolean checkStorageFileSystemStatus = true;

    private boolean makeStorageDirectory = true;

    private String mountFailedCheckFile = "NO_MOUNT";

    private long minFreeDiskSpace = MIN_FREE_DISK_SPACE;

    private long checkFreeDiskSpaceMinInterval;

    private long checkFreeDiskSpaceMaxInterval;

    private long checkFreeDiskSpaceTime;

    private DeleterThresholds deleterThresholds;

    private long expectedDataVolumePerDay = 100000L;

    private long adjustExpectedDataVolumePerDay = 0;

    private long maxNotAccessedFor = 0;

    private long minNotAccessedFor = 0;

    private boolean externalRetrieveable;

    private boolean storageNotCommited;

    private boolean copyOnMedia;

    private String copyOnFSGroup;

    private boolean copyArchived;

    private boolean copyOnReadOnlyFS;

    private boolean scheduleStudiesForDeletionOnSeriesStored;

    private int scheduleStudiesForDeletionBatchSize;

    private int deleteOrphanedPrivateFilesBatchSize;

    private int updateStudiesBatchSize;

    private FileSystemDTO storageFileSystem;

    private Integer scheduleStudiesForDeletionListenerID;

    private Integer deleteOrphanedPrivateFilesListenerID;

    private ObjectName storeScpServiceName;

    public ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public final void setStoreScpServiceName(ObjectName storeScpServiceName) {
        this.storeScpServiceName = storeScpServiceName;
    }

    public ObjectName getDeleteStudyServiceName() {
        return deleteStudy.getDeleteStudyServiceName();
    }

    public void setDeleteStudyServiceName(ObjectName deleteStudyServiceName) {
        deleteStudy.setDeleteStudyServiceName(deleteStudyServiceName);
    }

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }

    public void setTimerIDScheduleStudiesForDeletion(String timerID) {
        this.timerIDScheduleStudiesForDeletion = timerID;
    }

    public String getTimerIDScheduleStudiesForDeletion() {
        return timerIDScheduleStudiesForDeletion;
    }

    public String getScheduleStudiesForDeletionInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(
                scheduleStudiesForDeletionInterval);
    }

    public void setScheduleStudiesForDeletionInterval(String interval)
            throws Exception {
        this.scheduleStudiesForDeletionInterval = RetryIntervalls
                .parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDScheduleStudiesForDeletion,
                    scheduleStudiesForDeletionListenerID,
                    scheduleStudiesForDeletionListener);
            scheduleStudiesForDeletionListenerID = scheduler.startScheduler(
                    timerIDScheduleStudiesForDeletion,
                    scheduleStudiesForDeletionInterval,
                    scheduleStudiesForDeletionListener);
        }
    }

    public String getTimerIDDeleteOrphanedPrivateFiles() {
        return timerIDDeleteOrphanedPrivateFiles;
    }

    public void setTimerIDDeleteOrphanedPrivateFiles(
            String timerIDDeleteOrphanedPrivateFiles) {
        this.timerIDDeleteOrphanedPrivateFiles =
                timerIDDeleteOrphanedPrivateFiles;
    }

    public String getDeleteOrphanedPrivateFilesInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(
                deleteOrphanedPrivateFilesInterval);
    }

    public void setDeleteOrphanedPrivateFilesInterval(String interval)
            throws Exception {
        this.deleteOrphanedPrivateFilesInterval = RetryIntervalls
                .parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDDeleteOrphanedPrivateFiles,
                    deleteOrphanedPrivateFilesListenerID,
                    deleteOrphanedPrivateFilesListener);
            deleteOrphanedPrivateFilesListenerID = scheduler.startScheduler(
                    timerIDDeleteOrphanedPrivateFiles,
                    deleteOrphanedPrivateFilesInterval,
                    deleteOrphanedPrivateFilesListener);
        }
    }

    protected void startService() throws Exception {
        scheduleStudiesForDeletionListenerID = scheduler.startScheduler(
                timerIDScheduleStudiesForDeletion,
                scheduleStudiesForDeletionInterval,
                scheduleStudiesForDeletionListener);
        deleteOrphanedPrivateFilesListenerID = scheduler.startScheduler(
                timerIDDeleteOrphanedPrivateFiles,
                deleteOrphanedPrivateFilesInterval,
                deleteOrphanedPrivateFilesListener);
        server.addNotificationListener(storeScpServiceName,
                scheduleStudiesForDeletionOnSeriesStoredListener,
                SeriesStored.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDScheduleStudiesForDeletion,
                scheduleStudiesForDeletionListenerID,
                scheduleStudiesForDeletionListener);
        scheduler.stopScheduler(timerIDDeleteOrphanedPrivateFiles,
                deleteOrphanedPrivateFilesListenerID,
                deleteOrphanedPrivateFilesListener);
        server.removeNotificationListener(storeScpServiceName,
                scheduleStudiesForDeletionOnSeriesStoredListener,
                SeriesStored.NOTIF_FILTER, null);
    }

    private final NotificationListener scheduleStudiesForDeletionListener =
            new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            startScheduleStudiesForDeletion();
        }
    };

    private final NotificationListener scheduleStudiesForDeletionOnSeriesStoredListener =
            new NotificationListener() {
    public void handleNotification(Notification notif, Object handback) {
        if (scheduleStudiesForDeletionOnSeriesStored) {
            startScheduleStudiesForDeletion();
        }
    }
};

    private void startScheduleStudiesForDeletion() {
        new Thread(new Runnable(){
            public void run() {
                try {
                    scheduleStudiesForDeletion();
                } catch (Exception e) {
                    log.error("Schedule Studies for deletion failed:", e);
                }
            }}).start();
    }

    private NotificationListener deleteOrphanedPrivateFilesListener =
            new NotificationListener() {
        private Thread thread;

        public void handleNotification(Notification notif, Object handback) {
            if (thread == null) {
                thread = new Thread(new Runnable(){
                    public void run() {
                        try {
                            deleteOrphanedPrivateFiles();
                        } catch (Exception e) {
                            log.error("deleteOrphanedPrivateFiles() failed:", e);
                        }
                        thread = null;
                    }});
                thread.start();
            } else {
                log.info("deleteOrphanedPrivateFiles() still in progress! " +
                        "Ignore this timer notification!");
            }
        }
    };

    public String getFileSystemGroupID() {
        return serviceName.getKeyProperty(GROUP);
    }

    public String getDefRetrieveAET() {
        return defRetrieveAET;
    }

    public void setDefRetrieveAET(String defRetrieveAET) {
        this.defRetrieveAET = defRetrieveAET;
    }

    public String getDefAvailability() {
        return Availability.toString(defAvailability);
    }

    public void setDefAvailability(String availability) {
        this.defAvailability = Availability.toInt(availability);
    }

    public String getDefUserInfo() {
        return defUserInfo;
    }

    public void setDefUserInfo(String defUserInfo) {
        this.defUserInfo = defUserInfo;
    }

    public String getDefStorageDir() {
        return defStorageDir != null ? defStorageDir : NONE;
    }

    public void setDefStorageDir(String defStorageDir) {
        String trimmed = defStorageDir.trim();
        this.defStorageDir = trimmed.equalsIgnoreCase(NONE) ? null : trimmed;
    }

    public final boolean isCheckStorageFileSystemStatus() {
        return checkStorageFileSystemStatus;
    }

    public final void setCheckStorageFileSystemStatus(
            boolean checkStorageFileSystemStatus) {
        this.checkStorageFileSystemStatus = checkStorageFileSystemStatus;
    }

    public boolean isMakeStorageDirectory() {
        return makeStorageDirectory;
    }

    public void setMakeStorageDirectory(boolean makeStorageDirectory) {
        this.makeStorageDirectory = makeStorageDirectory;
    }

    public final String getMountFailedCheckFile() {
        return mountFailedCheckFile;
    }

    public final void setMountFailedCheckFile(String mountFailedCheckFile) {
        this.mountFailedCheckFile = mountFailedCheckFile;
    }

    public final String getMinFreeDiskSpace() {
        return minFreeDiskSpace == 0 ? NONE
                : FileUtils.formatSize(minFreeDiskSpace);
    }

    public final void setMinFreeDiskSpace(String str) {
        this.minFreeDiskSpace = str.equalsIgnoreCase(NONE) ? 0
                : FileUtils.parseSize(str, MIN_FREE_DISK_SPACE);
    }

    public long getFreeDiskSpaceOnCurFS() throws IOException {
        if (storageFileSystem == null || minFreeDiskSpace == 0)
            return -1L;
        File dir = FileUtils.toFile(storageFileSystem.getDirectoryPath());
        return dir.isDirectory() ? FileSystemUtils.freeSpace(dir.getPath())
                                 : -1L;
    }

    public String getFreeDiskSpaceOnCurFSString() throws IOException {
        return FileUtils.formatSize(getFreeDiskSpaceOnCurFS());
    }

    public long getUsableDiskSpaceOnCurFS() throws IOException {
        long free = getFreeDiskSpaceOnCurFS();
        return free == -1L ? -1L : Math.max(0, free - minFreeDiskSpace);
    }

    public String getUsableDiskSpaceOnCurFSString() throws IOException {
        return FileUtils.formatSize(getUsableDiskSpaceOnCurFS());
    }

    public long getFreeDiskSpace() throws Exception {
        if (minFreeDiskSpace == 0) {
            return -1L;
        }
        FileSystemDTO[] fsDTOs =
            fileSystemMgt().getFileSystemsOfGroup(getFileSystemGroupID());
        long free = 0L;
        for (FileSystemDTO fsDTO : fsDTOs) {
            int status = fsDTO.getStatus();
            if (status == FileSystemStatus.RW
                    || status == FileSystemStatus.DEF_RW) {
                File dir = FileUtils.toFile(fsDTO.getDirectoryPath());
                if (dir.isDirectory()) {
                    free += FileSystemUtils.freeSpace(dir.getPath());
                }
            }
        }
        return free;
    }

    public String getFreeDiskSpaceString() throws Exception {
        return FileUtils.formatSize(getFreeDiskSpace());
    }

    public long getUsableDiskSpace() throws Exception {
        if (minFreeDiskSpace == 0) {
            return -1L;
        }
        return calcUsableDiskSpace(fileSystemMgt()
                .getFileSystemsOfGroup(getFileSystemGroupID()));
    }

    private long calcUsableDiskSpace(FileSystemDTO[] fsDTOs) throws IOException {
        long free = 0L;
        for (FileSystemDTO fsDTO : fsDTOs) {
            int status = fsDTO.getStatus();
            if (status == FileSystemStatus.RW
                    || status == FileSystemStatus.DEF_RW) {
                File dir = FileUtils.toFile(fsDTO.getDirectoryPath());
                if (dir.isDirectory()) {
                    free += Math.max(0,
                            FileSystemUtils.freeSpace(dir.getPath())
                            - minFreeDiskSpace);
                }
            }
        }
        return free;
    }

    public String getUsableDiskSpaceString() throws Exception {
        return FileUtils.formatSize(getUsableDiskSpace());
    }

    public final String getCheckFreeDiskSpaceMinimalInterval() {
        return RetryIntervalls.formatInterval(checkFreeDiskSpaceMinInterval);
    }

    public final void setCheckFreeDiskSpaceMinimalInterval(String s) {
        this.checkFreeDiskSpaceMinInterval = RetryIntervalls.parseInterval(s);
    }

    public final String getCheckFreeDiskSpaceMaximalInterval() {
        return RetryIntervalls.formatInterval(checkFreeDiskSpaceMaxInterval);
    }

    public final void setCheckFreeDiskSpaceMaximalInterval(String s) {
        this.checkFreeDiskSpaceMaxInterval = RetryIntervalls.parseInterval(s);
    }

    public final String getDeleterThresholds() {
        return deleterThresholds == null ? NONE : deleterThresholds.toString();
    }

    public final void setDeleterThresholds(String s) {
        this.deleterThresholds = s.equalsIgnoreCase(NONE) ? null
                : new DeleterThresholds(s, true);
    }

    public final String getExpectedDataVolumePerDay() {
        return FileUtils.formatSize(expectedDataVolumePerDay);
    }

    public final void setExpectedDataVolumePerDay(String s) {
        this.expectedDataVolumePerDay = FileUtils.parseSize(s, FileUtils.MEGA);
    }

    public final boolean isAdjustExpectedDataVolumePerDay() {
        return adjustExpectedDataVolumePerDay != 0L;
    }

    public final void setAdjustExpectedDataVolumePerDay(boolean b) {
        this.adjustExpectedDataVolumePerDay = b ? nextMidnight() : 0L;
    }

    private long nextMidnight() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public String adjustExpectedDataVolumePerDay() throws Exception {
        FileSystemMgt2 fsMgt = fileSystemMgt();
        return adjustExpectedDataVolumePerDay(fsMgt,
                fsMgt.getFileSystemsOfGroup(getFileSystemGroupID()));
    }

    private String adjustExpectedDataVolumePerDay(FileSystemMgt2 fsMgt,
            FileSystemDTO[] fss) throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.roll(Calendar.DAY_OF_MONTH, false);
        long after = cal.getTimeInMillis();
        long sum = 0L;
        for (FileSystemDTO fs : fss) {
            sum = fsMgt.sizeOfFilesCreatedAfter(fs.getPk(), after);
        }
        String size = FileUtils.formatSize(sum);
        if (sum > expectedDataVolumePerDay) {
            server.setAttribute(super.serviceName, new Attribute(
                    "ExpectedDataVolumePerDay", size));
        }
        return size;
    }

    public long getCurrentDeleterThreshold() throws Exception {
        if (deleterThresholds == null) {
            return -1L;
        }
        FileSystemMgt2 fsMgt = fileSystemMgt();
        return getCurrentDeleterThreshold(fsMgt,
                fsMgt.getFileSystemsOfGroup(getFileSystemGroupID()));
    }

    private long getCurrentDeleterThreshold(FileSystemMgt2 fsMgt,
            FileSystemDTO[] fsDTOs) throws Exception {
        Calendar now = Calendar.getInstance();
        if (adjustExpectedDataVolumePerDay != 0
                && now.getTimeInMillis() > adjustExpectedDataVolumePerDay) {
            adjustExpectedDataVolumePerDay(fsMgt, fsDTOs);
            adjustExpectedDataVolumePerDay = nextMidnight();
        }
        return deleterThresholds.getDeleterThreshold(now)
                .getFreeSize(expectedDataVolumePerDay);
    }

    public String getDeleteStudyIfNotAccessedFor() {
        return RetryIntervalls.formatIntervalZeroAsNever(maxNotAccessedFor);
    }

    public void setDeleteStudyIfNotAccessedFor(String interval) {
        this.maxNotAccessedFor = RetryIntervalls.parseIntervalOrNever(interval);
    }

    public String getDeleteStudyOnlyIfNotAccessedFor() {
        return RetryIntervalls.formatInterval(minNotAccessedFor);
    }

    public void setDeleteStudyOnlyIfNotAccessedFor(String interval) {
        this.minNotAccessedFor = RetryIntervalls.parseInterval(interval);
    }

    public boolean isDeleteStudyOnlyIfStorageNotCommited() {
        return storageNotCommited;
    }

    public void setDeleteStudyOnlyIfStorageNotCommited(
            boolean storageNotCommited) {
        this.storageNotCommited = storageNotCommited;
    }

    public boolean isDeleteStudyOnlyIfCopyOnMedia() {
        return copyOnMedia;
    }

    public boolean isDeleteStudyOnlyIfCopyExternalRetrieveable() {
        return externalRetrieveable;
    }

    public void setDeleteStudyOnlyIfCopyExternalRetrieveable(
            boolean externalRetrieveable) {
        this.externalRetrieveable = externalRetrieveable;
    }

    public void setDeleteStudyOnlyIfCopyOnMedia(boolean copyOnMedia) {
        this.copyOnMedia = copyOnMedia;
    }

    public String getDeleteStudyOnlyIfCopyOnFileSystemOfFileSystemGroup() {
        return copyOnFSGroup != null ? copyOnFSGroup : NONE;
    }

    public void setDeleteStudyOnlyIfCopyOnFileSystemOfFileSystemGroup(
            String copyOnFSGroup) {
        String trimmed = copyOnFSGroup.trim();
        if (serviceName != null // may not be initialized when invoked during bean registration
                && trimmed.equals(getFileSystemGroupID())) {
            throw new IllegalArgumentException(
                    "Must differ from file system group managed by this service");
        }
        this.copyOnFSGroup = trimmed.equalsIgnoreCase(NONE) ? null : trimmed;
    }

    public boolean isDeleteStudyOnlyIfCopyArchived() {
        return copyArchived;
    }

    public void setDeleteStudyOnlyIfCopyArchived(boolean copyArchived) {
        this.copyArchived = copyArchived;
    }

    public boolean isDeleteStudyOnlyIfCopyOnReadOnlyFileSystem() {
        return copyOnReadOnlyFS;
    }

    public void setDeleteStudyOnlyIfCopyOnReadOnlyFileSystem(
            boolean copyOnReadOnlyFS) {
        this.copyOnReadOnlyFS = copyOnReadOnlyFS;
    }

    public void setScheduleStudiesForDeletionOnSeriesStored(
            boolean scheduleStudiesForDeletionOnSeriesStored) {
        this.scheduleStudiesForDeletionOnSeriesStored =
                scheduleStudiesForDeletionOnSeriesStored;
    }

    public boolean isScheduleStudiesForDeletionOnSeriesStored() {
        return scheduleStudiesForDeletionOnSeriesStored;
    }

    public void setScheduleStudiesForDeletionBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize: " + batchSize);
        }
        this.scheduleStudiesForDeletionBatchSize = batchSize;
    }

    public int getScheduleStudiesForDeletionBatchSize() {
        return scheduleStudiesForDeletionBatchSize;
    }

    public void setDeleteOrphanedPrivateFilesBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize: " + batchSize);
        }
        this.deleteOrphanedPrivateFilesBatchSize = batchSize;
    }

    public int getDeleteOrphanedPrivateFilesBatchSize() {
        return deleteOrphanedPrivateFilesBatchSize;
    }

    public int getUpdateStudiesBatchSize() {
        return updateStudiesBatchSize;
    }

    public void setUpdateStudiesBatchSize(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize: " + batchSize);
        }
        this.updateStudiesBatchSize = batchSize;
    }

    public String listAllFileSystems() throws Exception {
        FileSystemDTO[] fss = fileSystemMgt().getAllFileSystems();
        sortFileSystems(fss);
        return toString(fss);
    }

    public String listFileSystems() throws Exception {
        FileSystemDTO[] fss = fileSystemMgt()
                .getFileSystemsOfGroup(getFileSystemGroupID());
        sortFileSystems(fss);
        return toString(fss);
    }

    public FileSystemDTO addRWFileSystem(String dirPath) throws Exception {
        return fileSystemMgt().addAndLinkFileSystem(
                mkFileSystemDTO(dirPath, FileSystemStatus.RW));
    }

    private FileSystemDTO mkFileSystemDTO(String dirPath, int status) {
        FileSystemDTO fs = new FileSystemDTO();
        fs.setDirectoryPath(dirPath);
        fs.setGroupID(getFileSystemGroupID());
        fs.setRetrieveAET(defRetrieveAET);
        fs.setAvailability(defAvailability);
        fs.setUserInfo(defUserInfo);
        fs.setStatus(status);
        return fs;
    }

    public FileSystemDTO removeFileSystem(String dirPath) throws Exception {
        return fileSystemMgt()
                .removeFileSystem(getFileSystemGroupID(), dirPath);
    }

    public FileSystemDTO linkFileSystems(String dirPath, String next)
            throws Exception {
        return fileSystemMgt()
                .linkFileSystems(getFileSystemGroupID(), dirPath, next);
    }

    public FileSystemDTO updateFileSystemStatus(String dirPath, String status)
            throws Exception {
        return fileSystemMgt().updateFileSystemStatus(getFileSystemGroupID(),
                dirPath, FileSystemStatus.toInt(status));
    }

    public FileSystemDTO updateFileSystemAvailability(String dirPath,
            String availability) throws Exception {
        return fileSystemMgt().updateFileSystemAvailability(
                        getFileSystemGroupID(), dirPath,
                        Availability.toInt(availability), 
                        Availability.toInt(deleteStudy
                                .getAvailabilityOfExternalRetrieveable()), 
                        updateStudiesBatchSize);
    }

    public FileSystemDTO updateFileSystemRetrieveAETitle(String dirPath,
            String aet) throws Exception {
        return fileSystemMgt().updateFileSystemRetrieveAET(
                getFileSystemGroupID(), dirPath, aet, updateStudiesBatchSize);
    }

    public FileSystemDTO selectStorageFileSystem() throws Exception { 
        FileSystemMgt2 fsMgt = fileSystemMgt();
        if (storageFileSystem == null) {
            initStorageFileSystem(fsMgt);
        } else if (checkStorageFileSystemStatus) {
            checkStorageFileSystemStatus(fsMgt);
        }
        if (storageFileSystem == null) {
            log.warn("No writeable storage file system configured in group "
                    + getFileSystemGroupID() + " - storage will fail until "
                    + "a writeable storage file system is configured.");
            return null;
        }
        if (checkFreeDiskSpaceTime < System.currentTimeMillis()) {
            if (!(checkFreeDiskSpace(storageFileSystem)
                    || switchFileSystem(fsMgt, storageFileSystem))) {
                log.error("High Water Mark reached on storage file system "
                        + storageFileSystem + " - no alternative storage file "
                        + "system configured for file system group "
                        + getFileSystemGroupID());
                return null;
            }
        }
        return storageFileSystem;
    }

    private synchronized boolean switchFileSystem(FileSystemMgt2 fsMgt,
            FileSystemDTO fsDTO) throws Exception {
        if (storageFileSystem.getPk() != fsDTO.getPk()) {
            log.info("Storage file system has already been switched from "
                    + fsDTO + " to " + storageFileSystem
                    + " by another thread.");
            return true; 
        }
        FileSystemDTO tmp = storageFileSystem;
        String next;
        while ((next = tmp.getNext()) != null &&
                next != storageFileSystem.getDirectoryPath()) {
            tmp = fsMgt.getFileSystemOfGroup(getFileSystemGroupID(), next);
            if (tmp.getStatus() == FileSystemStatus.RW
                    && checkFreeDiskSpace(tmp)) {
                storageFileSystem = fsMgt.updateFileSystemStatus(
                        tmp.getPk(), FileSystemStatus.DEF_RW);
                log.info("Switch storage file system from " + fsDTO + " to "
                        + storageFileSystem);
                sendJMXNotification(new StorageFileSystemSwitched(
                        fsDTO, storageFileSystem));
                return true;
            }
        }
        return false;
    }

    void sendJMXNotification(Object o) {
        long eventID = super.getNextNotificationSequenceNumber();
        Notification notif = new Notification(o.getClass().getName(), this,
                eventID);
        notif.setUserData(o);
        super.sendNotification(notif);
    }

    private boolean checkFreeDiskSpace(FileSystemDTO fsDTO) throws IOException {
        File dir = FileUtils.toFile(fsDTO.getDirectoryPath());
        if (!dir.exists()) {
            if (!makeStorageDirectory) {
                log.warn("No such directory " + dir
                        + " - try to switch to next configured storage directory");
                return false;
            }
            log.info("M-WRITE " + dir);
            if (!dir.mkdirs()) {
                log.warn("Failed to create directory " + dir
                        + " - try to switch to next configured storage directory");
                return false;
            }
        }
        File nomount = new File(dir, mountFailedCheckFile);
        if (nomount.exists()) {
            log.warn("Mount on " + dir
                    + " seems broken - try to switch to next configured storage directory");
            return false;
        }
        if (minFreeDiskSpace == 0) {
            return true;
        }
        final long freeSpace = FileSystemUtils.freeSpace(dir.getPath());
        log.info("Free disk space on " + dir + ": "
                + FileUtils.formatSize(freeSpace));
        if (freeSpace < minFreeDiskSpace) {
            log.info("High Water Mark reached on current storage directory "
                    + dir
                    + " - try to switch to next configured storage directory");
            return false;
        }
        checkFreeDiskSpaceTime = System.currentTimeMillis() + Math.min(
                freeSpace * checkFreeDiskSpaceMinInterval / minFreeDiskSpace,
                checkFreeDiskSpaceMaxInterval);
        return true;
    }

    private void checkStorageFileSystemStatus(FileSystemMgt2 fsMgt)
            throws FinderException, RemoteException {
        storageFileSystem = fsMgt.getFileSystem(storageFileSystem.getPk());
        if (storageFileSystem.getStatus() == FileSystemStatus.DEF_RW) {
            return;
        }
        log.info("Status of previous storage file system changed: "
                + storageFileSystem);
        storageFileSystem = fsMgt.getDefRWFileSystemsOfGroup(
                getFileSystemGroupID());
        if (storageFileSystem != null) {
            log.info("New storage file system: " + storageFileSystem);
        }
        checkFreeDiskSpaceTime = 0;
    }

    private void initStorageFileSystem(FileSystemMgt2 fsMgt) throws Exception {
        storageFileSystem = fsMgt.getDefRWFileSystemsOfGroup(
                getFileSystemGroupID());
        if (storageFileSystem == null) {
            if (defStorageDir != null) {
                storageFileSystem = fsMgt.addFileSystem(
                        mkFileSystemDTO(defStorageDir, FileSystemStatus.DEF_RW));
                log.info("No writeable storage file system configured in group "
                        + getFileSystemGroupID() + " - auto configure "
                        + storageFileSystem);
                return;
            }
        }
        checkFreeDiskSpaceTime = 0;
    }

    static FileSystemMgt2 fileSystemMgt() throws Exception {
        FileSystemMgt2Home home = (FileSystemMgt2Home) EJBHomeFactory
                .getFactory().lookup(FileSystemMgt2Home.class,
                        FileSystemMgt2Home.JNDI_NAME);
        return home.create();
    }

    private static String toString(FileSystemDTO[] fss) {
        StringBuilder sb = new StringBuilder();
        for (FileSystemDTO fs : fss) {
            sb.append(fs).append("\r\n");
        }
        String s = sb.toString();
        return s;
    }

    private static void sortFileSystems(FileSystemDTO[] fss) {
        for (int i = 0; i < fss.length; i++) {
            selectRoot(fss, i);
            while (selectNext(fss, i)) {
                i++;
            }
        }
    }

    private static boolean selectRoot(FileSystemDTO[] fss, int index) {
        for (int i = index; i < fss.length; i++) {
            if (!hasPrevious(fss, i)) {
                swap(fss, index, i);
                return true;
            }
        }
        return false;
    }

    private static boolean selectNext(FileSystemDTO[] fss, int index) {
        String next = fss[index].getNext();
        if (next == null) {
            return false;
        }
        for (int i = index+1; i < fss.length; i++) {
            if (next.equals(fss[i].getDirectoryPath())) {
                swap(fss, index+1, i);
                return true;
            }
        }
        return false;
    }

    private static void swap(FileSystemDTO[] fss, int i, int j) {
        if (i == j) return;
        FileSystemDTO tmp = fss[i];
        fss[i] = fss[j];
        fss[j] = tmp;
    }

    private static boolean hasPrevious(FileSystemDTO[] fss, int index) {
        String next = fss[index].getDirectoryPath();
        for (int i = 0; i < fss.length; i++) {
            if (i != index && next.equals(fss[i].getNext())) {
                return true;
            }
        }
        return false;
    }

    public int scheduleStudiesForDeletion() throws Exception {
        if (maxNotAccessedFor == 0 && deleterThresholds == null) {
            return 0;
        }
        log.info("Check file system group " + getFileSystemGroupID()
                + " for deletion of studies");
        int countStudies = 0;
        FileSystemMgt2 fsMgt = fileSystemMgt();
        if (maxNotAccessedFor > 0) {
            countStudies = scheduleStudiesForDeletion(fsMgt,
                    System.currentTimeMillis() - maxNotAccessedFor,
                    Long.MAX_VALUE);
        }
        if (deleterThresholds != null) {
            FileSystemDTO[] fsDTOs = fsMgt.getFileSystemsOfGroup(
                    getFileSystemGroupID());
            long threshold = getCurrentDeleterThreshold(fsMgt, fsDTOs);
            long usable = calcUsableDiskSpace(fsDTOs);
            long sizeToDel = threshold - usable;
            if (sizeToDel > 0) {
                log.info("Try to free " + sizeToDel
                        + " of disk space on file system group "
                        + getFileSystemGroupID());
                countStudies += scheduleStudiesForDeletion(fsMgt,
                        System.currentTimeMillis() - minNotAccessedFor,
                        sizeToDel);
            }
        }
        if (countStudies > 0) {
            log.info("Scheduled " + countStudies
                    + " studies for deletion on file system group "
                    + getFileSystemGroupID());
        }
        return countStudies;
    }

    private int scheduleStudiesForDeletion(FileSystemMgt2 fsMgt,
            long notAccessedAfter, long sizeToDel0) throws Exception {
        int countStudies = 0;
        long minAccessTime = 0;
        long sizeToDel = sizeToDel0;
        do {
            Collection<DeleteStudyOrder> orders = 
                    fsMgt.createDeleteOrdersForStudiesOnFSGroup(
                            getFileSystemGroupID(), minAccessTime,
                            notAccessedAfter,
                            scheduleStudiesForDeletionBatchSize,
                            externalRetrieveable, storageNotCommited,
                            copyOnMedia, copyOnFSGroup, copyArchived,
                            copyOnReadOnlyFS);
            if (orders.isEmpty()) {
                if (sizeToDel0 != Long.MAX_VALUE) {
                    log.warn("Could not find any further study for deletion on "
                            + "file system group " + getFileSystemGroupID());
                }
                break;
            }
            Iterator<DeleteStudyOrder> orderIter = orders.iterator();
            do {
                DeleteStudyOrder order = orderIter.next();
                if (fsMgt.removeStudyOnFSRecord(order)) {
                    try {
                        deleteStudy.scheduleDeleteOrder(order);
                    } catch (Exception e) {
                        // re-insert SOF record, so the deleter keeps track 
                        // for deletion of the study
                        fsMgt.createStudyOnFSRecord(order);
                        throw e;
                    }
                    sizeToDel -= fsMgt.getStudySize(order);
                    countStudies++;
                }
                minAccessTime = order.getAccessTime();
            } while (sizeToDel > 0 && orderIter.hasNext());
        } while (sizeToDel > 0);
        return countStudies;
    }

    public long scheduleStudyForDeletion(String suid) throws Exception {
        FileSystemMgt2 fsMgt = fileSystemMgt();
        Collection<DeleteStudyOrder> orders = 
            fsMgt.createDeleteOrdersForStudyOnFSGroup(suid,
                    getFileSystemGroupID());
        long sizeToDel = 0;
        for (DeleteStudyOrder order : orders) {
            if (fsMgt.removeStudyOnFSRecord(order)) {
                try {
                    deleteStudy.scheduleDeleteOrder(order);
                } catch (Exception e) {
                    // re-insert SOF record, so the deleter keeps track 
                    // for deletion of the study
                    fsMgt.createStudyOnFSRecord(order);
                    throw e;
                }
                sizeToDel  += fsMgt.getStudySize(order);
            }
        }
        return sizeToDel;
    }

    public int deleteOrphanedPrivateFiles() throws Exception {
        log.info("Check file system group " + getFileSystemGroupID()
                + " for deletion of orphaned private files");
        FileSystemMgt2 fsMgt = fileSystemMgt();
        FileDTO[] fileDTOs = fsMgt.getOrphanedPrivateFilesOnFSGroup(
                getFileSystemGroupID(), deleteOrphanedPrivateFilesBatchSize);
        int deleted = 0;
        for (int i = 0; i < fileDTOs.length; i++) {
            FileDTO fileDTO = fileDTOs[i];
            File file = FileUtils.toFile(fileDTO.getDirectoryPath(),
                    fileDTO.getFilePath());
            try {
                fsMgt.deletePrivateFile(fileDTO.getPk());
            } catch (Exception e) {
                log.warn("Failed to remove File Record[pk=" + fileDTO.getPk()
                        + "] from DB:", e);
                log.info("-> Keep dereferenced file: " + file);
                continue;
            }
            FileUtils.delete(file, true);
            deleted++;
        }
        return deleted;
    }
}
