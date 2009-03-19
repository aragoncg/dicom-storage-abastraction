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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Gunter Zeilinger, Huetteldorferstr. 24/10, 1150 Vienna/Austria/Europe.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Gunter Zeilinger <gunterze@gmail.com>
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

package org.dcm4chex.archive.ejb.session;

import java.util.ArrayList;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.ejb.interfaces.GPPPSLocal;
import org.dcm4chex.archive.ejb.interfaces.GPPPSLocalHome;
import org.dcm4chex.archive.ejb.interfaces.GPSPSLocal;
import org.dcm4chex.archive.ejb.interfaces.GPSPSLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 7895 $ $Date: 2008-11-03 11:45:06 +0100 (Mon, 03 Nov 2008) $
 * @since Apr 9, 2006
 *
 * @ejb.bean name="GPPPSManager" type="Stateless" view-type="remote"
 *  jndi-name="ejb/GPPPSManager"
 * @ejb.transaction-type  type="Container"
 * @ejb.transaction type="Required"
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * @ejb.ejb-ref ejb-name="GPSPS" view-type="local" ref-name="ejb/GPSPS" 
 * @ejb.ejb-ref ejb-name="GPPPS" view-type="local" ref-name="ejb/GPPPS" 
 */
public abstract class GPPPSManagerBean implements SessionBean {

    private static Logger log = Logger.getLogger(GPPPSManagerBean.class);
    private static final int GPSPS_NOT_IN_PROGRESS = 0xA504;
    private static final int GPSPS_DIFF_TRANS_UID = 0xA505;
    private static final int GPPPS_NOT_IN_PROGRESS = 0xA506;
    private static final int[] PATIENT_ATTRS_EXC = {
            Tags.RefPatientSeq,         
            Tags.PatientName,
            Tags.PatientID,
            Tags.PatientBirthDate,
            Tags.PatientSex,
    };
    private static final int[] PATIENT_ATTRS_INC = {
            Tags.PatientName,
            Tags.PatientID,
            Tags.PatientBirthDate,
            Tags.PatientSex,
    };
    private PatientLocalHome patHome;
    private GPSPSLocalHome spsHome;
    private GPPPSLocalHome ppsHome;

