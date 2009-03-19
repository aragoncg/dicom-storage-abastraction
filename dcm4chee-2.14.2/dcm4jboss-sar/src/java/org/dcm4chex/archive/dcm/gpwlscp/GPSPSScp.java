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
import org.dcm4che.net.DcmServiceBase;
import org.dcm4che.net.DcmServiceException;
import org.dcm4che.net.Dimse;
import org.dcm4chex.archive.ejb.interfaces.GPWLManager;
import org.dcm4chex.archive.ejb.interfaces.GPWLManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.logging.Logger;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3420 $ $Date: 2007-06-28 11:41:24 +0200 (Thu, 28 Jun 2007) $
 * @since 04.04.2005
 *
 */

class GPSPSScp extends DcmServiceBase {
	
	private static final int REQUEST_GPSPS_STATUS_MODIFICATION = 1;
	private final GPWLScpService service;
	private final Logger log;

    public GPSPSScp(GPWLScpService service) {
        this.service = service;
        this.log = service.getLog();
    }

	protected Dataset doNAction(ActiveAssociation assoc, Dimse rq,
			Command rspCmd) throws IOException, DcmServiceException {
        Command rqCmd = rq.getCommand();
        Dataset actionInfo = rq.getDataset();
        log.debug("N-Action Information:");
		log.debug(actionInfo);

        final String iuid = rqCmd.getAffectedSOPInstanceUID();
        final int actionID = rqCmd.getInt(Tags.ActionTypeID, -1);
        if (actionID != REQUEST_GPSPS_STATUS_MODIFICATION) 
        	throw new DcmServiceException(Status.NoSuchActionType, "actionID:"
                    + actionID);
        if (!actionInfo.containsValue(Tags.TransactionUID))
            throw new DcmServiceException(Status.MissingAttributeValue,
                    "Missing Transaction UID (0008,1195)");
        if (!actionInfo.containsValue(Tags.GPSPSStatus))
            throw new DcmServiceException(Status.MissingAttributeValue,
                    "Missing GPSPS Status (0040,4001)");
        DcmElement src = actionInfo.get(Tags.ActualHumanPerformersSeq);
        if (src != null) {
	    	Dataset item, code;
	    	for (int i = 0, n = src.countItems(); i < n; ++i) {
	    		item = src.getItem(i);
	    		code = item.getItem(Tags.HumanPerformerCodeSeq);
	    		if (code == null) {
	    			log.warn("Missing >Human Performer Code Seq (0040,4009)");
	    		} else if (!code.containsValue(Tags.CodeValue)
	    		        || !code.containsValue(Tags.CodingSchemeDesignator)) {
	    			log.warn("Invalid Item in >Human Performer Code Seq (0040,4009)");    			
	    		}
	      	}
        }
        modifyStatus(iuid, actionInfo);
        service.sendActionNotification(iuid);
		return null;
	}

	private void modifyStatus(String iuid, Dataset actionInfo)
			throws DcmServiceException {
        try {
            getGPWLManager().modifyStatus(iuid, actionInfo);
        } catch (DcmServiceException e) {
        	log.error("Exception during status update:", e);
            throw e;
        } catch (Exception e) {
        	log.error("Exception during status update:", e);
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
	}

	private GPWLManager getGPWLManager()
    throws HomeFactoryException, RemoteException, CreateException {
        return ((GPWLManagerHome) EJBHomeFactory.getFactory().lookup(
                GPWLManagerHome.class, GPWLManagerHome.JNDI_NAME)).create();
    }
}
