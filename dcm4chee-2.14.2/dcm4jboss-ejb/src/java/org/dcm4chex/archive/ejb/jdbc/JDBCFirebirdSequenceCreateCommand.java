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
 * Jan Pechanec and Petr Kalina.
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ejb.CreateException;
import javax.sql.DataSource;

import org.jboss.deployment.DeploymentException;
import org.jboss.ejb.EntityEnterpriseContext;
import org.jboss.ejb.plugins.cmp.jdbc.JDBCInsertPKCreateCommand;
import org.jboss.ejb.plugins.cmp.jdbc.JDBCStoreManager;
import org.jboss.ejb.plugins.cmp.jdbc.JDBCUtil;
import org.jboss.ejb.plugins.cmp.jdbc.bridge.JDBCCMPFieldBridge;
import org.jboss.ejb.plugins.cmp.jdbc.metadata.JDBCEntityCommandMetaData;


/**
 * Create command for use with Firebird that uses a sequence (generator).
 * 
 * The sequence (generator) is called by the parameter attribute "sequence_name".
 * As an example, the sequence_name could be %%t_sequence to use %lt;table_name&gt;_sequence
 * for each distinct table.
 * 
 * <i>The code was derived from:
 *    <ul>
 *     <li>JBoss class org.jboss.ejb.plugins.cmp.jdbc.keygen.JDBCOracleSequenceCreateCommand
 *     <li>Class from Petr Kalina - http://www.jboss.com/index.html?module=bb&op=viewtopic&t=77075 
 *    </ul>
 * </i>
 * 
 * @author Jan Pechanec <jpechanec@orcz.cz>
 * @author Petr Kalina <petr.kalina@jlabs.cz>
 */
public class JDBCFirebirdSequenceCreateCommand extends JDBCInsertPKCreateCommand
{
    private String sequence_name;
    private String sequenceSQL;

    protected JDBCCMPFieldBridge pkField;
    
    /*private int pkIndex;
    private int jdbcType;*/

    public JDBCFirebirdSequenceCreateCommand() {
        super();
    }

    protected void initGeneratedFields() throws DeploymentException
    {
       super.initGeneratedFields();
       pkField = getGeneratedPKField();
    }

    public void init(JDBCStoreManager manager) throws DeploymentException
    {
       super.init(manager);
    }

    protected void initEntityCommand(JDBCEntityCommandMetaData entityCommand) throws DeploymentException
    {
       super.initEntityCommand(entityCommand);

       sequence_name = entityCommand.getAttribute("sequence_name");
       if (sequence_name == null) {
          throw new DeploymentException("sequence_name attribute must be specified inside <entity-command>");
       }

       String sequence_name_inst = replaceTable(sequence_name,entity.getTableName());
       
       sequenceSQL = "SELECT GEN_ID("+sequence_name_inst+",1) FROM RDB$DATABASE;";
       if (debug) {
          log.debug("SEQUENCE SQL is :"+sequenceSQL);
       }
    }
   
    /**
      * Replace %%t in the sql command with the current table name
      *
      * @param in sql statement with possible %%t to substitute with table name
      * @param table the table name
      * @return String with sql statement
      */
    private static String replaceTable(String in, String table)
    {
        int pos;

        pos = in.indexOf("%%t");
        // No %%t -> return input
        if(pos == -1)
            return in;

        String first = in.substring(0, pos);
        String last = in.substring(pos + 3);

        return first + table + last;
    }
   
    protected void generateFields(EntityEnterpriseContext ctx) throws CreateException
    {
       super.generateFields(ctx);

       Connection con = null;
       Statement s = null;
       ResultSet rs = null;
       try
       {
          if(debug)
          {
             log.debug("Executing SQL: " + sequenceSQL);
          }

          DataSource dataSource = entity.getDataSource();
          con = dataSource.getConnection();
          s = con.createStatement();

          rs = s.executeQuery(sequenceSQL);
          if(!rs.next())
          {
             throw new CreateException("Error fetching next primary key value: result set contains no rows");
          }
          pkField.loadInstanceResults(rs, 1, ctx);
       }
       catch(SQLException e)
       {
          log.error("Error fetching the next primary key value", e);
          throw new CreateException("Error fetching the next primary key value:" + e);
       }
       finally
       {
          JDBCUtil.safeClose(rs);
          JDBCUtil.safeClose(s);
          JDBCUtil.safeClose(con);
       }
    }

}