    public void setSessionContext(SessionContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome =
                (PatientLocalHome) jndiCtx.lookup("java:comp/env/ejb/Patient");
            spsHome = (GPSPSLocalHome) jndiCtx.lookup("java:comp/env/ejb/GPSPS");
            ppsHome = (GPPPSLocalHome) jndiCtx.lookup("java:comp/env/ejb/GPPPS");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {}
            }
        }
    }

    public void unsetSessionContext() {
        spsHome = null;
        ppsHome = null;
        patHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public void createGPPPS(Dataset ds)
    throws DcmServiceException {
        checkDuplicate(ds.getString(Tags.SOPInstanceUID));
        PatientLocal pat = findOrCreatePatient(ds);          
        Collection gpsps = findRefGpsps(ds.get(Tags.RefGPSPSSeq), pat);
        GPPPSLocal pps = doCreate(ds, pat);
        if (gpsps != null) {
            pps.setGpsps(gpsps);
        }
    }

    private PatientLocal findOrCreatePatient(Dataset ds)
            throws DcmServiceException {
        try {
            Collection c = patHome.selectByPatientDemographic(ds);
            if (c.size() == 1) {
                return patHome.followMergedWith(
                        (PatientLocal) c.iterator().next());
            }
            return patHome.create(ds.subSet(PATIENT_ATTRS_INC));
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }           
    }

    private GPPPSLocal doCreate(Dataset ds, PatientLocal pat)
    throws DcmServiceException {
        try {
            return ppsHome.create(ds.subSet(PATIENT_ATTRS_EXC, true, true), 
                    pat);
        } catch (CreateException e) {
            log.error("Creation of GP-PPS(iuid=" 
                    + ds.getString(Tags.SOPInstanceUID) + ") failed: ", e);
            throw new DcmServiceException(Status.ProcessingFailure);
        }                
    }

    private void checkDuplicate(String ppsiuid) throws DcmServiceException {
        try {
            ppsHome.findBySopIuid(ppsiuid);
            throw new DcmServiceException(Status.DuplicateSOPInstance);
        } catch (ObjectNotFoundException e) { // Ok           
        } catch (FinderException e) {
            log.error("Query for GP-PPS(iuid=" + ppsiuid + ") failed: ", e);
            throw new DcmServiceException(Status.ProcessingFailure);
        }
    }

    private Collection findRefGpsps(DcmElement spssq, PatientLocal pat)
    throws DcmServiceException {
        if (spssq == null) return null;
        int n = spssq.countItems();
        ArrayList c = new ArrayList(n);
        for (int i = 0; i < n; i++) {
            Dataset refSOP = spssq.getItem(i);
            String spsiuid = refSOP.getString(Tags.RefSOPInstanceUID);
            String spstuid = refSOP.getString(Tags.RefGPSPSTransactionUID);
            GPSPSLocal sps;
            try {
                sps = spsHome.findBySopIuid(spsiuid);
                PatientLocal spspat = sps.getPatient();
                if (!pat.isIdentical(spspat)) {
                    log.info("Patient of referenced GP-SPS(iuid=" + spsiuid
                            + "): " + spspat.asString() 
                            + " differes from Patient of GP-PPS: "
                            + pat.asString());                   
                    throw new DcmServiceException(Status.InvalidAttributeValue,
                            "GP-SPS PID: " + spspat.getPatientId()
                            + ", GP-PPS PID: " + pat.getPatientId());                
                }
                if (!sps.isInProgress()) {
                    String spsstatus = sps.getGpspsStatus();
                    log.info("Status of referenced GP-SPS(iuid=" + spsiuid
                            + ") is not IN PROGRESS, but " + spsstatus);
                    throw new DcmServiceException(GPSPS_NOT_IN_PROGRESS,
                            "ref GP-SPS status: " + spsstatus);                
                }
                String tuid = sps.getTransactionUid();
                if (!spstuid.equals(tuid)) {
                    log.info("Referenced GP-SPS Transaction UID: " + spstuid 
                            + " does not match the Transaction UID: " + tuid
                            + " of the N-ACTION request");
                    throw new DcmServiceException(GPSPS_DIFF_TRANS_UID);                
                }
                c.add(sps);
            } catch (ObjectNotFoundException e) {
                log.info("Referenced GP-SPS(iuid=" + spsiuid
                        + ") not in provided GP-WL");
            } catch (FinderException e) {
                log.error("Query for GP-SPS(iuid=" + spsiuid
                        + ") failed: ", e);
                throw new DcmServiceException(Status.ProcessingFailure);
            }
        }
        return c;
    }

    /**
     * @ejb.interface-method
     */
    public Dataset getGPPPS(String iuid) throws DcmServiceException {
        GPPPSLocal pps = findBySopIuid(iuid);
        final PatientLocal pat = pps.getPatient();
        Dataset attrs = pps.getAttributes();            
        attrs.putAll(pat.getAttributes(false));
        return attrs;
    }
    
    /**
     * @ejb.interface-method
     */
    public void updateGPPPS(Dataset ds)
        throws DcmServiceException {
        final String iuid = ds.getString(Tags.SOPInstanceUID);
        GPPPSLocal pps = findBySopIuid(iuid);
        if (!pps.isInProgress()) {
            String ppsstatus = pps.getPpsStatus();
            log.info("Status of GP-PPS(iuid=" + iuid
                    + ") is not IN PROGRESS, but " + ppsstatus);
            throw new DcmServiceException(GPPPS_NOT_IN_PROGRESS,
                    "GP-PPS status: " + ppsstatus);                
        }
        Dataset attrs = pps.getAttributes();
        attrs.putAll(ds);
        pps.setAttributes(attrs);
    }

    private GPPPSLocal findBySopIuid(String iuid) throws DcmServiceException {
        try {
            return ppsHome.findBySopIuid(iuid);
        } catch (ObjectNotFoundException e) {
            throw new DcmServiceException(Status.NoSuchObjectInstance);
        } catch (FinderException e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }
    
    /**
	 * @ejb.interface-method
	 */
	public void removeGPPPS(String iuid) 
			throws EJBException, RemoveException, FinderException {
		ppsHome.findBySopIuid(iuid).remove();
		log.info("GPPPS removed:"+iuid);
	}    
}
