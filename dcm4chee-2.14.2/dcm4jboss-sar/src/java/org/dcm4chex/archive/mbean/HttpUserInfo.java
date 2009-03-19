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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

package org.dcm4chex.archive.mbean;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 5039 $ $Date: 2007-09-21 15:23:20 +0200 (Fri, 21 Sep 2007) $
 * @since Mar 7, 2007
 */
public class HttpUserInfo {
    private static final String WEB_REQUEST_KEY =
            "javax.servlet.http.HttpServletRequest";
    private String userId;
    private String ip;
    private String hostName;

    public HttpUserInfo(boolean enableDNSLookups) {
        try {
            HttpServletRequest rq = (HttpServletRequest)
                    PolicyContext.getContext(WEB_REQUEST_KEY);
            init(rq, enableDNSLookups);
        } catch (PolicyContextException e) {
            userId = "UNKNOWN_USER";
        } catch (NullPointerException e) {
            // Thrown when mbean method is invoked by MDB
            userId = "SYSTEM";
        }
    }
    public HttpUserInfo(HttpServletRequest rq, boolean enableDNSLookups) {
        init(rq, enableDNSLookups);
    }
    
    private void init(HttpServletRequest rq, boolean enableDNSLookups) {
        userId = rq.getRemoteUser();
        String xForward = (String) rq.getHeader("x-forwarded-for");
        if (xForward != null) {
            int pos = xForward.indexOf(',');
            ip = (pos > 0 ? xForward.substring(0,pos) : xForward).trim();
        } else {
            ip = rq.getRemoteAddr();
        }
        if ( enableDNSLookups ) {
            try {
                hostName = InetAddress.getByName(ip).getHostName();
            } catch (UnknownHostException ignore) {
                hostName = ip;
            }
        } else {
            hostName = ip;
        }
    }
    
    public String getUserId() {
        return userId;
    }
    public String getIP() {
        return ip;
    }
    
    public String getHostName() {
        return hostName;
    }

}
