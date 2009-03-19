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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;

/**
 * @author Gunter.Zeilinger@tiani.com
 * @version $Revision: 5729 $ $Date: 2008-01-22 10:39:34 +0100 (Tue, 22 Jan 2008) $
 * @since 26.08.2003
 */
public class RetrieveStudyDatesCmd extends BaseReadCmd {

    public static int transactionIsolationLevel = 0;

    //be aware of order the fields: dont mix up different types (updated, created)  
    private static final String[] SELECT_ATTRIBUTE = { "Patient.updatedTime", "Study.updatedTime", "Series.updatedTime", "Instance.updatedTime",
    												"Patient.createdTime", "Study.createdTime", "Series.createdTime", "Instance.createdTime" };

    private static final String[] ENTITY = { "Patient", "Study", "Series",
            "Instance"};


    private static final String[] RELATIONS = { "Patient.pk",
            "Study.patient_fk", "Study.pk", "Series.study_fk", "Series.pk",
            "Instance.series_fk"};
    
    
    public static RetrieveStudyDatesCmd create(Dataset keys)
            throws SQLException {
        String qrLevel = keys.getString(Tags.QueryRetrieveLevel);
        if (qrLevel == null || qrLevel.length() == 0)
            throw new IllegalArgumentException("Missing QueryRetrieveLevel");
		if ("IMAGE".equals(qrLevel)) {
            return new RetrieveStudyDatesCmd(new ImageSql(keys).getSql());
        }
		if ("SERIES".equals(qrLevel)) {
			return new RetrieveStudyDatesCmd(new SeriesSql(keys, true).getSql());
        }
		if ("STUDY".equals(qrLevel)) {
			return new RetrieveStudyDatesCmd(new StudySql(keys, true).getSql());
        }
        if ("PATIENT".equals(qrLevel)) {			
			return new RetrieveStudyDatesCmd(new PatientSql(keys, true).getSql());
        }
        throw new IllegalArgumentException("QueryRetrieveLevel=" + qrLevel);
     }

    protected RetrieveStudyDatesCmd(String sql) throws SQLException {
        super(JdbcProperties.getInstance().getDataSource(),
                transactionIsolationLevel, ResultSet.TYPE_SCROLL_INSENSITIVE);
		execute(sql);
    }

    public Date getMostRecentUpdatedTime() throws SQLException {
    	return getBoundary(1, 4, false);//1 index of field patient.updatedTime
    }
    public Date getMostRecentCreatedTime() throws SQLException {
    	return getBoundary(5, 8, false);//5 index of field patient.createdTime
    }
    
    public Date getEldestUpdatedTime() throws SQLException {
    	return getBoundary(1, 4, true);//1 index of field patient.updatedTime
    }
    public Date getEldestCreatedTime() throws SQLException {
    	return getBoundary(5, 8, true);//5 index of field patient.createdTime
    }

    /**
     * Return the 'eldest' or 'most recent' date/time of all timestamps.
     * <p>
     * With eldest you can switch between 'eldest' or 'most recent' boundary.
     * <p>
     * You can select the relevant fields with <code>startIdx and endIdx</code>. 
     * 
     * @param startIdx
     * @param endIdx
     * @param eldest If true use boundary 'eldest', otherwise 'most recent'
     * @return
     * @throws SQLException
     */
    private Date getBoundary(int startIdx, int endIdx, boolean eldest) throws SQLException {
		try {
			rs.beforeFirst();
			if ( ! next() ) {
				return null;
			}
			endIdx++;
			Date mrDate;
			Date date = rs.getTimestamp(startIdx);
			rs.beforeFirst();
			while (next()) {
				for ( int i = startIdx ; i < endIdx ; i++ ) {
					mrDate = rs.getTimestamp(i);
					if ( eldest )
						date = mrDate.before(date) ? mrDate : date;
					else
						date = mrDate.after(date) ? mrDate : date;
				}
			}
			return date;
		} finally {
			close();
		}
		
	}
    
