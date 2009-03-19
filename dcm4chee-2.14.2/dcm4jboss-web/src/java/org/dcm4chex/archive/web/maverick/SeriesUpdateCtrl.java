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
import org.dcm4chex.archive.web.maverick.model.SeriesModel;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5426 $ $Date: 2007-11-12 23:37:43 +0100 (Mon, 12 Nov 2007) $
 * @since 5.10.2004
 *
 */
public class SeriesUpdateCtrl extends Dcm4cheeFormController {

    private int patPk;

    private int studyPk;
    
    private int seriesPk;

    private String bodyPartExamined;

    private String laterality;

    private String manufacturer;

    private String manufacturerModelName;

    private String modality;

    private String seriesDateTime;

    private String seriesDescription;

    private String seriesNumber;

    private String submit = null;

    private String cancel = null;

    public final void setPatPk(int patPk) {
        this.patPk = patPk;
    }

    public final void setStudyPk(int studyPk) {
        this.studyPk = studyPk;
    }

    public final void setSeriesPk(int seriesPk) {
        this.seriesPk = seriesPk;
    }
    
    public final void setBodyPartExamined(String s) {
        this.bodyPartExamined = s;
    }

    public final void setLaterality(String s) {
        this.laterality = s;
    }

    public final void setManufacturer(String s) {
        this.manufacturer = s;
    }

    public final void setManufacturerModelName(String s) {
        this.manufacturerModelName = s;
    }

    public final void setModality(String s) {
        this.modality = s;
    }

    public final void setSeriesDateTime(String s) {
        this.seriesDateTime = s;
    }

    public final void setSeriesDescription(String s) {
        this.seriesDescription = s;
    }

    public final void setSeriesNumber(String s) {
        this.seriesNumber = s;
    }
    
    protected String perform() throws Exception {
        if (submit != null)
        	if ( seriesPk != -1 )
        		executeUpdate();
        	else 
        		executeCreate();
        return SUCCESS;
    }

    private ContentManager lookupContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory.getFactory()
                .lookup(ContentManagerHome.class, ContentManagerHome.JNDI_NAME);
        return home.create();
    }

    private void executeCreate() {
        try {
        	SeriesModel series = new SeriesModel();
        	series.setSpecificCharacterSet("ISO_IR 100");
        	series.setPk( -1 );
        	series.setSeriesIUID( UIDGenerator.getInstance().createUID() );
        	
        	series.setBodyPartExamined(bodyPartExamined);
        	series.setLaterality(laterality);
        	series.setModality(modality);
        	series.setSeriesDateTime(seriesDateTime);
        	series.setSeriesDescription(seriesDescription);
        	series.setSeriesNumber(seriesNumber);
        	
            FolderSubmitCtrl.getDelegate().createSeries( series.toDataset(), studyPk );
            FolderForm form = FolderForm.getFolderForm(getCtx());
            PatientModel pat = form.getPatientByPk(patPk);
            StudyModel study = form.getStudyByPk(patPk, studyPk);
            
            ContentManager cm = lookupContentManager();
            List allSeries = cm.listSeriesOfStudy(studyPk);
            for (int i = 0, n = allSeries.size(); i < n; i++)
                allSeries.set(i, new SeriesModel((Dataset) allSeries.get(i)));
            form.getStudyByPk(patPk, studyPk).setSeries(allSeries);

            AuditLoggerDelegate.logProcedureRecord(getCtx(),
                    AuditLoggerDelegate.CREATE,
                    pat.getPatientID(),
                    pat.getPatientName(),
                    study.getPlacerOrderNumber(),
                    study.getFillerOrderNumber(),
                    study.getStudyIUID(),
                    study.getAccessionNumber(),
                    "new series:"+series.getSeriesIUID() );

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
            SeriesModel series = form.getSeriesByPk(patPk, studyPk, seriesPk);
            
            //updating data model
            StringBuffer sb = new StringBuffer("Series[");
            sb.append(series.getSeriesIUID()).append(" ] modified: ");
            boolean modified = false;            
            if (AuditLoggerDelegate.isModified("Body Part Examined",
                    series.getBodyPartExamined(), bodyPartExamined, sb)) {
                series.setBodyPartExamined(bodyPartExamined);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Laterality",
                    series.getLaterality(), laterality, sb)) {
                series.setLaterality(laterality);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Modality",
                    series.getModality(), modality, sb)) {
                series.setModality(modality);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Series Date/Time",
                    series.getSeriesDateTime(), seriesDateTime, sb)) {
                series.setSeriesDateTime(seriesDateTime);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Series Description",
                    series.getSeriesDescription(), seriesDescription, sb)) {
                series.setSeriesDescription(seriesDescription);
                modified = true;
            }
            if (AuditLoggerDelegate.isModified("Series Number",
                    series.getSeriesNumber(), seriesNumber, sb)) {
                series.setSeriesNumber(seriesNumber);
                modified = true;
            }
            if (modified) {
	            FolderSubmitCtrl.getDelegate().updateSeries(series.toDataset());
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