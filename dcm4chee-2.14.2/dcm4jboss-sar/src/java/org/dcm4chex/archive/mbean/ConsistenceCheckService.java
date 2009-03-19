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

package org.dcm4chex.archive.mbean;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Calendar;

import javax.ejb.FinderException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.ejb.interfaces.ConsistencyCheck;
import org.dcm4chex.archive.ejb.interfaces.ConsistencyCheckHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7176 $ $Date: 2006-10-24 20:34:19 +0200 (Tue, 24 Oct
 *          2006) $
 * @since 35.03.2005
 * 
 */
public class ConsistenceCheckService extends ServiceMBeanSupport {

    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);

    private int availabilityOfExternalRetrieveable;
    private long taskInterval = 0L;
    private long minStudyAge;
    private long maxStudyAge;
    private long maxCheckedBefore;

    private int disabledStartHour;
    private int disabledEndHour;
    private int limitNumberOfStudiesPerTask;

    private Integer listenerID;

    private String timerIDCheckStudyConsistency;

    private static final Logger log = Logger
            .getLogger(ConsistenceCheckService.class);

    private final NotificationListener consistentCheckListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (isDisabled(hour)) {
                if (log.isDebugEnabled())
                    log.debug("ConsistentCheck ignored in time between "
                            + disabledStartHour + " and " + disabledEndHour
                            + " !");
            } else {
                try {
                    check();
                } catch (Exception e) {
                    log.error("Consistant check failed!", e);
                }
            }
        }
    };

    public String getAvailabilityOfExternalRetrieveable() {
        return Availability.toString(availabilityOfExternalRetrieveable);
    }

    public void setAvailabilityOfExternalRetrieveable(String availability) {
        this.availabilityOfExternalRetrieveable =
                Availability.toInt(availability.trim());
    }

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }

    public final String getTaskInterval() {
        String s = RetryIntervalls.formatIntervalZeroAsNever(taskInterval);
        return (disabledEndHour == -1) ? s : s + "!" + disabledStartHour + "-"
                + disabledEndHour;
    }

    public void setTaskInterval(String interval) throws Exception {
        long oldInterval = taskInterval;
        int pos = interval.indexOf('!');
        if (pos == -1) {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval);
            disabledEndHour = -1;
        } else {
            taskInterval = RetryIntervalls.parseIntervalOrNever(interval
                    .substring(0, pos));
            int pos1 = interval.indexOf('-', pos);
            disabledStartHour = Integer.parseInt(interval.substring(pos + 1,
                    pos1));
            disabledEndHour = Integer.parseInt(interval.substring(pos1 + 1));
        }
        if (getState() == STARTED && oldInterval != taskInterval) {
            scheduler.stopScheduler(timerIDCheckStudyConsistency, listenerID,
                    consistentCheckListener);
            listenerID = scheduler.startScheduler(timerIDCheckStudyConsistency,
                    taskInterval, consistentCheckListener);
        }
    }

    public int getLimitNumberOfStudiesPerTask() {
        return limitNumberOfStudiesPerTask;
    }

    public void setLimitNumberOfStudiesPerTask(int limit) {
        this.limitNumberOfStudiesPerTask = limit;
    }

    /**
     * Getter for minStudyAge.
     * <p>
     * This value is used to ensure that consistent check is not performed on
     * studies that are not completed (store is not completed).
     * 
     * @return Returns ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMinStudyAge() {
        return RetryIntervalls.formatInterval(minStudyAge);
    }

    /**
     * Setter for minStudyAge.
     * <p>
     * This value is used to ensure that consistent check is not performed on
     * studies that are not completed (store is not completed).
     * 
     * @param age
     *            ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public void setMinStudyAge(String age) {
        this.minStudyAge = RetryIntervalls.parseInterval(age);
    }

    /**
     * Getter for maxStudyAge.
     * <p>
     * This value is used to limit consistent check to 'newer' studies.
     * 
     * @return ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMaxStudyAge() {
        return RetryIntervalls.formatInterval(maxStudyAge);
    }

    /**
     * Setter for maxStudyAge.
     * <p>
     * This value is used to limit consistent check to 'newer' studies.
     * 
     * @param maxStudyAge
     *            The maxStudyAge to set.
     */
    public void setMaxStudyAge(String age) {
        this.maxStudyAge = RetryIntervalls.parseInterval(age);
    }

    /**
     * Getter for maxStudyAge.
     * <p>
     * This value is used to limit consistent check to 'newer' studies.
     * 
     * @return ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMaxCheckedBefore() {
        return RetryIntervalls.formatInterval(maxCheckedBefore);
    }

    /**
     * Setter for maxStudyAge.
     * <p>
     * This value is used to limit consistent check to 'newer' studies.
     * 
     * @param maxStudyAge
     *            The maxStudyAge to set.
     */
    public void setMaxCheckedBefore(String maxCheckedBefore) {
        this.maxCheckedBefore = RetryIntervalls.parseInterval(maxCheckedBefore);
    }

    public String check() throws RemoteException, FinderException {
        int updated = 0;
        long l = System.currentTimeMillis();
        Timestamp createdBefore = new Timestamp(l - this.minStudyAge);
        Timestamp createdAfter = new Timestamp(l - this.maxStudyAge);
        Timestamp checkedBefore = new Timestamp(l - this.maxCheckedBefore);
        ConsistencyCheck checker = newConsistencyCheck();
        if (log.isDebugEnabled())
            log.debug("call findStudiesToCheck: createdAfter:" + createdAfter
                    + " createdBefore:" + createdBefore + " checkedBefore:"
                    + checkedBefore);
        long[] studyPks = checker.findStudiesToCheck(createdAfter,
                createdBefore, checkedBefore, this.limitNumberOfStudiesPerTask);
        for (int i = 0, len = studyPks.length; i < len; i++) {
            if (checker.updateStudy(studyPks[i],
                    availabilityOfExternalRetrieveable)) {
                updated++;
            }
        }
        return updated + " of " + studyPks.length + " studies updated!";
    }

    private boolean isDisabled(int hour) {
        if (disabledEndHour == -1)
            return false;
        boolean sameday = disabledStartHour <= disabledEndHour;
        boolean inside = hour >= disabledStartHour && hour < disabledEndHour;
        return sameday ? inside : !inside;
    }

    protected void startService() throws Exception {
        listenerID = scheduler.startScheduler(timerIDCheckStudyConsistency,
                taskInterval, consistentCheckListener);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDCheckStudyConsistency, listenerID,
                consistentCheckListener);
        super.stopService();
    }

    private ConsistencyCheck newConsistencyCheck() {
        try {
            ConsistencyCheckHome home = (ConsistencyCheckHome) EJBHomeFactory
                    .getFactory().lookup(ConsistencyCheckHome.class,
                            ConsistencyCheckHome.JNDI_NAME);
            return home.create();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to access ConsistencyCheck EJB:", e);
        }
    }

    public String getTimerIDCheckStudyConsistency() {
        return timerIDCheckStudyConsistency;
    }

    public void setTimerIDCheckStudyConsistency(
            String timerIDCheckStudyConsistency) {
        this.timerIDCheckStudyConsistency = timerIDCheckStudyConsistency;
    }

}