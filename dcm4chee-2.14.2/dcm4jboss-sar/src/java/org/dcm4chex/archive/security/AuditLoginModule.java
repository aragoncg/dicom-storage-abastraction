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

package org.dcm4chex.archive.security;

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.log4j.Logger;

/**
 * <description>
 * 
 * @see <related>
 * @author <a href="mailto:gunter@tiani.com">gunter zeilinger </a>
 * @version $Revision: 2010 $ $Date: 2005-10-06 21:55:27 +0200 (Thu, 06 Oct 2005) $
 * @since August 31, 2002
 * 
 */
public class AuditLoginModule implements LoginModule
{

    static final Logger log = Logger.getLogger(AuditLoginModule.class);

    private CallbackHandler cbh;

    private ObjectName auditLoggerName;

    private MBeanServer server;

    public void initialize(Subject subject, CallbackHandler cbh,
            Map sharedState, Map options) {
        this.cbh = cbh;
        try {
            auditLoggerName = new ObjectName((String) options
                    .get("auditLoggerName"));
        } catch (MalformedObjectNameException mone) {
            String prompt = "Illegal value of <module-option name=\"auditLoggerName\">"
                    + options.get("auditLoggerName");
            log.error(prompt);
            throw new IllegalArgumentException(prompt);

        }

        if (auditLoggerName == null) {
            log.error("Missing <module-option name=\"auditLoggerName\">");
            throw new IllegalArgumentException(
                    "Missing <module-option name=\"auditLoggerName\">");
        }
        server = (MBeanServer) MBeanServerFactory.findMBeanServer(null)
                .iterator().next();
    }

    public boolean login() throws LoginException {
        return true;
    }

    public boolean abort() throws LoginException {
        logUserAuthenticated("Failure");
        return true;
    }

    public boolean commit() throws LoginException {
		logUserAuthenticated("Login");
        return true;
    }

    public boolean logout() throws LoginException {
		logUserAuthenticated("Logout");
        return true;
    }

    private String getUserName() {
        try {
            NameCallback nc = new NameCallback("prompt");
            cbh.handle(new Callback[] { nc});
            return nc.getName();
        } catch (Exception e) {
            log.error("Failed to access UserName:", e);
        }
        return null;
    }

    private void logUserAuthenticated(String action) {
        try {
            server.invoke(auditLoggerName, "logUserAuthenticated",
                new Object[] { getUserName(), action },
                new String[] { String.class.getName(), String.class.getName()});
        } catch (Exception e) {
            log.warn("Audit Log failed:", e);
        }
    }	
}
