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

import java.io.File;
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
public class StudyModel extends AbstractModel {

    private long pk;
    
    private static String httpRoot = "";

    public StudyModel() {
    }

    public StudyModel(Dataset ds) {
        super(ds);
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.StudyPk);
        this.pk = bb == null ? -1 : Convert.toLong(bb.array());
    }
    
    public static void setHttpRoot(String root) {
    	if ( root == null ) return;
    	httpRoot = root;
    }
    
    public final long getPk() {
        return pk;
    }

    public final void setPk(long pk) {
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ds.putOB(PrivateTags.StudyPk, Convert.toBytes(pk));
        this.pk = pk;
    }

    public int hashCode() {
    	return (int)( pk ^ pk >>> 32);//like Long.hashCode()
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudyModel)) return false;
        StudyModel other = (StudyModel) o;
        return pk == other.pk;
    }

    public final String getPlacerOrderNumber() {
        return ds.getString(Tags.PlacerOrderNumber);
    }

    public final void setPlacerOrderNumber(String s) {
        ds.putSH(Tags.PlacerOrderNumber, s);
    }

    public final String getFillerOrderNumber() {
        return ds.getString(Tags.FillerOrderNumber);
    }

    public final void setFillerOrderNumber(String s) {
        ds.putSH(Tags.FillerOrderNumber, s);
    }

    public final String getAccessionNumber() {
        return ds.getString(Tags.AccessionNumber);
    }

    public final void setAccessionNumber(String s) {
        ds.putSH(Tags.AccessionNumber, s);
    }

    public final String getReferringPhysician() {
        return ds.getString(Tags.ReferringPhysicianName);
    }

    public final void setReferringPhysician(String s) {
        ds.putPN(Tags.ReferringPhysicianName, s);
    }

    public final String getStudyDateTime() {
        return getDateTime(Tags.StudyDate, Tags.StudyTime);
    }

    public final void setStudyDateTime(String s) {
        setDateTime(Tags.StudyDate, Tags.StudyTime, s);
    }

    public final String getStudyDescription() {
        return ds.getString(Tags.StudyDescription);
    }

    public final void setStudyDescription(String s) {
        ds.putLO(Tags.StudyDescription, s);
    }

    public final String getStudyID() {
        return ds.getString(Tags.StudyID);
    }

    public final void setStudyID(String s) {
        ds.putSH(Tags.StudyID, s);
    }

    public final String getStudyIUID() {
        return ds.getString(Tags.StudyInstanceUID);
    }

    public final void setStudyIUID(String s) {
        ds.putUI(Tags.StudyInstanceUID, s);
    }

    public final String getModalitiesInStudy() {
        return StringUtils
                .toString(ds.getStrings(Tags.ModalitiesInStudy), '\\');
    }

    public final int getNumberOfInstances() {
        return ds.getInt(Tags.NumberOfStudyRelatedInstances, 0);
    }

    public final int getNumberOfSeries() {
        return ds.getInt(Tags.NumberOfStudyRelatedSeries, 0);
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
     * @return Study Status ID
     */
    public final String getStudyStatusId() {
    	return ds.getString(Tags.StudyStatusID);
    }
    
    public final String getStudyStatusImage() {
    	String s = getStudyStatusId();
    	if ( s == null ) return null;
    	s = "images/s_"+s.toLowerCase()+".jpg";
    	return new File(httpRoot, s).exists() ? s : null;
    }
    
    /**
     * Returns the list of Series.
     * <p>
     * Use the <code>childs</code> from <code>AbstractModel</code> method now.
     * 
     * @return Series as List.
     */
    public final List getSeries() {
        return listOfChilds();
    }

    /**
     * Set a new list of series.
     * <p>
     * Use the <code>setChilds</code> from <code>AbstractModel</code> method now.
     * 
     * @param series List of Series
     */
    public final void setSeries(List series) {
        setChilds( series );
    }

}