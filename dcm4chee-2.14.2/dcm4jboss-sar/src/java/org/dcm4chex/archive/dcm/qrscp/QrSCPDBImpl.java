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

package org.dcm4chex.archive.dcm.qrscp;

import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import oracle.jdbc.OracleResultSet;

import org.dcm4chex.archive.dcm.DBConUtil;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.04.2009
 * 
 * This class is used to retrieve the data of Diocm image
 * stored in Oracle 11g database.
 */
public class QrSCPDBImpl {
    
    private Connection con = null;
    
    /**
     * Constructor of QrSCPDBImpl.
     */
    public QrSCPDBImpl() {
        try {
            setDBCon();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Set up a database connection.
     */
    private void setDBCon() throws Exception {
        if (useManualCon()) {
            configCon();
        } else {
            lookupCon();
        }
    }
    
    /**
     * Add looking up code if using connection pool.
     */
    private void lookupCon() {
        // JNDI look up
    }
    
    /**
     * Manually acquire a database connection.
     */
    private void configCon() throws ClassNotFoundException, SQLException {

        con = DriverManager.getConnection(
                DBConUtil.CON_URL, DBConUtil.CON_USER, DBConUtil.CON_PW);
    }
    
    /**
     * Acquire the inputStream of some OrdDicom record according
     * to record Id.
     * This method is thread-safe.
     */
    public InputStream getInputStream(File file) throws Exception{
        
        String fileURI = file.getPath();
        int pos = fileURI.indexOf(DBConUtil.DBSTORE_MARK);
        if(pos != -1) {           
            InputStream inputStream = null;
            PreparedStatement ps1 = null;
            ResultSet rs = null;
            oracle.sql.BLOB blob = null;
            
            int pk = Integer.parseInt(
                    fileURI.substring(pos + DBConUtil.DBSTORE_MARK.length()).trim());
            
//            ps1 = con.prepareStatement("select t.i_image.getContent() from " +
//            		"DICOM_IMAGE t where i_id = ?");
            ps1 = con.prepareStatement("select t." + 
  		                               DBConUtil.IMAGE_COL_NAME + 
		                               ".getContent() from " + 
		                               DBConUtil.TABLE_NAME +
		                               " t where " +                             
		                               DBConUtil.ID_COL_NAME +
                                       " = ?");
            ps1.setInt(1, pk);
            rs = ps1.executeQuery();

            if (rs.next()) {
                blob = ((OracleResultSet) rs).getBLOB(1);
                inputStream = blob.getBinaryStream();
            }
            
            rs.close();
            ps1.close();
            return inputStream;
        }
        else
            throw new Exception("No valid database store mark");
    }
    
    /**
     * Acquire the imageInputStream of some OrdDicom record according
     * to record Id.
     * This method is thread-safe.
     */
    public ImageInputStream getImageInputStream(File file) throws Exception{
        
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(getInputStream(file));
        return imageInputStream;
    }
    
    /**
     * Connection to database can be configured in JBOSS and 
     * acquired through JNDI, or configured and acquired manually.
     * This method decides which way to take.
     */
    private boolean useManualCon() {
    	
        return DBConUtil.MANUAL_CON_FLAG;
    }
    
}