    /**
     * Returns an array of all updated times of given row of current result set.
     * <p>
     * Return null if row is not within current result set.
     * <p>
     * This method changes the current row position!
     * 
     * @param row row position (first row is 1)
     * 
     * @return Date array with updated dates (Patient, Study, Series, Instance)
     * 
     * @throws SQLException
     */
    public Date[] getUpdatedTimes( int row ) throws SQLException {
    	rs.absolute(row);
    	if ( rs.isAfterLast() || rs.isBeforeFirst() ) return null;
    	Date[] dates = new Date[] { rs.getTimestamp(1), rs.getTimestamp(2),
    								rs.getTimestamp(3), rs.getTimestamp(4) };
    	
    	return dates;
    }

    /**
     * Returns an array of all created times of given row of current result set.
     * <p>
     * Return null if row is not within current result set.
     * <p>
     * This method changes the current row position!
     * 
     * @param row row position (first row is 1)
     * 
     * @return Date array with created dates (Patient, Study, Series, Instance)
     * 
     * @throws SQLException
     */
    public Date[] getCreatedTimes( int row ) throws SQLException {
    	rs.absolute(row);
    	if ( rs.isAfterLast() || rs.isBeforeFirst() ) return null;
    	Date[] dates = new Date[] { rs.getTimestamp(5), rs.getTimestamp(6),
    								rs.getTimestamp(7), rs.getTimestamp(8) };
    	
    	return dates;
    }

	private static class Sql {
		final SqlBuilder sqlBuilder = new SqlBuilder();
		Sql() {
	        sqlBuilder.setSelect(SELECT_ATTRIBUTE);
	        sqlBuilder.setFrom(ENTITY);
	        sqlBuilder.setRelations(RELATIONS);
		}
		public final String getSql() {
			return sqlBuilder.getSql();
		}
	}
	
	private static class PatientSql extends Sql {
		PatientSql(Dataset keys, boolean patientRetrieve) {
            String pid = keys.getString(Tags.PatientID);
            if (pid != null)
	            sqlBuilder.addWildCardMatch(null, "Patient.patientId",
	                    SqlBuilder.TYPE2, pid);
            else if (patientRetrieve)
                throw new IllegalArgumentException("Missing PatientID");
		}
	}

	private static class StudySql extends PatientSql {
		StudySql(Dataset keys, boolean studyRetrieve) {
			super(keys, false);
            String[] uid = keys.getStrings(Tags.StudyInstanceUID);
            if (uid != null && uid.length != 0)
	            sqlBuilder.addListOfUidMatch(null, "Study.studyIuid",
	                    SqlBuilder.TYPE1, uid);
            else if (studyRetrieve)
                throw new IllegalArgumentException("Missing StudyInstanceUID");
		}
	}
	
	private static class SeriesSql extends StudySql {
		SeriesSql(Dataset keys, boolean seriesRetrieve) {
			super(keys, false);
            String[] uid = keys.getStrings(Tags.SeriesInstanceUID);
            if (uid != null && uid.length != 0)
	            sqlBuilder.addListOfUidMatch(null, "Series.seriesIuid",
	                    SqlBuilder.TYPE1, uid);
            else if (seriesRetrieve)
                throw new IllegalArgumentException("Missing SeriesInstanceUID");
		}
	}
	
	private static class ImageSql extends SeriesSql {
		ImageSql(Dataset keys) {
			super(keys, false);
            String[] uid = keys.getStrings(Tags.SOPInstanceUID);
            if (uid != null && uid.length != 0)
	            sqlBuilder.addListOfUidMatch(null, "Instance.sopIuid",
	                    SqlBuilder.TYPE1, uid);
            else 
				throw new IllegalArgumentException("Missing SOPInstanceUID");
		}
	}

	private static class RefSOPSql extends Sql {
		RefSOPSql(DcmElement refSOPSeq) {
	        String[] uid = new String[refSOPSeq.countItems()];
	        for (int i = 0; i < uid.length; i++) {
	            uid[i] = refSOPSeq.getItem(i).getString(Tags.RefSOPInstanceUID);
	        }

	        sqlBuilder.addListOfUidMatch(null, "Instance.sopIuid", SqlBuilder.TYPE1,
	                uid);
		}
	}
	
}
