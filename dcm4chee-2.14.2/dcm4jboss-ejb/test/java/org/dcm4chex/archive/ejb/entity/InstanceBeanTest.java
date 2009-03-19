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
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public class InstanceBeanTest extends ServletTestCase {

    public static final String AET = "MODALITY_AET";
    public static final String PID = "P-999999";
    public static final String PNAME = "Test^InstanceBean";
    public static final String SUID = "1.2.40.0.13.1.1.9999";
    public static final String sUID = "1.2.40.0.13.1.1.9999.1";
    public static final String CUID = "1.2.40.0.13.1.1.9999.2";
    public static final String UID_ = "1.2.40.0.13.1.1.9999.";
    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private PatientLocalHome patHome;
    private StudyLocalHome studyHome;
    private SeriesLocalHome seriesHome;
    private InstanceLocalHome instHome;
    private Object patPk;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(InstanceBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        Context ctx = new InitialContext();
        patHome = (PatientLocalHome) ctx.lookup("java:comp/env/ejb/Patient");
        studyHome = (StudyLocalHome) ctx.lookup("java:comp/env/ejb/Study");
        seriesHome = (SeriesLocalHome) ctx.lookup("java:comp/env/ejb/Series");
        instHome = (InstanceLocalHome) ctx.lookup("java:comp/env/ejb/Instance");
        ctx.close();
        Dataset ds = dof.newDataset();
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ds.putAE(PrivateTags.CallingAET, AET);
        ds.putLO(Tags.PatientID, PID);
        ds.putPN(Tags.PatientName, PNAME);
        ds.putUI(Tags.StudyInstanceUID, SUID);
        ds.putUI(Tags.SeriesInstanceUID, sUID);
        ds.putUI(Tags.SOPClassUID, CUID);
        PatientLocal pat = patHome.create(ds);
        patPk = pat.getPrimaryKey();
        StudyLocal study = studyHome.create(ds, pat);
        SeriesLocal series = seriesHome.create(ds, study);
        for (int i = 0; i < 5; ++i) {
            ds.putUI(Tags.SOPInstanceUID, UID_ + i);
            instHome.create(ds, series);
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        patHome.remove(patPk);
    }

    /**
     * Constructor for StudyBeanTest.
     * @param arg0
     */
    public InstanceBeanTest(String arg0) {
        super(arg0);
    }

    public void testFindByIuid() throws Exception {
        for (int i = 0; i < 5; i++) {
            InstanceLocal inst = instHome.findBySopIuid(UID_ + i);
        }
    }
}
