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
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;

import javax.management.ObjectName;
import javax.security.auth.Subject;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AAssociateAC;
import org.dcm4che.net.AAssociateRQ;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.AssociationListener;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4che.net.PDU;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManager;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.exceptions.UnknownAETException;
import org.dcm4chex.archive.perf.PerfCounterEnum;
import org.dcm4chex.archive.perf.PerfMonDelegate;
import org.dcm4chex.archive.perf.PerfPropertyEnum;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 6312 $ $Date: 2008-05-15 15:06:29 +0200 (Thu, 15 May 2008) $
 * @since 31.08.2003
 */
public class MoveScp extends DcmServiceBase implements AssociationListener {
    
    private static final int MISSING_USER_ID_OF_MOVE_SCU_ERR_STATUS = 0xCE10;
    
    private static final int MISSING_USER_ID_OF_STORE_SCP_ERR_STATUS = 0xCE12;
    
    private static final int NO_READ_PERMISSION_ERR_STATUS = 0xCE20;
    
    private static final int NO_EXPORT_PERMISSION_ERR_STATUS = 0xCE22;
    
    private static final String MISSING_USER_ID_OF_MOVE_SCU_ERR_MSG =
            "Missing user identification of Move originator";
    
    private static final String MISSING_USER_ID_OF_STORE_SCP_ERR_MSG =
            "Missing or invalid user identification of Move destination";
    
    private static final String NO_READ_PERMISSION_ERR_MSG =
            "Move destination has no permission to read Study";
   
    private static final String NO_EXPORT_PERMISSION_ERR_MSG =
            "Move originator has no permission to export Study";
    
    protected final QueryRetrieveScpService service;
	
    private final Logger log;
    
    private PerfMonDelegate perfMon;

    public MoveScp(QueryRetrieveScpService service) {
        this.service = service;
		this.log = service.getLog();
        perfMon = new PerfMonDelegate(this.service);
    }

    public void c_move(ActiveAssociation assoc, Dimse rq) throws IOException {
        Command rqCmd = rq.getCommand();
        Association a = assoc.getAssociation();
        try {            
            Dataset rqData = rq.getDataset();
            if(log.isDebugEnabled()) {
            	log.debug("Identifier:\n");
            	log.debug(rqData);
            }
            String dest = rqCmd.getString(Tags.MoveDestination);
            if (dest == null) {
                throw new DcmServiceException(Status.UnableToProcess,
                        "Missing Move Destination");
            }
            QRLevel qrLevel = QRLevel.toQRLevel(rqData);
            qrLevel.checkSOPClass(rqCmd.getAffectedSOPClassUID(), 
                    UIDs.StudyRootQueryRetrieveInformationModelMOVE,
                    UIDs.PatientStudyOnlyQueryRetrieveInformationModelMOVE);
            qrLevel.checkRetrieveRQ(rqData);
            boolean thirdPartyMove = !dest.equals(a.getCallingAET());
            AEDTO aeData = null;
            FileInfo[][] fileInfos = null;
            try {
               	perfMon.start(assoc, rq, PerfCounterEnum.C_MOVE_SCP_QUERY_DB);
            	perfMon.setProperty(assoc, rq, PerfPropertyEnum.REQ_DIMSE, rq);

                aeData = service.queryAEData(dest, 
                        thirdPartyMove ? null : a.getSocket().getInetAddress());
                fileInfos = RetrieveCmd.create(rqData).getFileInfos();

                perfMon.setProperty(assoc, rq, PerfPropertyEnum.NUM_OF_RESULTS, String.valueOf(fileInfos.length));
                perfMon.stop(assoc, rq, PerfCounterEnum.C_MOVE_SCP_QUERY_DB);
                checkPermission(a, thirdPartyMove, aeData, fileInfos);
                
	            new Thread( createMoveTask(service,
	                    assoc,
	                    rq.pcid(),
	                    rqCmd,
	                    rqData,
	                    fileInfos,
	                    aeData,
	                    dest))
	                .start();
            } catch (DcmServiceException e) {
                throw e;
            } catch (UnknownAETException e) {
                throw new DcmServiceException(Status.MoveDestinationUnknown, dest);
            } catch (SQLException e) {
                service.getLog().error("Query DB failed:", e);
                throw new DcmServiceException(Status.UnableToCalculateNumberOfMatches, e);
            } catch (Throwable e) {
                service.getLog().error("Unexpected exception:", e);
                throw new DcmServiceException(Status.UnableToProcess, e);
            }
        } catch (DcmServiceException e) {
            Command rspCmd = objFact.newCommand();
            rspCmd.initCMoveRSP(
                rqCmd.getMessageID(),
                rqCmd.getAffectedSOPClassUID(),
                e.getStatus());
            e.writeTo(rspCmd);
            Dimse rsp = fact.newDimse(rq.pcid(), rspCmd);
            a.write(rsp);
        }
    }

