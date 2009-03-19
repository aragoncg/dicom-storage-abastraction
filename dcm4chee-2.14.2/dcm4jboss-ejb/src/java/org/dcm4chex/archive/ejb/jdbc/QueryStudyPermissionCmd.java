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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.security.auth.Subject;

import org.dcm4chex.archive.common.SecurityUtils;

/**
 * 
 * @author franz.willer@agfa.com
 * @version $Revision: $ $Date: $
 * @since 03.11.2007
 */
public class QueryStudyPermissionCmd extends BaseReadCmd {

    private static final String[] SELECT_ATTRIBUTE = null;

	public static int transactionIsolationLevel = 0;

    private final SqlBuilder sqlBuilder = new SqlBuilder();

    public QueryStudyPermissionCmd() throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel);
    	sqlBuilder.setFrom( new String[] {"StudyPermission"} );
        sqlBuilder.setSelect( new String[] { "StudyPermission.studyIuid","StudyPermission.action" });
    }
    
	
    public Map getGrantedActionsForStudies(String[] studyIUIDs, Subject subject) throws SQLException {
    	String[] roles = SecurityUtils.rolesOf(subject);
    	sqlBuilder.addListOfStringMatch(null, "StudyPermission.studyIuid", false, studyIUIDs );
   		sqlBuilder.addListOfStringMatch(null, "StudyPermission.role", false, roles );
        try {
            execute(sqlBuilder.getSql());
            HashMap result = new HashMap();
            Set actions;
            String suid;
            while (next()) {
            	suid = rs.getString(1);
            	actions = (Set) result.get(suid);
            	if ( actions == null ) {
            		actions = new HashSet();
            		result.put(suid, actions);
            	}
            	actions.add(rs.getString(2));
            }
            return result;
        } finally {
            close();
        }
    }
}