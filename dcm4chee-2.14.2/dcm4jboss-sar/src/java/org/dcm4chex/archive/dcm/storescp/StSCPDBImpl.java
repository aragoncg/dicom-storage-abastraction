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
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.dcm4chex.archive.dcm.ConnectionDispenser;
import org.dcm4chex.archive.dcm.DBConUtil;

import oracle.jdbc.OracleResultSet;
import oracle.sql.BLOB;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.15.2009
 * 
 * This class contains detailed JDBC operation to store Dicom image object.
 */
public class StSCPDBImpl {

    private static int newId = maxId();

    private static int maxId() {
        int maxNum = 0;
        Connection con = null;
        try {
            con = DriverManager.getConnection(DBConUtil.CON_URL,
                    DBConUtil.CON_USER, DBConUtil.CON_PW);
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("select max(i_id) from DICOM_IMAGE");
            if (rs.next()) {
                maxNum = rs.getInt(1);
                System.out.println("The current max id is " + maxNum);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return maxNum;

    }

    public int getNewId() {
        int id = 0;
        synchronized (this) {
            id = newId + 1;
            newId++;
        }
        return id;
    }
    //Called before writing content
    public OutputStream getOutputStream(int id) {
        
        //ThreadLocal Connection
        Connection con = ConnectionDispenser.getConnection();
        OutputStream outStream = null;
        BLOB blob = null;
        
        try {
            con.setAutoCommit(false);

            // Insert a new empty OrdDicom object
            PreparedStatement pst2 = con
                    .prepareStatement("insert into DICOM_IMAGE(i_id, i_date, i_image) "
                            + "values(?, to_date('05/16/2008', 'MM/DD/YYYY'), ORDDicom())");
            pst2.setInt(1, id);
            pst2.executeUpdate();

            // Get the BOLB of the new inserted OrdDicom object
            PreparedStatement pst3 = con
                    .prepareStatement("select t.i_image.getContent() from DICOM_IMAGE t "
                            + "where i_id=? for update");
            pst3.setInt(1, id);
            ResultSet rs = pst3.executeQuery();

            if (rs.next()) {
                blob = ((OracleResultSet)rs).getBLOB(1);
                outStream = blob.setBinaryStream(0);
            }
            rs.close();
            pst2.close();
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
        return outStream;
    }
    //Called after writing content
    public long setProperties(int id) throws SQLException{

        Connection con = ConnectionDispenser.getConnection();
        StringBuilder multiSql = new StringBuilder();
        long imageLength = 0;
        
        // Compose a PL/SQL block
        multiSql.append("declare obj orddicom;");
        multiSql.append("begin ");

        /*
         * Before call setProperties() call ord_dicom.setdatamodel first;
         * This method is necessary and it is only valid in one connection
         * which means that it must be called once everytime open a new 
         * connection 
         */
        multiSql.append("ord_dicom.setdatamodel;");

        // Call setProperties() method and update
        multiSql
                .append("select i_image into obj from DICOM_IMAGE where i_id=? for update;");
        multiSql.append("obj.setProperties();");
        multiSql.append("update DICOM_IMAGE set i_image = obj where i_id=?;");
        multiSql.append(" end;");

        // Execute the PL/SQL block
        try {
            PreparedStatement multiSqlPst = con.prepareStatement(multiSql
                    .toString());
            multiSqlPst.setInt(1, id);
            multiSqlPst.setInt(2, id);
            multiSqlPst.execute();
           
            PreparedStatement pst4 = con
                    .prepareStatement("select t.i_image.getContentLength() from DICOM_IMAGE t "
                    + "where i_id=?");
            pst4.setInt(1, id);
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
            throw e;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //If SQLException happens, need to manually throw it outside to tell the
        //doActualStore method that a serious error happens and should stop store
        con = null;
        return imageLength;
    }

    public void closeResource() {

    }

}
