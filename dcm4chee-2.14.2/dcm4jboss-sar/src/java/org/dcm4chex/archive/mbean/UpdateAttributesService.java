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

package org.dcm4chex.archive.mbean;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Iterator;

import javax.management.JMException;
import javax.management.ObjectName;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4chex.archive.common.Availability;
import org.dcm4chex.archive.ejb.conf.AttributeFilter;
import org.dcm4chex.archive.ejb.interfaces.UpdateAttributes;
import org.dcm4chex.archive.ejb.interfaces.UpdateAttributesHome;
import org.dcm4chex.archive.ejb.jdbc.FileInfo;
import org.dcm4chex.archive.ejb.jdbc.RetrieveCmd;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Revision$ $Date$
 * @since Nov 19, 2007
 */
public class UpdateAttributesService extends ServiceMBeanSupport {

    private static final String ANY = "ANY";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private String modality;
    private String sourceAETitle;
    private Timestamp updatedBefore;
    private Timestamp updatedAfter;
    private int availability;
    private int maximalNumberOfSeriesToUpdateByOneTask;
    private ObjectName queryRetrieveScpServiceName;
    private ObjectName tarRetrieverName;

    public final ObjectName getQueryRetrieveScpServiceName() {
        return queryRetrieveScpServiceName;
    }

    public final void setQueryRetrieveScpServiceName(ObjectName name) {
        this.queryRetrieveScpServiceName = name;
    }

    public final ObjectName getTarRetrieverName() {
        return tarRetrieverName;
    }

    public final void setTarRetrieverName(ObjectName tarRetrieverName) {
        this.tarRetrieverName = tarRetrieverName;
    }

    public final String getModality() {
        return modality != null ? modality : ANY;
    }

    public final void setModality(String modality) {
        this.modality = !ANY.equalsIgnoreCase(modality) ? modality : null;
    }

    public final String getSourceAETitle() {
        return sourceAETitle != null ? sourceAETitle : ANY;
    }

    public final void setSourceAETitle(String sourceAETitle) {
        this.sourceAETitle = !ANY.equalsIgnoreCase(sourceAETitle)
                ? sourceAETitle : null;
    }

    public final String getUpdatedBefore() {
        return formatDate(updatedBefore());
    }

    private Timestamp updatedBefore() {
        return (updatedBefore != null) ? updatedBefore
                : new Timestamp(AttributeFilter.lastModified());
    }

