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

package org.dcm4chex.archive.ejb.session;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.ejb.interfaces.FileLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocal;
import org.dcm4chex.archive.ejb.interfaces.InstanceLocalHome;
import org.dcm4chex.archive.ejb.interfaces.MediaDTO;
import org.dcm4chex.archive.ejb.interfaces.MediaLocal;
import org.dcm4chex.archive.ejb.interfaces.MediaLocalHome;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocal;
import org.dcm4chex.archive.ejb.interfaces.SeriesLocalHome;
import org.dcm4chex.archive.ejb.interfaces.StudyLocal;
import org.dcm4chex.archive.ejb.interfaces.StudyLocalHome;

/**
 * @ejb.bean name="MediaComposer" type="Stateless" view-type="remote"
 *           jndi-name="ejb/MediaComposer"
 * 
 * @ejb.transaction-type type="Container"
 * @ejb.transaction type="Required"
 * 
 * @ejb.ejb-ref ejb-name="Media" view-type="local" ref-name="ejb/Media"
 * 
 * @ejb.ejb-ref ejb-name="Instance" view-type="local" ref-name="ejb/Instance"
 * 
 * @ejb.ejb-ref ejb-name="Study" view-type="local" ref-name="ejb/Study"
 * 
 * @ejb.ejb-ref ejb-name="Series" view-type="local" ref-name="ejb/Series"
 * 
 * @author gunter.zeilinger@tiani.com
 * @version Revision $Date: 2008-11-12 14:31:15 +0100 (Wed, 12 Nov 2008) $
 * @since 14.12.2004
 */

public abstract class MediaComposerBean implements SessionBean {

    private static Logger log = Logger.getLogger(MediaComposerBean.class
            .getName());

    private MediaLocalHome mediaHome;

    private InstanceLocalHome instHome;

    private StudyLocalHome studyHome;

    private SeriesLocalHome seriesHome;

