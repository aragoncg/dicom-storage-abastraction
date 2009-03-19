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

import java.sql.Timestamp;

import javax.ejb.CreateException;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.common.FileStatus;
import org.dcm4chex.archive.common.FileSystemStatus;
import org.dcm4chex.archive.ejb.interfaces.FileSystemLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;

/**
 * @author <a href="mailto:gunter@tiani.com">Gunter Zeilinger</a>
 * @version $Revision: 7974 $ $Date: 2008-11-07 09:54:45 +0100 (Fri, 07 Nov 2008) $
 * @since 01.01.2005
 * 
 * @ejb.bean name="StudyOnFileSystem"
 *          type="CMP"
 *          view-type="local"
 *          local-jndi-name="ejb/StudyOnFileSystem"
 *          primkey-field="pk"
 * @ejb.transaction type="Required"
 * @ejb.persistence table-name="study_on_fs"
 * @jboss.entity-command name="hsqldb-fetch-key"
 * 
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.StudyOnFileSystemLocal findByStudyAndFileSystem(java.lang.String suid, java.lang.String dirPath)"
 * 	           query="" transaction-type="Supports"
 * @jboss.query signature="org.dcm4chex.archive.ejb.interfaces.StudyOnFileSystemLocal findByStudyAndFileSystem(java.lang.String suid, java.lang.String dirPath)"
 *             query="SELECT OBJECT(sof) FROM StudyOnFileSystem sof WHERE sof.study.studyIuid=?1 AND sof.fileSystem.directoryPath=?2"
 *             strategy="on-find" eager-load-group="*"
 * @ejb.finder signature="org.dcm4chex.archive.ejb.interfaces.StudyOnFileSystemLocal findByStudyAndFileSystem(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, org.dcm4chex.archive.ejb.interfaces.FileSystemLocal filesystem)"
 *             query="SELECT OBJECT(sof) FROM StudyOnFileSystem sof WHERE sof.study=?1 AND sof.fileSystem=?2" transaction-type="Supports"
 * @ejb.finder signature="java.util.Collection findByStudyIUIDAndFSGroup(java.lang.String siuid, java.lang.String groupId)"
 *             query="SELECT OBJECT(sof) FROM StudyOnFileSystem sof WHERE sof.study.studyIuid=?1 AND sof.fileSystem.groupID=?2"
 *             transaction-type="Supports"
 * @ejb.finder signature="java.util.Collection findByFSGroupAndAccessedBetween(java.lang.String groupId, java.sql.Timestamp tsAfter, java.sql.Timestamp tsBefore, int limit )"
 *             query="" transaction-type="Supports"
 * @jboss.query signature="java.util.Collection findByFSGroupAndAccessedBetween(java.lang.String groupId, java.sql.Timestamp tsAfter, java.sql.Timestamp tsBefore, int limit )"
 *              query="SELECT DISTINCT OBJECT(sof) FROM StudyOnFileSystem sof, IN (sof.study.series) s WHERE sof.fileSystem.groupID = ?1 AND sof.fileSystem.status IN (0,1) AND sof.accessTime > ?2 AND sof.accessTime < ?3 AND s.seriesStatus = 0 ORDER BY sof.accessTime ASC LIMIT ?4"
 *              strategy="on-find" eager-load-group="*"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatusAndFileStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fsStatus, int fileStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID = ?2 AND f.fileSystem.status = ?3 AND f.fileStatus = ?4"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndFileStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fileStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID = ?2 AND f.fileStatus = ?3"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fsStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID = ?2 AND f.fileSystem.status = ?3"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupId(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID = ?2"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatusAndFileStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fsStatus, int fileStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID <> ?2 AND f.fileSystem.status = ?3 AND f.fileStatus = ?4"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndFileStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fileStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID <> ?2 AND f.fileStatus = ?3"
 * @jboss.query signature="int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatus(org.dcm4chex.archive.ejb.interfaces.StudyLocal study, java.lang.String fsGroupId, int fsStatus)"
 *              query="SELECT COUNT(DISTINCT i) FROM Instance i, IN(i.files) f WHERE i.series.study = ?1 AND f.fileSystem.groupID <> ?2 AND f.fileSystem.status = ?3"
 */
public abstract class StudyOnFileSystemBean implements EntityBean {

    private static final Logger log = Logger
            .getLogger(StudyOnFileSystemBean.class);

    /**
     * @ejb.interface-method
     * @ejb.pk-field
     * @ejb.persistence column-name="pk"
     * @jboss.persistence auto-increment="true"
     */
    public abstract Long getPk();

    public abstract void setPk(Long pk);

