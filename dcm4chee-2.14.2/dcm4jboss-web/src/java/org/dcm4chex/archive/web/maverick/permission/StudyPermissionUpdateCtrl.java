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

package org.dcm4chex.archive.web.maverick.permission;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;

/**
 * 
 * @author franz.willer@agfa.com
 * @version $Revision: 2531 $ $Date: 2006-06-20 16:49:49 +0200 (Di, 20 Jun 2006) $
 * @since 19.10.2007
 */
public class StudyPermissionUpdateCtrl extends Dcm4cheeFormController {

    private static final String CANCEL = "cancel";

	private StudyPermissionModel model;

    private static Logger log = Logger.getLogger(StudyPermissionUpdateCtrl.class);

    protected String getCtrlName() {
	return "study_permission_update";
    }
    protected StudyPermissionModel getModel() {
        return model;
    }
    
    protected Object makeFormBean() {
        try {
            model = StudyPermissionModel.getModel(getCtx(), this);
        } catch (Exception e) {
            log.error("Failed to create StudyPermissionModel!");
        }
        return model;
    }

    
    protected String perform() throws Exception {
        log.info("perform called!");
        try {
	        HttpServletRequest req = getCtx().getRequest();
	        String cmd = nullEmptyValue(req.getParameter("cmd"));
	        StudyPermissionModel model = StudyPermissionModel.getModel(getCtx(), this);
	        if ( cmd == null ) {
	            model.setPopupMsg("folder.study_permission_missingAttr", "cmd");
	            return SUCCESS;
	        } else if ( "cancel".equalsIgnoreCase(cmd) ) {
	            return CANCEL;
	        }
	        String role = nullEmptyValue(req.getParameter("role"));
	        String action = nullEmptyValue(req.getParameter("action"));
	        if ( role == null && action == null ) {
	            model.setPopupMsg("folder.study_permission_missingAttr", "role, action");
	            return SUCCESS;
	        }
	        if ( "add".equalsIgnoreCase(cmd) ) {
	        	if ( action == null ) {
	        		model.addRole(role);
	        	} else if ( role == null ) {
	        		model.addAction(action);
	        	} else {
	        		model.addPermission(role, action);
	        	}
	        } else if ( "remove".equalsIgnoreCase(cmd) ) {
	            model.removePermission(role, action);
	        } else {
	            model.setPopupMsg("folder.study_permission_unknown_cmd", cmd);
	        }
	        return SUCCESS;
        } catch (Exception x) {
        	log.error("StudyPermission update failed:", x);
        	return CANCEL;
        }
    }
    
    private String nullEmptyValue( String value ) {
    	return ( value == null || value.trim().length() < 1 ) ? null : value;
    }

}
