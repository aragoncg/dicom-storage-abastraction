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

package org.dcm4chex.wado.web;

import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dcm4chex.wado.common.WADORequestObject;
import org.dcm4chex.wado.common.WADOResponseObject;
import org.dcm4chex.wado.mbean.WADOStreamResponseObjectImpl;
import org.jboss.mx.util.MBeanServerLocator;

/*
 * Created on 10.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WADOServiceDelegate {

    private static ObjectName wadoServiceName = null;
	private static MBeanServer server;
	private static boolean studyPermissionCheckEnabled;
	private static ArrayList disableStudyPermissionUserList;
	
    private static Logger log = Logger.getLogger( WADOServiceDelegate.class.getName() );

    /** 
     * Iinitialize the WADO service delegator.
     * <p>
     * Set the name of the WADOService MBean with the servlet config param 'wadoServiceName'.
     * 
     * @param config The ServletConfig object.
     */
	public void init( ServletConfig config ) {
        if (server != null) return;
        server = MBeanServerLocator.locate();
        String s = config.getInitParameter("wadoServiceName");
        try {
			wadoServiceName = new ObjectName(s);
			
		} catch (Exception e) {
			log.error( "Exception in init! Servlet init parameter 'wadoServiceName' not valid",e );
		}
		studyPermissionCheckEnabled = "true".equals(config.getInitParameter("enableStudyPermissionCheck"));
		if ( studyPermissionCheckEnabled ) {
			disableStudyPermissionUserList = new ArrayList(3);
			String users = config.getInitParameter("disableStudyPermissionCheckForUser");
			if ( users != null && users.trim().length() > 0) {
				for ( StringTokenizer st = new StringTokenizer(users, ",") ; st.hasMoreTokens() ; ) {
					disableStudyPermissionUserList.add(st.nextToken().trim());
				}
			}
		} 
    }

	public Logger getLogger() {
		return log;
	}
	
	/**
	 * Makes the MBean call to get the WADO response object for given WADO request.
	 * 
	 * @param reqVO	The WADO request.
	 * 
	 * @return The WADO response object.
	 */
	public WADOResponseObject getWADOObject( WADORequestObject reqVO ) {
		WADOResponseObject resp = null;
		reqVO.setStudyPermissionCheckDisabled( isStudyPermissionCheckDisabled(reqVO.getRequest()) );
		try {
	        Object o = server.invoke(wadoServiceName,
	                "getWADOObject",
	                new Object[] { reqVO },
	                new String[] { WADORequestObject.class.getName() } );
	        resp = (WADOResponseObject) o;
		} catch ( Exception x ) {
			log.error( "Exception occured in getWADOObject: "+x.getMessage(), x );
			resp = new WADOStreamResponseObjectImpl( null, "text.html", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unexpected error in WADO service ("+wadoServiceName+"): "+x.getMessage());
		}
        return resp;
	}
	
	public boolean isStudyPermissionCheckDisabled(HttpServletRequest request) {
		log.debug("studyPermissionCheckEnabled:"+studyPermissionCheckEnabled);
		log.debug("disableStudyPermissionUserList:"+disableStudyPermissionUserList);
		log.debug("remoteUser:"+request.getRemoteUser());
		return !studyPermissionCheckEnabled || disableStudyPermissionUserList.contains( request.getRemoteUser() );
	}

}
