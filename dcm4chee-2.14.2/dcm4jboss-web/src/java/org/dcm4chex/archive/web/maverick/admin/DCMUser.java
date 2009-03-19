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

import java.util.ArrayList;
import java.util.Collection;


/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DCMUser {

	public static final String JBOSSADMIN = "JBossAdmin";
	public static final String WEBADMIN = "WebAdmin";
	public static final String WEBUSER = "WebUser";
	public static final String ARRUSER = "AuditLogUser";
	public static final String MCMUSER = "McmUser";
	public static final String DATACARE_USER = "DatacareUser";
	
	private String userID;
	private Collection roles = new ArrayList();
	private int hash;
	
	public DCMUser( String userID, Collection roles ) {
		if ( userID == null ) {
			throw new IllegalArgumentException("Cant create DCMUser! UserID must not be null!");
		}
		this.userID = userID;
		hash = userID.hashCode();
		if ( roles != null )
			this.roles = roles;
	}
	
	/**
	 * @param hashCode
	 */
	private DCMUser(int hashCode) {
		hash = hashCode;
		// TODO Auto-generated constructor stub
	}

	public static final DCMUser getQueryUser( int hashCode ) {
		return new DCMUser(hashCode);
	}
	
	public void setUserID( String id ) {
		userID = id;
	}
	
	public void setRole(String role, boolean enable) {
		if ( enable )
			addRole(role);
		else
			removeRole(role);
	}
	public void addRole( String role ) {
		if ( ! roles.contains(role ) ) {
			roles.add(role);
		}
	}

	public void removeRole( String role ) {
		roles.remove(role);
	}
	
	public boolean isInRole( String role ) {
		return roles.contains( role );
	}
	
	/**
	 * 
	 * @return Get list of all roles assigned to this user.
	 */
	public Collection getRoles() { 
		return roles;
	}
	
	/**
	 * @return Returns the userID.
	 */
	public String getUserID() {
		return userID;
	}
	
	public int getUserHash() {
		return hash;
	}

	
	/**
	 * Returns simple description of this object.
	 */
	public String toString() {
		return "UserID:"+userID+" roles:"+roles;	
	}
	
	/**
	 * Returns true if parameter is a DCMUser object with same userID as this object.
	 * <p>
	 * This method returns true even roles are equal or not!
	 * <p>
	 * Use hashcode to check equality!
	 * 
	 * @param user The object to check equality.
	 * 
	 * @return true if userID is equal.
	 */
	public boolean equals( Object user ) {
		if ( user != null || (user instanceof DCMUser) ) {
			return hash == ((DCMUser)user).hashCode();
		} 
		return false;
	}
	
	/**
	 * Returns hashCode of userID String object.
	 * 
	 * @return Hashcode of this object.
	 */
	public int hashCode() {
		return hash;
	}
}