    private void checkPermission(Association a, boolean thirdPartyMove, 
            AEDTO ae, FileInfo[][] fileInfos) throws Exception {
        boolean checkExportPermissions = thirdPartyMove
                && !service.hasUnrestrictedExportPermissions(a.getCallingAET());
        boolean checkReadPermissions =
                !service.hasUnrestrictedReadPermissions(ae.getTitle());
        if (!checkExportPermissions && !checkReadPermissions) {
            return;
        }
        HashSet suids = new HashSet();
        for (int i = 0; i < fileInfos.length; i++) {
            suids.add(fileInfos[i][0].studyIUID);
        }
        Subject subject = (Subject) a.getProperty("user");
        StudyPermissionManager studyPermissionManager =
                service.getStudyPermissionManager(a);
        if (checkExportPermissions) {
            if (subject == null) {
                throw new DcmServiceException(
                        MISSING_USER_ID_OF_MOVE_SCU_ERR_STATUS,
                        MISSING_USER_ID_OF_MOVE_SCU_ERR_MSG);
            }
            for (Iterator iter = suids.iterator(); iter.hasNext();) {
                 if (!studyPermissionManager.hasPermission((String) iter.next(),
                         StudyPermissionDTO.EXPORT_ACTION, subject)) {
                    throw new DcmServiceException(
                            NO_EXPORT_PERMISSION_ERR_STATUS,
                            NO_EXPORT_PERMISSION_ERR_MSG);
                 }
            }
        }
        if (checkReadPermissions) {
            if (subject == null || thirdPartyMove) {
                subject = new Subject();
                String userId = ae.getUserID();
                String passwd = ae.getPassword();
                if (userId == null || userId.length() == 0
                        || !service.dicomSecurity().isValid(userId, passwd, subject)) {
                    throw new DcmServiceException(
                            MISSING_USER_ID_OF_STORE_SCP_ERR_STATUS,
                            MISSING_USER_ID_OF_STORE_SCP_ERR_MSG);
                }
            }
            for (Iterator iter = suids.iterator(); iter.hasNext();) {
                if (!studyPermissionManager.hasPermission((String) iter.next(),
                        StudyPermissionDTO.READ_ACTION, subject)) {
                   throw new DcmServiceException(
                           NO_READ_PERMISSION_ERR_STATUS,
                           NO_READ_PERMISSION_ERR_MSG);
                }
           }
       }
    }
    
    protected MoveTask createMoveTask( QueryRetrieveScpService service,
	    ActiveAssociation moveAssoc, int movePcid, Command moveRqCmd,
	    Dataset moveRqData, FileInfo[][] fileInfo, AEDTO aeData,
	    String moveDest) throws DcmServiceException {
    	return new MoveTask(
                service,
                moveAssoc,
                movePcid,
                moveRqCmd,
                moveRqData,
                fileInfo,
                aeData,
                moveDest);
    }

    public final ObjectName getPerfMonServiceName() {
		return perfMon.getPerfMonServiceName();
	}

	public final void setPerfMonServiceName(ObjectName perfMonServiceName) {
		perfMon.setPerfMonServiceName(perfMonServiceName);
	}
    
    public PerfMonDelegate getPerfMonDelegate() {
    	return perfMon;
    }

    public void write(Association src, PDU pdu) {
    	if (pdu instanceof AAssociateAC)
    		perfMon.assocEstEnd(src, Command.C_MOVE_RQ);
	}

	public void received(Association src, PDU pdu) {
    	if(pdu instanceof AAssociateRQ)
    		perfMon.assocEstStart(src, Command.C_MOVE_RQ);
	}

	public void write(Association src, Dimse dimse) {
	}

	public void received(Association src, Dimse dimse) {
	}

	public void error(Association src, IOException ioe) {
	}

	public void closing(Association assoc) {
    	if(assoc.getAAssociateAC() != null)
    		perfMon.assocRelStart(assoc, Command.C_MOVE_RQ);
	}

	public void closed(Association assoc) {
    	if(assoc.getAAssociateAC() != null)
    		perfMon.assocRelEnd(assoc, Command.C_MOVE_RQ);
	}
}
