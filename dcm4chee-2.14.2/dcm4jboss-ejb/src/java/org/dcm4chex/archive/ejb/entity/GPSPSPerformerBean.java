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
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.GPSPSLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2006-09-14 10:27:39 +0200 (Thu, 14 Sep 2006) $
 * @since 01.04.2005
 * 
 * @ejb.bean name="GPSPSPerformer" type="CMP" view-type="local"
 *           local-jndi-name="ejb/GPSPSPerformer" primkey-field="pk"
 * @ejb.persistence table-name="gpsps_perf"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @ejb.ejb-ref ejb-name="Code" view-type="local" ref-name="ejb/Code"
 */

public abstract class GPSPSPerformerBean implements EntityBean {

    private static final Logger log = Logger.getLogger(GPSPSPerformerBean.class);

    private CodeLocalHome codeHome;

    public void setEntityContext(EntityContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            codeHome = (CodeLocalHome) jndiCtx.lookup("java:comp/env/ejb/Code");
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
    public Long ejbCreate(Dataset ds, GPSPSLocal gpsps)
            throws CreateException {
        PersonName pn = ds.getPersonName(Tags.HumanPerformerName);
        if (pn != null) {
            setHumanPerformerName(toUpperCase(pn.toComponentGroupString(false)));
            PersonName ipn = pn.getIdeographic();
            if (ipn != null) {
                setHumanPerformerIdeographicName(ipn.toComponentGroupString(false));
            }
            PersonName ppn = pn.getPhonetic();
            if (ppn != null) {
                setHumanPerformerPhoneticName(ppn.toComponentGroupString(false));
            }
        }        
        return null;
    }

    private static String toUpperCase(String s) {
        return s != null ? s.toUpperCase() : null;
    }
    
    public void ejbPostCreate(Dataset ds, GPSPSLocal gpsps)
            throws CreateException {
        try {
            setHumanPerformerCode(CodeBean.valueOf(codeHome, ds
                    .getItem(Tags.HumanPerformerCodeSeq)));
        } catch (CreateException e) {
            throw new CreateException(e.getMessage());
        } catch (FinderException e) {
            throw new CreateException(e.getMessage());
        }
        setGpsps(gpsps);
        log.info("Created " + prompt());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
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
     * @ejb.interface-method
     * @ejb.persistence column-name="human_perf_name"
     */
    public abstract String getHumanPerformerName();
    public abstract void setHumanPerformerName(String name);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="hum_perf_i_name"
     */
    public abstract String getHumanPerformerIdeographicName();
    public abstract void setHumanPerformerIdeographicName(String name);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="hum_perf_p_name"
     */
    public abstract String getHumanPerformerPhoneticName();
    public abstract void setHumanPerformerPhoneticName(String name);

    /**
     * @ejb.relation name="human-performer-code"
     *               role-name="human-performer-with-code"
     *               target-ejb="Code"
     *               target-role-name="code-of-human-performer"
     *               target-multiple="yes"
     * @jboss.relation fk-column="code_fk" related-pk-field="pk"
     */
    public abstract CodeLocal getHumanPerformerCode();
    public abstract void setHumanPerformerCode(CodeLocal id);

    /**
     * @ejb.interface-method
     * @ejb.relation name="gpsps-human-performer" role-name="human-performer-for-gpsps"
     *               cascade-delete="yes"
     * @jboss.relation fk-column="gpsps_fk" related-pk-field="pk"
     */
    public abstract GPSPSLocal getGpsps();
    public abstract void setGpsps(GPSPSLocal gpsps);

    private String prompt() {
        return "GPSPSHumanPerformer[pk=" + getPk() 
                + ", name=" + getHumanPerformerName() 
                + ", code->" + getHumanPerformerCode()
                + ", gpsps->" + getGpsps() + "]";
    }
    
}
