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


package org.dcm4chex.archive.mbean;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.dcm.gpwlscp.GPWLScpService;
import org.dcm4chex.archive.ejb.interfaces.StudyMgt;
import org.dcm4chex.archive.ejb.interfaces.StudyMgtHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3228 $ $Date: 2007-03-28 12:21:35 +0200 (Wed, 28 Mar 2007) $
 * @since Dec 12, 2005
 */
public class StudyStatusService extends ServiceMBeanSupport
implements NotificationListener {

	private static final String DELIMS = ":=;,\n\r\t";
	private static final String NONE = "NONE";
	private ObjectName gpwlScpServiceName;
	private LinkedHashMap code2status = new LinkedHashMap();

	public final String getStatusUpdateRules() {
		if (code2status.isEmpty())
			return NONE;
		StringBuffer sb = new StringBuffer();
		for (Iterator iter = code2status.entrySet().iterator(); iter.hasNext();) {
			Map.Entry e = (Map.Entry) iter.next();
			sb.append(e.getKey()).append(':').append(e.getValue()).append(System.getProperty("line.separator", "\n"));
		}
		return sb.toString();
	}

	public final void setStatusUpdateRules(String str) {
		code2status.clear();
		if (str.equalsIgnoreCase(NONE))
			return;
		StringTokenizer strtk = new StringTokenizer(str, DELIMS);
		String tk;
		String key = null;
		while (strtk.hasMoreTokens())
		{
			tk = strtk.nextToken();
			if (key != null) {
				code2status.put(key, tk);
				key = null;
			} else {
				key = tk;
			}			
		}		
	}

	public final ObjectName getGpwlScpServiceName() {
		return gpwlScpServiceName;
	}

	public final void setGpwlScpServiceName(ObjectName serviceName) {
		this.gpwlScpServiceName = serviceName;
	}

	protected void startService() throws Exception {
		server.addNotificationListener(gpwlScpServiceName, this,
				GPWLScpService.ON_PPS_NOTIF_FILTER, null);
	}

	protected void stopService() throws Exception {
		server.removeNotificationListener(gpwlScpServiceName, this,
                GPWLScpService.ON_PPS_NOTIF_FILTER, null);
	}

	public void handleNotification(Notification notif, Object handback) {
		Dataset pps = (Dataset) notif.getUserData();
		Dataset codeItem = pps.getItem(Tags.PerformedWorkitemCodeSeq);
		if (codeItem == null)
			return;
				
		String key = "" + codeItem.getString(Tags.CodeValue) 
				+ '^' + codeItem.getString(Tags.CodingSchemeDesignator);
		String status = (String) code2status .get(key);
		if (status == null)
		{
			if (log.isDebugEnabled())
				log.debug("No Status Update Rule for Performed Work Item Code: " + key);
			return;
		}
		
		Dataset rqItem = pps.getItem(Tags.RefRequestSeq);
		if (rqItem == null)
		{
			log.debug("Missing Ref.Request Item - No Status Update");
			return;
		}
		String suid = rqItem.getString(Tags.StudyInstanceUID);
		if (suid == null)
		{
			log.debug("Missing Study Instance UID in Ref.Request Item - No Status Update");
			return;
		}

        try {
			StudyMgtHome home = (StudyMgtHome) EJBHomeFactory.getFactory()
					.lookup(StudyMgtHome.class, StudyMgtHome.JNDI_NAME);
			StudyMgt mgt = home.create();
			mgt.updateStudyStatusId(suid, status);
			log.info("Updated Status ID of Study[iuid=" + suid + "] to " + status);
		} catch (Exception e) {
			log.error("Failed to update Status ID of Study[iuid=" + suid + "]:", e);
		}
	}
}
