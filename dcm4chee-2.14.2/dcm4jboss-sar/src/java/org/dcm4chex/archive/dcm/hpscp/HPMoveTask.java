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

package org.dcm4chex.archive.dcm.hpscp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
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
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.exceptions.NoPresContextException;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3262 $ $Date: 2007-04-10 13:34:35 +0200 (Tue, 10 Apr 2007) $
 * @since Aug 30, 2005
 */
class HPMoveTask implements Runnable {

    private static final int STORE_PCID = 1;

    private static final String[] NATIVE_LE_TS = { UIDs.ExplicitVRLittleEndian,
            UIDs.ImplicitVRLittleEndian, };

    private final HPScpService service;

    private final List hpList;

    private Logger log;

    private ActiveAssociation moveAssoc;

    private int movePcid;

    private Command moveRqCmd;

    private Dataset moveRqData;

    private AEDTO aeData;

    private String moveDest;

    private String moveOriginatorAET;

    private String moveCalledAET;

    private int priority;

    private int msgID;

    private int failed = 0;

    private int warnings = 0;

    private int completed = 0;

    private int remaining = 0;

    private boolean canceled = false;

    private ActiveAssociation storeAssoc;

    private final ArrayList failedIUIDs = new ArrayList();

    public HPMoveTask(HPScpService service, ActiveAssociation moveAssoc,
            int movePcid, Command moveRqCmd, Dataset moveRqData, List hpList,
            AEDTO aeData, String moveDest) throws DcmServiceException {
        this.service = service;
        this.hpList = hpList;
        this.log = service.getLog();
        this.moveAssoc = moveAssoc;
        this.movePcid = movePcid;
        this.moveRqCmd = moveRqCmd;
        this.moveRqData = moveRqData;
        this.aeData = aeData;
        this.moveDest = moveDest;
        this.moveOriginatorAET = moveAssoc.getAssociation().getCallingAET();
        this.moveCalledAET = moveAssoc.getAssociation().getCalledAET();
        this.priority = moveRqCmd.getInt(Tags.Priority, Command.MEDIUM);
        this.msgID = moveRqCmd.getMessageID();
        if (hpList.isEmpty())
            return;
        openAssociation();
        moveAssoc.addCancelListener(msgID, new DimseListener() {
            public void dimseReceived(Association assoc, Dimse dimse) {
                canceled = true;
            }
        });
    }

    private void openAssociation() throws DcmServiceException {
        PDU ac = null;
        Association a = null;
        AssociationFactory asf = AssociationFactory.getInstance();
        try {
            a = asf.newRequestor(service.createSocket(moveCalledAET, aeData));
            a.setAcTimeout(service.getAcTimeout());
            AAssociateRQ rq = asf.newAAssociateRQ();
            rq.setCalledAET(moveDest);
            rq.setCallingAET(moveCalledAET);
            rq.addPresContext(asf.newPresContext(STORE_PCID,
                    UIDs.HangingProtocolStorage, NATIVE_LE_TS));
            ac = a.connect(rq);
        } catch (Exception e) {
            final String prompt = "Failed to connect " + moveDest;
            log.error(prompt, e);
            throw new DcmServiceException(Status.UnableToPerformSuboperations,
                    prompt, e);
        }
        if (!(ac instanceof AAssociateAC)) {
            final String prompt = "Association not accepted by " + moveDest
                    + ":\n" + ac;
            log.error(prompt);
            throw new DcmServiceException(Status.UnableToPerformSuboperations,
                    prompt);
        }
        storeAssoc = asf.newActiveAssociation(a, null);
        storeAssoc.start();
        if (a.listAcceptedPresContext(UIDs.HangingProtocolStorage).isEmpty()) {
            final String prompt = "No Presentation Context for Hanging Protocol Storage not negotiated by "
                    + moveDest;
            log.error(prompt);
            throw new DcmServiceException(Status.UnableToPerformSuboperations,
                    prompt);

        }
    }

