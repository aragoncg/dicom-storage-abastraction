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
import java.util.List;

import javax.security.auth.Subject;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.common.SecurityUtils;
import org.dcm4chex.archive.ejb.interfaces.StudyPermissionDTO;
import org.dcm4chex.archive.ejb.jdbc.Match.Node;
import org.dcm4chex.archive.util.Convert;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 8207 $ $Date: 2008-11-19 12:25:05 +0100 (Wed, 19 Nov 2008) $
 * @since 14.01.2004
 */
public class QueryStudiesCmd extends BaseReadCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private static final String[] SELECT_ATTRIBUTE = {
        "Patient.encodedAttributes",                // (1)
        "Study.encodedAttributes",                  // (2)
        "Patient.pk",                               // (3)
        "Study.pk",                                 // (4)
        "Study.modalitiesInStudy",                  // (5)
        "Study.numberOfStudyRelatedSeries",         // (6)
        "Study.numberOfStudyRelatedInstances",      // (7)
        "Study.retrieveAETs",                       // (8)
        "Study.availability",                       // (9)
        "Study.filesetId",                          // (10)
        "Study.studyStatusId",                      // (11)
    };

    private static final String[] LEFT_JOIN = { 
        "Study", null, "Patient.pk", "Study.patient_fk",};
    private static final String[] LEFT_JOIN_WITH_SERIES = { 
        "Study", null, "Patient.pk", "Study.patient_fk", 
        "Series", null, "Study.pk", "Series.study_fk"};

    private boolean hideMissingStudies;

    private final SqlBuilder sqlBuilder = new SqlBuilder();

    private boolean checkPermissions = true;

    /**
     * Creates a new QueryStudiesCmd object with given filter.
     * <p>
     * If parameter <code>noMatchForNoValue=true</code> all Type2 Matches are forced to Type1 matches and therefore all
     * 'empty field' matches will be hidden.
     * <p>
     * Dont use this feature for DICOM queries!
     *   
     * @param filter                    Filter Dataset.
     * @param hideMissingStudies        Hide patients without studies.
     * @param noMatchForNoValue         disable type2 matches.Hide results where object does not have a value for a query attribute (NOT DICOM conform)
     * @param queryHasIssuerOfPID          Enable query if issuer of patient is set. Only effective if IssuerOfPatientID isn't a query attribute!
     *                                  <dl><dd><code>null</code> disabled (default: contains</dd> 
     *                                  <dd>TRUE...only objects of patients with issuer</dd>
     *                                  <dd>FALSE..only objects of patients without issuer</dd></dl>
     * @param subject                   Security subject to apply study permissions for current user. If <code>null</code> studyPermission check is disabled!
     * 
     * @throws SQLException
     */
    public QueryStudiesCmd(Dataset filter, boolean hideMissingStudies, boolean noMatchForNoValue, Boolean queryHasIssuerOfPID, Subject subject)
    throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel);
        checkPermissions = subject != null;
        boolean type2 = noMatchForNoValue ? SqlBuilder.TYPE1 : SqlBuilder.TYPE2;
        sqlBuilder.setFrom(getTables());
        sqlBuilder.setLeftJoin( getLeftJoin(filter.containsValue(Tags.SeriesInstanceUID)));
        sqlBuilder.setRelations(getRelations());
        sqlBuilder.addLiteralMatch(null, "Patient.merge_fk", false, "IS NULL");
        sqlBuilder.addWildCardMatch(null, "Patient.patientId",
                type2,
                filter.getStrings(Tags.PatientID));
        String issuer = filter.getString(Tags.IssuerOfPatientID);
        if ( issuer != null ) {
            sqlBuilder.addSingleValueMatch(null, "Patient.issuerOfPatientId", type2, issuer);
        } else if ( queryHasIssuerOfPID != null ) {
            sqlBuilder.addNULLValueMatch(null,"Patient.issuerOfPatientId", queryHasIssuerOfPID.booleanValue() );
        }
        sqlBuilder.addPNMatch(new String[] {
                "Patient.patientName",
                "Patient.patientIdeographicName",
        "Patient.patientPhoneticName"},
        type2,
        filter.getString(Tags.PatientName));
        sqlBuilder.addWildCardMatch(null, "Study.studyId", type2,
                filter.getStrings(Tags.StudyID));
        sqlBuilder.addListOfStringMatch(null, "Study.studyIuid",
                SqlBuilder.TYPE1, filter.getStrings( Tags.StudyInstanceUID));
        sqlBuilder.addListOfStringMatch(null, "Series.seriesIuid",
                SqlBuilder.TYPE1, filter.getStrings( Tags.SeriesInstanceUID));
        sqlBuilder.addRangeMatch(null, "Study.studyDateTime", type2,
                filter.getDateTimeRange(Tags.StudyDate, Tags.StudyTime));
        sqlBuilder.addWildCardMatch(null, "Study.accessionNumber", type2,
                filter.getStrings(Tags.AccessionNumber));
        sqlBuilder.addModalitiesInStudyNestedMatch(null,
                filter.getStrings(Tags.ModalitiesInStudy));
        filter.setPrivateCreatorID(PrivateTags.CreatorID);
        sqlBuilder.addCallingAETsNestedMatch(false,
                filter.getStrings(PrivateTags.CallingAET));
        this.hideMissingStudies = hideMissingStudies;	
        if ( this.hideMissingStudies ) {
            sqlBuilder.addNULLValueMatch(null,"Study.encodedAttributes", true);
        }
        if ( checkPermissions ) {
            String[] roles = SecurityUtils.rolesOf(subject);
            if ( roles.length < 1 ) {
                throw new IllegalArgumentException("User is not in a StudyPermission relevant role");
            }
            addStudyPermissionMatch(subject);
        }
    }
    
    private void addStudyPermissionMatch(Subject subject) {
        if (subject != null) {
            sqlBuilder.addQueryPermissionNestedMatch(false,
                    SecurityUtils.rolesOf(subject));
        }
    }
    

    protected String[] getTables() {
        return new String[] { "Patient" };
    }

    protected String[] getLeftJoin(boolean withSeries) {
        return withSeries ? QueryStudiesCmd.LEFT_JOIN_WITH_SERIES : QueryStudiesCmd.LEFT_JOIN;
    }

    protected String[] getRelations() {
        return null;
    }


    public int count() throws SQLException {
        try {
            sqlBuilder.setSelectCount(new String[]{"Study.pk"}, true);
            execute( sqlBuilder.getSql() );
            next();
            if (hideMissingStudies) return rs.getInt(1);
            //we have to add number of studies and number of patients without studies.
            int studies = rs.getInt(1);
            rs.close();
            rs = null;
            sqlBuilder.setSelectCount(new String[]{"Patient.pk"}, true);
            sqlBuilder.addNULLValueMatch(null,"Study.pk", false);
            execute( sqlBuilder.getSql() );
            next();
            int emptyPatients = rs.getInt(1);
            List matches = sqlBuilder.getMatches();
            matches.remove( matches.size() - 1);//removes the Study.pk NULLValue match!
            return studies + emptyPatients;
        } finally {
            close();
        }
    }


    public List list(int offset, int limit) throws SQLException {
        defineColumnTypes(new int[] { 
                blobAccessType, // Patient.encodedAttributes
                blobAccessType, // Study.encodedAttributes
                Types.BIGINT,   // Patient.pk
                Types.BIGINT,   // Study.pk
                Types.VARCHAR,  // Study.modalitiesInStudy
                Types.INTEGER,  // Study.numberOfStudyRelatedSeries
                Types.INTEGER,  // Study.numberOfStudyRelatedInstances
                Types.VARCHAR,  // Study.retrieveAETs
                Types.INTEGER,  // Study.availability
                Types.VARCHAR,  // Study.filesetId
                Types.VARCHAR,  // Study.studyStatusId
        });
        sqlBuilder.setSelect(SELECT_ATTRIBUTE);
        sqlBuilder.addOrderBy("Patient.patientName", SqlBuilder.ASC);
        sqlBuilder.addOrderBy("Patient.pk", SqlBuilder.ASC);
        sqlBuilder.addOrderBy("Study.studyDateTime", SqlBuilder.ASC);
        sqlBuilder.setOffset(offset);
        sqlBuilder.setLimit(limit);
        try {
            execute(sqlBuilder.getSql());
            ArrayList result = new ArrayList();

            while (next()) {
                final byte[] patAttrs = rs.getBytes(1);
                final byte[] styAttrs = rs.getBytes(2);
                Dataset ds = dof.newDataset();
                ds.setPrivateCreatorID(PrivateTags.CreatorID);
                ds.putOB(PrivateTags.PatientPk, Convert.toBytes(rs.getLong(3)) );
                long studyPk = rs.getLong(4);
                DatasetUtils.fromByteArray(patAttrs, ds);
                if (styAttrs != null) {
                    ds.putOB(PrivateTags.StudyPk, Convert.toBytes(studyPk) );
                    DatasetUtils.fromByteArray(styAttrs, ds);
                    ds.putCS(Tags.ModalitiesInStudy, StringUtils.split(rs
                            .getString(5), '\\'));
                    ds.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(6));
                    ds.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(7));
                    ds.putAE(Tags.RetrieveAET, StringUtils.split(rs
                            .getString(8), '\\'));
                    ds.putCS(Tags.InstanceAvailability, Availability
                            .toString(rs.getInt(9)));
                    ds.putSH(Tags.StorageMediaFileSetID, rs.getString(10));
                    ds.putCS(Tags.StudyStatusID, rs.getString(11) );
                }
                result.add(ds);
            }
            return result;
        } finally {
            close();
        }
    }
}