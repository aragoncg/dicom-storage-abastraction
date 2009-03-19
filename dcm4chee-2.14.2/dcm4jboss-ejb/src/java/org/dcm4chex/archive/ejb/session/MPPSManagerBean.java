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

package org.dcm4chex.archive.ejb.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.ObjectNotFoundException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Status;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.DcmServiceException;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.MPPSLocal;
import org.dcm4chex.archive.ejb.interfaces.MPPSLocalHome;
import org.dcm4chex.archive.ejb.interfaces.MWLItemLocal;
import org.dcm4chex.archive.ejb.interfaces.MWLItemLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.exceptions.PatientMismatchException;

/**
 * @author gunter.zeilinter@tiani.com
 * @version $Revision: 8636 $ $Date: 2008-01-08 11:18:14 +0100 (Tue, 08 Jan
 *          2008) $
 * @since 21.03.2004
 * 
 * @ejb.bean name="MPPSManager" type="Stateless" view-type="remote"
 *           jndi-name="ejb/MPPSManager"
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * @ejb.ejb-ref ejb-name="Patient" view-type="local" ref-name="ejb/Patient"
 * @ejb.ejb-ref ejb-name="MPPS" view-type="local" ref-name="ejb/MPPS"
 * @ejb.ejb-ref ejb-name="MWLItem" view-type="local" ref-name="ejb/MWLItem"
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 * 
 */
public abstract class MPPSManagerBean implements SessionBean {

    private static Logger log = Logger.getLogger(MPPSManagerBean.class);
    private static final String NO_LONGER_BE_UPDATED_ERR_MSG = "Performed Procedure Step Object may no longer be updated";
    private static final int NO_LONGER_BE_UPDATED_ERR_ID = 0xA710;
    private static final int DELETED = 1;
    private static final int[] PATIENT_ATTRS_EXC = { Tags.RefPatientSeq,
            Tags.PatientName, Tags.PatientID, Tags.IssuerOfPatientID,
            Tags.PatientBirthDate, Tags.PatientSex, };
    private static final int[] PATIENT_ATTRS_INC = { Tags.PatientName,
            Tags.PatientID, Tags.IssuerOfPatientID, Tags.PatientBirthDate,
            Tags.PatientSex, };
    private PatientLocalHome patHome;
    private SeriesLocalHome seriesHome;
    private InstanceLocalHome instHome;
    private MPPSLocalHome mppsHome;
    private MWLItemLocalHome mwlItemHome;
    private SessionContext sessionCtx;