    private static String formatDate(Timestamp ts) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_TIME_FORMAT);
        return df.format(ts);
    }

    public final void setUpdatedBefore(String s) throws ParseException {
        this.updatedBefore = s.equalsIgnoreCase("AUTO") ? null
                : parseDate(s);
    }

    private static Timestamp parseDate(String s) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(
                DATE_TIME_FORMAT.substring(0, s.length()));
        return new Timestamp(df.parse(s).getTime());
    }

    public final String getUpdatedAfter() {
        return formatDate(updatedAfter);
    }

    public final void setUpdatedAfter(String s) throws ParseException {
        this.updatedAfter = parseDate(s);
    }

    public final String getAvailability() {
        return Availability.toString(availability);
    }

    public final void setAvailability(String availability) {
        this.availability = Availability.toInt(availability);
    }

    public final int getMaximalNumberOfSeriesToUpdateByOneTask() {
        return maximalNumberOfSeriesToUpdateByOneTask;
    }

    public final void setMaximalNumberOfSeriesToUpdateByOneTask(int max) {
        this.maximalNumberOfSeriesToUpdateByOneTask = max;
    }

    public void reloadAttributeFilter() {
        AttributeFilter.reload();
    }

    public int countNumberOfMatchingSeriesToUpdate() throws Exception {
        return updateAttributes().countSeriesForAttributesUpdate(availability,
                modality, sourceAETitle, updatedAfter, updatedBefore());
    }

    public int updateMatchingSeries() throws Exception {
        reloadAttributeFilter();
        UpdateAttributes updateAttributes = updateAttributes();
        Collection seriesIuids = updateAttributes.seriesIuidsForAttributesUpdate(
                availability, modality, sourceAETitle, updatedAfter,
                updatedBefore(), maximalNumberOfSeriesToUpdateByOneTask);
        for (Iterator iterator = seriesIuids.iterator(); iterator.hasNext();) {
            updateSeries((String) iterator.next(), updateAttributes);
        }
        return seriesIuids.size();
    }

    public int updateSeries(String seriesIuid) throws Exception {
        reloadAttributeFilter();
        return updateSeries(seriesIuid, updateAttributes());
    }

    private int updateSeries(String seriesIuid,
            UpdateAttributes updateAttributes) throws Exception {
        Dataset keys = DcmObjectFactory.getInstance().newDataset();
        keys.putUI(Tags.SeriesInstanceUID, seriesIuid);
        FileInfo[][] fileInfos = 
            RetrieveCmd.createSeriesRetrieve(keys).getFileInfos();
        int count = 0;
        Dataset series = null;
        for (int i = 0; i < fileInfos.length; i++) {
            Dataset inst = loadDataset(fileInfos[i]);
            if (inst != null) {
                try {
                    updateAttributes.updateInstanceAttributes(inst);
                    series = inst;
                    ++count;
                } catch (Exception e) {
                    log.error("Failed to update instance[uid= " 
                            + inst.getString(Tags.SOPInstanceUID) + "]");
                }
            }
        }
        if (series != null) {
            try {
                updateAttributes.updatePatientStudySeriesAttributes(series);
            } catch (Exception e) {
                log.error("Failed to update series[uid= " + seriesIuid + "]");
            }
            log.info("Updated " + count + " of " + fileInfos.length
                    + " instances from series[uid= " + seriesIuid + "]");
            if (count < fileInfos.length) {
                log.warn("Only updated " + count + " of " + fileInfos.length
                        + " instances from series[uid= " + seriesIuid + "]");
            }
        }
        return count;
    }

    private Dataset loadDataset(FileInfo[] fileInfos) {
        for (int i = 0; i < fileInfos.length; i++) {
            FileInfo fileInfo = fileInfos[i];
            if (isLocalRetrieveAET(fileInfo.fileRetrieveAET)) {
                Dataset ds = DcmObjectFactory.getInstance().newDataset();
                try {
                    ds.readFile(getFile(fileInfo), FileFormat.DICOM_FILE, Tags.PixelData);
                } catch (Exception e) {
                    log.error(
                            "Failed to read dataset referenced by " + fileInfo,
                            e);
                    return null;
                }
                return ds;
            }
        }
        return null;
    }

    public String updateMatchingSeriesShowElapsedTime() throws Exception {
        long begin = System.currentTimeMillis();
        int updated = updateMatchingSeries();
        long end = System.currentTimeMillis();
        return "Updated attributes of " + updated
                + " Series in the database in "
                + ((end - begin) / 1000.f) + " seconds.";
    }

    private UpdateAttributes updateAttributes() throws Exception {
        return ((UpdateAttributesHome) EJBHomeFactory.getFactory().lookup(
                UpdateAttributesHome.class, UpdateAttributesHome.JNDI_NAME)).create();
    }


    boolean isLocalRetrieveAET(String aet) {
        try {
            return (Boolean) server.invoke(queryRetrieveScpServiceName,
                    "isLocalRetrieveAET", new Object[]{ aet },
                    new String[]{ String.class.getName() });
        } catch (JMException e) {
            throw new RuntimeException(
                    "Failed to invoke isLocalRetrieveAET() on "
                    + queryRetrieveScpServiceName, e);
        }
    }

    private File getFile(FileInfo info) throws Exception {
        return info.basedir.startsWith("tar:") ? retrieveFileFromTAR(
                info.basedir, info.fileID) : FileUtils.toFile(info.basedir,
                        info.fileID);
    }

    File retrieveFileFromTAR(String fsID, String fileID) throws Exception {
        return (File) server.invoke(tarRetrieverName, "retrieveFileFromTAR",
                new Object[] { fsID, fileID }, new String[] {
                        String.class.getName(), String.class.getName() });
    }
}
