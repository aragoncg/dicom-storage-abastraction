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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;

/**
 * @author umberto.cappellini@tiani.com
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5355 $ $Date: 2007-10-28 23:25:25 +0100 (Sun, 28 Oct 2007) $
 */
public class UserEditSubmitCtrl extends Dcm4cheeFormController
{

	private String userHash = null;
	private String passwd = null;
	private String passwd1 = null;
	private DCMUser user = new DCMUser("",null);
	private String cancelPar = null;
	private String cmd;
	
	private static Logger log = Logger.getLogger(UserEditSubmitCtrl.class.getName());
	
	/**
	 * @param oldUserID The oldUserID to set.
	 */
	public void setUserHash(String hash) {
		this.userHash = hash;
	}
	/**
	 * @param cancelPar The cancelPar to set.
	 */
	public void setCancel(String cancelPar) {
		this.cancelPar = cancelPar;
	}
	/**
	 * @param newPar The newPar to set.
	 */
	public void setCmd(String cmd) {
		this.cmd = cmd;
	}
	/**
	 * @param passwd The passwd to set.
	 */
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	/**
	 * @param passwd The passwd to set.
	 */
	public void setPasswd1(String passwd1) {
		this.passwd1 = passwd1;
	}
	/**
	 * @param userID The userID to set.
	 */
	public void setUserID(String userID) {
		log.info("setUserID:"+userID);
		user.setUserID( userID );
	}
	
	protected String perform() throws Exception
	{
		HttpServletRequest req = getCtx().getRequest();
		UserAdminModel model = UserAdminModel.getModel(req);
		model.clearPopupMsg();
		if ( !model.isAdmin()) {
			log.warn("Illegal access to UserEditSubmitCtrl! User "+req.getUserPrincipal()+" is not in role WebAdmin!");
			return "error";
		}
		if ( cancelPar == null ) {
			String userID = req.getParameter("userID");
			String[] roles = req.getParameterValues("role");
			log.info("cmd:"+cmd);
			if ( "addRole".equals(cmd)) {
				model.addRoles(userID, roles);
			} else if ( "removeRole".equals(cmd)) {
				model.removeRoles(userID, roles);
			} else if ( "deleteUser".equals(cmd)) {
				model.deleteUser(userID);
			} else if ( "Create".equals(cmd)) {
				return createUser(model, userID, roles);
			}
		}
		return SUCCESS;
	}
	private String createUser(UserAdminModel model, String userID, String[] roles) {
		if ( passwd.equals(passwd1)) {
			if ( passwd.trim().length() > 2 ) {
				if ( model.createUser(userID, passwd, roles ) == null ) {
					return "passwd_mismatch";
				} else {
					return SUCCESS;
				}
			} else {
				model.setPopupMsg("admin.err_chgpwd_short",userID);
				return "passwd_mismatch";
				
			}
		} else {
			model.setPopupMsg("admin.err_chgpwd_newpwd",userID);
			return "passwd_mismatch";
		}
	}

}
