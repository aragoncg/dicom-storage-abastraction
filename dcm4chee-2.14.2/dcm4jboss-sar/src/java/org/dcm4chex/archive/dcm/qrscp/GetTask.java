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
package org.dcm4chex.archive.dcm.qrscp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.exceptions.NoPresContextException;
import org.jboss.logging.Logger;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since May 6, 2008
 */
class GetTask implements Runnable {

    private final QueryRetrieveScpService service;
    private final Logger log;
    private ActiveAssociation assoc;
    private String callingAET;
    private String calledAET;
    private final int pcid;
    private final Dataset rqData;
    private final int priority;
    private final int msgID;
    private final String sopClassUID;
    private final ArrayList<FileInfo> transferred = new ArrayList<FileInfo>();
    private final Set<String> remainingIUIDs;
    private final Set<String> failedIUIDs;

    private int total;

    private int warnings = 0;

    private int completed = 0;

    private boolean canceled = false;

    private RetrieveInfo retrieveInfo;

    private MoveScu moveScu;

    private DimseListener cancelListener = new DimseListener() {
        public void dimseReceived(Association assoc, Dimse dimse) {
            canceled = true;
            MoveScu tmp = moveScu;
            if (tmp != null) {
                tmp.forwardCancelRQ(dimse);
            }
        }
    };

    private TimerTask sendPendingRsp = new TimerTask() {
        public void run() {
            if (!canceled && !remainingIUIDs.isEmpty()) {
                sendGetRsp(Status.Pending, null);
            }
        }};

    public GetTask(QueryRetrieveScpService service, ActiveAssociation assoc,
            int pcid, Command rqCmd, Dataset rqData, FileInfo[][] fileInfos) {
        this.service = service;
        this.log = service.getLog();
        this.assoc = assoc;
        this.callingAET = assoc.getAssociation().getCallingAET();
        this.calledAET = assoc.getAssociation().getCalledAET();
        this.pcid = pcid;
        this.rqData = rqData;
        this.sopClassUID = rqCmd.getAffectedSOPClassUID();
        this.priority = rqCmd.getInt(Tags.Priority, Command.MEDIUM);
        this.msgID = rqCmd.getMessageID();
        this.total = fileInfos.length;
        this.retrieveInfo = new RetrieveInfo(service, fileInfos);
        this.remainingIUIDs = retrieveInfo.getAvailableIUIDs();
        this.failedIUIDs = retrieveInfo.getNotAvailableIUIDs();
        assoc.addCancelListener(msgID, cancelListener);
    }

    private void removeInstancesOfUnsupportedStorageSOPClasses() {
        Association a = assoc.getAssociation();
        Iterator<String> it = retrieveInfo.getCUIDs();
        String cuid;
        Set<String> iuids;
        while (it.hasNext()) {
            cuid = it.next();
            if (a.listAcceptedPresContext(cuid).isEmpty()) {
                iuids = retrieveInfo.removeInstancesOfClass(cuid);
                it.remove(); // Use Iterator itself to remove the current
                                // item to avoid ConcurrentModificationException
                final String prompt = "No Presentation Context for "
                        + QueryRetrieveScpService.uidDict.toString(cuid)
                        + " offered by " + callingAET
                        + "\n\tCannot send " + iuids.size()
                        + " instances of this class";
                if (!service.isIgnorableSOPClass(cuid, callingAET)) {
                    failedIUIDs.addAll(iuids);
                    log.warn(prompt);
                } else {
                    completed += iuids.size();
                    log.info(prompt);
                }
                remainingIUIDs.removeAll(iuids);
            }
        }
    }

