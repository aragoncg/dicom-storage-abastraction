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

package org.dcm4chex.archive.web.maverick.gpwl.model;

import java.text.ParseException;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.model.BasicFilterModel;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class GPWLFilter extends BasicFilterModel {

    Dataset workitemDS; 
    Dataset stationNameCodeDS;
    Dataset stationClassCodeDS;
    Dataset stationGeoCodeDS;
    Dataset humanCodeDS;
    Dataset refReqSqItem;
    
	/** holds the SPS Start time range. */
	private String spsStartDate = null;
	/** holds the Expected Completion time range. */
	private String completionStartDate = null;

	
	public GPWLFilter() {
        super();
	}
    
    public void init() {
        ds.putUI(Tags.SOPInstanceUID);
        //
        ds.putCS(Tags.GPSPSStatus);
        ds.putCS(Tags.InputAvailabilityFlag);
        ds.putCS(Tags.GPSPSPriority);
        ds.putCS(Tags.SPSID);
        //workitem code Sequence
        workitemDS = ds.putSQ(Tags.ScheduledWorkitemCodeSeq).addNewItem();
        workitemDS.putSH(Tags.CodeValue);
        workitemDS.putSH(Tags.CodingSchemeDesignator);
        workitemDS.putSH(Tags.CodeMeaning);
        //Scheduled Processing Applications code sequence
        Dataset appDS = ds.putSQ(Tags.ScheduledProcessingApplicationsCodeSeq).addNewItem();
        appDS.putSH(Tags.CodeValue);
        appDS.putSH(Tags.CodingSchemeDesignator);
        appDS.putSH(Tags.CodeMeaning);
        //Scheduled station Name code sequence
        stationNameCodeDS = ds.putSQ(Tags.ScheduledStationNameCodeSeq).addNewItem();
        stationNameCodeDS.putSH(Tags.CodeValue);
        stationNameCodeDS.putSH(Tags.CodingSchemeDesignator);
        stationNameCodeDS.putSH(Tags.CodeMeaning);
        //Scheduled station Class code sequence
        stationClassCodeDS = ds.putSQ(Tags.ScheduledStationClassCodeSeq).addNewItem();
        stationClassCodeDS.putSH(Tags.CodeValue);
        stationClassCodeDS.putSH(Tags.CodingSchemeDesignator);
        stationClassCodeDS.putSH(Tags.CodeMeaning);
        //Scheduled station geographic location code sequence
        stationGeoCodeDS = ds.putSQ(Tags.ScheduledStationGeographicLocationCodeSeq).addNewItem();
        stationGeoCodeDS.putSH(Tags.CodeValue);
        stationGeoCodeDS.putSH(Tags.CodingSchemeDesignator);
        stationGeoCodeDS.putSH(Tags.CodeMeaning);

        //Scheduled human performer code sequence
        humanCodeDS = ds.putSQ(Tags.HumanPerformerCodeSeq).addNewItem();
        humanCodeDS.putSH(Tags.CodeValue);
        humanCodeDS.putSH(Tags.CodingSchemeDesignator);
        humanCodeDS.putSH(Tags.CodeMeaning);
        
        refReqSqItem = ds.putSQ(Tags.RefRequestSeq).addNewItem();
        refReqSqItem.putUI(Tags.StudyInstanceUID );
        refReqSqItem.putSH( Tags.AccessionNumber );
        ds.putPN( Tags.PatientName );
        ds.putLO( Tags.PatientID);
        ds.putLO( Tags.IssuerOfPatientID);
        //Patient demographic
        ds.putDA( Tags.PatientBirthDate );
        ds.putCS( Tags.PatientSex );
        
        ds.putDT(Tags.SPSStartDateAndTime);
        ds.putDT(Tags.ExpectedCompletionDateAndTime);
    }
	/**
	 * @return Returns the iuid.
	 */
	public String getIUID() {
		return ds.getString(Tags.SOPInstanceUID);
	}
	/**
	 * @param iuid The iuid to set.
	 */
	public void setIUID(String iuid) {
        ds.putUI(Tags.SOPInstanceUID, iuid);
	}
	/**
	 * @return Returns the humanPerformerCode.
	 */
	public String getHumanPerformerCode() {
		return humanCodeDS.getString(Tags.CodeValue);
	}
	/**
	 * @param humanPerformerCode The humanPerformerCode to set.
	 */
	public void setHumanPerformerCode(String humanPerformerCode) {
        humanCodeDS.putSH(Tags.CodeValue, humanPerformerCode);
	}
	/**
	 * @return Returns the inputAvailability.
	 */
	public String getInputAvailability() {
		return ds.getString(Tags.InputAvailabilityFlag);
	}
	/**
	 * @param inputAvailability The inputAvailability to set.
	 */
	public void setInputAvailability(String inputAvailability) {
        ds.putCS(Tags.InputAvailabilityFlag, inputAvailability);
	}
	/**
	 * @return Returns the priority.
	 */
	public String getPriority() {
		return ds.getString(Tags.GPSPSPriority);
	}
	/**
	 * @param priority The priority to set.
	 */
	public void setPriority(String priority) {
        ds.putCS(Tags.GPSPSPriority, priority);
	}
    /**
     * @return Returns the spsID.
     */
    public String getSpsID() {
    	return ds.getString(Tags.SPSID);
    }
    /**
     * @param spsID The spsID to set.
     */
    public void setSpsID(String spsID) {
        ds.putCS(Tags.SPSID, spsID);
    }
	/**
	 * @return Returns the stationClassCode.
	 */
	public String getStationClassCode() {
		return stationClassCodeDS.getString(Tags.CodeValue);
	}
	/**
	 * @param stationClassCode The stationClassCode to set.
	 */
	public void setStationClassCode(String stationClassCode) {
        stationClassCodeDS.putSH(Tags.CodeValue, stationClassCode);
	}
	/**
	 * @return Returns the stationGeoCode.
	 */
	public String getStationGeoCode() {
		return stationGeoCodeDS.getString(Tags.CodeValue);
	}
	/**
	 * @param stationGeoCode The stationGeoCode to set.
	 */
	public void setStationGeoCode(String stationGeoCode) {
        stationGeoCodeDS.putSH(Tags.CodeValue, stationGeoCode);
	}
	/**
	 * @return Returns the stationNameCode.
	 */
	public String getStationNameCode() {
		return stationNameCodeDS.getString(Tags.CodeValue);
	}
	/**
	 * @param stationNameCode The stationNameCode to set.
	 */
	public void setStationNameCode(String stationNameCode) {
        stationNameCodeDS.putSH(Tags.CodeValue, stationNameCode);
	}
	/**
	 * @return Returns the status.
	 */
	public String getStatus() {
		return ds.getString(Tags.GPSPSStatus);
	}
	/**
	 * @param status The status to set.
	 */
	public void setStatus(String status) {
        ds.putCS(Tags.GPSPSStatus, status);
	}
	/**
	 * @return Returns the workitemCode.
	 */
	public String getWorkitemCode() {
		return workitemDS.getString(Tags.CodeValue);
	}
	/**
	 * @param workitemCode The workitemCode to set.
	 */
	public void setWorkitemCode(String workitemCode) {
        workitemDS.putSH(Tags.CodeValue, workitemCode);
	}
	
	/**
	 * Set the Scheduled Procedure Step query start date.
	 * <p>
	 * 
	 * 
	 * @param startDate The start Date to set.
	 * @throws ParseException
	 */
	public void setSPSStartDate(String startDate) throws ParseException {
		spsStartDate = startDate;
        setDateRange(ds, Tags.SPSStartDateAndTime, startDate );
	}
	
	public String getSPSStartDate() {
		return spsStartDate;
	}
	

	/**
	 * Set the Scheduled Procedure Step query start date.
	 * <p>
	 * 
	 * 
	 * @param startDate The start Date to set.
	 * @throws ParseException
	 */
	public void setCompletionStartDate(String startDate) throws ParseException {
		completionStartDate = startDate;
        setDateRange(ds, Tags.ExpectedCompletionDateAndTime, startDate );
	}
	
	public String getCompletionStartDate() {
		return completionStartDate;
	}
	
	/**
	 * @return
	 * @throws ParseException
	 */

	/**
	 * @return Returns the studyIUID.
	 */
	public String getStudyIUID() {
		return refReqSqItem.getString(Tags.StudyInstanceUID);
	}
	/**
	 * @param studyIUID The studyIUID to set.
	 */
	public void setStudyIUID(String studyIUID) {
        refReqSqItem.putUI(Tags.StudyInstanceUID, studyIUID);
	}
	/**
	 * @return Returns the accessionNumber.
	 */
	public String getAccessionNumber() {
		return refReqSqItem.getString( Tags.AccessionNumber );
	}
	/**
	 * @param accessionNumber The accessionNumber to set.
	 */
	public void setAccessionNumber(String accessionNumber) {
        refReqSqItem.putSH( Tags.AccessionNumber, accessionNumber );
    }
	/**
	 * 
	 */
	public void clear() {
        setStatus(null);
        setInputAvailability(null);
        setPriority(null);
        this.setWorkitemCode(null);
        this.setStationNameCode(null);
        this.setStationClassCode(null);
        this.setStationGeoCode(null);
        try {
            this.setSPSStartDate(null);
        } catch (ParseException ignore) {}
        try {
            this.setCompletionStartDate(null);
        } catch (ParseException ignore) {}
        this.setHumanPerformerCode(null);
        this.setPatientName(null);
        this.setStudyIUID(null);
        this.setAccessionNumber(null);
        this.setSpsID(null);
	}
	
}
