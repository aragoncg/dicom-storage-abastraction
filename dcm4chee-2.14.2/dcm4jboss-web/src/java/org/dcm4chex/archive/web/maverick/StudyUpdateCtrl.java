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

import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.model.PatientModel;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5426 $ $Date: 2007-11-12 23:37:43 +0100 (Mon, 12 Nov 2007) $
 * @since 5.10.2004
 *
 */
public class StudyUpdateCtrl extends Dcm4cheeFormController {

    private int patPk;

    private int studyPk;

    private String studyIUID;

    private String placerOrderNumber;

    private String fillerOrderNumber;

    private String accessionNumber;

    private String studyID;

    private String studyDateTime;

    private String studyDescription;

    private String referringPhysician;

    private String submit = null;

    private String cancel = null;

    public final void setPatPk(int patPk) {
        this.patPk = patPk;
    }

    public final void setStudyPk(int studyPk) {
        this.studyPk = studyPk;
    }

    public final void setPlacerOrderNumber(String s) {
        this.placerOrderNumber = s.trim();
    }

    public final void setFillerOrderNumber(String s) {
        this.fillerOrderNumber = s.trim();
    }

    public final void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public final void setReferringPhysician(String s) {
        this.referringPhysician = s.trim();
    }

    public final void setStudyDateTime(String s) {
        this.studyDateTime = s.trim();
    }

    public final void setStudyDescription(String s) {
        this.studyDescription = s.trim();
    }

    public final void setStudyID(String s) {
        this.studyID = s.trim();
    }
    
    public final void setStudyIUID(String s) {
        this.studyIUID = s.trim();
    }

    protected String perform() throws Exception {
        if (submit != null)
            if (studyPk == -1)
                executeCreate();
            else
                executeUpdate();
        return SUCCESS;
    }

    private ContentManager lookupContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory.getFactory()
                .lookup(ContentManagerHome.class, ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    private void executeCreate() {
        try {
	        StudyModel study = new StudyModel();
	        study.setPk( -1 );
	        if ( studyIUID == null ) studyIUID = UIDGenerator.getInstance().createUID();
            study.setStudyIUID( studyIUID );
	        study.setSpecificCharacterSet( "ISO_IR 100" );        
            study.setPlacerOrderNumber(placerOrderNumber);
            study.setFillerOrderNumber(fillerOrderNumber);
            study.setAccessionNumber(accessionNumber);
            study.setReferringPhysician(referringPhysician);
            study.setStudyDateTime(studyDateTime);
            study.setStudyDescription(studyDescription);
            study.setStudyID(studyID);
            FolderSubmitCtrl.getDelegate().createStudy( study.toDataset(), patPk );
            FolderForm form = FolderForm.getFolderForm(getCtx());
            PatientModel pat = form.getPatientByPk(patPk);

            ContentManager cm = lookupContentManager();
            List studies = cm.listStudiesOfPatient(patPk);
            for (int i = 0, n = studies.size(); i < n; i++)
                studies.set(i, new StudyModel((Dataset) studies.get(i)));
            pat.setStudies(studies);
            
            AuditLoggerDelegate.logProcedureRecord(getCtx(),
                    AuditLoggerDelegate.CREATE,
                    pat.getPatientID(),
                    pat.getPatientName(),
                    study.getPlacerOrderNumber(),
                    study.getFillerOrderNumber(),
                    study.getStudyIUID(),
                    study.getAccessionNumber(),
                    "new study:"+study.getStudyIUID() );
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
 
    private void executeUpdate() {
        try {
        	FolderForm form = FolderForm.getFolderForm(getCtx());
        	StudyModel study = form.getStudyByPk(patPk, studyPk);
            if ( !this.isStudyPermissionCheckDisabled() && 
            		!form.hasPermission(study.getStudyIUID(), StudyPermissionDTO.UPDATE_ACTION) ) {
            	form.setExternalPopupMsg("folder.update_denied", new String[]{study.getStudyIUID()} );
            	return;
            }
            //updating data model
            StringBuffer sb = new StringBuffer();
            boolean modified = false;            
            if (AuditLoggerDelegate.isModified("Placer Order Number",
                    study.getPlacerOrderNumber(), placerOrderNumber, sb)) {
                study.setPlacerOrderNumber(placerOrderNumber);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Filler Order Number",
                    study.getFillerOrderNumber(), fillerOrderNumber, sb)) {
                study.setFillerOrderNumber(fillerOrderNumber);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Accession Number",
                    study.getAccessionNumber(), accessionNumber, sb)) {
                study.setAccessionNumber(accessionNumber);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Referring Physician",
                    study.getReferringPhysician(), referringPhysician, sb)) {
                study.setReferringPhysician(referringPhysician);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Study Date/Time",
                    study.getStudyDateTime(), studyDateTime, sb)) {
                study.setStudyDateTime(studyDateTime);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Study Description",
                    study.getStudyDescription(), studyDescription, sb)) {
                study.setStudyDescription(studyDescription);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Study ID",
                    study.getStudyID(), studyID, sb)) {
                study.setStudyID(studyID);
                modified = true;
            }
            if (modified) {
	            FolderSubmitCtrl.getDelegate().updateStudy(study.toDataset());
	            PatientModel pat = form.getPatientByPk(patPk);
	            AuditLoggerDelegate.logProcedureRecord(getCtx(),
	                    AuditLoggerDelegate.MODIFY,
	                    pat.getPatientID(),
	                    pat.getPatientName(),
	                    study.getPlacerOrderNumber(),
	                    study.getFillerOrderNumber(),
	                    study.getStudyIUID(),
	                    study.getAccessionNumber(),
	                    AuditLoggerDelegate.trim(sb));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public final void setSubmit(String update) {
        this.submit = update;
    }

    public final void setCancel(String cancel) {
        this.cancel = cancel;
    }
}