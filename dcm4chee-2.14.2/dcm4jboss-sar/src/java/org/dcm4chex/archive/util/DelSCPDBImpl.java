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

package org.dcm4chex.archive.util;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.dcm4chex.archive.dcm.DBConUtil;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.09.2008
 * 
 * This class is used to delete OrdDicom records stored
 * in Oracle 11g database.
 * This class is thread-safe.
 * Make sure each DB identifier wrapped with a double quote.
 */
public class DelSCPDBImpl {
    
    /**
     * Delete OrdDicom record according to record Id in database.
     */
    public static int deleteDBFile(File file) throws Exception{
           	
        int number = -1;
        String fileURI = file.getPath();
        int pos = fileURI.indexOf(DBConUtil.DBSTORE_MARK);
        
        Connection con = null;
        PreparedStatement ps1 = null;
        
        //If the record is found stored in database
        if(pos != -1) {
            int pk = Integer.parseInt(
                    fileURI.substring(pos + DBConUtil.DBSTORE_MARK.length()).trim());
            
            con = DBConUtil.borrowConnection(DelSCPDBImpl.class);
            
//            ps1 = con.prepareStatement("delete DICOM_IMAGE where i_id=?");
            ps1 = con.prepareStatement("delete " +
            		                   "\"" +
            		                   DBConUtil.TABLE_NAME +
            		                   "\"" +
            		                   " where " +
            		                   "\"" +
            		                   DBConUtil.ID_COL_NAME +
            		                   "\"" +
            		                   "=?");
            ps1.setInt(1, pk);
            number = ps1.executeUpdate();
        }
        
        //Release resource
        //Catching the possible SQLExcption but not throwing it for
        //we don't want the exception caught by caller's try clause 
        try {
            if(ps1 != null) {           
               ps1.close();
            }
            if(con != null) {
               con.close();
               con = null;
            }     
        }catch (SQLException e) {
            e.printStackTrace();
        }      
     	       
        return number;
    }

}
