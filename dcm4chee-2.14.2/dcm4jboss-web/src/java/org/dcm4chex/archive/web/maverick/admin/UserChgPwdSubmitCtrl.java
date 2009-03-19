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


/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3140 $ $Date: 2007-02-22 15:37:56 +0100 (Thu, 22 Feb 2007) $
 */
public class UserChgPwdSubmitCtrl extends UserAdminCtrl
{
	private String cancelPar = null;
	private String userID;
	private String oldPasswd;
	private String passwd;
	private String passwd1;

	
    protected Object makeFormBean() {
        return this;
    }
	
	/**
	 * @param userID The userID to set.
	 */
	public void setUserID(String userID) {
		this.userID = userID;
	}
	/**
	 * @param oldPasswd The oldPasswd to set.
	 */
	public void setOldPasswd(String oldPasswd) {
		this.oldPasswd = oldPasswd;
	}
	/**
	 * @param passwd The passwd to set.
	 */
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	/**
	 * @param passwd1 The passwd1 to set.
	 */
	public void setPasswd1(String passwd1) {
		this.passwd1 = passwd1;
	}
	/**
	 * @param cancelPar The cancelPar to set.
	 */
	public void setCancel(String cancelPar) {
		this.cancelPar = cancelPar;
	}
	
	protected String perform() throws Exception
	{
		UserAdminModel model = UserAdminModel.getModel( getCtx().getRequest() );
		model.clearPopupMsg();
		if ( cancelPar == null ) {
			if ( passwd.equals(passwd1)) {
				if ( passwd.trim().length() > 2 ) {
					if ( ! model.changePassword( userID, oldPasswd, passwd ) ) {
						model.setPopupMsg("admin.err_chgpwd_oldpwd", userID);
						return "chgpwd_error";
					}
				} else {
					model.setPopupMsg("admin.err_chgpwd_short", userID);
					return "chgpwd_error";
				}
			} else {
				model.setPopupMsg("admin.err_chgpwd_newpwd", userID);
				return "chgpwd_error";
			}
		}
		
		return SUCCESS;
	}		
}
