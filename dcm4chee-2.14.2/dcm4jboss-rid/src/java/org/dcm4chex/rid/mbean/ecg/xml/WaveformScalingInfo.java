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
public class WaveformScalingInfo {
	private float pixPerUnit = -1f;
	private float pixPerSec = -1f;
	private float zeroLine = 0.5f;
	private String xScaleDesc = null;
	private String yScaleDesc = null;

	
	/**
	 * Creates a WaveformScalingInfo object.
	 * <p>
	 * zeroLine will be 0.5f (the default)
	 * 
	 * @param pixPerUnit
	 * @param pixPerSec
	 * @param scaleDesc
	 * @param scaleDesc2
	 */
	public WaveformScalingInfo(float pixPerSec,String xScaleDesc, float pixPerUnit, String yScaleDesc) {
		this.pixPerUnit = pixPerUnit;
		this.pixPerSec = pixPerSec;
		this.xScaleDesc = xScaleDesc;
		this.yScaleDesc = yScaleDesc;
	}

	/**
	 * @return Returns the pixPerSec.
	 */
	public float getPixPerSec() {
		return pixPerSec;
	}
	/**
	 * @return Returns the pixPerUnit.
	 */
	public float getPixPerUnit() {
		return pixPerUnit;
	}
	/**
	 * @return Returns the xScaleDesc.
	 */
	public String getXScaleDesc() {
		return xScaleDesc;
	}
	/**
	 * @return Returns the yScaleDesc.
	 */
	public String getYScaleDesc() {
		return yScaleDesc;
	}
	/**
	 * Returns the offset factor of the zero line ( line of value 0 ).
	 * <p>
	 * Default is 0.5f (in the middle of the area).
	 * <p>
	 * <dl>
	 * <dt>This value ( 0 &lt;= x &lt;= 1 ) is used to calculate the zero line position:</dt>
	 * <dd><b>  yPos0 = yTop+height*x</b></dd>
	 * <dd></dd>
	 * <dd>     yPos0....y-coord of zero line</dd>
	 * <dd>     yTop.....y-coord of upperleft corner of area</dd>
	 * <dd>     height...height of area</dd>
	 * <dd>     x........This zero line offset factor</dd>
	 * 
	 * @return Returns the zeroLine offset factor.
	 */
	public float getZeroLine() {
		return zeroLine;
	}
	/**
	 * Set the zero line offset factor.
	 * <p>
	 * @see getZeroLine()
	 * 
	 * @param zeroLine The zeroLine to set.
	 */
	public void setZeroLine(float zeroLine) {
		this.zeroLine = zeroLine;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("WaveformScalingInfo: pixPerSec:").append(pixPerSec).append(" (").append(xScaleDesc);
		sb.append(") pixPerUnit:").append(pixPerUnit).append(" (").append(yScaleDesc).append(") zeroLine:"+zeroLine);
		return sb.toString();
	}
}
