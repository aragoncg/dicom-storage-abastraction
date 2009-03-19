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

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.DatasetUtils;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5787 $ $Date: 2008-01-31 03:48:04 +0100 (Thu, 31 Jan 2008) $
 * @since Aug 22, 2005
 */
public class HPRetrieveCmd extends BaseReadCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;

    private static final String[] FROM = { "HP" };

    private static final String[] SELECT = { "HP.encodedAttributes" };

    private final SqlBuilder sqlBuilder = new SqlBuilder();

    public HPRetrieveCmd(Dataset keys) throws SQLException {
		super(JdbcProperties.getInstance().getDataSource(),
		        transactionIsolationLevel);
                defineColumnTypes(new int[] { blobAccessType });
		sqlBuilder.setSelect(SELECT);
		sqlBuilder.setFrom(FROM);
		sqlBuilder.addListOfUidMatch(null, "HP.sopIuid", SqlBuilder.TYPE1,
				keys.getStrings(Tags.SOPInstanceUID));
	}
	
	public List getDatasets() throws SQLException {
		ArrayList result = new ArrayList();
		try {
	        execute(sqlBuilder.getSql());
			while (next()) {
				result.add(DatasetUtils.fromByteArray(
				        rs.getBytes(1)));			
			}
		} finally {
			close();
		}
		return result;
    }
	
}
