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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationFactory;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.DimseListener;
import org.dcm4che.net.ExtNegotiation;
import org.dcm4che.net.PDU;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.jboss.logging.Logger;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 13, 2008
 */
class MoveScu {

    private static final String IMAGE = "IMAGE";
    private static final String PATIENT = "PATIENT";
    private static final byte[] RELATIONAL_RETRIEVE = { 1 };

    private final QueryRetrieveScpService service;
    private final Logger log;
    private final String localAET;
    private final String callingAET;
    private final String calledAET;
    private ActiveAssociation assoc;
    private int remaining;
    private int completed = 0;
    private int failed = 0;
    private int warnings = 0;
    private final Collection<String> failedIUIDs = new ArrayList<String>();
    private boolean canceled;
    private Command moveRspCmd;
    private Dataset moveRspData;

    public MoveScu(QueryRetrieveScpService service, String localAET,
            String callingAET,  String calledAET, int remaining) {
        this.service = service;
        this.log = service.getLog();
        this.localAET = localAET;
        this.callingAET = callingAET;
        this.calledAET = calledAET;
        this.remaining = remaining;
    }

    public final int completed() {
        return completed;
    }

    public final int warnings() {
        return warnings;
    }

    public final int failed() {
        return failed;
    }

    public final int remaining() {
        return remaining;
    }

    public final Collection<String> failedIUIDs() {
        return failedIUIDs;
    }

    public final Command getMoveRspCmd() {
        return moveRspCmd;
    }

    public void forwardCancelRQ(Dimse ccancelrq) {
        canceled = true;
        if (assoc != null) {
            try {
                assoc.getAssociation().write(ccancelrq);
            } catch (Exception e) {
                log.warn("Failed to forward C-CANCEL-RQ:", e);
            }
        }
    }

    public void splitAndForwardMoveRQ(int pcid, int msgid, int priority,
            String moveDest, Collection<String> iuids) {
        DcmObjectFactory dof = DcmObjectFactory.getInstance();
        Dataset ds = dof.newDataset();
        ds.putCS(Tags.QueryRetrieveLevel, IMAGE);
        String[] a = (String[]) iuids.toArray(new String[iuids.size()]);
        if (a.length <= service.getMaxUIDsPerMoveRQ()) {
            ds.putUI(Tags.SOPInstanceUID, a);
            forwardMoveRQ(pcid, msgid, priority, moveDest, ds, iuids);
        } else {
            String[] b = new String[service.getMaxUIDsPerMoveRQ()];
            int off = 0;
            while (off + b.length < a.length) {
                System.arraycopy(a, off, b, 0, b.length);
                ds.putUI(Tags.SOPInstanceUID, b);
                forwardMoveRQ(pcid, msgid, priority, moveDest, ds,
                        Arrays.asList(b));
                if (canceled) {
                    return;
                }
                off += b.length;
            }
            b = new String[a.length - off];
            System.arraycopy(a, off, b, 0, b.length);
            ds.putUI(Tags.SOPInstanceUID, b);
            forwardMoveRQ(pcid, msgid, priority, moveDest, ds,
                    Arrays.asList(b));
        }
    }

