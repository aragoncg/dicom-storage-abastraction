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

package org.dcm4chex.archive.web.maverick.gppps.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
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
public class GPPPSEntry {

	private Dataset ds;
	private List refRequests;
	private List gpspsList;
	private List results;
	
	private static Logger log = Logger.getLogger( GPPPSEntry.class.getName() );
	
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat dtformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	/** The Date/Time formatter to format date/time values. */
	private static final SimpleDateFormat shortDTformatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
	/** The Date formatter to format date values. */
	private static final SimpleDateFormat dformatter = new SimpleDateFormat("yyyy/MM/dd");
	
	private GPPPSEntry() { //for dummy
		
	}
	
	public GPPPSEntry( Dataset ds ) {
		this.ds = ds;
		initRefReqSq( ds );
		initRefGPSPS( ds );
	}
    
    public Dataset toDataset() {
        return ds;
    }
	
	public boolean equals(Object o) {
		if ( o != null && (o instanceof GPPPSEntry )) {
			return this.getGpppsIUID().equals( ((GPPPSEntry) o).getGpppsIUID());
		} 
		return false;
	}
	
	public int hashCode() {
		return getGpppsIUID().hashCode();
	}
	
	private void initRefReqSq( Dataset ds ) {
		refRequests = new ArrayList();
		DcmElement e = ds.get( Tags.RefRequestSeq );
		if ( e != null ) {
			for ( int i=0, len=e.countItems() ; i < len ; i++ ) {
				refRequests.add( new RefRequest( e.getItem(i)));
			}
		}
	}

	private void initRefGPSPS( Dataset ds ) {
		gpspsList = new ArrayList();
		DcmElement e = ds.get( Tags.RefGPSPSSeq );
		if ( e != null ) {
			for ( int i=0, len=e.countItems() ; i < len ; i++ ) {
				gpspsList.add( new GPSPS( e.getItem(i)));
			}
		}
		
	}

	private void initResults( Dataset ds ) {
		results = new ArrayList();
		DcmElement e = ds.get( Tags.OutputInformationSeq );
		if ( e != null ) {
			for ( int i=0, len=e.countItems() ; i < len ; i++ ) {
				results.addAll( getRefIUIDs( e.getItem(i)));
			}
		}
	}
	
	private List getRefIUIDs(Dataset item) {
		List l = new ArrayList();
		DcmElement series = ds.get( Tags.RefSeriesSeq );
		DcmElement instances;
		if ( series != null ) {
			for ( int i=0, len=series.countItems() ; i < len ; i++ ) {
				instances = series.getItem(i).get(Tags.RefSOPSeq);
				if ( instances != null ) {
					for ( int j = 0, len1=instances.countItems() ; j < len1 ; j++ ) {
						l.add( instances.getItem(j).getString(Tags.RefSOPInstanceUID));
					}
				}
			}
		}
		return l;
	}
	
	public String getGpppsIUID() {
		return ds.getString( Tags.SOPInstanceUID );
	}
	
	/**
	 * @return Returns the performed Procedure Step ID.
	 */
	public String getPPSID() {
		return ds.getString( Tags.PPSID );
	}
	
	public String getPPSStatus() {
		return ds.getString( Tags.GPPPSStatus );
	}

	public String getPPSComment() {
		return ds.getString( Tags.PPSComments );
	}
	
	public String getPPSDescription() {
		return ds.getString( Tags.PPSDescription );
	}
	
	public String getPerformedWorkitemCode() {
		return getCodeValue( ds.get(Tags.PerformedWorkitemCodeSeq));
	}

