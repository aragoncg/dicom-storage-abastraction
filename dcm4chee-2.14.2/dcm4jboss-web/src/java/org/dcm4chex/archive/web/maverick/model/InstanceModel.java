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

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.common.PrivateTags;
import org.dcm4chex.archive.util.Convert;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 5189 $ $Date: 2007-09-27 14:32:28 +0200 (Thu, 27 Sep 2007) $
 * @since 05.10.2004
 *
 */
public class InstanceModel extends AbstractModel {

	private List files = null;
	
    public static Object valueOf(Dataset ds) {
        String cuid = ds.getString(Tags.SOPClassUID);
        if (UIDs.GrayscaleSoftcopyPresentationStateStorage.equals(cuid))
                return new PresentationStateModel(ds);
        if (UIDs.BasicTextSR.equals(cuid) || UIDs.EnhancedSR.equals(cuid)
                || UIDs.ComprehensiveSR.equals(cuid)
                || UIDs.KeyObjectSelectionDocument.equals(cuid))
                return new StructuredReportModel(ds);
        if ( UIDs.TwelveLeadECGWaveformStorage.equals(cuid)
        	|| UIDs.GeneralECGWaveformStorage.equals(cuid)
        	|| UIDs.AmbulatoryECGWaveformStorage.equals(cuid)
        	|| UIDs.HemodynamicWaveformStorage.equals(cuid)
        	|| UIDs.CardiacElectrophysiologyWaveformStorage.equals(cuid)
        	|| UIDs.BasicVoiceAudioWaveformStorage.equals(cuid))
        	return new WaveformModel(ds);
        if ( UIDs.EncapsulatedPDFStorage.equals(cuid)) 
        	return new StructuredReportModel(ds);
        if ( ds.getString(Tags.MIMETypeOfEncapsulatedDocument) != null) {
        	return new EncapsulatedModel(ds);
        }
        return new ImageModel(ds);
    }

    private final long pk;

    public InstanceModel(Dataset ds) {
        super(ds);
        ds.setPrivateCreatorID(PrivateTags.CreatorID);
        ByteBuffer bb = ds.getByteBuffer(PrivateTags.InstancePk);
        this.pk = bb == null ? -1 : Convert.toLong(bb.array());
    }

    public final long getPk() {
        return pk;
    }

    public int hashCode() {
    	return (int)( pk ^ pk >>> 32);//like Long.hashCode()
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesModel)) return false;
        InstanceModel other = (InstanceModel) o;
        return pk == other.pk;
    }

    public final String getContentDateTime() {
        return getDateTime(Tags.ContentDate, Tags.ContentTime);
    }

    public final String getInstanceNumber() {
        return ds.getString(Tags.InstanceNumber);
    }

    public final String getSopCUID() {
        return ds.getString(Tags.SOPClassUID);
    }

    public final String getSopIUID() {
        return ds.getString(Tags.SOPInstanceUID);
    }

    public final String getAvailability() {
        return ds.getString(Tags.InstanceAvailability);
    }

    public final String getRetrieveAETs() {
        return StringUtils.toString(ds.getStrings(Tags.RetrieveAET), '\\');
    }
    
    public List getFiles() {
    	return files;
    }
    
    public void setFiles( List files ) {
    	if ( files == null || files.isEmpty() ) {
    		this.files = null;
    	} else {
    		this.files = files;
    	}
    }
    
    
 }