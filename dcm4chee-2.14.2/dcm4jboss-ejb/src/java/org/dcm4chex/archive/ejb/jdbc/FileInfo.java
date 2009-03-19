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

package org.dcm4chex.archive.ejb.jdbc;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger </a>
 *  
 */
public class FileInfo implements Serializable {

    private static final long serialVersionUID = 48244042710891993L;

    public static final Comparator ASC_ORDER = new Comparator() {

        public int compare(Object o1, Object o2) {
           	long l1 = ((FileInfo) o1).pk;
           	long l2 = ((FileInfo) o2).pk;
           	return l1 == l2 ? 0 : l1 < l2 ? -1 : 1;
        }
    };

    public static final Comparator DESC_ORDER = new Comparator() {

        public int compare(Object o1, Object o2) {
           	long l1 = ((FileInfo) o1).pk;
           	long l2 = ((FileInfo) o2).pk;
           	return l1 == l2 ? 0 : l2 < l1 ? -1 : 1;
        }
    };

    public long pk = -1;

    public byte[] patAttrs = null;

    public byte[] studyAttrs = null;

    public byte[] seriesAttrs = null;

    public byte[] instAttrs = null;

    public String patID = null;

    public String patName = null;

    public String studyIUID = null;

    public String seriesIUID = null;

    public String sopIUID = null;

    public String sopCUID = null;

    public String extRetrieveAET = null;
    
    public String fileRetrieveAET = null;

    public String basedir = null;

    public String fileID = null;

    public String tsUID = null;

    public String md5 = null;

    public long size = 0;

    public int status = 0;
    
    public int availability = 0;
    
    public FileInfo()
    {    	
    }

    public FileInfo(long pk, String patID, String patName, byte[] patAttrs,
            String studyIUID, String seriesIUID, byte[] studyAttrs, byte[] seriesAttrs,
            byte[] instAttrs, String sopIUID, String sopCUID,
            String extRetrieveAET, String fileRetrieveAET, int availability,
            String basedir, String fileID, String tsUID, String md5, long size,
            int status) {
        this.pk = pk;
        this.patID = patID;
        this.patName = patName;
        this.patAttrs = patAttrs;
        this.studyIUID = studyIUID;
        this.seriesIUID = seriesIUID;
        this.studyAttrs = studyAttrs;
        this.seriesAttrs = seriesAttrs;
        this.instAttrs = instAttrs;
        this.sopIUID = sopIUID;
        this.sopCUID = sopCUID;
        this.extRetrieveAET = extRetrieveAET;
        this.fileRetrieveAET = fileRetrieveAET;
        this.availability = availability;
        this.basedir = basedir;
        this.fileID = fileID;
        this.tsUID = tsUID;
        this.md5 = md5;
        this.size = size;
        this.status = status;
    }

    public String toString() {
        return "FileInfo[pk=" + pk + "iuid=" + sopIUID + ", cuid=" + sopCUID
                + ", extRetrieveAET=" + extRetrieveAET
                + ", fileRetrieveAET=" + fileRetrieveAET + ", basedir="
                + basedir + ", fileid=" + fileID + ", tsuid=" + tsUID;
    }
}
