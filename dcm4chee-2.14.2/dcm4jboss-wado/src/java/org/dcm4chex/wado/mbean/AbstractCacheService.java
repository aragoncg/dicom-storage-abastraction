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

package org.dcm4chex.wado.mbean;

import java.io.IOException;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.mbean.SchedulerDelegate;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.wado.mbean.cache.WADOCache;
import org.dcm4chex.wado.mbean.cache.WADOCacheImpl;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer
 *
 * The MBean to manage the WADO service.
 * <p>
 * This class use WADOSupport for the WADO methods and WADOCache for caching jpg images.
 */
public abstract class AbstractCacheService extends ServiceMBeanSupport {

    protected final SchedulerDelegate scheduler = new SchedulerDelegate(this);
    
	protected Logger log = Logger.getLogger( getClass().getName() );
	protected WADOCache cache = null;
	
	private static final long MIN_FREE = 1 * FileUtils.MEGA;
	private static final long MIN_PREF_FREE = 2 * FileUtils.MEGA;
		
	private String timerID;
	
    private final NotificationListener freeDiskSpaceListener = 
        new NotificationListener(){
            public void handleNotification(Notification notif, Object handback) {
                try {
                    cache.freeDiskSpace( true );
                } catch (IOException e) {
                    log.error("Failed to free disk space: ", e);
                }
            }};
	private long freeDiskSpaceInterval;
	private Integer freeDiskSpaceListenerID;
	
    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }
            
	public WADOCache getCache() {
		return cache;
	}
	
	public void setCacheRoot( String newRoot ) {
		cache.setCacheRoot( newRoot );
	}
	
	public String getCacheRoot() {
		return cache.getCacheRoot();
	}
	
    public String getDeleterThresholds() {
        return ((WADOCacheImpl) cache).getDeleterThresholds();
    }

    public void setDeleterThresholds(String s) {
    	((WADOCacheImpl) cache).setDeleterThresholds(s);
    }

	
	public String showMinFreeSpace() {
		return FileUtils.formatSize(cache.getMinFreeSpace());
	}

	public void setPreferredFreeSpace( String minFree ) {
		((WADOCacheImpl) cache).setPreferredFreeSpace( FileUtils.parseSize(minFree, MIN_PREF_FREE) );
	}
	
	public String getPreferredFreeSpace() {
		return FileUtils.formatSize(cache.getPreferredFreeSpace());
	}
	
	/**
	 * Clears the cache.
	 * <p>
	 * Remove all files in the cache.
	 * @throws IOException 
	 *
	 */
	public String clearCache() throws IOException {
		long before = cache.showFreeSpace();
		cache.clearCache();
		return getDeleteResult( before );
	}
	
	private String getDeleteResult( long before ) throws IOException {
		long after = cache.showFreeSpace();
		long delta = after - before;
		StringBuffer sb = new StringBuffer();
		log.info("getDeleteResult: before:"+before+" , after:"+after+" , delta:"+delta);
		sb.append(FileUtils.formatSize(delta)).append(" removed!");
		if ( after < cache.getMinFreeSpace() ) {
			sb.append(" WARNING: INSUFFICIANT Free disk space! Should be ").append(this.showMinFreeSpace());
		} else if ( after < cache.getPreferredFreeSpace() ) {
			sb.append(" WARNING: Free disk space is less than preferred free space (").append(this.getPreferredFreeSpace()).append(")");
		}
		return sb.toString();
	}

	/**
	 * Get the timer interval for scheduling freeDiskSpace.
	 * <p>
	 * The return value is either 'NEVER' or &lt;interval&gt;&lt;unit&gt; where interval is a 
	 * natural number and unit one of 'w', 'd', 'h', 'm' or 's' representing week, day, hour,
	 * minute or second.
	 * 
	 * @return A formatted String representing the timer interval.
	 */
    public final String getFreeDiskSpaceInterval() {
        return RetryIntervalls.formatIntervalZeroAsNever(freeDiskSpaceInterval);
    }
    
    /**
	 * Set the timer interval for scheduling freeDiskSpace.
	 * <p>
	 * The parameter value is either 'NEVER' or &lt;interval&gt;&lt;unit&gt; where interval is a 
	 * natural number and unit one of 'w', 'd', 'h', 'm' or 's' representing week, day, hour,
	 * minute or second.
     * 
     * @param interval Timer interval as formatted String.
     * @throws Exception 
     */
    public void setFreeDiskSpaceInterval(String interval) throws Exception {
        freeDiskSpaceInterval = RetryIntervalls.parseIntervalOrNever(interval);
        if (getState() == STARTED) {
            scheduler.stopScheduler(timerID, freeDiskSpaceListenerID,
            		freeDiskSpaceListener);
            freeDiskSpaceListenerID = scheduler.startScheduler(timerID,
            		freeDiskSpaceInterval, freeDiskSpaceListener);
        }
    }
	
	/**
	 * Cleans the cache if necessary.
	 * @throws IOException 
	 *
	 */
	public String freeDiskSpace() throws IOException {
		long before = cache.showFreeSpace();
		cache.freeDiskSpace( false ); //cleans the cache and wait until clean process is done. (not in background)
		return getDeleteResult( before );
	}
	
	/**
	 * Returns the available space of the cache drive.
	 * <p>
	 * 
	 * @return
	 * @throws IOException 
	 */
	public String showFreeSpace() throws IOException {
		return FileUtils.formatSize( cache.showFreeSpace() );
	}
	
	/**
	 * Start scheduler for freeDiskSpace.
	 * <p>
	 * This queue is used to receive media creation request from scheduler or web interface.
	 */
    protected void startService() throws Exception {
        freeDiskSpaceListenerID = scheduler.startScheduler(timerID, 
        		freeDiskSpaceInterval, freeDiskSpaceListener);
    }

	/**
	 * Stop scheduler for freeDiskSpace.
	 * 
	 */
    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerID, freeDiskSpaceListenerID,
        		freeDiskSpaceListener);
        super.stopService();
    }

	public String getTimerID() {
		return timerID;
	}

	public void setTimerID(String timerID) {
		this.timerID = timerID;
	}
	

}
