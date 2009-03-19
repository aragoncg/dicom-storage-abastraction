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

import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.dcm.mppsscp.MPPSScpService;
import org.dcm4chex.archive.ejb.interfaces.PrivateManager;
import org.dcm4chex.archive.ejb.interfaces.PrivateManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2836 $ $Date: 2006-10-09 16:31:41 +0200 (Mon, 09 Oct 2006) $
 * @since Dec 7, 2005
 */
public class PPSExceptionMgtService extends ServiceMBeanSupport
implements NotificationListener, MessageListener {

	/** Name of the JMS queue to 'serialize' SeriesStored and MPPSReceived Notifictions. */ 
    private String queueName;
	
	private ObjectName mppsScpServiceName;
	private PrivateManager privateManager;

	private long delay = 0;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
    }
        
        public final String getQueueName() {
            return queueName;
        }
        
        public final void setQueueName(String queueName) {
            this.queueName = queueName;
        }
            
	/**
	 * @return Returns the delay.
	 */
	public long getDelay() {
		return delay;
	}
	/**
	 * @param delay The delay to set.
	 */
	public void setDelay(long delay) {
		this.delay = delay;
	}
	public final ObjectName getMppsScpServiceName() {

		return mppsScpServiceName;
	}

	public final void setMppsScpServiceName(ObjectName mppsScpServiceName) {
		this.mppsScpServiceName = mppsScpServiceName;
	}
	
	protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, 1);
		server.addNotificationListener(mppsScpServiceName, this,
				MPPSScpService.NOTIF_FILTER, null);
	}

	protected void stopService() throws Exception {
		server.removeNotificationListener(mppsScpServiceName, this,
				MPPSScpService.NOTIF_FILTER, null);
		jmsDelegate.stopListening(queueName);
	}

	public void handleNotification(Notification notif, Object handback) {

		Dataset mppsDS = null;
        try {
	        if ( notif.getType().equals(MPPSScpService.EVENT_TYPE_MPPS_RECEIVED) ) {
	        	mppsDS = (Dataset)notif.getUserData();
	        	if ( log.isDebugEnabled() ) {
	        		log.debug("MPPS received. mpps:");log.debug(mppsDS);
	        	}
	        	Dataset item = mppsDS.getItem(Tags.PPSDiscontinuationReasonCodeSeq);
	        	if ( item != null && "110514".equals(item.getString(Tags.CodeValue)) && 
	        		 "DCM".equals(item.getString(Tags.CodingSchemeDesignator))) {
	        			String mppsIUID = mppsDS.getString( Tags.SOPInstanceUID );
		    			log.debug("Scheduled: Move discontinued Series (IncorrectWorklistEntry) to trash folder! mppsIuid:"+mppsIUID);
		                jmsDelegate.queue(queueName, mppsIUID, Message.DEFAULT_PRIORITY, System.currentTimeMillis()+getDelay());
	        	}
	        }
		} catch (Exception e) {
			log.error("Can not schedule: Move discontinued Series (IncorrectWorklistEntry) to trash folder!", e);
		}
	}
	
    private PrivateManager lookupPrivateManager() throws HomeFactoryException, RemoteException, CreateException {
    	if ( privateManager != null ) return privateManager;
    	privateManager = ((PrivateManagerHome) EJBHomeFactory.getFactory().lookup(
        		PrivateManagerHome.class, PrivateManagerHome.JNDI_NAME)).create();
        return privateManager;
    }

	public void onMessage(Message message) {
        try {
			String mppsIUID = (String)((ObjectMessage) message).getObject();
			log.info("Move discontinued Series (IncorrectWorklistEntry) to trash folder! mppsIuid:"+mppsIUID);
			lookupPrivateManager().moveSeriesOfPPSToTrash(mppsIUID, true);
		} catch (Exception e) {
			getLog().error("Can not move Series with MPPS 'IncorrectWorklistEntry' to trash folder!", e);
		}
	}
}