    /**
     * Initialize this class.
     * <p>
     * Set the home interfaces for MediaLocal and InstanceLocal.
     * 
     * @param arg0
     *            The session context.
     */
    public void setSessionContext(SessionContext arg0) throws EJBException,
            RemoteException {
        Context jndiCtx = null;
        try {
            jndiCtx = new InitialContext();
            mediaHome = (MediaLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Media");
            instHome = (InstanceLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Instance");
            studyHome = (StudyLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Study");
            seriesHome = (SeriesLocalHome) jndiCtx
                    .lookup("java:comp/env/ejb/Series");
        } catch (NamingException e) {
            throw new EJBException(e);
        } finally {
            if (jndiCtx != null) {
                try {
                    jndiCtx.close();
                } catch (NamingException ignore) {
                }
            }
        }
    }

    /**
     * Set the home interfaces to null.
     * 
     */
    public void unsetSessionContext() {
        mediaHome = null;
        instHome = null;
    }

    /**
     * @ejb.interface-method
     */
    public Collection getStudiesReceivedBefore(long time)
            throws FinderException {
        return studyHome.findStudiesNotOnMedia(new Timestamp(time));
    }

    /**
     * @throws FinderException
     * @throws CreateException
     * @ejb.interface-method
     */
    public List assignStudyToMedia(StudyLocal study, List mediaPool,
            long maxMediaSize, String prefix) throws FinderException,
            CreateException {
        if (mediaPool == null) {
            mediaPool = getCollectingMedia();
        }
        Map instanceFiles = new HashMap();
        InstanceLocal instance;
        FileLocal file, file2;
        int avail, avail2;
        Collection files;
        long size = 0;
        for (Iterator iter = study.getInstancesNotOnMedia().iterator(); iter
                .hasNext();) {
            instance = (InstanceLocal) iter.next();
            files = instance.getFiles();
            if (files.isEmpty()) {
                log.warn("Instance " + instance.getPk() + "("
                        + instance.getSopIuid() + ") has no files! ignored!");
                continue;
            }
            Iterator it = files.iterator();
            for (file = (FileLocal) it.next(); it.hasNext();) {
                file2 = (FileLocal) it.next();
                avail = file.getFileSystem().getAvailability();
                avail2 = file2.getFileSystem().getAvailability();
                if (avail2 < avail) {
                    file = file2;
                } else if (file2.getPk().longValue() > file.getPk().longValue()) {
                    file = file2;
                }
            }
            size += file.getFileSize();
            instanceFiles.put(instance, file);
        }
        if (size > maxMediaSize) {
            log.info("Study size (" + size + ") exceed maxMediaSize ("
                    + maxMediaSize + ")! Split study is necessary!");
            splitStudy(instanceFiles, mediaPool, maxMediaSize, prefix);
        } else {
            log.debug("assign study " + study.getStudyIuid() + " (size=" + size
                    + ") to media");
            assignInstancesToMedia(instanceFiles.keySet(), size, getMedia(
                    mediaPool, maxMediaSize - size, prefix));
        }
        return mediaPool;
    }

    /**
     * 
     * @param instanceFiles
     *            map with all instances for a media.
     * @param maxUsed
     *            The max mediaUsage a media must have to can store all
     *            instances.
     * @param mediaPool
     *            List of COLLECTING media
     * @param prefix
     *            Prefix for Fileset ID. (Used if a new media must be created.
     * @throws CreateException
     */
    private void assignInstancesToMedia(Set instances, long size,
            MediaLocal media) throws CreateException {
        log.debug("assign instances (" + instances + ") to media " + media);
        for (Iterator iter = instances.iterator(); iter.hasNext();) {
            ((InstanceLocal) iter.next()).setMedia(media);
        }
        media.setMediaUsage(media.getMediaUsage() + size);
    }

    private MediaLocal getMedia(List mediaPool, long maxUsed, String prefix)
            throws CreateException {
        maxUsed++;
        if (mediaPool.size() > 0) {
            MediaLocal media;
            for (Iterator iter = mediaPool.iterator(); iter.hasNext();) {
                media = (MediaLocal) iter.next();
                if (media.getMediaUsage() < maxUsed)
                    return media;
            }
        }
        log.debug("We need new media! mediaPool:" + mediaPool);
        MediaLocal media = createMedia(prefix);
        mediaPool.add(media);
        return media;

    }

    /**
     * @param instanceFiles
     * @param maxMediaSize
     * @param prefix
     * @throws CreateException
     */
    private void splitStudy(Map instanceFiles, List mediaPool,
            long maxMediaSize, String prefix) throws CreateException {
        Map.Entry entry;
        log.info("Split study!");
        long fileSize, size = 0;
        Set instances = new HashSet();
        for (Iterator iter = instanceFiles.entrySet().iterator(); iter
                .hasNext();) {
            entry = (Map.Entry) iter.next();
            fileSize = ((FileLocal) entry.getValue()).getFileSize();
            size += fileSize;
            if (size > maxMediaSize) {
                log.debug("assign instances (" + instances.size() + "/"
                        + instanceFiles.size() + ") to new media!");
                assignInstancesToMedia(instances, size, createMedia(prefix));
                size = fileSize;
                instances.clear();
            }
            instances.add(entry.getKey());
        }
        if (!instances.isEmpty()) {
            log.debug("assign remaining instances (" + instances.size() + "/"
                    + instanceFiles.size() + ") to new media!");
            assignInstancesToMedia(instances, size, getMedia(mediaPool,
                    maxMediaSize - size, prefix));
        }
    }

    /**
     * 
     * @ejb.interface-method
     */
    public List getCollectingMedia() throws FinderException {
        List mediaCollection = (List) mediaHome.findByStatus(MediaDTO.OPEN);
        Comparator comp = new Comparator() {
            public int compare(Object arg0, Object arg1) {
                MediaLocal ml1 = (MediaLocal) arg0;
                MediaLocal ml2 = (MediaLocal) arg1;
                return (int) (ml2.getMediaUsage() - ml1.getMediaUsage());// more
                                                                         // usage
                                                                         // before
                                                                         // lower
                                                                         // usage
                                                                         // !
            }
        };
        Collections.sort(mediaCollection, comp);
        log.debug("Number of 'COLLECTING' media found:"
                + mediaCollection.size());
        return mediaCollection;
    }

    /**
     * Creates a new media.
     * <p>
     * Set the fileset id with given prefix and the pk of the new media.
     * 
     * @param prefix
     *            A prefix for fileset id.
     * 
     * @return The new created MediaLocal bean.
     * @throws CreateException
     */
    private MediaLocal createMedia(String prefix) throws CreateException {
        MediaLocal ml = mediaHome
                .create(UIDGenerator.getInstance().createUID());
        ml.setFilesetId(prefix + ml.getPk());
        ml.setMediaStatus(MediaDTO.OPEN);
        if (log.isInfoEnabled())
            log.info("New media created:" + ml.getFilesetId());
        return ml;
    }

    /**
     * Returns a list of all media with the given media status.
     * <p>
     * The list contains a MediaDTO object for each media with the given status.
     * 
     * @param status
     *            The media status
     * 
     * @return A list of MediaDTO objects.
     * 
     * @ejb.interface-method
     */
    public List getWithStatus(int status) throws FinderException {
        return toMediaDTOs(mediaHome.findByStatus(status));
    }

    /**
     * Find media for given search params.
     * <p>
     * Add all founded media to the given collection.<br>
     * This allows to fill a collection with sequential calls without clearing
     * the collection.<br>
     * 
     * @param col
     *            The collection to store the result.
     * @param after
     *            'created after' Timestamp in milliseconds
     * @param before
     *            'created before' Timestamp in milliseconds
     * @param stati
     *            Media status (<code>null</code> to get all media for given
     *            time range)
     * @param offset
     *            Offset of the find result. (used for paging.
     * @param limit
     *            Max. number of results to return. (used for paging)
     * @param desc
     *            Sort order. if true descending, false ascending order.
     * 
     * @return The total number of search results.
     * 
     * @ejb.interface-method
     */
    public int findByCreatedTime(Collection col, Long after, Long before,
            int[] stati, Integer offset, Integer limit, boolean desc)
            throws FinderException {
        Timestamp tsAfter = null;
        if (after != null)
            tsAfter = new Timestamp(after.longValue());
        Timestamp tsBefore = null;
        if (before != null)
            tsBefore = new Timestamp(before.longValue());
        col.addAll(toMediaDTOs(mediaHome.listByCreatedTime(stati, tsAfter,
                tsBefore, offset, limit, desc)));
        return mediaHome.countByCreatedTime(stati, tsAfter, tsBefore);
    }

    /**
     * Find media for given search params.
     * <p>
     * Add all founded media to the given collection.<br>
     * This allows to fill a collection with sequential calls without clearing
     * the collection.<br>
     * 
     * @param col
     *            The collection to store the result.
     * @param after
     *            'updated after' Timestamp in milliseconds
     * @param before
     *            'updated before' Timestamp in milliseconds
     * @param stati
     *            Media status (<code>null</code> to get all media for given
     *            time range)
     * @param offset
     *            Offset of the find result. (used for paging.
     * @param limit
     *            Max. number of results to return. (used for paging)
     * @param desc
     *            Sort order. if true descending, false ascending order.
     * 
     * @return The total number of search results.
     * 
     * @ejb.interface-method
     */
    public int findByUpdatedTime(Collection col, Long after, Long before,
            int[] stati, Integer offset, Integer limit, boolean desc)
            throws FinderException {
        Timestamp tsAfter = null;
        if (after != null)
            tsAfter = new Timestamp(after.longValue());
        Timestamp tsBefore = null;
        if (before != null)
            tsBefore = new Timestamp(before.longValue());
        col.addAll(toMediaDTOs(mediaHome.listByUpdatedTime(stati, tsAfter,
                tsBefore, offset, limit, desc)));
        return mediaHome.countByUpdatedTime(stati, tsAfter, tsBefore);
    }

    /**
     * Converts a collection of MediaLocal objects to a list of MediaDTO
     * objects.
     * 
     * @param c
     *            Collection with MediaLocal objects.
     * 
     * @return List of MediaDTO objects.
     */
    private List toMediaDTOs(Collection c) {
        ArrayList list = new ArrayList();
        for (Iterator it = c.iterator(); it.hasNext();) {
            list.add(toMediaDTO((MediaLocal) it.next()));
        }
        return list;
    }

    /**
     * Creates a MediaDTO object for given given MediaLocal object.
     * 
     * @param media
     *            A MediaLocal object.
     * 
     * @return The MediaDTO object for given MediaLocal.
     */
    private MediaDTO toMediaDTO(MediaLocal media) {
        MediaDTO dto = new MediaDTO();
        dto.setPk(media.getPk().longValue());
        dto.setCreatedTime(media.getCreatedTime());
        dto.setUpdatedTime(media.getUpdatedTime());
        dto.setMediaUsage(media.getMediaUsage());
        dto.setMediaStatus(media.getMediaStatus());
        dto.setMediaStatusInfo(media.getMediaStatusInfo());
        dto.setFilesetId(media.getFilesetId());
        dto.setFilesetIuid(media.getFilesetIuid());
        dto.setMediaCreationRequestIuid(media.getMediaCreationRequestIuid());
        try {
            dto.setInstancesAvailable(media.checkInstancesAvailable());
        } catch (FinderException e) { /* ignore */
        }
        return dto;
    }

    /**
     * Set the media creation request IUID.
     * 
     * @param pk
     *            Primary key of media.
     * @param iuid
     *            Media creation request IUID to set.
     * 
     * @ejb.interface-method
     */
    public void setMediaCreationRequestIuid(long pk, String iuid)
            throws FinderException {
        MediaLocal media = mediaHome.findByPrimaryKey(new Long(pk));
        media.setMediaCreationRequestIuid(iuid);
    }

    /**
     * Set media staus and status info.
     * 
     * @param pk
     *            Primary key of media.
     * @param status
     *            Status to set.
     * @param info
     *            Status info to set.
     * 
     * @ejb.interface-method
     */
    public void setMediaStatus(long pk, int status, String info)
            throws FinderException {
        if (log.isDebugEnabled())
            log.debug("setMediaStatus: pk=" + pk + ", status:" + status
                    + ", info" + info);
        MediaLocal media = mediaHome.findByPrimaryKey(new Long(pk));
        media.setMediaStatus(status);
        media.setMediaStatusInfo(info);
        if (status == MediaDTO.COMPLETED)
            updateSeriesAndStudies(media);
    }

    /**
     * Returns a collection of study IUIDs of a given media.
     * 
     * @param pk
     *            Primary key of the media.
     * 
     * @return Collection with study IUIDs.
     * 
     * @ejb.interface-method
     */
    public Collection getStudyUIDSForMedia(long pk) throws FinderException {
        Collection c = new ArrayList();
        MediaLocal media = mediaHome.findByPrimaryKey(new Long(pk));
        Collection studies = studyHome.findStudiesOnMedia(media);
        for (Iterator iter = studies.iterator(); iter.hasNext();) {
            c.add(((StudyLocal) iter.next()).getStudyIuid());
        }
        return c;
    }

    /**
     * Returns a dataset for media creation request for given media.
     * <p>
     * <DL>
     * <DT>Set following Tags in dataset.</DT>
     * <DD>SpecificCharacterSet</DD>
     * <DD>StorageMediaFileSetID</DD>
     * <DD>StorageMediaFileSetUID</DD>
     * <DD>RefSOPSeq with instances of the media.</DD>
     * </DL>
     * 
     * @param pk
     *            Primary key of the media.
     * 
     * @return Prepared Dataset for media creation request.
     * 
     * @ejb.interface-method
     */
    public Dataset prepareMediaCreationRequest(long pk) throws FinderException {
        MediaLocal media = mediaHome.findByPrimaryKey(new Long(pk));
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        ds.putCS(Tags.SpecificCharacterSet, "ISO_IR 100");
        ds.putSH(Tags.StorageMediaFileSetID, media.getFilesetId());
        ds.putUI(Tags.StorageMediaFileSetUID, media.getFilesetIuid());
        Collection c = media.getInstances();
        InstanceLocal il;
        DcmElement refSOPSeq = ds.putSQ(Tags.RefSOPSeq);
        for (Iterator iter = c.iterator(); iter.hasNext();) {
            il = (InstanceLocal) iter.next();
            Dataset item = refSOPSeq.addNewItem();
            item.putUI(Tags.RefSOPInstanceUID, il.getSopIuid());
            item.putUI(Tags.RefSOPClassUID, il.getSopCuid());
        }
        return ds;
    }

    private void updateSeriesAndStudies(MediaLocal media)
            throws FinderException {
        Collection series = seriesHome.findSeriesOnMedia(media);
        Iterator iter = series.iterator();
        while (iter.hasNext()) {
            final SeriesLocal ser = ((SeriesLocal) iter.next());
            ser.updateFilesetId();
        }
        Collection studies = studyHome.findStudiesOnMedia(media);
        iter = studies.iterator();
        while (iter.hasNext()) {
            final StudyLocal sty = ((StudyLocal) iter.next());
            sty.updateFilesetId();
        }
    }

    /**
     * Deletes a media.
     * <p>
     * Update derived fields from series and studies after media is successfully
     * deleted.
     * 
     * @param mediaPk
     *            Primary key of the media.
     * 
     * @ejb.interface-method
     */
    public void deleteMedia(Long mediaPk) throws EJBException, RemoveException,
            FinderException {
        MediaLocal media = mediaHome.findByPrimaryKey(mediaPk);
        Collection series = seriesHome.findSeriesOnMedia(media);
        Collection studies = studyHome.findStudiesOnMedia(media);
        String filesetId = media.getFilesetId();
        mediaHome.remove(mediaPk);
        log.info("Media " + filesetId + " removed!");
        Iterator iter = series.iterator();
        while (iter.hasNext()) {
            SeriesLocal ser = (SeriesLocal) iter.next();
            ser.updateFilesetId();
            ser.updateAvailability();
        }
        if (log.isDebugEnabled())
            log.debug("Series updated after media " + filesetId
                    + " was deleted!");
        iter = studies.iterator();
        while (iter.hasNext()) {
            StudyLocal sty = (StudyLocal) iter.next();
            sty.updateFilesetId();
            sty.updateAvailability();
        }
        if (log.isDebugEnabled())
            log.debug("Studies updated after media " + filesetId
                    + " was deleted!");

    }

    /**
     * Checks if all instances of a media are locally available (online).
     * <p>
     * Update derived fields from series and studies after media is successfully
     * deleted.
     * 
     * @param mediaPk
     *            Primary key of the media.
     * 
     * @ejb.interface-method
     */
    public boolean checkInstancesAvailable(Long mediaPk) throws FinderException {
        return mediaHome.findByPrimaryKey(mediaPk).checkInstancesAvailable();
    }
}