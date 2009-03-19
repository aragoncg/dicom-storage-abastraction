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

package org.dcm4chex.archive.ejb.entity;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.MD5;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 7589 $ $Date: 2008-10-16 15:51:02 +0200 (Thu, 16 Oct 2008) $
 * 
 * @ejb.bean name="File" type="CMP" view-type="local" primkey-field="pk"
 * 	         local-jndi-name="ejb/File"
 * @ejb.persistence table-name="files"
 * @ejb.transaction type="Required"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.audit-created-time field-name="createdTime"
 * 
 * @ejb.finder signature="java.util.Collection findFilesToCompress(java.lang.String dirPath, java.lang.String cuid, java.sql.Timestamp before, int limit)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findFilesToCompress(java.lang.String dirPath, java.lang.String cuid, java.sql.Timestamp before, int limit)"
 *             query="SELECT OBJECT(f) FROM File AS f WHERE f.fileStatus = 0 AND f.fileTsuid IN ('1.2.840.10008.1.2','1.2.840.10008.1.2.1','1.2.840.10008.1.2.2') AND f.fileSystem.directoryPath = ?1 AND f.instance.sopCuid = ?2 AND (f.createdTime IS NULL OR f.createdTime < ?3) LIMIT ?4"
 *             strategy="on-find" eager-load-group="*"
 * @ejb.finder signature="java.util.Collection findFilesToCompress(long fspk, java.lang.String cuid, java.sql.Timestamp before, int limit)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findFilesToCompress(long fspk, java.lang.String cuid, java.sql.Timestamp before, int limit)"
 *             query="SELECT OBJECT(f) FROM File AS f WHERE f.fileStatus = 0 AND f.fileTsuid IN ('1.2.840.10008.1.2','1.2.840.10008.1.2.1','1.2.840.10008.1.2.2') AND f.fileSystem.pk = ?1 AND f.instance.sopCuid = ?2 AND (f.createdTime IS NULL OR f.createdTime < ?3) LIMIT ?4"
 *             strategy="on-find" eager-load-group="*"
 * @ejb.finder signature="java.util.Collection findToCheckMd5(java.lang.String dirPath, java.sql.Timestamp before, int limit)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findToCheckMd5(java.lang.String dirPath, java.sql.Timestamp before, int limit)"
 *             query="SELECT OBJECT(f) FROM File AS f WHERE f.fileSystem.directoryPath = ?1 AND f.fileMd5Field IS NOT NULL AND (f.timeOfLastMd5Check IS NULL OR f.timeOfLastMd5Check < ?2) LIMIT ?3"
 *             strategy="on-find" eager-load-group="*"
 * @ejb.finder signature="java.util.Collection findByStatusAndFileSystem(java.lang.String dirPath, int status, java.sql.Timestamp before, int limit)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByStatusAndFileSystem(java.lang.String dirPath, int status, java.sql.Timestamp before, int limit)"
 *             query="SELECT OBJECT(f) FROM File AS f WHERE f.fileSystem.directoryPath = ?1 AND f.fileStatus = ?2 AND (f.createdTime IS NULL OR f.createdTime < ?3) ORDER BY f.pk LIMIT ?4"
 *             strategy="on-find" eager-load-group="*"
 * @ejb.finder signature="java.util.Collection findByFileSystem(java.lang.String dirPath, int offset, int limit)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByFileSystem(java.lang.String dirPath, int offset, int limit)"
 *             query="SELECT OBJECT(f) FROM File AS f WHERE f.fileSystem.directoryPath = ?1 ORDER BY f.pk OFFSET ?2 LIMIT ?3"
 *             strategy="on-find" eager-load-group="*"
 */
public abstract class FileBean implements EntityBean {

    private static final Logger log = Logger.getLogger(FileBean.class);

    /**
     * Auto-generated Primary Key
     * 
     * @ejb.interface-method 
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="created_time"
     */
    public abstract java.sql.Timestamp getCreatedTime();

    public abstract void setCreatedTime(java.sql.Timestamp time);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="md5_check_time"
     */
    public abstract java.sql.Timestamp getTimeOfLastMd5Check();

    /**
     * @ejb.interface-method
     */
    public abstract void setTimeOfLastMd5Check(java.sql.Timestamp time);

    /**
     * File Path (relative path to Directory).
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="filepath"
     */
    public abstract String getFilePath();

