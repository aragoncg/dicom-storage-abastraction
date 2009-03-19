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
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.List;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.jdbc.HPRetrieveCmd;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3420 $ $Date: 2007-06-28 11:41:24 +0200 (Thu, 28 Jun 2007) $
 * @since Aug 17, 2005
 */
public class HPMoveScp extends DcmServiceBase {

	private final HPScpService service;

	private final Logger log;

	public HPMoveScp(HPScpService service) {
		this.service = service;
		this.log = service.getLog();
	}

	public void c_move(ActiveAssociation assoc, Dimse rq) throws IOException {
		Command rqCmd = rq.getCommand();
		try {
			Dataset rqData = rq.getDataset();
			log.debug("Identifier:\n");
			log.debug(rqData);
			checkMoveRQ(assoc.getAssociation(), rq.pcid(), rqCmd, rqData);
			String dest = rqCmd.getString(Tags.MoveDestination);
			InetAddress host = dest.equals( assoc.getAssociation().getCallingAET()) ? assoc.getAssociation().getSocket().getInetAddress() : null;
			AEDTO destAE = service.queryAEData(dest, host);
			List hpList = queryHPList(rqData);
			new Thread(new HPMoveTask(service, assoc, rq.pcid(), rqCmd, rqData,
					hpList, destAE, dest)).start();
		} catch (DcmServiceException e) {
			Command rspCmd = objFact.newCommand();
			rspCmd.initCMoveRSP(rqCmd.getMessageID(), rqCmd
					.getAffectedSOPClassUID(), e.getStatus());
			e.writeTo(rspCmd);
			Dimse rsp = fact.newDimse(rq.pcid(), rspCmd);
			assoc.getAssociation().write(rsp);
		}
	}

	private List queryHPList(Dataset rqData)
			throws DcmServiceException {
		try {
			return new HPRetrieveCmd(rqData).getDatasets();
		} catch (SQLException e) {
			service.getLog().error("Query DB failed:", e);
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}
	}

	private void checkMoveRQ(Association assoc, int pcid, Command rqCmd,
			Dataset rqData) throws DcmServiceException {
		
		if (!rqCmd.containsValue(Tags.MoveDestination)) {
			throw new DcmServiceException(Status.UnableToProcess, 
					"Missing Move Destination");
		}

		if (!rqData.containsValue(Tags.SOPInstanceUID)) {
			throw new DcmServiceException(Status.IdentifierDoesNotMatchSOPClass,
					"Missing SOP Instance UID");
		}
	}

}
