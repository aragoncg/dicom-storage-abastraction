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

package org.dcm4chex.archive.dcm.stgcmt;

import java.io.IOException;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 5813 $ $Date: 2008-02-06 22:38:34 +0100 (Wed, 06 Feb 2008) $
 * @since Jan 5, 2005
 */
public class StgCmtScuScp extends DcmServiceBase {

    private final StgCmtScuScpService service;

    private final Logger log;

    public StgCmtScuScp(StgCmtScuScpService service) {
        this.service = service;
        this.log = service.getLog();
    }

    protected Dataset doNAction(ActiveAssociation assoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        Command cmd = rq.getCommand();
        Dataset data = rq.getDataset();
		log.debug("StgCmt Request:\n");
		log.debug(data);
        if (!UIDs.StorageCommitmentPushModelSOPInstance.equals(cmd
                .getRequestedSOPInstanceUID())) {
            throw new DcmServiceException(Status.NoSuchObjectInstance);
        }
        final int actionTypeID = cmd.getInt(Tags.ActionTypeID, -1);
        if (actionTypeID != 1) {
            throw new DcmServiceException(Status.NoSuchActionType,
                    "ActionTypeID:" + actionTypeID);
        }
        if (!data.containsValue(Tags.TransactionUID)) {
            throw new DcmServiceException(Status.MissingAttributeValue,
                    "Missing Transaction UID (0008,1195) in Action Information");
        }
        if (!data.containsValue(Tags.RefSOPSeq)) {
            throw new DcmServiceException(Status.MissingAttributeValue,
                    "Missing Referenced SOP Sequence (0008,1199) in Action Information");
        }
        final Association a = assoc.getAssociation();
        final String aet = a.getCallingAET();
        try {
            AEDTO aeData = service.queryAEData(aet, a.getSocket().getInetAddress());
            service.queueStgCmtOrder(a.getCalledAET(), aet, data, true);
        } catch (UnknownAETException e) {
            throw new DcmServiceException(Status.MoveDestinationUnknown, aet);
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
	}
        return null;
    }

    protected Dataset doNEventReport(ActiveAssociation assoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        Command cmd = rq.getCommand();
        Dataset data = rq.getDataset();
		log.debug("StgCmt Result:\n");
		log.debug(data);
        if (!UIDs.StorageCommitmentPushModelSOPInstance.equals(cmd
                .getRequestedSOPInstanceUID())) {
            throw new DcmServiceException(Status.NoSuchObjectInstance);
        }
        final int eventTypeID = cmd.getInt(Tags.EventTypeID, -1);
        final DcmElement refSOPSeq = data.get(Tags.RefSOPSeq);
        final DcmElement failedSOPSeq = data.get(Tags.FailedSOPSeq);
        if (eventTypeID == 1) {
            if (refSOPSeq == null) {
                throw new DcmServiceException(Status.MissingAttributeValue,
                        "Missing Referenced SOP Sequence (0008,1199) in Event Information");
            }
            if (failedSOPSeq != null) {
                throw new DcmServiceException(Status.InvalidArgumentValue,
                        "Unexpected Failed SOP Sequence (0008,1198) in Event Information");
            }
        } else if (eventTypeID == 2) {
            if (failedSOPSeq == null) {
                throw new DcmServiceException(Status.MissingAttributeValue,
                        "Missing Failed SOP Sequence (0008,1198) in Event Information");
            }
        } else {
            throw new DcmServiceException(Status.NoSuchEventType,
                    "EventTypeID:" + eventTypeID);
        }
        if (!data.containsValue(Tags.TransactionUID)) {
            throw new DcmServiceException(Status.MissingAttributeValue,
                    "Missing Transaction UID (0008,1195) in Event Information");
        }
        checkRefSopSeq(refSOPSeq, false);
        checkRefSopSeq(failedSOPSeq, true);
        service.commited(data);
        return null;
    }

    private void checkRefSopSeq(DcmElement sq, boolean failed)
            throws DcmServiceException {
        if (sq == null)
            return;
        for (int i = 0, n = sq.countItems(); i < n; ++i) {
            final Dataset refSOP = sq.getItem(i);
            final String iuid = refSOP.getString(Tags.RefSOPInstanceUID);
            final String cuid = refSOP.getString(Tags.RefSOPClassUID);
            if (iuid == null) {
                throw new DcmServiceException(Status.MissingAttributeValue,
                        "Missing Ref. SOP Instance UID >(0008,1155) in Item of "
                                + (failed ? "Failed SOP Sequence (0008,1198)"
                                        : "Ref. SOP Sequence (0008,1199)"));
            }
            if (cuid == null) {
                throw new DcmServiceException(Status.MissingAttributeValue,
                        "Missing Ref. SOP Class UID >(0008,1150) in Item of "
                                + (failed ? "Failed SOP Sequence (0008,1198)"
                                        : "Ref. SOP Sequence (0008,1199)"));
            }
            if (failed) {
                Integer reason = refSOP.getInteger(Tags.FailureReason);
                if (reason == null) {
                    throw new DcmServiceException(Status.MissingAttributeValue,
                            "Missing Failed Reason >(0008,1197) in Item of Failed SOP Sequence (0008,1198)");
                }
                log.warn("Failed Storage Commitment for SOP Instance[iuid=" 
                        + iuid + ", cuid=" + cuid + "], reason: "
                        + Integer.toHexString(reason.intValue()) + "H");
            }
        }
    }
}
