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

package org.dcm4chex.archive.web.maverick.mpps.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
public class MPPSEntry {

	private Dataset ds;
	private List ssAttrs;
	private List series;
	private int numberOfInstances = 0;
	
	private List accNumbers = new ArrayList();
	private List studyUIDs = new ArrayList();

	
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat dtformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat shortDTformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	/** The Date formatter to format date values. */
	private static final SimpleDateFormat dformatter = new SimpleDateFormat("yyyy/MM/dd");
	
	private MPPSEntry() { //for dummy
		
	}
	
	public MPPSEntry( Dataset ds ) {
		this.ds = ds;
		initSSA( ds );
		initPSeries( ds );
	}
	
	public boolean equals(Object o) {
		if ( o != null && (o instanceof MPPSEntry )) {
			return this.getMppsIUID().equals( ((MPPSEntry) o).getMppsIUID());
		} 
		return false;
	}
	
	public int hashCode() {
		return getMppsIUID().hashCode();
	}
	
    public Dataset toDataset() {
        return ds;
    }
	private void initSSA( Dataset ds ) {
		ssAttrs = new ArrayList();
		DcmElement e = ds.get( Tags.ScheduledStepAttributesSeq );
		if ( e != null ) {
			for ( int i=0, len=e.countItems() ; i < len ; i++ ) {
				ssAttrs.add( new SSAttr( e.getItem(i)));
			}
		}
		
	}

	private void initPSeries( Dataset ds ) {
		series = new ArrayList();
		DcmElement e = ds.get( Tags.PerformedSeriesSeq );
		if ( e != null ) {
			for ( int i=0, len=e.countItems() ; i < len ; i++ ) {
				series.add( new PSeries( e.getItem(i)));
			}
		}
		
	}
	
	public String getMppsIUID() {
		return ds.getString( Tags.SOPInstanceUID );
	}
	
	/**
	 * @return Returns the performed Procedure Step ID.
	 */
	public String getPPSID() {
		return ds.getString( Tags.PPSID );
	}
	
	public String getPPSStatus() {
		return ds.getString( Tags.PPSStatus );
	}

	public String getPPSComment() {
		return ds.getString( Tags.PPSComments );
	}
	
	public String getPPSDescription() {
		return ds.getString( Tags.PPSDescription );
	}

	public String getPPSTypeDescription() {
		String desc = null;
		DcmElement ppcs = ds.get( Tags.PerformedProcedureCodeSeq );
		if ( ppcs != null ) {
			desc = getCodeValues( ppcs );
		} else {
			desc = ds.getString( Tags.PerformedProcedureTypeDescription );
		}
		return desc;
	}
	
	public String getDRCode() {
		return getCodeValues( ds.get( Tags.PPSDiscontinuationReasonCodeSeq ));
	}
	
	/**
	 * @return Returns the modality.
	 */
	public String getModality() {
		return ds.getString( Tags.Modality );
	}

	/**
	 * @return Returns the performed procedur step Start date/time.
	 */
	public String getPpsStartDateTime() {
		Date d = ds.getDateTime( Tags.PPSStartDate, Tags.PPSStartTime );
		if ( d == null ) return "";
		
		return shortDTformatter.format( d );
	}

	/**
	 * @return Returns the performed procedur step end date/time.
	 */
	public String getPpsEndDateTime() {
		Date d = ds.getDateTime( Tags.PPSEndDate, Tags.PPSEndTime );
		if ( d == null ) return "";
		
		return shortDTformatter.format( d );
	}
	
	/**
	 * @return Returns the stationAET.
	 */
	public String getStationAET() {
		return ds.getString( Tags.PerformedStationAET );
	}
	/**
	 * @return Returns the stationName.
	 */
	public String getStationName() {
		return ds.getString( Tags.PerformedStationName );
	}
	
	public String getPatientName() {
		return ds.getString( Tags.PatientName );
	}
	public String getPatientID() {
		return ds.getString( Tags.PatientID );
	}
	public String getPatientBirthDate() {
		Date d = ds.getDate( Tags.PatientBirthDate );
		if ( d == null ) return "";
		return dformatter.format( d );
	}
	
	public String getPatientSex() {
		return ds.getString( Tags.PatientSex );
	}
	
	public String getStudyUIDs() {
		return getListString( studyUIDs );
	}
	
	public String getAccNumbers() {
		return getListString( accNumbers );
	}

	public List getScheduledStepAttrs() {
		return ssAttrs;
	}
	
