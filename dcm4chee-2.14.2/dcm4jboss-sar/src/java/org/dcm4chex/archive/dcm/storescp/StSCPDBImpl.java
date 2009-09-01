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
 * ACG.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Lin Yang <YangLin@cn-arg.com>
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

package org.dcm4chex.archive.dcm.storescp;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.dcm4chex.archive.dcm.ConnectionDispenser;
import org.dcm4chex.archive.dcm.DBConUtil;

import oracle.jdbc.OracleCallableStatement;
import oracle.jdbc.OracleResultSet;
import oracle.jdbc.OracleTypes;
import oracle.sql.BLOB;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.15.2009
 * 
 * This class contains detailed JDBC operation to store 
 * OrdDicom object in Oracle 11g database.
 * Make sure each DB identifier wrapped with a double quote.
 */
public class StSCPDBImpl {
	
    
    /**
     * Acquire the new inserted empty OrdDicom object.
     */
    public DicomObj getDicomObj() {
        
    	DicomObj emptyDicom = new DicomObj();    	
    	long id = 0;
        OutputStream outStream = null;
        BLOB blob = null;
        
        //Get threadLocal connection
    	//The reason of using threadLocal connection is that
    	//we face a situation of distributed transaction
        Connection con = ConnectionDispenser.getConnection();
                       
        try {
            con.setAutoCommit(false);

            //Insert a new empty OrdDicom object
//            PreparedStatement pst2 = con
//                              .prepareStatement("BEGIN insert into DICOM_IMAGE(i_id, i_date, i_image) "
//                              + "values(dicom_seq.nextval, sysdate, ORDDicom()) returning i_id into ?; END;");
            OracleCallableStatement ocs = (OracleCallableStatement)con
                 .prepareCall("BEGIN insert into " +
            		          "\"" +
            		          DBConUtil.TABLE_NAME + 
            		          "\"" +
            		          "(" + 
            		          "\"" +
            		          DBConUtil.ID_COL_NAME +
            		          "\"" +
            		          ", " + 
            		          "\"" +
            		          DBConUtil.DATE_COL_NAME +
            		          "\"" +
            		          ", " +
            		          "\"" +
            		          DBConUtil.IMAGE_COL_NAME + 
            		          "\"" +
            		          ") " + 
                              "values(" + 
                              DBConUtil.SEQUENCE_NAME + 
                              ".nextval, sysdate, ORDDicom()) returning " +
                              "\"" +
            		          DBConUtil.ID_COL_NAME +
            		          "\"" +
            		          " into ?; END;");           
            ocs.registerOutParameter(1, OracleTypes.NUMBER);
			ocs.execute();
			id = ocs.getLong(1);
             
            //Get the BOLB of the new inserted OrdDicom object
//            PreparedStatement pst3 = con
//                    .prepareStatement("select t.i_image.getContent() from DICOM_IMAGE t "
//                            + "where i_id=? for update");
            PreparedStatement pst3 = con
            .prepareStatement("select t." + 
            		          "\"" +
            		          DBConUtil.IMAGE_COL_NAME +
            		          "\"" +
            		          ".getContent() from " +
            		          "\"" +
            		          DBConUtil.TABLE_NAME +
            		          "\"" +
            		          " t where " + 
            		          "\"" +
            		          DBConUtil.ID_COL_NAME +
            		          "\"" +
                              "=? for update");
            pst3.setLong(1, id);
            ResultSet rs = pst3.executeQuery();

            if (rs.next()) {
                blob = ((OracleResultSet)rs).getBLOB(1);
                outStream = blob.setBinaryStream(0);
            }
            rs.close();
            ocs.close();
            pst3.close();
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
        //If SQLException happens, outStream is mostly possible to be null. If the
        //null pointer is returned to storeToDB method, sooner or later a NullPointer
        //Exception will be thrown, so here no need to throw SQLException outside
        con = null;
        emptyDicom.setId(id);
        emptyDicom.setContent(outStream);
        return emptyDicom;
    }
    
    /**
     * Extract the metadata of the new inserted OrdDicom object.
     */
    public long setProperties(long id) throws SQLException{

        Connection con = ConnectionDispenser.getConnection();
        StringBuilder multiSql = new StringBuilder();
        long imageLength = 0;
        
        //Compose a PL/SQL block
        multiSql.append("declare obj orddicom;");
        multiSql.append("begin ");

        //Call setProperties() method and update
//        multiSql
//                .append("select i_image into obj from DICOM_IMAGE where i_id=? for update;");
        multiSql
                .append("select " +
                		"\"" +
                		DBConUtil.IMAGE_COL_NAME + 
                		"\"" +
                		" into obj from " +
                		"\"" +
                		DBConUtil.TABLE_NAME +
                		"\"" +
                		" where " +
                		"\"" +
                		DBConUtil.ID_COL_NAME +
                		"\"" +
                		"=? for update;");
        
        multiSql.append("obj.setProperties();");
        
//        multiSql.append("update DICOM_IMAGE set i_image = obj where i_id=?;");
        multiSql.append("update " +
        		        "\"" +
        		        DBConUtil.TABLE_NAME +
        		        "\"" +
        		        " set " +
        		        "\"" +
        		        DBConUtil.IMAGE_COL_NAME + 
        		        "\"" +
        		        " = obj where " +
        		        "\"" +
        		        DBConUtil.ID_COL_NAME + 
        		        "\"" +
        		        "=?;");
        
        multiSql.append(" end;");

        //Execute the PL/SQL block
        try {
            PreparedStatement multiSqlPst = con.prepareStatement(multiSql
                    .toString());
            multiSqlPst.setLong(1, id);
            multiSqlPst.setLong(2, id);
            multiSqlPst.execute();
           
//            PreparedStatement pst4 = con
//                    .prepareStatement("select t.i_image.getContentLength() from DICOM_IMAGE t "
//                    + "where i_id=?");
            PreparedStatement pst4 = con
            .prepareStatement("select t." +
            		          "\"" +
      	                      DBConUtil.IMAGE_COL_NAME +
      	                      "\"" +
    	                      ".getContentLength() from " + 
    	                      "\"" +
    	                      DBConUtil.TABLE_NAME +
    	                      "\"" +
    	                      " t where " + 
    	                      "\"" +
    	                      DBConUtil.ID_COL_NAME +
    	                      "\"" +
                              "=?");
            
            pst4.setLong(1, id);
            ResultSet rs = pst4.executeQuery();
            if (rs.next()) {
                imageLength = rs.getInt(1);
            }
            
            con.commit();            
            rs.close();
            multiSqlPst.close();
            pst4.close();  
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            } 
            //If SQLException happens, need to manually throw it outside to tell the
            //doActualStore method that a serious error happens and should stop store
            throw e;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        //Release the threadlocal connection
        ConnectionDispenser.releaseConnection(con);
        
        return imageLength;
    }
    
    /**
     * Release resource.
     */
    public void closeResource() {

    }

}

/**
 * POJO for DICOM object
 */
class DicomObj {
	
	private long id;
	
	private OutputStream content;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public OutputStream getContent() {
		return content;
	}

	public void setContent(OutputStream content) {
		this.content = content;
	}
	
}
