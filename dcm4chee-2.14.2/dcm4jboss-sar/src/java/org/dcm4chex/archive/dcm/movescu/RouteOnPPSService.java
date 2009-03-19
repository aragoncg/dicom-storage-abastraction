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


package org.dcm4chex.archive.dcm.movescu;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.config.DicomPriority;
import org.dcm4chex.archive.dcm.gpwlscp.GPWLScpService;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3228 $ $Date: 2007-03-28 12:21:35 +0200 (Wed, 28 Mar 2007) $
 * @since Dec 19, 2006
 */
public class RouteOnPPSService extends ServiceMBeanSupport
implements NotificationListener {

	private static final String DELIMS = ":;\n\r\t";
	private static final String NONE = "NONE";
    private int routingPriority = 0;
	private ObjectName gpwlScpServiceName;
    private ObjectName moveScuServiceName;
	private LinkedHashMap code2aets = new LinkedHashMap();

	public final String getRoutingRules() {
		if (code2aets.isEmpty())
			return NONE;
		StringBuffer sb = new StringBuffer();
		for (Iterator iter = code2aets.entrySet().iterator(); iter.hasNext();) {
			Map.Entry e = (Map.Entry) iter.next();
			sb.append(e.getKey()).append(':').append(e.getValue()).append(System.getProperty("line.separator", "\n"));
		}
		return sb.toString();
	}

	public final void setRoutingRules(String str) {
		code2aets.clear();
		if (str.equalsIgnoreCase(NONE))
			return;
		StringTokenizer strtk = new StringTokenizer(str, DELIMS);
		String tk;
		String key = null;
		while (strtk.hasMoreTokens()) {
			tk = strtk.nextToken();
			if (key != null) {
				code2aets.put(key, tk);
				key = null;
			} else {
				key = tk;
			}			
		}		
	}

    public final String getRoutingPriority() {
        return DicomPriority.toString(routingPriority);
    }

    public final void setRoutingPriority(String forwardPriority) {
        this.routingPriority = DicomPriority.toCode(forwardPriority);
    }
    
	public final ObjectName getGpwlScpServiceName() {
		return gpwlScpServiceName;
	}

	public final void setGpwlScpServiceName(ObjectName serviceName) {
		this.gpwlScpServiceName = serviceName;
	}
    
    public final ObjectName getMoveScuServiceName() {
        return moveScuServiceName;
    }

    public final void setMoveScuServiceName(ObjectName moveScuServiceName) {
        this.moveScuServiceName = moveScuServiceName;
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
        String patId = pps.getString(Tags.PatientID);
		Dataset codeItem = pps.getItem(Tags.PerformedWorkitemCodeSeq);
		if (codeItem == null)
			return;
				
		String key = "" + codeItem.getString(Tags.CodeValue) 
				+ '^' + codeItem.getString(Tags.CodingSchemeDesignator);
		String aets = (String) code2aets.get(key);
		if (aets == null) {
			log.debug("No Routing Rule for Performed Work Item Code: " + key);
			return;
		}
        log.info("Routing triggered by PPS work item code:"+key);    
		Dataset rqItem = pps.getItem(Tags.RefRequestSeq);
		if (rqItem == null) {
			log.warn("Missing Ref.Request Item - Routing abandoned");
			return;
		}
		String suid = rqItem.getString(Tags.StudyInstanceUID);
		if (suid == null) {
			log.warn("Missing Study Instance UID in Ref.Request Item - Routing abandoned");
			return;
		}

        String aet;
        for ( StringTokenizer st = new StringTokenizer(aets,",") ; st.hasMoreTokens() ; ) {
            aet = st.nextToken();
            scheduleMove(null, aet, patId, suid);
            log.info("Routing of Study[iuid=" + suid + "] to " + aet);
        }
	}
    
    private void scheduleMove(String retrieveAET, String destAET, String patId, String studyIUID) {
        try {
            server.invoke(moveScuServiceName, "scheduleMove", new Object[] {
                    retrieveAET, destAET, new Integer(this.routingPriority), patId,
                    studyIUID, null, null, new Long(-1l) },
                    new String[] { String.class.getName(),
                            String.class.getName(), int.class.getName(),
                            String.class.getName(), String.class.getName(),
                            String.class.getName(), String[].class.getName(),
                            long.class.getName() });
        } catch (Exception e) {
            log.error("Schedule Move failed:", e);
        }
    }
    
    public void testRouting(String code, String studyUID) {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        Dataset codeItem = ds.putSQ(Tags.PerformedWorkitemCodeSeq).addNewItem();
        int pos = code.indexOf('^');
        codeItem.putSH(Tags.CodeValue, pos==-1 ? code : code.substring(0,pos));
        if ( pos != -1 ) codeItem.putSH(Tags.CodingSchemeDesignator, code.substring(++pos));
        Dataset item = ds.putSQ(Tags.RefRequestSeq).addNewItem();
        item.putUI(Tags.StudyInstanceUID, studyUID);
        Notification notif = new Notification("testRouting", this, System.currentTimeMillis());
        notif.setUserData(ds);
        this.handleNotification(notif, null);
    }
    
}