	public List getPerformedSeries() {
		return series;
	}

	
	private String getListString( List l ) {
		if ( l.isEmpty() ) return null;
		StringBuffer sb = new StringBuffer();
		sb.append( l.get(0));
		for ( int i = 1, len = l.size() ; i < len ; i++ ) {
			sb.append("\\ ").append( l.get(i) );
		}
		return sb.toString();
	}
	
	/**
	 * @return Returns the numberOfInstances.
	 */
	public int getNumberOfInstances() {
		return numberOfInstances;
	}
	private String getCodeValues( DcmElement elem ) {
		if ( elem == null) return null;
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
	
	
	
	/**
	 * Inner class to hold Scheduled Step Attributes Sequence.
	 * <p>
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public class SSAttr {
		Dataset dsSSA;
		
		public SSAttr( Dataset ds ) {
			dsSSA = ds;
			String studyUID = getStudyUID();
			if ( studyUID != null ) {
				if ( ! studyUIDs.contains( studyUID ) ) studyUIDs.add( studyUID );
			}
			String accNo = getAccessionNumber();
			if ( accNo != null ) {
				if ( ! accNumbers.contains( accNo ) ) accNumbers.add( accNo );
			}
		}
		
		public String getStudyUID() {
			return dsSSA.getString( Tags.StudyInstanceUID );
		}
		
		public String getReferencedStudy() {
			DcmElement e = dsSSA.get( Tags.RefStudySeq );
			if ( e != null ) {
				Dataset item = e.getItem();
				if ( item != null ) {
					return item.getString( Tags.RefSOPClassUID)+"["+item.getString( Tags.RefSOPInstanceUID)+"]";
				}
			}
			return null;		
		}
		
		public String getAccessionNumber() {
			return dsSSA.getString( Tags.AccessionNumber );
		}
		public String getPlacerOrderNumber() {
			return dsSSA.getString( Tags.PlacerOrderNumber );
		}
		public String getFillerOrderNumber() {
			return dsSSA.getString( Tags.FillerOrderNumber );
		}

		public String getReqProcedureDescription() {
			String desc = null;
			DcmElement rpcs = dsSSA.get( Tags.RequestedProcedureCodeSeq );
			if ( rpcs != null ) {
				desc = getCodeValues( rpcs );
			} else {
				desc = dsSSA.getString( Tags.RequestedProcedureDescription );
			}
			return desc;
		}
		
		/**
		 * @return Returns the reqProcedureID.
		 */
		public String getReqProcedureID() {
			return dsSSA.getString( Tags.RequestedProcedureID );
		}

		public String getSpsID() {
			return dsSSA.getString( Tags.SPSID );
		}

		public String getSPSDescription() {
			String desc = null;
			DcmElement spcs = dsSSA.get( Tags.ScheduledProtocolCodeSeq );
			if ( spcs != null ) {
				desc = getCodeValues( spcs );
			} else {
				desc = dsSSA.getString( Tags.SPSDescription );
			}
			return desc;
		}
		
	}
	
	public class PSeries {
		private Dataset dsPS;
		private int noi;
		
		public PSeries( Dataset ds ) {
			dsPS = ds;
			calcNoI();
		}
		
		/**
		 * 
		 */
		private void calcNoI() {
			noi = 0;
			DcmElement elem = dsPS.get( Tags.RefImageSeq );
			if ( elem != null && elem.countItems() > 0 ) noi += elem.countItems();
			elem = dsPS.get( Tags.RefNonImageCompositeSOPInstanceSeq );
			if ( elem != null && elem.countItems() > 0 ) noi += elem.countItems();
			numberOfInstances += noi;
		}

		public String getSeriesUID() {
			return dsPS.getString( Tags.SeriesInstanceUID );
		}

		public String getSeriesDescr() {
			return dsPS.getString( Tags.SeriesDescription );
		}

		public String getOperator() {
			return dsPS.getString( Tags.OperatorName );
		}
	
		public String getPerformingPhysician() {
			return dsPS.getString( Tags.PerformingPhysicianName );
		}
		
		public int getNumberOfInstances() {
			return numberOfInstances;
		}
		
	}
	
	public class DummyMPPSEntry extends MPPSEntry{
		private String uid;
		public DummyMPPSEntry(String mppsIUID) {
			uid = mppsIUID;
		}
		
		public String getMppsIUID() { return uid; }
		
		public int hashCode() { return uid.hashCode(); }
	}
}
