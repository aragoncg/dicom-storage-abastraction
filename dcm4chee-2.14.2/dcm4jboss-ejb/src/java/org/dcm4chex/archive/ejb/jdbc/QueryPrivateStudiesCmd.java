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
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.util.Convert;

/**
 * 
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7903 $ $Date: 2008-11-03 16:20:48 +0100 (Mon, 03 Nov 2008) $
 * @since 16.12.2005
 */
public class QueryPrivateStudiesCmd extends BaseReadCmd {

    public static int transactionIsolationLevel = 0;
    public static int blobAccessType = Types.LONGVARBINARY;

    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private static final String[] SELECT_ATTRIBUTE = { 
            "PrivatePatient.encodedAttributes",
            "PrivateStudy.encodedAttributes",
            "PrivatePatient.pk",
            "PrivateStudy.pk"};

    private static final String[] ENTITY = {"PrivatePatient"};

    private static final String[] LEFT_JOIN = { 
            "PrivateStudy", null, "PrivatePatient.pk", "PrivateStudy.patient_fk",};

	private boolean hideMissingStudies = false;
    
    private final SqlBuilder sqlBuilder = new SqlBuilder();

    public QueryPrivateStudiesCmd(Dataset filter, int privateType, boolean hideMissingStudies)
            throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel);
    	this.hideMissingStudies = hideMissingStudies;
    	sqlBuilder.setFrom(ENTITY);
        sqlBuilder.setLeftJoin(LEFT_JOIN);
        sqlBuilder.addIntValueMatch(null, "PrivatePatient.privateType",
                SqlBuilder.TYPE1, privateType);
        if ( filter != null ) {
	        sqlBuilder.addWildCardMatch(null, "PrivatePatient.patientId",
	                SqlBuilder.TYPE2,
	                filter.getStrings(Tags.PatientID));
	        sqlBuilder.addWildCardMatch(null, "PrivatePatient.patientName",
	                SqlBuilder.TYPE2,
	                toWildcardMatchString( filter.getString(Tags.PatientName)) );
	        sqlBuilder.addWildCardMatch(null, "PrivateStudy.accessionNumber",
	                SqlBuilder.TYPE2,
	                filter.getStrings(Tags.AccessionNumber));
	        sqlBuilder.addListOfStringMatch(null, "PrivateStudy.studyIuid",
	                SqlBuilder.TYPE1, filter.getStrings( Tags.StudyInstanceUID));
	        sqlBuilder.addCallingAETsNestedMatch(true,
	                filter.getStrings(PrivateTags.CallingAET));
        }
        if ( this.hideMissingStudies ) {
        	sqlBuilder.addNULLValueMatch(null,"PrivateStudy.encodedAttributes", true);
    	}
        	
    }

    private static String toWildcardMatchString(String patientName) {
    	if ( patientName != null ) {
    		patientName = patientName.toUpperCase();
    		if ( patientName.length() > 0 && 
    				patientName.indexOf('*') == -1 &&
					patientName.indexOf('?') == -1) patientName+="*";
    	}
        return patientName;
    }

    public int count() throws SQLException {
        try {
            sqlBuilder.setSelectCount(new String[]{"PrivateStudy.pk"}, true);
            execute( sqlBuilder.getSql() );
            next();
            if (hideMissingStudies) return rs.getInt(1);
            //we have to add number of studies and number of patients without studies.
            int studies = rs.getInt(1);
            rs.close();
            rs = null;
            sqlBuilder.setSelectCount(new String[]{"PrivatePatient.pk"}, true);
        	sqlBuilder.addNULLValueMatch(null,"PrivateStudy.pk", false);
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
                blobAccessType,
                blobAccessType,
                Types.BIGINT,
                Types.BIGINT});
        sqlBuilder.setSelect(SELECT_ATTRIBUTE);
        sqlBuilder.addOrderBy("PrivatePatient.pk", SqlBuilder.ASC);
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
                } 
                result.add(ds);
            }
            return result;
        } finally {
            close();
        }
    }
}