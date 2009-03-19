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

package org.dcm4chex.archive.web.maverick.mwl.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.model.ModalityBaseFilterModel;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class MWLFilter extends ModalityBaseFilterModel {
    
	private Dataset dsSPS;
    public MWLFilter() {
        super();
	}
    
    public void init() {
    //Sched. procedure step seq
        dsSPS = ds.putSQ(Tags.SPSSeq).addNewItem();
        dsSPS.putAE( Tags.ScheduledStationAET );
        dsSPS.putDA(Tags.SPSStartDate);
        dsSPS.putTM(Tags.SPSStartTime);
        dsSPS.putCS( Tags.Modality );
        dsSPS.putPN( Tags.ScheduledPerformingPhysicianName );
        dsSPS.putLO( Tags.SPSDescription );
        dsSPS.putSH( Tags.ScheduledStationName );
        dsSPS.putSH( Tags.SPSLocation );
    	Dataset dsSpcs = addCodeSQ(ds, Tags.ScheduledProtocolCodeSeq);
    	{
    		DcmElement pCtxSq = dsSpcs.putSQ( Tags.ProtocolContextSeq );
    		Dataset dsPCtxItem = pCtxSq.addNewItem();
    		dsPCtxItem.putCS( Tags.ValueType );
    		addCodeSQ(ds, Tags.ConceptNameCodeSeq);
    		dsPCtxItem.putDT( Tags.DateTime );
    		dsPCtxItem.putPN( Tags.PersonName );
    		dsPCtxItem.putUT( Tags.TextValue );
    		addCodeSQ(ds, Tags.ConceptCodeSeq);
    		dsPCtxItem.putDS( Tags.NumericValue );
        	addCodeSQ(ds, Tags.MeasurementUnitsCodeSeq);
    		//TODO: all other from protocol code SQ 
    	}
        dsSPS.putLO( Tags.PreMedication );
        dsSPS.putSH( Tags.SPSID );
        dsSPS.putLO( Tags.RequestedContrastAgent );
        dsSPS.putCS(Tags.SPSStatus);
        //TODO: All other Attrs from SPS Module

        String d = new SimpleDateFormat(DATE_FORMAT).format(new Date());
        try {
            setStartDate( d );
        } catch (ParseException ignore) {}

//Requested Procedure        
        ds.putSH( Tags.RequestedProcedureID );
        ds.putLO( Tags.RequestedProcedureDescription );
        {
            Dataset dsRpcs = ds.putSQ( Tags.RequestedProcedureCodeSeq ).addNewItem();
            dsRpcs.putSH( Tags.CodeValue );
            dsRpcs.putLO( Tags.CodeMeaning );
            dsRpcs.putSH( Tags.CodingSchemeDesignator );
            dsRpcs.putSH( Tags.CodingSchemeVersion );
        }
        ds.putUI( Tags.StudyInstanceUID );
        {
            Dataset dsRefStdy = ds.putSQ( Tags.RefStudySeq ).addNewItem();
            dsRefStdy.putUI( Tags.RefSOPClassUID );
            dsRefStdy.putUI( Tags.RefSOPInstanceUID );
        }
        ds.putSH(Tags.RequestedProcedurePriority);
        ds.putLO(Tags.PatientTransportArrangements);
        //other Attrs from requested procedure Module
        ds.putLO(Tags.ReasonForTheRequestedProcedure);
        ds.putLT(Tags.RequestedProcedureComments);
        addCodeSQ(ds, Tags.ReasonforRequestedProcedureCodeSeq);
        ds.putSQ(Tags.RefStudySeq);
        ds.putLO(Tags.RequestedProcedureLocation);
        ds.putLO(Tags.ConfidentialityCode);
        ds.putSH(Tags.ReportingPriority);
        ds.putPN(Tags.NamesOfIntendedRecipientsOfResults);
        ds.putSQ(Tags.IntendedRecipientsOfResultsIdentificationSeq);
        
//imaging service request
        ds.putLT( Tags.ImagingServiceRequestComments );
        ds.putPN( Tags.RequestingPhysician );
        ds.putSQ(Tags.RequestingPhysicianIdentificationSeq);
        ds.putPN( Tags.ReferringPhysicianName );
        ds.putSQ(Tags.ReferringPhysicianIdentificationSeq);
        ds.putLO( Tags.RequestingService );
        ds.putSH( Tags.AccessionNumber );
        ds.putDA(Tags.IssueDateOfImagingServiceRequest);
        ds.putTM(Tags.IssueTimeOfImagingServiceRequest);
        ds.putLO( Tags.PlacerOrderNumber );
        ds.putLO( Tags.FillerOrderNumber );
        ds.putPN(Tags.OrderEnteredBy);
        ds.putSH(Tags.OrderEntererLocation);
        ds.putSH(Tags.OrderCallbackPhoneNumber);
        
//Patient/Visit Identification
        ds.putPN( Tags.PatientName );
        ds.putLO( Tags.PatientID);
        ds.putLO( Tags.IssuerOfPatientID);
        ds.putLO( Tags.AdmissionID );
//Visit Status
        ds.putLO(Tags.CurrentPatientLocation);
        
//Visit Relationship
        {
            Dataset dsRefPat = ds.putSQ( Tags.RefPatientSeq ).addNewItem();
            dsRefPat.putUI( Tags.RefSOPClassUID );
            dsRefPat.putUI( Tags.RefSOPInstanceUID );
        }
//Patient demographic
        ds.putDA( Tags.PatientBirthDate );
        ds.putCS( Tags.PatientSex );
        ds.putDS( Tags.PatientWeight );
        ds.putLO( Tags.ConfidentialityPatientData );
//Patient medical
        ds.putLO( Tags.PatientState );
        ds.putUS( Tags.PregnancyStatus );
        ds.putLO( Tags.MedicalAlerts );
        ds.putLO( Tags.ContrastAllergies );
        ds.putLO( Tags.SpecialNeeds );
    }
    

	private Dataset addCodeSQ(Dataset ds, int sqTag){
        DcmElement sq = ds.putSQ( sqTag );
        Dataset item = sq.addNewItem();
        item.putSH( Tags.CodeValue );
        item.putLO( Tags.CodeMeaning );
        item.putSH( Tags.CodingSchemeDesignator );
        item.putSH( Tags.CodingSchemeVersion );
        return item;
    }
	
	/**
	 * Set the start date.
	 * <p>
	 * Set both <code>startDate and startDateAsLong</code>.<br>
	 * If the parameter is null or empty, both values are set to <code>null</code>
	 * 
	 * @param startDate The start Date to set.
	 * @throws ParseException
	 */
	public void setStartDate(String startDate) throws ParseException {
        this.startDate = startDate;
        setDateRange(dsSPS, Tags.SPSStartDate, startDate );
	}
	
    public void setStationAET(String aet) {
        super.setStationAET(dsSPS, Tags.ScheduledStationAET, aet);
    }
	
	/**
	 * returns the modality filter value.
	 * 
	 * @return Filter value of modality field or null.
	 */
	public String getModality() {
		return dsSPS.getString( Tags.Modality );
	}
	
	/**
	 * set the filter modality.
	 * @param name
	 */
	public void setModality( String mod ){
		if ( mod == null || mod.trim().length() < 1 )
            dsSPS.putCS( Tags.Modality );
		else 
            dsSPS.putCS( Tags.Modality, mod);
	}
	
	/**
	 * @return Returns the accessionNumber.
	 */
	public String getAccessionNumber() {
		return ds.getString( Tags.AccessionNumber );
	}
	/**
	 * @param accessionNumber The accessionNumber to set.
	 */
	public void setAccessionNumber(String accessionNumber) {
		if ( accessionNumber == null || accessionNumber.trim().length() < 1 )
            ds.putSH( Tags.AccessionNumber );
		else
            ds.putSH( Tags.AccessionNumber, accessionNumber);
	}
	
}
