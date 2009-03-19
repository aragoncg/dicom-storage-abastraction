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
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.GPSPSPriority;
import org.dcm4chex.archive.common.GPSPSStatus;
import org.dcm4chex.archive.common.InputAvailabilityFlag;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.GPSPSLocal;
import org.dcm4chex.archive.ejb.interfaces.GPSPSPerformerLocalHome;
import org.dcm4chex.archive.ejb.interfaces.GPSPSRequestLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-09-11 13:06:07 +0200 (Thu, 11 Sep 2008) $
 * @since 28.03.2005
 * 
 * @ejb.bean name="GPSPS" type="CMP" view-type="local"
 *           local-jndi-name="ejb/GPSPS" primkey-field="pk"
 * @ejb.persistence table-name="gpsps"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @ejb.env-entry name="SpsIdPrefix" type="java.lang.String" value=""
 * @ejb.ejb-ref ejb-name="Code" view-type="local" ref-name="ejb/Code"
 * @ejb.ejb-ref ejb-name="GPSPSRequest" view-type="local" ref-name="ejb/Request"
 * @ejb.ejb-ref ejb-name="GPSPSPerformer" view-type="local" ref-name="ejb/Performer"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.GPSPSLocal findBySopIuid(java.lang.String uid)"
 *             query="SELECT OBJECT(gpsps) FROM GPSPS AS gpsps WHERE gpsps.sopIuid = ?1"
 *             transaction-type="Supports"
 * @ejb.finder signature="java.util.Collection findByReqProcId(int status, java.lang.String codeValue, java.lang.String codingScheme, java.lang.String rpid)"
 *             query="SELECT OBJECT(gpsps) FROM GPSPS AS gpsps, IN(gpsps.refRequests) AS rq WHERE gpsps.gpspsStatusAsInt = ?1 AND gpsps.scheduledWorkItemCode.codeValue = ?2 AND gpsps.scheduledWorkItemCode.codingSchemeDesignator = ?3 AND rq.requestedProcedureId = ?4"
 *             transaction-type="Supports"
 */

public abstract class GPSPSBean implements EntityBean {

    private static final Logger log = Logger.getLogger(GPSPSBean.class);

    private static java.sql.Timestamp toTimestamp(java.util.Date date) {
        return date != null ? new java.sql.Timestamp(date.getTime()) : null;
    }
    
    private String spsIdPrefix;
    private EntityContext ejbctx;
    private CodeLocalHome codeHome;
    private GPSPSRequestLocalHome rqHome;
    private GPSPSPerformerLocalHome performerHome;

    public void setEntityContext(EntityContext ctx) {
        ejbctx = ctx;
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            spsIdPrefix = (String) jndiCtx.lookup("java:comp/env/SpsIdPrefix");
            codeHome = (CodeLocalHome)
                    jndiCtx.lookup("java:comp/env/ejb/Code");
            rqHome = (GPSPSRequestLocalHome) 
                    jndiCtx.lookup("java:comp/env/ejb/Request");
            performerHome = (GPSPSPerformerLocalHome)
                    jndiCtx.lookup("java:comp/env/ejb/Performer");
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
        rqHome = null;
        performerHome = null;
        ejbctx = null;
    }
    
