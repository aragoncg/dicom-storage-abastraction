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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public abstract class BaseUpdateCmd extends BaseCmd {    

    protected BaseUpdateCmd(String dsJndiName, int transactionIsolationLevel,
			String sql) throws SQLException {
		super(dsJndiName, transactionIsolationLevel, sql);
    }
	
    public int execute() throws SQLException {        
        
        SQLException lastException = null;
        for(int i = 0; i < updateDatabaseMaxRetries; i++)
        {
	        try
	        {
	        	return ((PreparedStatement) stmt).executeUpdate();
	        }
	        catch(SQLException e)
	        {
	        	if(lastException == null || !lastException.getMessage().equals(e.getMessage()))
		        	log.warn( "failed to execute sql: "+sql+" - retry: "+(i+1)+" of "+updateDatabaseMaxRetries, e);
	        	else
		        	log.warn( "failed to execute sql: "+sql+". Got the same exception as above - retry: "+(i+1)+" of "+updateDatabaseMaxRetries);
	        	lastException = e;
	        	
				close();
				
				try {
					Thread.sleep(updateDatabaseRetryInterval);
				} catch (InterruptedException e1) { log.warn(e1);} 
				
				try
				{
					open();
				}
				catch(SQLException e1){}
	        }
        }
        throw new SQLException("give up executing SQL statement after all retries: " + sql);       
    }
}
