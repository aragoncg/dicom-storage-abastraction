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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4che.util.Base64;
import org.dcm4chex.archive.web.conf.WebRolesConfig;
import org.dcm4chex.archive.web.maverick.BasicFormModel;


/**
 * @author franz.willer
 *
 * The Model for Media Creation Managment WEB interface.
 */
public class UserAdminModel extends BasicFormModel {

	private List userList = null;
	
	private WebRolesConfig webRoles; 
	
	private UserAdminDelegate delegate = new UserAdminDelegate();

	private static final String USERMODEL_ATTR_NAME = "UserAdminModel";
    private static final String RESOURCE_BUNDLE_WEB_ROLES = "web_roles";

	private static Logger log = Logger.getLogger(UserAdminModel.class.getName());

	
	/**
	 * Creates the model.
	 * <p>
	 * Perform an initial media search with the default filter. <br>
	 * (search for all media with status COLLECTING)
	 * <p>
	 * performs an initial availability check for MCM_SCP service.
	 */
	private UserAdminModel(HttpServletRequest request) {
		super(request);
		webRoles = new WebRolesConfig();
	}
	
	/**
	 * Get the model for an http request.
	 * <p>
	 * Look in the session for an associated model via <code>MCMMODEL_ATTR_NAME</code><br>
	 * If there is no model stored in session (first request) a new model is created and stored in session.
	 * 
	 * @param request A http request.
	 * 
	 * @return The model for given request.
	 */
	public static final UserAdminModel getModel( HttpServletRequest request ) {
		UserAdminModel model = (UserAdminModel) request.getSession().getAttribute(USERMODEL_ATTR_NAME);
		if (model == null) {
			model = new UserAdminModel(request);
			request.getSession().setAttribute(USERMODEL_ATTR_NAME, model);
		}
		return model;
	}

	public String getModelName() { return "UserAdmin"; }
	
    public WebRolesConfig getWebRoles() {
    	return webRoles;
    }
	
	/**
	 * Get list of users.
	 * 
	 * @return List of DCMUser objects.
	 */
	public List getUserList() {
		if ( userList == null ) {
			try {
				queryUsers();
			} catch (Exception e) {
				log.error("Cant query list of users!",e);
			}
		}
		return userList;
	}
	
	/**
	 * create a new user.
	 * <p>
	 * returns the new created user or null if user cant be created.
	 * 
	 * @param userID The (unique) userID.
	 * @param passwd The password for the new user.
	 * @param roles The roles assigned to this user.
	 * 
	 * @return User object if user is created or null.
	 */
	public DCMUser createUser( String userID, String passwd, String[] roles ) {
		DCMUser user = new DCMUser(userID,null);
		if ( getUserList().contains( user ) ) {
			log.warn("Cant create user! UserID "+user.getUserID()+" already exists!");
			setPopupMsg("admin.user_exists", user.getUserID());
			return null;
		} else {
			try {
				for ( int i = 0 ; i < roles.length ; i++ ) {
					user.addRole(roles[i]);
				}
				String hashedPasswd = Base64.byteArrayToBase64(this.createPasswordHash(passwd));
				delegate.addUser(userID, hashedPasswd, user.getRoles());
			} catch (Exception e) {
				log.error("Cant create new user "+userID+" with roles "+user.getRoles(), e);
				this.setPopupMsg("admin.err_create", new String[]{userID,e.getMessage()});
				return null;
			}
			log.info("User "+user+" created! roles:"+user.getRoles());
		}
		return user;
	}

	
	public boolean changePassword(String userID, String oldPasswd, String newPasswd){
		try {
			final String oldHashedPasswd = Base64.byteArrayToBase64(this.createPasswordHash(oldPasswd));
			final String newHashedPasswd = Base64.byteArrayToBase64(this.createPasswordHash(newPasswd));
			if ( delegate.changePasswordForUser(userID, oldHashedPasswd, newHashedPasswd) ) {
				log.info("Password changed of user "+userID );
				return true;
			}
		} catch (Exception e) {
			log.error("Cant change password for user "+userID);
		}
		this.setPopupMsg( "admin.err_chgpwd",userID);
		return false;
	}

	public boolean removeRoles(String userID, String[] roles){
		try {
			DCMUser user = getUser(userID);
			for ( int i = 0 ; i < roles.length ; i++ ) {
				user.removeRole(roles[i]);
			}
			delegate.updateUser(userID, checkDependencies(user) );
			log.debug("Roles removed from user "+userID );
			return true;
		} catch (Exception e) {
			log.error("Cant remove roles from user "+userID);
		}
		this.setPopupMsg( "admin.err_update",userID);
		return false;
	}
	public boolean addRoles(String userID, String[] roles){
		try {
			DCMUser user = getUser(userID);
			for ( int i = 0 ; i < roles.length ; i++ ) {
				user.addRole(roles[i]);
			}
			delegate.updateUser(userID, checkDependencies(user) );
			log.debug("Roles added to user "+userID );
			return true;
		} catch (Exception e) {
			log.error("Cant add roles to user "+userID);
		}
		this.setPopupMsg( "admin.err_update",userID);
		return false;
	}
	
	private Collection checkDependencies(DCMUser user) {
		String role,d;
		Collection roles = user.getRoles();
		HashSet missing = new HashSet();
		for ( Iterator iter = roles.iterator() ; iter.hasNext() ; ) {
			role = (String) iter.next();
			d = webRoles.getDependencyForRole( role );
			if ( d != null && !roles.contains(d) ) {
				missing.add(d);
			}
		}
		log.info("Add missing dependencies:"+missing);
		for ( Iterator iter = missing.iterator() ; iter.hasNext() ; ) {
			user.addRole((String) iter.next() );
		}
		return user.getRoles();
	}

	/**
	 * Returns the user object for given userID or null if user doesnt exist.
	 * 
	 * @param userID
	 * @return user object or null.
	 */
	public DCMUser getUser( String userID ) {
		return (DCMUser) userList.get( getUserList().indexOf( new DCMUser(userID, null) ) );
	}

	/**
	 * Deletes given user.
	 * 
	 * @param userID The userID to delete
	 * 
	 * @return true if user is deleted, false otherwise.
	 */
	public boolean deleteUser( String userID ) {
		try {
			delegate.removeUser( userID );
		} catch (Exception e) {
			log.error("Cant delete user "+userID, e );
			setPopupMsg( "admin.err_delete", new String[]{userID, e.getMessage()});
			return false;
		}
		return true;
	}

	/**
	 * Get list of users from application server. 
	 * @throws Exception
	 */
	public void queryUsers() {
		try {
			userList = delegate.queryUsers();
		} catch ( Exception x ) {
			log.error("Cant query user list!", x);
			setPopupMsg( "admin.err_query",x.getMessage());
		}
	}
	
	/**
	 * Creates hash of the given password
	 * 
	 * @param password The given password
	 * 
	 * @return Byte array containing hash of the given password
	 */
	private byte[] createPasswordHash(String password)
	{
		try
		{
		   final MessageDigest digest = MessageDigest.getInstance("SHA");
		   byte[] hashBytes = digest.digest((password).getBytes("UTF-8"));
		   return hashBytes;
		} catch ( NoSuchAlgorithmException ex ) {
			log.error("Cannot create safe password!", ex);
			setPopupMsg( "admin.err_pwdhash", ex.getMessage());
			return new byte[0];
		} catch ( UnsupportedEncodingException ex ) {
			log.error("Cannot convert to UTF-8!", ex);
			setPopupMsg( "admin.err_pwdhash", ex.getMessage());
			return new byte[0];
		}
	}
}
