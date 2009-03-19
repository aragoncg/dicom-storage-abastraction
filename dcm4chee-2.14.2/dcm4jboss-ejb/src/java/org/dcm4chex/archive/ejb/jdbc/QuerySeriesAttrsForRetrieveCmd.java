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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Jan 18, 2008
 */
class QuerySeriesAttrsForRetrieveCmd extends BaseReadCmd {
    private final String seriesIUID;
    protected QuerySeriesAttrsForRetrieveCmd(int transactionIsolationLevel,
            int blobAccessType, String seriesIUID) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(), 
                transactionIsolationLevel,
                JdbcProperties.getInstance()
                        .getProperty("QuerySeriesAttrsForRetrieveCmd"));
        this.seriesIUID = seriesIUID;
        defineColumnTypes(new int[] {
                blobAccessType, // patient.pat_attrs
                blobAccessType, // study.study_attrs
                blobAccessType, // series.series_attrs
                Types.VARCHAR,  // patient.pat_id
                Types.VARCHAR,  // patient.pat_name
                Types.VARCHAR,  // study.study_iuid
                });
        ((PreparedStatement) stmt).setString(1, seriesIUID);
    }

    public FileInfo getFileInfo() throws SQLException {
        FileInfo info = new FileInfo();
        info.seriesIUID = seriesIUID;
        info.patAttrs = rs.getBytes(1);
        info.studyAttrs = rs.getBytes(2);
        info.seriesAttrs = rs.getBytes(3);
        info.patID = rs.getString(4);
        info.patName = rs.getString(5);
        info.studyIUID = rs.getString(6);
        return info;
    }
}
