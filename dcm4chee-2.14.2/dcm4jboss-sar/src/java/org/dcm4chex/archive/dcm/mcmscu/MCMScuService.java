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

package org.dcm4chex.archive.dcm.mcmscu;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.FutureRSP;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.dcm.AbstractScuService;
import org.dcm4chex.archive.ejb.interfaces.MediaComposer;
import org.dcm4chex.archive.ejb.interfaces.MediaComposerHome;
import org.dcm4chex.archive.ejb.interfaces.MediaDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.mbean.JMSDelegate;
import org.dcm4chex.archive.mbean.SchedulerDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;

/**
 * @author franz.willer
 * 
 * MBean to configure and service media creation managment issues.
 * <p>
 * 1) Schedule media for offline storage.<br>
 * 2) Listen to an JMS queue for receiving media creation request from scheduler
 * or WEB interface.<br>
 * 3) process a media creation request. (move instances and send media creation
 * request and action to media creation managment AET)
 * 
 */
public class MCMScuService extends AbstractScuService implements
        MessageListener {

    private static final Logger log = Logger.getLogger(MCMScuService.class);

    private static final long MIN_MAX_MEDIA_USAGE = 20 * FileUtils.MEGA;

    /** Action command for media creation. */
    private static final int INITIATE_MEDIA_CREATION = 1;

    /** Action command for cancel media creation. */
    private static final int CANCEL_MEDIA_CREATION = 2;

    /**
     * Array containing String values of priorities. (N-Action need text
     * values).
     */
    private static final String[] prioStrings = new String[] { "MED", "HIGH",
            "LOW" };

    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);

    /**
     * Name of the JMS queue to receive media creation requests from scheduler
     * or WEB interface.
     */
    private String queueName;

    /**
     * Holds the max. number of bytes that can be used to collect instances for
     * a singe media.
     */
    private long maxMediaUsage = 680 * FileUtils.MEGA;

    /** Holds the min age of instances in ms for collecting. */
    private long minStudyAge;

    /** Holds the max age of a media for status COLLECTING. */
    private long maxStudyAge;

    private long scheduleMediaInterval;

    private long updateMediaStatusInterval;

    private long burnMediaInterval;

    private Integer scheduleMediaListenerID;

    private Integer updateMediaStatusListenerID;

    private Integer burnMediaListenerID;

    private boolean automaticMediaCreation = false;

    private String notifyBurnMediaEmailTo = "";

    private String notifyBurnMediaEmailFrom = "";

    private int nrOfCopies = 1;

    private static ObjectName sendMailServiceName = null;

    /** Holds the prefix that is used to generate the fileset id. */
    private String fileSetIdPrefix;

    /** Holds the retrieve AET. (AET that perform the move). */
    private String retrieveAET;

    /**
     * Holds the destination AET (AET that receive the instances for media
     * creation).
     */
    private String destAET;

    /** Holds the media creation managment AET. (AET perform media creation) */
    private String mcmScpAET;

    /** Flag if media creation should use information extracted from instance. */
    private boolean useInstanceInfo = false;

    /**
     * Holds type of none DICOM objects that should be stored on media. Default
     * is NONE
     */
    private String includeNonDICOMObj = "NO";

    /** DICOM priority. Used for move and media creation action. */
    private int priority = 0;

    private int concurrency = 1;

    private String timerIDCheckForStudiesToSchedule;

    private String timerIDCheckForMediaToBurn;

    private String timerIDCheckMediaStatus;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
    }

    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final int getConcurrency() {
        return concurrency;
    }

    public final void setConcurrency(int concurrency) throws Exception {
        if (concurrency <= 0)
            throw new IllegalArgumentException("Concurrency: " + concurrency);
        if (this.concurrency != concurrency) {
            final boolean restart = getState() == STARTED;
            if (restart)
                stop();
            this.concurrency = concurrency;
            if (restart)
                start();
        }
    }

    private MediaComposer mediaComposer;

    private final NotificationListener scheduleMediaListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            scheduleMedia();
        }
    };

    private final NotificationListener updateMediaStatusListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            updateMediaStatus();
        }
    };

    private final NotificationListener burnMediaListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            burnMedia();
        }
    };

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }

    /**
     * Returns the prefix for FileSetID creation.
     * 
     * @return Returns the fileSetIdPrefix.
     */
    public String getFileSetIdPrefix() {
        return fileSetIdPrefix;
    }

    /**
     * Set the prefix for FileSetID creation.
     * 
     * @param fileSetIdPrefix
     *            The fileSetIdPrefix to set.
     */
    public void setFileSetIdPrefix(String fileSetIdPrefix) {
        this.fileSetIdPrefix = fileSetIdPrefix.toUpperCase();
    }

    /**
     * @return Returns the nrOfCopies.
     */
    public int getNrOfCopies() {
        return nrOfCopies;
    }

    /**
     * @param nrOfCopies
     *            The nrOfCopies to set.
     */
    public void setNrOfCopies(int nrOfCopies) {
        if (nrOfCopies < 1 || nrOfCopies > 99) {
            throw new IllegalArgumentException(
                    "Wrong number of copies! Please enter a number between 1 and 99 (incl.)");
        }
        this.nrOfCopies = nrOfCopies;
    }

    /**
     * Returns the max media usage for collecting studies.
     * <p>
     * The number of bytes, that can be used to collect studies for a media.<br>
     * This values is usually smaller than the real media size to save space for
     * index and optional html files.
     * 
     * @return Returns the maxMediaUsage in bytes.
     */
    public String getMaxMediaUsage() {
        return FileUtils.formatSize(maxMediaUsage);
    }

    /**
     * Set the max media usage for collecting studies.
     * 
     * @param maxMediaUsage
     *            The maxMediaUsage to set.
     */
    public void setMaxMediaUsage(String str) {
        this.maxMediaUsage = FileUtils.parseSize(str, MIN_MAX_MEDIA_USAGE);
    }

    /**
     * This value is used to get the search date from current date.
     * <p>
     * Instances must be older than the search date.
     * 
     * @return Returns ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMinStudyAge() {
        return RetryIntervalls.formatInterval(minStudyAge);
    }

    /**
     * Setter for minStudyAge.
     * <p>
     * This value is used to ensure that all instances of a study is stored on a
     * single media.
     * 
     * @param age
     *            ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public void setMinStudyAge(String age) {
        this.minStudyAge = RetryIntervalls.parseInterval(age);
    }

    /**
     * Returns the maxMediaAge before burned on media.
     * 
     * @return ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMaxStudyAge() {
        return RetryIntervalls.formatInterval(maxStudyAge);
    }

    /**
     * Sets the max study age in days.
     * <p>
     * This value is used to determine how long an instance may not be stored
     * offline.
     * 
     * @param maxStudyAge
     *            The maxStudyAge to set.
     */
    public void setMaxStudyAge(String age) {
        this.maxStudyAge = RetryIntervalls.parseInterval(age);
    }

    public final String getBurnMediaInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(burnMediaInterval);
    }

    public void setBurnMediaInterval(String interval) throws Exception {
        this.burnMediaInterval = RetryIntervalls.parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDCheckForMediaToBurn,
                    burnMediaListenerID, burnMediaListener);
            burnMediaListenerID = scheduler.startScheduler(
                    timerIDCheckForMediaToBurn, burnMediaInterval,
                    burnMediaListener);
        }
    }

    /**
     * @return Returns the automaticMediaCreation.
     */
    public boolean isAutomaticMediaCreation() {
        return automaticMediaCreation;
    }

    /**
     * @param automaticMediaCreation
     *            The automaticMediaCreation to set.
     */
    public void setAutomaticMediaCreation(boolean automaticMediaCreation) {
        this.automaticMediaCreation = automaticMediaCreation;
    }

    /**
     * @return Returns the notifyBurnMediaEmailTo.
     */
    public String getNotifyBurnMediaEmailTo() {
        return notifyBurnMediaEmailTo;
    }

    /**
     * @param notifyBurnMediaEmailTo
     *            The notifyBurnMediaEmailTo to set.
     */
    public void setNotifyBurnMediaEmailTo(String emails) {
        this.notifyBurnMediaEmailTo = emails.trim();
    }

    /**
     * @return Returns the notifyBurnMediaEmailFrom.
     */
    public String getNotifyBurnMediaEmailFrom() {
        return notifyBurnMediaEmailFrom == null ? "" : notifyBurnMediaEmailFrom;
    }

    /**
     * @param notifyBurnMediaEmailFrom
     *            The notifyBurnMediaEmailFrom to set.
     */
    public void setNotifyBurnMediaEmailFrom(String notifyBurnMediaEmailFrom) {
        if (notifyBurnMediaEmailFrom != null
                && notifyBurnMediaEmailFrom.trim().length() < 1)
            notifyBurnMediaEmailFrom = null;
        this.notifyBurnMediaEmailFrom = notifyBurnMediaEmailFrom;
    }

    public final ObjectName getSendmailServiceName() {
        return sendMailServiceName;
    }

    public final void setSendmailServiceName(ObjectName svName) {
        sendMailServiceName = svName;
    }

    public final String getScheduleMediaInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(scheduleMediaInterval);
    }

    public void setScheduleMediaInterval(String interval) throws Exception {
        this.scheduleMediaInterval = RetryIntervalls
                .parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDCheckForStudiesToSchedule,
                    scheduleMediaListenerID, scheduleMediaListener);
            scheduleMediaListenerID = scheduler.startScheduler(
                    timerIDCheckForStudiesToSchedule, scheduleMediaInterval,
                    scheduleMediaListener);
        }
    }

    public final String getUpdateMediaStatusInterval() {
        return RetryIntervalls
                .formatIntervalZeroAsNever(updateMediaStatusInterval);
    }

    public void setUpdateMediaStatusInterval(String interval) throws Exception {
        this.updateMediaStatusInterval = RetryIntervalls
                .parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerIDCheckMediaStatus,
                    updateMediaStatusListenerID, updateMediaStatusListener);
            updateMediaStatusListenerID = scheduler.startScheduler(
                    timerIDCheckMediaStatus, updateMediaStatusInterval,
                    updateMediaStatusListener);
        }
    }

    /**
     * Returns the retrieve AET defined in this MBean.
     * <p>
     * This AET performs the move operation.
     * 
     * @return The retrieve AET.
     */
    public String getRetrieveAET() {
        return retrieveAET;
    }

    /**
     * Set the retrieve AET.
     * 
     * @param aet
     *            The retrieve AET to set.
     */
    public void setRetrieveAET(String aet) {
        retrieveAET = aet;
    }

    /**
     * Return the move destination AET.
     * <p>
     * This AET is the destination of the move operation.
     * 
     * @return the destination AET.
     */
    public String getMoveDestinationAET() {
        return destAET;
    }

    /**
     * Set the move destination AET.
     * <p>
     * This AET must be on the same system as <code>mcMScpAET</code>.
     * 
     * @param aet
     *            AET to set.
     */
    public void setMoveDestinationAET(String aet) {
        destAET = aet;
    }

    /**
     * Returns the Media Creation Managment AET.
     * <p>
     * This AET performs the media creation.
     * 
     * @return The MCM AET.
     */
    public String getMcmScpAET() {
        return mcmScpAET;
    }

    /**
     * Set the Media Creation managment AET.
     * <p>
     * Thsi AET must be on the same system as <code>destAET</code>
     * 
     * @param aet
     *            AET to set.
     */
    public void setMcmScpAET(String aet) {
        mcmScpAET = aet;
    }

    /**
     * Returns the DICOM priority as int value.
     * <p>
     * This value is used for Move and media creation action (N-Action). 0..MED,
     * 1..HIGH, 2..LOW
     * 
     * @return Returns the priority.
     */
    public String getPriority() {
        return DicomPriority.toString(priority);
    }

    /**
     * Set the DICOM priority.
     * 
     * @param priority
     *            The priority to set.
     */
    public void setPriority(String priority) {
        this.priority = DicomPriority.toCode(priority);
    }

    /**
     * Returns 'NO' if the media should not contain none DICOM objects or the
     * named type of none DICOM object.
     * 
     * @return The type of none DICOM Objects to include or NO.
     */
    public String getIncludeNonDICOMObj() {
        return includeNonDICOMObj;
    }

    /**
     * Set the type of none DICOM object that should be included by media
     * creation.
     * <p>
     * Use NO if no such objects should be included.
     * <p>
     * Set the value to NO if argument is null!
     * 
     * @param includeNonDICOMObj
     *            The flag value to set.
     */
    public void setIncludeNonDICOMObj(String includeNonDICOMObj) {
        this.includeNonDICOMObj = includeNonDICOMObj;
    }

    /**
     * Returns true if media label should contain information extracted from
     * instances.
     * 
     * @return Returns the useInstanceInfo flag.
     */
    public boolean isUseInstanceInfo() {
        return useInstanceInfo;
    }

    /**
     * Set the useInstanceInfo flag.
     * <p>
     * true..Media label should contain info extracted from instances.
     * 
     * @param useInstanceInfo
     *            The flag value to set.
     */
    public void setUseInstanceInfo(boolean useInstanceInfo) {
        this.useInstanceInfo = useInstanceInfo;
    }

    /**
     * Collect studies to media for media creation. (offline storage)
     * <p>
     * Search for instances that are older than <code>daysBefore</code> and
     * are not already assigned to a media.<br>
     * Collect this instances to studies to ensure that all instances of a study
     * are on one media.<br>
     * Assign instances to media for best media usage using maxMediaUsage as
     * limit.
     * 
     * @return Number of instances
     */
    public int scheduleMedia() {
        log.info("Check for studies for scheduling on media");
        long l1 = System.currentTimeMillis();
        MediaComposer mc = null;
        try {
            mc = this.lookupMediaComposer();
            Collection studies = mc.getStudiesReceivedBefore(getSearchDate());
            if ( studies.isEmpty() ) {
            	log.info("No Studies found for scheduling on media!");
            	return 0;
            }
            List mediaPool = null;
            String prefix = getFileSetIdPrefix();
            log.info(studies.size() + " are selected for scheduling on media!");
            for (Iterator iter = studies.iterator(); iter.hasNext();) {
                mediaPool = mc.assignStudyToMedia((StudyLocal) iter.next(),
                        mediaPool, maxMediaUsage, prefix);
            }
            log.info("Schedule of " + studies.size()
                    + " studies completed. Number of collecting media:"
                    + mediaPool.size() + ". time:"
                    + (System.currentTimeMillis() - l1) + " ms!");
            return studies.size();
        } catch (Exception x) {
            log.error("Can not create MediaComposer!", x);
            return -1;
        }
    }

    /**
     * Returns the search date as long for finding instances to collect.
     * <p>
     * This value is current time minus <code>daysBefore</code> in ms.
     * 
     * @return The search date
     */
    private long getSearchDate() {
        return System.currentTimeMillis() - minStudyAge;
    }

    /**
     * Start listening to the JMS queue deined in <code>QUEUE</code>
     * <p>
     * This queue is used to receive media creation request from scheduler or
     * web interface.
     */
    protected void startService() throws Exception {
        scheduleMediaListenerID = scheduler.startScheduler(
                timerIDCheckForStudiesToSchedule, scheduleMediaInterval,
                scheduleMediaListener);
        updateMediaStatusListenerID = scheduler.startScheduler(
                timerIDCheckMediaStatus, updateMediaStatusInterval,
                updateMediaStatusListener);
        burnMediaListenerID = scheduler.startScheduler(
                timerIDCheckForMediaToBurn, burnMediaInterval,
                burnMediaListener);
        jmsDelegate.startListening(queueName, this, concurrency);
    }

    /**
     * Stop listening to the JMS queue deined in <code>QUEUE</code>
     * 
     */
    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDCheckForStudiesToSchedule,
                scheduleMediaListenerID, scheduleMediaListener);
        scheduler.stopScheduler(timerIDCheckMediaStatus,
                updateMediaStatusListenerID, updateMediaStatusListener);
        scheduler.stopScheduler(timerIDCheckForMediaToBurn,
                burnMediaListenerID, burnMediaListener);
        jmsDelegate.stopListening(queueName);
        super.stopService();
    }

    public void scheduleMediaCreation(MediaDTO mediaDTO) throws Exception {
        jmsDelegate.queue(queueName, mediaDTO, Message.DEFAULT_PRIORITY, 0L);
    }

    /**
     * Handles a JMS message.
     * 
     * @param message
     *            The JMS message to handle.
     */
    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        MediaDTO mediaDTO = null;
        try {
            mediaDTO = (MediaDTO) om.getObject();
            log.info("Start processing " + mediaDTO);
            process(mediaDTO);
        } catch (JMSException e) {
            handleError(mediaDTO, "jms error during processing message: "
                    + message, e);
        } catch (Throwable e) {
            handleError(mediaDTO,
                    "unexpected error during processing message: " + message, e);
        }
    }

    /**
     * Process a media creation request received from JMS queue.
     * <p>
     * <DL>
     * <DD>1) Set media status to PROCESSING.</DD>
     * <DD>2) Move instances to <code>destAET</code> using
     * <code>retrieveAET</code>.</DD>
     * <DD>3) Send media creation request to <code>mcMScpAET</code>. </DD>
     * <DD>4) Send N-Action command for media creation to
     * <code>mcMScpAET</code>. </DD>
     * </DL>
     * 
     * @param mediaDTO
     *            The MediaDTO object for the media to create.
     * 
     * @throws RemoteException
     * @throws FinderException
     * @throws HomeFactoryException
     * @throws CreateException
     */
    private void process(MediaDTO mediaDTO) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        this.lookupMediaComposer().setMediaCreationRequestIuid(
                mediaDTO.getPk(), null);
        this.lookupMediaComposer().setMediaStatus(mediaDTO.getPk(),
                MediaDTO.TRANSFERING, "");
        if (processMove(mediaDTO)) {
            log.info("Move instances of " + mediaDTO.getFilesetId() + " done!");
            this.lookupMediaComposer().setMediaStatus(mediaDTO.getPk(),
                    MediaDTO.BURNING, "");
            if (processMediaCreation(mediaDTO)) {
                log.info("Sending media creation request of "
                        + mediaDTO.getFilesetId() + " done!");
                return;
            }
        }
        log.error("processing " + mediaDTO.getFilesetId() + " failed!!!");
    }

    /**
     * Process the move command on study level.
     * <p>
     * Move instances of all studies defined for given media to
     * <code>destAET</code>.<br>
     * Use <code>retrieveAET</code> to perform the move command.
     * <p>
     * Moves all instances of the studies regardless if all instances assigned
     * to this media.<br>
     * (The normal case is that all instances of a study is assigned to one
     * media!)<br>
     * This has no effect for the creted media because the media creation
     * request contains only instances assigned to this media!<br>
     * The unassigned instances are deleted after a timeout by the
     * <code>mcMScpAET</code>.
     * 
     * @param mediaDTO
     *            The MediaDTO to process.
     * 
     * @return true if move was sucessful, false if move failed.
     */
    private boolean processMove(MediaDTO mediaDTO) {
        ActiveAssociation aa;
        try {
            aa = openAssociation(retrieveAET,
                            UIDs.StudyRootQueryRetrieveInformationModelMOVE);
        } catch (Exception e) {
                handleError(mediaDTO, "processMove failed for "
                        + mediaDTO.getFilesetId(), e);
                return false;       
        }
        try {
            Association as = aa.getAssociation();
            DcmObjectFactory oFact = DcmObjectFactory.getInstance();
            Command cmd = oFact.newCommand();
            String[] studyUIDs = getStudyUids(mediaDTO);
            Dataset ds = oFact.newDataset();

            ds.putCS(Tags.QueryRetrieveLevel, "STUDY");
            for (int i = 0, len = studyUIDs.length; i < len; i++) {
                cmd.initCMoveRQ(as.nextMsgID(),
                        UIDs.StudyRootQueryRetrieveInformationModelMOVE,
                        priority, this.getMoveDestinationAET());
                ds.putUI(Tags.StudyInstanceUID, studyUIDs[i]);
                Dimse moveRQ = AssociationFactory.getInstance().newDimse(1,
                        cmd, ds);
                FutureRSP rsp = aa.invoke(moveRQ);
                Dimse dimse = rsp.get();
                if (!checkResponse(dimse, mediaDTO, "for study:" + studyUIDs[i])) {
                    return false;
                }
            }
        } catch (Exception e) {
            handleError(mediaDTO, "processMove failed for "
                    + mediaDTO.getFilesetId() + "! Reason: unexpected error", e);
            return false;
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn("Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
        return true;
    }

    /**
     * Process the media creation request.
     * <DL>
     * <DD>1) Send media creation request to <code>mcMScpAET</code>. </DD>
     * <DD>2) Send N-Action command for media creation to
     * <code>mcMScpAET</code>. </DD>
     * </DL>
     * <p>
     * 
     * @param mediaDTO
     *            The MediaDTO object to process.
     */
    private boolean processMediaCreation(MediaDTO mediaDTO) {
        ActiveAssociation aa;
        try {
            aa = openAssociation(mcmScpAET,
                            UIDs.MediaCreationManagementSOPClass);
        } catch (Exception e) {
                handleError(mediaDTO, "processMove failed for "
                        + mediaDTO.getFilesetId(), e);
                return false;       
        }
        try {
            DcmObjectFactory oFact = DcmObjectFactory.getInstance();
            Command cmd = oFact.newCommand();
            cmd.initNCreateRQ(1, UIDs.MediaCreationManagementSOPClass, null);
            Dataset ds = getMediaCreationReqDS(mediaDTO);
            Dimse mcRQ = AssociationFactory.getInstance().newDimse(1, cmd, ds);
            FutureRSP rsp = aa.invoke(mcRQ);
            Dimse dimse = rsp.get();
            if (!checkResponse(dimse, mediaDTO, "")) {
                return false;
            }
            String iuid = dimse.getCommand().getAffectedSOPInstanceUID();
            Command cmdRsp = dimse.getCommand();
            Dataset dataRsp = dimse.getDataset();
            // send action
            FutureRSP futureRsp = aa.invoke(AssociationFactory.getInstance()
                    .newDimse(1, oFact.newCommand().initNActionRQ(3,
                                    UIDs.MediaCreationManagementSOPClass, iuid,
                                    INITIATE_MEDIA_CREATION),
                            getMediaCreationActionDS()));
            dimse = futureRsp.get();
            this.lookupMediaComposer().setMediaCreationRequestIuid(
                    mediaDTO.getPk(), iuid);
            if (!checkResponse(dimse, mediaDTO, "")) {
                return false;
            }

        } catch (Exception e) {
            handleError(mediaDTO, "processMediaCreation failed for "
                    + mediaDTO.getFilesetId() + "! Reason: unexpected error", e);
            return false;
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn("Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
        return true;
    }

    /**
     * Get the initialized media creation request dataset.
     * 
     * @return The Dataset for media creation request.
     * 
     * @throws CreateException
     * @throws HomeFactoryException
     * @throws FinderException
     * @throws RemoteException
     */
    private Dataset getMediaCreationReqDS(MediaDTO mediaDTO)
            throws RemoteException, FinderException, HomeFactoryException,
            CreateException {
        Dataset ds = lookupMediaComposer().prepareMediaCreationRequest(
                mediaDTO.getPk());
        ds.putCS(Tags.LabelUsingInformationExtractedFromInstances, this
                .isUseInstanceInfo() ? "YES" : "NO");
        ds.putCS(Tags.IncludeNonDICOMObjects, includeNonDICOMObj);
        if (log.isDebugEnabled()) {
            log.debug("getMediaCreationReqDS:\n");
            log.info(ds);
        }
        return ds;
    }

    /**
     * Get the dataset for N-Action command.
     * <p>
     * Set the defined priority in dataset.
     * 
     * @return Dateset for N-Action command
     */
    private Dataset getMediaCreationActionDS() {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        ds.putIS(Tags.NumberOfCopies, nrOfCopies);
        ds.putCS(Tags.RequestPriority, prioStrings[priority]);
        return ds;
    }

    /**
     * Handles an error.
     * <p>
     * <DL>
     * <DD>1) Log the error.</DD>
     * <DD>2) Set the media status to an error status with status info.
     * </DL>
     * 
     * @param mediaDTO
     *            The media effected.
     * @param msg
     *            The error message. (for logging and status info)
     * @param x
     *            A throwable object used for logging.
     */
    private void handleError(MediaDTO mediaDTO, String msg, Throwable x) {
        log.error(msg, x);
        if (mediaDTO != null) {
            try {
                this.lookupMediaComposer().setMediaStatus(mediaDTO.getPk(),
                        MediaDTO.ERROR, msg);
            } catch (Exception e) {
                log.error("cant set error media status for "
                        + mediaDTO.getFilesetId());
            }
        }

    }

    /**
     * Check a response DICOM message for return status.
     * <p>
     * Log a warning if status is <code>Status.AttributeValueOutOfRange</code>.
     * <p>
     * Call <code>handleError</code> with status info and error comment if
     * message indicates a failure.
     * <p>
     * This method is generic for the usage in this class because the status
     * values of move, create and action doesnt conflict!
     * 
     * @param mediaDTO
     *            The effected media
     * @param dimse
     *            The DICOM message to check.
     * 
     * @return true if status is OK, false if message indicates a failure.
     */
    private boolean checkResponse(Dimse rsp, MediaDTO mediaDTO, String msg) {
        Command cmdRsp = rsp.getCommand();
        Dataset dataRsp = null;
        try {
            dataRsp = rsp.getDataset();
        } catch (IOException e) {
            log.error("Cant get Dataset from response message!", e);
        }
        int status = cmdRsp.getStatus();
        switch (status) {
        case Status.AttributeValueOutOfRange:
            log.warn("Warning: Attribute Value Out Of Range: "
                    + cmdRsp.getString(Tags.ErrorComment, "") + dataRsp);
        case Status.Success:
            return true;
        }
        log.error("Media creation failed! " + " [" + msg + "]"
                + "Failure Status " + Integer.toHexString(status) + ": "
                + cmdRsp.getString(Tags.ErrorComment, "") + dataRsp);
        handleError(mediaDTO, "Media creation failed! return status:"
                + Integer.toHexString(status) + " Reason:"
                + cmdRsp.getString(Tags.ErrorComment, "") + " [" + msg + "]",
                null);
        return false;
    }

    /**
     * Return an array of study UIDs for given media.
     * 
     * @param mediaDTO
     *            The MediaDTO object.
     * 
     * @return String array with study UIDs.
     * 
     * @throws HomeFactoryException
     * @throws CreateException
     * @throws FinderException
     * @throws RemoteException
     */
    private String[] getStudyUids(MediaDTO mediaDTO)
            throws HomeFactoryException, RemoteException, FinderException,
            CreateException {
        Collection c = this.lookupMediaComposer().getStudyUIDSForMedia(
                mediaDTO.getPk());
        return (String[]) c.toArray(new String[c.size()]);
    }

    public String updateMediaStatus() {
        List procList;
        try {
            procList = this.lookupMediaComposer().getWithStatus(
                    MediaDTO.BURNING);
            if (procList.isEmpty())
                return "No Media in processing status.";
        } catch (Exception e) {
            log.error("Check for pending media creation processes fails!", e);
            return "Error: Check for pending media creation processes fails!";
        }
        ActiveAssociation aa;
        try {
            aa = openAssociation(mcmScpAET,
                            UIDs.MediaCreationManagementSOPClass);
        } catch (Exception e) {
            log.error("Cant get media creation status!", e);
            return "Error: could not open association!";
        }
        try {
            Association as = aa.getAssociation();
            String iuid = null;
            MediaDTO mediaDTO = null;
            int[] getAttrs = new int[] { Tags.ExecutionStatus,
                    Tags.ExecutionStatusInfo, Tags.FailedSOPSeq };
            int mediaStatus;
            int mediaWithAction = 0;
            int mediaDone = 0;
            int mediaFailed = 0;
            for (Iterator iter = procList.iterator(); iter.hasNext();) {
                mediaDTO = (MediaDTO) iter.next();
                iuid = mediaDTO.getMediaCreationRequestIuid();
                mediaStatus = mediaDTO.getMediaStatus();
                if (iuid != null && iuid.length() > 0) {
                    mediaWithAction++;
                    Command cmdRq = DcmObjectFactory.getInstance().newCommand();
                    cmdRq.initNGetRQ(as.nextMsgID(),
                            UIDs.MediaCreationManagementSOPClass, iuid,
                            getAttrs);
                    FutureRSP futureRsp = aa.invoke(AssociationFactory
                            .getInstance().newDimse(1, cmdRq));
                    Dimse rsp = futureRsp.get();
                    Command cmdRsp = rsp.getCommand();
                    Dataset dataRsp = rsp.getDataset();
                    int status = cmdRsp.getStatus();
                    if (status != 0) {
                        log
                                .error("Cant get media creation status! Failure Status:"
                                        + Integer.toHexString(status)
                                        + ": "
                                        + cmdRsp.getString(Tags.ErrorComment,
                                                "")
                                        + (dataRsp == null ? ""
                                                : ("\n" + dataRsp)));
                    } else {
                        if (log.isDebugEnabled())
                            log.debug("Received Attributes:\n" + dataRsp);
                        String execStatus = dataRsp
                                .getString(Tags.ExecutionStatus);
                        if ("DONE".equals(execStatus)) {
                            mediaDone++;
                            mediaComposer.setMediaStatus(mediaDTO.getPk(),
                                    MediaDTO.COMPLETED,
                                    "successfully completed");
                            if (log.isInfoEnabled())
                                log.info("Media " + mediaDTO.getFilesetId()
                                        + " successfully created!");
                        } else if ("FAILURE".equals(execStatus)) {
                            mediaFailed++;
                            String info = dataRsp
                                    .getString(Tags.ExecutionStatusInfo);
                            if ("NO_INSTANCE".equals(info)) {
                                info = info + "("
                                        + dataRsp.vm(Tags.FailedSOPSeq)
                                        + " number of instances missing)";
                            }
                            log.error("Cant create media "
                                    + mediaDTO.getFilesetId() + "! Reason:"
                                    + info);
                            mediaComposer.setMediaStatus(mediaDTO.getPk(),
                                    MediaDTO.ERROR, info);
                        } else {
                            mediaComposer.setMediaStatus(mediaDTO.getPk(),
                                    mediaStatus, execStatus);
                        }
                    }
                }// end if iuid
            }// end for
            return "Media creation status:" + mediaFailed + " media FAILED, "
                    + mediaDone + " media done! Total: " + procList.size()
                    + " media processing / " + mediaWithAction
                    + " with N-ACTION";
        } catch (Exception x) {
            log.error("Cant get media creation status! Reason: unexpected error.",
                            x);
            return "Cant get media create status: Unexpected error"
                    + x.getMessage();
        } finally {
            try {
                aa.release(true);
            } catch (Exception e) {
                log.warn("Failed to release association " + aa.getAssociation(),
                        e);
            }
        }
    }

    /**
     * Initiate creation of Media with studies older than MaxStudyAge.
     * 
     * @return Number of media creations initiated.
     */
    public int burnMedia() {
        log.info("Check for scheduled Media to burn");
        try {
            Collection c = lookupMediaComposer().getWithStatus(MediaDTO.OPEN);
            long maxAgeDate = System.currentTimeMillis()
                    - (maxStudyAge - minStudyAge);// media is created after
                                                    // minStudyAge -> max media
                                                    // age is
                                                    // maxStudyAge-minStudyAge
            List mediaToBurn = new ArrayList();
            MediaDTO mediaDTO;
            for (Iterator iter = c.iterator(); iter.hasNext();) {
                mediaDTO = (MediaDTO) iter.next();
                if (mediaDTO.getCreatedTime().getTime() < maxAgeDate) {
                    if (this.isAutomaticMediaCreation()) {
                        process(mediaDTO);
                    }
                    mediaToBurn.add(mediaDTO);
                }
            }
            if (!this.isAutomaticMediaCreation()) {
                notifyMediaToBurn(mediaToBurn);
            }
            return mediaToBurn.size();
        } catch (Exception e) {
            log.error("Failed to initiate media creation:", e);
            return -1;
        }
    }

    /**
     * @param mediaToBurn
     * @throws JMSException
     */
    private void notifyMediaToBurn(List mediaToBurn) throws JMSException {
        log.info("Notify " + this.notifyBurnMediaEmailTo + " that "
                + mediaToBurn.size() + " media are ready to burn!");
        try {
            Object o = server.invoke(sendMailServiceName, "send", new Object[] {
                    "Media Creation Service: media ready to burn!",
                    notifyBurnMediaEmailFrom, notifyBurnMediaEmailTo,
                    formatBody(mediaToBurn) }, new String[] {
                    String.class.getName(), String.class.getName(),
                    String.class.getName(), String.class.getName() });
        } catch (Exception x) {
            log.error("Exception occured in notifyMediaToBurn: "
                    + x.getMessage(), x);
        }

    }

    /**
     * @param mediaToBurn
     * @return
     */
    private String formatBody(List mediaToBurn) {
        StringBuffer sb = new StringBuffer(
                "Media Creation Notification: Following media are ready to burn:\n");
        for (Iterator iter = mediaToBurn.iterator(); iter.hasNext();) {
            sb.append(((MediaDTO) iter.next()).getFilesetId()).append(",");
        }
        return sb.toString();
    }

    /**
     * Checks the availability of Media Creation Managment SCP service.
     * <p>
     * Checks if the move destination is available and support
     * SecondaryCaptureImageStorage.<br>
     * Checks if the MediaCreation managment service is availabel.
     * <p>
     * 
     * @return Returns OK, MOVE_DEST_UNAVAIL or MCM_SCP_UNAVAIL
     */
    public String checkMcmScpAvail() {
        ActiveAssociation aa;
        try {
            aa = openAssociation(destAET,
                            UIDs.SecondaryCaptureImageStorage);
        } catch (Exception e) {
            log.info("Move destination (" + destAET + ") is not available!", e);
            return "MOVE_DEST_UNAVAIL";
        }
        try {
            aa.release(true);
        } catch (Exception e) {
            log.info("Failed to release association " + aa.getAssociation(),
                    e);
        }
        try {
            aa = openAssociation(mcmScpAET,
                            UIDs.MediaCreationManagementSOPClass);
        } catch (Exception e) {
            log.info("MCM SCP (" + mcmScpAET + ") is not available!", e);
            return "MCM_SCP_UNAVAIL";
        }
        try {
            aa.release(true);
        } catch (Exception e) {
            log.info("Failed to release association " + aa.getAssociation(),
                    e);
        }
        return "OK";
    }

    /**
     * Checks if all instances of a media are available (ONLINE).
     * 
     * @param mediaPk
     *            Primary key of media
     * 
     * @return true if all instances are ONLINE
     * 
     * @throws RemoteException
     * @throws FinderException
     * @throws HomeFactoryException
     * @throws CreateException
     */
    public boolean checkMediaInstances(long mediaPk) throws RemoteException,
            FinderException, HomeFactoryException, CreateException {
        return lookupMediaComposer().checkInstancesAvailable(new Long(mediaPk));
    }

    /**
     * Delete a media.
     * 
     * @param mediaPk
     *            Primary key of media.
     * 
     * @throws RemoteException
     * @throws RemoveException
     * @throws FinderException
     * @throws HomeFactoryException
     * @throws CreateException
     */
    public void deleteMedia(Long mediaPk) throws RemoteException,
            RemoveException, FinderException, HomeFactoryException,
            CreateException {
        lookupMediaComposer().deleteMedia(mediaPk);
    }

    /**
     * Returns the MediaComposer session bean.
     * 
     * @return The MediaComposer.
     * 
     * @throws HomeFactoryException
     * @throws RemoteException
     * @throws CreateException
     */
    private MediaComposer lookupMediaComposer() throws HomeFactoryException,
            RemoteException, CreateException {
        if (mediaComposer == null) {
            MediaComposerHome home = (MediaComposerHome) EJBHomeFactory
                    .getFactory().lookup(MediaComposerHome.class,
                            MediaComposerHome.JNDI_NAME);
            mediaComposer = home.create();
        }
        return mediaComposer;
    }

    public String getTimerIDCheckForMediaToBurn() {
        return timerIDCheckForMediaToBurn;
    }

    public void setTimerIDCheckForMediaToBurn(String timerIDCheckForMediaToBurn) {
        this.timerIDCheckForMediaToBurn = timerIDCheckForMediaToBurn;
    }

    public String getTimerIDCheckForStudiesToSchedule() {
        return timerIDCheckForStudiesToSchedule;
    }

    public void setTimerIDCheckForStudiesToSchedule(
            String timerIDCheckForStudiesToSchedule) {
        this.timerIDCheckForStudiesToSchedule = timerIDCheckForStudiesToSchedule;
    }

    public String getTimerIDCheckMediaStatus() {
        return timerIDCheckMediaStatus;
    }

    public void setTimerIDCheckMediaStatus(String timerIDCheckMediaStatus) {
        this.timerIDCheckMediaStatus = timerIDCheckMediaStatus;
    }
}
