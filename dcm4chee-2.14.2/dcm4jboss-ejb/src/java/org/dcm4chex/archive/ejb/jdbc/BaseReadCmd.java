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

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * 
 */
public abstract class BaseReadCmd extends BaseCmd {
    protected ResultSet rs = null;

    protected BaseReadCmd(String dsJndiName, int transactionIsolationLevel,
            int resultSetType)
            throws SQLException {
        super(dsJndiName, transactionIsolationLevel, null, resultSetType);
    }

    protected BaseReadCmd(String dsJndiName, int transactionIsolationLevel)
    throws SQLException {
        super(dsJndiName, transactionIsolationLevel, null);
    }

    protected BaseReadCmd(String dsJndiName, int transactionIsolationLevel,
            String sql) throws SQLException {
        super(dsJndiName, transactionIsolationLevel, sql);
    }

    public void execute(String sql) throws SQLException {
        if (rs != null) {
            throw new IllegalStateException();
        }
        log.debug("SQL: " + sql);

        Exception lastException = null;
        for (int i = 0; i < updateDatabaseMaxRetries; i++) {
            try {
                rs = stmt.executeQuery(sql);

                // Success
                if (i > 0)
                    log
                            .info("execute sql successfully after retry: "
                                    + (i + 1));

                return;
            } catch (Exception e) {
                if (lastException == null
                        || !lastException.getMessage().equals(e.getMessage()))
                    log.warn("failed to execute sql: " + sql + " - retry: "
                            + (i + 1) + " of " + updateDatabaseMaxRetries, e);
                else
                    log.warn("failed to execute sql: " + sql
                            + ", got the same exception as above - retry: "
                            + (i + 1) + " of " + updateDatabaseMaxRetries);
                lastException = e;

                close();

                try {
                    Thread.sleep(updateDatabaseRetryInterval);
                } catch (InterruptedException e1) {
                    log.warn(e1);
                }

                try {
                    open();
                } catch (SQLException e1) {
                }
            }
        }
        throw new SQLException(
                "give up executing SQL statement after all retries: " + sql);
    }

    public void execute() throws SQLException {
        if (rs != null) {
            throw new IllegalStateException();
        }

        Exception lastException = null;
        for (int i = 0; i < updateDatabaseMaxRetries; i++) {
            try {
                rs = ((PreparedStatement) stmt).executeQuery();

                // Success
                if (i > 0)
                    log
                            .info("execute sql successfully after retry: "
                                    + (i + 1));

                return;
            } catch (Exception e) {
                if (lastException == null
                        || !lastException.getMessage().equals(e.getMessage()))
                    log.warn("failed to execute sql: " + sql + " - retry: "
                            + (i + 1) + " of " + updateDatabaseMaxRetries, e);
                else
                    log.warn("failed to execute sql: " + sql
                            + ", got the same exception as above - retry: "
                            + (i + 1) + " of " + updateDatabaseMaxRetries);
                lastException = e;

                close();

                try {
                    Thread.sleep(updateDatabaseRetryInterval);
                } catch (InterruptedException e1) {
                    log.warn(e1);
                }

                try {
                    open();
                } catch (SQLException e1) {
                }
            }
        }
        throw new SQLException(
                "give up executing SQL statement after all retries: " + sql);
    }

    public boolean next() throws SQLException {
        if (rs == null) {
            throw new IllegalStateException();
        }
        return rs.next();
    }

    public void close() {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignore) {
            }
            rs = null;
        }
        super.close();
    }
}
