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

package org.dcm4chex.archive.ejb.jdbc;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.security.auth.Subject;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.DcmValueException;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.VRs;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SecurityUtils;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.jdbc.Match.Node;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 8622 $ $Date: 2008-12-08 18:32:43 +0100 (Mon, 08 Dec 2008) $
 */
public abstract class QueryCmd extends BaseDSQueryCmd {
    
    private static final int[] MATCHING_PATIENT_KEYS = new int[] {
            Tags.PatientID, 
            Tags.IssuerOfPatientID, 
            Tags.OtherPatientIDSeq,
            Tags.PatientName, 
            Tags.PatientBirthDate, 
            Tags.PatientBirthTime,
            Tags.PatientSex,
            };

    private static final int[] MATCHING_STUDY_KEYS = new int[] {
            Tags.StudyInstanceUID, 
            Tags.StudyID, 
            Tags.StudyDate,
            Tags.StudyTime, 
            Tags.AccessionNumber, 
            Tags.ReferringPhysicianName,
            Tags.StudyDescription,
            Tags.StudyStatusID,
            };

    private static final int[] MATCHING_SERIES_KEYS = new int[] {
            Tags.SeriesInstanceUID, 
            Tags.SeriesNumber,
            Tags.Modality,
            Tags.ModalitiesInStudy, 
            Tags.InstitutionName,
            Tags.StationName,
            Tags.SeriesDescription,
            Tags.InstitutionalDepartmentName,
            Tags.PerformingPhysicianName,
            Tags.BodyPartExamined,
            Tags.Laterality,
            Tags.PPSStartDate,
            Tags.PPSStartTime, 
            Tags.RequestAttributesSeq,
            };

    private static final int[] MATCHING_INSTANCE_KEYS = new int[] {
            Tags.SOPInstanceUID, 
            Tags.SOPClassUID, 
            Tags.InstanceNumber,
            Tags.VerificationFlag, 
            Tags.ContentDate, 
            Tags.ContentTime,
            Tags.CompletionFlag, 
            Tags.VerificationFlag,
            Tags.ConceptNameCodeSeq,
            Tags.VerifyingObserverSeq};

    private static final int[] MATCHING_VERIFYING_OBSERVER = new int[] {
            Tags.VerificationDateTime,
            Tags.VerifyingObserverName };

    private static final int[] MATCHING_REQ_ATTR_KEYS = new int[] {
            Tags.StudyInstanceUID,
            Tags.RequestedProcedureID,
            Tags.SPSID, 
            Tags.RequestingService,
            Tags.RequestingPhysician };

    private static final int[] MATCHING_OTHER_PAT_ID_SEQ = new int[] {
            Tags.PatientID, 
            Tags.IssuerOfPatientID, 
            Tags.TypeOfPatientID // We cannot match but should add here to avoid warnings.
    };

    private static final int[] ATTR_IGNORE_DIFF_LOG = new int[] {
            Tags.PatientID,
            Tags.IssuerOfPatientID,
            Tags.OtherPatientIDSeq };

    private static final String[] AVAILABILITY = { 
            "ONLINE", "NEARLINE", "OFFLINE", "UNAVAILABLE" };

    private static final String SR_CODE = "sr_code";

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;
    public static int seriesBlobAccessType = Types.BLOB;
    public static boolean lazyFetchSeriesAttrsOnImageLevelQuery = false;
    public static boolean cacheSeriesAttrsOnImageLevelQuery = true;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private boolean otherPatientIDMatchNotSupported = false;

    private HashMap chkPatAttrs;

    private boolean coercePatientIds = false;

    protected boolean noMatchWithoutIssuerOfPID;

    protected final Subject subject;

    public void setCoercePatientIds( boolean coercePatientIds ) {
        this.coercePatientIds = coercePatientIds;
    }
    
