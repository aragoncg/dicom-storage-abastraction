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

import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: SchedulerDelegate.java 2829 2006-10-03 09:52:50Z gunterze $
 * @since Oct 3, 2006
 */
public class SchedulerDelegate {
    private final ServiceMBeanSupport service;
    private ObjectName schedulerServiceName;

    public SchedulerDelegate(final ServiceMBeanSupport service) {
        this.service = service;
    }

    public final ObjectName getSchedulerServiceName() {
        return schedulerServiceName;
    }

    public final void setSchedulerServiceName(ObjectName schedulerServiceName) {
        this.schedulerServiceName = schedulerServiceName;
    }
    
    public Integer startScheduler(String name, long period,
            NotificationListener listener) throws Exception {
        return (Integer) service.getServer().invoke(
                schedulerServiceName, "startScheduler",
                new Object[] { name, new Long(period), listener},
                new String[] { String.class.getName(), long.class.getName(),
                        NotificationListener.class.getName()});       
    }

    public void stopScheduler(String name, Integer id,
            NotificationListener listener) throws Exception {
        service.getServer().invoke(schedulerServiceName, "stopScheduler",
                new Object[] { name, id, listener},
                new String[] { String.class.getName(), Integer.class.getName(),
                        NotificationListener.class.getName()});               
    }
}
