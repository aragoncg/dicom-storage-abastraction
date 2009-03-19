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

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.dcm4chex.archive.exceptions.ConfigurationException;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: JMSQueueDelegate.java 2986 2006-11-30 01:31:15Z gunterze $
 * @since Oct 9, 2006
 */
class JMSQueueDelegate {

    public static final String CONNECTION_FACTORY = "java:ConnectionFactory";

    /**
     * JBoss-vendor specific property for scheduling a JMS message. In
     * milliseconds since January 1, 1970.
     */
    public static final String PROPERTY_SCHEDULED_DELIVERY =
        "JMS_JBOSS_SCHEDULED_DELIVERY";

    private final QueueConnection conn;

    private final Queue queue;

    private int deliveryMode = DeliveryMode.PERSISTENT;

    public JMSQueueDelegate(String name, MessageListener listener,
            int receiverCount) throws JMSException {
        InitialContext iniCtx = null;
        QueueConnectionFactory qcf = null;
        try {
            iniCtx = new InitialContext();
            qcf = (QueueConnectionFactory) iniCtx.lookup(CONNECTION_FACTORY);
            queue = (Queue) iniCtx.lookup("queue/" + name);
            conn = qcf.createQueueConnection();
        } catch (NamingException e) {
            throw new ConfigurationException(e);
        } catch (JMSException e) {
            throw new ConfigurationException(e);
        } finally {
            if (iniCtx != null) {
                try {
                    iniCtx.close();
                } catch (Exception ignore) {
                }
            }
        }
        try {
            for (int i = 0; i < receiverCount; ++i) {
                QueueSession session = conn.createQueueSession(false,
                        QueueSession.AUTO_ACKNOWLEDGE);
                QueueReceiver receiver = session.createReceiver(queue);
                receiver.setMessageListener(listener);
            }
            conn.start();
        } catch (JMSException e) {
            close();
            throw e;
        }
    }

    public void close() {
        try {
            conn.close();
        } catch (Exception ignore) {
        }
    }

    public void queueMessage(Serializable obj, int priority, long scheduledTime)
            throws JMSException {
        QueueSession session = conn.createQueueSession(false,
                QueueSession.AUTO_ACKNOWLEDGE);
        try {
            ObjectMessage msg = session.createObjectMessage(obj);
            if (scheduledTime > 0L)
                msg.setLongProperty(PROPERTY_SCHEDULED_DELIVERY, scheduledTime);
            QueueSender sender = session.createSender(queue);
            sender.send(msg, deliveryMode, priority,
                    ObjectMessage.DEFAULT_TIME_TO_LIVE);
        } finally {
            session.close();
        }
    }

}
