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
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.util.Convert;
import org.dcm4chex.archive.web.maverick.xdsi.XDSConsumerModel;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 4943 $ $Date: 2007-09-03 16:10:22 +0200 (Mon, 03 Sep 2007) $
 * @since 05.10.2004
 *
 */
public class PatientModel extends AbstractModel {

    private long pk = -1l;
    private boolean showXDS = false;
    
    private static XDSConsumerModel consumerModel;

    public PatientModel() {
    }

    public PatientModel(Dataset ds) {
        super(ds);
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.PatientPk);
        this.pk = bb == null ? -1 : Convert.toLong(bb.array());
    }

    public final long getPk() {
        return pk;
    }

    public final void setPk(long pk) {
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ds.putOB(PrivateTags.PatientPk, Convert.toBytes(pk));
        this.pk = pk;
    }

    public int hashCode() {
    	return (int)( pk ^ pk >>> 32);//like Long.hashCode()
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PatientModel)) return false;
        PatientModel other = (PatientModel) o;
        return pk == other.pk;
    }

    public final String getIssuerOfPatientID() {
        return ds.getString(Tags.IssuerOfPatientID);
    }

    public final void setIssuerOfPatientID(String issuerOfPatientID) {
        ds.putLO(Tags.IssuerOfPatientID, issuerOfPatientID);
    }

    public final String getPatientBirthDate() {
        return getDate(Tags.PatientBirthDate);
    }

    public final void setPatientBirthDate(String s) {
        setDate(Tags.PatientBirthDate, s);
    }

    public final String getPatientID() {
        return ds.getString(Tags.PatientID);
    }

    public final void setPatientID(String patientID) {
        ds.putLO(Tags.PatientID, patientID);
    }

    public final String getPatientName() {
        return ds.getString(Tags.PatientName);
    }

    public final void setPatientName(String patientName) {
        ds.putPN(Tags.PatientName, patientName);
    }

    public final String getPatientSex() {
        return ds.getString(Tags.PatientSex);
    }

    public final void setPatientSex(String patientSex) {
        ds.putCS(Tags.PatientSex, patientSex);
    }
    
    /**
     * Returns the list of studies.
     * <p>
     * Use the <code>childs</code> from <code>AbstractModel</code> method now.
     * 
     * @return studies as List.
     */
    public final List getStudies() {
        return listOfChilds();
    }

    /**
     * Set a new list of studies.
     * <p>
     * Use the <code>setChilds</code> from <code>AbstractModel</code> method now.
     * 
     * @param studies List of studies
     */
    public final void setStudies(List studies) {
        setChilds( studies );
    }
    
    public boolean isShowStudies() {
        return listOfChilds().size() > 0;
    }
    
    public static void setConsumerModel(XDSConsumerModel m) {
        consumerModel = m;
    }

    public static XDSConsumerModel getConsumerModel() {
        return consumerModel;
    }

    /**
     * @return the showXDS
     */
    public boolean isShowXDS() {
        return showXDS;
    }

    /**
     * @param showXDS the showXDS to set
     */
    public void setShowXDS(boolean showXDS) {
        this.showXDS = showXDS;
    }
    
    public List getXDSDocuments() {
        return showXDS ? consumerModel.getDocuments(getPatientID()) : null;
    }
    
}