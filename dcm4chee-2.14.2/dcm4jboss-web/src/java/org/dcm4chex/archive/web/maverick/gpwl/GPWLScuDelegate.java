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

package org.dcm4chex.archive.web.maverick.gpwl;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer
 * @version $Revision: 2417 $ $Date: 2006-04-10 17:18:10 +0200 (Mon, 10 Apr 2006) $
 */
public class GPWLScuDelegate {
	private static ObjectName gpwlScuServiceName = null;

	private static MBeanServer server;

	private static Logger log = Logger
			.getLogger(GPWLScuDelegate.class.getName());

	/**
	 * Iinitialize the GPWLScu service delegator.
	 * <p>
	 * Set the name of the GPWLScuService MBean with the servlet config param
	 * 'gpwlScuServiceName'.
	 * 
	 * @param config
	 *            The ServletConfig object.
	 */
	public void init(ServletConfig config) {
		if (server != null)
			return;
		server = MBeanServerLocator.locate();
		String s = config.getInitParameter("gpwlScuServiceName");
		try {
			gpwlScuServiceName = new ObjectName(s);
			s = config.getInitParameter("mppsScpServiceName");
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
	public List findGPWLEntries(Dataset ds) {
		List resp = null;
		try {
			Object o = server.invoke(gpwlScuServiceName, "findGPWLEntries",
					new Object[] { ds },
					new String[] { Dataset.class.getName() });
			return (List) o;
		} catch (Exception x) {
			log.error("Exception occured in findGPWLEntries: " + x.getMessage(),
					x);
		}
		return new ArrayList();
	}

	/**
	 * Checks if the GPWLScpAET is local.
	 * <p>
	 * This means, that the GPWLSCP is in the same container.
	 * <p>
	 * If it runs in the same container, the query can be done directly without
	 * a CFIND. Also we can allow deletion of GPWLEntries.
	 * 
	 * @return true if the GPWLSCP runs in the same container.
	 */
	public boolean isLocal() {
		try {
			Boolean b = (Boolean) server.getAttribute(gpwlScuServiceName,
					"Local");
			return b.booleanValue();
		} catch (Exception x) {
			log.error("Exception occured in isLocal: " + x.getMessage(), x);
		}
		return false;
	}

	/**
	 * Deletes an GPWL entry with given id.
	 * <p>
	 * This method should only be called if isLocal() returns true!
	 * 
	 * @param gpspsID
	 *            The ID of the GPWLEntry (General Purpose Scheduled Procedure Step Instance UID)
	 * @return
	 */
	public boolean deleteGPWLEntry(String gpspsID) {
		try {
			Object o = server.invoke(gpwlScuServiceName, "deleteGPWLEntry",
					new Object[] { gpspsID }, new String[] { String.class
							.getName() });
			return ((Boolean) o).booleanValue();
		} catch (Exception x) {
			log.error("Exception occured in deleteGPWLEntry: " + x.getMessage(),
					x);
		}
		return false;
	}

	
}
