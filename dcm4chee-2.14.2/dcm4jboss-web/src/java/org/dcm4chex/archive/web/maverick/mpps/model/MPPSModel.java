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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.web.maverick.BasicFormPagingModel;
import org.dcm4chex.archive.web.maverick.mpps.MPPSConsoleCtrl;
import org.dcm4chex.archive.web.maverick.mpps.MPPSFilter;

/**
 * @author franz.willer
 *
 * The Model for Modality Performed Procedure Steps WEB interface.
 */
public class MPPSModel extends BasicFormPagingModel {

    /** The session attribute name to store the model in http session. */
    public static final String MPPS_MODEL_ATTR_NAME = "mppsModel";

    /** Errorcode: unsupported action */
    public static final String ERROR_UNSUPPORTED_ACTION = "UNSUPPORTED_ACTION";

    private static final SimpleDateFormat dFormatter = new SimpleDateFormat("yyyy/MM/dd");

    private String[] mppsIDs = null;
    //Holds MPPSEntries with sticky
    Map<String, MPPSEntry> stickyList;

    /** Holds list of MPPSEntries */
    private Map<String, MPPSEntry> mppsEntries = new HashMap<String, MPPSEntry>();

    private MPPSFilter mppsFilter;


    /**
     * Creates the model.
     * <p>
     * Creates the filter instance for this model.
     */
    private MPPSModel(HttpServletRequest request) {
        super(request);
        getFilter();
    }

    /**
     * Get the model for an http request.
     * <p>
     * Look in the session for an associated model via <code>MPPS_MODEL_ATTR_NAME</code><br>
     * If there is no model stored in session (first request) a new model is created and stored in session.
     * 
     * @param request A http request.
     * 
     * @return The model for given request.
     */
    public static final MPPSModel getModel( HttpServletRequest request ) {
        MPPSModel model = (MPPSModel) request.getSession().getAttribute(MPPS_MODEL_ATTR_NAME);
        if (model == null) {
            model = new MPPSModel(request);
            request.getSession().setAttribute(MPPS_MODEL_ATTR_NAME, model);
            model.filterWorkList( true );
        }
        return model;
    }

    public String getModelName() { return "MPPS"; }

    /**
     * Returns the Filter of this model.
     * 
     * @return MPPSFilter instance that hold filter criteria values.
     */
    public MPPSFilter getFilter() {
        if ( mppsFilter == null ) {
            mppsFilter = new MPPSFilter();
        }
        return mppsFilter;
    }

    /**
     * @return Returns the stickies.
     */
    public String[] getMppsIUIDs() {
        return mppsIDs;
    }
    /**
     * @param stickies The stickies to set.
     * @param check
     */
    public void setMppsIUIDs(String[] stickies, boolean check) {
        this.mppsIDs = stickies;
        stickyList = new HashMap();
        if ( mppsEntries.isEmpty() || mppsIDs == null || mppsIDs.length < 1) return;
        MPPSEntry stickyEntry = (MPPSEntry) mppsEntries.get(mppsIDs[0]);
        String patID = stickyEntry.getPatientID(); 
        stickyList.put( mppsIDs[0], stickyEntry );
        for ( int i = 1; i < mppsIDs.length ; i++ ) {
            stickyEntry = (MPPSEntry) mppsEntries.get(mppsIDs[i]);
            if ( check && ! patID.equals( stickyEntry.getPatientID() )) {
                throw new IllegalArgumentException("All selected MPPS must have the same patient!");
            }
            stickyList.put( mppsIDs[i], stickyEntry );
        }
    }
    /**
     * Return a list of MPPSEntries for display.
     * 
     * @return Returns the mppsEntries.
     */
    public Collection getMppsEntries() {
        return mppsEntries.values();
    }

    public MPPSEntry getMppsEntry( String mppsIUID ) {
        return (MPPSEntry) mppsEntries.get(mppsIUID);
    }

    /**
     * Update the list of MPPSEntries for the view.
     * <p>
     * The query use the search criteria values from the filter and use offset and limit for paging.
     * <p>
     * if <code>newSearch is true</code> will reset paging (set <code>offset</code> to 0!)
     * @param newSearch
     */
    public void filterWorkList(boolean newSearch) {
        int total = 0;
        int limit = getLimit();
        int offset = getOffset();
        if ( newSearch ) {
            setOffset(0);
            total = MPPSConsoleCtrl.getMppsDelegate().countMppsEntries(this.mppsFilter);
            setTotal(total);
        }
        List<Dataset> l = MPPSConsoleCtrl.getMppsDelegate().findMppsEntries( this.mppsFilter, offset, limit );
        if ( l == null ) {
            this.setPopupMsg("mpps.err", "findMppsEntries");
            return;
        }
        if ( l.size() == limit ) {
            offset += limit;
        }
        mppsEntries.clear();
        if ( stickyList != null ) {
            mppsEntries.putAll(stickyList);
        }
        MPPSEntry entry;
        int countNull = 0;
        for ( Dataset ds : l ){
            if ( ds != null ) {
                entry = new MPPSEntry( ds );
                mppsEntries.put( entry.getMppsIUID(), entry );
            } else {
                countNull++;
            }
        }
        if (countNull > 0) {
            log.warn("List of MPPS entries contains null entries! countNull:"+countNull); 
        }
    }

    public String getPatientOfSelectedMpps(String mppsIUID) {
        if ( stickyList.isEmpty() ) return null;
        if ( mppsIUID == null ) {
            return ((MPPSEntry) stickyList.values().iterator().next()).getPatientName();
        } else {
            MPPSEntry entry =(MPPSEntry) stickyList.get(mppsIUID);
            return entry == null ? null : entry.getPatientName();
        }
    }
    public String getStartDateOfSelectedMpps(String mppsIUID) {
        if ( stickyList.isEmpty() ) return null;
        String startDate = null;
        if ( mppsIUID == null ) {
            startDate = ((MPPSEntry) stickyList.values().iterator().next()).getPpsStartDateTime();
        } else {
            MPPSEntry entry =(MPPSEntry) stickyList.get(mppsIUID);
            if ( entry != null ) {
                startDate = entry.getPpsStartDateTime();
            }
        }
        if ( startDate != null && startDate.length() > 10) //shrink to yyyy/mm/dd
            startDate = startDate.substring(0,10);
        return startDate;
    }
    public String getModalityOfSelectedMpps(String mppsIUID) {
        if ( stickyList.isEmpty() ) return null;
        if ( mppsIUID == null ) {
            return ((MPPSEntry) stickyList.values().iterator().next()).getModality();
        } else {
            MPPSEntry entry =(MPPSEntry) stickyList.get(mppsIUID);
            return entry == null ? null : entry.getModality();
        }
    }

    /* (non-Javadoc)
     * @see org.dcm4chex.archive.web.maverick.BasicFormPagingModel#gotoCurrentPage()
     */
    public void gotoCurrentPage() {
        filterWorkList(false);
    }

    public void setSelectedStationAetGroups(String[] selected) {
        getFilter().selectStationAetGroupNames(selected);
    }

}
