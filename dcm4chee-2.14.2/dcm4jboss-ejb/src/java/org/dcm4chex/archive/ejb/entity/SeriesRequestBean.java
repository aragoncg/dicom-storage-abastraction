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
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2007-08-29 11:58:47 +0200 (Wed, 29 Aug 2007) $
 * @since 01.04.2005
 * 
 * @ejb.bean name="SeriesRequest" type="CMP" view-type="local"
 *           local-jndi-name="ejb/SeriesRequest" primkey-field="pk"
 * @ejb.persistence table-name="series_req"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 */

public abstract class SeriesRequestBean implements EntityBean {

    private static final Logger log = Logger.getLogger(SeriesRequestBean.class);

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, SeriesLocal series)
            throws CreateException {
        setStudyIuid(ds.getString(Tags.StudyInstanceUID));
        setRequestedProcedureId(ds.getString(Tags.RequestedProcedureID));
        setSpsId(ds.getString(Tags.SPSID));
        setRequestingService(ds.getString(Tags.RequestingService));
        PersonName pn = ds.getPersonName(Tags.RequestingPhysician);
        if (pn != null) {
            setRequestingPhysician(
                    toUpperCase(pn.toComponentGroupString(false)));
            PersonName ipn = pn.getIdeographic();
            if (ipn != null) {
                setRequestingPhysicianIdeographicName(
                        ipn.toComponentGroupString(false));                
            }
            PersonName ppn = pn.getPhonetic();
            if (ppn != null) {
                setRequestingPhysicianPhoneticName(
                        ppn.toComponentGroupString(false));                
            }
        }
        return null;
    }

    private static String toUpperCase(String s) {
        return s != null ? s.toUpperCase() : null;
    }
    
    public void ejbPostCreate(Dataset ds, SeriesLocal series)
            throws CreateException {
        setSeries(series);
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
     * @ejb.persistence column-name="study_iuid"
     */
    public abstract String getStudyIuid();

    public abstract void setStudyIuid(String uid);
   
    /**
     * @ejb.persistence column-name="req_proc_id"
     */
    public abstract String getRequestedProcedureId();

    public abstract void setRequestedProcedureId(String id);

    /**
     * @ejb.persistence column-name="sps_id"
     */
    public abstract String getSpsId();

    public abstract void setSpsId(String no);

    /**
     * @ejb.persistence column-name="req_service"
     */
    public abstract String getRequestingService();

    public abstract void setRequestingService(String service);

    /**
     * @ejb.persistence column-name="req_physician"
     */
    public abstract String getRequestingPhysician();
    public abstract void setRequestingPhysician(String name);

    /**
     * @ejb.persistence column-name="req_phys_i_name"
     */
    public abstract String getRequestingPhysicianIdeographicName();
    public abstract void setRequestingPhysicianIdeographicName(String name);

    /**
     * @ejb.persistence column-name="req_phys_p_name"
     */
    public abstract String getRequestingPhysicianPhoneticName();
    public abstract void setRequestingPhysicianPhoneticName(String name);
    
    /**
     * @ejb.relation name="series-request-attributes"
     *               role-name="request-attributes-of-series"
     *               cascade-delete="yes"
     * @jboss.relation fk-column="series_fk" related-pk-field="pk"
     */
    public abstract void setSeries(SeriesLocal series);

    /**
     * @ejb.interface-method
     */
    public abstract SeriesLocal getSeries();

    private String prompt() {
        return "SeriesRequestAttribute[pk=" + getPk() 
                + ", rpid=" + getRequestedProcedureId()
                + ", spsid=" + getSpsId()
                + ", service=" + getRequestingService()
                + ", phys=" + getRequestingPhysician()
                + ", series->" + getSeries() + "]";
    }
    
}
