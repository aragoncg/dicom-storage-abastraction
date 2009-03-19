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
 * Portions created by the Initial Developer are Copyright (C) 2005
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

package org.dcm4chex.archive.common;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is abstract JMS order message that provides failure counting feature
 * 
 * @author fang.yang@agfa.com
 * @version $Revision: 2830 $ $Date: 2006-10-03 12:25:22 +0200 (Tue, 03 Oct 2006) $
 * @since April 4, 2006
 */
public abstract class BaseJmsOrder implements Serializable {
		
	protected static long counter = 0;
	private String id;
    private int failureCount = 0;
    private Throwable throwable = null;  // Remember last exception happened
	private String origQueueName = null; // The original queue
	
    public BaseJmsOrder()
    {
    	id = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + counter++;
    }
    
    public final int getFailureCount() {
        return failureCount;
    }

    public final void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

	public Throwable getThrowable() {
		return throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}
	
	public String toIdString()
	{
		return getClass().getName() + "@" + id + "@" + Integer.toHexString(hashCode());
	}

	/**
	 * Set the original queue name, only the first time
	 * 
	 * @param queueName
	 */
	public void setQueueName(String queueName) {		
		if(origQueueName == null)
			origQueueName = queueName;
	}
	
	public String getQueueName()
	{
		return origQueueName;
	}
	
	public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("\tInternal ID: ").append(toIdString()).append("\n");
		sb.append("\tOriginal queue name: ").append(origQueueName).append("\n");
		sb.append("\tFailure count: ").append(failureCount).append("\n");
		if(throwable != null)
		{
			StringWriter sw = new StringWriter(); 
			throwable.printStackTrace( new PrintWriter( sw ) ); 
			sb.append("\tException caught: ").append(sw.toString()).append("\n");
		}
		return sb.toString();
	}
}
