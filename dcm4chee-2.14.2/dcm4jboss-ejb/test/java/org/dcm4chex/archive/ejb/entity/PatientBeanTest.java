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

import java.util.Collection;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.cactus.ServletTestCase;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.ejb.interfaces.PatientLocal;
import org.dcm4chex.archive.ejb.interfaces.PatientLocalHome;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public class PatientBeanTest extends ServletTestCase {

    private static final String PID_ = "P-9999";
    private static final String[] PAT_NAME =
        { "Marinescu^Floyd", "Cavaness^Chuck", "Keeton^Brian", };
    private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();

    private PatientLocalHome patHome;
    private Object[] pk = new Object[PAT_NAME.length];

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PatientBeanTest.class);
    }

    public static Test suite()
    {
        return new TestSuite(PatientBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        Context ctx = new InitialContext();
        patHome = (PatientLocalHome) ctx.lookup("java:comp/env/ejb/Patient");
        ctx.close();
        for (int i = 0; i < PAT_NAME.length; i++) {
            Dataset ds = dof.newDataset();
            ds.putLO(Tags.PatientID, PID_ + i);
            ds.putPN(Tags.PatientName, PAT_NAME[i]);
            PatientLocal p = patHome.create(ds);
            pk[i] = p.getPrimaryKey();
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        for (int i = 0; i < pk.length; i++) {
            patHome.remove(pk[i]);
        }
    }

    /**
     * Constructor for PatientBeanTest.
     * @param arg0
     */
    public PatientBeanTest(String arg0) {
        super(arg0);
    }

    public void testFindByPatientId() throws Exception {
        for (int i = 0; i < PAT_NAME.length; i++) {
            Collection c = patHome.findByPatientId(PID_ + i);
            assertEquals(1, c.size());
            PatientLocal pat = (PatientLocal) c.iterator().next();
            String pn = pat.getPatientName();
            if (!PAT_NAME[i].equals(pn)) {
                fail("expected:" + PAT_NAME[i] + ", value:" + pn);
            }
        }
    }
}
