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

package org.dcm4chex.archive.web.maverick.ae;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class AEDelegate {
    private static ObjectName echoServiceName = null;

    private static ObjectName aeServiceName = null;

    private static MBeanServer server;

    private List aes;

    private static Logger log = Logger.getLogger(AEDelegate.class.getName());

    /**
     * Iinitialize the Echo service delegator.
     * <p>
     * Set the name of the EchoService MBean with the servlet config param
     * 'echoServiceName'.
     * 
     * @param config
     *            The ServletConfig object.
     */
    public void init(ServletConfig config) {
        if (server != null)
            return;
        server = MBeanServerLocator.locate();
        try {
            echoServiceName = new ObjectName(config
                    .getInitParameter("echoServiceName"));
            aeServiceName = new ObjectName(config
                    .getInitParameter("aeServiceName"));
        } catch (Exception e) {
            log.error("Exception in init! ", e);
        }

    }

    public Logger getLogger() {
        return log;
    }

    // AE Service
    /**
     * Return list of all configured AE
     */
    public List getAEs() {
        try {
            aes = (List) server.invoke(aeServiceName, "listAEs", null, null);
            return aes;
        } catch (Exception x) {
            log.error("Exception occured in getAEs: " + x.getMessage(), x);
            return null;
        }
    }

    /**
     * @param title
     * @return
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     */
    public AEDTO getAE(String title) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        return (AEDTO) server
                .invoke(aeServiceName, "getAE", new Object[] { title },
                        new String[] { String.class.getName() });
    }

    /**
     * @param newAE
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     */
    public void updateAE(AEDTO ae, boolean checkHost)
            throws InstanceNotFoundException, MBeanException,
            ReflectionException {
        server.invoke(aeServiceName, "updateAE", 
                new Object[] {
                    new Long(ae.getPk()),
                    ae.getTitle(),
                    ae.getHostName(),
                    new Integer(ae.getPort()),
                    ae.getCipherSuitesAsString(),
                    ae.getIssuerOfPatientID(),
                    ae.getUserID(),
                    ae.getPassword(),
                    ae.getFileSystemGroupID(),
                    ae.getDescription(),
                    ae.getWadoUrl(),
                    new Boolean(checkHost) },
                new String[] {
                    long.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    int.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    boolean.class.getName() });
    }

    /**
     * @param title
     * @return
     * @throws ReflectionException
     * @throws MBeanException
     * @throws InstanceNotFoundException
     */
    public AEDTO delAE(String title) throws InstanceNotFoundException,
            MBeanException, ReflectionException {
        return (AEDTO) server
                .invoke(aeServiceName, "removeAE", new Object[] { title },
                        new String[] { String.class.getName() });
    }

    // ECHO Service
    /**
     * Makes the MBean call to echo an AE configuration.
     * 
     * 
     * @return An info string for status of echo.
     */
    public String echo(AEDTO aeData, int nrOfTests) {
        if (log.isDebugEnabled())
            log.debug("Send echo to " + aeData);
        String resp = null;
        try {
            Object o = server.invoke(echoServiceName, "echo", new Object[] {
                    aeData, new Integer(nrOfTests) }, new String[] {
                    AEDTO.class.getName(), Integer.class.getName() });
            resp = (String) o;
        } catch (Exception x) {
            log.error("Exception occured in echoAE: " + x.getMessage(), x);
        }
        if (log.isDebugEnabled())
            log.debug("echo response for " + aeData + ":" + resp);
        return resp;
    }

    /**
     * Makes the MBean call to echo an AET (AE config for given title).
     * 
     * 
     * @return An info string for status of echo.
     */
    public String echo(String aet, int nrOfTests) {
        String resp = null;
        try {
            Object o = server.invoke(echoServiceName, "echo", new Object[] {
                    aet, new Integer(nrOfTests) }, new String[] {
                    String.class.getName(), Integer.class.getName() });
            resp = (String) o;
        } catch (Exception x) {
            log.error("Exception occured in echo (AET=" + aet + "): "
                    + x.getMessage(), x);
        }
        return resp;
    }

    /**
     * Makes the MBean call to echoe an AE configuration.
     * 
     * 
     * @return An info string for status of echo.
     */
    public String[] echoAll(AEDTO aeData) {
        String[] resp = null;
        try {
            Object o = server.invoke(echoServiceName, "echoAll", null, null);
            resp = (String[]) o;
        } catch (Exception x) {
            log.error("Exception occured in echo (" + aeData + "): "
                    + x.getMessage(), x);
        }
        return resp;
    }
}
