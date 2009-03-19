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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.net.DcmServiceException;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.CodeLocal;
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.MediaDTO;
import org.dcm4chex.archive.ejb.interfaces.MediaLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.VerifyingObserverLocalHome;
import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.dcm4chex.archive.util.AETs;
import org.dcm4chex.archive.util.Convert;

/**
 * Instance Bean
 * 
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 8057 $ $Date: 2008-09-11 13:37:20 +0200 (Thu, 11 Sep
 *          2008) $
 * 
 * @ejb.bean name="Instance" type="CMP" view-type="local" primkey-field="pk"
 *           local-jndi-name="ejb/Instance"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="instance"
 * @jboss.load-group name="most"
 * @jboss.eager-load-group name="most"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * @jboss.audit-updated-time field-name="updatedTime"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.InstanceLocal findBySopIuid(java.lang.String uid)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.sopIuid = ?1"
 *             transaction-type="Supports"
 * @jboss.query signature="org.dcm4chex.archive.ejb.interfaces.InstanceLocal findBySopIuid(java.lang.String uid)"
 *              strategy="on-find" eager-load-group="most"
 * 
 * @ejb.finder signature="java.util.Collection findNotOnMediaAndStudyReceivedBefore(java.sql.Timestamp receivedBefore)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.media IS NULL AND i.series.study.createdTime < ?1"
 *             transaction-type="Supports"
 * 
 * @ejb.finder signature="java.util.Collection findByPatientAndSopCuid(org.dcm4chex.archive.ejb.interfaces.PatientLocal patient, java.lang.String uid)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.series.study.patient = ?1 AND i.sopCuid = ?2"
 *             transaction-type="Supports"
 * 
 * @ejb.finder signature="java.util.Collection findByPatientAndSrCode(org.dcm4chex.archive.ejb.interfaces.PatientLocal patient, org.dcm4chex.archive.ejb.interfaces.CodeLocal srcode)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.series.study.patient = ?1 AND i.srCode = ?2"
 *             transaction-type="Supports"
 * 
 * @ejb.finder signature="java.util.Collection findByStudyAndSrCode(java.lang.String suid, java.lang.String cuid, java.lang.String code, java.lang.String designator)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.series.study.studyIuid = ?1 AND i.sopCuid = ?2 AND i.srCode.codeValue = ?3 AND i.srCode.codingSchemeDesignator = ?4"
 *             transaction-type="Supports"
 * 
 * @ejb.finder 
 *             signature="java.util.Collection findBySeriesPk(java.lang.Long seriesPk)"
 *             query=
 *             "SELECT OBJECT(i) FROM Instance AS i WHERE i.series.pk = ?1 ORDER BY i.pk"
 *             transaction-type="Supports"
 * 
 * @jboss.query 
 *              signature="java.util.Collection findBySeriesPk(java.lang.Long seriesPk)"
 *              strategy="on-find" eager-load-group="*"
 * 
 * @ejb.finder signature=
 *             "java.util.Collection findBySeriesIuid(java.lang.String seriesIuid)"
 *             query="SELECT OBJECT(i) FROM Instance AS i WHERE i.series.seriesIuid = ?1 ORDER BY i.pk"
 *             transaction-type="Supports"
 * 
 * @jboss.query signature=
 *              "java.util.Collection findBySeriesIuid(java.lang.String seriesIuid)"
 *              strategy="on-find" eager-load-group="most"
 * 
 * @jboss.query signature="java.util.Collection ejbSelectGeneric(java.lang.String jbossQl, java.lang.Object[] args)"
 *              dynamic="true" strategy="on-load" page-size="20"
 *              eager-load-group="*"
 * 
 * @ejb.ejb-ref ejb-name="Code" view-type="local" ref-name="ejb/Code"
 * @ejb.ejb-ref ejb-name="VerifyingObserver" view-type="local"
 *              ref-name="ejb/VerifyingObserver"
 */
public abstract class InstanceBean implements EntityBean {

    private static final int DEFAULT_IN_LIST_SIZE = 200;

    private static final Logger log = Logger.getLogger(InstanceBean.class);

    private static final Class[] STRING_PARAM = new Class[] { String.class };

    private CodeLocalHome codeHome;
    private VerifyingObserverLocalHome observerHome;

