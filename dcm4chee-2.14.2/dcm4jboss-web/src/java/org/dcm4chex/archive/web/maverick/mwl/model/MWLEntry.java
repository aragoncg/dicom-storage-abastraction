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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;

/**
 * @author franz.willer
 *
 * Holds a single entry of the MWLList from the model.
 * <p>
 * This class is a wrapper over a Dataset to get values with getter methods for the view. 
 */
public class MWLEntry {

	private Dataset ds;
	private Dataset spsItem;
	
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat dtformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat shortDTformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	/** The Date formatter to format date values. */
	private static final SimpleDateFormat dformatter = new SimpleDateFormat("yyyy/MM/dd");
	
	
	public MWLEntry( Dataset ds ) {
		this.ds = ds;
		spsItem = ds.get( Tags.SPSSeq ).getItem();//scheduled procedure step sequence item.
	}
    
    public Dataset toDataset() {
        return ds;
    }
	
	public String getSpsID() {
		return spsItem.getString( Tags.SPSID );
	}
	/**
	 * @return Returns the modality.
	 */
	public String getModality() {
		return spsItem.getString( Tags.Modality );
	}
	/**
	 * @return Returns the physiciansName.
	 */
	public String getPhysiciansName() {
		return spsItem.getString( Tags.ScheduledPerformingPhysicianName );
	}
	/**
	 * @return Returns the reqProcedureID.
	 */
	public String getReqProcedureID() {
		return ds.getString( Tags.RequestedProcedureID );
	}
        

        public String getRqSpsID() {
            return getReqProcedureID() + '\\' + getSpsID();
        }

	/**
	 * @return Returns the spsStartDateTime.
	 */
	public String getSpsStartDateTime() {
		Date d = spsItem.getDate( Tags.SPSStartDateAndTime );
		if ( d == null ) {
			d = spsItem.getDateTime( Tags.SPSStartDate, Tags.SPSStartTime );
		}
		if ( d == null ) return "";
		
		return shortDTformatter.format( d );
	}

	/**
	 * @return Returns the stationAET.
	 */
	public String getStationAET() {
		return spsItem.getString( Tags.ScheduledStationAET );
	}
	/**
	 * @return Returns the stationName.
	 */
	public String getStationName() {
		return spsItem.getString( Tags.ScheduledStationName );
	}
	
	public String getAccessionNumber() {
		return ds.getString( Tags.AccessionNumber );
	}
	
	public String getPatientName() {
		return ds.getString( Tags.PatientName );
	}
	public String getPatientID() {
		return ds.getString( Tags.PatientID );
	}
	
	public String getStudyUID() {
		return ds.getString( Tags.StudyInstanceUID );
	}
	public String getImgServiceReqComments() {
		return ds.getString( Tags.ImagingServiceRequestComments );
	}
	public String getRequestingPhysician() {
		return ds.getString( Tags.RequestingPhysician );
	}
	public String getReferringPhysicianName() {
		return ds.getString( Tags.ReferringPhysicianName );
	}
	public String getPlacerOrderNumber() {
		return ds.getString( Tags.PlacerOrderNumber );
	}
	public String getFillerOrderNumber() {
		return ds.getString( Tags.FillerOrderNumber );
	}
	//Visit Identification
	public String getAdmissionID() {
		return ds.getString( Tags.AdmissionID );
	}
	//Patient demographic
	public String getPatientBirthDate() {
		Date d = ds.getDate( Tags.PatientBirthDate );
		if ( d == null ) return "";
		return dformatter.format( d );
	}
	
	public String getPatientSex() {
		return ds.getString( Tags.PatientSex );
	}
	
	public String getSPSDescription() {
		String desc = null;
		DcmElement spcs = spsItem.get( Tags.ScheduledProtocolCodeSeq );
		if ( spcs != null ) {
			desc = getCodeValues( spcs );
		} else {
			desc = spsItem.getString( Tags.SPSDescription );
		}
		return desc;
	}
	
	private String getCodeValues( DcmElement elem ) {
		int len = elem.countItems();
		Dataset dsCode;
		StringBuffer sb = new StringBuffer();
		for ( int i = 0 ; i < len ; i++ ) {
			dsCode = elem.getItem( i );
			if ( i > 0 ) sb.append("/");
			sb.append( dsCode.getString( Tags.CodeMeaning ) ).append("[");
			sb.append( dsCode.getString( Tags.CodeValue ) ).append("]");
		}
		return sb.toString();
		
	}
	
	public String getReqProcedureDescription() {
		String desc = null;
		DcmElement rpcs = ds.get( Tags.RequestedProcedureCodeSeq );
		if ( rpcs != null ) {
			desc = getCodeValues( rpcs );
		} else {
			desc = ds.getString( Tags.RequestedProcedureDescription );
		}
		return desc;
	}
	
	public String getSpsStatus() {
	    return spsItem.getString(Tags.SPSStatus);
	}

}