    public void run() {
        service.scheduleSendPendingRsp(sendPendingRsp);
        try {
            Set<String> localUIDs = retrieveInfo.removeLocalIUIDs();
            boolean updateLocalUIDs = false;
            while (!canceled && retrieveInfo.nextMoveForward()) {
                String retrieveAET = retrieveInfo.getMoveForwardAET();
                Collection<String> iuids = retrieveInfo.getMoveForwardUIDs();
                moveScu = new MoveScu(service, calledAET, calledAET,
                        retrieveAET, 0);
                if (iuids.size() == total) {
                    moveScu.forwardMoveRQ(pcid, msgID, priority,
                            service.getLocalStorageAET(), rqData, iuids);
                } else {
                    moveScu.splitAndForwardMoveRQ(pcid, msgID, priority,
                            service.getLocalStorageAET(), iuids);
                }
                if (moveScu.completed() > 0 || moveScu.warnings() > 0) {
                    updateLocalUIDs = true;
                }
                moveScu = null;
            }
            if (!canceled) {
                try {
                    if (updateLocalUIDs) {
                        FileInfo[][] fileInfo = 
                            RetrieveCmd.create(rqData).getFileInfos();
                        retrieveInfo = new RetrieveInfo(service, fileInfo);
                        localUIDs = retrieveInfo.removeLocalIUIDs();
                    }
                    if (!localUIDs.isEmpty()) {
                        service.prefetchTars(retrieveInfo.getLocalFiles());
                        removeInstancesOfUnsupportedStorageSOPClasses();
                        retrieveInstances();
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
            
        } finally {
            sendPendingRsp.cancel();
        }
        if (!canceled) {
            failedIUIDs.addAll(remainingIUIDs);
            remainingIUIDs.clear();
        }
        sendGetRsp(status(), service.makeRetrieveRspIdentifier(failedIUIDs));
    }

    private void retrieveInstances() {
        Set<StudyInstanceUIDAndDirPath> studyInfos =
            new HashSet<StudyInstanceUIDAndDirPath>();
        Association a = assoc.getAssociation();
        Collection<List<FileInfo>> localFiles = retrieveInfo.getLocalFiles();
        for (List<FileInfo> list : localFiles) {
            final FileInfo fileInfo = list.get(0);
            final String iuid = fileInfo.sopIUID;
            DimseListener storeScpListener = new DimseListener() {

                public void dimseReceived(Association assoc, Dimse dimse) {
                    switch (dimse.getCommand().getStatus()) {
                    case Status.Success:
                        ++completed;
                        transferred.add(fileInfo);
                        break;
                    case Status.CoercionOfDataElements:
                    case Status.DataSetDoesNotMatchSOPClassWarning:
                    case Status.ElementsDiscarded:
                        ++warnings;
                        transferred.add(fileInfo);
                        break;
                    default:
                        failedIUIDs.add(iuid);
                    break;
                    }
                    remainingIUIDs.remove(iuid);
                }
            };

            try {
                Dimse rq = service.makeCStoreRQ(assoc, fileInfo,
                        priority, null, 0, null);
                assoc.invoke(rq, storeScpListener);
            } catch (NoPresContextException e) {
                if (!service.isIgnorableSOPClass(fileInfo.sopCUID, 
                        a.getCallingAET())) {
                    failedIUIDs.add(fileInfo.sopIUID);
                    log.warn(e.getMessage());
                } else {
                    log.info(e.getMessage());
                }
            } catch (Exception e) {
                log.error("Exception during retrieve of " + iuid, e);
            }
            // track access on ONLINE FS
            if (fileInfo.availability == Availability.ONLINE) {
                studyInfos.add(new StudyInstanceUIDAndDirPath(fileInfo));
            }
            if (canceled 
                    || a.getState() != Association.ASSOCIATION_ESTABLISHED) {
                break;
            }
        }
        if (a.getState() == Association.ASSOCIATION_ESTABLISHED) {
            try {
                assoc.waitForPendingRSP();
            } catch (InterruptedException e) {
                log.warn("Exception during wait for pending C-STORE RSP:", e);
            }
        }
        if (!transferred.isEmpty()) {
            service.logInstancesSent(a, a, transferred);
        }
        service.updateStudyAccessTime(studyInfos);
    }

    private int status() {
        return canceled ? Status.Cancel 
                : failedIUIDs.isEmpty() ? Status.Success
                : completed == 0 && warnings == 0 
                        ? Status.UnableToPerformSuboperations
                        : Status.SubOpsOneOrMoreFailures;
    }

    private void sendGetRsp(int status, Dataset ds) {
        if (assoc == null)
            return;
        Command rspCmd = DcmObjectFactory.getInstance().newCommand();
        rspCmd.initCGetRSP(msgID, sopClassUID, status);
        if (!remainingIUIDs.isEmpty()) {
            rspCmd.putUS(Tags.NumberOfRemainingSubOperations,
                    remainingIUIDs.size());
        } else {
            rspCmd.remove(Tags.NumberOfRemainingSubOperations);
        }
        rspCmd.putUS(Tags.NumberOfCompletedSubOperations, completed);
        rspCmd.putUS(Tags.NumberOfWarningSubOperations, warnings);
        rspCmd.putUS(Tags.NumberOfFailedSubOperations, failedIUIDs.size());
        try {
            assoc.getAssociation().write(
                    AssociationFactory.getInstance().newDimse(pcid, rspCmd, ds));
        } catch (Exception e) {
            log.info("Failed to send C-GET RSP to "
                    + assoc.getAssociation().getCallingAET(), e);
            assoc  = null;
        }
    }

}
