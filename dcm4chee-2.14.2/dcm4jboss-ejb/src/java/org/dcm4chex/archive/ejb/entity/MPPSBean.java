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
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PPSStatus;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 5640 $ $Date: 2008-01-08 11:18:14 +0100 (Tue, 08 Jan 2008) $
 * @since 21.03.2004
 * 
 * @ejb.bean name="MPPS" type="CMP" view-type="local" primkey-field="pk"
 *           local-jndi-name="ejb/MPPS"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="mpps"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * @jboss.audit-updated-time field-name="updatedTime"
 * 
 * @ejb.finder signature="java.util.Collection findAll()"
 *             query="SELECT OBJECT(a) FROM MPPS AS a" transaction-type="Supports"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.MPPSLocal findBySopIuid(java.lang.String uid)"
 * 	           query="SELECT OBJECT(a) FROM MPPS AS a WHERE a.sopIuid = ?1"
 *             transaction-type="Supports"
 * 
 * @jboss.query signature="org.dcm4chex.archive.ejb.interfaces.MPPSLocal findBySopIuid(java.lang.String uid)"
 *              strategy="on-find" eager-load-group="*"
 *              
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * @ejb.ejb-ref ejb-name="Code" view-type="local" ref-name="ejb/Code"
 */
public abstract class MPPSBean implements EntityBean {
	private static final Logger log = Logger.getLogger(MPPSBean.class);

        private SeriesLocalHome seriesHome;

	private CodeLocalHome codeHome;

