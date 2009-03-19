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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 5804 $ $Date: 2008-02-05 09:52:48 +0100 (Tue, 05 Feb 2008) $
 * @since 26.08.2003
 */
public class RetrieveCmd extends BaseReadCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;
    public static int seriesBlobAccessType = Types.BLOB;
    public static boolean lazyFetchSeriesAttrs = false;
    public static boolean cacheSeriesAttrs = true;
    public static long seriesAttrsCacheCurrencyTimeLimit = 600000;
    private static int seriesAttrsCacheMaxSize = 100;

    static class SeriesAttrsCacheEntry {
        public final long timestamp = System.currentTimeMillis();
        public final FileInfo fileInfo;
        public SeriesAttrsCacheEntry(FileInfo fileInfo) {
            this.fileInfo = fileInfo;
        }
    }

    private static Map<String, SeriesAttrsCacheEntry> seriesAttrsCache =
        Collections.synchronizedMap(
            new LinkedHashMap<String, SeriesAttrsCacheEntry>() {

                private static final long serialVersionUID = -3342074672179596774L;

                @Override
                protected boolean removeEldestEntry(
                        Entry<String, SeriesAttrsCacheEntry> eldest) {
                    if (size() >= seriesAttrsCacheMaxSize) {
                        if (log.isDebugEnabled()) {
                            log.debug("Remove Series attributes for Series "
                                    + eldest.getValue().fileInfo.seriesIUID
                                    + " from cache");
                        }
                        return true;
                    }
                    return false;
                }
            });

    /** Number of max. parameters in IN(...) statement. */
    public static int maxElementsInUIDMatch = 100;

    private static final String[] ENTITY = { "Patient", "Study", "Series",
            "Instance" };

    private static final String[] ENTITY_NO_LEFT_JOIN = { "Patient", "Study",
            "Series", "Instance", "File", "FileSystem" };
    
    private static final String[] LEFT_JOIN = { "File", null, "Instance.pk",
            "File.instance_fk", "FileSystem", null, "File.filesystem_fk",
            "FileSystem.pk" };

    private static final String[] RELATIONS = { "Patient.pk",
            "Study.patient_fk", "Study.pk", "Series.study_fk", "Series.pk",
            "Instance.series_fk" };

    private static final String[] RELATIONS_NO_LEFT_JOIN = { "Patient.pk",
            "Study.patient_fk", "Study.pk", "Series.study_fk", "Series.pk",
            "Instance.series_fk", "Instance.pk", "File.instance_fk",
            "File.filesystem_fk", "FileSystem.pk" };

    private static final Comparator DESC_FILE_PK = new Comparator() {

        /**
         * This will make sure the most available file will be listed first
         */
        public int compare(Object o1, Object o2) {
            FileInfo fi1 = (FileInfo) o1;
            FileInfo fi2 = (FileInfo) o2;
            int diffAvail = fi1.availability - fi2.availability;
            return diffAvail != 0 ? diffAvail : fi2.pk == fi1.pk ? 0
                    : fi2.pk < fi1.pk ? -1 : 1;
        }
    };
    
    private static String[] entity = ENTITY;
    private static String[] leftJoin = LEFT_JOIN;
    private static String[] relations = RELATIONS;

    private Sql sqlCmd;
    
    public static boolean isNoLeftJoin() {
        return leftJoin == null;
    }
    
    public static int getSeriesAttrsCacheMaxSize() {
        return seriesAttrsCacheMaxSize;
    }
    
    public static void setSeriesAttrsCacheMaxSize(int maxSize) {
        int toRemove = seriesAttrsCache.size() - maxSize;
        if (toRemove > 0) {
            for (Iterator iterator = seriesAttrsCache.entrySet().iterator();
                    toRemove > 0; toRemove--) {
                iterator.next();
                iterator.remove();
            }
        }
        seriesAttrsCacheMaxSize = maxSize;
    }

    public static void setNoLeftJoin(boolean noleftJoin) {
        if (noleftJoin) {
            entity = ENTITY_NO_LEFT_JOIN;
            leftJoin = null;
            relations = RELATIONS_NO_LEFT_JOIN;
        } else {
            entity = ENTITY;
            leftJoin = LEFT_JOIN;
            relations = RELATIONS;         
        }
    }

    public static RetrieveCmd create(Dataset keys) throws SQLException {
        String qrLevel = keys.getString(Tags.QueryRetrieveLevel);
        if (qrLevel == null || qrLevel.length() == 0)
            throw new IllegalArgumentException("Missing QueryRetrieveLevel");
        if ("IMAGE".equals(qrLevel))
            return createInstanceRetrieve(keys);
        if ("SERIES".equals(qrLevel))
            return createSeriesRetrieve(keys);
        if ("STUDY".equals(qrLevel))
            return createStudyRetrieve(keys);
        if ("PATIENT".equals(qrLevel))
            return createPatientRetrieve(keys);
        throw new IllegalArgumentException("QueryRetrieveLevel=" + qrLevel);
    }

    public static RetrieveCmd createPatientRetrieve(Dataset keys)
            throws SQLException {
        return new RetrieveCmd(new PatientSql(keys, true));
    }

    public static RetrieveCmd createStudyRetrieve(Dataset keys)
            throws SQLException {
        return new RetrieveCmd(new StudySql(keys, true));
    }

    public static RetrieveCmd createSeriesRetrieve(Dataset keys)
            throws SQLException {
        return new RetrieveCmd(new SeriesSql(keys, true));
    }

    public static RetrieveCmd createInstanceRetrieve(Dataset keys)
            throws SQLException {
        return new ImageRetrieveCmd(new ImageSql(keys), keys
                .getStrings(Tags.SOPInstanceUID));
    }

    public static RetrieveCmd create(DcmElement refSOPSeq) throws SQLException {
        return new RetrieveCmd(new RefSOPSql(refSOPSeq));
    }

    protected RetrieveCmd(Sql sql) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel, sql.getSql());
        defineColumnTypes(lazyFetchSeriesAttrs
                ? new int[] {
                    blobAccessType,     // Instance.encodedAttributes
                    Types.VARCHAR,      // Series.seriesIuid
                    Types.BIGINT,       // Instance.pk
                    Types.VARCHAR,      // Instance.sopIuid
                    Types.VARCHAR,      // Instance.sopCuid
                    Types.VARCHAR,      // Instance.externalRetrieveAET
                    Types.BIGINT,       // File.pk
                    Types.VARCHAR,      // FileSystem.retrieveAET
                    Types.INTEGER,      // FileSystem.availability
                    Types.VARCHAR,      // FileSystem.directoryPath
                    Types.VARCHAR,      // File.filePath
                    Types.VARCHAR,      // File.fileTsuid
                    Types.VARCHAR,      // File.fileMd5Field
                    Types.INTEGER,      // File.fileSize
                    Types.INTEGER,      // File.fileStatus
                    }
                : new int[] {
                    blobAccessType,     // Instance.encodedAttributes
                    Types.VARCHAR,      // Series.seriesIuid
                    blobAccessType,     // Patient.encodedAttributes
                    blobAccessType,     // Study.encodedAttributes
                    blobAccessType,     // Series.encodedAttributes
                    Types.VARCHAR,      // Patient.patientId
                    Types.VARCHAR,      // Patient.patientName
                    Types.VARCHAR,      // Study.studyIuid
                    Types.BIGINT,       // Instance.pk
                    Types.VARCHAR,      // Instance.sopIuid
                    Types.VARCHAR,      // Instance.sopCuid
                    Types.VARCHAR,      // Instance.externalRetrieveAET
                    Types.BIGINT,       // File.pk
                    Types.VARCHAR,      // FileSystem.retrieveAET
                    Types.INTEGER,      // FileSystem.availability
                    Types.VARCHAR,      // FileSystem.directoryPath
                    Types.VARCHAR,      // File.filePath
                    Types.VARCHAR,      // File.fileTsuid
                    Types.VARCHAR,      // File.fileMd5Field
                    Types.INTEGER,      // File.fileSize
                    Types.INTEGER,      // File.fileStatus
                    });
        this.sqlCmd = sql;
    }

    public FileInfo[][] getFileInfos() throws SQLException {
        Map result = map();
        try {
            PreparedStatement pstmt = ((PreparedStatement) stmt);
            int start = 0;
            String[] fixParams = sqlCmd.getFixParams();
            for (int i = 0; i < fixParams.length; i++) {
                pstmt.setString(i + 1, fixParams[i]);
            }
            int firstListIdx = fixParams.length;
            String[] params = sqlCmd.getParams();
            if (params != null) {
                int len = sqlCmd.getNumberOfParams();
                while (start < params.length) {
                    if (start + len > params.length) { // we need a new
                                                        // statement for the
                                                        // remaining parameter
                                                        // values
                        len = params.length - start;
                        sqlCmd.updateUIDMatch(len);
                        pstmt = con.prepareStatement(sqlCmd.getSql(),
                                resultSetType, resultSetConcurrency);
                        if (firstListIdx > 0) { // we need to set the fix params
                                                // for the new statement!
                            for (int i = 0; i < fixParams.length; i++) {
                                pstmt.setString(i + 1, fixParams[i]);
                            }
                        }
                    }
                    for (int i = 1; i <= len; i++) {// set the values for the
                                                    // uid list match
                        pstmt.setString(firstListIdx + i, params[start++]);
                    }
                    rs = pstmt.executeQuery();
                    addFileInfos(result);
                }
            } else {
                rs = pstmt.executeQuery();
                addFileInfos(result);
            }
        } finally {
            close();
        }
        return toArray(result);
    }

    private void addFileInfos(Map result) throws SQLException {
        while (next()) {
            if (lazyFetchSeriesAttrs) {
                addFileInfoWithLazyFetchSeriesAttrs(result);
            } else {
                addFileInfoWithEagerFetchSeriesAttrs(result);
            }
        }
    }

    private void addFileInfoWithLazyFetchSeriesAttrs(Map result)
            throws SQLException {
        FileInfo info = new FileInfo();
        info.instAttrs = rs.getBytes(1);
        info.seriesIUID = rs.getString(2);
        long instPk = rs.getLong(3);
        info.sopIUID = rs.getString(4);
        info.sopCUID = rs.getString(5);
        info.extRetrieveAET = rs.getString(6);
        info.pk = rs.getLong(7);
        info.fileRetrieveAET = rs.getString(8);
        info.availability = rs.getInt(9);
        info.basedir = rs.getString(10);
        info.fileID = rs.getString(11);
        info.tsUID = rs.getString(12);
        info.md5 = rs.getString(13);
        info.size = rs.getInt(14);
        info.status = rs.getInt(15);
        FileInfo seriesFileInfo = getCachedSeriesAttrs(info.seriesIUID);
        if (seriesFileInfo == null) {
            if (log.isDebugEnabled()) {
                log.debug("Lazy fetch Series attributes for Series "
                        + info.seriesIUID);
            }
            QuerySeriesAttrsForRetrieveCmd seriesQuery =
                new QuerySeriesAttrsForRetrieveCmd(
                        QueryCmd.transactionIsolationLevel,
                        QueryCmd.seriesBlobAccessType,
                        info.seriesIUID);
            try {
                seriesQuery.execute();
                seriesQuery.next();
                seriesFileInfo = seriesQuery.getFileInfo();
            } finally {
                seriesQuery.close();
            }
            seriesAttrsCache.put(info.seriesIUID,
                    new SeriesAttrsCacheEntry(seriesFileInfo));
        }
        info.patAttrs = seriesFileInfo.patAttrs;
        info.studyAttrs = seriesFileInfo.studyAttrs;
        info.seriesAttrs = seriesFileInfo.seriesAttrs;
        info.patID = seriesFileInfo.patID;
        info.patName = seriesFileInfo.patName;
        info.studyIUID = seriesFileInfo.studyIUID;
        addFileInfo(result, instPk, info.sopIUID, info);
    }

    private static FileInfo getCachedSeriesAttrs(String seriesIUID) {
        SeriesAttrsCacheEntry cacheEntry = seriesAttrsCache.get(seriesIUID);
        if (cacheEntry == null) {
            return null;
        }
        if (cacheEntry.timestamp + seriesAttrsCacheCurrencyTimeLimit 
                < System.currentTimeMillis()) {
            if (log.isDebugEnabled()) {
                log.debug("Remove stale Series attributes for Series "
                        + seriesIUID + " from cache");
            }
            seriesAttrsCache.remove(seriesIUID);
            return null;
        }
        return cacheEntry.fileInfo;
    }

    private void addFileInfoWithEagerFetchSeriesAttrs(Map result)
            throws SQLException {
        FileInfo info = new FileInfo();
        info.instAttrs = rs.getBytes(1);
        info.seriesIUID = rs.getString(2);
        if (cacheSeriesAttrs) {
            FileInfo seriesFileInfo = getCachedSeriesAttrs(info.seriesIUID);
            if (seriesFileInfo == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache Series attributes for Series "
                            + info.seriesIUID);
                }
                info.patAttrs = rs.getBytes(3);
                info.studyAttrs = rs.getBytes(4);
                info.seriesAttrs = rs.getBytes(5);
                info.patID = rs.getString(6);
                info.patName = rs.getString(7);
                info.studyIUID = rs.getString(8);
                seriesAttrsCache.put(info.seriesIUID,
                        new SeriesAttrsCacheEntry(info));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Use cached Series attributes for Series "
                            + info.seriesIUID);
                }
                info.patAttrs = seriesFileInfo.patAttrs;
                info.studyAttrs = seriesFileInfo.studyAttrs;
                info.seriesAttrs = seriesFileInfo.seriesAttrs;
                info.patID = seriesFileInfo.patID;
                info.patName = seriesFileInfo.patName;
                info.studyIUID = seriesFileInfo.studyIUID;
            }
        } else {
            info.patAttrs = rs.getBytes(3);
            info.studyAttrs = rs.getBytes(4);
            info.seriesAttrs = rs.getBytes(5);
            info.patID = rs.getString(6);
            info.patName = rs.getString(7);
            info.studyIUID = rs.getString(8);
        }
        long instPk = rs.getLong(9);
        info.sopIUID = rs.getString(10);
        info.sopCUID = rs.getString(11);
        info.extRetrieveAET = rs.getString(12);
        info.pk = rs.getLong(13);
        info.fileRetrieveAET = rs.getString(14);
        info.availability = rs.getInt(15);
        info.basedir = rs.getString(16);
        info.fileID = rs.getString(17);
        info.tsUID = rs.getString(18);
        info.md5 = rs.getString(19);
        info.size = rs.getInt(20);
        info.status = rs.getInt(21);
        addFileInfo(result, instPk, info.sopIUID, info);
    }

    private void addFileInfo(Map result, long instPk, String sopIUID,
            FileInfo info) {
        Object key = selectKey(instPk, sopIUID);
        ArrayList list = (ArrayList) result.get(key);
        if (list == null) {
            result.put(key, list = new ArrayList());
        }
        list.add(info);
    }

    protected Map map() {
        return new TreeMap();
    }

    protected Object selectKey(long instPk, String sopIUID) {
        return new Long(instPk);
    }

    protected FileInfo[][] toArray(Map result) {
        FileInfo[][] array = new FileInfo[result.size()][];
        ArrayList list;
        Iterator it = result.values().iterator();
        for (int i = 0; i < array.length; i++) {
            list = (ArrayList) it.next();
            array[i] = (FileInfo[]) list.toArray(new FileInfo[list.size()]);
            Arrays.sort(array[i], DESC_FILE_PK);
        }
        return array;
    }

    static class ImageRetrieveCmd extends RetrieveCmd {
        final String[] uids;

        ImageRetrieveCmd(Sql sql, String[] uids) throws SQLException {
            super(sql);
            this.uids = uids;
        }

        protected Map map() {
            return new HashMap();
        }

        protected Object selectKey(long instPk, String sopIUID) {
            return sopIUID;
        }
    }

    private static class Sql {
        protected String[] params = null;

        int numberOfParams;

        Match.AppendLiteral uidMatch = null;

        final SqlBuilder sqlBuilder = new SqlBuilder();

        ArrayList fixValues = new ArrayList();

        Sql() {
            sqlBuilder.setSelect(getSelectAttributes());
            sqlBuilder.setFrom(RetrieveCmd.entity);
            sqlBuilder.setLeftJoin(RetrieveCmd.leftJoin);
            sqlBuilder.setRelations(RetrieveCmd.relations);
        }

        private String[] getSelectAttributes() {
            return lazyFetchSeriesAttrs 
                ? new String[] {
                        "Instance.encodedAttributes",   // (1)
                        "Series.seriesIuid",            // (2)
                        "Instance.pk",                  // (3)
                        "Instance.sopIuid",             // (4)
                        "Instance.sopCuid",             // (5)
                        "Instance.externalRetrieveAET", // (6)
                        "File.pk",                      // (7)
                        "FileSystem.retrieveAET",       // (8)
                        "FileSystem.availability",      // (9)
                        "FileSystem.directoryPath",     // (10)
                        "File.filePath",                // (11)
                        "File.fileTsuid",               // (12)
                        "File.fileMd5Field",            // (13)
                        "File.fileSize",                // (14)
                        "File.fileStatus"               // (15)
                        }
                : new String[] { 
                        "Instance.encodedAttributes",   // (1)
                        "Series.seriesIuid",            // (2)
                        "Patient.encodedAttributes",    // (3)
                        "Study.encodedAttributes",      // (4)
                        "Series.encodedAttributes",     // (5)
                        "Patient.patientId",            // (6)
                        "Patient.patientName",          // (7)
                        "Study.studyIuid",              // (8)
                        "Instance.pk",                  // (9)
                        "Instance.sopIuid",             // (10)
                        "Instance.sopCuid",             // (11)
                        "Instance.externalRetrieveAET", // (12)
                        "File.pk",                      // (13)
                        "FileSystem.retrieveAET",       // (14)
                        "FileSystem.availability",      // (15)
                        "FileSystem.directoryPath",     // (16)
                        "File.filePath",                // (17)
                        "File.fileTsuid",               // (18)
                        "File.fileMd5Field",            // (19)
                        "File.fileSize",                // (20)
                        "File.fileStatus"               // (21)
                        };
        }

        public final String getSql() {
            return sqlBuilder.getSql();
        }

        public String[] getFixParams() {
            return (String[]) fixValues.toArray(new String[fixValues.size()]);
        }

        /** return all parameter values of the uid list match */
        public String[] getParams() {
            return params;
        }

        /**
         * returns number of list params in SQL statement (no of ? in uid list
         * match)
         */
        public int getNumberOfParams() {
            return numberOfParams;
        }

        public boolean updateUIDMatch(int len) {
            if (uidMatch == null)
                return false;
            uidMatch.setLiteral(getUIDMatchLiteral(len));
            return true;
        }

        protected void addUidMatch(String column, String[] uid) {
            if (uid.length <= maxElementsInUIDMatch) {
                sqlBuilder.addLiteralMatch(null, column, SqlBuilder.TYPE1,
                        getUIDMatchLiteral(uid.length));
                for (int i = 0; i < uid.length; i++) {
                    fixValues.add(uid[i]);
                }
            } else {
                if (params != null)
                    throw new IllegalArgumentException(
                            "Only one UID list > maxElementsInUIDMatch ("
                                    + maxElementsInUIDMatch
                                    + ") is allowed in RetrieveCmd!");
                params = uid;
                numberOfParams = uid.length < maxElementsInUIDMatch ? uid.length
                        : maxElementsInUIDMatch;
                uidMatch = (Match.AppendLiteral) sqlBuilder.addLiteralMatch(
                        null, column, SqlBuilder.TYPE1,
                        getUIDMatchLiteral(numberOfParams));
            }
        }

        /**
         * @param uid
         * @return
         */
        private String getUIDMatchLiteral(int len) {
            if (len == 1)
                return "=?";
            StringBuffer sb = new StringBuffer();
            sb.append(" IN (?");
            for (int i = 1; i < len; i++) {
                sb.append(", ?");
            }
            sb.append(")");
            return sb.toString();
        }
    }

    private static class PatientSql extends Sql {
        PatientSql(Dataset keys, boolean patientRetrieve) {
            String pid = keys.getString(Tags.PatientID);
            if (pid != null && !"*".equals(pid)) {
                sqlBuilder.addLiteralMatch(null, "Patient.patientId",
                        SqlBuilder.TYPE2, "=?");
                fixValues.add(pid);
            } else if (patientRetrieve)
                throw new IllegalArgumentException("Missing PatientID");
        }
    }

    private static class StudySql extends PatientSql {
        StudySql(Dataset keys, boolean studyRetrieve) {
            super(keys, false);
            String[] uid = keys.getStrings(Tags.StudyInstanceUID);
            if (uid != null && uid.length != 0 && !"*".equals(uid[0])) {
                addUidMatch("Study.studyIuid", uid);
            } else if (studyRetrieve)
                throw new IllegalArgumentException("Missing StudyInstanceUID");
        }
    }

    private static class SeriesSql extends StudySql {
        SeriesSql(Dataset keys, boolean seriesRetrieve) {
            super(keys, false);
            String[] uid = keys.getStrings(Tags.SeriesInstanceUID);
            if (uid != null && uid.length != 0 && !"*".equals(uid[0])) {
                addUidMatch("Series.seriesIuid", uid);
            } else if (seriesRetrieve)
                throw new IllegalArgumentException("Missing SeriesInstanceUID");
        }

    }

    private static class ImageSql extends SeriesSql {
        ImageSql(Dataset keys) {
            super(keys, false);
            String[] uid = keys.getStrings(Tags.SOPInstanceUID);
            if (uid != null && uid.length != 0 && !"*".equals(uid[0])) {
                addUidMatch("Instance.sopIuid", uid);
            } else
                throw new IllegalArgumentException("Missing SOPInstanceUID");
        }
    }

    private static class RefSOPSql extends Sql {
        RefSOPSql(DcmElement refSOPSeq) {
            String[] uid = new String[refSOPSeq.countItems()];
            for (int i = 0; i < uid.length; i++) {
                uid[i] = refSOPSeq.getItem(i).getString(Tags.RefSOPInstanceUID);
            }
            addUidMatch("Instance.sopIuid", uid);
        }
    }

}
