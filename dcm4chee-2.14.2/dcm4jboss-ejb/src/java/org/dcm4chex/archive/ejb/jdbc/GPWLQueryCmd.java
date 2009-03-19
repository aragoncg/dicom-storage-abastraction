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

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.GPSPSPriority;
import org.dcm4chex.archive.common.GPSPSStatus;
import org.dcm4chex.archive.common.InputAvailabilityFlag;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-09-23 13:13:23 +0200 (Tue, 23 Sep 2008) $
 * @since 02.04.2005
 */

public class GPWLQueryCmd extends BaseDSQueryCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;

    private static final String[] FROM = { "Patient", "GPSPS"};

    private static final String[] SELECT = { "Patient.encodedAttributes",
            "GPSPS.encodedAttributes"};

    private static final String[] RELATIONS = { "Patient.pk",
            "GPSPS.patient_fk"};
    
    private static final String ITEM_CODE = "item_code";
    private static final String APP_CODE = "app_code";
    private static final String DEVNAME_CODE = "devname_code";
    private static final String DEVCLASS_CODE = "devclass_code";
    private static final String DEVLOC_CODE = "devloc_code";
    private static final String PERF_CODE = "perf_code";
    
    public GPWLQueryCmd(Dataset keys) throws SQLException {
        super(keys, true, false, transactionIsolationLevel);
        defineColumnTypes(new int[] { blobAccessType, blobAccessType });
        String s;
        // ensure keys contains (8,0005) for use as result filter
        if (!keys.contains(Tags.SpecificCharacterSet)) {
            keys.putCS(Tags.SpecificCharacterSet);
        }
        sqlBuilder.setSelect(SELECT);
        sqlBuilder.setFrom(FROM);
        sqlBuilder.setRelations(RELATIONS);
        sqlBuilder.setLeftJoin(getLeftJoin());
        sqlBuilder.addListOfUidMatch(null, "GPSPS.sopIuid",
                SqlBuilder.TYPE1,
                keys.getStrings(Tags.SOPInstanceUID));
        if ((s = keys.getString(Tags.GPSPSStatus)) != null) {
            sqlBuilder.addIntValueMatch(null, "GPSPS.gpspsStatusAsInt",
                    SqlBuilder.TYPE1,
                    GPSPSStatus.toInt(s));
        }
        if ((s = keys.getString(Tags.InputAvailabilityFlag)) != null) {
            sqlBuilder.addIntValueMatch(null, "GPSPS.inputAvailabilityFlagAsInt",
                    SqlBuilder.TYPE1,
                    InputAvailabilityFlag.toInt(s));
        }
        s = keys.getString(Tags.GPSPSPriority);
        if (s != null) {
            sqlBuilder.addIntValueMatch(null, "GPSPS.gpspsPriorityAsInt",
                    SqlBuilder.TYPE1,
                    GPSPSPriority.toInt(s));
        }
        sqlBuilder.addRangeMatch(null, "GPSPS.spsStartDateTime",
                SqlBuilder.TYPE1,
                keys.getDateRange(Tags.SPSStartDateAndTime));
        sqlBuilder.addRangeMatch(null, "GPSPS.expectedCompletionDateTime",
                SqlBuilder.TYPE2,
                keys.getDateRange(Tags.ExpectedCompletionDateAndTime));        
        addCodeMatch(Tags.ScheduledWorkitemCodeSeq, ITEM_CODE);
        addCodeMatch(Tags.ScheduledProcessingApplicationsCodeSeq, APP_CODE);
        addCodeMatch(Tags.ScheduledStationNameCodeSeq, DEVNAME_CODE);
        addCodeMatch(Tags.ScheduledStationClassCodeSeq, DEVCLASS_CODE);
        addCodeMatch(Tags.ScheduledStationGeographicLocationCodeSeq, DEVLOC_CODE);
        Dataset item = keys.getItem(Tags.ScheduledHumanPerformersSeq);
        if (item != null) {
            sqlBuilder.addPNMatch(new String[] {
                    "GPSPSPerformer.humanPerformerName",
                    "GPSPSPerformer.humanPerformerIdeographicName",
                    "GPSPSPerformer.humanPerformerPhoneticName"},
                    SqlBuilder.TYPE2,
                    item.getString(Tags.HumanPerformerName));
            addCodeMatch(item.getItem(Tags.HumanPerformerCodeSeq), PERF_CODE);
        }
        item = keys.getItem(Tags.RefRequestSeq);
        if (item != null) {
            sqlBuilder.addListOfStringMatch(null,
                    "GPSPSRequest.requestedProcedureId",
                    SqlBuilder.TYPE2,
                    item.getStrings(Tags.RequestedProcedureID));
            sqlBuilder.addListOfStringMatch(null,
                    "GPSPSRequest.accessionNumber",
                    SqlBuilder.TYPE2,
                    item.getStrings(Tags.AccessionNumber));
        }        
        sqlBuilder.addListOfStringMatch(null, "Patient.patientId",
                SqlBuilder.TYPE1,
                keys.getStrings(Tags.PatientID));
        sqlBuilder.addPNMatch(new String[] {
                "Patient.patientName",
                "Patient.patientIdeographicName",
                "Patient.patientPhoneticName"},
                SqlBuilder.TYPE2,
                keys.getString(Tags.PatientName));
    }

    private void addCodeMatch(int tag, String alias) {
        addCodeMatch(keys.getItem(tag), alias);
    }

    private void addCodeMatch(Dataset item, String alias) {
        if (item != null) {
            sqlBuilder.addSingleValueMatch(alias, "Code.codeValue",
                    SqlBuilder.TYPE2,
                    item.getString(Tags.CodeValue));
            sqlBuilder.addSingleValueMatch(alias, "Code.codingSchemeDesignator",
                    SqlBuilder.TYPE2,
                    item.getString(Tags.CodingSchemeDesignator));
        }
    }

    private boolean isMatchCode(int tag) {
        return isMatchCode(keys.getItem(tag));
    }

    private boolean isMatchCode(Dataset code) {
        return code != null
                && (code.containsValue(Tags.CodeValue) 
                        || code.containsValue(Tags.CodingSchemeDesignator));
    }

    private boolean isMatchRefRequest() {
        Dataset refrq = keys.getItem(Tags.RefRequestSeq);
        return refrq != null
                && (refrq.containsValue(Tags.RequestedProcedureID)
                        || refrq.containsValue(Tags.AccessionNumber));
    }
    
    private String[] getLeftJoin() {
        ArrayList list = new ArrayList();
        if (isMatchCode(Tags.ScheduledWorkitemCodeSeq)) {
            list.add("Code");
            list.add(ITEM_CODE);
            list.add("GPSPS.code_fk");
            list.add("Code.pk");
        }
        if (isMatchCode(Tags.ScheduledProcessingApplicationsCodeSeq)) {
            sqlBuilder.setDistinct(true);
            list.add("rel_gpsps_appcode");
            list.add(null);
            list.add("GPSPS.pk");
            list.add("rel_gpsps_appcode.gpsps_fk");
            list.add("Code");
            list.add(APP_CODE);
            list.add("rel_gpsps_appcode.appcode_fk");
            list.add("Code.pk");
        }
        if (isMatchCode(Tags.ScheduledStationNameCodeSeq)) {
            sqlBuilder.setDistinct(true);
            list.add("rel_gpsps_devname");
            list.add(null);
            list.add("GPSPS.pk");
            list.add("rel_gpsps_devname.gpsps_fk");
            list.add("Code");
            list.add(DEVNAME_CODE);
            list.add("rel_gpsps_devname.devname_fk");
            list.add("Code.pk");
        }
        if (isMatchCode(Tags.ScheduledStationClassCodeSeq)) {
            sqlBuilder.setDistinct(true);
            list.add("rel_gpsps_devclass");
            list.add(null);
            list.add("GPSPS.pk");
            list.add("rel_gpsps_devclass.gpsps_fk");
            list.add("Code");
            list.add(DEVCLASS_CODE);
            list.add("rel_gpsps_devclass.devclass_fk");
            list.add("Code.pk");
        }
        if (isMatchCode(Tags.ScheduledStationGeographicLocationCodeSeq)) {
            sqlBuilder.setDistinct(true);
            list.add("rel_gpsps_devloc");
            list.add(null);
            list.add("GPSPS.pk");
            list.add("rel_gpsps_devloc.gpsps_fk");
            list.add("Code");
            list.add(DEVLOC_CODE);
            list.add("rel_gpsps_devloc.devloc_fk");
            list.add("Code.pk");
        }
        Dataset item = keys.getItem(Tags.ScheduledHumanPerformersSeq);
        if (item != null) {
            boolean matchCode = isMatchCode(item.getItem(Tags.HumanPerformerCodeSeq));
            if (matchCode || item.containsValue(Tags.HumanPerformerName)) {
                sqlBuilder.setDistinct(true);
                list.add("GPSPSPerformer");
                list.add(null);
                list.add("GPSPS.pk");
                list.add("GPSPSPerformer.gpsps_fk");
                if (matchCode) {
                    list.add("Code");
                    list.add(PERF_CODE);
                    list.add("GPSPSPerformer.code_fk");
                    list.add("Code.pk");
                }
            }
        }
        if (isMatchRefRequest()) {
            sqlBuilder.setDistinct(true);
            list.add("GPSPSRequest");
            list.add(null);
            list.add("GPSPS.pk");
            list.add("GPSPSRequest.gpsps_fk");
        }
        return (String[]) (list.isEmpty() ? null
                : list.toArray(new String[list.size()]));
    }

    public void execute() throws SQLException {
        execute(sqlBuilder.getSql());
    }

    public Dataset getDataset() throws SQLException {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        DatasetUtils.fromByteArray( rs.getBytes(1), ds);
        DatasetUtils.fromByteArray( rs.getBytes(2), ds);
        adjustDataset(ds, keys);
        return ds.subSet(keys);
    }
}
