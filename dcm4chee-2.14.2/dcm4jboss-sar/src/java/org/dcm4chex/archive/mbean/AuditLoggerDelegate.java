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

import javax.management.ObjectName;

import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 3181 $ $Date: 2007-03-07 12:35:17 +0100 (Wed, 07 Mar 2007) $
 * @since Mar 7, 2007
 */
public class AuditLoggerDelegate {

    private ServiceMBeanSupport service;

    private ObjectName auditLogName;

    private Boolean auditLogIHEYr4;
    
    public AuditLoggerDelegate(ServiceMBeanSupport service) {
        this.service = service;
    }

    public final ObjectName getAuditLoggerName() {
        return auditLogName;
    }

    public final void setAuditLoggerName(ObjectName auditLogName) {
        this.auditLogName = auditLogName;
    }
    
    public boolean isAuditLogIHEYr4() {
        if (auditLogName == null) {
            return false;
        }
        if (auditLogIHEYr4 == null) {
            try {
                this.auditLogIHEYr4 = (Boolean) service.getServer()
                        .getAttribute(auditLogName, "IHEYr4");
            } catch (Exception e) {
                service.getLog().warn("JMX failure: ", e);
                this.auditLogIHEYr4 = Boolean.FALSE;
            }
        }
        return auditLogIHEYr4.booleanValue();
    }
}
