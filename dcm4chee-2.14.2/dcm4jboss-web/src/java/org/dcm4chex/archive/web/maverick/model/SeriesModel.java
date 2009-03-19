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

package org.dcm4chex.archive.web.maverick.model;

import java.nio.ByteBuffer;
import java.util.List;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.util.Convert;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2509 $ $Date: 2006-05-31 15:32:55 +0200 (Wed, 31 May 2006) $
 * @since 05.10.2004
 *
 */
public class SeriesModel extends AbstractModel {

    private long pk = -1l;

    private boolean incorrectWLEntry = false;
    private String drCode = null;
    private String drCodeMeaning = null;
    private String drCodeDesignator = null;

    
    
    public SeriesModel() {
    }

    public SeriesModel(Dataset ds) {
        super(ds);
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        Dataset item = ds.getItem(Tags.PPSDiscontinuationReasonCodeSeq);
        if ( item != null ) {
	        drCode = item.getString(Tags.CodeValue);
	        drCodeMeaning = item.getString(Tags.CodeMeaning);
	        drCodeDesignator = item.getString(Tags.CodingSchemeDesignator);
	        incorrectWLEntry = "110514".equals(drCode) && "DCM".equals(drCodeDesignator);
    	}
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.SeriesPk);
        this.pk = bb == null ? -1 : Convert.toLong(bb.array());
    }

    public final long getPk() {
        return pk;
    }

    public final void setPk(long pk) {
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ds.putOB(PrivateTags.SeriesPk, Convert.toBytes(pk));
        this.pk = pk;
    }

    public int hashCode() {
    	return (int)( pk ^ pk >>> 32);//like Long.hashCode()
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesModel)) return false;
        SeriesModel other = (SeriesModel) o;
        return pk == other.pk;
    }

    
	/**
	 * @return Returns the incorrectWLEntry.
	 */
	public boolean isIncorrectWLEntry() {
		return incorrectWLEntry;
	}
    public final String getBodyPartExamined() {
        return ds.getString(Tags.BodyPartExamined);
    }

    public final void setBodyPartExamined(String s) {
        ds.putCS(Tags.BodyPartExamined, s);
    }

    public final String getLaterality() {
        return ds.getString(Tags.Laterality);
    }

    public final void setLaterality(String s) {
        ds.putCS(Tags.Laterality, s);
    }

    public final String getManufacturer() {
        return ds.getString(Tags.Manufacturer);
    }

    public final void setManufacturer(String s) {
        ds.putCS(Tags.Manufacturer, s);
    }

    public final String getManufacturerModelName() {
        return ds.getString(Tags.ManufacturerModelName);
    }

    public final void setManufacturerModelName(String s) {
        ds.putCS(Tags.ManufacturerModelName, s);
    }

    public final String getModality() {
        return ds.getString(Tags.Modality);
    }

    public final void setModality(String s) {
        ds.putCS(Tags.Modality, s);
    }

    public final String getSeriesDateTime() {
        return getDateTime(Tags.SeriesDate, Tags.SeriesTime);
    }

    public final void setSeriesDateTime(String s) {
        setDateTime(Tags.SeriesDate, Tags.SeriesTime, s);
    }

    public final String getSeriesDescription() {
        return ds.getString(Tags.SeriesDescription);
    }

    public final void setSeriesDescription(String s) {
        ds.putLO(Tags.SeriesDescription, s);
    }

    public final String getSeriesIUID() {
        return ds.getString(Tags.SeriesInstanceUID);
    }

    public final void setSeriesIUID(String s) {
        ds.putUI(Tags.SeriesInstanceUID, s);
    }

    public final String getSeriesNumber() {
        return ds.getString(Tags.SeriesNumber);
    }

    public final void setSeriesNumber(String s) {
        ds.putIS(Tags.SeriesNumber, s);
    }

    public final int getNumberOfInstances() {
        return ds.getInt(Tags.NumberOfSeriesRelatedInstances, 0);
    }

    public final String getAvailability() {
        return ds.getString(Tags.InstanceAvailability);
    }

    public final String getRetrieveAETs() {
        return StringUtils.toString(ds.getStrings(Tags.RetrieveAET), '\\');
    }

    public final String getFilesetId() {
        String s = ds.getString(Tags.StorageMediaFileSetID);
        if ( s == null || s.trim().length() < 1 ) s = "_NA_";
        return s;
    }
    
    /**
     * Returns the list of Instances.
     * <p>
     * Use the <code>childs</code> from <code>AbstractModel</code> method now.
     * 
     * @return Instances as List.
     */
    public final List getInstances() {
        return listOfChilds();
    }

    /**
     * Set a new list of instances.
     * <p>
     * Use the <code>setChilds</code> from <code>AbstractModel</code> method now.
     * 
     * @param instances List of instances
     */
    public final void setInstances(List instances) {
        setChilds(instances);
    }
    
    public String getPPSID() {
    	return ds.getString(Tags.PPSID);
    }

    public String getPPSDesc() {
    	return ds.getString(Tags.PPSDescription);
    }
    
    public String getPPSStatus() {
    	return ds.getString(Tags.PPSStatus);
    }

    public String getDRCode() { return drCode; }
    public String getDRCodeDesignator() { return drCodeDesignator; }
    public String getDRCodeMeaning() { return drCodeMeaning; }
    
    public String getPPSStartDate() {
    	return getDateTime(Tags.PPSStartDate, Tags.PPSStartTime);
    }
    
    public String getPPSEndDate() {
    	return getDateTime(Tags.PPSEndDate, Tags.PPSEndTime);
    }
    
}