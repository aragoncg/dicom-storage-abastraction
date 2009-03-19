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

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 6920 $ $Date: 2006-06-28 18:14:25 +0200 (Wed, 28 Jun
 *          2006) $
 * @since 20.02.2004
 */
public final class FileDTO implements Serializable {

    private static final long serialVersionUID = 3073146245744171178L;

    private long pk;

    private long fspk;

    private String aet;

    private String userInfo;

    private String basedir;

    private String fsgrid;

    private String path;

    private String tsuid;

    private String sopCuid;

    private long size;

    private byte[] md5;

    private int status;

    private int availability;

    public String toString() {
        return "FileDTO[pk=" + pk + ", basedir=" + basedir + ", fsgrid="
                + fsgrid + ", path=" + path + ", status=" + status + "]";
    }

    /**
     * @return Returns the pk.
     */
    public final long getPk() {
        return pk;
    }

    /**
     * @param pk
     *            The pk to set.
     */
    public final void setPk(long pk) {
        this.pk = pk;
    }

    /**
     * @return Returns the aet.
     */
    public final String getRetrieveAET() {
        return aet;
    }

    /**
     * @param aets
     *            The aets to set.
     */
    public final void setRetrieveAET(String aet) {
        this.aet = aet;
    }

    public final String getUserInfo() {
        return userInfo;
    }

    public final void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public final long getFileSystemPk() {
        return fspk;
    }

    public final void setFileSystemPk(long fspk) {
        this.fspk = fspk;
    }

    public final String getFileSystemGroupID() {
        return fsgrid;
    }

    public final void setFileSystemGroupID(String fsgrid) {
        this.fsgrid = fsgrid;
    }

    public final String getDirectoryPath() {
        return basedir;
    }

    public final void setDirectoryPath(String baseDir) {
        this.basedir = baseDir;
    }

    public final String getMd5String() {
        return MD5.toString(md5);
    }

    /**
     * @return Returns the md5.
     */
    public final byte[] getFileMd5() {
        return md5;
    }

    /**
     * @param md5
     *            The md5 to set.
     */
    public final void setFileMd5(byte[] md5) {
        this.md5 = md5;
    }

    public final int getFileStatus() {
        return status;
    }

    public final void setFileStatus(int status) {
        this.status = status;
    }

    /**
     * @return Returns the path.
     */
    public final String getFilePath() {
        return path;
    }

    /**
     * @param path
     *            The path to set.
     */
    public final void setFilePath(String path) {
        this.path = path;
    }

    /**
     * @return Returns the size.
     */
    public final long getFileSize() {
        return size;
    }

    /**
     * @param size
     *            The size to set.
     */
    public final void setFileSize(long size) {
        this.size = size;
    }

    /**
     * @return Returns the tsuid.
     */
    public final String getFileTsuid() {
        return tsuid;
    }

    /**
     * @param tsuid
     *            The tsuid to set.
     */
    public final void setFileTsuid(String tsuid) {
        this.tsuid = tsuid;
    }

    public final int getAvailability() {
        return availability;
    }

    public final void setAvailability(int availability) {
        this.availability = availability;
    }

    /**
     * @return Returns the sopClassUID.
     */
    public String getSopClassUID() {
        return sopCuid;
    }

    /**
     * @param sopClassUID
     *            The sopClassUID to set.
     */
    public void setSopClassUID(String sopClassUID) {
        this.sopCuid = sopClassUID;
    }
}