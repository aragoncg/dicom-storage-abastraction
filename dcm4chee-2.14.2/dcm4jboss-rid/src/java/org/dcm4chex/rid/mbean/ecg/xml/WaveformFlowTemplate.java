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
public class WaveformFlowTemplate implements WaveformTemplate {

	public static final float MIN_WIDTH = 1000;
	public static final float MIN_HEIGHT = 1000;
	public static final float CAL_PULSE_WIDTH = 120;
	
	private float width;
	private float height;
	private int nrOfChannels;
	private boolean useCalPulse;
	private WaveformArea[] calPulseAreas;
	private WaveformArea[] wfAreas;

	public WaveformFlowTemplate( float w, float h, int nrOfChannels, boolean useCalPulse ) {
		if ( w < MIN_WIDTH | h < MIN_HEIGHT ) {
			throw new IllegalArgumentException("Template size too small ( "+w+" x "+h+" )! Min size: "+MIN_WIDTH+" x "+ MIN_HEIGHT );
		}
		width = w;
		height = h;
		this.nrOfChannels = nrOfChannels;
		this.useCalPulse = useCalPulse;
		initAreas();
	}
	
	private void initAreas() {
		float lineHeight = height / nrOfChannels;
		float wfWidth = width;
		float wfX = 0;
		wfAreas = new WaveformArea[ nrOfChannels ];
		if ( useCalPulse ) {
			wfWidth -= CAL_PULSE_WIDTH;
			wfX += CAL_PULSE_WIDTH;
			calPulseAreas = new WaveformArea[ nrOfChannels ];
		}
		float topY = 0;
		for ( int i = 0 ; i < nrOfChannels ; i++, topY += lineHeight ) {
			if ( useCalPulse ) calPulseAreas[i] = WaveformArea.getCalPulseArea( 0, topY, CAL_PULSE_WIDTH, lineHeight );
			wfAreas[i] = WaveformArea.getWaveformArea( wfX, topY, wfWidth, lineHeight, i, null, null );
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
		if ( useCalPulse ) {
			WaveformArea[] all = new WaveformArea[ calPulseAreas.length << 1 ];
			System.arraycopy( calPulseAreas, 0, all, 0, calPulseAreas.length );
			System.arraycopy( wfAreas, 0, all, calPulseAreas.length, calPulseAreas.length << 1 );
			return all;
		}
		return wfAreas;
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
		return null;
	}

}
