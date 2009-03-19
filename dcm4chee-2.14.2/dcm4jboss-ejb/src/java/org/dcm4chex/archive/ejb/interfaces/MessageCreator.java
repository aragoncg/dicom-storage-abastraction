package org.dcm4chex.archive.ejb.interfaces;

import java.io.Serializable;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

/**
 * Interface to create a JMS message for given session
 * 
 * @author fang.yang@agfa.com
 * @version $Id: MessageCreator.java 2504 2006-05-31 12:49:43Z javawilli $
 * @since May 4, 2006
 *
 */
public interface MessageCreator extends Serializable {
    
	public Message getMessage(Session session) throws JMSException;
	
}
