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

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.dict.Tags;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WaveFormChannel {

	private WaveformGroup wfGrp;
	private int chNr;
	private String chSource;
	private int bitsStored;
	private Integer minValue;
	private Integer maxValue;
	private String label;
	private String status;
	
	private float sensitivity;
	private float channelSensitivity;
	private String sensitivityUnit;
	private float sensitivityCorrection;
	private float fCorr;
	private float pixPerUnit;
	
	private WaveFormBuffer buffer;
	private Float lowFreq;
	private Float highFreq;
	
	private static Logger log = Logger.getLogger( WaveFormChannel.class.getName() );

	/**
	 * @param buffer
	 * @param item
	 */
	public WaveFormChannel(WaveformGroup grp, Dataset ch, WaveFormBuffer buffer, float fCorr) {
		wfGrp = grp;
		this.fCorr = fCorr;
		chNr = ch.getInt( Tags.WaveformChannelNumber, -1);
		chSource = ch.get(Tags.ChannelSourceSeq).getItem().getString( Tags.CodeMeaning);
		try {
			bitsStored = ch.getInt( Tags.WaveformBitsStored, -1 );
		} catch ( UnsupportedOperationException x ) {
			bitsStored = ch.getFloat( Tags.WaveformBitsStored ).intValue();
		}
		minValue = ch.getInteger( Tags.ChannelMinimumValue );//min (max) should have the same VR as waveform data!
		maxValue = ch.getInteger( Tags.ChannelMaximumValue );
		label = ch.getString( Tags.ChannelLabel );
		status = ch.getString( Tags.ChannelStatus );
		
		sensitivity = channelSensitivity = ch.getFloat( Tags.ChannelSensitivity, 1f );
		sensitivityCorrection = ch.getFloat( Tags.ChannelSensitivityCorrectionFactor, 1f );
		sensitivityUnit = getSensitivityUnit( ch, "" );
		if ( fCorr != 1f ) sensitivity *= fCorr;
		sensitivity *= sensitivityCorrection; 
		
		lowFreq = ch.getFloat( Tags.FilterLowFrequency );
		highFreq = ch.getFloat( Tags.FilterHighFrequency );
		this.buffer = buffer;
		if ( log.isDebugEnabled() ) logInfo();
	}
	
	/**
	 * @return Returns the wfGrp.
	 */
	public WaveformGroup getWaveformGroup() {
		return wfGrp;
	}
	/**
	 * @param ch
	 * @return
	 */
	private String getSensitivityUnit(Dataset ch, String def) {
		DcmElement sensSeq = ch.get(Tags.ChannelSensitivityUnitsSeq);
		if ( sensSeq != null) {
			Dataset ds = sensSeq.getItem();
			if ( ds != null) {
				if ( "UCUM".equals(ds.getString(Tags.CodingSchemeDesignator)) ) {
					return ds.getString( Tags.CodeValue );
				} else {
					return ds.getString(Tags.CodeMeaning);
				}
			}
		}
		return def;
	}

	/**
	 * @return Returns the chSource.
	 */
	public String getChSource() {
		return chSource;
	}
	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * @return
	 */
	public Float getLowFreq() {
		return lowFreq;
	}

	/**
	 * @return
	 */
	public Float getHighFreq() {
		return highFreq;
	}

	/**
	 * @return Returns the sensitivity.
	 */
	public float getSensitivity() {
		return sensitivity;
	}
	
	
	
	public int getRawValue() {
		return buffer.getValue();
	}
	
	public int getRawValue( int sampleNr ) {
		return buffer.getValue( sampleNr );
	}
	
	/**
	 * @return Returns the maxValue.
	 */
	public Integer getMaxValue() {
		return maxValue;
	}
	/**
	 * @return Returns the minValue.
	 */
	public Integer getMinValue() {
		return minValue;
	}
	/**
	 * @return Returns the sensitivityUnit.
	 */
	public String getUnit() {
		return sensitivityUnit;
	}
	/**
	 * This method reset the channel. 
	 * Therefore next getValue() call will return first sample!
	 *
	 */
	public void reset() {
		buffer.reset();
	}
	
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("WaveFormChannel: chNr").append(chNr).append(" label:").append(label);
		sb.append(" bitsStored:").append( bitsStored).append(" min:").append(minValue).append(" max:").append(maxValue);
		sb.append(" status:").append(status);
		return sb.toString();
	}

	/**
	 * @param ch
	 */
	private void logInfo() {
		log.debug("WaveFormchannel: chNr:"+chNr);
		log.debug("   chSource:"+chSource);
		log.debug("   bitsStored:"+bitsStored);
		log.debug("   minValue:"+minValue);
		log.debug("   maxValue:"+maxValue);
		log.debug("   status:"+status);
		log.debug("   lowFreq:"+lowFreq);
		log.debug("   highFreq:"+highFreq);
		log.debug("   sensitivityUnit:"+sensitivityUnit);
		log.debug("   calculated sensitivity:"+sensitivity);
		log.debug("      ChannelSensitivity:"+channelSensitivity);
		log.debug("      sensitivityCorrection:"+sensitivityCorrection);
		log.debug("      fCorr:"+fCorr);
		log.debug("   Buffer:"+buffer);
	}
	
	/**
	 * @return
	 */
	public float getValue() {
		return getRawValue() * sensitivity;//TODO all the other things to get the real value!
	}
	
	/**
	 * Calculates min and max value for given samples (start..(end-1)).
	 * <p>
	 * Return the float array with min value at index 0 and max at index 1.
	 * <p>
	 * This method reset the channel. Therefore next getValue() call will return first sample!
	 * 
	 * @param start The first sample
	 * @param end The last sample (exclusive)
	 * 
	 * @return array with min and max value.
	 */
	public float[] calcMinMax(int start, int end) {
		int min = buffer.getValue(start);
		int max = min;
		int value;
		for ( int i = start+1 ; i < end ; i++ ) {
			value = buffer.getValue(i);
			if ( value < min ) min = value;
			else if ( value > max ) max = value;
		}
		buffer.reset();
		return new float[]{ min*sensitivity, max*sensitivity};
	}
	
	public void applyAreaScaling(String unit) {
		if (sensitivityUnit != null && unit != null && !sensitivityUnit.equals(unit)) {
			float f1 = getUnitFactor(sensitivityUnit);
			float f2 = getUnitFactor(unit);
			if ( f1 != f2 ) {
				sensitivity *= f1 / f2;
				log.debug("Sensitivity corrected! Units: (source:"+sensitivityUnit+" area:"+unit+" -> new value:"+sensitivity);
			}
		}
	}
	private float getUnitFactor(String unit) {
		if (unit.length() < 2) return 1f;
		switch (unit.charAt(0)) {
		case 'u': return 1e-6f;
		case 'm': return 1e-3f;
		case 'k': return 1000f;
		case 'M': return 1e6f;
		case 'G': return 1e9f;
		case 'n': return 1e-9f;
		case 'p': return 1e-12f;
		case 'd': return 0.1f;
		case 'c': return 0.01f;
			default:
				return 1f;
		}
	}

}
