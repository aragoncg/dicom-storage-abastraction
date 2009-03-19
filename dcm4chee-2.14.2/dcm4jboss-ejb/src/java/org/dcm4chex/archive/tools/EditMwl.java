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

package org.dcm4chex.archive.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.ResourceBundle;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.MWLManager;
import org.dcm4chex.archive.ejb.interfaces.MWLManagerHome;
import org.xml.sax.SAXException;

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3287 $ $Date: 2007-04-19 01:25:26 +0200 (Thu, 19 Apr 2007) $
 * @since 17.02.2004
 */
public final class EditMwl {

    private static ResourceBundle messages =
        ResourceBundle.getBundle(EditMwl.class.getName());

    private final static LongOpt[] LONG_OPTS =
        new LongOpt[] {
            new LongOpt("url", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
            };

    private String ejbProviderURL = "jnp://localhost:1099";

    public static void main(String[] args) {
         Getopt g = new Getopt("mwlitem.jar", args, "af:r:u:hv", LONG_OPTS);

        EditMwl mwl = new EditMwl();
        int c;
        int cmd = 0;
        String[] rpIdspsId = null;
        String fpath = null;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'a' :
                    cmd = c;
                    break;
                case 'r' :
                    cmd = c;
                    rpIdspsId = StringUtils.split(g.getOptarg(), '/');
                    if (rpIdspsId.length != 2) {
                        exit(messages.getString("rqidspsid"), true);                        
                    }
                    break;
                case 'f' :
                    fpath = g.getOptarg();
                    break;
                case 'u' :
                    mwl.ejbProviderURL = g.getOptarg();
                    break;
                case 'v' :
                    exit(messages.getString("version"), false);
                case 'h' :
                    exit(messages.getString("usage"), false);
                case '?' :
                    exit(null, true);
                    break;
            }
        }
        if (cmd == 0) {
            exit(messages.getString("missing"), true);
        }
        try {
            switch (cmd) {
                case 'a' :
                {
                    Dataset ds = mwl.add(loadMWLItem(fpath));
                    Dataset spsitem = ds.getItem(Tags.SPSSeq);
                    System.out.println(ds.getString(Tags.RequestedProcedureID)
                            + '/' + spsitem.getString(Tags.SPSID));
                    break;
                }
                case 'r' :
                    mwl.remove(rpIdspsId[0], rpIdspsId[1]);
                    }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * @param dataset
     * @return
     */
    private Dataset add(Dataset ds) throws Exception {
        return getMWLManager().addWorklistItem(ds);
    }

    /**
     * @param arg
     */
    private Dataset remove(String rqpid, String spsid) throws Exception {
        return getMWLManager().removeWorklistItem(rqpid, spsid);
    }

    private static Dataset loadMWLItem(String fpath) throws SAXException, IOException {
        if (isXML(fpath)) {
            InputStream is = fpath == null ? System.in : new FileInputStream(fpath);
            try {
                return DatasetUtils.fromXML(is); 
            } finally {
                if (fpath != null) {
                    try {
                        is.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        } else {
            Dataset ds = DcmObjectFactory.getInstance().newDataset();
            ds.readFile(new File(fpath), null, -1);
            return ds;
        }
    }

    private static boolean isXML(String fpath) {
        return fpath == null || fpath.toUpperCase().endsWith(".XML");
    }

    private Hashtable makeEnv() {
        Hashtable env = new Hashtable();
        env.put(
                "java.naming.factory.initial",
        "org.jnp.interfaces.NamingContextFactory");
        env.put(
                "java.naming.factory.url.pkgs",
        "org.jboss.naming:org.jnp.interfaces");
        env.put("java.naming.provider.url", ejbProviderURL);
        return env;
    }

    private MWLManager getMWLManager() throws Exception {
	    Context ctx = new InitialContext(makeEnv());
	    MWLManagerHome home = (MWLManagerHome) ctx.lookup(MWLManagerHome.JNDI_NAME);
	    ctx.close();
	    return home.create();
    }
    
    private static void exit(String prompt, boolean error) {
        if (prompt != null) {
            System.err.println(prompt);
        }
        if (error) {
            System.err.println(messages.getString("usage"));
        }
        System.exit(1);
    }

}