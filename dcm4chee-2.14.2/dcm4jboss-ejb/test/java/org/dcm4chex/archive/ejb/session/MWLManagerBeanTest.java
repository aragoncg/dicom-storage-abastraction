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

package org.dcm4chex.archive.ejb.session;

import java.io.FileInputStream;
import java.io.IOException;

import javax.naming.Context;
import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.xml.sax.SAXException;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 3287 $ $Date: 2007-04-19 01:25:26 +0200 (Thu, 19 Apr 2007) $
 */
public class MWLManagerBeanTest extends TestCase
{

    public static final String FILE = "mwlitem.xml";

    private MWLManager mwlManager;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(MWLManagerBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Context ctx = new InitialContext();
        MWLManagerHome home = (MWLManagerHome) ctx.lookup(MWLManagerHome.JNDI_NAME);
        ctx.close();
        mwlManager = home.create();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        mwlManager.remove();
    }

    /**
     * Constructor for StudyBeanTest.
     * @param name
     */
    public MWLManagerBeanTest(String name)
    {
        super(name);
    }

    public void testAddAndRemoveWorklistItem() throws Exception
    {
        Dataset ds = loadMWLItemFromFile();
        Dataset spsItem = ds.getItem(Tags.SPSSeq);
        String spsId1ds = spsItem.getString(Tags.SPSID);
        // insert first entry with sps-id
        Dataset mwlItem1 = mwlManager.addWorklistItem(ds);
        String rpId1ret = mwlItem1.getString(Tags.RequestedProcedureID);
        String spsId1ret = mwlItem1.getItem(Tags.SPSSeq).getString(Tags.SPSID);
        assertEquals(spsId1ds, spsId1ret);        

        // insert second entry without sps-id -> returns mwl item with new generated sps-id
        spsItem.remove(Tags.SPSID);
        Dataset mwlItem2 = mwlManager.addWorklistItem(ds);
        String rpId2ret = mwlItem2.getString(Tags.RequestedProcedureID);
        String spsId2ret = mwlItem2.getItem(Tags.SPSSeq).getString(Tags.SPSID);
        
        // remove first entry
        mwlManager.removeWorklistItem(rpId1ret, spsId1ret);

        // remove second entry -> returned entry contains generated sps-id
        Dataset dsRet = mwlManager.removeWorklistItem(rpId2ret, spsId2ret);                
        Dataset spsItemRet = dsRet.getItem(Tags.SPSSeq);
        String spsId2ds = spsItemRet.getString(Tags.SPSID);
        assertEquals(spsId2ret, spsId2ds);
    }

    private Dataset loadMWLItemFromFile() throws SAXException, IOException {
        FileInputStream fis = new FileInputStream(FILE);
        try {
            return DatasetUtils.fromXML(fis);
        } finally {
            fis.close();
        }
    }
}
