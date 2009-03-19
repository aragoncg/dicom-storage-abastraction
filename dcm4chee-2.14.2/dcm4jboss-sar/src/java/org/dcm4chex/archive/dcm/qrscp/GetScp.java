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
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionManager;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.jboss.logging.Logger;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since May 6, 2008
 */
public class GetScp extends DcmServiceBase {

    private static final int MISSING_USER_ID_OF_STORE_SCP_ERR_STATUS = 0xCE12;
    
    private static final int NO_READ_PERMISSION_ERR_STATUS = 0xCE20;
    
    private static final String MISSING_USER_ID_OF_STORE_SCP_ERR_MSG =
            "Missing or invalid user identification of retrieve destination";
    
    private static final String NO_READ_PERMISSION_ERR_MSG =
            "Retrieve destination has no permission to read Study";

    protected final QueryRetrieveScpService service;

    private final Logger log;

    public GetScp(QueryRetrieveScpService service) {
        this.service = service;
        this.log = service.getLog();
    }

    @Override
    public void c_get(ActiveAssociation assoc, Dimse rq) throws IOException {
        int pcid = rq.pcid();
        Command rqCmd = rq.getCommand();
        Association a = assoc.getAssociation();
        try {            
            Dataset rqData = rq.getDataset();
            if(log.isDebugEnabled()) {
                log.debug("Identifier:\n");
                log.debug(rqData);
            }
            QRLevel qrLevel = QRLevel.toQRLevel(rqData);
            qrLevel.checkSOPClass(rqCmd.getAffectedSOPClassUID(), 
                    UIDs.StudyRootQueryRetrieveInformationModelGET,
                    UIDs.PatientStudyOnlyQueryRetrieveInformationModelGET);
            qrLevel.checkRetrieveRQ(rqData);
            FileInfo[][] fileInfos = null;
            try {
                fileInfos = RetrieveCmd.create(rqData).getFileInfos();
                checkPermission(a, fileInfos);
                    new Thread(new GetTask(
                            service, assoc, pcid, rqCmd, rqData, fileInfos))
                        .start();
            } catch (DcmServiceException e) {
                throw e;
            } catch (SQLException e) {
                service.getLog().error("Query DB failed:", e);
                throw new DcmServiceException(Status.UnableToCalculateNumberOfMatches, e);
            } catch (Throwable e) {
                service.getLog().error("Unexpected exception:", e);
                throw new DcmServiceException(Status.UnableToProcess, e);
            }
        } catch (DcmServiceException e) {
            Command rspCmd = objFact.newCommand();
            rspCmd.initCGetRSP(
                rqCmd.getMessageID(),
                rqCmd.getAffectedSOPClassUID(),
                e.getStatus());
            e.writeTo(rspCmd);
            Dimse rsp = fact.newDimse(pcid, rspCmd);
            a.write(rsp);
        }
    }

    private void checkPermission(Association a, FileInfo[][] fileInfos)
            throws Exception {
        if (fileInfos.length == 0) {
            return;
        }
        String callingAET = a.getCallingAET();
        if (service.hasUnrestrictedReadPermissions(callingAET)) {
            return;
        }
        Subject subject = (Subject) a.getProperty("user");
        if (subject == null) {
            throw new DcmServiceException(
                    MISSING_USER_ID_OF_STORE_SCP_ERR_STATUS,
                    MISSING_USER_ID_OF_STORE_SCP_ERR_MSG);
        }
        StudyPermissionManager studyPermissionManager =
            service.getStudyPermissionManager(a);
        Set<String> suids = new HashSet<String>();
        for (int i = 0; i < fileInfos.length; i++) {
            if (suids.add(fileInfos[i][0].studyIUID)) {
                if (!studyPermissionManager.hasPermission(
                        fileInfos[i][0].studyIUID,
                        StudyPermissionDTO.READ_ACTION, subject)) {
                   throw new DcmServiceException(
                           NO_READ_PERMISSION_ERR_STATUS,
                           NO_READ_PERMISSION_ERR_MSG);
                }
            }
        }
    }
}

