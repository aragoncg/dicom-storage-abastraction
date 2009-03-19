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

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EntityBean;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.common.FileSystemStatus;
import org.dcm4chex.archive.ejb.interfaces.FileSystemDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocal;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 7589 $ $Date: 2008-10-16 15:51:02 +0200 (Thu, 16 Oct 2008) $
 * @since 31.08.2004
 *
 * @ejb.bean name="FileSystem"
 *           type="CMP"
 *           view-type="local"
 *           primkey-field="pk"
 *           local-jndi-name="ejb/FileSystem"
 * 
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="filesystem"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * @jboss.read-ahead strategy="on-find" eager-load-group="*"
 * 
 * @ejb.finder signature="java.util.Collection findAll()"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.FileSystemLocal findByDirectoryPath(java.lang.String path)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.directoryPath = ?1"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.FileSystemLocal findByGroupIdAndDirectoryPath(java.lang.String groupID, java.lang.String path)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.groupID = ?1 AND fs.directoryPath = ?2"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="java.util.Collection findByLikeDirectoryPath(java.lang.String path, int availability, int status)"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByLikeDirectoryPath(java.lang.String path, int availability, int status)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.directoryPath LIKE ?1 AND fs.availability = ?2 AND fs.status = ?3"
 *
 * @ejb.finder signature="java.util.Collection findByGroupId(java.lang.String groupID)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.groupID = ?1"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="java.util.Collection findByGroupIdAndStatus(java.lang.String groupID, int status)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.groupID = ?1 AND fs.status = ?2"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="java.util.Collection findRWByGroupId(java.lang.String groupID)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.groupID = ?1 AND fs.status IN (0,1)"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="java.util.Collection findByRetrieveAET(java.lang.String aet)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.retrieveAET = ?1"
 *             transaction-type="Supports"
 *             
 * @ejb.finder signature="java.util.Collection findByRetrieveAETAndAvailabilityAndStatus(java.lang.String aet, int availability, int status)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.retrieveAET = ?1 AND fs.availability = ?2 AND fs.status = ?3"
 *             transaction-type="Supports"
 *
 * @ejb.finder signature="java.util.Collection findByRetrieveAETAndAvailabilityAndStatus2(java.lang.String aet, int availability, int status, int alt)"
 *             query="SELECT OBJECT(fs) FROM FileSystem AS fs WHERE fs.retrieveAET = ?1 AND fs.availability = ?2 AND (fs.status = ?3 OR fs.status = ?4)"
 *             transaction-type="Supports"
 *             
 * @jboss.query signature="int ejbSelectNumberOfFiles(java.lang.Long pk)"
 *              query="SELECT COUNT(f) FROM File f WHERE f.fileSystem.pk = ?1"
 * @jboss.query signature="int ejbSelectNumberOfPrivateFiles(java.lang.Long pk)"
 *              query="SELECT COUNT(f) FROM PrivateFile f WHERE f.fileSystem.pk = ?1"
 *              
 * @jboss.query signature="Long ejbSelectSizeOfFilesCreatedAfter(java.lang.Long pk, java.sql.Timestamp createdAfter)"
 *              query="SELECT SUM(f.fileSize) FROM File f WHERE f.fileSystem.pk = ?1 AND f.createdTime > ?2"
 *
 * @jboss.query signature="java.util.Collection ejbSelectAllRetrieveAETs()"
 *              query="SELECT DISTINCT f.retrieveAET FROM FileSystem f"
 */
public abstract class FileSystemBean implements EntityBean {

    private static final Logger log = Logger.getLogger(FileSystemBean.class);
    private EntityContext ctx;


    public void setEntityContext(EntityContext ctx) 
    throws EJBException, RemoteException {
        this.ctx = ctx;     
    }

    public void unsetEntityContext() throws EJBException, RemoteException {
        this.ctx = null;        
    }
    
    /**
     * Create File System.
     * 
     * @ejb.create-method
     */
    public Long ejbCreate(FileSystemDTO dto) throws CreateException {
        fromDTO(dto);
        return null;
    }

    public void ejbPostCreate(FileSystemDTO dto) throws CreateException {
        log.info("Created " + asString());
    }

    public void ejbRemove() throws RemoveException {
        log.info("Deleting " + asString());
    }   
    
    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfFiles(Long pk)
    throws FinderException;
    
    /**
     * @ejb.interface-method
     */
    public int countFiles() throws FinderException {
        return ejbSelectNumberOfFiles(getPk());
    }
    
    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfPrivateFiles(Long pk)
    throws FinderException;
    
    /**
     * @ejb.select query=""
     */ 
    public abstract Long ejbSelectSizeOfFilesCreatedAfter(Long pk, Timestamp createdAfter)
    throws FinderException;
    