    /**
     * @ejb.interface-method
     */
    public abstract void setFilePath(String path);

    /**
     * Transfer Syntax UID
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="file_tsuid"
     */
    public abstract String getFileTsuid();

    /**
     * @ejb.interface-method
     */
    public abstract void setFileTsuid(String tsuid);

    /**
     * MD5 checksum as hex string
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="file_md5"
     */
    public abstract String getFileMd5Field();

    public abstract void setFileMd5Field(String md5);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="file_status"
     */
    public abstract int getFileStatus();

    /**
     * @ejb.interface-method
     */
    public abstract void setFileStatus(int status);

    /**
     * MD5 checksum in binary format
     * 
     * @ejb.interface-method
     */
    public byte[] getFileMd5() {
        return MD5.toBytes(getFileMd5Field());
    }

    /**
     * @ejb.interface-method
     */
    public void setFileMd5(byte[] md5) {
        setFileMd5Field(MD5.toString(md5));
    }

    /**
     * File Size
     * 
     * @ejb.interface-method
     * @ejb.persistence column-name="file_size"
     */
    public abstract long getFileSize();

    /**
     * @ejb.interface-method
     */
    public abstract void setFileSize(long size);

    /**
     * @ejb.interface-method
     * @ejb.relation name="instance-files"
     * 	             role-name="files-of-instance"
     * @jboss.relation fk-column="instance_fk"
     * 	               related-pk-field="pk"
     */
    public abstract void setInstance(InstanceLocal inst);

    /**
     * @ejb.interface-method
     */
    public abstract InstanceLocal getInstance();

    /**
     * @ejb.interface-method
     * @ejb.relation name="filesystem-files"
     * 	             role-name="files-of-filesystem"
     *               target-role-name="filesystem-of-file"
     *               target-ejb="FileSystem"
     *               target-multiple="yes"
     * @jboss.relation fk-column="filesystem_fk"
     * 	               related-pk-field="pk"
     */
    public abstract void setFileSystem(FileSystemLocal fs);

    /**
     * @ejb.interface-method
     */
    public abstract FileSystemLocal getFileSystem();

    /**
     * @ejb.interface-method
     */
    public boolean isRedundant() {
        InstanceLocal inst = getInstance();
        return inst == null || inst.getFiles().size() > 1;
    }

    /**
     * @ejb.interface-method
     * @jboss.method-attributes read-only="true"
     */
    public FileDTO getFileDTO() {
        FileSystemLocal fs = getFileSystem();
        FileDTO retval = new FileDTO();
        retval.setPk(getPk().longValue());
        retval.setRetrieveAET(fs.getRetrieveAET());
        retval.setFileSystemPk(fs.getPk().longValue());
        retval.setFileSystemGroupID(fs.getGroupID());
        retval.setDirectoryPath(fs.getDirectoryPath());
        retval.setAvailability(fs.getAvailability());
        retval.setUserInfo(fs.getUserInfo());
        retval.setFilePath(getFilePath());
        retval.setFileTsuid(getFileTsuid());
        retval.setFileSize(getFileSize());
        retval.setFileMd5(getFileMd5());
        retval.setFileStatus(getFileStatus());
        
        InstanceLocal inst = getInstance();
        if (inst != null)
            retval.setSopClassUID(inst.getSopCuid());
        return retval;
    }

    /**
     * @ejb.interface-method
     */
    public String asString() {
        return prompt();
    }

    private String prompt() {
        return "File[pk=" + getPk() + ", filepath=" + getFilePath()
                + ", tsuid=" + getFileTsuid() + ", filesystem->"
                + getFileSystem() + ", inst->" + getInstance() + "]";
    }

    /**
     * Create file.
     * 
     * @ejb.create-method
     */
    public Long ejbCreate(String path, String tsuid, long size, byte[] md5,
    		int status, InstanceLocal instance, FileSystemLocal filesystem)
            throws CreateException {
        setFilePath(path);
        setFileTsuid(tsuid);
        setFileSize(size);
        setFileMd5(md5);
        setFileStatus(status);
        return null;
    }

    public void ejbPostCreate(String path, String tsuid, long size, byte[] md5,
    		int status, InstanceLocal instance, FileSystemLocal filesystem)
            throws CreateException {
        setInstance(instance);
        setFileSystem(filesystem);
        log.info("Created " + prompt());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + prompt());
    }
}
