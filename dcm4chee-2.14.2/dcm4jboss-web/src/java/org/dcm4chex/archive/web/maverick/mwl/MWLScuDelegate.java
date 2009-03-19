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

package org.dcm4chex.archive.web.maverick.mwl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;
import org.dcm4che.data.Command;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer
 * @version $Revision: 3209 $ $Date: 2007-03-16 09:45:07 +0100 (Fri, 16 Mar 2007) $
 */
public class MWLScuDelegate {
	private static ObjectName mwlScuServiceName = null;
	private static ObjectName contentEditServiceName = null;

	private static MBeanServer server;

	private static Logger log = Logger
			.getLogger(MWLScuDelegate.class.getName());

	/**
	 * Iinitialize the MWLScu service delegator.
	 * <p>
	 * Set the name of the MwlScuService MBean with the servlet config param
	 * 'mwlScuServiceName'.
	 * 
	 * @param config
	 *            The ServletConfig object.
	 */
	public void init(ServletConfig config) {
		if (server != null)
			return;
		server = MBeanServerLocator.locate();
		String s = config.getInitParameter("mwlScuServiceName");
		try {
			mwlScuServiceName = new ObjectName(s);
			s = config.getInitParameter("contentEditName");
			contentEditServiceName = new ObjectName(s);
		} catch (Exception e) {
			log.error("Exception in init! ", e);
		}

	}

	public Logger getLogger() {
		return log;
	}

	/**
	 * Makes the MBean call to get the list of worklist entries for given filter
	 * (ds).
	 * 
	 * @param ds
	 * @return The list of worklist entries ( Each item in the list is a Dataset
	 *         of one scheduled procedure step).
	 */
	public List findMWLEntries(Dataset ds) {
		List resp = new ArrayList();
		try {
			server.invoke(mwlScuServiceName, "findMWLEntries",
					new Object[] { ds, resp },
					new String[] { Dataset.class.getName(), List.class.getName() });
		} catch (Exception x) {
			log.error("Exception occured in findMWLEntries: " + x.getMessage(),
					x);
		}
		return resp;
	}

	/**
	 * Checks if the MwlScpAET is local.
	 * <p>
	 * This means, that the MWLSCP is in the same container.
	 * <p>
	 * If it runs in the same container, the query can be done directly without
	 * a CFIND. Also we can allow deletion of MWLEntries.
	 * 
	 * @return true if the MWLSCP runs in the same container.
	 */
	public boolean isLocal() {
		try {
			Boolean b = (Boolean) server.getAttribute(mwlScuServiceName,
					"Local");
			return b.booleanValue();
		} catch (Exception x) {
			log.error("Exception occured in isLocal: " + x.getMessage(), x);
		}
		return false;
	}

	/**
	 * Deletes an MWL entry with given id.
	 * <p>
	 * This method should only be called if isLocal() returns true!
	 * 
	 * @param spsID
	 *            The ID of the MWLEntry (Scheduled Procedure Step ID)
	 * @return
	 */
	public boolean deleteMWLEntry(String spsID) {
		try {
			Object o = server.invoke(mwlScuServiceName, "deleteMWLEntry",
					new Object[] { spsID }, new String[] { String.class
							.getName() });
			return ((Boolean) o).booleanValue();
		} catch (Exception x) {
			log.error("Exception occured in deleteMWLEntry: " + x.getMessage(),
					x);
		}
		return false;
	}

    public Map linkMppsToMwl( String[] spsIDs, String[] mppsIUIDs ) {
        try {
            Map map = (Map) server.invoke(contentEditServiceName, "linkMppsToMwl",
                    new Object[] { spsIDs, mppsIUIDs }, 
                    new String[] { String[].class.getName(), String[].class.getName() });
            return map;
        } catch (Exception x) {
            log.error("Exception occured in linkMppsToMwl: " + x.getMessage(), x);
            return null;
        }
    }
    public Map linkMppsToMwl( Dataset[] mwlDs, String[] mppsIUIDs ) {
        try {
            Map map = (Map) server.invoke(contentEditServiceName, "linkMppsToMwl",
                    new Object[] { mwlDs, mppsIUIDs }, 
                    new String[] { Dataset[].class.getName(), String[].class.getName() });
            return map;
        } catch (Exception x) {
            log.error("Exception occured in linkMppsToMwl: " + x.getMessage(), x);
            return null;
        }
    }
}