	public String getPPSTypeDescription() {
		String desc = null;
		DcmElement ppcs = ds.get( Tags.PerformedProcedureCodeSeq );
		if ( ppcs != null ) {
			desc = getCodeValue( ppcs );
		} else {
			desc = ds.getString( Tags.PerformedProcedureTypeDescription );
		}
		return desc;
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

	public String[] getActualHumanPerformers() {
		DcmElement elem = ds.get(Tags.ActualHumanPerformersSeq);
		if ( elem != null ) {
			String[] performers = new String[elem.countItems()];
			for ( int i = 0 ; i < elem.countItems() ; i++) {
				performers[i] = elem.getItem(i).getItem(Tags.HumanPerformerCodeSeq).getString(Tags.CodeMeaning);
			}
			return performers;
		}
		return null;
	}
	
	/**
	 * @return Returns the stationName.
	 */
	public String getStationName() {
		return getCodeValue( ds.get( Tags.PerformedStationNameCodeSeq ) );
	}
	public String getStationClass() {
		return getCodeValue( ds.get( Tags.PerformedStationClassCodeSeq ) );
	}
	public String getStationGeoLocation() {
		return getCodeValue( ds.get( Tags.PerformedStationGeographicLocationCodeSeq ) );
	}
	public String[] getProcessingApplications() {
		return getCodeValues( ds.get( Tags.PerformedProcessingApplicationsCodeSeq ) );
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
	

	public List getRefRequests() {
		return refRequests;
	}
	
	public List getRefGPSPS() {
		return gpspsList;
	}
	
	public String[] getReqSubsequentWorkitemCodes() {
		return getCodeValues( ds.get(Tags.RequestedSubsequentWorkitemCodeSeq));
	}
	public String[] getNonDICOMOutputCodes() {
		return getCodeValues( ds.get(Tags.NonDICOMOutputCodeSeq));
	}
	
	public List getResultIUIDs() {
		return results;
	}

	
	private String getCodeValue( DcmElement elem ) {
		if ( elem == null || elem.countItems() < 1) return null;
		StringBuffer sb = new StringBuffer();
		Dataset dsCode = elem.getItem();
		sb.append( dsCode.getString( Tags.CodeMeaning ) ).append("[");
		sb.append( dsCode.getString( Tags.CodeValue ) ).append("]");
		return sb.toString();
		
	}
	private String[] getCodeValues( DcmElement elem ) {
		if ( elem == null || elem.countItems() < 1 ) return null;
		int len = elem.countItems();
		Dataset dsCode;
		StringBuffer sb = new StringBuffer();
		String[] sa = new String[len];
		for ( int i = 0 ; i < len ; i++ ) {
			dsCode = elem.getItem( i );
			sb.setLength(0);
			sb.append( dsCode.getString( Tags.CodeMeaning ) ).append("[");
			sb.append( dsCode.getString( Tags.CodeValue ) ).append("]");
			sa[i] = sb.toString();
		}
		return sa;
		
	}
	
	
	
	/**
	 * Inner class to hold Scheduled Step Attributes Sequence.
	 * <p>
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public class RefRequest {
		Dataset dsRefReq;
		
		public RefRequest( Dataset ds ) {
			dsRefReq = ds;
		}
		
		public String getStudyUID() {
			return dsRefReq.getString( Tags.StudyInstanceUID );
		}
		
		public String getReferencedStudy() {
			DcmElement e = dsRefReq.get( Tags.RefStudySeq );
			if ( e != null ) {
				Dataset item = e.getItem();
				if ( item != null ) {
					return item.getString( Tags.RefSOPClassUID)+"["+item.getString( Tags.RefSOPInstanceUID)+"]";
				}
			}
			return null;		
		}
		
		public String getAccessionNumber() {
			return dsRefReq.getString( Tags.AccessionNumber );
		}
		public String getPlacerOrderNumber() {
			return dsRefReq.getString( Tags.PlacerOrderNumber );
		}
		public String getFillerOrderNumber() {
			return dsRefReq.getString( Tags.FillerOrderNumber );
		}

		public String getReqProcedureDescription() {
			String desc = null;
			DcmElement rpcs = dsRefReq.get( Tags.RequestedProcedureCodeSeq );
			if ( rpcs != null ) {
				desc = getCodeValue( rpcs );
			} else {
				desc = dsRefReq.getString( Tags.RequestedProcedureDescription );
			}
			return desc;
		}
		
		/**
		 * @return Returns the reqProcedureID.
		 */
		public String getReqProcedureID() {
			return dsRefReq.getString( Tags.RequestedProcedureID );
		}

		public String getSpsID() {
			return dsRefReq.getString( Tags.SPSID );
		}

		public String getSPSDescription() {
			String desc = null;
			DcmElement spcs = dsRefReq.get( Tags.ScheduledProtocolCodeSeq );
			if ( spcs != null ) {
				desc = getCodeValue( spcs );
			} else {
				desc = dsRefReq.getString( Tags.SPSDescription );
			}
			return desc;
		}
		
	}
	
	public class GPSPS {
		private Dataset dsGPSPS;
		private int noi;
		
		public GPSPS( Dataset ds ) {
			dsGPSPS = ds;
		}
		
		public String getRefSOPClassUID() {
			return dsGPSPS.getString( Tags.RefSOPClassUID );
		}

		public String getRefSOPInstanceUID() {
			return dsGPSPS.getString( Tags.RefSOPInstanceUID );
		}

		public String getRefGPSPSTransactionUID() {
			return dsGPSPS.getString( Tags.RefGPSPSTransactionUID );
		}
	
	}
	
	public class DummyGPPPSEntry extends GPPPSEntry{
		private String uid;
		public DummyGPPPSEntry(String gpppsIUID) {
			uid = gpppsIUID;
		}
		
		public String getGpppsIUID() { return uid; }
		
		public int hashCode() { return uid.hashCode(); }
	}
}
