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

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.PersonName;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocal;
import org.dcm4chex.archive.ejb.interfaces.OtherPatientIDLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.exceptions.CircularMergedException;
import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.dcm4chex.archive.exceptions.NonUniquePatientException;
import org.dcm4chex.archive.exceptions.PatientMergedException;
import org.dcm4chex.archive.util.Convert;

/**
 * @ejb.bean name="Patient" type="CMP" view-type="local"
 *           local-jndi-name="ejb/Patient" primkey-field="pk"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="patient"
 * @jboss.load-group name="pid"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * @jboss.audit-updated-time field-name="updatedTime"
 * 
 * @ejb.finder signature="Collection findAll()"
 *             query="SELECT OBJECT(p) FROM Patient AS p"
 *             transaction-type="Supports"
 * @ejb.finder signature="Collection findAll(int offset, int limit)"
 *             query=""
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findAll(int offset, int limit)"
 *              query="SELECT OBJECT(p) FROM Patient AS p ORDER BY p.pk OFFSET ?1 LIMIT ?2"
 *              
 * @ejb.finder signature="java.util.Collection findByPatientId(java.lang.String pid)"
 *             query="SELECT OBJECT(p) FROM Patient AS p WHERE p.patientId = ?1"
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByPatientId(java.lang.String pid)"
 *              strategy="on-find" eager-load-group="*"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.PatientLocal findByPatientIdWithIssuer(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT OBJECT(p) FROM Patient AS p WHERE p.patientId = ?1 AND p.issuerOfPatientId = ?2"
 *             transaction-type="Supports"
 * @jboss.query signature="org.dcm4chex.archive.ejb.interfaces.PatientLocal findByPatientIdWithIssuer(java.lang.String pid, java.lang.String issuer)"
 *              strategy="on-find" eager-load-group="*"
 *
 * @ejb.finder signature="java.util.Collection findByPatientIdAndNameAndBirthDate(java.lang.String pid, java.lang.String pn, java.lang.String birthdate)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByPatientIdAndNameAndBirthDate(java.lang.String pid, java.lang.String pn, java.lang.String birthdate)"
 *             query="SELECT OBJECT(p) FROM Patient AS p
 *             WHERE p.patientId = ?1 AND p.patientName LIKE ?2 AND p.patientBirthDate = ?3"
 *             strategy="on-find" eager-load-group="*"
 *
 * @ejb.finder signature="java.util.Collection findByPatientIdWithoutIssuerAndNameAndBirthDate(java.lang.String pid, java.lang.String pn, java.lang.String birthdate)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByPatientIdWithoutIssuerAndNameAndBirthDate(java.lang.String pid, java.lang.String pn, java.lang.String birthdate)"
 *             query="SELECT OBJECT(p) FROM Patient AS p
 *             WHERE p.patientId = ?1 AND p.issuerOfPatientId IS NULL AND p.patientName LIKE ?2 AND p.patientBirthDate = ?3"
 *             strategy="on-find" eager-load-group="*"
 *
 * @ejb.finder signature="java.util.Collection findByPatientNameAndBirthDate(java.lang.String pn, java.lang.String birthdate)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByPatientNameAndBirthDate(java.lang.String pn, java.lang.String birthdate)"
 *             query="SELECT OBJECT(p) FROM Patient AS p
 *             WHERE p.patientName LIKE ?1 AND p.patientBirthDate = ?2"
 *             strategy="on-find" eager-load-group="*"
 *
 * @ejb.finder signature="java.util.Collection findCorresponding(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT DISTINCT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid,
 *             IN(opid.patients) p2
 *             WHERE (p1.patientId = ?1 AND p1.issuerOfPatientId = ?2)
 *             OR (p2.patientId = ?1 AND p2.issuerOfPatientId = ?2)
 *             OR (opid.patientId = ?1 AND opid.issuerOfPatientId = ?2)"
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorresponding(java.lang.String pid, java.lang.String issuer)"
 *              strategy="on-find" eager-load-group="pid"
 *
 * @ejb.finder signature="java.util.Collection findCorrespondingLike(java.lang.String pid, java.lang.String issuer)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorrespondingLike(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT DISTINCT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid,
 *             IN(opid.patients) p2
 *             WHERE (p1.patientId LIKE ?1 AND p1.issuerOfPatientId = ?2)
 *             OR (p2.patientId LIKE ?1 AND p2.issuerOfPatientId = ?2)
 *             OR (opid.patientId LIKE ?1 AND opid.issuerOfPatientId = ?2)"
 *             strategy="on-find" eager-load-group="pid"
 *
 * @ejb.finder signature="java.util.Collection findCorrespondingByPrimaryPatientID(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT DISTINCT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid,
 *             IN(opid.patients) p2
 *             WHERE (p1.patientId = ?1 AND p1.issuerOfPatientId = ?2)
 *             OR (p2.patientId = ?1 AND p2.issuerOfPatientId = ?2)"
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorrespondingByPrimaryPatientID(java.lang.String pid, java.lang.String issuer)"
 *              strategy="on-find" eager-load-group="pid"
 *
 * @ejb.finder signature="java.util.Collection findCorrespondingByPrimaryPatientIDLike(java.lang.String pid, java.lang.String issuer)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorrespondingByPrimaryPatientIDLike(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT DISTINCT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid,
 *             IN(opid.patients) p2
 *             WHERE (p1.patientId LIKE ?1 AND p1.issuerOfPatientId = ?2)
 *             OR (p2.patientId LIKE ?1 AND p2.issuerOfPatientId = ?2)"
 *             strategy="on-find" eager-load-group="pid"
 *
 * @ejb.finder signature="java.util.Collection findCorrespondingByOtherPatientID(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid
 *             WHERE (opid.patientId = ?1 AND opid.issuerOfPatientId = ?2)"
 *             transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorrespondingByOtherPatientID(java.lang.String pid, java.lang.String issuer)"
 *              strategy="on-find" eager-load-group="pid"
 *
 * @ejb.finder signature="java.util.Collection findCorrespondingByOtherPatientIDLike(java.lang.String pid, java.lang.String issuer)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findCorrespondingByOtherPatientIDLike(java.lang.String pid, java.lang.String issuer)"
 *             query="SELECT OBJECT(p1) FROM Patient AS p1,
 *             IN(p1.otherPatientIds) opid
 *             WHERE (opid.patientId LIKE ?1 AND opid.issuerOfPatientId = ?2)"
 *             strategy="on-find" eager-load-group="pid"
 *
 * @ejb.ejb-ref ejb-name="OtherPatientID" view-type="local" ref-name="ejb/OtherPatientID"
 *
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public abstract class PatientBean implements EntityBean {

    private static final Logger log = Logger.getLogger(PatientBean.class);

    private static final int[] OTHER_PID_SQ = { Tags.OtherPatientIDSeq};
    
    private static final Class[] STRING_PARAM = new Class[] { String.class };

    private OtherPatientIDLocalHome opidHome;

    private EntityContext ctx;

    public void setEntityContext(EntityContext ctx) {
        Context jndiCtx = null;
        try {
            this.ctx = ctx;
            jndiCtx = new InitialContext();
            opidHome = (OtherPatientIDLocalHome)
                    jndiCtx.lookup("java:comp/env/ejb/OtherPatientID");
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
        ctx = null;
        opidHome = null;
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
     * Patient ID
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_id"
     * @jboss.load-group name="pid"
     */
    public abstract String getPatientId();

    public abstract void setPatientId(String pid);

    /**
     * Patient ID Issuer
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_id_issuer"
     * @jboss.load-group name="pid"
     */
    public abstract String getIssuerOfPatientId();

    /**
     * @ejb.interface-method
     */
    public abstract void setIssuerOfPatientId(String issuer);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_name"
     */
    public abstract String getPatientName();
    public abstract void setPatientName(String name);
        
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_i_name"
     */
    public abstract String getPatientIdeographicName();
    public abstract void setPatientIdeographicName(String name);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_p_name"
     */
    public abstract String getPatientPhoneticName();
    public abstract void setPatientPhoneticName(String name);

    /**
     * Patient Birth Date
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_birthdate"
     */
    public abstract String getPatientBirthDate();

    /**
     * @ejb.interface-method
     */
    public abstract void setPatientBirthDate(String dateString);

    /**
     * Patient Sex
     *
     * @ejb.interface-method
     * @ejb.persistence column-name="pat_sex"
     */
    public abstract String getPatientSex();

    /**
     * @ejb.interface-method
     *
     */
    public abstract void setPatientSex(String sex);

    /**
     * @ejb.persistence column-name="pat_custom1"
     */
    public abstract String getPatientCustomAttribute1();
    public abstract void setPatientCustomAttribute1(String value);

    /**
     * @ejb.persistence column-name="pat_custom2"
     */
    public abstract String getPatientCustomAttribute2();
    public abstract void setPatientCustomAttribute2(String value);

    /**
     * @ejb.persistence column-name="pat_custom3"
     */
    public abstract String getPatientCustomAttribute3();
    public abstract void setPatientCustomAttribute3(String value);

   /**
     * Patient DICOM Attributes
     *
     * @ejb.persistence
     *  column-name="pat_attrs"
     * 
     */
    public abstract byte[] getEncodedAttributes();

    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.interface-method
     * @ejb.relation name="patient-other-pid" role-name="patient-with other-pids"
     * @jboss.relation-table table-name="rel_pat_other_pid"
     * @jboss.relation fk-column="other_pid_fk" related-pk-field="pk"     
     */
    public abstract java.util.Collection getOtherPatientIds();
    public abstract void setOtherPatientIds(java.util.Collection otherPIds);
    
    /**
     * @return Patient, with which this Patient was merged.
     *
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="merged-patients"
     *    role-name="dereferenced-patient"
     *    cascade-delete="yes"
     *
     * @jboss.relation fk-column="merge_fk" related-pk-field="pk"
     */
    public abstract PatientLocal getMergedWith();

    /**
     * @param mergedWith, Patient, with which this Patient was merged.
     *
     * @ejb.interface-method
     */
    public abstract void setMergedWith(PatientLocal mergedWith);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="merged-patients"
     *    role-name="dominant-patient"
     *    
     * @return all patients merged with this patient
     */
    public abstract java.util.Collection getMerged();
    public abstract void setMerged(java.util.Collection patients);

    /**
     * @ejb.interface-method view-type="local"
     *
     * @param studies all studies of this patient
     */
    public abstract void setStudies(java.util.Collection studies);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-study" role-name="patient-has-studies"
     *    
     * @return all studies of this patient
     */
    public abstract java.util.Collection getStudies();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setMwlItems(java.util.Collection mwlItems);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-mwlitems" role-name="patient-has-mwlitems"
     */
    public abstract java.util.Collection getMwlItems();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setMpps(java.util.Collection mpps);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-mpps" role-name="patient-has-mpps"
     */
    public abstract java.util.Collection getMpps();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setGppps(java.util.Collection mpps);

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-gppps" role-name="patient-has-gppps"
     */
    public abstract java.util.Collection getGppps();

    /**
     * @ejb.interface-method view-type="local"
     * @ejb.relation name="patient-gpsps" role-name="patient-has-gpsps"
     */
    public abstract java.util.Collection getGsps();

    /**
     * @ejb.interface-method view-type="local"
     */
    public abstract void setGsps(java.util.Collection gsps);

    /**
     * Create patient.
     *
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds) throws CreateException {
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(Dataset ds) throws CreateException {
        try {
            createOtherPatientIds(ds.get(Tags.OtherPatientIDSeq));
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        log.info("Created " + prompt());
    }

    private void createOtherPatientIds(DcmElement opidsq)
            throws CreateException, FinderException {
        if (opidsq == null || opidsq.isEmpty() || opidsq.getItem().isEmpty()) {
            return;
        }
        Collection opids = getOtherPatientIds();
        for (int i = 0, n = opidsq.countItems(); i < n; i++) {
            Dataset opid = opidsq.getItem(i);
            opids.add(opidHome.valueOf(
                    opid.getString(Tags.PatientID),
                    opid.getString(Tags.IssuerOfPatientID)));
        }
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
        // Remove OtherPatientIDs only related to this Patient
        for ( Iterator iter = getOtherPatientIds().iterator() ; iter.hasNext() ; ) {
            OtherPatientIDLocal opid = (OtherPatientIDLocal) iter.next();
            iter.remove();
            if (opid.getPatients().isEmpty()) {
                opid.remove();
            }
        }
        // we have to delete studies explicitly here due to an foreign key constrain error 
        // if an mpps key is set in one of the series.
        for ( Iterator iter = getStudies().iterator() ; iter.hasNext() ; ) {
            StudyLocal study = (StudyLocal) iter.next();
            iter.remove(); 
            study.remove();
        }
    }

    /**
     * @ejb.home-method
     */
    public PatientLocal ejbHomeSelectByPatientId(Dataset ds)
            throws FinderException {
        PatientLocalHome patHome = (PatientLocalHome) ctx.getEJBLocalHome();
        String pid = ds.getString(Tags.PatientID);
        String issuer = ds.getString(Tags.IssuerOfPatientID);
        if (issuer != null) {
            return patHome.findByPatientIdWithIssuer(pid, issuer);
        }
        Collection c = patHome.findByPatientId(pid);
        if (c.isEmpty()) {
            throw new ObjectNotFoundException();
        }
        if (c.size() > 1) {
            throw new NonUniquePatientException("Patient ID[id="
                    + pid + " ambiguous");
        }
        PatientLocal pat = (PatientLocal) c.iterator().next();
        PatientLocal merged = pat.getMergedWith();
        if (merged != null) {
             String prompt = "Patient ID[id="
                                + pat.getPatientId() + ",issuer="
                                + pat.getIssuerOfPatientId()
                                + "] merged with Patient ID[id="
                                + merged.getPatientId() + ",issuer=" 
                                + merged.getIssuerOfPatientId() + "]";
            log.warn(prompt);
            throw new PatientMergedException(prompt); 
        }
        return pat;
    }

    /**
     * @ejb.home-method
     */
    public Collection ejbHomeSelectByPatientDemographic(Dataset ds)
            throws FinderException {
        PatientLocalHome patHome = (PatientLocalHome) ctx.getEJBLocalHome();
        String pid = ds.getString(Tags.PatientID);
        String issuer = ds.getString(Tags.IssuerOfPatientID);
        if (pid != null && issuer != null) {
            try {
                return Collections.singleton(
                        patHome.findByPatientIdWithIssuer(pid, issuer));
            } catch (ObjectNotFoundException onfe) {}
        }
        PersonName pn = ds.getPersonName(Tags.PatientName);
        String birthdate = normalizeDA(ds.getString(Tags.PatientBirthDate));
        if (birthdate == null || pn == null
                || pn.get(PersonName.FAMILY) == null
                || pn.get(PersonName.GIVEN) == null ) {
            return Collections.emptyList();
        }
        String pnwc = toWildCard(pn);
        return pid == null
                ? patHome.findByPatientNameAndBirthDate(pnwc, birthdate)
                : issuer == null
                ? patHome.findByPatientIdAndNameAndBirthDate(pid, pnwc, birthdate)
                : patHome.findByPatientIdWithoutIssuerAndNameAndBirthDate(pid, pnwc, birthdate);
    }

    /**
     * @ejb.home-method
     */
    public PatientLocal ejbHomeFollowMergedWith(PatientLocal pat)
            throws CircularMergedException {
        PatientLocal result = pat;
        PatientLocal merged;
        while ((merged = result.getMergedWith()) != null) {
            if (merged.isIdentical(pat)) {
                String prompt = "Detect circular merged Patient "
                    + pat.asString();
                log.warn(prompt);
                throw new CircularMergedException(prompt);
            }
            result = merged;
        }
        return result;
    }


    private String toWildCard(PersonName pn) {
        StringBuilder sb = new StringBuilder(64);
        sb.append(pn.get(PersonName.FAMILY).toUpperCase());
        sb.append('^');
        sb.append(pn.get(PersonName.GIVEN).toUpperCase());
        if (pn.get(PersonName.MIDDLE) != null) {
            sb.append('^');
            sb.append(pn.get(PersonName.MIDDLE).toUpperCase());
        }
        sb.append("^%");
        return sb.toString();
    }

     /**
     * @ejb.interface-method
     */
    public Dataset getAttributes(boolean supplement) {
        Dataset ds = DatasetUtils.fromByteArray(getEncodedAttributes());
        if (supplement) {
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putOB(PrivateTags.PatientPk, Convert.toBytes(getPk().longValue()));
        }
        return ds;
    }

    /**
     * @ejb.interface-method
     */
    public void setAttributes(Dataset ds) {
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
        setAttributesInternal(filter.filter(ds), filter.getTransferSyntaxUID());
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            setField(filter.getField(fieldTags[i]), ds.getString(fieldTags[i]));
        }
    }

    private void setField(String field, String value ) {
        try {
            Method m = PatientBean.class.getMethod("set" 
                    + Character.toUpperCase(field.charAt(0))
                    + field.substring(1), STRING_PARAM);
            m.invoke(this, new Object[] { value });
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }       
    }

    private void setAttributesInternal(Dataset ds, String tsuid) {
        setPatientId(ds.getString(Tags.PatientID));
        setIssuerOfPatientId(ds.getString(Tags.IssuerOfPatientID));
        PersonName pn = ds.getPersonName(Tags.PatientName);
        if (pn != null) {
            setPatientName(toUpperCase(pn.toComponentGroupString(false)));
            PersonName ipn = pn.getIdeographic();
            if (ipn != null) {
                setPatientIdeographicName(ipn.toComponentGroupString(false));
            }
            PersonName ppn = pn.getPhonetic();
            if (ppn != null) {
                setPatientPhoneticName(ppn.toComponentGroupString(false));
            }
        }
        setPatientBirthDate(normalizeDA(ds.getString(Tags.PatientBirthDate)));
        setPatientSex(ds.getString(Tags.PatientSex));
        byte[] b = DatasetUtils.toByteArray(ds, tsuid);
        if (log.isDebugEnabled()) {
            log.debug("setEncodedAttributes(byte[" + b.length + "])");
        }
        setEncodedAttributes(b);
    }
    
    /**
     * @throws DcmServiceException 
     * @ejb.interface-method
     */
    public void coerceAttributes(Dataset ds, Dataset coercedElements)
    throws DcmServiceException {
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
        
        if (filter.isOverwrite()) {
            Dataset attrs;
            if (filter.isMerge()) {
                attrs = getAttributes(false);
                appendOtherPatientIds(attrs, ds);
                AttrUtils.updateAttributes(attrs, 
                        filter.filter(ds).exclude(OTHER_PID_SQ), log);
            } else {
                log.debug("-merge update-strategy not specified.  Not synchronizing other patient ids!");
                attrs = filter.filter(ds);
            }
            setAttributesInternal(attrs, filter.getTransferSyntaxUID());
        } else {
            Dataset attrs = getAttributes(false);
            boolean b = false;
            if (filter.isMerge())
                 b = appendOtherPatientIds(attrs, ds);
            else
                log.debug("-merge update-strategy not specified.  Not synchronizing other patient ids!");

            AttrUtils.coerceAttributes(attrs, ds, coercedElements, filter, log);
            if (filter.isMerge()
                    && (AttrUtils.mergeAttributes(attrs, filter.filter(ds), log) || b)) {
                setAttributesInternal(attrs, filter.getTransferSyntaxUID());
            }
        }
    }
    
    /**
     * @ejb.interface-method
     */
    public void updateAttributes(Dataset ds) {
        Dataset attrs = getAttributes(false);
        boolean b = appendOtherPatientIds(attrs, ds);
        if (AttrUtils.updateAttributes(attrs, ds.exclude(OTHER_PID_SQ), log) || b) {
            setAttributes(attrs);
        }
    }

    private boolean appendOtherPatientIds(Dataset attrs, Dataset ds) {
        DcmElement nopidsq = ds.get(Tags.OtherPatientIDSeq);
        if (nopidsq == null || nopidsq.isEmpty() || nopidsq.getItem().isEmpty()) {
            return false;
        }
        boolean update = false;
        DcmElement opidsq = attrs.get(Tags.OtherPatientIDSeq);
        if (opidsq == null) {
            opidsq = attrs.putSQ(Tags.OtherPatientIDSeq);
        }
        for (int i = 0, n = nopidsq.countItems(); i < n; i++) {
            Dataset nopid = nopidsq.getItem(i);
            String pid = nopid.getString(Tags.PatientID);
            String issuer = nopid.getString(Tags.IssuerOfPatientID);
            if (!containsPID(pid, issuer, opidsq)) {
                opidsq.addItem(nopid);
                getOtherPatientIds().add(opidHome.valueOf(pid, issuer));
                update = true;
                log.info("Add additional Other Patient ID: "
                        + pid + "^^^"
                        +  issuer
                        + " to " + prompt());
            }
        }
        return update;
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateOtherPatientIDs(Dataset ds) {
        Dataset attrs = getAttributes(false);
        DcmElement opidsq = attrs.remove(Tags.OtherPatientIDSeq);
        DcmElement nopidsq = ds.get(Tags.OtherPatientIDSeq);
        boolean update = false;
        if (opidsq != null) {
            for (int i = 0, n = opidsq.countItems(); i < n; i++) {
                Dataset opid = opidsq.getItem(i);
                String pid = opid.getString(Tags.PatientID);
                String issuer = opid.getString(Tags.IssuerOfPatientID);
                if (nopidsq == null || !containsPID(pid, issuer, nopidsq)) {
                    try {
                        OtherPatientIDLocal otherPatientId = 
                            opidHome.findByPatientIdAndIssuer(pid, issuer);
                        getOtherPatientIds().remove(otherPatientId);
                        if (otherPatientId.getPatients().isEmpty()) {
                            otherPatientId.remove();
                        }
                    } catch (FinderException e) {
                        throw new EJBException(e);
                    } catch (RemoveException e) {
                        throw new EJBException(e);
                    }
                    update = true;
                    log.info("Remove Other Patient ID: " + pid + "^^^" 
                            +  issuer + " from " + prompt());
                }
            }
        }
        if (nopidsq != null) {
            for (int i = 0, n = nopidsq.countItems(); i < n; i++) {
                Dataset nopid = nopidsq.getItem(i);
                String pid = nopid.getString(Tags.PatientID);
                String issuer = nopid.getString(Tags.IssuerOfPatientID);
                if (opidsq == null || !containsPID(pid, issuer, opidsq)) {
                    getOtherPatientIds().add(opidHome.valueOf(pid, issuer));
                    update = true;
                    log.info("Add additional Other Patient ID: "
                            + pid + "^^^" +  issuer + " to " + prompt());
                }
            }
            if (update) {
                opidsq = attrs.putSQ(Tags.OtherPatientIDSeq);
                for (int i = 0, n = nopidsq.countItems(); i < n; i++) {
                    opidsq.addItem(nopidsq.getItem(i));
                }
            }
        }
        if (update) {
            setAttributes(attrs);
        }
        return update;
    }

    private boolean containsPID(String pid, String issuer, DcmElement opidsq) {
        for (int i = 0, n = opidsq.countItems(); i < n; i++) {
            Dataset opid = opidsq.getItem(i);
            if (opid.getString(Tags.PatientID)
                    .equals(pid)
                && opid.getString(Tags.IssuerOfPatientID)
                    .equals(issuer)) {
                    return true;
            }
        }
        return false;
    }

    private static String toUpperCase(String s) {
        return s != null ? s.toUpperCase() : null;
    }

    private static String normalizeDA(String s) {
        if (s == null) {
            return null;
        }
        String trim = s.trim();
        int l = trim.length();
        if (l == 0) {
            return null;
        }
        if (l == 10 && trim.charAt(4) == '-' && trim.charAt(7) == '-') {
            StringBuilder sb = new StringBuilder(8);
            sb.append(trim.substring(0, 4));
            sb.append(trim.substring(5, 7));
            sb.append(trim.substring(8));
            return sb.toString();
        }
        return trim;
    }

    /**
     * @ejb.interface-method
     */
    public String asString() {
        return prompt();
    }

    private String prompt() {
        return "Patient[pk=" + getPk() + ", pid=" + getPatientId() + ", name="
                + getPatientName() + "]";
    }
}