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
import org.dcm4chex.archive.web.maverick.gpwl.GPWLConsoleCtrl;

/**
 * @author franz.willer
 *
 * The Model for Media Creation Managment WEB interface.
 */
public class GPWLModel extends BasicFormPagingModel {

	/** The session attribute name to store the model in http session. */
	public static final String GPWLMODEL_ATTR_NAME = "gpwlModel";
	
    /** Errorcode: unsupported action */
	public static final String ERROR_UNSUPPORTED_ACTION = "UNSUPPORTED_ACTION";
	
	/** Holds list of GPWLEntries */
	private Map gpwlEntries = new HashMap();

	private GPWLFilter gpwlFilter;
	
	/** True if GPWLScpAET is 'local' to allow deletion */
	private boolean isLocal = false;

	/** Comparator to sort list of SPS datasets. */
	private Comparator comparator = new SpsDSComparator();

	/**
	 * Creates the model.
	 * <p>
	 * Creates the filter instance for this model.
	 */
	private GPWLModel(  HttpServletRequest request ) {
		super(request);
		getFilter();
	}
	
	/**
	 * Get the model for an http request.
	 * <p>
	 * Look in the session for an associated model via <code>GPWLMODEL_ATTR_NAME</code><br>
	 * If there is no model stored in session (first request) a new model is created and stored in session.
	 * 
	 * @param request A http request.
	 * 
	 * @return The model for given request.
	 */
	public static final GPWLModel getModel( HttpServletRequest request ) {
		GPWLModel model = (GPWLModel) request.getSession().getAttribute(GPWLMODEL_ATTR_NAME);
		if (model == null) {
				model = new GPWLModel(request);
				request.getSession().setAttribute(GPWLMODEL_ATTR_NAME, model);
				model.filterWorkList( true );
		}
		return model;
	}

	public String getModelName() { return "GPWL"; }
	
	/**
	 * Returns the Filter of this model.
	 * 
	 * @return GPWLFilter instance that hold filter criteria values.
	 */
	public GPWLFilter getFilter() {
		if ( gpwlFilter == null ) {
			gpwlFilter = new GPWLFilter();
		}
		return gpwlFilter;
	}
	
	/**
	 * Return a list of GPWLEntries for display.
	 * 
	 * @return Returns the GPWLEntries.
	 */
	public Collection getGPWLEntries() {
		return gpwlEntries.values();
	}
	
	public GPWLEntry getGPWLEntry(String spsID) {
		return (GPWLEntry) gpwlEntries.get(spsID);
	}
	/**
	 * Returns true if the GPWLScpAET is 'local'.
	 * <p>
	 * <DL>
	 * <DT>If it is local:</DT>
	 * <DD>  1) Entries can be deleted. (shows a button in view)</DD>
	 * <DD>  2) The query for the working list is done directly without a CFIND.</DD>
	 * </DL>
	 * @return Returns the isLocal.
	 */
	public boolean isLocal() {
		return isLocal;
	}

	/**
	 * Update the list of GPWLEntries for the view.
	 * <p>
	 * The query use the search criteria values from the filter and use offset and limit for paging.
	 * <p>
	 * if <code>newSearch is true</code> will reset paging (set <code>offset</code> to 0!)
	 * @param newSearch
	 */
	public void filterWorkList(boolean newSearch) {
		
		if ( newSearch ) setOffset(0);
		Dataset searchDS = gpwlFilter.toDataset();
		isLocal = GPWLConsoleCtrl.getGPWLScuDelegate().isLocal();
		List l = GPWLConsoleCtrl.getGPWLScuDelegate().findGPWLEntries( searchDS );
		Collections.sort( l, comparator );
		int total = l.size();
		int offset = getOffset();
		int limit = getLimit();
		int end;
		if ( offset >= total ) {
			offset = 0;
			end = limit < total ? limit : total;
		} else {
			end = offset + limit;
			if ( end > total ) end = total;
		}
		Dataset ds;
		gpwlEntries.clear();
		int countNull = 0;
		GPWLEntry entry;
		for ( int i = offset ; i < end ; i++ ){
			ds = (Dataset) l.get( i );
			if ( ds != null ) {
				entry = new GPWLEntry( ds );
				gpwlEntries.put( entry.getIUID(), entry );
			} else {
				countNull++;
			}
		}
		setTotal(total - countNull); // the real total (without null entries!)	
	}

	/**
	 * Inner class that compares two datasets for sorting Scheduled Procedure Steps 
	 * according scheduled Procedure step start date.
	 * 
	 * @author franz.willer
	 *
	 * TODO To change the template for this generated type comment go to
	 * Window - Preferences - Java - Code Style - Code Templates
	 */
	public class SpsDSComparator implements Comparator {

		private final Date DATE_0 = new Date(0l);
		public SpsDSComparator() {
			
		}

		/**
		 * Compares the scheduled procedure step start date and time of two Dataset objects.
		 * <p>
		 * USe either SPSStartDateAndTime or SPSStartDate and SPSStartTime to get the date.
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
		 * @return the date of this SPS Dataset.
		 */
		private Date _getStartDateAsLong(Dataset ds) {
			if ( ds == null ) return DATE_0;
			Date d = ds.getDate( Tags.SPSStartDateAndTime );
			if ( d == null ) d = DATE_0;
			return d;
		}
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
	 */
	public void gotoCurrentPage() {
		filterWorkList( false );
	}
	
}
