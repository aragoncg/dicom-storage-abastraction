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
import org.dcm4chex.archive.common.HPLevel;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5787 $ $Date: 2008-01-31 03:48:04 +0100 (Thu, 31 Jan 2008) $
 * @since Aug 17, 2005
 */
public class HPQueryCmd extends BaseDSQueryCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;

    private static final String[] FROM = { "HP" };

    private static final String[] SELECT = { "HP.encodedAttributes" };

    private static final String USER_CODE = "user_code";
    private static final String[] REGION_CODE = new String[]{"region_code","rel_hpdef_region","rel_hpdef_region.hpdef_fk",
    														 "rel_hpdef_region.region_fk"};
    private static final String[] PROC_CODE = new String[]{"proc_code","rel_hpdef_proc","rel_hpdef_proc.hpdef_fk",
    														"rel_hpdef_proc.proc_fk"};
    private static final String[] REASON_CODE = new String[]{"reason_code","rel_hpdef_reason",
    													"rel_hpdef_reason.hpdef_fk","rel_hpdef_reason.reason_fk"};

    public HPQueryCmd(Dataset keys) throws SQLException {
		super(keys, true, false, transactionIsolationLevel);
	        defineColumnTypes(new int[] { blobAccessType });
		String s;
		int i;
		// ensure keys contains (8,0005) for use as result filter
		if (!keys.contains(Tags.SpecificCharacterSet)) {
			keys.putCS(Tags.SpecificCharacterSet);
		}
		sqlBuilder.setSelect(SELECT);
		sqlBuilder.setFrom(FROM);
		sqlBuilder.setLeftJoin(getLeftJoin());
		sqlBuilder.addListOfUidMatch(null, "HP.sopIuid", SqlBuilder.TYPE1, keys
				.getStrings(Tags.SOPInstanceUID));
		sqlBuilder.addListOfUidMatch(null, "HP.sopCuid", SqlBuilder.TYPE1, keys
				.getStrings(Tags.SOPClassUID));
		sqlBuilder.addWildCardMatch(null, "HP.hangingProtocolName",
				SqlBuilder.TYPE2, keys.getStrings(Tags.HangingProtocolName));
		if ((s = keys.getString(Tags.HangingProtocolLevel)) != null) {
			sqlBuilder.addIntValueMatch(null, "HP.hangingProtocolLevelAsInt",
					SqlBuilder.TYPE1, HPLevel.toInt(s));
		}
		if ((i = keys.getInt(Tags.NumberOfPriorsReferenced, -1)) != -1) {
			sqlBuilder.addIntValueMatch(null, "HP.numberOfPriorsReferenced",
					SqlBuilder.TYPE1, i);
		}
		if ((i = keys.getInt(Tags.NumberOfScreens, -1)) != -1) {
			sqlBuilder.addIntValueMatch(null, "HP.numberOfScreens",
					SqlBuilder.TYPE2, i);
		}
		sqlBuilder.addWildCardMatch(null, "HP.hangingProtocolUserGroupName",
				SqlBuilder.TYPE2, keys.getStrings(Tags.HangingProtocolUserGroupName));
		addCodeMatch(keys
				.getItem(Tags.HangingProtocolUserIdentificationCodeSeq),
				USER_CODE);
		Dataset item = keys.getItem(Tags.HangingProtocolDefinitionSeq);
		if (item != null) {
			sqlBuilder.addWildCardMatch(null, "HPDefinition.modality",
					SqlBuilder.TYPE2, item.getStrings(Tags.Modality));
			sqlBuilder.addWildCardMatch(null, "HPDefinition.laterality",
					SqlBuilder.TYPE2, item.getStrings(Tags.Laterality));
			addCodeMatch(item.getItem(Tags.AnatomicRegionSeq), REGION_CODE);
			addCodeMatch(item.getItem(Tags.ProcedureCodeSeq), PROC_CODE);
			addCodeMatch(item.getItem(Tags.ReasonforRequestedProcedureCodeSeq),
					REASON_CODE);
		}
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

    private void addCodeMatch(Dataset item, String[] codeQuery) {
        if ( isMatchCode(item) ){
        	SqlBuilder subQuery = new SqlBuilder();
        	subQuery.setSelect(new String[]{"HPDefinition.pk"});
        	subQuery.setFrom(new String[]{"HPDefinition",codeQuery[1],"Code"});
        	subQuery.addFieldValueMatch(null, "HPDefinition.pk", SqlBuilder.TYPE1, null, codeQuery[2]);
        	subQuery.addFieldValueMatch(null, "Code.pk", SqlBuilder.TYPE1, null, codeQuery[3]);
        	subQuery.addSingleValueMatch(null, "Code.codeValue",
                    SqlBuilder.TYPE2,
                    item.getString(Tags.CodeValue));
        	subQuery.addSingleValueMatch(null, "Code.codingSchemeDesignator",
                    SqlBuilder.TYPE2,
                    item.getString(Tags.CodingSchemeDesignator));
            Match.Node node0 = sqlBuilder.addNodeMatch("OR",false);
            node0.addMatch( new Match.Subquery(subQuery, null, null));
        }
    }

    private boolean isMatchCode(Dataset code) {
        return code != null
                && (code.containsValue(Tags.CodeValue)
                        || code.containsValue(Tags.CodingSchemeDesignator));
    }

    private String[] getLeftJoin() {
        ArrayList list = new ArrayList(); 	 
        if (isMatchCode(keys.getItem(Tags.HangingProtocolUserIdentificationCodeSeq))) {
        	list.add("Code");
        	list.add(USER_CODE);
        	list.add("HP.user_fk");
        	list.add("Code.pk");
        }
        Dataset item = keys.getItem(Tags.HangingProtocolDefinitionSeq); 	 
        if (item != null && !item.isEmpty()) { 	 
                list.add("HPDefinition"); 	 
                list.add(null); 	 
                list.add("HP.pk"); 	 
                list.add("HPDefinition.hp_fk"); 	 
        }
        return (String[]) (list.isEmpty() ? null : list.toArray(new String[list.size()])); 	 
    }

    public void execute() throws SQLException {
        execute(sqlBuilder.getSql());
    }

    public Dataset getDataset() throws SQLException {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        DatasetUtils.fromByteArray( rs.getBytes(1), ds);
        adjustDataset(ds, keys);
        return ds.subSet(keys);
    }
	
}
