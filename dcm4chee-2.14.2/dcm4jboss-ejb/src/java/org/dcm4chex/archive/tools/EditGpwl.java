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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

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
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.GPWLManager;
import org.dcm4chex.archive.ejb.interfaces.GPWLManagerHome;
import org.xml.sax.SAXException;

/**
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2007-03-22 12:15:17 +0100 (Thu, 22 Mar 2007) $
 * @since 28.03.2005
 */

public class EditGpwl {

    private static ResourceBundle messages =
        ResourceBundle.getBundle(EditGpwl.class.getName());

    private final static LongOpt[] LONG_OPTS =
        new LongOpt[] {
            new LongOpt("url", LongOpt.REQUIRED_ARGUMENT, null, 'u'),
            new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'v'),
            };

    private String ejbProviderURL = "jnp://localhost:1099";

    public static void main(String[] args) {
         Getopt g = new Getopt("gpwlitem.jar", args, "af:r:u:hv", LONG_OPTS);

        EditGpwl gpwl = new EditGpwl();
        int c;
        int cmd = 0;
        String iuid = null;
        String fpath = null;
        while ((c = g.getopt()) != -1) {
            switch (c) {
                case 'a' :
                    cmd = c;
                    break;
                case 'r' :
                    cmd = c;
                    iuid = g.getOptarg();
                    break;
                case 'f' :
                    fpath = g.getOptarg();
                    break;
                case 'u' :
                    gpwl.ejbProviderURL = g.getOptarg();
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
                    iuid = gpwl.add(loadGPWLItem(fpath));
                    System.out.println(iuid);
                    break;
                case 'r' :
                    gpwl.remove(iuid);
                    }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private String add(Dataset ds) throws Exception {
        GPWLManager mgr = getGPWLManager();
        try {
            return mgr.addWorklistItem(ds);
        } finally {
            try {
                mgr.remove();
            } catch (Exception ignore) {
            }
        }
    }

    private Dataset remove(String uid) throws Exception {
        GPWLManager mgr = getGPWLManager();
        try {
            return mgr.removeWorklistItem(uid);
        } finally {
            try {
                mgr.remove();
            } catch (Exception ignore) {
            }
        }
    }

    private static Dataset loadGPWLItem(String fpath) throws SAXException, IOException {
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

    private GPWLManager getGPWLManager() throws Exception {
        Context ctx = new InitialContext(makeEnv());
        GPWLManagerHome home = (GPWLManagerHome) ctx.lookup(GPWLManagerHome.JNDI_NAME);
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
