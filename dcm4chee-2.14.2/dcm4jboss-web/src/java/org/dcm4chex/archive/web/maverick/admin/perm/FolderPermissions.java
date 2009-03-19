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
package org.dcm4chex.archive.web.maverick.admin.perm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3391 $ $Date: 2007-06-19 16:08:59 +0200 (Tue, 19 Jun 2007) $
 * @since 13.04.2006
 */
public class FolderPermissions {
    
    public static final String AEFILTER = "aefilter";
    public static final String STATION_AET_GROUP_LIST = "stationAetGroupList";
    public static final String STATION_AET_GROUP = "stationAetGroup";
    public static final String STATION_AET_FILTER = "stationAetFilter";

    private Map allPermissions = new HashMap();
	
	/**
	 * Returns the list of permissions for given application
	 * <p>
	 *  This list
	 * @param app
	 * @return
	 */
	public Set getPermissionsForApp(String app) {
		return (Set) allPermissions.get(app);
	}
	
	/**
	 * Returns the list of allowed methods for given application.
	 * 
	 * @param app
	 * @return
	 */
	public Set getMethodsForApp( String app){
		Set set = getPermissionsForApp(app);
		if ( set == null ) return null;
		Set methods = new HashSet();
		app = app+".";
		int cutLen = app.length();
		int i = 0 ;
		String perm;
		for ( Iterator iter = set.iterator() ; iter.hasNext() ; i++ ) {
			perm = (String) iter.next();
			if ( perm.startsWith(app)) {
				methods.add(perm.substring(cutLen)); //cut off app name (xxx.)
			}
		}
		return methods;
		
	}
	/**
	 * @param string
	 * @param string2
	 */
	public void addPermissions(String app, String[] methods) {
		Set allowed = (Set) allPermissions.get(app);
		if ( allowed == null ) {
			allowed = new HashSet();
			allPermissions.put(app,allowed);
		}
		if ( methods != null ) {
			for ( int j=0 ; j < methods.length ; j++ ) {
				allowed.add(app+"."+methods[j]);
			}
		}
	}
	
	public int getNumberOfPrivilegedApps() {
		return allPermissions.size();
	}
	
	public String toString() {
		return "FolderPermissions:"+allPermissions;
	}
}
