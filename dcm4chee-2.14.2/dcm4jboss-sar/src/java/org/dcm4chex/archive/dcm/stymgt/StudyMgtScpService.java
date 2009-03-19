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

package org.dcm4chex.archive.dcm.stymgt;

import javax.management.Notification;
import javax.management.NotificationFilter;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.UIDs;
import org.dcm4che.net.AcceptorPolicy;
import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.DcmServiceRegistry;
import org.dcm4chex.archive.dcm.AbstractScpService;

public class StudyMgtScpService extends AbstractScpService {

    public static final String EVENT_TYPE = "org.dcm4chex.archive.dcm.stymgt";

    public static final NotificationFilter NOTIF_FILTER = new NotificationFilter() {

		private static final long serialVersionUID = 3257281448414097465L;

		public boolean isNotificationEnabled(Notification notif) {
            return EVENT_TYPE.equals(notif.getType());
        }
    };

    private StudyMgtScp stymgtScp = new StudyMgtScp(this);
    
	/**
	 * @return Returns the ignoreDeleteFailed.
	 */
	public boolean isIgnoreDeleteFailed() {
		return stymgtScp.isIgnoreDeleteFailed();
	}
	/**
	 * @param ignoreDeleteFailed The ignoreDeleteFailed to set.
	 */
	public void setIgnoreDeleteFailed(boolean ignoreDeleteFailed) {
		stymgtScp.setIgnoreDeleteFailed( ignoreDeleteFailed );
	}

	protected void bindDcmServices(DcmServiceRegistry services) {
        services.bind(UIDs.Dcm4cheStudyManagement, stymgtScp);
	}

	protected void unbindDcmServices(DcmServiceRegistry services) {
		services.unbind(UIDs.Dcm4cheStudyManagement);		
	}

    protected void enablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.Dcm4cheStudyManagement,
                valuesToStringArray(tsuidMap));
    }

    protected void disablePresContexts(AcceptorPolicy policy) {
        policy.putPresContext(UIDs.Dcm4cheStudyManagement, null);
    }

    void sendStudyMgtNotification(ActiveAssociation assoc, int cmdField,
			int actionTypeID, String iuid, Dataset ds) {
		Association a = assoc.getAssociation();
		long eventID = super.getNextNotificationSequenceNumber();
		Notification notif = new Notification(EVENT_TYPE, this, eventID);
		notif.setUserData(new StudyMgtOrder(a.getCallingAET(), a
				.getCalledAET(), cmdField, actionTypeID, iuid, ds));
		super.sendNotification(notif);
	}

}
