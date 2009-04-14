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

package org.dcm4chex.archive.dcm;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jboss.dom4j.Document;
import org.jboss.dom4j.DocumentException;
import org.jboss.dom4j.Element;
import org.jboss.dom4j.io.SAXReader;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.04.2009
 * 
 * Set JDBC connection related items according to 
 * predefined configuration file.
 */
public class DBConUtil {

	/** If connection is set up manually or not. */
    public static boolean MANUAL_CON_FLAG;
    
    public static String CON_DRIVER;
    
    public static String CON_URL;
    
    public static String CON_USER;
    
    public static String CON_PW; 
    
    /** A mark to indicate database storage. */
    public static String DBSTORE_MARK;
    
    /** Default table name. */
    public static String TABLE_NAME = "DICOM_IMAGE";
    
    /** Default Id column name. */
    public static String ID_COL_NAME = "I_ID";
    
    /** Default Date column name. */
    public static String DATE_COL_NAME = "I_DATE";
    
    /** Default DICOM image column name. */
    public static String IMAGE_COL_NAME = "I_IMAGE";
    
    /** If image is to be stored in database. */
    public static boolean saveToDB = readSaveOpt(); 
        
    /**
     * Read storage option from configure file.
     */
    private static boolean readSaveOpt() {
    	
        boolean saveToDB = false;
        String key = "save-to";
        String defValue = "FILE";
        String filePath = (System.getProperty("user.dir") + 
        		           "//..//server//default//deploy//dcm4chee-storage-opt.xml")
                           .replace("//", File.separator);
        
//        String filePath = (System.getProperty("user.dir") + 
//        "//dcm4chee-storage-opt.xml")
//        .replace("//", File.separator);
        
        SAXReader reader = new SAXReader();

        try {
        	Document document = reader.read(new File(filePath));
        	
        	Element root = document.getRootElement(); 
        	
        	String value = root.elementText(key).trim().toUpperCase();
        	
            if(!value.equals(defValue)) {
            	
            	MANUAL_CON_FLAG = Boolean.parseBoolean(root.elementText("manual-con-flag").trim());
            	CON_DRIVER = root.elementText("con-driver").trim();
            	CON_URL = root.elementText("con-url").trim();
            	CON_USER = root.elementText("con-user").trim();
            	CON_PW = root.elementText("con-password").trim();
            	DBSTORE_MARK = root.elementText("dbstore-mark").trim();
            	
            	String useDefSchema = root.elementText("use-default-table").trim().toUpperCase();
            	if(!useDefSchema.equals("YES")){
            		TABLE_NAME = root.elementText("table_name").trim();
            		ID_COL_NAME = root.elementText("id_col_name").trim();
            		DATE_COL_NAME = root.elementText("date_col_ame").trim();
            		IMAGE_COL_NAME = root.elementText("image_col_name").trim();
            		
            		//Check the column types according to the names user provided
            		if(!checkColType()) {
//            			System.out.println("Invalid column types user provided!" +
//            					           "Image storage will be defaulted to file system!");
            			//If fail the type check system will default to save image in file system
            			return saveToDB;
            		}
            	}
            	
                saveToDB = true;
            }          
        } catch (DocumentException e) {
			e.printStackTrace();
		} 
        
        return saveToDB;
    }
    
    /**
     * Check if the types of columns retrieved by the names
     * user provided are valid.
     */
    private static boolean checkColType() {
    	
    	String[] colNames = {ID_COL_NAME, DATE_COL_NAME, IMAGE_COL_NAME}; 
    	String[] typeNames = {"NUMBER", "DATE", "ORDDICOM"};
    	boolean isNameValid = true;
    	
    	Connection con = null;
        PreparedStatement pst = null;
        ResultSet rs = null;
        
        
        try {
            con = DriverManager.getConnection(CON_URL, CON_USER, CON_PW);
            pst = con.prepareStatement("select DATA_TYPE from User_Tab_Columns t " +
            		                   "where t.TABLE_NAME = ? and t.COLUMN_NAME = ?");
            pst.setString(1, TABLE_NAME.toUpperCase());
            
            for(int i=0; i<3; i++) {           
                pst.setString(2, colNames[i].toUpperCase());
                rs = pst.executeQuery();
                if (rs.next()) {  
                	if(!rs.getString(1).equals(typeNames[i])){
                		isNameValid = false;
                		break;
                	}
                		
                }else {
                	isNameValid = false;
                	break;
                }	
            }                      
        } catch (SQLException e) {
            e.printStackTrace();
            isNameValid = false;         
        } finally {
        	try {
            	if(rs != null)
                   rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
            	if(pst != null)
                   pst.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
            	if(con != null)
                   con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    	
        return isNameValid;
    }
    
	public static void main(String[] args) {
        
	}

}
