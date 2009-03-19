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

package org.dcm4chex.archive.ejb.interfaces;

import java.io.Serializable;
import java.util.Date;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2006-06-28 18:14:25 +0200 (Wed, 28 Jun 2006) $
 * @since 14.12.2004
 */

public class MediaDTO implements Serializable {

    private static final long serialVersionUID = 3545516192564721461L;

    public static final int OPEN = 0;
    public static final int QUEUED = 1;
    public static final int TRANSFERING = 2;
    public static final int BURNING = 3;
    public static final int COMPLETED = 4;
    public static final int ERROR = 999;

    private long pk;
    private String filesetId;
    private String filesetIuid;
    private String mediaCreationRequestIuid;
    private int mediaStatus;
    private String mediaStatusInfo;
    private long mediaUsage;
    private long createdTime;
    private long updatedTime;
    private boolean instancesAvailable;

    public final long getPk() {
        return pk;
    }

    public final void setPk(long pk) {
        this.pk = pk;
    }

    public final Date getCreatedTime() {
        return new Date(createdTime);
    }

    public final void setCreatedTime(Date time) {
        this.createdTime = time.getTime();
    }

    public final Date getUpdatedTime() {
        return new Date(updatedTime);
    }

    public final void setUpdatedTime(Date time) {
        this.updatedTime = time.getTime();
    }

    public final String getFilesetId() {
        return filesetId;
    }

    public final void setFilesetId(String filesetId) {
        this.filesetId = filesetId;
    }

    public final String getFilesetIuid() {
        return filesetIuid;
    }

    public final void setFilesetIuid(String filesetIuid) {
        this.filesetIuid = filesetIuid;
    }

    public final String getMediaCreationRequestIuid() {
        return mediaCreationRequestIuid;
    }
    
    public final void setMediaCreationRequestIuid(
            String mediaCreationRequestIuid) {
        this.mediaCreationRequestIuid = mediaCreationRequestIuid;
    }
    
    public final long getMediaUsage() {
        return mediaUsage;
    }

    public final void setMediaUsage(long mediaUsage) {
        this.mediaUsage = mediaUsage;
    }
    
    public final int getMediaStatus() {
        return mediaStatus;
    }

    public final void setMediaStatus(int mediaStatus) {
        this.mediaStatus = mediaStatus;
    }
    
    public final String getMediaStatusInfo() {
        return mediaStatusInfo;
    }
    
    public final void setMediaStatusInfo(String info) {
        this.mediaStatusInfo = info;
    }
    
	/**
	 * @return Returns the instanceAvailable.
	 */
	public boolean getInstancesAvailable() {
		return instancesAvailable;
	}
	/**
	 * @param instanceAvailable The instanceAvailable to set.
	 */
	public void setInstancesAvailable(boolean instanceAvailable) {
		this.instancesAvailable = instanceAvailable;
	}
}
