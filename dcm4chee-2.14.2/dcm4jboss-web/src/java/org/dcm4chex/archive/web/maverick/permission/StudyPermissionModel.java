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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.hl7.StudyPermissionDelegate;
import org.dcm4chex.archive.web.conf.WebRolesConfig;
import org.dcm4chex.archive.web.maverick.BasicFormModel;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.FolderForm;
import org.dcm4chex.archive.web.maverick.model.PatientModel;
import org.dcm4chex.archive.web.maverick.model.StudyModel;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;


/**
 * @author franz.willer
 *
 * The Model for Media Creation Managment WEB interface.
 */
public class StudyPermissionModel extends BasicFormModel {

    private static final String STUDY_PERMISSION_ATTR_NAME = "StudyPermissionModel";
    private static final String RESOURCE_BUNDLE_WEB_ROLES = "web_roles";

    private static Logger log = Logger.getLogger(StudyPermissionModel.class.getName());

    private StudyPermissionDelegate delegate;

    private WebRolesConfig webRoles; 
    private Map permissions = new HashMap();
	
    private String suid;
    private PatientModel patient;
    private int countStudies;
    
    private boolean studyPermissionCheckDisabled;
    private boolean grantPrivileg;
    private boolean grantOwnPrivileg;
    
    private Map grantedStudyActions;
    private Set grantedActions;
    
    private StudyPermissionModel(ControllerContext ctx) throws Exception {
    	super(ctx.getRequest());
    	initWebRolesConfig();
        initStudyPermissionDelegate(ctx);
    }

	public void initWebRolesConfig() {
		webRoles = new WebRolesConfig();
	}
	
    public static final StudyPermissionModel getModel(ControllerContext ctx, Dcm4cheeFormController ctrl) throws Exception {
        HttpServletRequest request = ctx.getRequest();
		StudyPermissionModel model = (StudyPermissionModel) request.getSession().getAttribute(STUDY_PERMISSION_ATTR_NAME);
		if (model == null) {
		    model = new StudyPermissionModel(ctx);
		    request.getSession().setAttribute(STUDY_PERMISSION_ATTR_NAME, model);
		}
		model.initActiveUserPermissions(ctx, ctrl);
		return model;
    }

	private void initActiveUserPermissions(ControllerContext ctx, Dcm4cheeFormController ctrl) {
		HttpServletRequest request = ctx.getRequest();
		this.studyPermissionCheckDisabled = ctrl.isStudyPermissionCheckDisabled();
	    this.grantPrivileg = request.isUserInRole("GrantPrivileg");
	    this.grantOwnPrivileg = request.isUserInRole("GrantOwnPrivileg");
	    this.grantedStudyActions = FolderForm.getFolderForm(ctx).getGrantedStudyActions();
	}

    public String getModelName() { return STUDY_PERMISSION_ATTR_NAME; }
	
    public WebRolesConfig getRolesConfig() {
    	return webRoles;
    }

    public String getStudyIUID() {
        return suid;
    }
    
    public PatientModel getPatient() {
        return patient;
    }

    public void setPatient(PatientModel patient) {
        log.info("set patient:"+patient);
        this.patient = patient;
    }

    public Map getRolesWithActions() {
        return permissions;
    }
    
	
    public boolean isStudyPermissionCheckDisabled() {
		return studyPermissionCheckDisabled;
	}

	public boolean isGrantPrivileg() {
		return grantPrivileg;
	}

	public boolean isGrantOwnPrivileg() {
		return grantOwnPrivileg;
	}

    public Set getGrantedActions() {
        return this.grantedActions;
    }
	
	private StudyPermissionDelegate initStudyPermissionDelegate(ControllerContext ctx) throws MalformedObjectNameException, NullPointerException {
        if ( delegate == null ) {
            delegate = new StudyPermissionDelegate( MBeanServerLocator.locate() );
            String s = ctx.getServletConfig().getInitParameter("studyPermissionServiceName");
            delegate.setStudyPermissionServiceName(new ObjectName(s));
        }
        log.info("initialized StudyPermissionDelegate!:"+delegate );
        return delegate;
    }

    public boolean setFilter( String suid, PatientModel patModel) throws Exception {
        if ( suid == null && patModel == null) {
            log.info("Use model! suid:"+suid+" patient:"+patient.getPatientName());
            return false;
        }
    	this.suid = suid;
    	this.patient = patModel;
	    if ( !studyPermissionCheckDisabled && !grantPrivileg && grantOwnPrivileg ) {
	    	if ( suid != null ) {
	    		this.grantedActions = (Set) this.grantedStudyActions.get(suid);
	    	} else {
	    		this.grantedActions = new HashSet();
	    		Set actions;
	    		StudyModel study;
	    		for ( Iterator iter = patient.getStudies().iterator() ; iter.hasNext() ; ) {
	    			 study = (StudyModel) iter.next();
	    			 actions = (Set) this.grantedStudyActions.get( study.getStudyIUID() );
	    			 if ( actions != null )
	    				 this.grantedActions.addAll(actions);
	    		}
	    	}
	    } else {
	    	this.grantedActions = null;
	    }
        return true;
    }
    
    public boolean query() throws Exception {
		permissions.clear();
		Collection studyPermissions;
        if ( suid != null ) {
            studyPermissions = delegate.findByStudyIuid(suid);
            countStudies = 1;
        } else if ( patient != null ) {
            studyPermissions = delegate.findByPatientPk(patient.getPk());
            countStudies = delegate.countStudiesOfPatient(new Long(patient.getPk()));
        } else {
        	this.setPopupMsg("folder.study_permission_missingAttr", "studyIUID, patPk");
        	return false; 
        }
        StudyPermissionDTO dto;
        Map actions;
        List suids;
        for (Iterator iter = studyPermissions.iterator(); iter.hasNext(); ) {
            dto = (StudyPermissionDTO) iter.next();
            actions = (Map) permissions.get(dto.getRole());
            if ( actions == null ) {
                actions = new HashMap();
                String role = dto.getRole();
                permissions.put(role, actions);
                addRole(role);
            }
            suids = (List) actions.get(dto.getAction());
            if ( suids == null ) {
                suids = new ArrayList();
                String action = dto.getAction();
                actions.put(action, suids);
                addAction(action);
            }
            suids.add( dto.getStudyIuid() );
        }
        return true;
    }

	public void addAction(String action) {
		if ( ! webRoles.getActions().containsKey(action) ) {
			webRoles.getActions().put(action, "unconfigured action");
		}
	}

	public void addRole(String role) {
		if ( webRoles.getRole(role) == null) {
			webRoles.addRole(role, "unconfigured role");
		}
	}

    /**
     * Return count of studies.
     * <p>
     * If study IUID is given: return 1.<br/>
     * If patPk is given: return total number of studies for this patient.
     * <p>
     * This count is used to determine if a permission is given to all studies of the patient.
     * 
     * @return
     */
    public int getCountStudies() throws Exception {
        return countStudies;
    }
    
    public void removePermission(String role, String action) throws Exception {
        if ( suid != null ) {
                StudyPermissionDTO dto = new StudyPermissionDTO();
                dto.setStudyIuid(suid);
                dto.setRole(role);
                dto.setAction(action);
                delegate.revoke(dto);
        } else {
                delegate.revokeForPatient(patient.getPk(), action, role);
        }
    }

    public void addPermission(String role, String action) throws Exception {
        if ( suid != null ) {
                delegate.grant(suid, action, role);
        } else {
                delegate.grantForPatient(patient.getPk(), action, role);
        }
        addRole(role);
        addAction(action);
    }
}