    /**
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, PatientLocal patient) throws CreateException {
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(Dataset ds, PatientLocal patient)
    throws CreateException {
        if (ds.getString(Tags.SPSID) == null) {
            String id = spsIdPrefix + getPk();
            ds.putSH(Tags.SPSID, id);
            setEncodedAttributes(DatasetUtils.toByteArray(ds,
                    UIDs.DeflatedExplicitVRLittleEndian));
        }
        setPatient(patient);
        try {
            setScheduledWorkItemCode(CodeBean.valueOf(codeHome,
            		ds.getItem(Tags.ScheduledWorkitemCodeSeq)));
            CodeBean.addCodesTo(codeHome, 
            		ds.get(Tags.ScheduledProcessingApplicationsCodeSeq),
                    getScheduledProcessingApplicationsCodes());
            CodeBean.addCodesTo(codeHome,
            		ds.get(Tags.ScheduledStationNameCodeSeq),
                    getScheduledStationNameCodes());
            CodeBean.addCodesTo(codeHome,
            		ds.get(Tags.ScheduledStationClassCodeSeq),
                    getScheduledStationClassCodes());
            CodeBean.addCodesTo(codeHome,
            		ds.get(Tags.ScheduledStationGeographicLocationCodeSeq),
                    getScheduledStationGeographicLocationCodes());
            createScheduledHumanPerformers(
                    ds.get(Tags.ScheduledHumanPerformersSeq));
            createRefRequests(ds.get(Tags.RefRequestSeq));            
        } catch (Exception e) {
            throw new EJBException(e);
        }
        log.info("Created " + prompt());
        if (log.isDebugEnabled()) {
            log.debug(ds);
        }
    }

    private void createScheduledHumanPerformers(DcmElement sq)
            throws CreateException {
        if (sq == null)
            return;
        Collection c = getScheduledHumanPerformers();
        GPSPSLocal gpsps = (GPSPSLocal) ejbctx.getEJBLocalObject();
        for (int i = 0, n = sq.countItems(); i < n; i++) {
            c.add(performerHome.create(sq.getItem(i), gpsps));
        }
    }
    private void createRefRequests(DcmElement sq) throws CreateException {
        if (sq == null) return;
        Collection c = getRefRequests();
        GPSPSLocal gpsps = (GPSPSLocal) ejbctx.getEJBLocalObject();
        for (int i = 0, n = sq.countItems(); i < n; i++) {
            c.add(rqHome.create(sq.getItem(i), gpsps));
        }
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
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);
    
    /**
     * @ejb.persistence column-name="gpsps_iuid"
     * @ejb.interface-method
     */
    public abstract String getSopIuid();

    public abstract void setSopIuid(String iuid);

    /**
     * @ejb.persistence column-name="gpsps_tuid"
     * @ejb.interface-method
     */
    public abstract String getTransactionUid();

    /**
     * @ejb.interface-method
     */
    public abstract void setTransactionUid(String iuid);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="start_datetime"
     */
    public abstract java.sql.Timestamp getSpsStartDateTime();
    public abstract void setSpsStartDateTime(java.sql.Timestamp dateTime);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="end_datetime"
     */
    public abstract java.sql.Timestamp getExpectedCompletionDateTime();
    public abstract void setExpectedCompletionDateTime(java.sql.Timestamp time);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="gpsps_status"
     */
    public abstract int getGpspsStatusAsInt();
    public abstract void setGpspsStatusAsInt(int status);

    /**
     * @ejb.interface-method
     */
    public String getGpspsStatus() {
        return GPSPSStatus.toString(getGpspsStatusAsInt());
    }

    public void setGpspsStatus(String status) {
        setGpspsStatusAsInt(GPSPSStatus.toInt(status));
    }

    /**
     * @ejb.interface-method
     */
    public boolean isScheduled() {
        return getGpspsStatusAsInt() == GPSPSStatus.SCHEDULED;
    }

    /**
     * @ejb.interface-method
     */
    public boolean isInProgress() {
        return getGpspsStatusAsInt() == GPSPSStatus.IN_PROGRESS;
    }

    /**
     * @ejb.interface-method
     */
    public boolean isSuspended() {
        return getGpspsStatusAsInt() == GPSPSStatus.SUSPENDED;
    }

    /**
     * @ejb.interface-method
     */
    public boolean isCompleted() {
        return getGpspsStatusAsInt() == GPSPSStatus.COMPLETED;
    }

    /**
     * @ejb.interface-method
     */
    public boolean isDiscontinued() {
        return getGpspsStatusAsInt() == GPSPSStatus.DISCONTINUED;
    }
    
    /**
     * @ejb.persistence column-name="gpsps_prior"
     */
    public abstract int getGpspsPriorityAsInt();

    public abstract void setGpspsPriorityAsInt(int prior);

    /**
     * @ejb.interface-method
     */
    public String getGpspsPriority() {
        return GPSPSPriority.toString(getGpspsPriorityAsInt());
    }

    public void setGpspsPriority(String prior) {
        setGpspsPriorityAsInt(GPSPSPriority.toInt(prior));
    }

    /**
     * @ejb.persistence column-name="in_availability"
     */
    public abstract int getInputAvailabilityFlagAsInt();

    public abstract void setInputAvailabilityFlagAsInt(int availability);

    /**
     * @ejb.interface-method
     */
    public String getInputAvailabilityFlag() {
        return InputAvailabilityFlag.toString(getInputAvailabilityFlagAsInt());
    }

    public void setInputAvailabilityFlag(String availability) {
        setInputAvailabilityFlagAsInt(InputAvailabilityFlag.toInt(availability));
    }

