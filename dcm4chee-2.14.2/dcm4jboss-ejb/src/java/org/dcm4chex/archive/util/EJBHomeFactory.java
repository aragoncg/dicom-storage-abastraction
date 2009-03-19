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

package org.dcm4chex.archive.util;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import javax.ejb.EJBHome;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

/**
 * 
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3340 $ $Date: 2007-05-12 18:00:31 +0200 (Sat, 12 May 2007) $
 * @since 16.12.2003
 */
public class EJBHomeFactory {

    private static final String EJB_JNDI_PROPERTIES = "ejb-jndi.properties";

    private static EJBHomeFactory factory;

    private Hashtable homes = new Hashtable();

    private Context ctx;

    public static EJBHomeFactory getFactory() throws HomeFactoryException {
        if (EJBHomeFactory.factory == null) {
            try {
                EJBHomeFactory.factory = new EJBHomeFactory();
            } catch (Exception e) {
                throw new HomeFactoryException(e);
            }
        }
        return EJBHomeFactory.factory;
    }

    private EJBHomeFactory() throws NamingException, IOException {
        Properties env = new Properties();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        env.load(cl.getResourceAsStream(EJB_JNDI_PROPERTIES));
        ctx = new InitialContext(env);
    }

    public EJBHome lookup(Class homeClass, String jndiName)
            throws HomeFactoryException {
        EJBHome home = (EJBHome) homes.get(homeClass);
        if (home == null) {
            try {
                home = (EJBHome) PortableRemoteObject.narrow(ctx
                        .lookup(jndiName), homeClass);
            } catch (ClassCastException e) {
                throw new HomeFactoryException(e);
            } catch (NamingException e) {
                throw new HomeFactoryException(e);
            }
            homes.put(homeClass, home);
        }
        return home;
    }
}