    public void run() {
        Association a = storeAssoc.getAssociation();
        for (int i = 0, n = hpList.size(); i < n
                && a.getState() == Association.ASSOCIATION_ESTABLISHED
                && !canceled; ++i) {
            Dataset hp = (Dataset) hpList.get(i);
            final String iuid = hp.getString(Tags.SOPInstanceUID);
            DimseListener storeScpListener = new DimseListener() {

                public void dimseReceived(Association assoc, Dimse dimse) {
                    switch (dimse.getCommand().getStatus()) {
                    case Status.Success:
                        ++completed;
                        break;
                    case Status.CoercionOfDataElements:
                    case Status.DataSetDoesNotMatchSOPClassWarning:
                    case Status.ElementsDiscarded:
                        ++warnings;
                        break;
                    default:
                        ++failed;
                        failedIUIDs.add(iuid);
                        break;
                    }
                    if (--remaining > 0) {
                        notifyMovePending();
                    }
                }
            };
            try {
                storeAssoc.invoke(makeCStoreRQ(hp), storeScpListener);
            } catch (Exception e) {
                log.error("Exception during move of " + iuid, e);
            }
        }
        if (a.getState() == Association.ASSOCIATION_ESTABLISHED) {
            try {
                storeAssoc.release(true);
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
        if (!canceled) {
            for (int i = completed + warnings + failed, n = hpList.size(); i < n; ++i) {
                Dataset hp = (Dataset) hpList.get(i);
                failedIUIDs.add(hp.getString(Tags.SOPInstanceUID));
            }
            remaining = 0;
            failed = failedIUIDs.size();
        }
        notifyMoveFinished();
    }

    private Dimse makeCStoreRQ(Dataset hp) throws NoPresContextException {
        Association assoc = storeAssoc.getAssociation();
        Command storeRqCmd = DcmObjectFactory.getInstance().newCommand();
        storeRqCmd.initCStoreRQ(assoc.nextMsgID(), hp
                .getString(Tags.SOPClassUID),
                hp.getString(Tags.SOPInstanceUID), priority);
        storeRqCmd.putUS(Tags.MoveOriginatorMessageID, msgID);
        storeRqCmd.putAE(Tags.MoveOriginatorAET, moveOriginatorAET);
        return AssociationFactory.getInstance().newDimse(STORE_PCID,
                storeRqCmd, hp);
    }

    private void notifyMovePending() {
        if (service.isSendPendingMoveRSP()) {
            notifyMoveSCU(Status.Pending, null);
        }
    }

    private void notifyMoveFinished() {
        notifyMoveSCU(canceled ? Status.Cancel
                : failed > 0 ? Status.SubOpsOneOrMoreFailures : Status.Success,
                makeMoveRspIdentifier());
    }

    private void notifyMoveSCU(int status, Dataset ds) {
        if (moveAssoc == null)
            return;
        try {
            moveAssoc.getAssociation().write(
                    AssociationFactory.getInstance().newDimse(movePcid,
                            makeMoveRsp(status), ds));
        } catch (Exception e) {
            log.info("Failed to send Move RSP to Move Originator:", e);
            moveAssoc = null;
        }
    }

    private Dataset makeMoveRspIdentifier() {
        if (failed == 0)
            return null;
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        if (failed == failedIUIDs.size()) {
            String[] a = (String[]) failedIUIDs.toArray(new String[failedIUIDs
                    .size()]);
            ds.putUI(Tags.FailedSOPInstanceUIDList, a);
            // check if 64k limit for UI attribute is reached
            if (ds.get(Tags.FailedSOPInstanceUIDList).length() < 0x10000)
                return ds;
            log.warn("Failed SOP InstanceUID List exceeds 64KB limit "
                    + "- send empty attribute instead");
        }
        ds.putUI(Tags.FailedSOPInstanceUIDList);
        return ds;
    }

    private Command makeMoveRsp(int status) {
        Command rspCmd = DcmObjectFactory.getInstance().newCommand();
        rspCmd.initCMoveRSP(moveRqCmd.getMessageID(), moveRqCmd
                .getAffectedSOPClassUID(), status);
        if (remaining > 0) {
            rspCmd.putUS(Tags.NumberOfRemainingSubOperations, remaining);
        } else {
            rspCmd.remove(Tags.NumberOfRemainingSubOperations);
        }
        rspCmd.putUS(Tags.NumberOfCompletedSubOperations, completed);
        rspCmd.putUS(Tags.NumberOfWarningSubOperations, warnings);
        rspCmd.putUS(Tags.NumberOfFailedSubOperations, failed);
        return rspCmd;
    }
}
