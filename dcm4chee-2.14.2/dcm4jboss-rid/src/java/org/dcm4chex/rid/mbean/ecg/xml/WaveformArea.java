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
public class WaveformArea {

	/** Type identifier for calibration pulse area. */ 
	public static final int TYPE_CAL_PULSE = 1;
	/** Type identifier for waveform area. */
	public static final int TYPE_WAVEFORM = 2;
	
	private int type;
	private float leftX;
	private float topY;
	private float width;
	private float height;
	private int waveformIndex = -1;
	private String waveformDescription = null;
	private WaveformScalingInfo scalingInfo;
	
	private WaveformArea( int type, float x, float y, float w, float h ) {
		this.type = type;
		leftX = x;
		topY = y;
		width = w;
		height = h;
	}
	private WaveformArea( int type, float x, float y, float w, float h, int idx, String desc, WaveformScalingInfo scale ) {
		this( type, x, y, w, h );
		waveformIndex = idx;
		waveformDescription = desc;
		scalingInfo = scale;
	}

	public static WaveformArea getCalPulseArea( float x, float y, float w, float h ){
		return new WaveformArea( TYPE_CAL_PULSE, x, y, w, h );
	}

	public static WaveformArea getWaveformArea( float x, float y, float w, float h, int idx, String desc, WaveformScalingInfo scale ){
		return new WaveformArea( TYPE_WAVEFORM, x, y, w, h, idx, desc, scale );
	}
	
	/**
	 * Returns the type of this area.
	 * 
	 * @return The type.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Returns the left position of this area.
	 * 
	 * @return X-coord of the upper-left corner in mm.
	 */
	public float getLeftX() {
		return leftX;
	}

	/**
	 * Returns the top position of this area.
	 * 
	 * @return The Y coord of the upper-left corner in mm.
	 */
	public float getTopY() {
		return topY;
	}
	/**
	 * Returns the heigth of this area.
	 * 
	 * @return height in mm.
	 */
	public float getHeight() {
		return height;
	}
	/**
	 * Returns the width of this area.
	 * 
	 * @return width in mm.
	 */
	public float getWidth() {
		return width;
	}
	/**
	 * Returns a description of the waveform for this area.
	 * 
	 * @return Returns the waveformDescription.
	 */
	public String getWaveformDescription() {
		return waveformDescription;
	}
	/**
	 * Returns the channel index of the Waveform for this area.
	 * 
	 * @return Returns the waveformIndex or -1 if type is not TYPE_WAVEFORM.
	 */
	public int getWaveformIndex() {
		return waveformIndex;
	}
	/**
	 * @return Returns the scalingInfo.
	 */
	public WaveformScalingInfo getScalingInfo() {
		return scalingInfo;
	}
}
