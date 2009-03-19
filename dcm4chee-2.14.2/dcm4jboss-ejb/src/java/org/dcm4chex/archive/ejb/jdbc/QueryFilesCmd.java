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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.MD5;
import java.sql.PreparedStatement;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6920 $ $Date: 2008-09-01 10:12:52 +0200 (Mon, 01 Sep 2008) $
 * @since 06.12.2004
 */
public final class QueryFilesCmd extends BaseReadCmd {

    private static final String QueryFilesByUIDCmd = "SELECT f.pk, f.filepath, f.file_md5, f.file_status, "
            + "fs.fs_group_id, fs.dirpath, fs.retrieve_aet, fs.availability, fs.user_info, i.sop_cuid "
            + "FROM files f, filesystem fs, instance i "
            + "WHERE f.filesystem_fk = fs.pk AND f.instance_fk = i.pk AND i.sop_iuid=?";

    private static final String QueryLinkedFilesCmd = "SELECT f.pk, f.filepath, f.file_md5, f.file_status, "
            + "fs.fs_group_id, fs.dirpath, fs.retrieve_aet, fs.availability, fs.user_info, i.sop_cuid "
            + "FROM files f, filesystem fs, instance i, files f2 "
            + "WHERE f.filesystem_fk = fs.pk AND f.instance_fk = i.pk AND f.instance_fk = f2.instance_fk and f2.pk=?";

    private static final Comparator DESC_FILE_PK = new Comparator() {

        public int compare(Object o1, Object o2) {
            FileDTO fi1 = (FileDTO) o1;
            FileDTO fi2 = (FileDTO) o2;
            int diffAvail = fi1.getAvailability() - fi2.getAvailability();
            return diffAvail != 0 ? diffAvail : (int) (fi2.getPk() - fi1
                    .getPk());
        }
    };

    public static int transactionIsolationLevel = 0;

    public QueryFilesCmd(String iuid) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel, QueryFilesByUIDCmd);
        ((PreparedStatement) stmt).setString(1, iuid);
        execute();
    }

    public QueryFilesCmd(long filePk) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel, QueryLinkedFilesCmd);
        ((PreparedStatement) stmt).setLong(1, filePk);
        execute();
    }

    private FileDTO getFileDTO() throws SQLException {
        FileDTO dto = new FileDTO();
        dto.setPk(rs.getLong(1));
        dto.setFilePath(rs.getString(2));
        dto.setFileMd5(MD5.toBytes(rs.getString(3)));
        dto.setFileStatus(rs.getInt(4));
        dto.setFileSystemGroupID(rs.getString(5));
        dto.setDirectoryPath(rs.getString(6));
        dto.setRetrieveAET(rs.getString(7));
        dto.setAvailability(rs.getInt(8));
        dto.setUserInfo(rs.getString(9));
        dto.setSopClassUID(rs.getString(10));
        return dto;
    }

    public List getFileDTOs() throws SQLException {
        List list = new ArrayList();
        try {
            while (next())
                list.add(getFileDTO());
        } finally {
            close();
        }
        Collections.sort(list, DESC_FILE_PK);
        return list;
    }
}
