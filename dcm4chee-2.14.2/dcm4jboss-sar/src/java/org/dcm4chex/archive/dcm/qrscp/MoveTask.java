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

package org.dcm4chex.archive.dcm.qrscp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.dcm4che.auditlog.AuditLoggerFactory;
import org.dcm4che.auditlog.InstancesAction;
import org.dcm4che.auditlog.RemoteNode;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.AAbort;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4che.net.PDU;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfMonDelegate;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 8177 $
 * @since 16.09.2003
 */
public class MoveTask implements Runnable {

    protected final QueryRetrieveScpService service;

    protected final Logger log;

    private final String moveDest;

    private final AEDTO aeData;

    private final int movePcid;

    private final Dataset moveRqData;

    private final String moveOriginatorAET;

    private final String moveCalledAET;

    private final int priority;

    private final int msgID;

    private final String sopClassUID;

    private ActiveAssociation moveAssoc;

    private final Set<String> remainingIUIDs;

    private final Set<String> failedIUIDs;

    private final int total;

    private int remaining;

    private int warnings = 0;

    private int completed = 0;

    private boolean canceled = false;

    private boolean moveAssocClosed = false;
    
    private ArrayList<FileInfo> successfulTransferred =
            new ArrayList<FileInfo>();
    
    private MoveScu moveScu;

    private boolean directForwarding;

    private RemoteNode remoteNode;

    private InstancesAction instancesAction;

    private RetrieveInfo retrieveInfo;

    private Dataset stgCmtActionInfo;
    
    private DcmElement refSOPSeq;

