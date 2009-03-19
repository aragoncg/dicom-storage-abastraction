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

package org.dcm4chex.archive.dcm.stymgt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.StudyMgt;
import org.dcm4chex.archive.ejb.interfaces.StudyMgtHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

class StudyMgtScp extends DcmServiceBase {

	final StudyMgtScpService service;
	private Logger log;
	private boolean ignoreDeleteFailed;

	public StudyMgtScp(StudyMgtScpService service) {
		this.service = service;
		this.log = service.getLog();
	}

	/**
	 * @return Returns the ignoreDeleteFailed.
	 */
	public boolean isIgnoreDeleteFailed() {
		return ignoreDeleteFailed;
	}
	/**
	 * @param ignoreDeleteFailed The ignoreDeleteFailed to set.
	 */
	public void setIgnoreDeleteFailed(boolean ignoreDeleteFailed) {
		this.ignoreDeleteFailed = ignoreDeleteFailed;
	}
	
    private StudyMgtHome getStudyMgtHome() throws HomeFactoryException {
        return (StudyMgtHome) EJBHomeFactory.getFactory().lookup(
				StudyMgtHome.class, StudyMgtHome.JNDI_NAME);
    }

	
	protected Dataset doNAction(ActiveAssociation assoc, Dimse rq, 
			Command rspCmd) throws IOException, DcmServiceException {
		Command cmd = rq.getCommand();
		int actionTypeID = cmd.getInt(Tags.ActionTypeID, -1);
		String iuid = cmd.getRequestedSOPInstanceUID();
		Dataset ds = rq.getDataset();
		if ( log.isDebugEnabled() ) {
			log.debug("Received N-Action cmd with ds:");log.debug(ds);
		}
		checkStudyIuid(iuid, ds);
		try {
			StudyMgt stymgt = getStudyMgtHome().create();
			try {
				switch (actionTypeID) {
				case 1:
					stymgt.deleteSeries(toSeriesIuids(ds));
					break;
				case 2:
					stymgt.deleteInstances(toSopIuids(ds));
					break;
				}
			} finally {
				try {
					stymgt.remove();
				} catch (Exception e) {
					log.warn("Failed to remove StudyMgt Session Bean", e);
				}
			}
		} catch (DcmServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}
		service.sendStudyMgtNotification(assoc, Command.N_ACTION_RQ, 
				actionTypeID, iuid, ds);
		return null;
	}

	private String[] toSeriesIuids(Dataset ds) throws DcmServiceException {
		return toIuids(ds, true);
	}
	
	private String[] toSopIuids(Dataset ds) throws DcmServiceException {
		return toIuids(ds, false);
	}
	
	private String[] toIuids(Dataset ds, boolean seriesIuids)
			throws DcmServiceException {
		List iuids = new ArrayList();
		DcmElement sersq = ds.get(Tags.RefSeriesSeq);
		if (sersq == null) {
			throw new DcmServiceException(Status.MissingAttribute,
					"Missing Referenced Series Seq.");
		}
		for (int i = 0, n = sersq.countItems(); i < n; ++i) {
			Dataset ser = sersq.getItem(i);
			String siuid = ser.getString(Tags.SeriesInstanceUID);
			if (siuid == null) {
				throw new DcmServiceException(Status.MissingAttribute,
						"Missing Series Instance UID");
			}
			if (seriesIuids) {
				iuids.add(siuid);				
			} else {
				DcmElement sops = ser.get(Tags.RefSOPSeq);
				if (sops == null) {
					throw new DcmServiceException(Status.MissingAttribute,
							"Missing Referenced SOP Seq.");
				}
				for (int j = 0, m = sops.countItems(); j < m; ++j) {
					Dataset sop = sops.getItem(i);
					String iuid = sop.getString(Tags.RefSOPInstanceUID);
					if (iuid == null) {
						throw new DcmServiceException(Status.MissingAttribute,
								"Missing Referenced SOP Instance UID");
					}
					iuids.add(iuid);
				}
			}
		}
		return (String[]) iuids.toArray(new String[iuids.size()]);
	}

	protected Dataset doNCreate(ActiveAssociation assoc, Dimse rq, 
			Command rspCmd) throws IOException, DcmServiceException {
		Command cmd = rq.getCommand();
		String iuid = cmd.getAffectedSOPInstanceUID();
		Dataset ds = rq.getDataset();
		String suid = ds.getString(Tags.StudyInstanceUID);
		if (suid == null) {
			throw new DcmServiceException(Status.MissingAttribute,
					"Missing Study Instance UID");
		}
		if (iuid == null) {
			rspCmd.putUI(Tags.AffectedSOPInstanceUID, iuid = suid);
		} else {
			checkStudyIuid(iuid, ds);
		}
		try {
			StudyMgt stymgt = getStudyMgtHome().create();
			try {
				stymgt.createStudy(ds);
			} finally {
				try {
					stymgt.remove();
				} catch (Exception e) {
					log.warn("Failed to remove StudyMgt Session Bean", e);
				}
			}
		} catch (DcmServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}
		service.sendStudyMgtNotification(assoc, Command.N_CREATE_RQ, 0, iuid, ds);
		return null;
	}

	protected Dataset doNDelete(ActiveAssociation assoc, Dimse rq, 
			Command rspCmd) throws IOException, DcmServiceException {
		Command cmd = rq.getCommand();
		String iuid = cmd.getRequestedSOPInstanceUID();
		Dataset ds = rq.getDataset(); // should be null
		try {
			StudyMgt stymgt = getStudyMgtHome().create();
			try {
				stymgt.deleteStudy(iuid);
			} catch ( DcmServiceException e ) {
				if ( ! (ignoreDeleteFailed && e.getStatus() == Status.NoSuchSOPClass ) ) {
					throw e;
				}
			} finally {
				try {
					stymgt.remove();
				} catch (Exception e) {
					log.warn("Failed to remove StudyMgt Session Bean", e);
				}
			}
		} catch (DcmServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}
		service.sendStudyMgtNotification(assoc, Command.N_DELETE_RQ, 0, iuid, null);
		return null;
	}

	protected Dataset doNSet(ActiveAssociation assoc, Dimse rq, 
			Command rspCmd) throws IOException, DcmServiceException {
		Command cmd = rq.getCommand();
		String iuid = cmd.getRequestedSOPInstanceUID();
		Dataset ds = rq.getDataset();
		checkStudyIuid(iuid, ds);
		try {
			StudyMgt stymgt = getStudyMgtHome().create();
			try {
				stymgt.updateStudy(iuid, ds);
			} finally {
				try {
					stymgt.remove();
				} catch (Exception e) {
					log.warn("Failed to remove StudyMgt Session Bean", e);
				}
			}
		} catch (DcmServiceException e) {
			throw e;
		} catch (Exception e) {
			throw new DcmServiceException(Status.ProcessingFailure, e);
		}
		service.sendStudyMgtNotification(assoc, Command.N_SET_RQ, 0, iuid, ds);
		return null;
	}

	private void checkStudyIuid(String iuid, Dataset ds) throws DcmServiceException {
		String suid = ds.getString(Tags.StudyInstanceUID);
		if (suid != null && !suid.equals(iuid)) {
			throw new DcmServiceException(Status.InvalidAttributeValue,
				"Study Instance UID must match SOP Instance UID");
		}
	}

}
