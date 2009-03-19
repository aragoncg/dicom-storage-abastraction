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

package org.dcm4chex.rid.mbean.ecg.xml;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TwelveLeadTemplate implements WaveformTemplate {

	public static final float MIN_WIDTH = 1000;
	public static final float MIN_HEIGHT = 1000;
	public static final float CAL_PULSE_WIDTH = 120;
	public static final WaveformScalingInfo SCALING_INFO = new WaveformScalingInfo(250f,null,100f,"mV");
	
	private float width;
	private float height;
	private WaveformArea[] calPulseAreas = new WaveformArea[6];
	private WaveformArea[] wfAreas = new WaveformArea[12];

	public TwelveLeadTemplate( float w, float h ) {
		if ( w < MIN_WIDTH | h < MIN_HEIGHT ) {
			throw new IllegalArgumentException("Template size too small ( "+w+" x "+h+" )! Min size: "+MIN_WIDTH+" x "+ MIN_HEIGHT );
		}
		width = w;
		height = h;
		initAreas();
	}
	
	private void initAreas() {
		float lineHeight = height / 6f;
		float wfWidth = ( width - CAL_PULSE_WIDTH ) / 2f;
		float rightWaveformX = CAL_PULSE_WIDTH + wfWidth;
		float topY = 0;
		for ( int i = 0 ; i < 6 ; i++, topY += lineHeight ) {
			calPulseAreas[i] = WaveformArea.getCalPulseArea( 0, topY, CAL_PULSE_WIDTH, lineHeight );
			wfAreas[i] = WaveformArea.getWaveformArea( CAL_PULSE_WIDTH, topY, wfWidth, lineHeight, i, null, SCALING_INFO );
			wfAreas[i+6] = WaveformArea.getWaveformArea( rightWaveformX, topY, wfWidth, lineHeight, i+6, null, SCALING_INFO );
		}
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.mbean.ecg.xml.WaveformTemplate#getWidth()
	 */
	public float getWidth() {
		return width;
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.mbean.ecg.xml.WaveformTemplate#getHeight()
	 */
	public float getHeight() {
		return height;
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.mbean.ecg.xml.WaveformTemplate#getAreas()
	 */
	public WaveformArea[] getAreas() {
		WaveformArea[] all = new WaveformArea[18];
		System.arraycopy( calPulseAreas, 0, all, 0, 6 );
		System.arraycopy( wfAreas, 0, all, 6, 12 );
		return all;
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.mbean.ecg.xml.WaveformTemplate#getCalPulseAreas()
	 */
	public WaveformArea[] getCalPulseAreas() {
		return calPulseAreas;
	}

	/* (non-Javadoc)
	 * @see org.dcm4chex.rid.mbean.ecg.xml.WaveformTemplate#getWaveformAreas()
	 */
	public WaveformArea[] getWaveformAreas() {
		return wfAreas;
	}
	
	public String getFooterText() {
		return "25mm/sec 10mm/mV";
	}


}
