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
 * Agfa-Gevaert Group.
 * Portions created by the Initial Developer are Copyright (C) 2003-2005
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below.
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

package org.dcm4chex.archive.emf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmEncodeParam;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.dict.VRs;
import org.dcm4che.util.BufferedOutputStream;
import org.dcm4chex.archive.common.SeriesStored;
import org.dcm4chex.archive.ejb.interfaces.FileDTO;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2;
import org.dcm4chex.archive.ejb.interfaces.FileSystemMgt2Home;
import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.dcm4chex.archive.mbean.JMSDelegate;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 26, 2007
 */
public class UpgradeToEnhancedMFService extends ServiceMBeanSupport
        implements MessageListener, NotificationListener {

    // not yet finally defined in Supp 117
    private static final int PETFrameTypeSeqTag = 0x00189551;

    private static final int[] SERIES_IUID = { Tags.SeriesInstanceUID };

    private static final String UPGRADE_CT_XML = "upgrade-ct.xml";

    private static final String UPGRADE_MR_XML = "upgrade-mr.xml";

    private static final String UPGRADE_PET_XML = "upgrade-pet.xml";

    private ObjectName storeScpServiceName;

    private ObjectName queryRetrieveScpServiceName;

    private String queueName;

    private int concurrency = 1;

    private String configDir;

    private boolean mergePatientStudySeriesAttributesFromDB;

    private boolean noPixelData;

    private boolean deflate;

    private boolean deleteOriginalStoredSeries;

    private int bufferSize = 8192;

    private JMSDelegate jmsDelegate = new JMSDelegate(this);

    public final ObjectName getJmsServiceName() {
        return jmsDelegate.getJmsServiceName();
    }

    public final void setJmsServiceName(ObjectName jmsServiceName) {
        jmsDelegate.setJmsServiceName(jmsServiceName);
    }
    
    public final ObjectName getStoreScpServiceName() {
        return storeScpServiceName;
    }

    public final void setStoreScpServiceName(ObjectName storeScpServiceName) {
        this.storeScpServiceName = storeScpServiceName;
    }

    public final ObjectName getQueryRetrieveScpServiceName() {
        return queryRetrieveScpServiceName;
    }

    public final void setQueryRetrieveScpServiceName(ObjectName name) {
        this.queryRetrieveScpServiceName = name;
    }

    public final String getQueueName() {
        return queueName;
    }

    public final void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public final String getConfigDir() {
        return configDir;
    }

    public final void setConfigDir(String path) {
        this.configDir = path;
    }

    public final boolean isMergePatientStudySeriesAttributesFromDB() {
        return mergePatientStudySeriesAttributesFromDB;
    }

    public final void setMergePatientStudySeriesAttributesFromDB(
            boolean mergeFromDB) {
        this.mergePatientStudySeriesAttributesFromDB = mergeFromDB;
    }

    public final int getConcurrency() {
        return concurrency;
    }

    public final void setConcurrency(int concurrency) throws Exception {
        if (concurrency <= 0)
            throw new IllegalArgumentException("Concurrency: " + concurrency);
        if (this.concurrency != concurrency) {
            final boolean restart = getState() == STARTED;
            if (restart)
                stop();
            this.concurrency = concurrency;
            if (restart)
                start();
        }
    }

    public final boolean isNoPixelData() {
        return noPixelData;
    }

    public final void setNoPixelData(boolean noPixelData) {
        this.noPixelData = noPixelData;
    }

    public final boolean isDeflate() {
        return deflate;
    }

    public final void setDeflate(boolean deflate) {
        this.deflate = deflate;
    }

    public final boolean isDeleteOriginalStoredSeries() {
        return deleteOriginalStoredSeries;
    }

    public final void setDeleteOriginalStoredSeries(boolean delete) {
        this.deleteOriginalStoredSeries = delete;
    }

    public final int getBufferSize() {
        return bufferSize;
    }

    public final void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    protected void startService() throws Exception {
        jmsDelegate.startListening(queueName, this, concurrency);
        server.addNotificationListener(storeScpServiceName, this,
                SeriesStored.NOTIF_FILTER, null);
    }

    protected void stopService() throws Exception {
        server.removeNotificationListener(storeScpServiceName, this,
                SeriesStored.NOTIF_FILTER, null);
        jmsDelegate.stopListening(queueName);
    }

    public boolean isUpgradeEnabled(SeriesStored seriesStored) {
        String modality = seriesStored.getModality();
        if ("CT".equals(modality)) {
            return isUpgradeEnabled(seriesStored, UPGRADE_CT_XML,
                    UIDs.CTImageStorage);
        }
        if ("MR".equals(modality)) {
            return isUpgradeEnabled(seriesStored, UPGRADE_MR_XML,
                    UIDs.MRImageStorage);
        }
        if ("PT".equals(modality)) {
            return isUpgradeEnabled(seriesStored, UPGRADE_PET_XML,
                    UIDs.PositronEmissionTomographyImageStorage);
        }
        return false;
    }

    private File getConfigFile(String aet, String fname) {
        if (aet != null) {
            File f =  FileUtils.resolve(new File(new File(configDir, aet), fname));
            if (f.exists()) {
                return f;
            }
        }
        return FileUtils.resolve(new File(configDir, fname));
    }

    private Dataset loadConfig(File file) throws ConfigurationException {
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        try {
            SAXParserFactory f = SAXParserFactory.newInstance();
            SAXParser p = f.newSAXParser();
            p.parse(file, ds.getSAXHandler2(null));
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Failed to load VMF Configuration from " + file);
        }
        return ds;
    }

    private boolean isUpgradeEnabled(SeriesStored seriesStored, String xml,
            String cuid) {
        String callingAET = seriesStored.getSourceAET();
        return getConfigFile(callingAET, xml).exists()
            && containsOnlySOPClass(seriesStored, cuid);
    }

    private boolean containsOnlySOPClass(SeriesStored seriesStored,
            String cuid) {
        Dataset ian = seriesStored.getIAN();
        Dataset refSeriesSeq = ian.getItem(Tags.RefSeriesSeq);
        DcmElement refSOPSeq = refSeriesSeq.get(Tags.RefSOPSeq);
        for (int i = 0, n = refSOPSeq.countItems(); i < n; i++) {
            Dataset refSOP = refSOPSeq.getItem(i);
            String refCUID = refSOP.getString(Tags.RefSOPClassUID);
            if (!cuid.equals(refCUID)) {
                return false;
            }
        }
        return true;
    }

    public void handleNotification(Notification notif, Object handback) {
        SeriesStored seriesStored = (SeriesStored) notif.getUserData();
        if (isUpgradeEnabled(seriesStored)) {
            schedule(seriesStored);
        }
    }

    private void schedule(SeriesStored seriesStored) {
        try {
            log.info("Scheduling Upgrade to Enhanced MF for " + seriesStored);
            jmsDelegate.queue(queueName, seriesStored, 
                    Message.DEFAULT_PRIORITY, 0L);
        } catch (Exception e) {
            log.error("Failed to schedule Upgrade to Enhanced MF for "
                    + seriesStored, e);
        }
    }

    public void onMessage(Message message) {
        ObjectMessage om = (ObjectMessage) message;
        try {
            SeriesStored seriesStored = (SeriesStored) om.getObject();
            log.info("Start upgrading " + seriesStored + " to Enhanced MF");
            try {
                upgradeToEMF(seriesStored);
                log.info("Finished upgrading " + seriesStored
                            + " to Enhanced MF");
                if (deleteOriginalStoredSeries) {
                    try {
                        deleteStoredSeries(seriesStored);
                    } catch (Exception e) {
                        log.error("Failed to delete original "
                                + seriesStored, e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to upgrade " + seriesStored
                                + " to Enhanced MF", e);
            }
        } catch (Throwable e) {
            log.error("unexpected error during processing message: " + message,
                    e);
        }
    }

    private void upgradeToEMF(SeriesStored seriesStored) throws Exception {
        Dataset ian = seriesStored.getIAN();
        Dataset refSeriesSeq = ian.getItem(Tags.RefSeriesSeq);
        DcmElement refSOPSeq = refSeriesSeq.get(Tags.RefSOPSeq);
        int numFrames = refSOPSeq.countItems();
        EnhancedMFBuilder builder = newEMFBuilder(seriesStored, numFrames);
        File[] files = new File[numFrames];
        for (int i = 0; i < numFrames; i++) {
            Dataset refSOP = refSOPSeq.getItem(i);
            String iuid = refSOP.getString(Tags.RefSOPInstanceUID);
            builder.add(files[i] = locateInstance(iuid));
        }
        Dataset mfds = builder.build();
        if (mergePatientStudySeriesAttributesFromDB) {
            mfds.putAll(seriesStored.getSeriesAttrs().exclude(SERIES_IUID));
            mfds.putAll(seriesStored.getStudyAttrs());
            mfds.putAll(seriesStored.getPatientAttrs());
        }
        String tsUID = mfds.getFileMetaInfo().getTransferSyntaxUID();
        FileDTO fileDTO = makeFile(mfds);
        File mffile = FileUtils.toFile(
                fileDTO.getDirectoryPath(), fileDTO.getFilePath());
        boolean deleteFile = true;
        try {
            log.info("M-WRITE file:" + mffile);
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestOutputStream dos = new DigestOutputStream(
                    new FileOutputStream(mffile), md);
            BufferedOutputStream out = 
                    new BufferedOutputStream(dos, new byte[bufferSize]);
            try {
                DcmEncodeParam encParam = DcmEncodeParam.valueOf(tsUID);
                mfds.writeFile(out, encParam);
                if (!noPixelData) {
                    mfds.writeHeader(out, encParam, Tags.PixelData,
                            builder.getPixelDataVR(), 
                            builder.getPixelDataLength());
                    if (encParam.encapsulated) {
                        mfds.writeHeader(out, encParam, Tags.Item, VRs.NONE, 0);
                    }
                    for (int i = 0; i < numFrames; i++) {
                        long off = builder.getPixelDataOffset(i);
                        int len = builder.getPixelDataLength(i);
                        if (encParam.encapsulated) {
                            mfds.writeHeader(out, encParam, Tags.Item, VRs.NONE, len);
                        }
                        FileInputStream in = new FileInputStream(files[i]);
                        try {
                            while (off > 0) {
                                off -= in.skip(off);
                            }
                            out.copyFrom(in, len);
                        } finally {
                            in.close();
                        }
                    }
                    if (encParam.encapsulated) {
                        mfds.writeHeader(out, encParam,
                                Tags.SeqDelimitationItem, VRs.NONE, 0);
                    }
                }
            } finally {
                out.close();
            }
            fileDTO.setFileMd5(md.digest());
            fileDTO.setFileSize(mffile.length());
            fileDTO.setFileTsuid(tsUID);
            importFile(fileDTO, mfds);
            deleteFile = false;
        } finally {
            if (deleteFile) {
                log.info("M-DELETE file:" + mffile);
                if (!mffile.delete()) {
                    log.error("Failed to delete " + mffile);
                }
            }
        }
    }

    private EnhancedMFBuilder newEMFBuilder(SeriesStored seriesStored,
            int numFrames) {
        String callingAET = seriesStored.getSourceAET();
        String modality = seriesStored.getModality();
        if ("CT".equals(modality)) {
            return new EnhancedMFBuilder(this,
                    loadConfig(getConfigFile(callingAET, UPGRADE_CT_XML)),
                    Tags.CTImageFrameTypeSeq, numFrames);
        }
        if ("MR".equals(modality)) {
            return new EnhancedMFBuilder(this,
                    loadConfig(getConfigFile(callingAET, UPGRADE_MR_XML)),
                    Tags.MRImageFrameTypeSeq, numFrames);
        }
        if ("PT".equals(modality)) {
            return new EnhancedMFBuilder(this,
                    loadConfig(getConfigFile(callingAET, UPGRADE_PET_XML)),
                    PETFrameTypeSeqTag, numFrames);
        }
        throw new IllegalArgumentException("modality: " + modality);
    }

    private FileDTO makeFile(Dataset dataset) throws Exception {
        return (FileDTO) server.invoke(storeScpServiceName, "makeFile",
                new Object[] { dataset },
                new String[] { Dataset.class.getName() });
    }

    private void importFile(FileDTO fileDTO, Dataset dataset) throws Exception {
        server.invoke(storeScpServiceName, "importFile",
                new Object[] { fileDTO, dataset, null, Boolean.TRUE },
                new String[] { FileDTO.class.getName(), Dataset.class.getName(),
                        String.class.getName(), boolean.class.getName() });
    }

    private File locateInstance(String iuid) throws Exception {
        return (File) server.invoke(queryRetrieveScpServiceName,
                "locateInstance", new Object[] { iuid },
                new String[] { String.class.getName() });
    }

    protected FileSystemMgt2 newFileSystemMgt() throws Exception {
        return ((FileSystemMgt2Home) EJBHomeFactory.getFactory().lookup(
                FileSystemMgt2Home.class, FileSystemMgt2Home.JNDI_NAME)).create();
    }

    private void deleteStoredSeries(SeriesStored seriesStored)
            throws Exception {
        FileDTO[] fileDTOs = newFileSystemMgt()
                .deleteStoredSeries(seriesStored);
        for (FileDTO fileDTO : fileDTOs) {
            FileUtils.delete(FileUtils.toFile(fileDTO.getDirectoryPath(),
                    fileDTO.getFilePath()), true);
        }
    }
}
