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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Sep 24, 2007
 * 
 */
public class QueryOldARRCmd extends BaseReadCmd {
    public static final class Record {
        public final long pk;
        public final String xml_data;

        public Record(final long pk, String xml_data) {
            this.pk = pk;
            this.xml_data = xml_data;
        }
    };

    public static final int transactionIsolationLevel = 0;

    public QueryOldARRCmd(long skipUntilPk, int limit) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel,
                JdbcProperties.getInstance().getProperty("QueryOldARRCmd"));
        int limitPos = Integer.parseInt(JdbcProperties.getInstance()
                .getProperty("QueryOldARRCmdLimitPos"));
        PreparedStatement ps = ((PreparedStatement) stmt);
		ps.setLong(3-limitPos, skipUntilPk);
		ps.setInt(limitPos, limit);
        execute();
    }

    public int fetch(Record[] result) throws SQLException {
        try {
            int count = 0;
            while (count < result.length && next()) {
                result[count++] = new Record(rs.getLong(1), rs.getString(2));
            }
            return count;
        } finally {
            close();
        }
    }
}
