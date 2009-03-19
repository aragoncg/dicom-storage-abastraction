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

package org.dcm4chex.archive.ejb.session;

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
import org.dcm4che.data.DcmObject;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.MWLItemLocal;
import org.dcm4chex.archive.ejb.interfaces.MWLItemLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.exceptions.DuplicateMWLItemException;
import org.dcm4chex.archive.exceptions.PatientMismatchException;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7895 $ $Date: 2008-11-03 11:45:06 +0100 (Mon, 03 Nov 2008) $
 * @since 10.12.2003
 * 
 * @ejb.bean name="MWLManager" type="Stateless" view-type="remote"
 *           jndi-name="ejb/MWLManager"
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * @ejb.ejb-ref ejb-name="MWLItem" view-type="local" ref-name="ejb/MWLItem"
 */
public abstract class MWLManagerBean implements SessionBean {
    private static final int[] PATIENT_ATTRS_WITH_CHARSET = {
        Tags.SpecificCharacterSet,
        Tags.PatientName,
        Tags.PatientID,
        Tags.IssuerOfPatientID,
        Tags.PatientBirthDate, 
        Tags.PatientSex,
        Tags.OtherPatientIDSeq,
        Tags.PatientMotherBirthName
    };

    private static final int[] PATIENT_ATTRS = {
        Tags.PatientName,
        Tags.PatientID,
        Tags.IssuerOfPatientID,
        Tags.PatientBirthDate, 
        Tags.PatientSex,
        Tags.OtherPatientIDSeq,
        Tags.PatientMotherBirthName
    };

    private static Logger log = Logger.getLogger(MWLManagerBean.class);

    private PatientLocalHome patHome;

    private MWLItemLocalHome mwlItemHome;

    public void setSessionContext(SessionContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Patient");
            mwlItemHome = (MWLItemLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/MWLItem");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    public void unsetSessionContext() {
        mwlItemHome = null;
        patHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public Dataset removeWorklistItem(String rpid, String spsid) {
        try {
            MWLItemLocal mwlItem = mwlItemHome.findByRpIdAndSpsId(rpid, spsid);
            Dataset attrs = toAttributes(mwlItem);
            mwlItem.remove();
            return attrs;
        } catch (ObjectNotFoundException e) {
            return null;
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @ejb.interface-method
     */
    public Dataset removeWorklistItem(Dataset ds)
            throws PatientMismatchException, FinderException, RemoveException {
        try {
            MWLItemLocal mwlItem = getWorklistItem(ds, false);
            Dataset attrs = toAttributes(mwlItem);
            mwlItem.remove();
            return attrs;
        } catch (ObjectNotFoundException e) {
            return null;
        }
    }

    private MWLItemLocal getWorklistItem(Dataset ds, boolean updatePatient)
            throws FinderException, PatientMismatchException {
        Dataset sps = ds.getItem(Tags.SPSSeq);
        MWLItemLocal mwlItem = mwlItemHome.findByRpIdAndSpsId(
                    ds.getString(Tags.RequestedProcedureID),
                    sps.getString(Tags.SPSID));
        PatientLocal pat = mwlItem.getPatient();
        try {
            if (patHome.selectByPatientId(ds).isIdentical(pat)) {
                if (updatePatient) {
                    pat.updateAttributes(ds.subSet(PATIENT_ATTRS_WITH_CHARSET));
                }
                return mwlItem;
            }
        } catch (ObjectNotFoundException onfe) {}
        String prompt = "Patient[pid="
            + ds.getString(Tags.PatientID) + ", issuer=" 
            + ds.getString(Tags.IssuerOfPatientID)
            + "] does not match Patient associated with "
            + mwlItem.asString();
        log.warn(prompt);
        throw new PatientMismatchException(prompt);        
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateSPSStatus(String rpid, String spsid, String status) {
        try {
            MWLItemLocal mwlItem = mwlItemHome.findByRpIdAndSpsId(rpid, spsid);
            Dataset attrs = mwlItem.getAttributes();
            attrs.getItem(Tags.SPSSeq).putCS(Tags.SPSStatus, status);
            mwlItem.setAttributes(attrs);
            return true;
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateSPSStatus(Dataset ds) throws PatientMismatchException {
        MWLItemLocal mwlItem;
        try {
            mwlItem = getWorklistItem(ds, true);
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        String status = ds.getItem(Tags.SPSSeq).getString(Tags.SPSStatus);
        Dataset attrs = mwlItem.getAttributes();
        attrs.getItem(Tags.SPSSeq).putCS(Tags.SPSStatus, status);
        mwlItem.setAttributes(attrs);
        return true;
    }


    private PatientLocal updateOrCreatePatient(Dataset ds)
            throws FinderException, CreateException  {
        try {
            return patHome.selectByPatientId(ds);
        } catch (ObjectNotFoundException onfe) {
            return patHome.create(ds.subSet(PATIENT_ATTRS_WITH_CHARSET));
        }           
    }
    
    /**
     * @ejb.interface-method
     */
    public Dataset addWorklistItem(Dataset ds)
            throws CreateException, FinderException {
        checkDuplicate(ds);
        MWLItemLocal mwlItem = mwlItemHome.create(ds.subSet(PATIENT_ATTRS,
                true, true), updateOrCreatePatient(ds));
        return toAttributes(mwlItem);
    }


    private void checkDuplicate(Dataset ds)
            throws DuplicateMWLItemException, FinderException {
        try {
            Dataset sps = ds.getItem(Tags.SPSSeq);
            MWLItemLocal mwlItem = mwlItemHome.findByRpIdAndSpsId(
                    ds.getString(Tags.RequestedProcedureID),
                    sps.getString(Tags.SPSID));
            throw new DuplicateMWLItemException("Duplicate " + mwlItem.asString());
        } catch (ObjectNotFoundException e) { // Ok           
        }
    } 
    
    private Dataset toAttributes(MWLItemLocal mwlItem) {
        Dataset attrs = mwlItem.getAttributes();
        attrs.putAll(mwlItem.getPatient().getAttributes(false));
        return attrs;
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateWorklistItem(Dataset ds)
            throws PatientMismatchException {
        MWLItemLocal mwlItem;
        try {
            mwlItem = getWorklistItem(ds, true);
        } catch (ObjectNotFoundException e) {
            return false;
        } catch (FinderException e) {
            throw new EJBException(e);
        } 
        Dataset attrs = mwlItem.getAttributes();
        attrs.putAll(ds.subSet(PATIENT_ATTRS, true, true),
                DcmObject.MERGE_ITEMS);
        mwlItem.setAttributes(attrs);
        return true;
    }
}
