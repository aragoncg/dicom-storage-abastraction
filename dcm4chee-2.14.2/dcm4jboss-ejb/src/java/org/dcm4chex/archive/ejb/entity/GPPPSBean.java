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
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PPSStatus;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;

/**
 * @author gunter zeilinger(gunterze@gmail.com)
 * @version $Revision: 2754 $ $Date: 2006-09-14 10:27:39 +0200 (Thu, 14 Sep 2006) $
 * @since Apr 9, 2006
 *
 * @ejb.bean name="GPPPS" type="CMP" view-type="local" primkey-field="pk"
 *           local-jndi-name="ejb/GPPPS"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="gppps"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * @jboss.audit-updated-time field-name="updatedTime"
 * 
 * @ejb.finder signature="java.util.Collection findAll()"
 *             query="SELECT OBJECT(a) FROM GPPPS AS a" transaction-type="Supports"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.GPPPSLocal findBySopIuid(java.lang.String uid)"
 *             query="SELECT OBJECT(a) FROM GPPPS AS a WHERE a.sopIuid = ?1"
 *             transaction-type="Supports"
 * 
 * @jboss.query signature="org.dcm4chex.archive.ejb.interfaces.GPPPSLocal findBySopIuid(java.lang.String uid)"
 *              strategy="on-find" eager-load-group="*"
 */
public abstract class GPPPSBean implements EntityBean {
    private static final Logger log = Logger.getLogger(GPPPSBean.class);

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
     * @ejb.persistence column-name="pps_iuid"
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
     * @ejb.interface-method
     * @ejb.persistence column-name="pps_status"
     */
    public abstract int getPpsStatusAsInt();

    public abstract void setPpsStatusAsInt(int status);

    /**
     * @ejb.persistence column-name="pps_attrs"
     */
    public abstract byte[] getEncodedAttributes();

    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.interface-method view-type="local"
     * 
     * @ejb.relation name="patient-gppps" role-name="gppps-of-patient"
     *               cascade-delete="yes"
     * 
     * @jboss.relation fk-column="patient_fk" related-pk-field="pk"
     */
    public abstract void setPatient(PatientLocal patient);

    /**
     * @ejb.interface-method view-type="local"
     * 
     * @return patient of this gppps
     */
    public abstract PatientLocal getPatient();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setGpsps(java.util.Collection gpsps);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="gpsps-gppps" role-name="gppps-results-from-gpsps"
     * @jboss.relation-table table-name="rel_gpsps_gppps"
     * @jboss.relation fk-column="gpsps_fk" related-pk-field="pk"     
     */
    public abstract java.util.Collection getGpsps();

    /**
     * Create Instance.
     * 
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, PatientLocal patient)
            throws CreateException {
        setSopIuid(ds.getString(Tags.SOPInstanceUID));
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(Dataset ds, PatientLocal patient)
            throws CreateException {
        setPatient(patient);
        log.info("Created " + prompt());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
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

    private String prompt() {
        return "GPPPS[pk=" + getPk() + ", iuid=" + getSopIuid() + ", status="
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
        setPpsStatus(ds.getString(Tags.GPPPSStatus));
        byte[] b = DatasetUtils.toByteArray(ds,
                UIDs.DeflatedExplicitVRLittleEndian);
        if (log.isDebugEnabled()) {
            log.debug("setEncodedAttributes(byte[" + b.length + "])");
        }
        setEncodedAttributes(b);
    }
}
