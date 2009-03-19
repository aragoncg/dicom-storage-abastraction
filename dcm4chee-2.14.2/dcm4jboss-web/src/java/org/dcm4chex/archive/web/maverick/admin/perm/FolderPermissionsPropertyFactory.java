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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;

import org.apache.log4j.Logger;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 5697 $ $Date: 2008-01-16 12:10:22 +0100 (Wed, 16 Jan 2008) $
 * @since 13.04.2006
 */
public class FolderPermissionsPropertyFactory extends FolderPermissionsFactory {
    private static final String RAW_PROPERTIES = "RAW_PROPERTIES";
    private static final String SUBJECT_CONTEXT_KEY = "javax.security.auth.Subject.container";
	
    private Map mapPermissions = new HashMap();
	
    private static Logger log = Logger.getLogger(FolderPermissionsPropertyFactory.class.getName());
	
    public void init() {
	try {
	    if ( getInitParameter() == null) {
		log.warn("No initString set. Use default");
		setInitParameter( "conf/dcm4chee-web/folder.permissions" );
	    }
	    Properties props = new Properties();
	    File f = FileUtils.resolve(new File(getInitParameter()));
	    log.info("read permission properties from file: "+f);
	    props.load(new FileInputStream(f));
	    initMap(props);
	} catch ( Exception x ) {
	    log.error("Cant initialize FolderPermissionsPropertyFactory!",x);
	}
    }
	
    /**
     * @param props
     * @throws IOException
     */
    private void initMap(Properties props) throws IOException {
        mapPermissions.clear();
	mapPermissions.put(RAW_PROPERTIES,props);
	String[] apps = Dcm4cheeFormController.FOLDER_APPLICATIONS;
	String perm, value, role, methods;
	int pos;
	Map map;
	Set set;
	for ( int i = 0, len = apps.length ; i < len ; i++ ) {
	    perm = props.getProperty(apps[i]);
	    if ( perm != null ) {
		StringTokenizer st = new StringTokenizer(perm, ";");
		while ( st.hasMoreTokens() ) {
		    value = st.nextToken();
		    pos = value.indexOf('(');
		    role = value.substring(0,pos);
		    map = (Map)mapPermissions.get(role);
		    if ( map == null ) {
		        map = new HashMap();
		        mapPermissions.put(role,map);
		    }
		    set = (Set) map.get(apps[i]);
		    if (set == null) {
		        set = new HashSet();
		        map.put(apps[i],set);
		    }

		    methods = value.substring(++pos,value.length()-1);
		    addMethods(set, apps[i], methods, props);
		}
	    }
	}
    }

    private void addMethods(Set set, String app, String methods, Properties props ) {
        if ( methods != null ) {
            StringTokenizer st = new StringTokenizer(methods,",");
            String method;
            while ( st.hasMoreTokens() ) {
                method = st.nextToken();
                if (method.charAt(0)=='*') {
                    addMethods(set, app, props.getProperty(app+"."+method,"?"), props);
                } else {
                    set.add(method);
                }
            }
        }		
    }

    public FolderPermissions getFolderPermissions(String userID) {
        log.debug("getFolderPermission for user:"+userID);
        FolderPermissions permissions = new FolderPermissions();
        Collection roles = new ArrayList();
        try {
            Subject subject = (Subject) PolicyContext.getContext(SUBJECT_CONTEXT_KEY);
            Set groups = subject.getPrincipals(Group.class);
            for (Iterator iter = groups.iterator(); iter.hasNext();) {
                Group group = (Group) iter.next();
                if ("Roles".equals(group.getName())) {
                    for (Enumeration members = group.members();
                    members.hasMoreElements();) {
                        Principal member = (Principal) members.nextElement();
                        roles.add(member.getName());
                    }
                }
            }
        } catch ( Exception x ) {
            log.error("Cant get roles for user "+userID+"! use role default",x);
            roles.add("default");
        }
        log.debug("Roles:"+roles);
        Map map;
        Set set;
        Map.Entry entry;
        for ( Iterator iter = roles.iterator() ; iter.hasNext() ; ) {
            map = (Map) mapPermissions.get(iter.next());//permission for a role (key=app,value=list of methods)
            log.debug("permission for role:"+map);
            if ( map != null ) {
                for ( Iterator iter1 = map.entrySet().iterator() ; iter1.hasNext() ; ) {
                    entry = (Map.Entry) iter1.next();
                    set = (Set) entry.getValue();
                    permissions.addPermissions((String)entry.getKey(), (String[])set.toArray(new String[set.size()]));
                }
            }
        }
        if ( permissions.getNumberOfPrivilegedApps() == 0 ) {
            log.warn("User "+userID+" has no Permissions for any Folder Application! Set to use folder only!");
            permissions.addPermissions("folder",null);
        }
        Properties rawProps = (Properties) mapPermissions.get(RAW_PROPERTIES);
        String aeList = rawProps.getProperty(FolderPermissions.AEFILTER+"."+userID);
        if ( aeList != null ) {
            permissions.addPermissions(FolderPermissions.AEFILTER, StringUtils.split(aeList, ',') );
        }
        applyStationAet(permissions,rawProps, userID);
        return permissions;
    }

    private void applyStationAet(FolderPermissions permissions, Properties rawProps, String userID) {
        String stationAetGroups = rawProps.getProperty(FolderPermissions.STATION_AET_GROUP_LIST);
        if ( stationAetGroups != null ) {
            String[] groups = StringUtils.split(stationAetGroups, ',');
            permissions.addPermissions(FolderPermissions.STATION_AET_GROUP_LIST, groups );
            String group, groupAETs;
            for ( int i = 0 ; i < groups.length ; i++) {
                group = FolderPermissions.STATION_AET_GROUP+"."+groups[i];
                groupAETs = rawProps.getProperty(group);
                if ( groupAETs != null ) {
                    permissions.addPermissions(group, StringUtils.split(groupAETs, ',') );
                }
            }
        }
        String stationAetFilter = rawProps.getProperty(FolderPermissions.STATION_AET_FILTER+"."+userID);
        if ( stationAetFilter != null ) {
            permissions.addPermissions(FolderPermissions.STATION_AET_FILTER, StringUtils.split(stationAetFilter, ',') );
        }
    }
}
