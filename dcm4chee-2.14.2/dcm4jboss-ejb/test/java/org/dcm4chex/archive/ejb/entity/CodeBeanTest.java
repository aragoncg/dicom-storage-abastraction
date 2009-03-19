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
import org.dcm4chex.archive.ejb.interfaces.CodeLocalHome;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 *
 */
public class CodeBeanTest extends ServletTestCase
{

    public static final String VALUE_ = "999";
    public static final String MEANING_ = "Meaning of code 999";
    public static final String DESIGNATOR = "99DCM4CHE";
    private CodeLocalHome codeHome;
    private Object[] codePks;

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(CodeBeanTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Context ctx = new InitialContext();
        codeHome = (CodeLocalHome) ctx.lookup("java:comp/env/ejb/Code");
        ctx.close();
        codePks = new Object[5];
        for (int i = 0; i < 5; ++i)
        {
            String value = VALUE_ + i;
            String meaning = MEANING_ + i;
            codePks[i] = codeHome.create(value, DESIGNATOR, null, meaning).getPrimaryKey();
        }
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        for (int i = 0; i < 5; ++i)
        {
            codeHome.remove(codePks[i]);
        }
    }

    /**
     * Constructor for StudyBeanTest.
     * @param arg0
     */
    public CodeBeanTest(String arg0)
    {
        super(arg0);
    }

    public void testFindByValueAndDesignator() throws Exception
    {
        for (int i = 0; i < 5; i++)
        {
            String value = VALUE_ + i;
            codeHome.findByValueAndDesignator(value, DESIGNATOR);
        }
    }
}
