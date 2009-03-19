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

package org.dcm4chex.archive.web.maverick;

import org.dcm4che.data.Dataset;
import org.dcm4chex.archive.web.maverick.model.PatientModel;

/**
 * @author umberto.cappellini@tiani.com
 */
public class PatientUpdateCtrl extends Dcm4cheeFormController {
    private long pk;

    private String patientID = "";

    private String issuerOfPatientID = "";

    private String patientName = "";

    private String patientSex = "";

    private String patientBirthDate = "";

    private String submit = null;

    private String cancel = null;

    protected String perform() throws Exception {
    	getCtx().getRequest().getSession().setAttribute("errorMsg", null);
        if (submit != null) {
            if (pk == -1)
                return executeCreate();
            else
                return executeUpdate();
        } else {
        	FolderForm.getFolderForm( getCtx() ).setEditPatient(null);
        }
        return SUCCESS;
    }

    private String executeCreate() {
        FolderForm form = FolderForm.getFolderForm( getCtx() );
        try {
	        PatientModel pat = new PatientModel();
	        pat.setPk(-1);
	        pat.setSpecificCharacterSet("ISO_IR 100");        
	        pat.setPatientID(patientID);
	        pat.setIssuerOfPatientID(issuerOfPatientID);
	        pat.setPatientSex(patientSex);
	        pat.setPatientName(patientName);
	        pat.setPatientBirthDate(patientBirthDate);
	        form.setEditPatient(pat);
	        Dataset ds = FolderSubmitCtrl.getDelegate().createPatient(pat.toDataset());
	        
	        //add new patient to model (as first element) and set sticky flag!
	        pat = new PatientModel( ds );
	        form.getStickyPatients().add( String.valueOf( pat.getPk() ) );
	        form.getPatients().add(0, pat);
	        form.setEditPatient(null);
            AuditLoggerDelegate.logPatientRecord(getCtx(), AuditLoggerDelegate.CREATE, pat
                    .getPatientID(), pat.getPatientName(), null);
            return SUCCESS;
        } catch (Exception e) {
        	form.setExternalPopupMsg("folder.err_createPatient",new String[]{e.getMessage()});
            return ERROR;
        }
    }

    
    private String executeUpdate() {
        try {
            PatientModel pat = FolderForm.getFolderForm(
                    getCtx()).getPatientByPk(pk);
            StringBuffer sb = new StringBuffer();
            boolean modified = false;            
            if (AuditLoggerDelegate.isModified("Patient Name",
                    pat.getPatientName(), patientName, sb)) {
                pat.setPatientName(patientName);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Patient Sex",
                    pat.getPatientSex(), patientSex, sb)) {
                pat.setPatientSex(patientSex);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Birth Date",
                    pat.getPatientBirthDate(), patientBirthDate, sb)) {
                pat.setPatientBirthDate(patientBirthDate);
                modified = true;
            }
            if (modified) {
	            //updating data model
	            FolderSubmitCtrl.getDelegate().updatePatient(pat.toDataset());
	            AuditLoggerDelegate.logPatientRecord(getCtx(), 
	                    AuditLoggerDelegate.MODIFY, pat.getPatientID(),
	                    pat.getPatientName(), AuditLoggerDelegate.trim(sb));
            }
            return SUCCESS;
        } catch (Exception e) {
            FolderForm.getFolderForm( getCtx() ).setExternalPopupMsg("folder.err_updatePatient",new String[]{e.getMessage()});
            return ERROR;
        }
    }


    public final void setIssuerOfPatientID(String issuerOfPatientID) {
        this.issuerOfPatientID = issuerOfPatientID.trim();
    }

    public final void setPatientID(String patientID) {
        this.patientID = patientID.trim();
    }

    public final void setPatientName(String patientName) {
        this.patientName = patientName.trim();
    }

    public final void setPatientSex(String patientSex) {
        this.patientSex = patientSex.trim();
    }

    public final void setPatientBirthDate(String date) {
        this.patientBirthDate = date.trim();
    }

    public final void setPk(long pk) {
        this.pk = pk;
    }

    public final void setSubmit(String update) {
        this.submit = update;
    }

    public final void setCancel(String cancel) {
        this.cancel = cancel;
    }
}