    private PerfMonDelegate perfMon;

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
            if (!canceled && remaining > 0) {
                Command rsp = makeMoveRsp(Status.Pending);
                MoveScu tmp = moveScu;
                if (directForwarding && tmp != null) {
                    tmp.adjustPendingRsp(rsp);
                }
                if (rsp.getInt(Tags.NumberOfRemainingSubOperations, 0) > 0) {
                    notifyMoveSCU(rsp, null);
                }
            }
        }};

    public MoveTask(QueryRetrieveScpService service,
            ActiveAssociation moveAssoc, int movePcid, Command moveRqCmd,
            Dataset moveRqData, FileInfo[][] fileInfo, AEDTO aeData,
            String moveDest) throws DcmServiceException {
        this.service = service;
        this.log = service.getLog();
        this.moveAssoc = moveAssoc;
        this.movePcid = movePcid;
        this.moveRqData = moveRqData;
        this.aeData = aeData;
        this.moveDest = moveDest;
        this.moveOriginatorAET = moveAssoc.getAssociation().getCallingAET();
        this.moveCalledAET = moveAssoc.getAssociation().getCalledAET();
        this.perfMon = service.getMoveScp().getPerfMonDelegate();
        this.priority = moveRqCmd.getInt(Tags.Priority, Command.MEDIUM);
        this.msgID = moveRqCmd.getMessageID();
        this.sopClassUID = moveRqCmd.getAffectedSOPClassUID();
        this.total = fileInfo.length;
        this.remaining = total;
        this.retrieveInfo = new RetrieveInfo(service, fileInfo);
        this.remainingIUIDs = retrieveInfo.getAvailableIUIDs();
        this.failedIUIDs = retrieveInfo.getNotAvailableIUIDs();
        moveAssoc.addCancelListener(msgID, cancelListener);
    }

    private ActiveAssociation openAssociation() throws Exception {
        AssociationFactory asf = AssociationFactory.getInstance();
        Association a = asf.newRequestor(
                service.createSocket(moveCalledAET, aeData));
        a.setAcTimeout(service.getAcTimeout());
        a.setDimseTimeout(service.getDimseTimeout());
        a.setSoCloseDelay(service.getSoCloseDelay());
        a.putProperty("MoveAssociation", moveAssoc);
        AAssociateRQ rq = asf.newAAssociateRQ();
        rq.setCalledAET(moveDest);
        rq.setCallingAET(moveAssoc.getAssociation().getCalledAET());
        int maxOpsInvoked = service.getMaxStoreOpsInvoked();
        if (maxOpsInvoked != 1) {
            rq.setAsyncOpsWindow(asf.newAsyncOpsWindow(maxOpsInvoked, 1));
        }
        retrieveInfo.addPresContext(rq,
                service.isSendWithDefaultTransferSyntax(moveDest),
                service.isOfferNoPixelData(moveDest),
                service.isOfferNoPixelDataDeflate(moveDest));

        perfMon.assocEstStart(a, Command.C_STORE_RQ);
        PDU pdu = a.connect(rq);
        perfMon.assocEstEnd(a, Command.C_STORE_RQ);
        if (!(pdu instanceof AAssociateAC)) {
            throw new IOException("Association not accepted by "
                    + moveDest + ":\n" + pdu);
        }
        ActiveAssociation storeAssoc = asf.newActiveAssociation(a, null);
        storeAssoc.start();
        if (a.countAcceptedPresContext() == 0) {
            try {
                storeAssoc.release(false);
            } catch (Exception e) {
                log.info("Exception during release of assocation to "
                        + moveDest, e);
            }
            throw new IOException("No Presentation Context for Storage accepted by "
                    + moveDest);
        }
        removeInstancesOfUnsupportedStorageSOPClasses(a);
        AuditLoggerFactory alf = AuditLoggerFactory.getInstance();
        remoteNode = alf.newRemoteNode(storeAssoc.getAssociation().getSocket(),
                storeAssoc.getAssociation().getCalledAET());
        return storeAssoc;
    }

    private void removeInstancesOfUnsupportedStorageSOPClasses(Association a) {
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
                        + " accepted by " + moveDest
                        + "\n\tCannot send " + iuids.size()
                        + " instances of this class";
                if (!service.isIgnorableSOPClass(cuid, moveDest)) {
                    failedIUIDs.addAll(iuids);
                    log.warn(prompt);
                } else {
                    completed += iuids.size();
                    log.info(prompt);
                }
                remainingIUIDs.removeAll(iuids);
                remaining = remainingIUIDs.size();
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
                directForwarding = !retrieveInfo.isExternalRetrieveAET()
                        || service.isDirectForwarding(retrieveAET, moveDest);
                String moveDest1 = directForwarding ? moveDest 
                        : service.getLocalStorageAET();
                String callingAET = (directForwarding
                        && service.isForwardAsMoveOriginator()) 
                            ? moveOriginatorAET
                            : moveCalledAET;
                moveScu = new MoveScu(service, moveCalledAET, callingAET,
                        retrieveAET, remaining);
                if (iuids.size() == total) {
                    if (log.isDebugEnabled())
                        log.debug("Forward original Move RQ to " + retrieveAET);
                    moveScu.forwardMoveRQ(movePcid, msgID, priority,
                            moveDest1, moveRqData, iuids);
                } else {
                    moveScu.splitAndForwardMoveRQ(movePcid, msgID, priority,
                            moveDest1, iuids);
                }
                if (directForwarding) {
                    completed += moveScu.completed();
                    warnings += moveScu.warnings();
                    failedIUIDs.addAll(moveScu.failedIUIDs());
                    remainingIUIDs.removeAll(iuids);
                    remaining = canceled ? moveScu.remaining() 
                            : remainingIUIDs.size();
                } else {
                    if (moveScu.completed() > 0 || moveScu.warnings() > 0) {
                        updateLocalUIDs = true;
                    }
                }
                moveScu = null;
            }
            if (!canceled) {
                try {
                    if (updateLocalUIDs) {
                        FileInfo[][] fileInfo = RetrieveCmd.create(moveRqData).getFileInfos();
                        retrieveInfo = new RetrieveInfo(service, fileInfo);
                        localUIDs = retrieveInfo.removeLocalIUIDs();
                    }
                    if (!localUIDs.isEmpty()) {
                        service.prefetchTars(retrieveInfo.getLocalFiles());
                        ActiveAssociation storeAssoc = openAssociation();
                        retrieveLocal(storeAssoc);
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
            remaining = 0;
        }
        notifyMoveSCU(makeMoveRsp(status()),
                service.makeRetrieveRspIdentifier(failedIUIDs));
    }

    private void retrieveLocal(ActiveAssociation storeAssoc) {
        this.stgCmtActionInfo = DcmObjectFactory.getInstance().newDataset();
        this.refSOPSeq = stgCmtActionInfo.putSQ(Tags.RefSOPSeq);
        Set<StudyInstanceUIDAndDirPath> studyInfos = 
                new HashSet<StudyInstanceUIDAndDirPath>();
        Association a = storeAssoc.getAssociation();
        Collection<List<FileInfo>> localFiles = retrieveInfo.getLocalFiles();
        for (List<FileInfo> list : localFiles) {
            final FileInfo fileInfo = list.get(0);
            final String iuid = fileInfo.sopIUID;
            DimseListener storeScpListener = new DimseListener() {

                public void dimseReceived(Association assoc, Dimse dimse) {
                    switch (dimse.getCommand().getStatus()) {
                    case Status.Success:
                        ++completed;
                        updateInstancesAction(fileInfo);
                        updateStgCmtActionInfo(fileInfo);
                        successfulTransferred.add(fileInfo);
                        break;
                    case Status.CoercionOfDataElements:
                    case Status.DataSetDoesNotMatchSOPClassWarning:
                    case Status.ElementsDiscarded:
                        ++warnings;
                        updateInstancesAction(fileInfo);
                        updateStgCmtActionInfo(fileInfo);
                        successfulTransferred.add(fileInfo);
                        break;
                    default:
                        failedIUIDs.add(iuid);
                        break;
                    }
                    remainingIUIDs.remove(iuid);
                    --remaining;
                }
            };
            
            try {
                Dimse rq = service.makeCStoreRQ(storeAssoc,
                        fileInfo, priority, moveOriginatorAET, msgID, perfMon);
                perfMon.start(storeAssoc, rq, PerfCounterEnum.C_STORE_SCU_OBJ_OUT );
                perfMon.setProperty(storeAssoc, rq, PerfPropertyEnum.REQ_DIMSE, rq);
                perfMon.setProperty(storeAssoc, rq, PerfPropertyEnum.STUDY_IUID, fileInfo.studyIUID);

                storeAssoc.invoke(rq, storeScpListener);
                
                perfMon.stop(storeAssoc, rq, PerfCounterEnum.C_STORE_SCU_OBJ_OUT);
            } catch (Exception e) {
                log.error("Exception during move of " + iuid, e);
            }
            // track access on ONLINE FS
            if (fileInfo.availability == Availability.ONLINE) {
                studyInfos.add(new StudyInstanceUIDAndDirPath(fileInfo));
            }
            if (canceled || a.getState() != Association.ASSOCIATION_ESTABLISHED) {
                break;
            }
        }
        if (a.getState() == Association.ASSOCIATION_ESTABLISHED) {
            try {
            	perfMon.assocRelStart(a, Command.C_STORE_RQ);
            	
                storeAssoc.release(true);
                
                perfMon.assocRelEnd(a, Command.C_STORE_RQ);
                
                // workaround to ensure that last STORE-RSP is processed before
                // finally MOVE-RSP is sent
                Thread.sleep(10);
            } catch (Exception e) {
                log.error("Exception during release:", e);
            }
        } else {
            try {
                a.abort(AssociationFactory.getInstance().newAAbort(
                        AAbort.SERVICE_PROVIDER, AAbort.REASON_NOT_SPECIFIED));
            } catch (IOException ignore) {
            }
        }
        if (instancesAction != null) {
            service.logInstancesSent(remoteNode, instancesAction);
        }
        if (!successfulTransferred.isEmpty()) {
            service.logInstancesSent(moveAssoc.getAssociation(),
                    storeAssoc.getAssociation(), successfulTransferred);
        }
        service.updateStudyAccessTime(studyInfos);
        String stgCmtAET = service.getStgCmtAET(moveDest);
        if (stgCmtAET != null && refSOPSeq.countItems() > 0) {
            service.queueStgCmtOrder(moveCalledAET, stgCmtAET,
                            stgCmtActionInfo);
        }
    }
    
    private void updateInstancesAction(final FileInfo info) {
        if (instancesAction == null) {
            AuditLoggerFactory alf = AuditLoggerFactory.getInstance();
            instancesAction = alf.newInstancesAction("Access", info.studyIUID,
                    alf.newPatient(info.patID, info.patName));
            instancesAction.setUser(alf.newRemoteUser(alf.newRemoteNode(
                    moveAssoc.getAssociation().getSocket(), moveAssoc
                            .getAssociation().getCallingAET())));
        } else {
            instancesAction.addStudyInstanceUID(info.studyIUID);
        }
        instancesAction.addSOPClassUID(info.sopCUID);
        instancesAction.incNumberOfInstances(1);
    }

    private void updateStgCmtActionInfo(FileInfo fileInfo) {
        Dataset item = refSOPSeq.addNewItem();
        item.putUI(Tags.RefSOPClassUID, fileInfo.sopCUID);
        item.putUI(Tags.RefSOPInstanceUID, fileInfo.sopIUID);
    }

    private int status() {
        return canceled ? Status.Cancel 
                : failedIUIDs.isEmpty() ? Status.Success
                : completed == 0 && warnings == 0 
                        ? Status.UnableToPerformSuboperations
                        : Status.SubOpsOneOrMoreFailures;
    }

    private void notifyMoveSCU(Command moveRspCmd, Dataset moveRspData) {
        if (!moveAssocClosed) {
            try {
                moveAssoc.getAssociation().write(
                        AssociationFactory.getInstance().newDimse(movePcid,
                                moveRspCmd, moveRspData));
            } catch (Exception e) {
                log.info("Failed to send Move RSP to Move Originator:", e);
                moveAssocClosed  = true;
            }
        }
    }

    private Command makeMoveRsp(int status) {
        Command rspCmd = DcmObjectFactory.getInstance().newCommand();
        rspCmd.initCMoveRSP(msgID, sopClassUID, status);
        if (remaining > 0) {
            rspCmd.putUS(Tags.NumberOfRemainingSubOperations,
                    remaining);
        } else {
            rspCmd.remove(Tags.NumberOfRemainingSubOperations);
        }
        rspCmd.putUS(Tags.NumberOfCompletedSubOperations, completed);
        rspCmd.putUS(Tags.NumberOfWarningSubOperations, warnings);
        rspCmd.putUS(Tags.NumberOfFailedSubOperations, failedIUIDs.size());
        return rspCmd;
    }

}