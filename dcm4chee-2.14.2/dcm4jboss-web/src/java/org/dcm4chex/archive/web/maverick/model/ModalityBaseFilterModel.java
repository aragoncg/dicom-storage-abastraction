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

package org.dcm4chex.archive.web.maverick.model;

import java.text.ParseException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public abstract class ModalityBaseFilterModel extends BasicFilterModel {
    protected String startDate;
    private Map stationAetGroups = new HashMap();
    private Map selectedStationAetMap = new HashMap();
    private String stationAet;
    private boolean onlyGroups;
    
	public ModalityBaseFilterModel() {
        super();
	}
    
    /**
     * @return Returns the startDate.
     */
    public String getStartDate() {
        return startDate;
    }
    public abstract void setStartDate(String startDate) throws ParseException;
    
    /**
     * Return Map with StationAET and/or group names with selection state.
     * <p/>
     * key: String: name<br/>
     * value: Boolean: Selection state
     * 
     * @return List of 
     */
    public Map getStationAetGroupNames() {
        return selectedStationAetMap;
    }

    /**
     * Set list of Station AET Groups.
     * <p/>
     * key: Group Name<br/>
     * value: Set with Aets of the named group.
     * 
     * @param stationAetGroups
     * @param onlyGroups
     */
    public void setStationAetGroups(Map stationAetGroups, boolean onlyGroups) {
        selectedStationAetMap.clear();
        if ( stationAetGroups == null ) {
            this.stationAetGroups.clear();
        } else {
            this.stationAetGroups = stationAetGroups;
            initStationAetGroupNames(false);
        }
        this.onlyGroups = onlyGroups;
    }

    public boolean isOnlyGroups() {
        return onlyGroups;
    }

    public void setOnlyGroups(boolean onlyGroups) {
        this.onlyGroups = onlyGroups;
    }

    /**
     * Initialize Group names with given selection state.
     * <p/>
     * Use all keys of stationAetGroups as group names.
     * 
     * @param selected
     */
    public void initStationAetGroupNames(boolean selected) {
        Boolean b = new Boolean(selected);
        for ( Iterator iter=stationAetGroups.keySet().iterator() ; iter.hasNext() ;) {
            selectedStationAetMap.put(iter.next(), b);
        }
    }
    
    /**
     * change selection state of all given (known) names as selected.
     * <p/>
     * 
     * @param selected
     */
    public void selectStationAetGroupNames(String[] selected) {
        initStationAetGroupNames(false);
        for ( int i = 0 ; i < selected.length ; i++) {
            if ( selectedStationAetMap.containsKey(selected[i]) ) {
                selectedStationAetMap.put(selected[i], Boolean.TRUE);
            }
        }
        //updateStationAetFilter()
    }
    
    /**
     * @return Returns the stationAET.
     */
    public String getStationAET() {
        return stationAet;
    }
    /**
     * @param aet The stationAET to set.
     */
    protected void setStationAET(Dataset ds, int tag, String aet) {
        stationAet = ( aet == null || aet.trim().length() < 1 ) ? null : aet;
        updateStationAetFilter(ds, tag, true);
    }
    
    private void updateStationAetFilter(Dataset ds, int tag, boolean addSelected) {
        Set newStationAets = getGroupResolvedList( stationAet, ',');
        if ( addSelected ) {
            Map.Entry entry;
            for ( Iterator iter = selectedStationAetMap.entrySet().iterator() ; iter.hasNext() ; ) {
                entry = (Map.Entry) iter.next();
                
                if ( Boolean.TRUE.equals(entry.getValue()) ) {
                    addItemOrGroupItems(newStationAets, (String) entry.getKey() );
                }
            }
        }
        if ( newStationAets.isEmpty() ) {
            if ( onlyGroups && !stationAetGroups.isEmpty() ) {
                initStationAetGroupNames(true);
                updateStationAetFilter(ds, tag, true);
            } else {
                ds.putAE( tag );
            }
        } else {
            ds.putAE( tag, (String[]) newStationAets.toArray(new String[newStationAets.size()]));
        }
    }

    
    private Set getGroupResolvedList(String aet, char delim) {
        Set newStationAets = new HashSet();
        if ( aet != null ) {
            int start = 0, end = aet.indexOf(delim);
            while ( end != -1) {
               addItemOrGroupItems(newStationAets, aet.substring(start,end));
               start = ++end;
               end = start == aet.length() ? -1 : aet.indexOf(delim, start);
            }
            if ( start != aet.length() ) {
                addItemOrGroupItems(newStationAets, aet.substring(start));
            }
        }
        return newStationAets;
    }

    private void addItemOrGroupItems(Set newStationAets, String item) {
       Collection groupedItems = (Collection) stationAetGroups.get(item);
       if ( groupedItems == null ) {
           if ( onlyGroups ) {
               throw new IllegalArgumentException("Only named groups are allowed for current user!");
           }
           newStationAets.add(item);
       } else {
           for ( Iterator iter = groupedItems.iterator() ; iter.hasNext() ; ) {
               newStationAets.add(iter.next());
           }
       }
    }
    
}