    /**
     * @ejb.persistence column-name="item_attrs"
     */
    public abstract byte[] getEncodedAttributes();

    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-gpsps" role-name="gpsps-of-patient"
     *               cascade-delete="yes"
     * @jboss.relation fk-column="patient_fk" related-pk-field="pk"
     */
    public abstract void setPatient(PatientLocal patient);

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract PatientLocal getPatient();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setGppps(java.util.Collection gpsps);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="gpsps-gppps" role-name="gpsps-resulting-in-gppps"
     * @jboss.relation-table table-name="rel_gpsps_gppps"
     * @jboss.relation fk-column="gppps_fk" related-pk-field="pk"     
     */
    public abstract java.util.Collection getGppps();
    
    /**
     * @ejb.relation name="gpsps-workitemcode" role-name="gpsps-with-workitemcode"
     *               target-ejb="Code" target-role-name="workitemcode-of-gpsps"
     *               target-multiple="yes"
     * @jboss.relation fk-column="code_fk" related-pk-field="pk"
     */
    public abstract CodeLocal getScheduledWorkItemCode();
    public abstract void setScheduledWorkItemCode(CodeLocal code);

    
    /**
     * @ejb.relation name="gpsps-appcode" role-name="gpsps-with-appcodes"
     *               target-ejb="Code" target-role-name="appcode-for-gpspss"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_gpsps_appcode"
     * @jboss.relation fk-column="appcode_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="gpsps_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getScheduledProcessingApplicationsCodes();
    public abstract void setScheduledProcessingApplicationsCodes(java.util.Collection codes);
    
    /**
     * @ejb.relation name="gpsps-devnamecode" role-name="gpsps-with-devnamecodes"
     *               target-ejb="Code" target-role-name="devnamecode-for-gpspss"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_gpsps_devname"
     * @jboss.relation fk-column="devname_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="gpsps_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getScheduledStationNameCodes();
    public abstract void setScheduledStationNameCodes(java.util.Collection codes);
    
    /**
     * @ejb.relation name="gpsps-devclasscode" role-name="gpsps-with-devclasscodes"
     *               target-ejb="Code" target-role-name="devclasscode-for-gpspss"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_gpsps_devclass"
     * @jboss.relation fk-column="devclass_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="gpsps_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getScheduledStationClassCodes();
    public abstract void setScheduledStationClassCodes(java.util.Collection codes);
        
    /**
     * @ejb.relation name="gpsps-devloccode" role-name="gpsps-with-devloccodes"
     *               target-ejb="Code" target-role-name="devloccode-for-gpspss"
     *               target-multiple="yes"
     * @jboss.relation-table table-name="rel_gpsps_devloc"
     * @jboss.relation fk-column="devloc_fk" related-pk-field="pk"     
     * @jboss.target-relation fk-column="gpsps_fk" related-pk-field="pk"     
     */    
    public abstract java.util.Collection getScheduledStationGeographicLocationCodes();
    public abstract void setScheduledStationGeographicLocationCodes(java.util.Collection codes);

    /**
     * @ejb.relation name="gpsps-human-performer" role-name="gpsps-for-human-performers"
     */    
    public abstract java.util.Collection getScheduledHumanPerformers();
    public abstract void setScheduledHumanPerformers(java.util.Collection humanPerformers);
    
    /**
     * @ejb.interface-method
     * @ejb.relation name="gpsps-request" role-name="gpsps-for-requests"
     */    
    public abstract java.util.Collection getRefRequests();
    public abstract void setRefRequests(java.util.Collection refRequests);
    
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
        setSopIuid(ds.getString(Tags.SOPInstanceUID));
        setGpspsStatus(ds.getString(Tags.GPSPSStatus));
        setGpspsPriority(ds.getString(Tags.GPSPSPriority));
        setInputAvailabilityFlag(ds.getString(Tags.InputAvailabilityFlag));
        setSpsStartDateTime(toTimestamp(ds.getDate(Tags.SPSStartDateAndTime)));
        setExpectedCompletionDateTime(
                toTimestamp(ds.getDate(Tags.ExpectedCompletionDateAndTime)));
        setEncodedAttributes(DatasetUtils.toByteArray(ds,
                UIDs.DeflatedExplicitVRLittleEndian));
    }

    /**
     * @ejb.interface-method
     */
    public String toString() {
        return prompt();
    }

    private String prompt() {
        return "GPSPS[pk=" + getPk() 
                + ", iuid=" + getSopIuid()
                + ", pat->" + getPatient() + "]";
    }

}
