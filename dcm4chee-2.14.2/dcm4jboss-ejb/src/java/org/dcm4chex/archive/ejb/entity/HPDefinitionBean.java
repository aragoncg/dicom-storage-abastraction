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

package org.dcm4chex.archive.ejb.entity;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.HPLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2754 $ $Date: 2006-09-14 10:27:39 +0200 (Thu, 14 Sep 2006) $
 * @since Aug 2, 2005
 * 
 * @ejb.bean name="HPDefinition" type="CMP" view-type="local"
 *           local-jndi-name="ejb/HPDefinition" primkey-field="pk"
 * @ejb.persistence table-name="hpdef"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.ejb-ref ejb-name="Code" view-type="local" ref-name="ejb/Code"
 */
public abstract class HPDefinitionBean implements EntityBean {

    private static final Logger log = Logger.getLogger(HPDefinitionBean.class);

    private CodeLocalHome codeHome;

    public void setEntityContext(EntityContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            codeHome = (CodeLocalHome)
                    jndiCtx.lookup("java:comp/env/ejb/Code");
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

    public void unsetEntityContext() {
        codeHome = null;
    }
	
    /**
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, HPLocal hp)
            throws CreateException {
		setModality(ds.getString(Tags.Modality));
		setLaterality(ds.getString(Tags.Laterality));
        return null;
    }
    
    public void ejbPostCreate(Dataset ds, HPLocal hp)
            throws CreateException {
        setHP(hp);
        try {
            initCodes(ds.get(Tags.AnatomicRegionSeq),
                    getAnatomicRegionCodes());
            initCodes(ds.get(Tags.ProcedureCodeSeq),
                    getProcedureCodes());
            initCodes(ds.get(Tags.ReasonforRequestedProcedureCodeSeq),
                    getReasonforRequestedProcedureCodes());
        } catch (CreateException e) {
            throw new CreateException(e.getMessage());
        } catch (FinderException e) {
            throw new CreateException(e.getMessage());
        }
        log.info("Created " + toString());
    }

    private void initCodes(DcmElement sq, Collection c) throws CreateException,
			FinderException {
		if (sq == null)
			return;
		for (int i = 0, n = sq.countItems(); i < n; i++) {
			c.add(CodeBean.valueOf(codeHome, sq.getItem(i)));
		}
	}
	
    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + toString());
    }
    
    /**
     * Auto-generated Primary Key
     * 
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     *  
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);
    
    /**
     * @ejb.relation name="hp-definition" role-name="definition-for-hp"
     *               cascade-delete="yes"
     * @jboss.relation fk-column="hp_fk" related-pk-field="pk"
     */
    public abstract void setHP(HPLocal hp);

    /**
     * @ejb.interface-method
     */
    public abstract HPLocal getHP();
	
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="modality"
     */
    public abstract String getModality();

    public abstract void setModality(String md);
	
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="laterality"
     */
    public abstract String getLaterality();

    public abstract void setLaterality(String laterality);

    /**
     * @ejb.relation name="hpdef-regioncode" role-name="hpdef-with-regioncode"
     *               target-ejb="Code" target-role-name="regioncode-of-hpdef"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_hpdef_region"
     * @jboss.relation fk-column="region_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="hpdef_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getAnatomicRegionCodes();
    public abstract void setAnatomicRegionCodes(java.util.Collection codes);
	
    /**
     * @ejb.relation name="hpdef-proccode" role-name="hpdef-with-proccode"
     *               target-ejb="Code" target-role-name="proccode-of-hpdef"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_hpdef_proc"
     * @jboss.relation fk-column="proc_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="hpdef_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getProcedureCodes();
    public abstract void setProcedureCodes(java.util.Collection codes);
	
    /**
     * @ejb.relation name="hpdef-reasoncode" role-name="hpdef-with-reasoncode"
     *               target-ejb="Code" target-role-name="reasoncode-of-hpdef"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_hpdef_reason"
     * @jboss.relation fk-column="reason_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="hpdef_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getReasonforRequestedProcedureCodes();
    public abstract void setReasonforRequestedProcedureCodes(java.util.Collection codes);
	
    public String toString() {
        return "HPDefinition[pk=" + getPk() + ", hp->" + getHP() + "]";
    }
	
}
