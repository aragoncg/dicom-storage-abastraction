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

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.DatasetUtils;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Jan 18, 2008
 */
class QuerySeriesAttrsForQueryCmd extends BaseReadCmd {

    protected QuerySeriesAttrsForQueryCmd(int transactionIsolationLevel,
                int blobAccessType, String seriesIUID) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(), 
                transactionIsolationLevel,
                JdbcProperties.getInstance()
                        .getProperty("QuerySeriesAttrsForQueryCmd"));
        defineColumnTypes(new int[] {
                blobAccessType, // patient.pat_attrs
                blobAccessType, // study.study_attrs
                blobAccessType, // series.series_attrs
                Types.VARCHAR,  // study.mods_in_study
                Types.VARCHAR,  // study.study_status_id
                Types.INTEGER,  // study.num_series
                Types.INTEGER,  // study.num_instances
                Types.INTEGER,  // series.num_instances
                });
        ((PreparedStatement) stmt).setString(1, seriesIUID);
    }

    public Dataset getDataset() throws SQLException {
        Dataset dataset = DcmObjectFactory.getInstance().newDataset();
        DatasetUtils.fromByteArray(rs.getBytes(1), dataset);
        DatasetUtils.fromByteArray(rs.getBytes(2), dataset);
        DatasetUtils.fromByteArray(rs.getBytes(3), dataset);
        dataset.putCS(Tags.ModalitiesInStudy,
                StringUtils.split(rs.getString(4), '\\'));
        dataset.putCS(Tags.StudyStatusID, rs.getString(5));
        dataset.putIS(Tags.NumberOfStudyRelatedSeries, rs.getInt(6));
        dataset.putIS(Tags.NumberOfStudyRelatedInstances, rs.getInt(7));
        dataset.putIS(Tags.NumberOfSeriesRelatedInstances, rs.getInt(8));
        return dataset;
    }
}
