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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa-Gevaert AG.
 * Portions created by the Initial Developer are Copyright (C) 2002-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below.
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
package org.dcm4chex.archive.mbean;

import javax.management.Attribute;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.jdbc.QueryOldARRCmd;
import org.dcm4chex.archive.ejb.jdbc.QueryOldARRCmd.Record;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Sep 24, 2007
 * 
 */
public class MigrateOldAuditRecordRepositoryService
        extends ServiceMBeanSupport {

    private int emissionInterval;
    private int recordsByPass;
    private long lastEmittedPk;

	public final int getEmissionInterval() {
		return emissionInterval;
	}

	public final void setEmissionInterval(int emissionInterval) {
		if (emissionInterval < 0) {
			throw new IllegalArgumentException(
					"emissionInterval: " + emissionInterval);
		}
		this.emissionInterval = emissionInterval;
	}

	public final int getRecordsByPass() {
        return recordsByPass;
    }

    public final void setRecordsByPass(int recordByPass) {
		if (recordByPass < 0) {
			throw new IllegalArgumentException("recordByPass: " + recordByPass);
		}
        this.recordsByPass = recordByPass;
    }

    public final long getLastEmittedPk() {
        return lastEmittedPk;
    }
    
    public final void setLastEmittedPk(long lastEmittedPk) {
        this.lastEmittedPk = lastEmittedPk;
    }
    
    public String emitAuditRecords(int num) throws Exception {
        int count = 0;
        long ms0 = System.currentTimeMillis();
        while (count < num) {
            int emitted = emitBlockOfAuditRecords(
                    Math.min(num - count, recordsByPass));
            if (emitted == 0) {
                break;
            }
            count += emitted;
        }
        long ms = System.currentTimeMillis() - ms0;
        return "Sent " + count + " Audit messages in " + (ms / 1000.f) + " s.";
    }

    private int emitBlockOfAuditRecords(int limit) throws Exception {
        log.info("Prepare sending " + limit + " Audit messages");
        Record[] result = new Record[limit];
        QueryOldARRCmd cmd = new QueryOldARRCmd(lastEmittedPk, limit);
        int fetched = cmd.fetch(result);
        long lastEmittedPk = 0;
        int sent = 0;
        try {
            while (sent < fetched) {
                Record rec = result[sent];
                Logger.getLogger("auditlog").info(rec.xml_data);
                lastEmittedPk = rec.pk;
                ++sent;
                Thread.sleep(emissionInterval);
            }
            return sent;
        } finally {
            if (sent > 0) {
            	log.info("Sent " + sent + " Audit messages");
                server.setAttribute(serviceName, new Attribute(
                        "LastEmittedPk", new Long(lastEmittedPk)));
            }
        }
    }
}
