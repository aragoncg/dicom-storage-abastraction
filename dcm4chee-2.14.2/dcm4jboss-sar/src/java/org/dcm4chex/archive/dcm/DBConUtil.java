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

import java.io.Console;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

import org.jboss.dom4j.Document;
import org.jboss.dom4j.DocumentException;
import org.jboss.dom4j.Element;
import org.jboss.dom4j.io.SAXReader;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.Cipher;
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
    
    /** If password is provided in plain text. */
    public static boolean readPlainText = true;
    
    /** Fields for RSAEncrypt. */
    private static RSAEncrypt encrypter;
    private static RSAPrivateKey privateKey;
    private static RSAPublicKey publicKey;
    private static byte[] encodedBytes;
    
    /** Connection pool. */
    private static PoolDataSource pds;
    
    /** Default connection factory class. */
    private static String CON_FACTORY = "oracle.jdbc.pool.OracleDataSource";
    
    /** Connection pool size properties. */
    private static int INI_POOL_SIZE = 5;  
    private static int MIN_POOL_SIZE = 5;   
    private static int MAX_POOL_SIZE = 20;
    /** Time(in second) how long a borrowed connection can remain unused 
     *  before it is considered as abandoned and reclaimed by connection 
     *  pool. */
    private static int ABANDONED_CON_TIMEOUT = 5;
    
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
            	//Read pool size properties
            	
            	//Check if user name and password provided
            	if(!(!CON_PW.equals("") && !CON_USER.equals(""))) {
            	   readPlainText = false;
            	   
            	   //If exception happens during querying password system will default to save 
            	   //image in file system
            	   if(!queryPassword())
            		   return saveToDB;      	   
            	}
            	
            	//Populate a pool-enabled data source
            	pds = PoolDataSourceFactory.getPoolDataSource();            	
     		    try {
     		    	//Set connection properties of the data source
					pds.setConnectionFactoryClassName(CON_FACTORY);
					pds.setURL(CON_URL);
	     		    pds.setUser(CON_USER);
	     		    if(!CON_PW.equals("") && !readPlainText) 
	     		       pds.setPassword(decodePassword(CON_PW));	            	   
	            	else
	            	   pds.setPassword(CON_PW);	
	            	
	     		    //Set pool properties
	     		    pds.setInitialPoolSize(INI_POOL_SIZE);
	     		    pds.setMinPoolSize(MIN_POOL_SIZE);
	     		    pds.setMaxPoolSize(MAX_POOL_SIZE);
	     		    pds.setAbandonedConnectionTimeout(ABANDONED_CON_TIMEOUT);
	     		    
				} catch (SQLException e) {
					e.printStackTrace();
					//If exception happens system will default to save image in file system
					return saveToDB;
				}
     		    
            	
            	//Check column types according to column names provided if default schema not applied
            	String useDefSchema = root.elementText("use-default-table").trim().toUpperCase();
            	if(!useDefSchema.equals("YES")){
            		TABLE_NAME = root.elementText("table_name").trim();
            		ID_COL_NAME = root.elementText("id_col_name").trim();
            		DATE_COL_NAME = root.elementText("date_col_ame").trim();
            		IMAGE_COL_NAME = root.elementText("image_col_name").trim();
            		
            		//If fail the type check system will default to save image in file system
            		if(!checkColType()) {
//            			System.out.println("Invalid column types user provided!" +
//            					           "Image storage will be defaulted to file system!");            			
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
     * Query user name and password in console.
     */
    private static boolean queryPassword() {
    	
       //Prompt for user name and password input
       Console console = System.console();
       if (console == null) {
            System.err.println("Console not available");
            return false;
       }
       String username = console.readLine("Enter Oracle 11g Database username: ");
       String password = new String(console.readPassword("Enter Oracle 11g Database password: "));
       
//    	String username = "SYSTEM";
//    	String password = "000000";
    	
       if(username.equals("")) {
    	   System.out.println("NO username input. Database storage NOT available");
    	   return false;
       }
       CON_USER = username;
       
       if(!password.equals(""))
          CON_PW = encodePassword(password);
       else 
    	  CON_PW = "";
       
       return true;
    }

    /**
     * Encode password in RSA way.
     * If failed return unchanged text.
     */
	private static String encodePassword(String pw) {
		
		encrypter = new RSAEncrypt();
        
        String encryptText = pw;
        KeyPairGenerator keyPairGen;
        
		try {
			keyPairGen = KeyPairGenerator.getInstance("RSA");
			keyPairGen.initialize(1024);
	        KeyPair keyPair = keyPairGen.generateKeyPair();
	        
	        // Generate keys
	        privateKey = (RSAPrivateKey) keyPair.getPrivate();
	        publicKey = (RSAPublicKey) keyPair.getPublic();
	        
	        encodedBytes = encrypter.encrypt(publicKey, encryptText.getBytes());
	        
	        //If encrypt fails password will be returned without change
	        if(encodedBytes != null)
	           encryptText = encrypter.bytesToString(encodedBytes);
		} catch (NoSuchAlgorithmException e) {			
			e.printStackTrace();
		}
        
		return encryptText;
	}
	
	/**
     * Decode password in RSA way.
     * If failed return unchanged text.
     */
	private static String decodePassword(String pw) {
		
		String decryptText = pw;
		
		//If encrypt failed before, no need to do decrypt
		if(encodedBytes != null) {
		   byte[] result = encrypter.decrypt(privateKey, encodedBytes);
		   
		   //If decrypt fails SQL exception will be thrown soon in checkColType()
		   if(result != null)
		      decryptText = encrypter.bytesToString(result);
		}
		
		return decryptText;
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
        	con = pds.getConnection();       	           
            pst = con.prepareStatement("select DATA_TYPE from SYS.USER_TAB_COLUMNS t " +
            		                   "where t.TABLE_NAME = ? and t.COLUMN_NAME = ?");
             
            pst.setString(1, TABLE_NAME);
            
            for(int i=0; i<3; i++) {           
            	pst.setString(2, colNames[i]);
                rs = pst.executeQuery();
                if (rs.next()) {
                	//If any of column types not qualified
                	if(!rs.getString(1).equals(typeNames[i])){
                		isNameValid = false;
                		break;
                	}
                //If any of table and column names not found		
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
            	if(con != null) {
                   con.close();
                   con = null;
            	}
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    	
        return isNameValid;
    }
    
    /**
     * Borrow connection from connection pool.
     */
    public static Connection borrowConnection(Object fromWhom) {
    	
    	Connection con = null;
    	
    	//No exception will be thrown since it has pass through checkColType() 
    	try {
    		System.out.println("Available connection number is: " + 
    				           pds.getAvailableConnectionsCount());
    		
    		//Suppose this method is synchronized(it should be)
			con = pds.getConnection();
			
			System.out.println("*** " + fromWhom + " borrowed a connection ***");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return con;
    }
    
	public static void main(String[] args) {
		
	}

}


/**
 * RSA Encrypt class with encrypt and decrypt methods.
 */
class RSAEncrypt { 
    
	/**
     * Transform bytes to string. 
     */
    protected String bytesToString(byte[] encrytpByte) {
        String result = "";
        for (Byte bytes : encrytpByte) {
            result += (char) bytes.intValue();
        }
        return result;
    }
    
    /**
     * RSA Encode.
     */
    protected byte[] encrypt(RSAPublicKey publicKey, byte[] obj)  {
        if (publicKey != null) {
            try {
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                return cipher.doFinal(obj);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * RSA Decode.
     */
    protected byte[] decrypt(RSAPrivateKey privateKey, byte[] obj) {
        if (privateKey != null) {
            try {
                 Cipher cipher = Cipher.getInstance("RSA");
                 cipher.init(Cipher.DECRYPT_MODE, privateKey);
                 return cipher.doFinal(obj);
            } catch (Exception e) {
                 e.printStackTrace();
            }
        }    
        return null;
    }
}