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

package org.dcm4chex.rid.mbean.ecg;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WaveformInfo {
	private Dataset dataset;
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm:ss");

	public WaveformInfo( Dataset ds ) {
		dataset = ds;
	}
	
	public String getPatientName() {
		String s = dataset.getString( Tags.PatientName );
		return s == null ? "":s.replace('^', ' ');
	}
	public String getPatientID() {
		String s = dataset.getString( Tags.PatientID );
		return s == null ? "":s;
	}

	public String getAcquisTime() {
		Date d = dataset.getDate( Tags.AcquisitionDatetime );
		if ( d == null ) {
			d = dataset.getDate( Tags.AcquisitionTime );
		}
		if ( d == null ) return "??:??:??";
		return timeFormatter.format( d );
	}
	
	public String getAcquisDate() {
		Date d = dataset.getDate( Tags.AcquisitionDatetime );
		if ( d == null ) {
			d = dataset.getDate( Tags.AcquisitionDate );
		}
		return getDateString( d );
	}
	
	public String getBirthday() {
		Date d = dataset.getDate( Tags.PatientBirthDate );
		return getDateString( d );
	}
	
	public String getSex() {
		String sex = dataset.getString( Tags.PatientSex );
		if ( sex != null ) {
			if ( "M".equalsIgnoreCase( sex ) ) return "Male";
			if ( "F".equalsIgnoreCase( sex ) ) return "Female";
		}
		return "Other";
	}
	
	public String getPatientSize() {
		return (dataset.getFloat( Tags.PatientSize, 0f )*100 )+" cm";
	}

	public String getPatientWeight() {
		return dataset.getFloat( Tags.PatientWeight, 0f )+" kg";
	}
	
	private String getDateString( Date d ) {
		if ( d == null ) return "????-??-??";
		return dateFormatter.format( d );
	}	
}
