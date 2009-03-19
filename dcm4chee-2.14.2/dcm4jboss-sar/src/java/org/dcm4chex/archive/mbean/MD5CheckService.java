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

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;

import javax.ejb.FinderException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.util.MD5Utils;
import org.dcm4chex.archive.common.FileStatus;
import org.dcm4chex.archive.config.RetryIntervalls;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 7668 $ $Date: 2008-10-20 16:34:15 +0200 (Mon, 20 Oct 2008) $
 * @since 35.03.2005
 *
 */
public class MD5CheckService extends ServiceMBeanSupport {

    private final SchedulerDelegate scheduler = new SchedulerDelegate(this);

    private long taskInterval = 0L;
	private long maxCheckedBefore;

    private int disabledStartHour;
    private int disabledEndHour;
    private int limitNumberOfFilesPerTask;
    private int bufferSize = 8192;

    private Integer listenerID;

    private String timerIDCheckMD5;
    
    private final NotificationListener timerListener = new NotificationListener() {
        public void handleNotification(Notification notif, Object handback) {
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            if (isDisabled(hour)) {
                if (log.isDebugEnabled())
                    log.debug("MD5Check ignored in time between "
                            + disabledStartHour + " and " + disabledEndHour
                            + " !");
            } else {
                try {
                	check();
                } catch (Exception e) {
                    log.error("MD5 check failed!", e);
                }
            }
        }
    };

    public ObjectName getSchedulerServiceName() {
        return scheduler.getSchedulerServiceName();
    }

    public void setSchedulerServiceName(ObjectName schedulerServiceName) {
        scheduler.setSchedulerServiceName(schedulerServiceName);
    }
        
    public final int getBufferSize() {
        return bufferSize ;
    }

    public final void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
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
            scheduler.stopScheduler(timerIDCheckMD5, listenerID, timerListener);
            listenerID = scheduler.startScheduler(timerIDCheckMD5, taskInterval,
            		timerListener);
        }
    }


    public int getLimitNumberOfFilesPerTask() {
        return limitNumberOfFilesPerTask;
    }

    public void setLimitNumberOfFilesPerTask(int limit) {
        this.limitNumberOfFilesPerTask = limit;
    }

    
    /**
	 * Getter for maxCheckedBefore. 
	 * <p>
	 * This value is used to limit check not recently checked files only.
     * 
     * @return ##w (in weeks), ##d (in days), ##h (in hours).
     */
    public String getMaxCheckedBefore() {
        return RetryIntervalls.formatInterval(maxCheckedBefore);
    }
    /**
	 * Setter for maxCheckedBefore. 
	 * <p>
	 * This value is used to check not recently checked files only.
     *  
     * @param maxCheckedBefore The maxCheckedBefore to set.
     */
    public void setMaxCheckedBefore(String maxCheckedBefore) {
        this.maxCheckedBefore = RetryIntervalls.parseInterval(maxCheckedBefore);
    }
    
    public String check() throws Exception {
    	if ( log.isDebugEnabled() ) log.debug("MD5 check started!");
    	int corrupted = 0;
    	int total = 0;
        Timestamp before = new Timestamp( System.currentTimeMillis() - this.maxCheckedBefore );
        FileDTO[] files;
        int limit = limitNumberOfFilesPerTask;
        FileSystemMgt2 fsMgt = newFileSystemMgt();
        FileSystemDTO[] fsdirs =fsMgt.getAllFileSystems();
        byte[] buffer = null;
        for (int j = 0; j < fsdirs.length; j++) {
            files = fsMgt.findFilesForMD5Check(fsdirs[j].getDirectoryPath(), before, limit);
        	if ( log.isDebugEnabled() ) log.debug("Check MD5 for " + files.length + " files on filesystem " + fsdirs[j]);
            if (files.length > 0) {
            	if (buffer == null)
            		buffer = new byte[bufferSize];
                total += files.length;
                for (int k = 0; k < files.length; k++) {
					if ( ! doCheck(fsMgt, files[k], buffer) ) 
                    	corrupted++;
                }
                limit -= files.length;
            }
        }
        if ( corrupted > 0 ) 
        	log.warn( corrupted + " files are corrupted!");
    	return corrupted + " of "+ total + " files corrupted!";
    }
    
    /**
	 * @param fsMgt
	 * @param fileDTO
     * @param buffer 
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws FinderException
	 */
	private boolean doCheck(FileSystemMgt2 fsMgt, FileDTO fileDTO, byte[] buffer)
	throws IOException, NoSuchAlgorithmException, FinderException {
		if ( log.isDebugEnabled() ) log.debug("check md5 for file "+fileDTO );
        char[] storedMD5 = MD5Utils.toHexChars(fileDTO.getFileMd5());
        final char[] fileMD5 = new char[32];
        File file = FileUtils.toFile(fileDTO.getDirectoryPath(), fileDTO
                .getFilePath());
        
        MessageDigest digest = MessageDigest.getInstance("MD5");
		MD5Utils.md5sum(file, fileMD5, digest, buffer);
        fsMgt.updateTimeOfLastMd5Check( fileDTO.getPk() );
        if (!Arrays.equals(fileMD5, storedMD5 ) ) {
        	fsMgt.setFileStatus( fileDTO.getPk(), FileStatus.MD5_CHECK_FAILED );
        	log.warn("File (pk="+fileDTO.getPk()+") " + file 
        			+ " corrupted! MD5 of file:"+ new String(fileMD5)
        			+" should be "+ new String(storedMD5) );
            return false;
        }
        return true;
	}

	private boolean isDisabled(int hour) {
        if (disabledEndHour == -1) return false;
        boolean sameday = disabledStartHour <= disabledEndHour;
        boolean inside = hour >= disabledStartHour && hour < disabledEndHour; 
        return sameday ? inside : !inside;
    }

    protected void startService() throws Exception {
        listenerID = scheduler.startScheduler(timerIDCheckMD5, taskInterval, timerListener);
    }

    protected void stopService() throws Exception {
        scheduler.stopScheduler(timerIDCheckMD5, listenerID, timerListener);
        super.stopService();
    }

    protected FileSystemMgt2 newFileSystemMgt() throws Exception {
        return ((FileSystemMgt2Home) EJBHomeFactory.getFactory().lookup(
                FileSystemMgt2Home.class, FileSystemMgt2Home.JNDI_NAME)).create();
    }

	public String getTimerIDCheckMD5() {
		return timerIDCheckMD5;
	}

	public void setTimerIDCheckMD5(String timerIDCheckMD5) {
		this.timerIDCheckMD5 = timerIDCheckMD5;
	}

}