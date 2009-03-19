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

package org.dcm4chex.archive.ejb.entity;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.cactus.ServletTestCase;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.FileSystemStatus;
import org.dcm4chex.archive.ejb.interfaces.FileLocal;
import org.dcm4chex.archive.ejb.interfaces.FileLocalHome;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocal;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocalHome;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 6814 $ $Date: 2008-08-20 12:13:04 +0200 (Wed, 20 Aug 2008) $
 */
public class FileBeanTest extends ServletTestCase {
    public static final String RETRIEVE_AET = "QR_SCP";
    public static final String DIRPATH = "/var/local/archive";
    public static final String FS_GROUP_ID = "PRIMARY";
    public static final String FILEID = "2003/07/11/12345678/9ABCDEF0";
    public static final String TSUID = "1.2.40.0.13.1.1.9999.3";
    public static final int SIZE = 567890;
    public static final byte[] MD5 =
        { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

    private FileLocalHome fileHome;
    private FileSystemLocalHome fileSystemHome;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FileBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        Context ctx = new InitialContext();
        fileSystemHome = (FileSystemLocalHome) ctx.lookup("java:comp/env/ejb/FileSystem");
        fileHome = (FileLocalHome) ctx.lookup("java:comp/env/ejb/File");
        ctx.close();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {}

    /**
     * Constructor for StudyBeanTest.
     * @param arg0
     */
    public FileBeanTest(String arg0) {
        super(arg0);
    }

    public void testCreate() throws Exception {
        FileSystemDTO dto = new FileSystemDTO();
        dto.setDirectoryPath(DIRPATH);
        dto.setGroupID(FS_GROUP_ID);
        dto.setRetrieveAET(RETRIEVE_AET);
        dto.setAvailability(Availability.ONLINE);
        dto.setStatus(FileSystemStatus.DEF_RW);
        FileSystemLocal fs =
            fileSystemHome.create(dto);
        FileLocal file =
            fileHome.create(
                FILEID,
                TSUID,
                SIZE,
                MD5,
                0,
                null,
                fs);
        file.remove();
        fs.remove();
    }
}
