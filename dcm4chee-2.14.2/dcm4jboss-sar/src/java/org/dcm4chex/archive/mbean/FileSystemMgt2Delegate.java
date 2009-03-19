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

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Aug 28, 2008
 */
public class FileSystemMgt2Delegate {

    private final ServiceMBeanSupport service;
    private String fileSystemMgtServiceNamePrefix =
            "dcm4chee.archive:service=FileSystemMgt,group=";

    public FileSystemMgt2Delegate(ServiceMBeanSupport service) {
        this.service = service;
    }

    public boolean isFileSystemGroupLocalAccessable(String groupID) {
        return service.getServer().isRegistered(toObjectName(groupID));
    }

    private ObjectName toObjectName(String groupID) {
        try {
            return new ObjectName(fileSystemMgtServiceNamePrefix + groupID);
        } catch (MalformedObjectNameException e) {
            throw new IllegalArgumentException(groupID);
        }
    }

    public String getFileSystemMgtServiceNamePrefix() {
        return fileSystemMgtServiceNamePrefix;
    }

    public void setFileSystemMgtServiceNamePrefix(String prefix) {
        this.fileSystemMgtServiceNamePrefix = prefix;
    }

    public FileSystemDTO selectStorageFileSystem(String groupID) {
        try {
            return (FileSystemDTO) service.getServer().invoke(
                    toObjectName(groupID), "selectStorageFileSystem", null, null);
        } catch (JMException e) {
            throw new RuntimeException(
                    "Failed to invoke 'selectStorageFileSystem'", e);
        }
    }
}
