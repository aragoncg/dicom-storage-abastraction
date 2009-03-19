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
 * Agfa HealthCare.
 * Portions created by the Initial Developer are Copyright (C) 2006-2008
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.MessageFormat;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.dcm4che.util.Executer;
import org.dcm4chex.archive.notif.StorageFileSystemSwitched;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Sep 1, 2008
 */
public class OnSwitchStorageFileSystemService extends ServiceMBeanSupport
        implements NotificationListener {

    private static final String NONE = "NONE";

    private ObjectName fileSystemMgtName;

    private String onSwitchStorageFSCmd;

    public ObjectName getFileSystemMgtName() {
        return fileSystemMgtName;
    }

    public void setFileSystemMgtName(ObjectName fileSystemMgtName) {
        this.fileSystemMgtName = fileSystemMgtName;
    }

    public String getOnSwitchStorageFileSystemInvoke() {
        return onSwitchStorageFSCmd != null ? onSwitchStorageFSCmd : NONE;
    }

    public void setOnSwitchStorageFileSystemInvoke(String command) {
        String s = command.trim();
        if (NONE.equalsIgnoreCase(s)) {
            onSwitchStorageFSCmd = null;
        } else if (new MessageFormat(s).getFormats().length == 2) {
            onSwitchStorageFSCmd = s;
        } else {
            throw new IllegalArgumentException(command);
        }
    }

    protected void startService() throws Exception {
        server.addNotificationListener(fileSystemMgtName, this,
                StorageFileSystemSwitched.NOTIF_FILTER, null);

    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(fileSystemMgtName, this,
                StorageFileSystemSwitched.NOTIF_FILTER, null);
    }

    public void handleNotification(Notification notif, Object handback) {
        if (onSwitchStorageFSCmd == null) {
            return;
        }
        StorageFileSystemSwitched sfss = 
            (StorageFileSystemSwitched) notif.getUserData();
        final String cmd = MessageFormat.format(onSwitchStorageFSCmd,
                sfss.getPreviousStorageFileSystem().getDirectoryPath()
                        .replace('/', File.separatorChar),
                sfss.getNewStorageFileSystem().getDirectoryPath()
                        .replace('/', File.separatorChar));
        new Thread(new Runnable() {
            public void run() {
                execute(cmd);
            }
        }).start();
    }

    private void execute(final String cmd) {
        try {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            Executer ex = new Executer(cmd, stdout, null);
            int exit = ex.waitFor();
            if (exit != 0) {
                log.info("Non-zero exit code(" + exit + ") of " + cmd);
            }
        } catch (Exception e) {
            log.error("Failed to execute " + cmd, e);
        }
    }
}
