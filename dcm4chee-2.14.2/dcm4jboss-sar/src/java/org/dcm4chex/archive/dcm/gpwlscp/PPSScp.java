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

package org.dcm4chex.archive.dcm.gpwlscp;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.ejb.CreateException;

import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.common.PPSStatus;
import org.dcm4chex.archive.ejb.interfaces.GPPPSManager;
import org.dcm4chex.archive.ejb.interfaces.GPPPSManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 4958 $ $Date: 2007-09-06 13:47:56 +0200 (Thu, 06 Sep 2007) $
 */
class PPSScp extends DcmServiceBase {

    private static final int[] TYPE1_NCREATE_ATTR = {
        Tags.PPSID, Tags.PPSStartDate, Tags.PPSStartTime, Tags.GPPPSStatus
    };
    private static final int[] TYPE1_REF_GPSPS_ATTR = {
        Tags.RefSOPClassUID, Tags.RefSOPInstanceUID, Tags.RefGPSPSTransactionUID
    };
    private static final int[] CODEITEM_SQ_ATTR = {
        Tags.PerformedStationNameCodeSeq, Tags.PerformedStationClassCodeSeq,
        Tags.PerformedStationGeographicLocationCodeSeq,
        Tags.PerformedProcessingApplicationsCodeSeq,
        Tags.PerformedWorkitemCodeSeq
    };
    private static final int[] TYPE1_CODEITEM_ATTR = {
        Tags.CodeValue, Tags.CodingSchemeDesignator, Tags.CodeMeaning
    };
    private static final int[] ONLY_NCREATE_ATTR = {
        Tags.RefRequestSeq, Tags.RefGPSPSSeq, Tags.PatientName, Tags.PatientID,
        Tags.PatientBirthDate, Tags.PatientSex, Tags.ActualHumanPerformersSeq,
        Tags.PPSID, Tags.PerformedStationNameCodeSeq,
        Tags.PerformedStationClassCodeSeq,
        Tags.PerformedStationGeographicLocationCodeSeq,
        Tags.PerformedProcessingApplicationsCodeSeq,
        Tags.PerformedWorkitemCodeSeq, Tags.PPSStartDate, Tags.PPSStartTime
    };
    private static final int[] TYPE1_FINAL_ATTR = {
        Tags.PPSEndDate, Tags.PPSEndTime
    };
    
    private final GPWLScpService service;
	private final Logger log;

    public PPSScp(GPWLScpService service) {
        this.service = service;
		this.log = service.getLog();
    }

    protected Dataset doNCreate(ActiveAssociation assoc, Dimse rq,
            Command rspCmd) throws IOException, DcmServiceException {
        Association as = assoc.getAssociation();
        String callingAET = as.getCallingAET();
        final Command cmd = rq.getCommand();
        final Dataset gppps = rq.getDataset();
        final String cuid = cmd.getAffectedSOPClassUID();
        String iuid = cmd.getAffectedSOPInstanceUID();
        if (iuid == null) {
            iuid = rspCmd.getAffectedSOPInstanceUID();
        }
		log.debug("GP-PPS Attributes:");
		log.debug(gppps);
        checkCreateAttributs(gppps);
        service.supplementIssuerOfPatientID(gppps, callingAET);
        service.generatePatientID(gppps, gppps.getItem(Tags.RefRequestSeq));
        gppps.putUI(Tags.SOPClassUID, cuid);
        gppps.putUI(Tags.SOPInstanceUID, iuid);
        createGPPPS(gppps);
        service.sendPPSNotification(gppps);
        return null;
    }

    protected Dataset doNSet(ActiveAssociation assoc, Dimse rq, Command rspCmd)
            throws IOException, DcmServiceException {
        final Command cmd = rq.getCommand();
        final Dataset gppps = rq.getDataset();
        final String iuid = cmd.getRequestedSOPInstanceUID();
		log.debug("GP-PPS Attributes:");
		log.debug(gppps);
        checkSetAttributs(gppps);
        gppps.putUI(Tags.SOPInstanceUID, iuid);
        updateGPPPS(gppps);
        service.sendPPSNotification(gppps);
        return null;
    }

    protected Dataset doNGet(ActiveAssociation assoc, Dimse rq, Command rspCmd)
            throws IOException, DcmServiceException {
        Command rqCmd = rq.getCommand();
        Dataset ds = rq.getDataset(); // should be null!
        String iuid = rqCmd.getAffectedSOPInstanceUID();
        Dataset gppps = getGPPPS(iuid);
        final int[] filter = rqCmd.getTags(Tags.AttributeIdentifierList);
        return filter != null ? gppps.subSet(filter) : gppps;
    }
    
