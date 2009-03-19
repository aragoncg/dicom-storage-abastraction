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

package org.dcm4chex.archive.ejb.entity;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 5317 $ $Date: 2007-10-16 09:01:33 +0000 (Tue, 16 Oct
 *          2007) $
 * @since Jun 29, 2007
 * 
 * @ejb.bean name="StudyPermission" type="CMP" view-type="local"
 *           local-jndi-name="ejb/StudyPermission" primkey-field="pk"
 * @ejb.persistence table-name="study_permission"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.StudyPermissionLocal find(java.lang.String suid, java.lang.String action, java.lang.String role)"
 *             query="SELECT OBJECT(p) FROM StudyPermission AS p WHERE p.studyIuid = ?1 AND p.action = ?2 AND p.role = ?3" transaction-type="Supports"
 * 
 * @ejb.finder signature="java.util.Collection findByStudyIuidAndAction(java.lang.String suid, java.lang.String action)"
 *             query="SELECT OBJECT(p) FROM StudyPermission AS p WHERE p.studyIuid = ?1 AND p.action = ?2" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByStudyIuidAndAction(java.lang.String suid, java.lang.String action)"
 *              strategy="on-find" eager-load-group="*"
 * 
 * @ejb.finder signature="java.util.Collection findByStudyIuid(java.lang.String suid)"
 *             query="SELECT OBJECT(p) FROM StudyPermission AS p WHERE p.studyIuid = ?1" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByStudyIuid(java.lang.String suid)"
 *              strategy="on-find" eager-load-group="*"
 * 
 * @ejb.finder signature="java.util.Collection findByPatientPk(java.lang.Long pk)"
 *             query="SELECT OBJECT(sp) FROM Patient p, IN(p.studies) s, StudyPermission sp WHERE p.pk = ?1 AND s.studyIuid = sp.studyIuid"
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByPatientPk(java.lang.Long pk)"
 *              strategy="on-find" eager-load-group="*"
 */
public abstract class StudyPermissionBean implements EntityBean {

    private static final Logger log = Logger
            .getLogger(StudyPermissionBean.class);

    /**
     * Auto-generated Primary Key
     * 
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="study_iuid"
     */
    public abstract String getStudyIuid();

    public abstract void setStudyIuid(String uid);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="action"
     */
    public abstract String getAction();

    public abstract void setAction(String action);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="roles"
     */
    public abstract String getRole();

    public abstract void setRole(String role);

    /**
     * @ejb.interface-method
     */
    public StudyPermissionDTO toDTO() {
        StudyPermissionDTO dto = new StudyPermissionDTO();
        dto.setPk(getPk().longValue());
        dto.setStudyIuid(getStudyIuid());
        dto.setAction(getAction());
        dto.setRole(getRole());
        return dto;
    }

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(String suid, String action, String role)
            throws CreateException {
        setStudyIuid(suid);
        setAction(action);
        setRole(role);
        return null;
    }

    public void ejbPostCreate(String suid, String action, String role)
            throws CreateException {
        log.info("Created " + prompt());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
    }

    private String prompt() {
        return "StudyPermission[pk=" + getPk() + ", suid=" + getStudyIuid()
                + ", action=" + getAction() + ", role=" + getRole() + "]";
    }

    /**
     * @ejb.home-method
     */
    public Collection ejbHomeSelectStudyIuidsByPatientPk(Long pk)
            throws FinderException {
        return ejbSelectStudyIuids(pk);
    }

    /**
     * @ejb.home-method
     */
    public Collection ejbHomeSelectStudyIuidsByPatientId(String pid,
            String issuer) throws FinderException {
        return issuer != null && issuer.length() != 0
                ? ejbSelectStudyIuids(pid, issuer)
                : ejbSelectStudyIuids(pid);
    }

    /**
     * @ejb.select query="SELECT s.studyIuid FROM Patient p, IN(p.studies) s WHERE p.pk = ?1"
     */
    public abstract Collection ejbSelectStudyIuids(Long pk)
            throws FinderException;

    /**
     * @ejb.select query="SELECT s.studyIuid FROM Patient p, IN(p.studies) s WHERE p.patientId = ?1"
     */
    public abstract Collection ejbSelectStudyIuids(String pid)
            throws FinderException;

    /**
     * @ejb.select query="SELECT s.studyIuid FROM Patient p, IN(p.studies) s WHERE p.patientId = ?1 AND (p.issuerOfPatientId IS NULL OR p.issuerOfPatientId = ?2)"
     */
    public abstract Collection ejbSelectStudyIuids(String pid, String issuer)
            throws FinderException;

}