    public void forwardMoveRQ(int pcid, int msgid, int priority,
            String moveDest, Dataset moveRqData,
            Collection<String> iuids) {
        remaining -= iuids.size();
        String sopClassUID = PATIENT.equals(
                moveRqData.getString(Tags.QueryRetrieveLevel)) 
                    ? UIDs.PatientRootQueryRetrieveInformationModelMOVE
                    : UIDs.StudyRootQueryRetrieveInformationModelMOVE;
        try {
            assoc = openAssociation(pcid, sopClassUID );
        } catch (Exception e) {
            log.info("Failed to open assocation to " + calledAET, e);
            failedIUIDs.addAll(iuids);
            return;
        }
        try {
            moveRspCmd = null;
            moveRspData = null;
            DimseListener moveRspListener = new DimseListener() {
                public void dimseReceived(Association assoc, Dimse dimse) {
                    moveRspCmd = dimse.getCommand();
                    try {
                        moveRspData = dimse.getDataset();
                    } catch (IOException e) {
                        log.error("Failure during receive of C-MOVE_RSP from " 
                                + calledAET, e);
                    }
                }
            };
            AssociationFactory asf = AssociationFactory.getInstance();
            Command moveRqCmd = DcmObjectFactory.getInstance().newCommand();
            moveRqCmd.initCMoveRQ(msgid, sopClassUID, priority, moveDest);
            assoc.invoke(asf.newDimse(pcid, moveRqCmd , moveRqData), moveRspListener);
        } catch (Exception e) {
            log.error("Failed to forward MOVE RQ to " + calledAET, e);
        } finally {
            try {
                assoc.release(true);
                // workaround to ensure that the final MOVE-RSP of forwarded
                // MOVE-RQ is processed before continuing
                Thread.sleep(10);
            } catch (Exception e) {
                log.info("Exception during release of assocation to "
                        + calledAET, e);
            }
         }
        if (moveRspCmd == null || moveRspCmd.getStatus() == Status.Pending) {
            log.error("No final MOVE RSP received from " + calledAET);
            failedIUIDs.addAll(iuids);
            failed += iuids.size();
        } else {
            completed += moveRspCmd.getInt(Tags.NumberOfCompletedSubOperations, 0);
            warnings += moveRspCmd.getInt(Tags.NumberOfWarningSubOperations, 0);
            failed += moveRspCmd.getInt(Tags.NumberOfFailedSubOperations, 0);
            remaining += moveRspCmd.getInt(Tags.NumberOfRemainingSubOperations, 0);
            if (moveRspData != null) {
                String[] a = moveRspData
                        .getStrings(Tags.FailedSOPInstanceUIDList);
                if (a != null && a.length != 0) {
                    failedIUIDs.addAll(Arrays.asList(a));
                } else {
                    failedIUIDs.addAll(iuids);
                }
            }
        }
        moveRspCmd = null;
    }

    private ActiveAssociation openAssociation(int pcid, String sopClassUID)
            throws Exception {
        AEDTO retrieveAEData = service.queryAEData(calledAET, null);
        AssociationFactory asf = AssociationFactory.getInstance();
        Association a = asf.newRequestor(
                service.createSocket(localAET, retrieveAEData));
        a.setAcTimeout(service.getAcTimeout());
        a.setDimseTimeout(service.getDimseTimeout());
        a.setSoCloseDelay(service.getSoCloseDelay());
        AAssociateRQ rq = asf.newAAssociateRQ();
        rq.setCalledAET(calledAET);
        rq.setCallingAET(callingAET);
        rq.addPresContext(asf.newPresContext(pcid, sopClassUID));
        rq.addExtNegotiation(asf.newExtNegotiation(sopClassUID, RELATIONAL_RETRIEVE));
        PDU pdu = a.connect(rq);
        if (!(pdu instanceof AAssociateAC)) {
            throw new IOException("Association not accepted by "
                    + calledAET + ":\n" + pdu);
        }
        AAssociateAC ac = (AAssociateAC) pdu;
        ActiveAssociation assoc = asf.newActiveAssociation(a, null);
        assoc.start();
        if (a.getAcceptedTransferSyntaxUID(pcid) == null) {
            try {
                assoc.release(false);
            } catch (Exception e) {
                log.info("Exception during release of assocation to "
                        + calledAET, e);
            }
            throw new IOException(
                    QueryRetrieveScpService.uidDict.toString(sopClassUID)
                    + " not accepted by " + calledAET);
        }
        ExtNegotiation extNeg = ac.getExtNegotiation(sopClassUID);
        if (extNeg == null
                || !Arrays.equals(extNeg.info(), RELATIONAL_RETRIEVE)) {
            log.warn("Relational Retrieve not supported by " + calledAET);
        }
        return assoc;
    }

    public void adjustPendingRsp(Command rspCmd) {
        Command tmp = moveRspCmd;
        rspCmd.putUS(Tags.NumberOfRemainingSubOperations, remaining
                + (tmp == null ? 0 
                        : tmp.getInt(Tags.NumberOfRemainingSubOperations, 0)));
        rspCmd.putUS(Tags.NumberOfCompletedSubOperations, 
                rspCmd.getInt(Tags.NumberOfCompletedSubOperations, 0)
                + completed + (tmp == null ? 0 
                        : tmp.getInt(Tags.NumberOfCompletedSubOperations, 0)));
        rspCmd.putUS(Tags.NumberOfWarningSubOperations, 
                rspCmd.getInt(Tags.NumberOfWarningSubOperations, 0)
                + warnings + (tmp == null ? 0 
                        : tmp.getInt(Tags.NumberOfWarningSubOperations, 0)));
        rspCmd.putUS(Tags.NumberOfFailedSubOperations, 
                rspCmd.getInt(Tags.NumberOfFailedSubOperations, 0)
                + failed + (tmp == null ? 0 
                        : tmp.getInt(Tags.NumberOfFailedSubOperations, 0)));
     }

}
