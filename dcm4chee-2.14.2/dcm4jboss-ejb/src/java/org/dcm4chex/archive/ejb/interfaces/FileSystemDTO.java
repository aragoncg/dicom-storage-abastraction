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

import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.FileSystemStatus;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6814 $ $Date: 2008-08-20 12:13:04 +0200 (Wed, 20 Aug 2008) $
 * @since 13.09.2004
 * 
 */
public class FileSystemDTO implements Serializable {

    private static final long serialVersionUID = -1301228596642462447L;

    private long pk = -1; // unknown mark

    private String directoryPath;

    private String groupID;

    private String retrieveAET;

    private int availability;

    private int status;

    private String userInfo;

    private String next;

    public StringBuffer toString(StringBuffer sb) {
        sb.append("FileSystem[pk=").append(pk);
        sb.append(", ").append(directoryPath);
        sb.append(", groupID=").append(groupID);
        sb.append(", aet=").append(retrieveAET);
        sb.append(", ").append(Availability.toString(availability));
        sb.append(", ").append(FileSystemStatus.toString(status));
        sb.append(", userinfo=").append(userInfo);
        if (next != null)
            sb.append(", next=").append(next);
        sb.append("]");
        return sb;
    }

    public String toString() {
        return toString(new StringBuffer()).toString();
    }

    public final long getPk() {
        return pk;
    }

    public final void setPk(long pk) {
        this.pk = pk;
    }

    public final String getDirectoryPath() {
        return directoryPath;
    }

    public final void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public final String getGroupID() {
        return groupID;
    }

    public final void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public final String getRetrieveAET() {
        return retrieveAET;
    }

    public final void setRetrieveAET(String retrieveAET) {
        this.retrieveAET = retrieveAET;
    }

    public final int getAvailability() {
        return availability;
    }

    public final void setAvailability(int availability) {
        this.availability = availability;
    }

    public final int getStatus() {
        return status;
    }

    public final void setStatus(int status) {
        this.status = status;
    }

    public final String getUserInfo() {
        return userInfo;
    }

    public final void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public final String getNext() {
        return next;
    }

    public final void setNext(String next) {
        this.next = next;
    }

}