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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author YangLin@cn-arg.com
 * @version 1.0
 * @since 01.15.2009
 * 
 * Create thread_local connections in case of concurrent storage.
 */
public class ConnectionDispenser {
	
	/**
     * Set up a threadLocal connection and call setdatamodel.
     */
    private static class ThreadLocalConnection extends ThreadLocal<Connection> {
        public Connection initialValue() {
            Connection con = null;
            try {
                con = DBConUtil.borrowConnection(this);
            	
                StringBuilder multiSql = new StringBuilder();
                
                multiSql.append("begin ");
                multiSql.append("ord_dicom.setdatamodel;");
                multiSql.append(" end;");
                
                Statement st = con.createStatement();
                st.execute(multiSql.toString());
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return con;
        }
    }

    private static ThreadLocalConnection conn = new ThreadLocalConnection();
    
    /**
     * Acquire a threadLocal connection.
     */
    public static Connection getConnection() {
        return conn.get();
    }
    
    /**
     * Release borrowed connection to the pool.
     */
    public static void releaseConnection(Connection borrowedCon) {
    	try {
			borrowedCon.close();
			borrowedCon = null;
		} catch (SQLException e) {
			e.printStackTrace();
		} 		
		conn.remove();
    }

}