    public void setSessionContext(SessionContext ctx) {
        sessionCtx = ctx;
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            patHome = (PatientLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Patient");
            seriesHome = (SeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Series");
            instHome = (InstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Instance");
            mppsHome = (MPPSLocalHome) jndiCtx.lookup("java:comp/env/ejb/MPPS");
            mwlItemHome = (MWLItemLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/MWLItem");
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

    public void unsetSessionContext() {
        sessionCtx = null;
        mppsHome = null;
        patHome = null;
        seriesHome = null;
        instHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public void createMPPS(Dataset ds) throws DcmServiceException {
        checkDuplicate(ds.getString(Tags.SOPInstanceUID));
        try {
            mppsHome.create(ds.subSet(PATIENT_ATTRS_EXC, true, true),
                    findOrCreatePatient(ds));
        } catch (CreateException e) {
            log.error("Creation of MPPS(iuid="
                    + ds.getString(Tags.SOPInstanceUID) + ") failed: ", e);
            throw new DcmServiceException(Status.ProcessingFailure);
        }
    }

    private void checkDuplicate(String ppsiuid) throws DcmServiceException {
        try {
            mppsHome.findBySopIuid(ppsiuid);
            throw new DcmServiceException(Status.DuplicateSOPInstance);
        } catch (ObjectNotFoundException e) { // Ok
        } catch (FinderException e) {
            log.error("Query for GMPS(iuid=" + ppsiuid + ") failed: ", e);
            throw new DcmServiceException(Status.ProcessingFailure);
        }
    }

    private PatientLocal findOrCreatePatient(Dataset ds)
            throws DcmServiceException {
        try {
            Collection c = patHome.selectByPatientDemographic(ds);
            if (c.size() == 1) {
                return patHome.followMergedWith(
                        (PatientLocal) c.iterator().next());
            }
            return patHome.create(ds.subSet(PATIENT_ATTRS_INC));
        } catch (Exception e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    /**
     * @ejb.interface-method
     */
    public Dataset getMPPS(String iuid) throws FinderException {
        final MPPSLocal mpps = mppsHome.findBySopIuid(iuid);
        final PatientLocal pat = mpps.getPatient();
        Dataset attrs = mpps.getAttributes();
        attrs.putAll(pat.getAttributes(false));
        return attrs;
    }

    /**
     * @ejb.interface-method
     */
    public void updateMPPS(Dataset ds) throws DcmServiceException {
        MPPSLocal mpps;
        try {
            mpps = mppsHome.findBySopIuid(ds.getString(Tags.SOPInstanceUID));
        } catch (ObjectNotFoundException e) {
            throw new DcmServiceException(Status.NoSuchObjectInstance);
        } catch (FinderException e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
        if (!"IN PROGRESS".equals(mpps.getPpsStatus())) {
            DcmServiceException e = new DcmServiceException(
                    Status.ProcessingFailure, NO_LONGER_BE_UPDATED_ERR_MSG);
            e.setErrorID(NO_LONGER_BE_UPDATED_ERR_ID);
            throw e;
        }
        Dataset attrs = mpps.getAttributes();
        attrs.putAll(ds);
        mpps.setAttributes(attrs);
    }

    /**
     * @ejb.interface-method
     */
    public Dataset createIANwithPatSummaryAndStudyID(String iuid) {
        final MPPSLocal mpps;
        try {
            mpps = mppsHome.findBySopIuid(iuid);
        } catch (ObjectNotFoundException onf) {
            return null;
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        Dataset attrs = mpps.getAttributes();
        Dataset ian = DcmObjectFactory.getInstance().newDataset();
        DcmElement refSeriesSq = ian.putSQ(Tags.RefSeriesSeq);
        DcmElement perfSeriesSq = attrs.get(Tags.PerformedSeriesSeq);
        for (int i = 0, n = perfSeriesSq.countItems(); i < n; i++) {
            Dataset perfSeries = perfSeriesSq.getItem(i);
            String seriesIUID = perfSeries.getString(Tags.SeriesInstanceUID);
            DcmElement refImageSeq = perfSeries.get(Tags.RefImageSeq);
            DcmElement refNonImageSeq = perfSeries
                    .get(Tags.RefNonImageCompositeSOPInstanceSeq);
            final Collection insts;
            try {
                insts = instHome.findBySeriesIuid(seriesIUID);
            } catch (FinderException e) {
                throw new EJBException(e);
            }
            int countImage = refImageSeq != null ? refImageSeq.countItems() : 0;
            int countNonImage = refNonImageSeq != null ? refNonImageSeq
                    .countItems() : 0;
            if (insts.size() < countImage + countNonImage) {
                return null;
            }
            Dataset refSeries = refSeriesSq.addNewItem();
            refSeries.putUI(Tags.SeriesInstanceUID, seriesIUID);
            DcmElement refSOPSeq = refSeries.putSQ(Tags.RefSOPSeq);
            if (refImageSeq != null
                    && !containsAll(insts, refImageSeq, refSOPSeq)) {
                return null;
            }
            if (refNonImageSeq != null
                    && !containsAll(insts, refNonImageSeq, refSOPSeq)) {
                return null;
            }
        }
        Dataset ssa = attrs.getItem(Tags.ScheduledStepAttributesSeq);
        String studyIUID = ssa != null ? ssa.getString(Tags.StudyInstanceUID)
                : null;
        DcmElement refPPSSeq = ian.putSQ(Tags.RefPPSSeq);
        Dataset refPPS = refPPSSeq.addNewItem();
        refPPS.putUI(Tags.RefSOPClassUID, UIDs.ModalityPerformedProcedureStep);
        refPPS.putUI(Tags.RefSOPInstanceUID, iuid);
        refPPS.putSQ(Tags.PerformedWorkitemCodeSeq);
        ian.putUI(Tags.StudyInstanceUID, studyIUID);
        ian.putSH(Tags.StudyID, attrs.getString(Tags.StudyID));
        Dataset patAttrs = mpps.getPatient().getAttributes(false);
        ian.putPN(Tags.PatientName, patAttrs.getString(Tags.PatientName));
        ian.putLO(Tags.PatientID, patAttrs.getString(Tags.PatientID));
        return ian;
    }

    private static boolean containsAll(Collection insts,
            DcmElement srcRefSOPSeq, DcmElement dstRefSOPSeq) {
        for (int i = 0, n = srcRefSOPSeq.countItems(); i < n; i++) {
            Dataset srcRefSOP = srcRefSOPSeq.getItem(i);
            String iuid = srcRefSOP.getString(Tags.RefSOPInstanceUID);
            InstanceLocal inst = selectByIuid(insts, iuid);
            if (inst == null) {
                return false;
            }
            Dataset dstRefSOP = dstRefSOPSeq.addNewItem();
            dstRefSOP.putUI(Tags.RefSOPClassUID, inst.getSopCuid());
            dstRefSOP.putUI(Tags.RefSOPInstanceUID, iuid);
            dstRefSOP.putCS(Tags.InstanceAvailability, Availability
                    .toString(inst.getAvailabilitySafe()));
            dstRefSOP.putAE(Tags.RetrieveAET, inst.getRetrieveAETs());
        }
        return true;
    }

    private static InstanceLocal selectByIuid(Collection insts, String iuid) {
        for (Iterator iter = insts.iterator(); iter.hasNext();) {
            InstanceLocal inst = (InstanceLocal) iter.next();
            if (inst.getSopIuid().equals(iuid)) {
                return inst;
            }
        }
        return null;
    }

    /**
     * Links a mpps to a mwl entry (LOCAL).
     * <p>
     * This method can be used if MWL entry is locally available.
     * <p>
     * Sets SpsID and AccessionNumber from mwl entry.
     * <P>
     * Returns a Map with following key/value pairs.
     * <dl>
     * <dt>mppsAttrs: (Dataset)</dt>
     * <dd> Attributes of mpps entry. (for notification)</dd>
     * <dt>mwlPat: (Dataset)</dt>
     * <dd> Patient of MWL entry.</dd>
     * <dd> (The dominant patient of patient merge).</dd>
     * <dt>mppsPat: (Dataset)</dt>
     * <dd> Patient of MPPS entry.</dd>
     * <dd> (The merged patient).</dd>
     * </dl>
     * 
     * @param mwlPk
     *                pk to select MWL entry
     * @param mppsIUID
     *                Instance UID of mpps.
     * 
     * @return A map with mpps attributes and patient attributes to merge.
     * 
     * @ejb.interface-method
     */
    public Map linkMppsToMwl(String rpid, String spsid, String mppsIUID)
            throws DcmServiceException {
        log.info("linkMppsToMwl spsId:" + spsid + " mpps:" + mppsIUID);
        MWLItemLocal mwlItem;
        MPPSLocal mpps;
        try {
            mwlItem = mwlItemHome.findByRpIdAndSpsId(rpid, spsid);
            mpps = mppsHome.findBySopIuid(mppsIUID);
            PatientLocal mwlPat = mwlItem.getPatient();
            PatientLocal mppsPat = mpps.getPatient();
            Dataset mwlAttrs = mwlItem.getAttributes();
            Map map = updateLinkedMpps(mpps, mwlItem, mwlAttrs);
            if (!mwlPat.equals(mppsPat)) {
                map.put("mwlPat", mwlPat.getAttributes(true));
                map.put("mppsPat", mppsPat.getAttributes(true));
            }
            return map;
        } catch (ObjectNotFoundException e) {
            throw new DcmServiceException(Status.NoSuchObjectInstance);
        } catch (FinderException e) {
            throw new DcmServiceException(Status.ProcessingFailure, e);
        }
    }

    private Map updateLinkedMpps(MPPSLocal mpps, MWLItemLocal mwlItem,
            Dataset mwlAttrs) {
        Map map = new HashMap();
        Dataset ssa;
        Dataset mppsAttrs = mpps.getAttributes();
        log.debug("MPPS attrs:");
        log.debug(mppsAttrs);
        log.debug("MWL attrs:");
        log.debug(mwlAttrs);
        String rpid = mwlAttrs.getString(Tags.RequestedProcedureID);
        String spsid = mwlAttrs.getItem(Tags.SPSSeq).getString(Tags.SPSID);
        String accNo = mwlAttrs.getString(Tags.AccessionNumber);
        DcmElement ssaSQ = mppsAttrs.get(Tags.ScheduledStepAttributesSeq);
        String ssaSpsID, studyIUID = null;
        boolean spsNotInList = true;
        for (int i = 0, len = ssaSQ.countItems(); i < len; i++) {
            ssa = ssaSQ.getItem(i);
            if (ssa != null) {
                if (studyIUID == null) {
                    studyIUID = ssa.getString(Tags.StudyInstanceUID);
                    if (!studyIUID.equals(mwlAttrs
                            .getString(Tags.StudyInstanceUID))) {
                        if (mwlItem != null) {
                            log.info("StudyInstanceUID corrected for spsID "
                                    + spsid);
                            mwlAttrs.putUI(Tags.StudyInstanceUID, studyIUID);
                            mwlItem.setAttributes(mwlAttrs);
                        } else {
                            log
                                    .warn("StudyInstanceUID of external MWL entry can not be corrected! spsID "
                                            + spsid);
                            log.warn("--- StudyIUID MWL:"
                                    + mwlAttrs.getString(Tags.StudyInstanceUID)
                                    + "   StudyIUID MPPS:" + studyIUID);
                        }
                    }
                }
                ssaSpsID = ssa.getString(Tags.SPSID);
                if (ssaSpsID == null || spsid.equals(ssaSpsID)) {
                    ssa.putSH(Tags.AccessionNumber, accNo);
                    ssa.putSH(Tags.SPSID, spsid);
                    ssa.putSH(Tags.RequestedProcedureID, rpid);
                    ssa.putUI(Tags.StudyInstanceUID, studyIUID);
                    spsNotInList = false;
                }
            }
        }
        if (spsNotInList) {
            ssa = ssaSQ.addNewItem();
            Dataset spsDS = mwlAttrs.getItem(Tags.SPSSeq);
            ssa.putUI(Tags.StudyInstanceUID, studyIUID);
            ssa.putSH(Tags.SPSID, spsid);
            ssa.putSH(Tags.RequestedProcedureID, rpid);
            ssa.putSH(Tags.AccessionNumber, accNo);
            ssa.putSQ(Tags.RefStudySeq);
            ssa.putSH(Tags.RequestedProcedureID, mwlAttrs
                    .getString(Tags.RequestedProcedureID));
            ssa
                    .putLO(Tags.SPSDescription, spsDS
                            .getString(Tags.SPSDescription));
            DcmElement mppsSPCSQ = ssa.putSQ(Tags.ScheduledProtocolCodeSeq);
            DcmElement mwlSPCSQ = spsDS.get(Tags.ScheduledProtocolCodeSeq);
            if (mwlSPCSQ != null && mwlSPCSQ.countItems() > 0) {
                for (int i = 0, len = mwlSPCSQ.countItems(); i < len; i++) {
                    mppsSPCSQ.addNewItem().putAll(mwlSPCSQ.getItem(i));
                }
            }
            log.debug("add new scheduledStepAttribute item:");
            log.debug(ssa);
            log.debug("new mppsAttrs:");
            log.debug(mppsAttrs);
        }
        mppsAttrs.putAll(mpps.getPatient().getAttributes(false));
        mpps.setAttributes(mppsAttrs);
        map.put("mppsAttrs", mppsAttrs);
        map.put("mwlAttrs", mwlAttrs);
        return map;
    }

    /**
     * Links a mpps to a mwl entry (external).
     * <p>
     * This Method can be used to link a MPPS entry with an MWL entry from an
     * external Modality Worklist.
     * <p>
     * Sets SpsID and AccessionNumber from mwlDs.
     * <P>
     * Returns a Map with following key/value pairs.
     * <dl>
     * <dt>mppsAttrs: (Dataset)</dt>
     * <dd> Attributes of mpps entry. (for notification)</dd>
     * <dt>mwlPat: (Dataset)</dt>
     * <dd> Patient of MWL entry.</dd>
     * <dd> (The dominant patient of patient merge).</dd>
     * <dt>mppsPat: (Dataset)</dt>
     * <dd> Patient of MPPS entry.</dd>
     * <dd> (The merged patient).</dd>
     * </dl>
     * 
     * @param mwlDs
     *                Datset of MWL entry
     * @param mppsIUID
     *                Instance UID of mpps.
     * 
     * @return A map with mpps attributes and patient attributes to merge.
     * @throws FinderException
     * @throws CreateException
     * 
     * @ejb.interface-method
     */
    public Map linkMppsToMwl(Dataset mwlAttrs, String mppsIUID)
            throws DcmServiceException, FinderException, CreateException {
        String spsID = mwlAttrs.get(Tags.SPSSeq).getItem()
                .getString(Tags.SPSID);
        log.info("linkMppsToMwl sps:" + spsID + " mpps:" + mppsIUID);
        MPPSLocal mpps = mppsHome.findBySopIuid(mppsIUID);
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
        Dataset mwlPatDs = filter.filter(mwlAttrs);
        PatientLocal mppsPat = mpps.getPatient();
        Map map = updateLinkedMpps(mpps, null, mwlAttrs);
        if (!isSamePatient(mwlPatDs, mppsPat)) {
            PatientLocal mwlPat;
            try {
                mwlPat = patHome.findByPatientIdWithIssuer(
                        mwlPatDs.getString(Tags.PatientID),
                        mwlPatDs.getString(Tags.IssuerOfPatientID));
            } catch (ObjectNotFoundException onfe) {
                mwlPat = patHome.create(mwlPatDs);
            }
            map.put("mwlPat", mwlPat.getAttributes(true));
            map.put("mppsPat", mppsPat.getAttributes(true));
        }
        return map;
    }

    private boolean isSamePatient(Dataset mwlPatDs, PatientLocal mppsPat) {
        String mppsPatId = mppsPat.getPatientId();
        if (mppsPatId == null) {
            log
                    .warn("Link MPPS to MWL: MPPS patient without PatientID! try to check via Patient Name");
            String name = mppsPat.getPatientName();
            if (name == null) {
                log
                        .error("Link MPPS to MWL: MPPS patient without Patient Name! Ignore differences to avoid merge!");
                return true;
            }
            return name.equals(mwlPatDs.getString(Tags.PatientName));
        } else if (!mppsPat.getPatientId().equals(
                mwlPatDs.getString(Tags.PatientID)))
            return false;
        String issuer = mppsPat.getIssuerOfPatientId();
        return (issuer != null) ? issuer.equals(mwlPatDs
                .getString(Tags.IssuerOfPatientID)) : true;
    }

    /**
     * @ejb.interface-method
     */
    public void unlinkMpps(String mppsIUID) throws FinderException {
        MPPSLocal mpps = mppsHome.findBySopIuid(mppsIUID);
        Dataset mppsAttrs = mpps.getAttributes();
        DcmElement ssaSQ = mppsAttrs.get(Tags.ScheduledStepAttributesSeq);
        Dataset ds = null;
        String rpID, spsID;
        for (int i = ssaSQ.countItems() - 1; i >= 0; i--) {
            ds = ssaSQ.getItem(i);
            rpID = ds.getString(Tags.RequestedProcedureID);
            spsID = ds.getString(Tags.SPSID);
            if (spsID != null) {
                try {
                    MWLItemLocal mwlItem = mwlItemHome.findByRpIdAndSpsId(rpID,
                            spsID);
                    Dataset mwlDS = mwlItem.getAttributes();
                    mwlDS.getItem(Tags.SPSSeq).putCS(Tags.SPSStatus,
                            "SCHEDULED");
                    mwlItem.setAttributes(mwlDS);
                } catch (FinderException ignore) {
                }
            }
        }
        String studyIUID = ds.getString(Tags.StudyInstanceUID);
        ds.clear();
        ds.putUI(Tags.StudyInstanceUID, studyIUID);
        // add empty type 2 attributes.
        ds.putSH(Tags.SPSID, (String) null);
        ds.putSH(Tags.AccessionNumber, (String) null);
        ds.putSQ(Tags.RefStudySeq);
        ds.putSH(Tags.RequestedProcedureID, (String) null);
        ds.putLO(Tags.SPSDescription, (String) null);
        ds.putSQ(Tags.ScheduledProtocolCodeSeq);
        mppsAttrs.putSQ(Tags.ScheduledStepAttributesSeq).addItem(ds);
        mpps.setAttributes(mppsAttrs);
    }

    /**
     * @ejb.interface-method
     */
    public Collection getSeriesIUIDs(String mppsIUID) throws FinderException {
        Collection col = new ArrayList();
        Collection series = seriesHome.findByPpsIuid(mppsIUID);
        for (Iterator iter = series.iterator(); iter.hasNext();) {
            col.add(((SeriesLocal) iter.next()).getSeriesIuid());
        }
        return col;
    }

    /**
     * @ejb.interface-method
     */
    public Collection getSeriesAndStudyDS(String mppsIUID)
            throws FinderException {
        Collection col = new ArrayList();
        Collection seriess = seriesHome.findByPpsIuid(mppsIUID);
        SeriesLocal series;
        Dataset ds;
        for (Iterator iter = seriess.iterator(); iter.hasNext();) {
            series = (SeriesLocal) iter.next();
            ds = series.getAttributes(true);
            ds.putAll(series.getStudy().getAttributes(true));
            col.add(ds);
        }
        return col;
    }

    /**
     * Returns a StudyMgt Dataset.
     * 
     * @ejb.interface-method
     */
    public Dataset updateSeriesAndStudy(Collection seriesDS)
            throws FinderException, CreateException {
        Dataset ds = null;
        String iuid;
        SeriesLocal series = null;
        Dataset dsN = DcmObjectFactory.getInstance().newDataset();
        DcmElement refSeriesSeq = dsN.putSQ(Tags.RefSeriesSeq);
        Dataset dsSer;
        for (Iterator iter = seriesDS.iterator(); iter.hasNext();) {
            ds = (Dataset) iter.next();
            iuid = ds.getString(Tags.SeriesInstanceUID);
            series = seriesHome.findBySeriesIuid(iuid);
            series.updateAttributes(ds, true);
            dsSer = refSeriesSeq.addNewItem();
            dsSer.putAll(series.getAttributes(true));
            Iterator iter2 = series.getInstances().iterator();
            if (iter2.hasNext()) {
                DcmElement refSopSeq = dsSer.putSQ(Tags.RefSOPSeq);
                InstanceLocal il;
                Dataset dsInst;
                while (iter2.hasNext()) {
                    il = (InstanceLocal) iter2.next();
                    dsInst = refSopSeq.addNewItem();
                    dsInst.putUI(Tags.RefSOPClassUID, il.getSopCuid());
                    dsInst.putUI(Tags.RefSOPInstanceUID, il.getSopIuid());
                    dsInst.putAE(Tags.RetrieveAET, il.getRetrieveAETs());
                }
            }
        }
        if (series != null) {
            StudyLocal study = series.getStudy();
            study.setAttributes(ds);
            dsN.putAll(study.getAttributes(true));
        }
        return dsN;
    }

    /**
     * @ejb.interface-method
     */
    public void updateSeriesAttributes(String uid, Dataset newAttrs,
            boolean updateStudyAttributes) {
        try {
            SeriesLocal series = seriesHome.findBySeriesIuid(uid);
            series.updateAttributes(newAttrs, true);
            if (updateStudyAttributes) {
                series.getStudy().updateAttributes(newAttrs);
            }
        } catch (FinderException e) {
            throw new EJBException(e);
        }
    }

    /**
     * Update Scheduled Step Attributes on receive of ORM^O01 message AFTER
     * acquisition and storage of objects
     * 
     * @throws PatientMismatchException
     * @ejb.interface-method
     */
    public List updateScheduledStepAttributes(Dataset mwlitem)
            throws PatientMismatchException {
        // query for already received MPPS for scheduled/updated procedure
        String suid = mwlitem.getString(Tags.StudyInstanceUID);
        if (log.isDebugEnabled()) {
            log.debug("Query for MPPS for Study " + suid);
        }
        Collection c;
        try {
            c = mppsHome.mppsByStudyIuid(suid);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Found " + c.size() + " MPPS for Study " + suid);
        }
        if (c.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        PatientLocal pat;
        try {
            pat = patHome.selectByPatientId(mwlitem);
        } catch (FinderException e) {
            throw new EJBException(e);
        }
        List updated = new ArrayList(c.size());
        for (Iterator it = c.iterator(); it.hasNext();) {
            MPPSLocal mpps = (MPPSLocal) it.next();
            if (!pat.isIdentical(mpps.getPatient())) {
                String prompt = "Patient[pid="
                        + mwlitem.getString(Tags.PatientID) + ", issuer="
                        + mwlitem.getString(Tags.IssuerOfPatientID)
                        + "] does not match Patient associated with "
                        + mpps.asString();
                log.warn(prompt);
                throw new PatientMismatchException(prompt);
            }
            Dataset attrs = mpps.getAttributes();
            if (log.isDebugEnabled()) {
                log.debug("Check " + mpps.asString()
                        + " for update of Scheduled Step Attributes:");
                log.debug(attrs);
            }
            DcmElement ssasq = attrs.get(Tags.ScheduledStepAttributesSeq);
            if (updateScheduledStepAttributes(mwlitem, ssasq)) {
                mpps.setAttributes(attrs);
                updated.add(attrs);
                log.info("Updated Scheduled Step Attributes of "
                        + mpps.asString());
                if (log.isDebugEnabled()) {
                    log.debug(attrs);
                }
            }
        }
        return updated;
    }

    private boolean updateScheduledStepAttributes(Dataset mwlitem,
            DcmElement ssasq) {
        String suid = mwlitem.getString(Tags.StudyInstanceUID);
        String rpid = mwlitem.getString(Tags.RequestedProcedureID);
        Dataset sps = mwlitem.getItem(Tags.SPSSeq);
        String spsid = sps.getString(Tags.SPSID);
        for (int i = 0, n = ssasq.countItems(); i < n; i++) {
            Dataset ssa = ssasq.getItem(i);
            if (suid.equals(ssa.getString(Tags.StudyInstanceUID))
                    && isNullOrEquals(ssa.getString(Tags.RequestedProcedureID),
                            rpid)
                    && isNullOrEquals(ssa.getString(Tags.SPSID), spsid)) {
                boolean updated = updateRefStudySeq(ssa, suid);
                if (updateSH(ssa, Tags.AccessionNumber, mwlitem
                        .getString(Tags.AccessionNumber))) {
                    updated = true;
                }
                if (updateLO(ssa, Tags.PlacerOrderNumber, mwlitem
                        .getString(Tags.PlacerOrderNumber))) {
                    updated = true;
                }
                if (updateLO(ssa, Tags.FillerOrderNumber, mwlitem
                        .getString(Tags.FillerOrderNumber))) {
                    updated = true;
                }
                if (updateSH(ssa, Tags.RequestedProcedureID, rpid)) {
                    updated = true;
                }
                if (updateLO(ssa, Tags.RequestedProcedureDescription, mwlitem
                        .getString(Tags.RequestedProcedureDescription))) {
                    updated = true;
                }
                if (updateItem(ssa, Tags.RequestedProcedureCodeSeq, mwlitem
                        .getItem(Tags.RequestedProcedureCodeSeq))) {
                    updated = true;
                }
                if (updateSH(ssa, Tags.SPSID, spsid)) {
                    updated = true;
                }
                if (updateLO(ssa, Tags.SPSDescription, sps
                        .getString(Tags.SPSDescription))) {
                    updated = true;
                }
                if (updateItems(ssa, Tags.ScheduledProtocolCodeSeq, sps
                        .get(Tags.ScheduledProtocolCodeSeq))) {
                    updated = true;
                }
                return updated;
            }
        }
        return false;
    }

    private boolean isNullOrEquals(String val, String ref) {
        return val == null || val.equals(ref);
    }

    private boolean updateRefStudySeq(Dataset ssa, String suid) {
        if (ssa.getItem(Tags.RefStudySeq) != null) {
            return false;
        }
        Dataset refStudy = ssa.putSQ(Tags.RefStudySeq).addNewItem();
        refStudy.putUI(Tags.RefSOPClassUID, UIDs.DetachedStudyManagement);
        refStudy.putUI(Tags.RefSOPInstanceUID, suid);
        return true;
    }

    private boolean updateSH(Dataset ssa, int tag, String val) {
        if (val == null || val.equals(ssa.getString(tag))) {
            return false;
        }
        ssa.putSH(tag, val);
        return true;
    }

    private boolean updateLO(Dataset ssa, int tag, String val) {
        if (val == null || val.equals(ssa.getString(tag))) {
            return false;
        }
        ssa.putLO(tag, val);
        return true;
    }

    private boolean updateItem(Dataset ssa, int tag, Dataset item) {
        if (item == null || item.equals(ssa.getItem(tag))) {
            return false;
        }
        ssa.putSQ(tag).addItem(item);
        return true;
    }

    private boolean updateItems(Dataset ssa, int tag, DcmElement sq) {
        if (sq == null || sq.isEmpty() || sq.equals(ssa.get(tag))) {
            return false;
        }
        DcmElement dstsq = ssa.putSQ(tag);
        for (int i = 0, n = sq.countItems(); i < n; i++) {
            dstsq.addItem(sq.getItem(i));
        }
        return true;
    }

}