    public static QueryCmd create(Dataset keys, boolean filterResult,
            boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        String qrLevel = keys.getString(Tags.QueryRetrieveLevel);
        if ("IMAGE".equals(qrLevel))
            return createInstanceQuery(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("SERIES".equals(qrLevel))
            return createSeriesQuery(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("STUDY".equals(qrLevel))
            return createStudyQuery(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        if ("PATIENT".equals(qrLevel))
            return createPatientQuery(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
        throw new IllegalArgumentException("QueryRetrieveLevel=" + qrLevel);
    }

    public static PatientQueryCmd createPatientQuery(Dataset keys,
            boolean filterResult, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final PatientQueryCmd cmd = new PatientQueryCmd(keys, filterResult,
                noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static StudyQueryCmd createStudyQuery(Dataset keys,
            boolean filterResult, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final StudyQueryCmd cmd = new StudyQueryCmd(keys, filterResult,
                noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static SeriesQueryCmd createSeriesQuery(Dataset keys,
            boolean filterResult, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final SeriesQueryCmd cmd = new SeriesQueryCmd(keys, filterResult,
                noMatchForNoValue, noMatchWithoutIssuerOfPID, subject);
        cmd.init();
        return cmd;
    }

    public static ImageQueryCmd createInstanceQuery(Dataset keys,
            boolean filterResult, boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        final ImageQueryCmd cmd = new ImageQueryCmd(keys, filterResult,
                noMatchForNoValue, noMatchWithoutIssuerOfPID,
                subject);
        cmd.init();
        return cmd;
    }

    protected QueryCmd(Dataset keys, boolean filterResult,
            boolean noMatchForNoValue,
            boolean noMatchWithoutIssuerOfPID, Subject subject)
            throws SQLException {
        super(keys, filterResult, noMatchForNoValue, transactionIsolationLevel);
        this.noMatchWithoutIssuerOfPID =
                noMatchWithoutIssuerOfPID;
        this.subject = subject;
        if (!keys.contains(Tags.SpecificCharacterSet)) {
            keys.putCS(Tags.SpecificCharacterSet);
        }
        matchingKeys.add(Tags.QueryRetrieveLevel);
    }

    protected void addAdditionalReturnKeys() {
        keys.putAE(Tags.RetrieveAET);
        keys.putSH(Tags.StorageMediaFileSetID);
        keys.putUI(Tags.StorageMediaFileSetUID);
        keys.putCS(Tags.InstanceAvailability);
    }

    protected void init() {
        sqlBuilder.setSelect(getSelectAttributes());
        sqlBuilder.setFrom(getTables());
        sqlBuilder.setLeftJoin(getLeftJoin());
        sqlBuilder.setRelations(getRelations());
    }

    protected abstract String[] getSelectAttributes();

    protected abstract String[] getTables();

    protected String[] getLeftJoin() {
        return null;
    }

    protected String[] getRelations() {
        return null;
    }

    public void execute() throws SQLException {
        execute(sqlBuilder.getSql());
    }

    public boolean isMatchNotSupported() {
        return sqlBuilder.isMatchNotSupported();
    }

    /**
     * Check if this QueryCmd use an unsupported matching key.
     * 
     * @return true if an unsupported matching key is found!
     */
    public boolean isMatchingKeyNotSupported() {
        return otherPatientIDMatchNotSupported
                || super.isMatchingKeyNotSupported();
    }

    protected void addPatientMatch() {
        sqlBuilder.addLiteralMatch(null, "Patient.merge_fk", false, "IS NULL");
        DcmElement otherPatIdSQ = getOtherPatientIdMatchSQ();
        if (otherPatIdSQ != null) {
            addListOfPatIdMatch(otherPatIdSQ);
        } else {
            sqlBuilder.addWildCardMatch(null, "Patient.patientId", type2, keys
                    .getStrings(Tags.PatientID));
            String issuer = keys.getString(Tags.IssuerOfPatientID);
            if (issuer != null) {
                sqlBuilder.addSingleValueMatch(null, "Patient.issuerOfPatientId",
                        type2, issuer);
            } else if (noMatchWithoutIssuerOfPID) {
                sqlBuilder.addNULLValueMatch(null,"Patient.issuerOfPatientId",
                        true);
            }
        }
        sqlBuilder.addPNMatch(
                new String[] { "Patient.patientName",
                        "Patient.patientIdeographicName",
                        "Patient.patientPhoneticName" }, type2, keys
                        .getString(Tags.PatientName));
        sqlBuilder
                .addRangeMatch(null, "Patient.patientBirthDate", type2,
                        keys.getString(Tags.PatientBirthDate));
        sqlBuilder.addWildCardMatch(null, "Patient.patientSex", type2, keys
                .getStrings(Tags.PatientSex));
        AttributeFilter filter = AttributeFilter.getPatientAttributeFilter();
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Patient." + filter.getField(fieldTags[i]), type2,
                    keys.getStrings(fieldTags[i]));
            
        }
        matchingKeys.add(MATCHING_PATIENT_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(new Integer(Tags.OtherPatientIDSeq), new IntList()
                .add(MATCHING_OTHER_PAT_ID_SEQ));
    }

    private DcmElement getOtherPatientIdMatchSQ() {
        DcmElement otherPatIdSQ = keys.get(Tags.OtherPatientIDSeq);
        if (otherPatIdSQ == null || !otherPatIdSQ.hasItems())
            return null;
        StringBuffer sb = new StringBuffer();
        String patId = keys.getString(Tags.PatientID);
        if (checkMatchValue(patId, "Patient ID", sb)) {
            String issuer = keys.getString(Tags.IssuerOfPatientID);
            if (checkMatchValue(issuer, "Issuer of Patient ID", sb)) {
                Dataset item;
                for (int i = 0, len = otherPatIdSQ.countItems(); i < len; i++) {
                    item = otherPatIdSQ.getItem(i);
                    if (!checkMatchValue(item.getString(Tags.PatientID),
                            "PatientID of item", sb)
                            || !checkMatchValue(item
                                    .getString(Tags.IssuerOfPatientID),
                                    "Issuer of item", sb)) {
                        break;
                    }
                }
                if (sb.length() == 0)
                    return otherPatIdSQ;
            }
        }
        log.warn("Matching of items in OtherPatientIdSequence disabled! Reason: " + sb);
        otherPatientIDMatchNotSupported = true;
        return null;
    }

    private void addListOfPatIdMatch(DcmElement otherPatIdSQ) {
        Node n = sqlBuilder.addNodeMatch("OR", false);
        addIdAndIssuerPair(n, keys.getString(Tags.PatientID), keys
                .getString(Tags.IssuerOfPatientID));
        Dataset item;
        for (int i = 0, len = otherPatIdSQ.countItems(); i < len; i++) {
            item = otherPatIdSQ.getItem(i);
            addIdAndIssuerPair(n, item.getString(Tags.PatientID), item
                    .getString(Tags.IssuerOfPatientID));
        }
    }

    private void addIdAndIssuerPair(Node n, String patId, String issuer) {
        Node n1 = new Match.Node("AND", false);
        n1.addMatch(new Match.SingleValue(null, "Patient.patientId", type2,
                patId));
        n1.addMatch(new Match.SingleValue(null, "Patient.issuerOfPatientId",
                type2, issuer));
        n.addMatch(n1);
    }

    private boolean checkMatchValue(String value, String chkItem,
            StringBuffer sb) {
        if (value == null) {
            sb.append("Missing attribute ").append(chkItem);
        } else if (value.indexOf('*') != -1 || value.indexOf('?') != -1) {
            sb.append("Wildcard ('*','?') not allowed in ").append(chkItem)
                    .append(" ('").append(value).append("')");
        } else {
            return true;
        }
        return false;
    }

    protected void addStudyMatch() {
        sqlBuilder.addListOfUidMatch(null, "Study.studyIuid", SqlBuilder.TYPE1,
                keys.getStrings(Tags.StudyInstanceUID));
        sqlBuilder.addWildCardMatch(null, "Study.studyId", type2, keys
                .getStrings(Tags.StudyID));
        sqlBuilder.addRangeMatch(null, "Study.studyDateTime", type2, keys
                .getDateTimeRange(Tags.StudyDate, Tags.StudyTime));
        sqlBuilder.addWildCardMatch(null, "Study.accessionNumber", type2, keys
                .getStrings(Tags.AccessionNumber));
        sqlBuilder.addPNMatch(new String[] { "Study.referringPhysicianName",
                "Study.referringPhysicianIdeographicName",
                "Study.referringPhysicianPhoneticName" }, type2, keys
                .getString(Tags.ReferringPhysicianName));
        sqlBuilder.addWildCardMatch(null, "Study.studyDescription", type2,
                SqlBuilder.toUpperCase(keys.getString(Tags.StudyDescription)));
        sqlBuilder.addListOfStringMatch(null, "Study.studyStatusId", type2,
                keys.getStrings(Tags.StudyStatusID));
        AttributeFilter filter = AttributeFilter.getStudyAttributeFilter();
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Study." + filter.getField(fieldTags[i]), type2,
                    keys.getStrings(fieldTags[i]));
            
        }
        matchingKeys.add(MATCHING_STUDY_KEYS);
        matchingKeys.add(fieldTags);
    }


    protected void addStudyPermissionMatch(boolean patientLevel) {
        if (subject != null) {
            sqlBuilder.addQueryPermissionNestedMatch(patientLevel,
                    SecurityUtils.rolesOf(subject));
        }
    }

    
    protected void addNestedSeriesMatch() {
        sqlBuilder.addModalitiesInStudyNestedMatch(null, keys
                .getStrings(Tags.ModalitiesInStudy));
        keys.setPrivateCreatorID(PrivateTags.CreatorID);
        sqlBuilder.addCallingAETsNestedMatch(false, keys
                .getStrings(PrivateTags.CallingAET));
        matchingKeys.add(Tags.ModalitiesInStudy);
        matchingKeys.add(PrivateTags.CallingAET);
    }

    protected void addSeriesMatch() {
        sqlBuilder.addListOfUidMatch(null, "Series.seriesIuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SeriesInstanceUID));
        sqlBuilder.addWildCardMatch(null, "Series.seriesNumber", type2, keys
                .getStrings(Tags.SeriesNumber));
        String[] modality = keys.getStrings(Tags.Modality);
        if (modality == null || modality.length < 1)
            modality = keys.getStrings(Tags.ModalitiesInStudy);
        sqlBuilder.addWildCardMatch(null, "Series.modality", SqlBuilder.TYPE1,
                modality);
        sqlBuilder.addWildCardMatch(null, "Series.seriesNumber", type2,
                keys.getStrings(Tags.SeriesNumber));
        sqlBuilder.addWildCardMatch(null, "Series.bodyPartExamined", type2,
                keys.getStrings(Tags.BodyPartExamined));
        sqlBuilder.addWildCardMatch(null, "Series.laterality", type2,
                keys.getStrings(Tags.Laterality));
        sqlBuilder.addWildCardMatch(null, "Series.institutionName", type2,
                SqlBuilder.toUpperCase(keys.getString(Tags.InstitutionName)));
        sqlBuilder.addWildCardMatch(null, "Series.stationName", type2,
                SqlBuilder.toUpperCase(keys.getString(Tags.StationName)));
        sqlBuilder.addWildCardMatch(null, "Series.seriesDescription", type2,
                SqlBuilder.toUpperCase(keys.getString(Tags.SeriesDescription)));
        sqlBuilder.addWildCardMatch(null, "Series.institutionalDepartmentName",
                type2, SqlBuilder.toUpperCase(keys
                        .getString(Tags.InstitutionalDepartmentName)));
        sqlBuilder.addPNMatch(new String[] { "Series.performingPhysicianName",
                "Series.performingPhysicianIdeographicName",
                "Series.performingPhysicianPhoneticName" }, type2,
                keys.getString(Tags.PerformingPhysicianName));
        sqlBuilder.addRangeMatch(null, "Series.ppsStartDateTime", type2, keys
                .getDateTimeRange(Tags.PPSStartDate, Tags.PPSStartTime));
        keys.setPrivateCreatorID(PrivateTags.CreatorID);
        sqlBuilder.addListOfStringMatch(null, "Series.sourceAET", type2, keys
                .getStrings(PrivateTags.CallingAET));
        if (this.isMatchRequestAttributes()) {
            Dataset rqAttrs = keys.getItem(Tags.RequestAttributesSeq);

            SqlBuilder subQuery = new SqlBuilder();
            subQuery.setSelect(new String[] { "SeriesRequest.pk" });
            subQuery.setFrom(new String[] { "SeriesRequest" });
            subQuery.addFieldValueMatch(null, "Series.pk", SqlBuilder.TYPE1, null,
                    "SeriesRequest.series_fk");
            subQuery.addListOfUidMatch(null, "SeriesRequest.studyIuid", type2,
                    rqAttrs.getStrings(Tags.StudyInstanceUID));
            subQuery.addWildCardMatch(null,
                    "SeriesRequest.requestedProcedureId", SqlBuilder.TYPE1, rqAttrs
                            .getStrings(Tags.RequestedProcedureID));
            subQuery.addWildCardMatch(null, "SeriesRequest.spsId", SqlBuilder.TYPE1,
                    rqAttrs.getStrings(Tags.SPSID));
            subQuery.addWildCardMatch(null, "SeriesRequest.requestingService",
                    type2, rqAttrs.getStrings(Tags.RequestingService));
            subQuery.addPNMatch(new String[] {
                    "SeriesRequest.requestingPhysician",
                    "SeriesRequest.requestingPhysicianIdeographicName",
                    "SeriesRequest.requestingPhysicianPhoneticName" }, type2,
                    rqAttrs.getString(Tags.RequestingPhysician));

            Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
            node0.addMatch(new Match.Subquery(subQuery, null, null));
        }

        AttributeFilter filter = AttributeFilter.getSeriesAttributeFilter();
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Series." + filter.getField(fieldTags[i]), type2,
                    keys.getStrings(fieldTags[i]));
            
        }

        matchingKeys.add(MATCHING_SERIES_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(new Integer(Tags.RequestAttributesSeq),
                new IntList().add(MATCHING_REQ_ATTR_KEYS));
    }

    protected void addInstanceMatch() {
        sqlBuilder.addListOfUidMatch(null, "Instance.sopIuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SOPInstanceUID));
        sqlBuilder.addListOfUidMatch(null, "Instance.sopCuid",
                SqlBuilder.TYPE1, keys.getStrings(Tags.SOPClassUID));
        sqlBuilder.addWildCardMatch(null, "Instance.instanceNumber", type2,
                keys.getStrings(Tags.InstanceNumber));
        sqlBuilder.addRangeMatch(null, "Instance.contentDateTime", type2, keys
                .getDateTimeRange(Tags.ContentDate, Tags.ContentTime));
        sqlBuilder.addSingleValueMatch(null, "Instance.srCompletionFlag",
                type2, keys.getString(Tags.CompletionFlag));
        sqlBuilder.addSingleValueMatch(null, "Instance.srVerificationFlag",
                type2, keys.getString(Tags.VerificationFlag));
        Dataset code = keys.getItem(Tags.ConceptNameCodeSeq);
        if (code != null) {
            sqlBuilder.addSingleValueMatch(SR_CODE, "Code.codeValue", type2,
                    code.getString(Tags.CodeValue));
            sqlBuilder.addSingleValueMatch(SR_CODE,
                    "Code.codingSchemeDesignator", type2, code
                            .getString(Tags.CodingSchemeDesignator));
        }
        if (this.isMatchVerifyingObserver()) {
            Dataset voAttrs = keys.getItem(Tags.VerifyingObserverSeq);

            SqlBuilder subQuery = new SqlBuilder();
            subQuery.setSelect(new String[] { "VerifyingObserver.pk" });
            subQuery.setFrom(new String[] { "VerifyingObserver" });
            subQuery.addFieldValueMatch(null, "Instance.pk", SqlBuilder.TYPE1, null,
                    "VerifyingObserver.instance_fk");
            subQuery.addRangeMatch(null,
                    "VerifyingObserver.verificationDateTime", SqlBuilder.TYPE1,
                    voAttrs.getDateRange(Tags.VerificationDateTime));
            subQuery.addPNMatch(new String[] {
                    "VerifyingObserver.verifyingObserverName",
                    "VerifyingObserver.verifyingObserverIdeographicName",
                    "VerifyingObserver.verifyingObserverPhoneticName" },
                    SqlBuilder.TYPE1,
                    voAttrs.getString(Tags.VerifyingObserverName));

            Match.Node node0 = sqlBuilder.addNodeMatch("OR", false);
            node0.addMatch(new Match.Subquery(subQuery, null, null));
        }
        AttributeFilter filter = AttributeFilter.getInstanceAttributeFilter(null);
        int[] fieldTags = filter.getFieldTags();
        for (int i = 0; i < fieldTags.length; i++) {
            sqlBuilder.addWildCardMatch(null,
                    "Instance." + filter.getField(fieldTags[i]), type2,
                    keys.getStrings(fieldTags[i]));
            
        }
        matchingKeys.add(MATCHING_INSTANCE_KEYS);
        matchingKeys.add(fieldTags);
        seqMatchingKeys.put(new Integer(Tags.ConceptNameCodeSeq), new IntList()
                .add(Tags.CodeValue).add(Tags.CodingSchemeDesignator));
        seqMatchingKeys.put(new Integer(Tags.VerifyingObserverSeq),
                new IntList().add(MATCHING_VERIFYING_OBSERVER));
    }

    public Dataset getDataset() throws SQLException {
        Dataset ds = dof.newDataset();
        fillDataset(ds);
        if (!otherPatientIDMatchNotSupported)
            addOtherPatientSeq(ds, keys);
        adjustDataset(ds, keys);
        return filterResult ? ds.subSet(keys) : ds;
    }

    private void addOtherPatientSeq(Dataset ds, Dataset keys)
    		throws SQLException {
    	DcmElement sq = keys.get(Tags.OtherPatientIDSeq);
    	if (sq != null) {
    		AttributeFilter patientFilter = AttributeFilter.getPatientAttributeFilter();
    		Dataset patientAttrs = patientFilter.filter(ds);
    		checkPatAttrs(patientAttrs);
    		if ( coercePatientIds ) {
    			if (log.isDebugEnabled()) {
    				log.debug("PatientID in response:"
    						+ keys.getString(Tags.PatientID) + "^"
    						+ keys.getString(Tags.IssuerOfPatientID));
    				log.debug("PatientID of match:" + ds.getString(Tags.PatientID)
    						+ "^" + ds.getString(Tags.IssuerOfPatientID));
    			}
    			ds.putAll(keys.subSet(new int[] { Tags.PatientID,
    					Tags.IssuerOfPatientID, Tags.OtherPatientIDSeq }));
    		}
    	}
    }

    private void checkPatAttrs(Dataset ds) throws SQLException {
    	String key = getPatIdString(ds);
    	if (chkPatAttrs == null) {
    		chkPatAttrs = new HashMap();
    		chkPatAttrs.put(key, ds);
    	} else if (!chkPatAttrs.containsKey(key)) {
    		for (Iterator iter = chkPatAttrs.values().iterator(); iter
    		.hasNext();) {
    			logDiffs(ds, (Dataset) iter.next(), key);
    		}
    		chkPatAttrs.put(key, ds);
    	}
    }

    private void logDiffs(Dataset ds, Dataset ds1, String dsPrefix) {
        DcmElement elem, elem1;
        int tag;
        String ds1Prefix = getPatIdString(ds1);
        for (Iterator iter = ds.subSet(ATTR_IGNORE_DIFF_LOG, true, false)
                .iterator(); iter.hasNext();) {
            elem = (DcmElement) iter.next();
            tag = elem.tag();
            elem1 = ds1.get(tag);
            if (log.isDebugEnabled())
                log.debug("compare:" + elem + " with " + elem1);
            if (elem != null && elem1 != null && !checkAttr(elem, elem1)) {
                log.warn("Different patient attribute found! " + dsPrefix
                        + elem + " <-> " + ds1Prefix + elem1);
            }
        }
    }

    private boolean checkAttr(DcmElement elem, DcmElement elem1) {
        if (elem.isEmpty() && elem1.isEmpty())
            return true;
        if (elem.vr() == VRs.PN) {
            return getFnGn(elem).equals(getFnGn(elem1));
        }
        return elem.equals(elem1);
    }

    private String getFnGn(DcmElement el) {
        try {
            String pn = el.getString(null);
            if (pn == null) {
            	return "";
            }
            int pos = pn.indexOf('=');
            if (pos != -1)
                pn = pn.substring(0, pos);
            pos = pn.indexOf('^');
            if (pos != -1) {
                pos = pn.indexOf('^', pos);
                return pos != -1 ? pn.substring(0, pos) : pn;
            } else {
                return pn;
            }
        } catch (DcmValueException x) {
            log.error("Cant get family and given name value of " + el, x);
            return "";
        }
    }

    private String getPatIdString(Dataset ds) {
        return ds.getString(Tags.PatientID) + "^"
                + ds.getString(Tags.IssuerOfPatientID);
    }

    protected abstract void fillDataset(Dataset ds) throws SQLException;

    protected void fillDataset(Dataset ds, int column) throws SQLException {
        DatasetUtils.fromByteArray(rs.getBytes(column), ds);
    }

    public static class PatientQueryCmd extends QueryCmd {

        protected PatientQueryCmd(Dataset keys, boolean filterResult,
                boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
            super(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
            defineColumnTypes(new int[] { blobAccessType });
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyPermissionMatch(true);
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            ds.putCS(Tags.QueryRetrieveLevel, "PATIENT");
        }

        protected String[] getSelectAttributes() {
            return new String[] { "Patient.encodedAttributes" };
        }

        protected String[] getTables() {
            return new String[] { "Patient" };
        }

    }

    public static class StudyQueryCmd extends QueryCmd {

        protected StudyQueryCmd(Dataset keys, boolean filterResult,
                boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
            super(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
            defineColumnTypes(new int[] {
                    blobAccessType,     // Patient.encodedAttributes
                    blobAccessType,     // Study.encodedAttributes
                    Types.VARCHAR,      // Study.modalitiesInStudy
                    Types.VARCHAR,      // Study.sopClassesInStudy
                    Types.VARCHAR,      // Study.studyStatusId
                    Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                    Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                    Types.VARCHAR,      // Study.filesetId
                    Types.VARCHAR,      // Study.filesetIuid
                    Types.VARCHAR,      // Study.retrieveAETs
                    Types.VARCHAR,      // Study.externalRetrieveAET
                    Types.INTEGER       // Study.availability
                    });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addNestedSeriesMatch();
        }

        protected String[] getSelectAttributes() {
            return new String[] { 
                    "Patient.encodedAttributes",                // (1)
                    "Study.encodedAttributes",                  // (2)
                    "Study.modalitiesInStudy",                  // (3)
                    "Study.sopClassesInStudy",                  // (4)
                    "Study.studyStatusId",                      // (5)
                    "Study.numberOfStudyRelatedSeries",         // (6)
                    "Study.numberOfStudyRelatedInstances",      // (7)
                    "Study.filesetId",                          // (8)
                    "Study.filesetIuid",                        // (9)
                    "Study.retrieveAETs",                       // (10)
                    "Study.externalRetrieveAET",                // (11)
                    "Study.availability",                       // (12)
                    };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study" };
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk" };
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            fillDataset(ds, 2);
            ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(rs.getString(3),
                    '\\'));
            ds.putUI(Tags.SOPClassesInStudy, StringUtils.split(rs.getString(4),
                    '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(5));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(6));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(7));
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(8));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(9));
            DatasetUtils.putRetrieveAET(ds, rs.getString(10), rs.getString(11));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(12)]);
            ds.putCS(Tags.QueryRetrieveLevel, "STUDY");
        }

    }

    public static class SeriesQueryCmd extends QueryCmd {

        protected SeriesQueryCmd(Dataset keys, boolean filterResult,
                boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID, Subject subject)
                throws SQLException {
            super(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
            defineColumnTypes(new int[] {
                    blobAccessType,     // Patient.encodedAttributes
                    blobAccessType,     // Study.encodedAttributes
                    blobAccessType,     // Series.encodedAttributes
                    Types.VARCHAR,      // Study.modalitiesInStudy
                    Types.VARCHAR,      // Study.sopClassesInStudy
                    Types.VARCHAR,      // Study.studyStatusId
                    Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                    Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                    Types.INTEGER,      // Series.numberOfSeriesRelatedInstances
                    Types.VARCHAR,      // Series.filesetId
                    Types.VARCHAR,      // Series.filesetIuid
                    Types.VARCHAR,      // Series.retrieveAETs
                    Types.VARCHAR,      // Series.externalRetrieveAET
                    Types.INTEGER,      // Series.availability
                    });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addSeriesMatch();
        }

        protected String[] getSelectAttributes() {
            return new String[] {
                    "Patient.encodedAttributes",                // (1)
                    "Study.encodedAttributes",                  // (2)
                    "Series.encodedAttributes",                 // (3)
                    "Study.modalitiesInStudy",                  // (4)
                    "Study.sopClassesInStudy",                  // (5)
                    "Study.studyStatusId",                      // (6)
                    "Study.numberOfStudyRelatedSeries",         // (7)
                    "Study.numberOfStudyRelatedInstances",      // (8)
                    "Series.numberOfSeriesRelatedInstances",    // (9)
                    "Series.filesetId",                         // (10)
                    "Series.filesetIuid",                       // (11)
                    "Series.retrieveAETs",                      // (12)
                    "Series.externalRetrieveAET",               // (13)
                    "Series.availability",                      // (14)
                    };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study", "Series" };
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk",
                            "Study.pk", "Series.study_fk" };
        }

        protected String[] getLeftJoin() {
            return null;
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            fillDataset(ds, 1);
            fillDataset(ds, 2);
            fillDataset(ds, 3);
            ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(rs.getString(4),
                    '\\'));
            ds.putUI(Tags.SOPClassesInStudy, StringUtils.split(rs.getString(5),
                    '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(6));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(7));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(8));
            ds.putIS(Tags.NumberOfSeriesRelatedInstances, rs.getInt(9));
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(10));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(11));
            DatasetUtils.putRetrieveAET(ds, rs.getString(12), rs.getString(13));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(14)]);
            ds.putCS(Tags.QueryRetrieveLevel, "SERIES");
        }
    }

    public static class ImageQueryCmd extends QueryCmd {

        HashMap seriesAttrsCache = new HashMap();

        protected ImageQueryCmd(Dataset keys, boolean filterResult,
                boolean noMatchForNoValue,
                boolean noMatchWithoutIssuerOfPID,
                Subject subject) throws SQLException {
            super(keys, filterResult, noMatchForNoValue,
                    noMatchWithoutIssuerOfPID, subject);
            defineColumnTypes(lazyFetchSeriesAttrsOnImageLevelQuery
                    ? new int[] {
                            blobAccessType,     // Instance.encodedAttributes
                            Types.VARCHAR,      // Instance.retrieveAETs
                            Types.VARCHAR,      // Instance.externalRetrieveAET
                            Types.INTEGER,      // Instance.availability
                            Types.VARCHAR,      // Series.seriesIuid
                            Types.VARCHAR,      // Media.filesetId
                            Types.VARCHAR,      // Media.filesetIuid
                            }
                    : new int[] {
                            blobAccessType,     // Instance.encodedAttributes
                            Types.VARCHAR,      // Series.seriesIuid
                            blobAccessType,     // Patient.encodedAttributes
                            blobAccessType,     // Study.encodedAttributes
                            blobAccessType,     // Series.encodedAttributes
                            Types.VARCHAR,      // Study.modalitiesInStudy
                            Types.VARCHAR,      // Study.sopClassesInStudy
                            Types.VARCHAR,      // Study.studyStatusId
                            Types.INTEGER,      // Study.numberOfStudyRelatedSeries
                            Types.INTEGER,      // Study.numberOfStudyRelatedInstances
                            Types.INTEGER,      // Series.numberOfSeriesRelatedInstances
                            Types.VARCHAR,      // Instance.retrieveAETs
                            Types.VARCHAR,      // Instance.externalRetrieveAET
                            Types.INTEGER,      // Instance.availability
                            Types.VARCHAR,      // Media.filesetId
                            Types.VARCHAR,      // Media.filesetIuid
                            });
            addAdditionalReturnKeys();
        }

        protected void init() {
            super.init();
            addPatientMatch();
            addStudyMatch();
            addStudyPermissionMatch(false);
            addSeriesMatch();
            addInstanceMatch();
        }

        protected String[] getSelectAttributes() {
            return lazyFetchSeriesAttrsOnImageLevelQuery
                    ? new String[] {
                            "Instance.encodedAttributes",               // (1)
                            "Instance.retrieveAETs",                    // (2)
                            "Instance.externalRetrieveAET",             // (3)
                            "Instance.availability",                    // (4)
                            "Series.seriesIuid",                        // (5)
                            "Media.filesetId",                          // (6)
                            "Media.filesetIuid",                        // (7)
                            }
                    : new String[] {
                            "Instance.encodedAttributes",               // (1)
                            "Series.seriesIuid",                        // (2)
                            "Patient.encodedAttributes",                // (3)
                            "Study.encodedAttributes",                  // (4)
                            "Series.encodedAttributes",                 // (5)
                            "Study.modalitiesInStudy",                  // (6)
                            "Study.sopClassesInStudy",                  // (7)
                            "Study.studyStatusId",                      // (8)
                            "Study.numberOfStudyRelatedSeries",         // (9)
                            "Study.numberOfStudyRelatedInstances",      // (10)
                            "Series.numberOfSeriesRelatedInstances",    // (11)
                            "Instance.retrieveAETs",                    // (12)
                            "Instance.externalRetrieveAET",             // (13)
                            "Instance.availability",                    // (14)
                            "Media.filesetId",                          // (15)
                            "Media.filesetIuid",                        // (16)
                            };
        }

        protected String[] getTables() {
            return new String[] { "Patient", "Study", "Series", "Instance" };
        }

        protected String[] getLeftJoin() {
            ArrayList list = new ArrayList(12);
            if (isMatchSrCode()) {
                list.add("Code");
                list.add(SR_CODE);
                list.add("Instance.srcode_fk");
                list.add("Code.pk");
            }
            list.add("Media");
            list.add(null);
            list.add("Instance.media_fk");
            list.add("Media.pk");
            return (String[]) list.toArray(new String[list.size()]);
        }

        protected String[] getRelations() {
            return new String[] { "Patient.pk", "Study.patient_fk",
                            "Study.pk", "Series.study_fk", "Series.pk",
                            "Instance.series_fk" };
        }

        protected void fillDataset(Dataset ds) throws SQLException {
            if (lazyFetchSeriesAttrsOnImageLevelQuery) {
                fillDatasetWithLazyFetchSeriesAttrs(ds);
            } else {
                fillDatasetWithEagerFetchSeriesAttrs(ds);
            }
        }
        
        private void fillDatasetWithLazyFetchSeriesAttrs(Dataset ds)
                throws SQLException {
            fillDataset(ds, 1);
            DatasetUtils.putRetrieveAET(ds, rs.getString(2), rs.getString(3));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(4)]);
            String seriesIuid = rs.getString(5);
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(6));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(7));
            Dataset seriesAttrs = (Dataset) seriesAttrsCache.get(seriesIuid);
            if (seriesAttrs == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Lazy fetch Series attributes for Series "
                            + seriesIuid);
                }
                QuerySeriesAttrsForQueryCmd seriesQuery =
                        new QuerySeriesAttrsForQueryCmd(
                                QueryCmd.transactionIsolationLevel,
                                QueryCmd.seriesBlobAccessType,
                                seriesIuid);
                try {
                    seriesQuery.execute();
                    seriesQuery.next();
                    seriesAttrs = seriesQuery.getDataset();
                } finally {
                    seriesQuery.close();
                }
                seriesAttrsCache.put(seriesIuid, seriesAttrs);
            }
            ds.putAll(seriesAttrs);
            ds.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        }
        
        private void fillDatasetWithEagerFetchSeriesAttrs(Dataset ds)
                throws SQLException {
            fillDataset(ds, 1);
            if (cacheSeriesAttrsOnImageLevelQuery) {
                String seriesIuid = rs.getString(2);
                Dataset seriesAttrs = (Dataset) seriesAttrsCache.get(seriesIuid);
                if (seriesAttrs == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cache Series attributes for Series "
                                + seriesIuid);
                    }
                    seriesAttrs = DcmObjectFactory.getInstance().newDataset();
                    fillDataset(seriesAttrs, 3);
                    fillDataset(seriesAttrs, 4);
                    fillDataset(seriesAttrs, 5);
                    seriesAttrsCache.put(seriesIuid, seriesAttrs);
                } else if (log.isDebugEnabled()) {
                    log.debug("Use cached Series attributes for Series "
                            + seriesIuid);
                }
                ds.putAll(seriesAttrs);
            } else {
                fillDataset(ds, 3);
                fillDataset(ds, 4);
                fillDataset(ds, 5);
            }
            ds.putCS(Tags.ModalitiesInStudy,
                    StringUtils.split(rs.getString(6), '\\'));
            ds.putUI(Tags.SOPClassesInStudy,
                    StringUtils.split(rs.getString(7), '\\'));
            ds.putCS(Tags.StudyStatusID, rs.getString(8));
            ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(9));
            ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(10));
            ds.putIS(Tags.NumberOfSeriesRelatedInstances, rs.getInt(11));
            DatasetUtils.putRetrieveAET(ds, rs.getString(12), rs.getString(13));
            ds.putCS(Tags.InstanceAvailability, AVAILABILITY[rs.getInt(14)]);
            ds.putSH(Tags.StorageMediaFileSetID, rs.getString(15));
            ds.putUI(Tags.StorageMediaFileSetUID, rs.getString(16));
            ds.putCS(Tags.QueryRetrieveLevel, "IMAGE");
        }
    }
    
    protected boolean isMatchSrCode() {
        Dataset code = keys.getItem(Tags.ConceptNameCodeSeq);
        return code != null
                && (code.containsValue(Tags.CodeValue)
                        || code.containsValue(Tags.CodingSchemeDesignator));
    }

    protected boolean isMatchRequestAttributes() {
        Dataset rqAttrs = keys.getItem(Tags.RequestAttributesSeq);
        return rqAttrs != null
                && (rqAttrs.containsValue(Tags.RequestedProcedureID)
                        || rqAttrs.containsValue(Tags.SPSID)
                        || rqAttrs.containsValue(Tags.RequestingService)
                        || rqAttrs.containsValue(Tags.RequestingPhysician)
                        || rqAttrs.containsValue(Tags.StudyInstanceUID));
    }

    protected boolean isMatchVerifyingObserver() {
        Dataset item = keys.getItem(Tags.VerifyingObserverSeq);
        return item != null
                && (item.containsValue(Tags.VerificationDateTime)
                        || item.containsValue(Tags.VerifyingObserverName));
    }
}