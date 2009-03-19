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

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.SPSStatus;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;

/**
 * @ejb.bean name="MWLItem" type="CMP" view-type="local"
 *  primkey-field="pk" local-jndi-name="ejb/MWLItem"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="mwl_item"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * @jboss.audit-updated-time field-name="updatedTime"
 * 
 * @ejb.env-entry name="SpsIdPrefix" type="java.lang.String" value="S" 
 * @ejb.env-entry name="RpIdPrefix" type="java.lang.String" value="P" 
 *
 * @ejb.finder signature="Collection findAll()"
 *  query="SELECT OBJECT(a) FROM MWLItem AS a"
 *  transaction-type="Supports"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.MWLItemLocal findByRpIdAndSpsId(java.lang.String rpid, java.lang.String spsid)"
 *  query="SELECT OBJECT(a) FROM MWLItem AS a WHERE a.requestedProcedureId = ?1 AND a.spsId = ?2"
 *  transaction-type="Supports"
 *
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public abstract class MWLItemBean implements EntityBean {
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final Logger log = Logger.getLogger(MWLItemBean.class);
    private String spsIdPrefix;
    private String rpIdPrefix;

    public void setEntityContext(EntityContext arg0)
        throws EJBException, RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            spsIdPrefix = (String) jndiCtx.lookup("java:comp/env/SpsIdPrefix");
            rpIdPrefix = (String) jndiCtx.lookup("java:comp/env/RpIdPrefix");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {}
            }
        }
    }

    /**
     * Auto-generated Primary Key
     *
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence
     *  column-name="pk"
     * @jboss.persistence
     *  auto-increment="true"
     *
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);

    /**
     * @ejb.persistence column-name="created_time"
     */
    public abstract java.sql.Timestamp getCreatedTime();
    public abstract void setCreatedTime(java.sql.Timestamp time);

    /**
     * @ejb.persistence column-name="updated_time"
     */
    public abstract java.sql.Timestamp getUpdatedTime();
    public abstract void setUpdatedTime(java.sql.Timestamp time);
	
    /**
     * @ejb.persistence column-name="sps_id"
     */
    public abstract String getSpsId();
    public abstract void setSpsId(String spsId);

    /**
     * @ejb.persistence column-name="start_datetime"
     */
    public abstract java.sql.Timestamp getSpsStartDateTime();

    public abstract void setSpsStartDateTime(java.sql.Timestamp dateTime);

    /**
     * @ejb.persistence column-name="station_aet"
     */
    public abstract String getScheduledStationAET();
    public abstract void setScheduledStationAET(String aet);

    /**
     * @ejb.persistence column-name="station_name"
     */
    public abstract String getScheduledStationName();
    public abstract void setScheduledStationName(String station);

    /**
     * @ejb.persistence column-name="modality"
     */
    public abstract String getModality();
    public abstract void setModality(String md);

    /**
     * @ejb.persistence column-name="perf_physician"
     */
    public abstract String getPerformingPhysicianName();
    public abstract void setPerformingPhysicianName(String name);

    /**
     * @ejb.persistence column-name="perf_phys_i_name"
     */
    public abstract String getPerformingPhysicianIdeographicName();
    public abstract void setPerformingPhysicianIdeographicName(String name);

    /**
     * @ejb.persistence column-name="perf_phys_p_name"
     */
    public abstract String getPerformingPhysicianPhoneticName();
    public abstract void setPerformingPhysicianPhoneticName(String name);

    /**
     * @ejb.persistence column-name="req_proc_id"
     */
    public abstract String getRequestedProcedureId();

    public abstract void setRequestedProcedureId(String id);

    /**
     * @ejb.persistence column-name="accession_no"
     */
    public abstract String getAccessionNumber();
    public abstract void setAccessionNumber(String no);

    /**
     * @ejb.persistence column-name="study_iuid"
     */
    public abstract String getStudyIuid();
    public abstract void setStudyIuid(String uid);
    
    /**
     * @ejb.persistence column-name="item_attrs"
     */
    public abstract byte[] getEncodedAttributes();

    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.relation name="patient-mwlitems" role-name="mwlitem-of-patient"
     *  cascade-delete="yes"
     * @jboss.relation fk-column="patient_fk" related-pk-field="pk"
     */
    public abstract void setPatient(PatientLocal patient);

    /**
     * @ejb.interface-method
     */
    public abstract PatientLocal getPatient();

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, PatientLocal patient) throws CreateException {
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(Dataset ds, PatientLocal patient) throws CreateException {
        setPatient(patient);
        addMissingIds(ds);
        log.info("Created " + prompt());
    }

    private void addMissingIds(Dataset ds) {
        boolean dirty = false;
        if (ds.getString(Tags.RequestedProcedureID) == null) {
            ds.putSH(Tags.RequestedProcedureID, rpIdPrefix + getPk());
            dirty = true;
        }
        Dataset spsItem = ds.getItem(Tags.SPSSeq);
        if (spsItem.getString(Tags.SPSID) == null) {
            spsItem.putSH(Tags.SPSID, spsIdPrefix + getPk());
            dirty = true;
        }
        if (dirty) {
            setAttributes(ds);            
        }
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
    }

    /**
     * @ejb.interface-method
     */
    public Dataset getAttributes() {
        return DatasetUtils.fromByteArray(getEncodedAttributes());
    }

    /**
     * @ejb.interface-method
     */
    public void setAttributes(Dataset ds) {
    	
        Dataset spsItem = ds.getItem(Tags.SPSSeq);
        if (spsItem == null) {
            throw new IllegalArgumentException("Missing Scheduled Procedure Step Sequence (0040,0100) Item");
        }
        setSpsId(spsItem.getString(Tags.SPSID));
        setSpsStatus(spsItem.getString(Tags.SPSStatus, "SCHEDULED"));
        setSpsStartDateTime(
            spsItem.getDateTime(Tags.SPSStartDate, Tags.SPSStartTime));
        setScheduledStationAET(spsItem.getString(Tags.ScheduledStationAET));
        setScheduledStationName(spsItem.getString(Tags.ScheduledStationName));
        PersonName pn = spsItem.getPersonName(Tags.PerformingPhysicianName);
        if (pn != null) {
            setPerformingPhysicianName(toUpperCase(pn.toComponentGroupString(false)));
            PersonName ipn = pn.getIdeographic();
            if (ipn != null) {
                setPerformingPhysicianIdeographicName(ipn.toComponentGroupString(false));
            }
            PersonName ppn = pn.getPhonetic();
            if (ppn != null) {
                setPerformingPhysicianPhoneticName(ppn.toComponentGroupString(false));
             }
        }
        setModality(spsItem.getString(Tags.Modality));
        setRequestedProcedureId(ds.getString(Tags.RequestedProcedureID));
        setAccessionNumber(ds.getString(Tags.AccessionNumber));
        setStudyIuid(ds.getString(Tags.StudyInstanceUID));
        byte[] b = DatasetUtils.toByteArray(ds,
                UIDs.DeflatedExplicitVRLittleEndian);
        if (log.isDebugEnabled()) {
            log.debug("setEncodedAttributes(byte[" + b.length + "])");
        }
        setEncodedAttributes(b);
    }

    private static String toUpperCase(String s) {
        return s != null ? s.toUpperCase() : null;
    }
    
    private void setSpsStartDateTime(java.util.Date date) {
        setSpsStartDateTime(date != null ? new java.sql.Timestamp(date.getTime()) : null);
    }

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="sps_status"
     */
    public abstract int getSpsStatusAsInt();
    public abstract void setSpsStatusAsInt(int status);

    /**
     * @ejb.interface-method
     */
    public String getSpsStatus() {
        return SPSStatus.toString(getSpsStatusAsInt());
    }

    public void setSpsStatus(String status) {
        setSpsStatusAsInt(SPSStatus.toInt(status));
    }

    /**
     * @ejb.interface-method
     */
    public void updateSpsStatus(int status) {
    	if (status == getSpsStatusAsInt())
    		return;
    	Dataset ds = getAttributes();
        Dataset spsItem = ds.getItem(Tags.SPSSeq);
        spsItem.putCS(Tags.SPSStatus, SPSStatus.toString(status));
        setSpsStatusAsInt(status);
        byte[] b = DatasetUtils.toByteArray(ds,
                UIDs.DeflatedExplicitVRLittleEndian);
        if (log.isDebugEnabled()) {
            log.debug("setEncodedAttributes(byte[" + b.length + "])");
        }
        setEncodedAttributes(b);
    }
    
    /**
     * 
     * @ejb.interface-method
     */
    public String asString() {
        return prompt();
    }

    private String prompt() {
        java.sql.Timestamp spsDT = getSpsStartDateTime();
        return "MWLItem[pk="
            + getPk()
            + ", spsId="
            + getSpsId()
            + ", spsStartDateTime="
            + (spsDT != null ? new SimpleDateFormat(DATE_FORMAT).format(spsDT) : "")
            + ", spsStatus="
            + getSpsStatus()
            + ", stationAET="
            + getScheduledStationAET()
            + ", rqProcId="
            + getRequestedProcedureId()
            + ", modality="
            + getModality()
            + ", accessionNo="
            + getAccessionNumber()
            + ", patient->"
            + getPatient()
            + "]";
    }

}