    /**
     * @ejb.interface-method
     * @ejb.persistence column-name="access_time"
     */
    public abstract java.sql.Timestamp getAccessTime();

    public abstract void setAccessTime(java.sql.Timestamp time);

    /**
     * @ejb.interface-method
     * @ejb.relation name="study-sof"
     *               role-name="sof-of-study"
     *               cascade-delete="yes"
     *               target-ejb="Study"
     *               target-role-name="study-of-sof"
     *               target-multiple="yes" 
     * @jboss.relation fk-column="study_fk" related-pk-field="pk"
     */
    public abstract StudyLocal getStudy();

    public abstract void setStudy(StudyLocal study);

    /**
     * @ejb.interface-method
     * @ejb.relation name="filesystem-sof"
     *               role-name="sof-of-filesystem"
     *               cascade-delete="yes" 
     * 	             target-ejb="FileSystem"
     *               target-role-name="filesystem-of-sof"
     *               target-multiple="yes"
     * @jboss.relation fk-column="filesystem_fk" 
     *                 related-pk-field="pk"
     */
    public abstract FileSystemLocal getFileSystem();

    public abstract void setFileSystem(FileSystemLocal fs);

    /**
     * @ejb.create-method
     */
    public Long ejbCreate(StudyLocal study, FileSystemLocal fs)
            throws CreateException {
        setAccessTime(new Timestamp(System.currentTimeMillis()));
        return null;
    }

    public void ejbPostCreate(StudyLocal study, FileSystemLocal fs)
            throws CreateException {
        setStudy(study);
        setFileSystem(fs);
    }

    /**
     * @ejb.interface-method
     */
    public void touch() {
        setAccessTime(new Timestamp(System.currentTimeMillis()));
    }

    /**
     * @ejb.interface-method
     */
    public String asString() {
        StudyLocal study = getStudy();
        FileSystemLocal fs = getFileSystem();
        return "StudyOnFileSystem[" 
        	+ (study == null ? "null" : study.asString())
        	+ "@"
        	+ (fs == null ? "null" : fs.asString())
        	+ "]"; 
    }

    /**    
     * @ejb.interface-method
     */
    public boolean matchDeleteConstrains(
            boolean externalRetrieveable,
            boolean storageNotCommited,
            boolean copyOnMedia,
            String copyOnFSGroup,
            boolean copyArchived,
            boolean copyOnReadOnlyFS) throws FinderException {
        StudyLocal study = getStudy();
        if (externalRetrieveable && !study.isStudyExternalRetrievable()
                || storageNotCommited && study.getNumberOfCommitedInstances() != 0
                || copyOnMedia && !study.isStudyAvailableOnMedia()) {
            return false;
        }
        int count;
        if (copyOnFSGroup != null) {
            if (copyArchived) {
                if (copyOnReadOnlyFS) {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatusAndFileStatus(
                            study, copyOnFSGroup, FileSystemStatus.RO, FileStatus.ARCHIVED);
                } else {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndFileStatus(
                            study, copyOnFSGroup, FileStatus.ARCHIVED);
                }
            } else {
                if (copyOnReadOnlyFS) {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatus(
                            study, copyOnFSGroup, FileSystemStatus.RO);
                } else {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupId(
                            study, copyOnFSGroup);
                }
            }
        } else {
            if (copyArchived) {
                if (copyOnReadOnlyFS) {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatusAndFileStatus(
                            study, getFileSystem().getGroupID(), FileSystemStatus.RO, FileStatus.ARCHIVED);
                } else {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndFileStatus(
                            study, getFileSystem().getGroupID(), FileStatus.ARCHIVED);
                }
            } else {
                if (copyOnReadOnlyFS) {
                    count = ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatus(
                            study, getFileSystem().getGroupID(), FileSystemStatus.RO);
                } else {
                    return true; // no constraint
                }
            }
        }
        return count == study.getNumberOfStudyRelatedInstances();
    }

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatusAndFileStatus(
            StudyLocal study, String fsGroupId, int fsStatus, int fileStatus)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndFileStatus(
            StudyLocal study, String fsGroupId, int fileStatus)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupIdAndStatus(
            StudyLocal study, String fsGroupId, int fsStatus)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithGroupId(
            StudyLocal study, String fsGroupId)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatusAndFileStatus(
            StudyLocal study, String fsGroupId, int fsStatus, int fileStatus)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndFileStatus(
            StudyLocal study, String fsGroupId, int fileStatus)
            throws FinderException;

    /**
     * @ejb.select query=""
     */ 
    public abstract int ejbSelectNumberOfStudyRelatedInstancesOnFSWithDifferentGroupIdAndStatus(
            StudyLocal study, String fsGroupId, int fsStatus)
            throws FinderException;
}
