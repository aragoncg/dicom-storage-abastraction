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
package org.dcm4chex.archive.web.maverick.admin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.UserManager;
import org.dcm4chex.archive.ejb.interfaces.UserManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.web.maverick.admin.perm.FolderPermissions;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2589 $ $Date: 2006-07-05 10:15:20 +0200 (Wed, 05 Jul 2006) $
 * @since 13.04.2006
 */
public class UserAdminDelegate {
	
	private String securityDomain = "java:/jaas/dcm4chee-web";

	protected static Logger log = Logger.getLogger(UserAdminDelegate.class);
	
	/**
	 * Get list of users from application server. 
	 * @throws Exception
	 */
	public List queryUsers() throws Exception {
		List userList = new ArrayList();
		UserManager manager = lookupUserManager();
		Iterator iterUsers = manager.getUsers().iterator();
		String user; 
		StringBuffer sbRoles;
		while ( iterUsers.hasNext() ) {
			user = (String) iterUsers.next();
			userList.add( new DCMUser( user, manager.getRolesOfUser( user ) ) );
		}
		return userList;
	}
	
	public void addUser( String userID, String passwd, Collection roles) throws RemoteException, Exception {
		lookupUserManager().addUser(userID,passwd, roles);
	}
	
	public void updateUser(String userID, Collection roles) throws RemoteException, Exception {
		lookupUserManager().updateUser(userID, roles);
		clearAuthenticationCache();
	}
	
	public boolean changePasswordForUser(String user, String oldPasswd, String newPasswd) throws RemoteException, Exception {
		boolean b = lookupUserManager().changePasswordForUser(user, oldPasswd, newPasswd);
		clearAuthenticationCache();
		return b;
	}
	
	public void removeUser(String userID) throws RemoteException, Exception {
		lookupUserManager().removeUser( userID );
		clearAuthenticationCache();
	}
	
	public Collection getRolesOfUser(String userID) throws RemoteException, Exception {
		return lookupUserManager().getRolesOfUser(userID);
	}
	
	public FolderPermissions getFolderPermissions(String userID) {
		return null;
	}
	
	private void clearAuthenticationCache() {
		try {
			MBeanServerLocator.locate().invoke(new ObjectName("jboss.security:service=JaasSecurityManager"),
			        "flushAuthenticationCache",
			        new Object[] { securityDomain },
			        new String[] { String.class.getName()});
		} catch (Exception x) {
			log.error("Cant clear Authentication Cache! Reason:", x);
		}
	}
	
	/**
	 * Returns the UserManager bean.
	 * 
	 * @return The UserManager bean.
	 * @throws Exception
	 */
	protected UserManager lookupUserManager() throws Exception
	{
		UserManagerHome home =
			(UserManagerHome) EJBHomeFactory.getFactory().lookup(
					UserManagerHome.class,
					UserManagerHome.JNDI_NAME);
		return home.create();
	}			

}
