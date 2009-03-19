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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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
package org.dcm4chex.archive.hl7;

import java.util.Collection;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Oct 16, 2007
 * 
 */
public class StudyPermissionDelegate {
    
    private final MBeanServer server;
    private ObjectName studyPermissionServiceName;

    public StudyPermissionDelegate(MBeanServer server) {
        this.server = server;
    }

    public final ObjectName getStudyPermissionServiceName() {
        return studyPermissionServiceName;
    }

    public final void setStudyPermissionServiceName(ObjectName serviceName) {
        this.studyPermissionServiceName = serviceName;
    }

    public boolean hasPermission(String suid, String action, String role)
            throws Exception {
        Boolean b = (Boolean) server.invoke(studyPermissionServiceName,
                "hasPermission", new Object[] { suid, action, role },
                new String[] { String.class.getName(), String.class.getName(),
                        String.class.getName() });
        return b.booleanValue();
    }

    public Collection findByPatientPk(long pk) throws Exception {
        return (Collection) server.invoke(studyPermissionServiceName,
                "findByPatientPk", new Object[] { new Long(pk) },
                new String[] { long.class.getName() });
    }
    
    public Collection findByStudyIuid(String suid) throws Exception {
        return (Collection) server.invoke(studyPermissionServiceName,
                "findByStudyIuid", new Object[] { suid },
                new String[] { String.class.getName() });
    }

    public Collection findByStudyIuidAndAction(String suid, String action)
            throws Exception {
        return (Collection) server
                .invoke(studyPermissionServiceName, "findByStudyIuidAndAction",
                        new Object[] { suid, action },
                        new String[] { String.class.getName(),
                                String.class.getName() });
    }


    public int grant(String suid, String actions, String role) throws Exception {
        Integer i = (Integer) server.invoke(studyPermissionServiceName,
                "grant", new Object[] { suid, actions, role }, new String[] {
                        String.class.getName(), String.class.getName(),
                        String.class.getName() });
        return i.intValue();
    }

    public boolean revoke(StudyPermissionDTO dto) throws Exception {
        Boolean b = (Boolean) server.invoke(studyPermissionServiceName,
                "revoke", new Object[] { dto },
                new String[] { StudyPermissionDTO.class.getName() });
        return b.booleanValue();
    }

    public Collection grantForPatient(long patPk, String actions, String role)
            throws Exception {
        return (Collection) server.invoke(studyPermissionServiceName,
                "grantForPatient", new Object[] { new Long(patPk), actions,
                        role }, new String[] { long.class.getName(),
                        String.class.getName(), String.class.getName() });
    }

    public Collection grantForPatient(String pid, String issuer, String actions,
            String role) throws Exception {
    	return (Collection) server.invoke(studyPermissionServiceName,
                "grantForPatient", new Object[] { pid, issuer, actions, role },
                new String[] { String.class.getName(), String.class.getName(),
                        String.class.getName(), String.class.getName() });
    }
    
    public Collection revokeForPatient(long patPk, String actions, String role)
            throws Exception {
    	return (Collection) server.invoke(studyPermissionServiceName,
                "revokeForPatient", new Object[] { new Long(patPk), actions,
                        role }, new String[] { long.class.getName(),
                        String.class.getName(), String.class.getName() });
    }

    public Collection revokeForPatient(String pid, String issuer, String actions,
            String role) throws Exception {
    	return (Collection) server.invoke(studyPermissionServiceName,
                "revokeForPatient", new Object[] { pid, issuer, actions, role },
                new String[] { String.class.getName(), String.class.getName(),
                        String.class.getName(), String.class.getName() });
    }
    
    public int countStudiesOfPatient(Long patPk) throws Exception {
        Integer i = (Integer) server.invoke(studyPermissionServiceName,
                "countStudiesOfPatient", new Object[] { patPk },
                new String[] { Long.class.getName() });
        return i.intValue();
    }
}
