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

package org.dcm4chex.archive.web.maverick.mcmc.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import org.dcm4chex.archive.ejb.interfaces.MediaDTO;

/**
 * @author franz.willer
 *
 * The Filter for searching media.
 */
public class MCMFilter {

	/** Identifier for searching not in a time range. */
	public static final String DATE_FILTER_ALL = "all";
	/** Identifier for searching within a creation time range. */
	public static final String CREATED_FILTER = "create";
	/** Identifier for searching within a update time range. */
	public static final String UPDATED_FILTER = "update";
	/** Identifier for searching all media stati. */
	public static final String MEDIA_TYPE_ALL = "-all-";
	
	public static final int[] DEFAULT_MEDIA_STATI = new int[]{ MediaDTO.OPEN, MediaDTO.ERROR }; 

	/** The Date/Time formatter to parse input field values. (dd.MM.yyyy) */
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
	/** Collection with all defined media stati. (defined in MediaData) */ 
	private Collection mediaStatusList = null;
	/** holds the 'left' string value of creation time range. */
	private String startDate = "";
	/** holds the 'right' string value of creation time range. */
	private String endDate = "";
	/** holds the 'left' Long value of creation time range. (null if string value is empty) */
	private Long startDateAsLong;
	/** holds the 'right' Long value of creation time range. (null if string value is empty) */
	private Long endDateAsLong;
	/** holds the selected stati for this filter. <code>null</code> means all! */
	private int[] selectedStati = null;
	/** holds the selected stati for this filter in a string. Therefore all selected stati are concat seperated with '|' or -all-(MEDIA_TYPE_ALL) */
	private String selectedStatiAsString = null;
	/** holds the switch between search of 'all', 'created' or 'updated' time range. */ 
	private String createOrUpdateDate = "all";
	/** Change status of this filter. */
	private boolean isChanged;
	/** holds sort order of this filter. */
	private boolean descent = true;
	
	/**
	 * Creates a new Filer for media search.
	 * <p>
	 * Set the Collection of defined media stati.
	 */
	public MCMFilter() { 
		this.setDefaultSelectedStati();
		mediaStatusList = MediaData.DEFINED_MEDIA_STATI;//List of all (in MediaData) defined media stati.
	}
	
	/**
	 * Returns the collection of defined media stati.
	 * 
	 * @return all defined media stati.
	 */
	public Collection getMediaStatusList() {
		return mediaStatusList;
	}
	/**
	 * @return Returns the endCreationDate.
	 */
	public String getEndDate() {
		return endDate;
	}
	/**
	 * Set the end creation date.
	 * <p>
	 * Set both <code>endCreationDate and endCreationAsLong</code>.<br>
	 * If the parameter is null or empty, both values are set to <code>null</code>
	 * 
	 * @param endDate The endCreatenDate to set.
	 * 
	 * @throws ParseException If param is not a date/time string of format specified in formatter.
	 */
	public void setEndDate(String endDate) throws ParseException {
		if ( ! check( this.endDate, endDate ) ) return;
		if ( endDate == null || endDate.trim().length() < 1 ) {
			this.endDateAsLong = null;
			this.endDate = null;
		} else {
			this.endDateAsLong = new Long( formatter.parse( endDate ).getTime() );
			this.endDate = endDate;
		}
	}
	/**
	 * @return Returns the selectedStati.
	 */
	public int[] selectedStati() {
		return selectedStati;
	}
	/**
	 * @return Returns the selectedStati as String.
	 */
	public String getSelectedStatiAsString() {
		return selectedStatiAsString;
	}
	/**
	 * @param stati The selectedStatus to set.
	 */
	public void setSelectedStati(String[] stati) {
		if ( stati == null || stati.length < 1 ) {
			setDefaultSelectedStati();
			return;
		}
		int len = stati.length;
		int[] ia = new int[ len ];
		StringBuffer sb = new StringBuffer();
		for ( int i = 0 ; i < len ; i++ ) { 
			if ( MEDIA_TYPE_ALL.equals( stati[i]) ) { //if -all- selected set selectedStati to null!
				ia = null;
				sb.setLength(0);//make sure sb is empty!
				sb.append( MEDIA_TYPE_ALL );
				break;
			}
			ia[i] = Integer.parseInt( stati[i] );
			sb.append(' ').append(stati[i]).append('|');//' ' needed because in xslt this will prevent matching 1 with -1.
		}
		check( this.selectedStati, ia );
		this.selectedStatiAsString = sb.toString();
		this.selectedStati = ia;
	}
	
