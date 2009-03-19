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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below. 
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

package org.dcm4chex.archive.mbean;

import java.io.Serializable;

import javax.jms.MessageListener;
import javax.management.MBeanException;
import javax.management.ObjectName;

import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: JMSDelegate.java 2915 2006-11-01 20:36:29Z gunterze $
 * @since Oct 9, 2006
 */
public class JMSDelegate {

    private ServiceMBeanSupport service;

    private ObjectName jmsServiceName;

    public static int toJMSPriority(int dcmPriority) {
        return (dcmPriority == 1) ? 5 : (dcmPriority == 2) ? 3 : 4;
    }

    public JMSDelegate(ServiceMBeanSupport service) {
        this.service = service;
    }

    public final ObjectName getJmsServiceName() {
        return jmsServiceName;
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        this.jmsServiceName = jmsServiceName;
    }

    public void startListening(String name, MessageListener listener,
            int receiverCount) throws Exception {
        try {
            service.getServer().invoke(jmsServiceName,"startListening",
                    new Object[] { name, listener, new Integer(receiverCount) },
                    new String[] { String.class.getName(),
                        MessageListener.class.getName(), int.class.getName() });
        } catch (MBeanException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation startListening on "
                    + jmsServiceName, e);
        }
    }

    public void stopListening(String name) throws Exception {
        try {
            service.getServer().invoke(jmsServiceName, "stopListening",
                    new Object[] { name },
                    new String[] { String.class.getName() });
        } catch (MBeanException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation stopListening on "
                    + jmsServiceName, e);
        }
    }

    public void queue(String name, Serializable obj, int prior,
            long scheduledTime) throws Exception {
        try {
            service.getServer().invoke(jmsServiceName, "queue",
                    new Object[] { name, obj, new Integer(prior),
                        new Long(scheduledTime) },
                    new String[] { String.class.getName(),
                        Serializable.class.getName(), int.class.getName(),
                        long.class.getName() });
        } catch (MBeanException e) {
            throw e.getTargetException();
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation queue on " + jmsServiceName, e);
        }
    }
}
