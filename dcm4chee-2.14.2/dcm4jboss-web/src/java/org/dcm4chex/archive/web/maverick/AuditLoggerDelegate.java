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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3151 $ $Date: 2007-02-26 01:36:36 +0100 (Mon, 26 Feb 2007) $
 * @since 14.10.2004
 *
 */
public class AuditLoggerDelegate {

    public static final String CREATE = "Create";

    public static final String MODIFY = "Modify";
    
    public static final String ACCESS = "Access";
    
    public static final String DELETE = "Delete";

    private static Logger log = Logger.getLogger(AuditLoggerDelegate.class);

    private static MBeanServer server;

    private static ObjectName auditLogName;
    
    private static boolean iheYr4;

    private static boolean init(ControllerContext ctx) throws Exception {
        if (auditLogName == null) {
            AuditLoggerDelegate.server = MBeanServerLocator.locate();
            String s = ctx.getServletConfig().getInitParameter("auditLoggerName");
            AuditLoggerDelegate.auditLogName = new ObjectName(s);
            Boolean b = (Boolean) server.getAttribute(auditLogName, "IHEYr4");
            iheYr4 = b.booleanValue();
        }
        return iheYr4;
    }

    public static void logActorConfig(ControllerContext ctx, String desc,
            String type) {
        try {
            if (!init(ctx)) return;
            AuditLoggerDelegate.server.invoke(auditLogName,
                    "logActorConfig",
                    new Object[] { desc, type},
                    new String[] { String.class.getName(),
                            String.class.getName(),});
        } catch (Exception e) {
            log.warn("Failed to log ActorConfig:", e);
        }
    }

    public static void logStudyDeleted(ControllerContext ctx, String patid,
            String patname, String suid, int numberOfInstances, String desc) {
        try {
            if (!init(ctx)) return;
            AuditLoggerDelegate.server.invoke(auditLogName,
                    "logStudyDeleted",
                    new Object[] { patid, patname, suid,
                            new Integer(numberOfInstances), desc},
                    new String[] { String.class.getName(),
                            String.class.getName(), String.class.getName(),
                            Integer.class.getName(), String.class.getName()});
        } catch (Exception e) {
            log.warn("Failed to log studyDeleted:", e);
        }
    }

    public static void logPatientRecord(ControllerContext ctx, String action,
            String patid, String patname, String desc) {
        try {
            if (!init(ctx)) return;
            AuditLoggerDelegate.server.invoke(auditLogName,
                    "logPatientRecord",
                    new Object[] { action, patid, patname, desc},
                    new String[] { String.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName()});
        } catch (Exception e) {
            log.warn("Failed to log patientRecord:", e);
        }
    }

    public static void logProcedureRecord(ControllerContext ctx, String action,
            String patid, String patname, String placerOrderNo,
            String fillerOrderNo, String suid, String accNo, String desc) {
        try {
            if (!init(ctx)) return;
            AuditLoggerDelegate.server.invoke(auditLogName,
                    "logProcedureRecord",
                    new Object[] { action, patid, patname, placerOrderNo,
                            fillerOrderNo, suid, accNo, desc},
                    new String[] { String.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName()});
        } catch (Exception e) {
            log.warn("Failed to log procedureRecord:", e);
        }
    }

    public static boolean equals(String prevVal, String newVal) {
        return prevVal == null || prevVal.length() == 0
        ? newVal == null || newVal.length() == 0
                : prevVal.equals(newVal);
    }
    
    public static boolean isModified(String name, String prevVal, String newVal,
            StringBuffer desc) {
        if (prevVal == null || prevVal.length() == 0
                ? newVal == null || newVal.length() == 0
                : prevVal.equals(newVal))
            return false;
        desc.append(name);
        desc.append(" changed from \"");
        if (prevVal != null)
            desc.append(prevVal);
        desc.append("\" to \"");
        if (newVal != null)
            desc.append(newVal);
        desc.append("\", ");
        return true;
    }
    
    public static String trim(StringBuffer sb) {
        int len = sb.length();
        char ch;
        while (len > 0 && ((ch = sb.charAt(len-1)) == ' ' || ch == ',')) --len;
        sb.setLength(len);
        return sb.toString();
    }
    
}