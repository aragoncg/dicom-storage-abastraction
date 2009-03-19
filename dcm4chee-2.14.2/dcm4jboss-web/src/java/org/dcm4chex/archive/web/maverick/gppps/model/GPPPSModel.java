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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.BasicFormPagingModel;
import org.dcm4chex.archive.web.maverick.gppps.GPPPSConsoleCtrl;
import org.dcm4chex.archive.web.maverick.gppps.PPSFilter;

/**
 * @author franz.willer
 *
 * The Model for Modality Performed Procedure Steps WEB interface.
 */
public class GPPPSModel extends BasicFormPagingModel {

	/** The session attribute name to store the model in http session. */
	public static final String GPPPS_MODEL_ATTR_NAME = "gpppsModel";
	
    /** Errorcode: unsupported action */
	public static final String ERROR_UNSUPPORTED_ACTION = "UNSUPPORTED_ACTION";

	private static final SimpleDateFormat dFormatter = new SimpleDateFormat("yyyy/MM/dd");

	private String[] gpppsIDs = null;
	//Holds GPPPSEntries with sticky
	Map stickyList;
	
	/** Holds list of GPPPSEntries */
	private Map gpppsEntries = new HashMap();

	private PPSFilter ppsFilter;
	

	/** Comparator to sort list of GPPPS datasets. */
	private Comparator comparator = new GpppsDSComparator();

	/**
	 * Creates the model.
	 * <p>
	 * Creates the filter instance for this model.
	 */
	private GPPPSModel(HttpServletRequest request) {
		super(request);
		getFilter();
	}
	
	/**
	 * Get the model for an http request.
	 * <p>
	 * Look in the session for an associated model via <code>GPPPS_MODEL_ATTR_NAME</code><br>
	 * If there is no model stored in session (first request) a new model is created and stored in session.
	 * 
	 * @param request A http request.
	 * 
	 * @return The model for given request.
	 */
	public static final GPPPSModel getModel( HttpServletRequest request ) {
		GPPPSModel model = (GPPPSModel) request.getSession().getAttribute(GPPPS_MODEL_ATTR_NAME);
		if (model == null) {
				model = new GPPPSModel(request);
				request.getSession().setAttribute(GPPPS_MODEL_ATTR_NAME, model);
				model.filterWorkList( true );
		}
		return model;
	}

	public String getModelName() { return "GPPPS"; }
	
	/**
	 * Returns the Filter of this model.
	 * 
	 * @return PPSFilter instance that hold filter criteria values.
	 */
	public PPSFilter getFilter() {
		if ( ppsFilter == null ) {
			ppsFilter = new PPSFilter();
		}
		return ppsFilter;
	}
	
	/**
	 * @return Returns the stickies.
	 */
	public String[] getGpppsIUIDs() {
		return gpppsIDs;
	}
	/**
	 * @param stickies The stickies to set.
	 * @param check
	 */
	public void setGpppsIUIDs(String[] stickies, boolean check) {
		this.gpppsIDs = stickies;
		stickyList = new HashMap();
		if ( gpppsEntries.isEmpty() || gpppsIDs == null || gpppsIDs.length < 1) return;
		GPPPSEntry stickyEntry = (GPPPSEntry) gpppsEntries.get(gpppsIDs[0]);
		String patID = stickyEntry.getPatientID(); 
		stickyList.put( gpppsIDs[0], stickyEntry );
		for ( int i = 1; i < gpppsIDs.length ; i++ ) {
			stickyEntry = (GPPPSEntry) gpppsEntries.get(gpppsIDs[i]);
			if ( check && ! patID.equals( stickyEntry.getPatientID() )) {
				throw new IllegalArgumentException("All selected GPPPS must have the same patient!");
			}
			stickyList.put( gpppsIDs[i], stickyEntry );
		}
	}
	/**
	 * Return a list of GPPPSEntries for display.
	 * 
	 * @return Returns the gpppsEntries.
	 */
	public Collection getGpppsEntries() {
		return gpppsEntries.values();
	}
    
    public GPPPSEntry getGPPPSEntry( String iuid ) {
        return (GPPPSEntry) gpppsEntries.get(iuid);
    }

	/**
	 * Update the list of GPPPSEntries for the view.
	 * <p>
	 * The query use the search criteria values from the filter and use offset and limit for paging.
	 * <p>
	 * if <code>newSearch is true</code> will reset paging (set <code>offset</code> to 0!)
	 * @param newSearch
	 */
	public void filterWorkList(boolean newSearch) {
		
		if ( newSearch ) setOffset(0);
		List l = GPPPSConsoleCtrl.getGPPPSDelegate().findGPPPSEntries( this.ppsFilter );
		Collections.sort( l, comparator );
		int total = l.size();
		int offset = getOffset();
		int limit = getLimit();
		int end;
		if ( offset >= total ) {
			offset = 0;
			setOffset(0);
			end = limit < total ? limit : total;
		} else {
			end = offset + limit;
			if ( end > total ) end = total;
		}
		Dataset ds;
		gpppsEntries.clear();
		if ( stickyList != null ) {
			gpppsEntries.putAll(stickyList);
		}
		int countNull = 0;
		GPPPSEntry entry;
		for ( int i = offset ; i < end ; i++ ){
			ds = (Dataset) l.get( i );
			if ( ds != null ) {
				entry = new GPPPSEntry( ds );
				gpppsEntries.put( entry.getGpppsIUID(), entry );
			} else {
				countNull++;
			}
		}
		setTotal(total - countNull); // the real total (without null entries!)
	}

	/**
	 * Inner class that compares two datasets for sorting Performed Procedure Steps 
	 * according Performed Procedure step start date/time.
	 * 
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public class GpppsDSComparator implements Comparator {

		public GpppsDSComparator() {
			
		}

		/**
		 * Compares the performed procedure step start date and time of two Dataset objects.
		 * <p>
		 * USe PPSStartDate and PPSStartTime to get the date.
		 * <p>
		 * Use the '0' Date (new Date(0l)) if the date is not in the Dataset.
		 * <p>
		 * Compares its two arguments for order. Returns a negative integer, zero, or a positive integer 
		 * as the first argument is less than, equal to, or greater than the second.
		 * <p>
		 * Throws an Exception if one of the arguments is null or not a Dataset object.
		 * 
		 * @param arg0 	First argument
		 * @param arg1	Second argument
		 * 
		 * @return <0 if arg0<arg1, 0 if equal and >0 if arg0>arg1
		 */
		public int compare(Object arg0, Object arg1) {
			Dataset ds1 = (Dataset) arg0;
			Dataset ds2 = (Dataset) arg1;
			Date d1 = _getStartDateAsLong( ds1 );
			return d1.compareTo( _getStartDateAsLong( ds2 ) );
		}

		/**
		 * @param ds1 The dataset
		 * 
		 * @return the date of this PPS Dataset.
		 */
		private Date _getStartDateAsLong(Dataset ds) {
			if ( ds == null ) return new Date( 0l );
			
			Date d = ds.getDateTime( Tags.PPSStartDate, Tags.PPSStartTime );
			if ( d == null ) d = new Date(0l);
			return d;
		}
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
	 */
	public void gotoCurrentPage() {
		filterWorkList(false);
	}
	
}