	public void setEntityContext(EntityContext ctx) {
		Context jndiCtx = null;
		try {
			jndiCtx = new InitialContext();
			seriesHome = (SeriesLocalHome) 
					jndiCtx.lookup("java:comp/env/ejb/Series");
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
		seriesHome = null;
		codeHome = null;
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
     * @ejb.persistence column-name="created_time"
     */
    public abstract java.sql.Timestamp getCreatedTime();

    public abstract void setCreatedTime(java.sql.Timestamp time);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="updated_time"
     */
    public abstract java.sql.Timestamp getUpdatedTime();

    public abstract void setUpdatedTime(java.sql.Timestamp time);
	
	/**
	 * SOP Instance UID
	 * 
	 * @ejb.persistence column-name="mpps_iuid"
	 * @ejb.interface-method
	 */
	public abstract String getSopIuid();

	public abstract void setSopIuid(String iuid);

    /**
     * PPS Start Datetime
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="pps_start"
     */
    public abstract java.sql.Timestamp getPpsStartDateTime();
    public abstract void setPpsStartDateTime(java.sql.Timestamp dateTime);

    /**
     * @ejb.interface-method
     */
    public void setPpsStartDateTime(java.util.Date date) {
        setPpsStartDateTime(date != null ? new java.sql.Timestamp(date
                .getTime()) : null);
    }
    
    /**
     * Station AET
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="station_aet"
     */
    public abstract String getPerformedStationAET();
    public abstract void setPerformedStationAET(String aet);

    /**
     * Modality
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="modality"
     */
    public abstract String getModality();
    public abstract void setModality(String md);

    /**
     * Modality
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="accession_no"
     */
    public abstract String getAccessionNumber();
    public abstract void setAccessionNumber(String md);

    /**
	 * MPPS Status
	 * 
	 * @ejb.persistence column-name="mpps_status"
	 */
	public abstract int getPpsStatusAsInt();

	public abstract void setPpsStatusAsInt(int status);

    /**
	 * MPPS DICOM Attributes
	 * 
	 * @ejb.persistence column-name="mpps_attrs"
	 */
	public abstract byte[] getEncodedAttributes();

	public abstract void setEncodedAttributes(byte[] bytes);

	/**
	 * @ejb.interface-method view-type="local"
	 * 
	 * @ejb.relation name="patient-mpps" role-name="mpps-of-patient"
	 *               cascade-delete="yes"
	 * 
	 * @jboss.relation fk-column="patient_fk" related-pk-field="pk"
	 */
	public abstract void setPatient(PatientLocal patient);

	/**
	 * @ejb.interface-method view-type="local"
	 * 
	 * @return patient of this mpps
	 */
	public abstract PatientLocal getPatient();

	/**
	 * @ejb.interface-method view-type="local"
	 */
	public abstract void setSeries(java.util.Collection series);

	/**
	 * @ejb.interface-method view-type="local"
	 * @ejb.relation name="mpps-series" role-name="mpps-has-series"
	 */
	public abstract java.util.Collection getSeries();

	/**
	 * @ejb.relation name="mpps-drcode" role-name="mpps-with-drcode"
	 *               target-ejb="Code" target-role-name="drcode-of-mpps"
	 *               target-multiple="yes"
	 * 
	 * @jboss.relation fk-column="drcode_fk" related-pk-field="pk"
	 */
	public abstract void setDrCode(CodeLocal srCode);

	/**
	 * @ejb.interface-method view-type="local"
	 */
	public abstract CodeLocal getDrCode();

	/**
	 * Create Instance.
	 * 
	 * @ejb.create-method
	 */
	public Long ejbCreate(Dataset ds, PatientLocal patient)
			throws CreateException {
		setSopIuid(ds.getString(Tags.SOPInstanceUID));
		return null;
	}

	public void ejbPostCreate(Dataset ds, PatientLocal patient)
			throws CreateException {
		setPatient(patient);
		setAttributes(ds);
		try {
			setSeries(seriesHome.findByPpsIuid(getSopIuid()));
		} catch (FinderException e) {
			throw new EJBException(e);
		}
		log.info("Created " + asString());
	}

	public void ejbRemove() throws RemoveException {
		log.info("Deleting " + asString());
	}

	/**
	 * @ejb.interface-method
	 */
	public boolean isInProgress() {
		return getPpsStatusAsInt() == PPSStatus.IN_PROGRESS;
	}

	/**
	 * @ejb.interface-method
	 */
	public boolean isCompleted() {
		return getPpsStatusAsInt() == PPSStatus.COMPLETED;
	}

	/**
	 * @ejb.interface-method
	 */
	public boolean isDiscontinued() {
		return getPpsStatusAsInt() == PPSStatus.DISCONTINUED;
	}

	/**
	 * @ejb.interface-method
	 */
	public String getPpsStatus() {
		return PPSStatus.toString(getPpsStatusAsInt());
	}

	public void setPpsStatus(String status) {
		setPpsStatusAsInt(PPSStatus.toInt(status));
	}

        /**
         * @ejb.interface-method
         */
	public String asString() {
		return "MPPS[pk=" + getPk() + ", iuid=" + getSopIuid() + ", status="
				+ getPpsStatus() + ", patient->" + getPatient() + "]";
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
		setPpsStartDateTime(ds.getDateTime(Tags.PPSStartDate, Tags.PPSStartTime));
		setPerformedStationAET(ds.getString(Tags.PerformedStationAET));
		setModality(ds.getString(Tags.Modality));
		setPpsStatus(ds.getString(Tags.PPSStatus));
		Dataset ssa = ds.getItem(Tags.ScheduledStepAttributesSeq);
		setAccessionNumber(ssa.getString(Tags.AccessionNumber));
		try {
			setDrCode(CodeBean.valueOf(codeHome, ds
					.getItem(Tags.PPSDiscontinuationReasonCodeSeq)));
		} catch (CreateException e) {
			throw new EJBException(e);
		} catch (FinderException e) {
			throw new EJBException(e);
		}
        byte[] b = DatasetUtils.toByteArray(ds,
                UIDs.DeflatedExplicitVRLittleEndian);
        if (log.isDebugEnabled()) {
            log.debug("setEncodedAttributes(byte[" + b.length + "])");
        }
        setEncodedAttributes(b);
	}

	/**
	 * @ejb.interface-method
	 */
	public boolean isIncorrectWorklistEntrySelected() {
		CodeLocal drcode = getDrCode();
		return drcode != null && "110514".equals(drcode.getCodeValue())
				&& "DCM".equals(drcode.getCodingSchemeDesignator());
	}

        /**
         * @ejb.select query="SELECT DISTINCT mpps.sopIuid FROM MPPS mpps, IN(mpps.series) s WHERE s.study.studyIuid = ?1"
         */ 
        public abstract Collection ejbSelectMppsIuidsByStudyIuid(String suid)
                throws FinderException;
        
        
        /**
         * @ejb.home-method
         */
        public Collection ejbHomeMppsIuidsByStudyIuid(String suid)
                throws FinderException {
            return ejbSelectMppsIuidsByStudyIuid(suid);            
        }

        /**
         * @ejb.select query="SELECT DISTINCT OBJECT(mpps) FROM MPPS mpps, IN(mpps.series) s WHERE s.study.studyIuid = ?1"
         */ 
        public abstract Collection ejbSelectMppsByStudyIuid(String suid)
                throws FinderException;
        
        
        /**
         * @ejb.home-method
         */
        public Collection ejbHomeMppsByStudyIuid(String suid)
                throws FinderException {
            return ejbSelectMppsByStudyIuid(suid);            
        }
}