	public void setDefaultSelectedStati() {
		selectedStati = DEFAULT_MEDIA_STATI;
		if ( selectedStati != null ) {
			selectedStatiAsString = String.valueOf( MediaDTO.OPEN );
			StringBuffer sb = new StringBuffer();
			for ( int i = 0, len = selectedStati.length ; i < len ; i++ ) { 
				sb.append(' ').append(selectedStati[i]).append('|');//' ' needed because in xslt this will prevent matching 1 with -1.
			}
			selectedStatiAsString = sb.toString();
		} else {
			selectedStatiAsString = MCMFilter.MEDIA_TYPE_ALL;
		}
	}

	/**
	 * @return Returns the startCreationDate.
	 */
	public String getStartDate() {
		return startDate;
	}
	/**
	 * Set the start creation date.
	 * <p>
	 * Set both <code>startCreationDate and startCreationAsLong</code>.<br>
	 * If the parameter is null or empty, both values are set to <code>null</code>
	 * 
	 * @param startCreationDate The startCreationDate to set.
	 * @throws ParseException
	 */
	public void setStartDate(String startCreationDate) throws ParseException {
		check( this.startDate, startCreationDate );
		this.startDate = startCreationDate;

		if ( ! check( this.startDate, startCreationDate ) ) return;
		if ( startCreationDate == null || startCreationDate.trim().length() < 1 ) {
			this.startDateAsLong = null;
			this.startDate = null;
		} else {
			this.startDateAsLong = new Long( formatter.parse( startCreationDate ).getTime() );
			this.startDate = startCreationDate;
		}
	
	}
	/**
	 * @return Returns the createOrUpdateDate.
	 */
	public String getCreateOrUpdateDate() {
		return createOrUpdateDate;
	}
	/**
	 * @param createOrUpdateDate The createOrUpdateDate to set.
	 */
	public void setCreateOrUpdateDate(String createOrUpdateDate) {

		check( this.createOrUpdateDate, createOrUpdateDate );
		
		this.createOrUpdateDate = createOrUpdateDate;
	}
	
	/**
	 * Return sort order flag.
	 * 
	 * @return true for descending, false for ascending sort order
	 */
	public boolean isDescent() {
		return descent ;
	}
	
	/**
	 * Set the sort order.
	 * <p>
	 * true for descending, false for ascending.
	 * 
	 * @param desc.
	 */
	public void setDescent( boolean desc ) {
		isChanged = isChanged || ( desc ^ descent );
		descent = desc;
	}

	/**
	 * Set isChecked if params are not equal.
	 * <p>
	 * Used to check if this filter has changed.
	 * 
	 * @param o1 first param
	 * @param o2 second param
	 * 
	 * @return the current isChanged value;
	 */
	private boolean check(Object o1, Object o2 ) {
		if ( o1 == null ) {
			isChanged = isChanged || (o2 != null);
		} else {
			isChanged = isChanged || ( !o1.equals( o2 ));
		}
		return isChanged;
	}
	
	/**
	 * @return Returns the endCreationAsLong.
	 */
	public Long endDateAsLong() {
		return endDateAsLong;
	}
	/**
	 * @return Returns the startCreationAsLong.
	 */
	public Long startDateAsLong() {
		return startDateAsLong;
	}

	/**
	 * Returns the changed status of this filter.
	 * <p>
	 * Set the current changed state to false! 
	 * So further calls return always false until filter is changed.
	 * 
	 * @return true if this filter has been changed.
	 */
	public boolean isChanged() {
		if ( isChanged ) {
			isChanged = false;
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Return a short description of this filter.
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("MCMFilter: mediaStatus:").append(selectedStati);
		sb.append(" createOrUpdateDate:").append(createOrUpdateDate);
		sb.append(" searchDate:").append(startDate).append(" - ").append(endDate);
		sb.append(" Descent:").append(isDescent());
		sb.append(" changed:").append(isChanged);
		return sb.toString();
	}
}