    public void setEntityContext(EntityContext ctx) {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            codeHome = (CodeLocalHome) jndiCtx.lookup("java:comp/env/ejb/Code");
            observerHome = (VerifyingObserverLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/VerifyingObserver");
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
        observerHome = null;
    }

    /**
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
     * @ejb.interface-method
     * @ejb.persistence column-name="sop_iuid"
     * @jboss.load-group name="most"
     */
    public abstract String getSopIuid();

    public abstract void setSopIuid(String iuid);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="sop_cuid"
     * @jboss.load-group name="most"
     * 
     */
    public abstract String getSopCuid();

    public abstract void setSopCuid(String cuid);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="inst_no"
     */
    public abstract String getInstanceNumber();

    public abstract void setInstanceNumber(String no);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="content_datetime"
     */
    public abstract java.sql.Timestamp getContentDateTime();

    public abstract void setContentDateTime(java.sql.Timestamp dateTime);

    private void setContentDateTime(java.util.Date date) {
        setContentDateTime(date != null ? new java.sql.Timestamp(date.getTime())
                : null);
    }

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="sr_complete"
     */
    public abstract String getSrCompletionFlag();

    public abstract void setSrCompletionFlag(String flag);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="sr_verified"
     */
    public abstract String getSrVerificationFlag();

    public abstract void setSrVerificationFlag(String flag);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="inst_custom1"
     */
    public abstract String getInstanceCustomAttribute1();

    public abstract void setInstanceCustomAttribute1(String value);

    /**
     * @ejb.persistence column-name="inst_custom2"
     */
    public abstract String getInstanceCustomAttribute2();

    public abstract void setInstanceCustomAttribute2(String value);

    /**
     * @ejb.persistence column-name="inst_custom3"
     */
    public abstract String getInstanceCustomAttribute3();

    public abstract void setInstanceCustomAttribute3(String value);

    /**
     * @ejb.persistence column-name="inst_attrs"
     */
    public abstract byte[] getEncodedAttributes();

    public abstract void setEncodedAttributes(byte[] bytes);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="ext_retr_aet"
     * @jboss.load-group name="most"
     */
    public abstract String getExternalRetrieveAET();

    /**
     * @ejb.interface-method
     */
    public abstract void setExternalRetrieveAET(String aet);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="retrieve_aets"
     * @jboss.load-group name="most"
     */
    public abstract String getRetrieveAETs();

    public abstract void setRetrieveAETs(String aets);

    /**
     * @ejb.persistence column-name="availability"
     * @jboss.load-group name="most"
     */
    public abstract int getAvailability();

    /**
     * @ejb.interface-method
     */
    public int getAvailabilitySafe() {
        try {
            return getAvailability();
        } catch (NullPointerException npe) {
            return 0;
        }
    }

    /**
     * @ejb.interface-method
     */
    public abstract void setAvailability(int availability);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="inst_status"
     */
    public abstract int getInstanceStatus();

    /**
     * @ejb.interface-method
     */
    public abstract void setInstanceStatus(int status);

    /**
     * @ejb.persistence column-name="all_attrs"
     */
    public abstract boolean getAllAttributes();

    /**
     * @ejb.interface-method
     */
    public abstract void setAllAttributes(boolean allAttributes);

    /**
     * @ejb.persistence column-name="commitment"
     */
    public abstract boolean getCommitment();

    /**
     * @ejb.interface-method
     */
    public boolean getCommitmentSafe() {
        try {
            return getCommitment();
        } catch (NullPointerException npe) {
            return false;
        }
    }

    /**
     * @ejb.interface-method
     */
    public abstract void setCommitment(boolean commitment);

    /**
     * @ejb.interface-method
     * @ejb.relation name="series-instance" role-name="instance-of-series"
     *               cascade-delete="yes"
     * @jboss.relation fk-column="series_fk" related-pk-field="pk"
     * @jboss.load-group name="most"
     * 
     * @param series
     *            series of this instance
     */
    public abstract void setSeries(SeriesLocal series);

    /**
     * @ejb.interface-method
     * 
     * @return series of this series
     */
    public abstract SeriesLocal getSeries();

    /**
     * @ejb.relation name="instance-files" role-name="instance-in-files"
     * 
     * @ejb.interface-method
     * 
     * @return all files of this instance
     */
    public abstract java.util.Collection getFiles();

    public abstract void setFiles(java.util.Collection files);

    /**
     * @ejb.relation name="instance-media" role-name="instance-on-media"
     * @jboss.relation fk-column="media_fk" related-pk-field="pk"
     * 
     * @ejb.interface-method
     */
    public abstract MediaLocal getMedia();

    /**
     * @ejb.interface-method
     */
    public abstract void setMedia(MediaLocal media);

    /**
     * @ejb.relation name="instance-srcode" role-name="sr-with-title"
     *               target-ejb="Code" target-role-name="title-of-sr"
     *               target-multiple="yes"
     * @jboss.relation fk-column="srcode_fk" related-pk-field="pk"
     * 
     * @param srCode
     *            code of SR title
     */
    public abstract void setSrCode(CodeLocal srCode);

    /**
     * @ejb.interface-method
     * 
     * @return code of SR title
     */
    public abstract CodeLocal getSrCode();

    /**
     * @ejb.interface-method
     * @ejb.relation name="instance-verifying-observer"
     *               role-name="instance-with-verifying-observer"
     *               target-role-name="verifying-observer-of-instance"
     *               target-ejb="VerifyingObserver" target-cascade-delete="yes"
     * @jboss.target-relation fk-column="instance_fk" related-pk-field="pk"
     */
    public abstract java.util.Collection getVerifyingObservers();

    public abstract void setVerifyingObservers(java.util.Collection observers);

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(Dataset ds, SeriesLocal series)
            throws CreateException {
        setAttributes(ds);
        return null;
    }

    public void ejbPostCreate(Dataset ds, SeriesLocal series)
            throws CreateException {
        try {
            setSrCode(CodeBean.valueOf(codeHome, ds
                    .getItem(Tags.ConceptNameCodeSeq)));
            DcmElement sq = ds.get(Tags.VerifyingObserverSeq);
            if (sq != null) {
                Collection c = getVerifyingObservers();
                for (int i = 0, n = sq.countItems(); i < n; i++) {
                    c.add(observerHome.create(sq.getItem(i)));
                }
            }
        } catch (Exception e) {
            // ensure to rollback transaction
            throw new EJBException(e);
        }
        setSeries(series);
        log.info("Created " + prompt());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
    }

    /**
     * @ejb.select query="SELECT DISTINCT f.fileSystem.retrieveAET FROM Instance i, IN(i.files) f WHERE i.pk = ?1"
     */
    public abstract Set ejbSelectRetrieveAETs(Long pk) throws FinderException;

    /**
     * @ejb.interface-method
     */
    public void addRetrieveAET(String aet) {
        String s = getRetrieveAETs();
        if (s == null) {
            setRetrieveAETs(aet);
        } else {
            final Set aetSet = new HashSet(Arrays.asList(StringUtils.split(s,
                    '\\')));
            if (aetSet.add(aet))
                setRetrieveAETs(toString(aetSet));
        }
    }

    /**
     * @ejb.interface-method
     */
    public boolean updateRetrieveAETs() {
        Set aetSet;
        try {
            aetSet = ejbSelectRetrieveAETs(getPk());
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        if (aetSet.remove(null))
            log.warn("Instance[iuid=" + getSopIuid()
                    + "] reference File(s) with unspecified Retrieve AET");
        final String aets = toString(aetSet);
        boolean updated;
        if (aets == null ? getRetrieveAETs() == null
                         : aets.equals(getRetrieveAETs())) {
            return false;
        }
        setRetrieveAETs(aets);
        return true;
    }

    /**
     * @ejb.select query="SELECT MIN(f.fileSystem.availability) FROM Instance i, IN(i.files) f WHERE i.pk = ?1"
     */
    public abstract int ejbSelectLocalAvailability(Long pk)
            throws FinderException;

    /**
     * @ejb.interface-method
     */
    public boolean updateAvailability(int availabilityOfExternalRetrieveable) {
        int availability = Availability.UNAVAILABLE;
        MediaLocal media;
        if (getRetrieveAETs() != null)
            try {
                availability = ejbSelectLocalAvailability(getPk());
            } catch (FinderException e) {
                throw new EJBException(e);
            }
        if (getExternalRetrieveAET() != null)
            availability = Math.min(availability, availabilityOfExternalRetrieveable);
        if (availability == Availability.UNAVAILABLE
                && (media = getMedia()) != null
                && media.getMediaStatus() == MediaDTO.COMPLETED)
            availability = Availability.OFFLINE;
        if (availability == getAvailabilitySafe()) {
            return false;
        }
        setAvailability(availability);
        return true;
    }

    /** 
     * @ejb.interface-method
     */
    public void updateRetrieveAETs(String oldAET, String newAET) {
        setRetrieveAETs(AETs.update(getRetrieveAETs(), oldAET, newAET));
    }

    private static String toString(Set s) {
        if (s.isEmpty())
            return null;
        String[] a = (String[]) s.toArray(new String[s.size()]);
        return StringUtils.toString(a, '\\');
    }

    /**
     * @ejb.interface-method
     */
    public Dataset getAttributes(boolean supplement) {
        Dataset ds;
        try {
            ds = DatasetUtils.fromByteArray(getEncodedAttributes());
        } catch (IllegalArgumentException x) {
            // BLOB size not sufficient to store Attributes
            log
                    .warn("Instance (pk:"
                            + getPk()
                            + ") Attributes truncated in database! (BLOB size not sufficient to store Attributes correctly) !");
            ds = DcmObjectFactory.getInstance().newDataset();
            ds.putUI(Tags.SOPInstanceUID, this.getSopIuid());
            ds.putUI(Tags.SOPClassUID, this.getSopCuid());
        }
        if (supplement) {
            ds.setPrivateCreatorID(PrivateTags.CreatorID);
            ds.putOB(PrivateTags.InstancePk, Convert.toBytes(getPk()
                    .longValue()));
            MediaLocal media = getMedia();
            if (media != null && media.getMediaStatus() == MediaDTO.COMPLETED) {
                ds.putSH(Tags.StorageMediaFileSetID, media.getFilesetId());
                ds.putUI(Tags.StorageMediaFileSetUID, media.getFilesetIuid());
            }
            DatasetUtils.putRetrieveAET(ds, getRetrieveAETs(),
                    getExternalRetrieveAET());
            ds.putCS(Tags.InstanceAvailability, Availability
                    .toString(getAvailabilitySafe()));
        }
        return ds;
    }

    /**
     * 
     * @ejb.interface-method
     */
    public void setAttributes(Dataset ds) {
        String cuid = ds.getString(Tags.SOPClassUID);
        AttributeFilter filter = AttributeFilter
                .getInstanceAttributeFilter(cuid);
        setAllAttributes(filter.isNoFilter());
        setAttributesInternal(filter.filter(ds), filter.getTransferSyntaxUID());
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            setField(filter.getField(fieldTags[i]), ds.getString(fieldTags[i]));
        }
    }

    private void setField(String field, String value) {
        try {
            Method m = InstanceBean.class.getMethod("set"
                    + Character.toUpperCase(field.charAt(0))
                    + field.substring(1), STRING_PARAM);
            m.invoke(this, new Object[] { value });
        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
    }

    private void setAttributesInternal(Dataset ds, String tsuid) {
        setSopIuid(ds.getString(Tags.SOPInstanceUID));
        setSopCuid(ds.getString(Tags.SOPClassUID));
        setInstanceNumber(ds.getString(Tags.InstanceNumber));
        try {
            setContentDateTime(ds.getDateTime(Tags.ContentDate,
                    Tags.ContentTime));
        } catch (IllegalArgumentException e) {
            log.warn("Illegal Content Date/Time format: " + e.getMessage());
        }
        setSrCompletionFlag(ds.getString(Tags.CompletionFlag));
        setSrVerificationFlag(ds.getString(Tags.VerificationFlag));
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
        String cuid = ds.getString(Tags.SOPClassUID);
        AttributeFilter filter = AttributeFilter
                .getInstanceAttributeFilter(cuid);
        if (filter.isOverwrite()) {
            Dataset attrs;
            if (filter.isMerge()) {
                attrs = getAttributes(false);
                AttrUtils.updateAttributes(attrs, filter.filter(ds), log);
            } else {
                attrs = filter.filter(ds);
            }
            setAttributesInternal(attrs, filter.getTransferSyntaxUID());
        } else {
            Dataset attrs = getAttributes(false);
            AttrUtils.coerceAttributes(attrs, ds, coercedElements, filter, log);
            if (filter.isMerge()
                    && AttrUtils.mergeAttributes(attrs, filter.filter(ds), log)) {
                setAttributesInternal(attrs, filter.getTransferSyntaxUID());
            }
        }
    }

    /**
     * @ejb.select query="" transaction-type="Supports"
     */
    public abstract Collection ejbSelectGeneric(String jbossQl, Object[] args)
            throws FinderException;

    /**
     * @ejb.home-method
     */
    public Collection ejbHomeListByIUIDs(String[] iuids) throws FinderException {
        if (iuids == null || iuids.length < 1)
            return new ArrayList();
        Collection c;
        String jbossQl;
        log.debug("List by IUIDs:" + iuids.length);
        long t0 = System.currentTimeMillis();
        if (iuids.length > DEFAULT_IN_LIST_SIZE) {
            jbossQl = getByIUIDsQueryString(DEFAULT_IN_LIST_SIZE);
            String[] uids = new String[DEFAULT_IN_LIST_SIZE];
            System.arraycopy(iuids, 0, uids, 0, DEFAULT_IN_LIST_SIZE);
            int idx = DEFAULT_IN_LIST_SIZE;
            int maxIdx = iuids.length - DEFAULT_IN_LIST_SIZE;
            c = ejbSelectGeneric(jbossQl, uids);
            log.debug("First chunked query:" + c.size());
            while (idx < maxIdx) {
                System.arraycopy(iuids, idx, uids, 0, DEFAULT_IN_LIST_SIZE);
                c.addAll(ejbSelectGeneric(jbossQl, uids));
                log.debug("chunked query (idx:" + idx + "):" + c.size());
                idx += DEFAULT_IN_LIST_SIZE;
            }
            if (idx < iuids.length) {
                int remain = iuids.length - idx;
                jbossQl = getByIUIDsQueryString(remain);
                uids = new String[remain];
                System.arraycopy(iuids, idx, uids, 0, remain);
                c.addAll(ejbSelectGeneric(jbossQl, uids));
                log.debug("Remain chunked query(remain:" + remain + "):"
                        + c.size());
            }
        } else {
            jbossQl = getByIUIDsQueryString(iuids.length);
            c = ejbSelectGeneric(jbossQl, iuids);
        }
        log.debug("Total time:" + (System.currentTimeMillis() - t0));
        return c;
    }

    private String getByIUIDsQueryString(int size) {
        StringBuffer sb = new StringBuffer("SELECT OBJECT(i) FROM Instance i");
        sb.append(" WHERE i.sopIuid");
        addIN(sb, 1, size);
        log.debug("Execute JBossQL: " + sb);
        return sb.toString();
    }

    /**
     * @ejb.home-method
     */
    public Collection ejbHomeListByPatientAndSRCode(PatientLocal pat,
            Collection srCodes, Collection cuids) throws FinderException {
        StringBuffer jbossQl = new StringBuffer(
                "SELECT OBJECT(i) FROM Instance i");
        jbossQl.append(" WHERE i.series.study.patient = ?1");
        ArrayList params = new ArrayList();
        params.add(pat);
        int idx = 2;
        if (srCodes != null && srCodes.size() > 0) {
            jbossQl
                    .append(" AND CONCAT(i.srCode.codeValue, CONCAT('^', i.srCode.codingSchemeDesignator) )");
            idx = addIN(jbossQl, idx, srCodes.size());
            params.addAll(srCodes);
        }
        if (cuids != null && cuids.size() > 0) {
            jbossQl.append(" AND i.sopCuid");
            idx = addIN(jbossQl, idx, cuids.size());
            params.addAll(cuids);
        }
        log.debug("Execute JBossQL: " + jbossQl);
        return ejbSelectGeneric(jbossQl.toString(), params.toArray());
    }

    private int addIN(StringBuffer jbossQl, int idx, int len) {
        if (len > 1) {
            jbossQl.append(" IN ( ");
            for (int i = 1; i < len; i++) {
                jbossQl.append("?").append(idx++).append(", ");
            }
            jbossQl.append("?").append(idx++).append(")");
        } else {
            jbossQl.append(" = ?").append(idx++);
        }
        return idx;
    }

    /**
     * 
     * @ejb.interface-method
     */
    public String asString() {
        return prompt();
    }

    private String prompt() {
        return "Instance[pk=" + getPk() + ", iuid=" + getSopIuid() + ", cuid="
                + getSopCuid() + ", series->" + getSeries() + "]";
    }
}