    /**
     * @ejb.home-method
     */
    public long ejbHomeSizeOfFilesCreatedAfter(Long pk, Timestamp createdAfter)
    throws FinderException {
        try {
            Long sum = ejbSelectSizeOfFilesCreatedAfter(pk, createdAfter);
            return sum != null ? sum.longValue() : 0L;
        } catch (Exception e) {
            log.info("Failed - assume no matching file records");
            return 0L;
        }
    }

    /**
     * @ejb.select query=""
     */ 
    public abstract Collection ejbSelectAllRetrieveAETs()
    throws FinderException;

    /**
     * Get all available distinct retrieve AETs registered with file systems
     * 
     * @ejb.home-method
     */
    public Collection ejbHomeAllRetrieveAETs() throws FinderException {
        return ejbSelectAllRetrieveAETs();
    }
    
    /**
     * @ejb.interface-method
     */
    public int countPrivateFiles() throws FinderException {
        return ejbSelectNumberOfPrivateFiles(getPk());
    }
    
    /**
     * @ejb.interface-method
     */ 
    public String asString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileSystem[pk=").append(getPk());
        sb.append(", ").append(getDirectoryPath());
        sb.append(", groupID=").append(getGroupID());
        sb.append(", aet=").append(getRetrieveAET());
        sb.append(", ").append(Availability.toString(getAvailability()));
        sb.append(", ").append(FileSystemStatus.toString(getStatus()));
        sb.append(", userinfo=").append(getUserInfo());
        FileSystemLocal next = getNextFileSystem();
        if (next != null)
            sb.append(", next=").append(next.getDirectoryPath());
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Auto-generated Primary Key
     *
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     *
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="dirpath"
     */
    public abstract String getDirectoryPath();

    /**
     * @ejb.interface-method
     */ 
    public abstract void setDirectoryPath(String dirpath);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="fs_group_id"
     */
    public abstract String getGroupID();

    /**
     * @ejb.interface-method
     */ 
    public abstract void setGroupID(String id);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="retrieve_aet"
     */
    public abstract String getRetrieveAET();

    /**
     * @ejb.interface-method
     */ 
    public abstract void setRetrieveAET(String aet);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="availability"
     */
    public abstract int getAvailability();

    /**
     * @ejb.interface-method
     */
    public abstract void setAvailability(int availability);
    
    /**
     * @ejb.interface-method
     */
    public int getAvailabilitySafe() {
        try {
            return getAvailability();
        } catch (NullPointerException npe) {
            return 0;
        }
    }
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="fs_status"
     */
    public abstract int getStatus();

    /**
     * @ejb.interface-method
     */
    public abstract void setStatus(int status);
    
    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="user_info"
     */
    public abstract String getUserInfo();

    /**
     * @ejb.interface-method
     */ 
    public abstract void setUserInfo(String info);


    /**
     * @ejb.interface-method
     * @ejb.relation name="next-filesystem"
     *    role-name="prev-filesystem"
     *
     * @jboss.relation fk-column="next_fk" related-pk-field="pk"
     */
    public abstract FileSystemLocal getNextFileSystem();

    /**
     * @ejb.interface-method
     */
    public abstract void setNextFileSystem(FileSystemLocal filesystem);
    
    /**
     * @ejb.interface-method
     * @ejb.relation name="next-filesystem"
     *    role-name="next-filesystem"
     */
    public abstract java.util.Collection getPreviousFileSystems();

    /**
     * @ejb.interface-method
     */
    public abstract void setPreviousFileSystems(java.util.Collection previous);
    
    /**
     * @ejb.interface-method
     */
    public void fromDTO(FileSystemDTO dto) {
        setDirectoryPath(dto.getDirectoryPath());
        setGroupID(dto.getGroupID());
        setRetrieveAET(dto.getRetrieveAET());
        setAvailability(dto.getAvailability());
        setStatus(dto.getStatus());
        setUserInfo(dto.getUserInfo());
    }
    /**
     * @ejb.interface-method
     */
    public FileSystemDTO toDTO() {
        FileSystemDTO dto = new FileSystemDTO();
        dto.setPk(getPk().longValue());
        dto.setDirectoryPath(getDirectoryPath());
        dto.setGroupID(getGroupID());
        dto.setRetrieveAET(getRetrieveAET());
        dto.setAvailability(getAvailability());
        dto.setStatus(getStatus());
        dto.setUserInfo(getUserInfo());
        FileSystemLocal next = getNextFileSystem();
        if (next != null) {
            // prevent reentry in case of next == this
            String nextPath = next.isIdentical(ctx.getEJBLocalObject()) 
                    ? getDirectoryPath() : next.getDirectoryPath();
            dto.setNext(nextPath);
        }
        return dto;
    }
}
