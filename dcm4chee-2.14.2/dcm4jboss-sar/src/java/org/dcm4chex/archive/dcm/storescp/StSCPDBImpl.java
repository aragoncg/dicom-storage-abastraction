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
import java.sql.SQLException;

import org.dcm4chex.archive.dcm.ConnectionDispenser;
import org.dcm4chex.archive.dcm.DBConUtil;

import oracle.jdbc.OracleCallableStatement;
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
            OracleCallableStatement ocs = (OracleCallableStatement)con
                 .prepareCall("BEGIN insert into " +
            		          "\"" +
            		          DBConUtil.TABLE_NAME + 
            		          "\"" +
            		          " t(" + 
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
            		          ") values(" + 
                              "\"" +
                              DBConUtil.SEQUENCE_NAME + 
                              "\"" +
                              ".nextval, sysdate, ORDDicom()) returning " +
                              "\"" +
            		          DBConUtil.ID_COL_NAME +
            		          "\"" +
            		          ", t." +
            		          "\"" +
            		          DBConUtil.IMAGE_COL_NAME + 
            		          "\"" +
            		          ".getContent() into ?, ?; END;");
            
            ocs.registerOutParameter(1, OracleTypes.NUMBER);
            ocs.registerOutParameter(2, OracleTypes.BLOB);
            
			ocs.execute();
			
			//Get the id and the BOLB of the new inserted OrdDicom object
			id = ocs.getLong(1);
            blob = ocs.getBLOB(2);
            
            outStream = blob.setBinaryStream(0);
            
            ocs.close();            
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
        
        multiSql.append("update " +
        		        "\"" +
        		        DBConUtil.TABLE_NAME +
        		        "\"" +
        		        " t set " +
        		        "\"" +
        		        DBConUtil.IMAGE_COL_NAME + 
        		        "\"" +
        		        " = obj where " +
        		        "\"" +
        		        DBConUtil.ID_COL_NAME + 
        		        "\"" +
        		        "=? returning obj.getContentLength() into ?;");
        
        multiSql.append(" end;");

        //Execute the PL/SQL block
        try {
        	OracleCallableStatement multiSqlOcs = (OracleCallableStatement)con
                                                   .prepareCall(multiSql.toString());
        	multiSqlOcs.setLong(1, id);
        	multiSqlOcs.setLong(2, id);
        	multiSqlOcs.registerOutParameter(3, OracleTypes.NUMBER);
            
        	multiSqlOcs.execute();
            imageLength = multiSqlOcs.getLong(3);
            
            con.commit();            
            multiSqlOcs.close();            
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