    private void checkCreateAttributs(Dataset gppps) 
            throws DcmServiceException {
        for (int i = 0; i < TYPE1_NCREATE_ATTR.length; ++i) {
            if (!gppps.containsValue(TYPE1_NCREATE_ATTR[i]))
                    throw new DcmServiceException(Status.MissingAttributeValue,
                            "Missing Type 1 Attribute "
                                    + Tags.toString(TYPE1_NCREATE_ATTR[i]));
        }
        DcmElement refReqSQ = gppps.get(Tags.RefRequestSeq);
        for (int i = 0, n = refReqSQ.countItems(); i < n; ++i) {
            if (!refReqSQ.getItem(i).containsValue(Tags.StudyInstanceUID))
                    throw new DcmServiceException(Status.MissingAttributeValue,
                            "Missing Study Instance UID in Referenced Request Seq.");
        }
        DcmElement refGPSPSSQ = gppps.get(Tags.RefGPSPSSeq);
        if (refGPSPSSQ != null) {
            for (int i = 0, n = refGPSPSSQ.countItems(); i < n; ++i) {
                Dataset refGPSPS = refGPSPSSQ.getItem(i);
                for (int j = 0; j < TYPE1_REF_GPSPS_ATTR.length; ++j) {
                    if (!refGPSPS.containsValue(TYPE1_REF_GPSPS_ATTR[j]))
                            throw new DcmServiceException(Status.MissingAttributeValue,
                                    "Missing Type 1 Attribute "
                                            + Tags.toString(TYPE1_REF_GPSPS_ATTR[j])
                                            + " in Referenced General Purpose Scheduled Procedure Step Seq.");
                }
            }
        }
        DcmElement ahpSQ = gppps.get(Tags.ActualHumanPerformersSeq);
        for (int i = 0, n = ahpSQ.countItems(); i < n; ++i) {            
            Dataset item = ahpSQ.getItem(i);
            checkCodeItem(item, Tags.HumanPerformerCodeSeq, true);
        }
        for (int i = 0; i < CODEITEM_SQ_ATTR.length; ++i) {
            checkCodeItem(gppps, CODEITEM_SQ_ATTR[i], false);
        }
        String status = gppps.getString(Tags.GPPPSStatus);
        try {
            if (PPSStatus.toInt(status) == PPSStatus.IN_PROGRESS) {
                return;
            }
        } catch (IllegalArgumentException e) {}
        throw new DcmServiceException(Status.InvalidAttributeValue,
                "Invalid General Purpose Performed Procedure Step Status: " + status);
    }

    private void checkCodeItem(Dataset ds, int tag, boolean type1)
    throws DcmServiceException {
        Dataset item = ds.getItem(tag);
        if (item == null) {
            if (type1) {
                throw new DcmServiceException(Status.MissingAttributeValue,
                        "Missing Type 1 Attribute " + Tags.toString(tag));
            }
            return;
        }
        for (int i = 0; i < TYPE1_CODEITEM_ATTR.length; ++i) {
            if (!item.containsValue(TYPE1_CODEITEM_ATTR[i])) {
                    throw new DcmServiceException(Status.MissingAttributeValue,
                            "Missing Type 1 Attribute "
                                    + Tags.toString(tag) + "/"
                                    + Tags.toString(TYPE1_CODEITEM_ATTR[i]));
            }
        }        
    }

    private void checkSetAttributs(Dataset gppps) throws DcmServiceException {
        for (int i = 0; i < ONLY_NCREATE_ATTR.length; ++i) {
            if (gppps.contains(ONLY_NCREATE_ATTR[i]))
                    throw new DcmServiceException(Status.ProcessingFailure,
                            "Cannot update attribute "
                                    + Tags.toString(ONLY_NCREATE_ATTR[i]));
        }
        final String status = gppps.getString(Tags.GPPPSStatus);
        try {
            if (status == null || PPSStatus.toInt(status) == PPSStatus.IN_PROGRESS) {
                return;
            }
        } catch (IllegalArgumentException e) {
                throw new DcmServiceException(Status.InvalidAttributeValue,
                        "Invalid GPPPS Status: " + status);
        }
        for (int i = 0; i < TYPE1_FINAL_ATTR.length; ++i) {
            if (!gppps.containsValue(TYPE1_FINAL_ATTR[i]))
                    throw new DcmServiceException(Status.MissingAttributeValue,
                            "Missing Type 1 Attribute "
                                    + Tags.toString(TYPE1_FINAL_ATTR[i]));
        }
    }
    
    private GPPPSManager getGPPPSManager() 
            throws HomeFactoryException, RemoteException, CreateException {
        return ((GPPPSManagerHome) EJBHomeFactory.getFactory().lookup(
                GPPPSManagerHome.class, GPPPSManagerHome.JNDI_NAME)).create();
    }
    
    private void createGPPPS(Dataset gppps) throws DcmServiceException {
        try {
            getGPPPSManager().createGPPPS(gppps);
        } catch (DcmServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    private void updateGPPPS(Dataset gppps) throws DcmServiceException {
         try {
             getGPPPSManager().updateGPPPS(gppps);
         } catch (DcmServiceException e) {
             throw e;
         } catch (Exception e) {
             throw new DcmServiceException(Status.ProcessingFailure, e);
         }
    }

    private Dataset getGPPPS(String iuid) throws DcmServiceException {
        try {
            return getGPPPSManager().getGPPPS(iuid);
        } catch (DcmServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
   }

}
