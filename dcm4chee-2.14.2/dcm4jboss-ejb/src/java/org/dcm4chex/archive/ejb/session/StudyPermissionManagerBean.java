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
package org.dcm4chex.archive.ejb.session;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;

import org.dcm4chex.archive.common.SecurityUtils;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionLocalHome;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Oct 15, 2007
 * 
 * @ejb.bean name="StudyPermissionManager" type="Stateless" view-type="remote"
 *           jndi-name="ejb/StudyPermissionManager"
 * 
 * @ejb.ejb-ref ejb-name="StudyPermission" view-type="local"
 *              ref-name="ejb/StudyPermission"
 */
public abstract class StudyPermissionManagerBean implements SessionBean {

    private StudyPermissionLocalHome studyPermissionHome;

    public void setSessionContext(SessionContext ctx) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            studyPermissionHome = (StudyPermissionLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/StudyPermission");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    public void unsetSessionContext() {
        studyPermissionHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public Collection findByPatientPk(Long pk) {
        try {
            return toDTOs(studyPermissionHome.findByPatientPk(pk));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Collection findByStudyIuid(String suid) {
        try {
            return toDTOs(studyPermissionHome.findByStudyIuid(suid));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Collection findByStudyIuidAndAction(String suid, String action) {
        try {
            return toDTOs(studyPermissionHome.findByStudyIuidAndAction(suid,
                    action));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public boolean hasPermission(String suid, String action, String role) {
        try {
            studyPermissionHome.find(suid, action, role);
            return true;
        } catch (ObjectNotFoundException onfe) {
            return false;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @ejb.interface-method
     */
    public boolean hasPermission(String suid, String action, Subject subject) {
        String[] roles = SecurityUtils.rolesOf(subject);
        for (int i = 0; i < roles.length; i++) {
            if (hasPermission(suid, action, roles[i])) {
                return true;                
            }
        }
        return false;
    }
    
    private Collection toDTOs(Collection c) {
        ArrayList dtos = new ArrayList(c.size());
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            dtos.add(((StudyPermissionLocal) iter.next()).toDTO());
        }
        return dtos;
    }

    /**
     * @ejb.interface-method
     */
    public boolean grant(String suid, String action, String role) {
        if (hasPermission(suid, action, role)) {
            return false;
        }
        try {
            studyPermissionHome.create(suid, action, role);
            return true;
        } catch (CreateException e) {
            if (hasPermission(suid, action, role)) {
                return false;
            }
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public int grant(String suid, String[] actions, String role) {
        int count = 0;
        for (int i = 0; i < actions.length; i++) {
            if (grant(suid, actions[i], role)) {
                ++count;
            }
        }
        return count;
    }
    
    /**
     * @ejb.interface-method
     */
    public boolean revoke(StudyPermissionDTO dto) {
        try {
            if (dto.getPk() != -1) {
                studyPermissionHome.remove(new Long(dto.getPk()));
                return true;
            } else {
                return revoke(dto.getStudyIuid(), dto.getAction(),
                        dto.getRole());
            }
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public boolean revoke(String suid, String action, String role)  {
        try {
            StudyPermissionLocal studyPermission;
            try {
                studyPermission = studyPermissionHome.find(suid, action, role);
            } catch (ObjectNotFoundException onfe) {
                return false;
            }
            studyPermission.remove();
            return true;
        } catch (FinderException e) {
            throw new EJBException(e);
        } catch (RemoveException e) {
            throw new EJBException(e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public int revoke(String suid, String[] actions, String role) {
        int count = 0;
        for (int i = 0; i < actions.length; i++) {
            if (revoke(suid, actions[i], role)) {
                ++count;
            }
        }
        return count;
    }
    
    /**
     * @ejb.interface-method
     * @return Collection of related Study Instance UIDs
     */
    public Collection grantForPatient(long patPk, String[] actions, String role) {
        Collection c;
        try {
            c = studyPermissionHome
                    .selectStudyIuidsByPatientPk(new Long(patPk));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        grant(c, actions, role);
        return c;
    }

    /**
     * @ejb.interface-method
     * @return Collection of related Study Instance UIDs
     */
    public Collection revokeForPatient(long patPk, String[] actions, String role) {
        Collection c;
        try {
            c = studyPermissionHome
                    .selectStudyIuidsByPatientPk(new Long(patPk));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        revoke(c, actions, role);
        return c;
    }

    /**
     * @ejb.interface-method
     * @return Collection of related Study Instance UIDs
     */
    public Collection grantForPatient(String pid, String issuer, String[] actions,
            String role) {
        Collection c;
        try {
            c = studyPermissionHome.selectStudyIuidsByPatientId(pid, issuer);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        grant(c, actions, role);
        return c;
    }

    private int grant(Collection suids, String[] actions, String role) {
        int count = 0;
        for (Iterator iter = suids.iterator(); iter.hasNext();) {
            count += grant((String) iter.next(), actions, role);
        }
        return count;
    }

    /**
     * @ejb.interface-method
     * @return Collection of related Study Instance UIDs
     */
    public Collection revokeForPatient(String pid, String issuer, String[] actions,
            String role) {
        Collection c;
        try {
            c = studyPermissionHome.selectStudyIuidsByPatientId(pid, issuer);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        revoke(c, actions, role);
        return c;
    }

    private int revoke(Collection suids, String[] actions, String role) {
        int count = 0;
        for (Iterator iter = suids.iterator(); iter.hasNext();) {
            count += revoke((String) iter.next(), actions, role);
        }
        return count;
    }

    /**
     * @ejb.interface-method
     */
    public int countStudiesOfPatient(Long patPk) {
        try {
            return studyPermissionHome.selectStudyIuidsByPatientPk(patPk).size();
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }
    
}
