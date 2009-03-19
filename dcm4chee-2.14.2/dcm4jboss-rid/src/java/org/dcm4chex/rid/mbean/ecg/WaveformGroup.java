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

import java.nio.ByteBuffer;

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
public class WaveformGroup {

	private int grpIndex;
	private int nrOfChannels;
	private int nrOfSamples;
	private int bitsAlloc;
	private float sampleFreq;//in Hz
	private String muxGrpLabel;
	private String sampleInterpretation;
	private WaveFormChannel[] channels = null;
	private ByteBuffer data = null;
	
	private String cuid;

	private static Logger log = Logger.getLogger( WaveformGroup.class.getName() );
		
	/**
	 * @param elem
	 */
	public WaveformGroup(String cuid, DcmElement elem, int grpIndex, float fCorr) {
		this.cuid = cuid;
		if ( elem == null ) throw new NullPointerException( "WaveFormSequence missing!");
		Dataset ds = elem.getItem( grpIndex );
		this.grpIndex = grpIndex;
		nrOfChannels = ds.getInt( Tags.NumberOfWaveformChannels, 0 );
		nrOfSamples = ds.getInt( Tags.NumberOfWaveformSamples, 0 );
		sampleFreq = ds.getFloat( Tags.SamplingFrequency, 0f );
		muxGrpLabel = ds.getString( Tags.MultiplexGroupLabel );
		bitsAlloc = ds.getInt( Tags.WaveformBitsAllocated, 0 );
		sampleInterpretation = ds.getString( Tags.WaveformSampleInterpretation );
		data = ds.getByteBuffer( Tags.WaveformData );
		if ( nrOfSamples < 1 ) { //nrOfSamples is not correct?!!
			int nrOfSamples_recalc = data.limit() / nrOfChannels;
			if ( bitsAlloc > 8 ) {
				nrOfSamples_recalc /= bitsAlloc / 8;
			}
			log.warn("NumberOfWaveformSamples ("+nrOfSamples+") not valid! Recalc with WaveformData:"+nrOfSamples_recalc);
			nrOfSamples = nrOfSamples_recalc;
			if ( log.isDebugEnabled() ) log.debug("Recalculated NumberOfWaveformSamples is "+nrOfSamples+
					"! ( WaveformData.size:"+data.limit()+" nrOfChannels:"+nrOfChannels+" bitsAlloc:"+bitsAlloc+" )" );
		}
		prepareChannels( ds.get( Tags.ChannelDefinitionSeq ), fCorr );
	}
	
	/**
	 * Returns the SOP Class UID of this waveform.
	 * 
	 * @return
	 */
	public String getCUID() {
		return cuid;
	}
	
	/**
	 * @return Returns the nrOfSamples.
	 */
	public int getNrOfSamples() {
		return nrOfSamples;
	}
	/**
	 * @return Returns the nrOfChannels.
	 */
	public int getNrOfChannels() {
		return nrOfChannels;
	}
	
	/**
	 * @return Returns the bitsAlloc.
	 */
	public int getBitsAlloc() {
		return bitsAlloc;
	}
	/**
	 * @return Returns the sampleFreq.
	 */
	public float getSampleFreq() {
		return sampleFreq;
	}
	public WaveFormChannel getChannel( int idx ) {
		return channels[idx];
	}

	/**
	 * @return
	 */
	public String getFilterText() {
		if ( channels[0].getLowFreq() != null )
			return channels[0].getLowFreq()+"-"+channels[0].getHighFreq()+" Hz";
		else
			return "No Filter!";
	}
	
	/**
	 * @param element
	 */
	private void prepareChannels( DcmElement chDefs, float fCorr ) {
		int len = chDefs.countItems();
		channels = new WaveFormChannel[ len ];
		WaveFormChannel ch;
		for ( int i = 0 ; i < len ; i++ ) {
			ch = new WaveFormChannel( this, chDefs.getItem(i), getWaveFormBuffer(i), fCorr );
			channels[i] = ch;
		}
		
	}
	
	/**
	 * @param i
	 * @return
	 */
	private WaveFormBuffer getWaveFormBuffer(int idx) {
		if ( this.bitsAlloc == 8 ) {
			return new WaveForm8Buffer( data, idx, nrOfChannels, sampleInterpretation );
		} else if ( bitsAlloc == 16 ) {
			return new WaveForm16Buffer( data, idx, nrOfChannels, sampleInterpretation );
		} else {
			return null;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("WaveFormGroup(").append(grpIndex).append("):").append(muxGrpLabel);
		sb.append(" channels:").append( nrOfChannels ).append(" samples:").append( nrOfSamples );
		sb.append(" sampleFreq:").append( sampleFreq ).append(" channelDefs:").append( channels );
		return sb.toString();
	}


}
