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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocal;
import org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocalHome;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision: 3397 $ $Date: 2007-06-21 07:58:05 +0200 (Thu, 21 Jun 2007) $
 * @since Jun 11, 2007
 * 
 * @ejb.bean name="OtherPatientID" type="CMP" view-type="local" primkey-field="pk"
 *               local-jndi-name="ejb/OtherPatientID"
 * @ejb.persistence table-name="other_pid"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocal findByPatientIdAndIssuer(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT OBJECT(o) FROM OtherPatientID AS o WHERE o.patientId = ?1 AND o.issuerOfPatientId = ?2"
 */
public abstract class OtherPatientIDBean implements EntityBean {
    
    private static final Logger log = Logger.getLogger(OtherPatientIDBean.class);

    private EntityContext ctx;

    public void setEntityContext(EntityContext ctx) {
        this.ctx = ctx;     
    }

    public void unsetEntityContext() {
        this.ctx = null;        
    }
    
    /**
     * @ejb.home-method
     */
    public OtherPatientIDLocal ejbHomeValueOf(String pid, String issuer) {
        OtherPatientIDLocalHome opidhome = (OtherPatientIDLocalHome) ctx.getEJBLocalHome();
        try {
            try {
                return opidhome.findByPatientIdAndIssuer(pid, issuer);
            } catch (ObjectNotFoundException e) {
                return opidhome.create(pid, issuer);
            }
        } catch (Exception e) {
            throw new EJBException(e);
        }
    }
    
    /**
     * @ejb.create-method
     */
    public Long ejbCreate(String pid, String issuer) throws CreateException {
        setPatientId(pid);
        setIssuerOfPatientId(issuer);
        return null;
    }

    public void ejbPostCreate(String id, String issuer) throws CreateException {
        log.info("Created " + this);
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + this);
    }

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
     * @ejb.persistence column-name="pat_id"
     */
    public abstract String getPatientId();
    public abstract void setPatientId(String pid);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_id_issuer"
     */
    public abstract String getIssuerOfPatientId();
    public abstract void setIssuerOfPatientId(String issuer);

    /**
     * @ejb.interface-method
     * @ejb.relation name="patient-other-pid" role-name="other-pid-of-patients"
     * @jboss.relation-table table-name="rel_pat_other_pid"
     * @jboss.relation fk-column="patient_fk" related-pk-field="pk"     
     */
    public abstract java.util.Collection getPatients();
    public abstract void setPatients(java.util.Collection studies);

    public String toString() {
        return "OtherPatientId(id=" + getPatientId() 
                + ", issuer=" + getIssuerOfPatientId() + ")";
    }
    
}
