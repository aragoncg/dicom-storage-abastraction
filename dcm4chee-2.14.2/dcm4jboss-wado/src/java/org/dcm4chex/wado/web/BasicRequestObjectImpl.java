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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.wado.common.BasicRequestObject;
import org.dcm4chex.wado.mbean.WADOService;

/**
 * @author franz.willer
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public abstract class BasicRequestObjectImpl implements BasicRequestObject {

    private static Logger log = Logger.getLogger(WADOService.class.getName());

    protected String reqType;
    protected Map headerMap;
    protected Map paramMap;
    protected List allowedContentTypes = null;

    protected String reqURL;
    protected String errMsg;

    protected String remoteAddr;
    protected String remoteHost = null;
    protected String remoteUser;
    boolean studyPermissionCheckDisabled;
    
    protected HttpServletRequest request;

    /**
     * Initialize an RequestObject with http request.
     * <p>
     * This constructor should be used by the real implementations.
     * 
     * @param request
     *                The http request.
     */
    public BasicRequestObjectImpl(HttpServletRequest request) {
        this.request = request;
        reqType = request.getParameter("requestType");
        if (reqType == null)
            reqType = request.getParameter("RT");
        paramMap = request.getParameterMap();
        try {
            reqURL = request.getRequestURL().append("?").append(
                    request.getQueryString()).toString();
        } catch (Exception x) {

        }
        headerMap = new HashMap();
        Enumeration enum1 = request.getHeaderNames();
        String key;
        while (enum1.hasMoreElements()) {
            key = (String) enum1.nextElement();
            if (log.isDebugEnabled())
                log.debug("header: " + key + "=" + request.getHeader(key));
            headerMap.put(key, request.getHeader(key));
        }
        setAllowedContentTypes(request.getHeader("accept"));
        this.remoteAddr = request.getRemoteAddr();
        remoteUser = request.getRemoteUser();
    }

    public final HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @param accept
     */
    private void setAllowedContentTypes(String accept) {
        List l = null;
        String s;
        if (accept != null) {
            l = new ArrayList();
            StringTokenizer st = new StringTokenizer(accept, ",");
            while (st.hasMoreElements()) {
                s = st.nextToken();
                if (s.indexOf(";") != -1)
                    s = s.substring(0, s.indexOf(";"));// ignore quality value
                l.add(s.trim());
            }
        }
        allowedContentTypes = l;
    }

    /**
     * Returns the requestType parameter of the http request.
     * 
     * @return requestType
     */
    public String getRequestType() {
        return reqType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.dcm4chex.wado.common.RIDRequestObject#getAllowedContentTypes()
     */
    public List getAllowedContentTypes() {
        return this.allowedContentTypes;
    }

    /**
     * Returns all parameter of the http request in a map.
     * 
     * @return All http parameter
     */
    public Map getRequestParams() {
        return paramMap;
    }

    /**
     * Returns a Map of all request header fields of the http request.
     * 
     * @see org.dcm4chex.wado.common.WADORequestObject#getRequestHeaders()
     * 
     * @return All request header fields in a map.
     */
    public Map getRequestHeaders() {
        return headerMap;
    }

    public String getRequestURL() {
        return reqURL;

    }

    /**
     * @return Returns the errMsg.
     */
    public String getErrorMsg() {
        return errMsg;
    }

    /**
     * @param errMsg
     *                The errMsg to set.
     */
    protected void setErrorMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    /**
     * @return Returns the remoteAddr.
     */
    public String getRemoteAddr() {
        return remoteAddr;
    }

    /**
     * @return Returns the remoteHost.
     */
    public String getRemoteHost() {
        if (remoteHost == null) {
            try {
                InetAddress ia = InetAddress.getByName(remoteAddr);
                remoteHost = ia.getHostName();
            } catch (Exception ignore) {
            }
        }
        return remoteHost;
    }

    public String getRemoteUser() {
        return remoteUser;
    }

	public boolean isStudyPermissionCheckDisabled() {
		return studyPermissionCheckDisabled;
	}

	public void setStudyPermissionCheckDisabled(boolean studyPermissionCheckDisabled) {
		this.studyPermissionCheckDisabled = studyPermissionCheckDisabled;
	}

}
