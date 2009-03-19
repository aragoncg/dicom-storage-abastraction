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

package org.dcm4chex.archive.web.maverick;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.conf.WebRolesConfig;
import org.dcm4chex.archive.web.maverick.admin.perm.FolderPermissions;
import org.dcm4chex.archive.web.maverick.admin.perm.FolderPermissionsFactory;
import org.infohazard.maverick.ctl.Throwaway2;
import org.infohazard.maverick.flow.ControllerContext;

/**
 * Variation of Maverick's ThrowawayFormBeanUser, including support for 
 * input="image" HTML parameters.
 * 
 * @author <a href="mailto:umberto.cappellini@tiani.com">Umberto Cappellini</a>
 * Created: Apr 29, 2004 - 2:24:54 PM
 * Module: dcm4chee-web
 */
public class Dcm4cheeFormController extends Throwaway2
{
    private static final String WEB_USER_STUDY_ROLES_ATTR_NAME = "webUserStudyRoles";

	public static final String[] FOLDER_APPLICATIONS = new String[]{"folder","trash","ae_mgr",
				"offline_storage","mwl_console","mpps_console",
				"gpwl_console","gppps_console","user_admin","audit_repository"};	
	
    public static final String INSPECT = "inspect";
    
	private static Logger log = Logger.getLogger(Dcm4cheeFormController.class.getName());
	/**
	 * The form bean gets set here
	 */
	private Object formBean;

	/**
	 */
	protected Object getForm()
	{
		return this.formBean;
	}
    
	/**
	 * Executes this controller.  Override one of the other perform()
	 * methods to provide application logic.
	 */
	public final String go() throws Exception
	{
		this.formBean = this.makeFormBean();
		Map modified_parameters = new HashMap();
		Map parameters = this.getCtx().getRequest().getParameterMap();
		modified_parameters.putAll(parameters);
		for (Iterator i = parameters.keySet().iterator(); i.hasNext();)
		{
			String parameterName = (String)i.next();
			if (parameterName.endsWith(".x"))
			{
				String newName =
					parameterName.substring(0, parameterName.indexOf(".x"));
				modified_parameters.put(newName, newName);
			}
		}

		BeanUtils.populate(this.formBean, modified_parameters);
		BeanUtils.populate(this.formBean, this.getCtx().getControllerParams());

		getCtx().setModel(this.formBean);
        String version = Dcm4cheeFormController.class.getPackage().getImplementationVersion();
        getCtx().setTransformParam("dcm4chee_version", version != null ? version : "");
        getCtx().setTransformParam("request_uri", getCtx().getRequest().getRequestURI());

		applyPermissions(getCtrlName());
		return this.perform();
	}

	/**
	 * This method can be overriden to perform application logic.
	 *
	 * Override this method if you want the model to be something
	 * other than the formBean itself.
	 *
	 * @param formBean will be a bean created by makeFormBean(),
	 * which has been populated with the http request parameters.
	 */
	protected String perform() throws Exception
	{
		return SUCCESS;
	}

	/**
	 * This method will be called to produce a simple bean whose properties
	 * will be populated with the http request parameters.  The parameters
	 * are useful for doing things like persisting beans across requests.
	 *
	 * Default is to return this.
	 */
	protected Object makeFormBean()
	{
		return this;
	}
	
	protected void applyPermissions(String app) {
		FolderPermissions perms = getPermissions();
		Set allowed = perms.getPermissionsForApp(app);
		ControllerContext ctx = getCtx();
		String param;
		if ( allowed != null ) {
			for ( Iterator iter = allowed.iterator() ; iter.hasNext() ; ) {
				param = (String)iter.next();
				log.debug("setTranformParam method:"+param);
				ctx.setTransformParam( param,"true");
			}
		}
		for ( int i = 0, len = FOLDER_APPLICATIONS.length ; i < len ; i++ ) {
			param = FOLDER_APPLICATIONS[i];
			log.debug("setTransformPath for application:"+param+"="+(perms.getPermissionsForApp(param)!=null));
			ctx.setTransformParam( param, 
					String.valueOf(perms.getPermissionsForApp(param)!=null));
		}
	}
	
	protected FolderPermissions getPermissions() {
		HttpServletRequest req = getCtx().getRequest();
		FolderPermissions perm = null;
		FolderPermissionsFactory f = FolderPermissionsFactory.getInstance(getCtx().getServletConfig());
		if ( req.getParameter("reset")!=null ) {
			f.init();
		} else {
			perm = (FolderPermissions) req.getSession().getAttribute("folderPermissions");
		}
		if ( perm == null ) {
			perm = f.getFolderPermissions(getCtx().getRequest().getUserPrincipal().getName());
			req.getSession().setAttribute("folderPermissions",perm);
		}
		return perm;
	}
	
	protected String getCtrlName() {
		return "folder";
	}

	/**
	 * get list of AETs
	 */
	protected String[] getAEFilterPermissions() {
		Set set = getPermissions().getMethodsForApp(FolderPermissions.AEFILTER);
		return set == null ? null : (String[])set.toArray(new String[set.size()]);
	}

    /**
     * get list of statioAET Filter groupss
     */
    protected Map getStationAEFilterGroups() {
        Set grpNames = getStationAEFilter();
        if ( grpNames == null ) {
            grpNames = getPermissions().getMethodsForApp(FolderPermissions.STATION_AET_GROUP_LIST);
        }
        if ( grpNames == null ) return null;
        HashMap map = new HashMap();
        String group;
        Set aets;
        for ( Iterator iter = grpNames.iterator() ; iter.hasNext() ; ) {
            group = (String) iter.next();
            aets = getStationAETsOfGroup(group);
            if ( aets != null ) {
                map.put(group, aets);
            } else {
                aets = new HashSet();
                aets.add(group);
                map.put(group, aets);
            }
        }
        return map;
    }
  
    protected Set getStationAEFilter() {
        return getPermissions().getMethodsForApp(FolderPermissions.STATION_AET_FILTER);
    }
    
    protected Set getStationAETsOfGroup(String group) {
        return getPermissions().getMethodsForApp(FolderPermissions.STATION_AET_GROUP+"."+group);
    }
    

    public boolean isStudyPermissionCheckDisabled() {
	ControllerContext ctx = getCtx();
	if ( "false".equals(ctx.getServletConfig().getInitParameter("enableStudyPermissionCheck") ) ) {
		log.debug("StudyPermission check is disabled!");
		return true;
	}
	String disableUser = ctx.getServletConfig().getInitParameter("disableStudyPermissionCheckForUser");
	if (disableUser == null || disableUser.trim().length() < 1 || "NONE".equalsIgnoreCase( disableUser ))
		return false;
	String user = ctx.getRequest().getUserPrincipal().getName();
	StringTokenizer st = new StringTokenizer(disableUser, ",");
	while ( st.hasMoreElements() ) {
		if ( user.equals(st.nextToken()) ) {
			log.debug("StudyPermission check is disabled for user "+user+"!");
			return true;
		}
	}
	return false;
    }
}
