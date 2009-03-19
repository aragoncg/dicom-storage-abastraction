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

package org.dcm4chex.archive.web.maverick.util;

import org.dcm4cheri.util.StringUtils;

/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2287 $ $Date: 2006-02-21 13:46:37 +0100 (Tue, 21 Feb 2006) $
 * @since Feb 15, 2006
 */
public class CodeItem {
	private String codeValue;
	private String codeDesignator = null;
	private String codeMeaning;
	
	public CodeItem( String codeValue, String codeMeaning ) {
		this.codeValue = codeValue;
		this.codeMeaning = codeMeaning;
	}
	public CodeItem( String codeValue, String codeMeaning, String codeDesignator ) {
		this( codeValue, codeMeaning );
		this.codeDesignator = codeDesignator;
	}
	
	/**
	 * Create a CodeItem object from a String representation.
	 * <p>
	 * Format of codeString: (CDM) &lt;code&gt;[^&lt;designator&gt;]^&lt;meaning&gt; 
	 *  
	 * @param codeString
	 * @return
	 */
	public static CodeItem valueofCDM( String codeString ) {
		if ( codeString == null )
			throw new IllegalArgumentException("codeString must not be null!");
		String[] sa = StringUtils.split( codeString, '^' );
		if ( sa.length > 2 ) {
			return new CodeItem( sa[0], sa[2], sa[1] );
		} else if ( sa.length > 1 ) {
			return new CodeItem( sa[0], sa[1] );
		} else {
			throw new IllegalArgumentException("codeString must contain at least CodeValue and CodeMeaning!");
		}
	}

	/**
	 * Create a CodeItem object from a String representation.
	 * <p>
	 * Format of codeString: (DCM) [&lt;designator&gt;^]&lt;code&gt;^&lt;meaning&gt; 
	 *  
	 * @param codeString
	 * @return
	 */
	public static CodeItem valueofDCM( String codeString ) {
		if ( codeString == null )
			throw new IllegalArgumentException("codeString must not be null!");
		String[] sa = StringUtils.split( codeString, '^' );
		if ( sa.length > 2 ) {
			return new CodeItem( sa[1], sa[2], sa[0] );
		} else if ( sa.length > 1 ) {
			return new CodeItem( sa[0], sa[1] );
		} else {
			throw new IllegalArgumentException("codeString must contain at least CodeValue and CodeMeaning!");
		}
	}
	
	/**
	 * @return Returns the codeDesignator.
	 */
	public String getCodeDesignator() {
		return codeDesignator;
	}
	/**
	 * @param codeDesignator The codeDesignator to set.
	 */
	public void setCodeDesignator(String codeDesignator) {
		this.codeDesignator = codeDesignator;
	}
	/**
	 * @return Returns the codeMeaning.
	 */
	public String getCodeMeaning() {
		return codeMeaning;
	}
	/**
	 * @param codeMeaning The codeMeaning to set.
	 */
	public void setCodeMeaning(String codeMeaning) {
		this.codeMeaning = codeMeaning;
	}
	/**
	 * @return Returns the codeValue.
	 */
	public String getCodeValue() {
		return codeValue;
	}
	/**
	 * @param codeValue The codeValue to set.
	 */
	public void setCodeValue(String codeValue) {
		this.codeValue = codeValue;
	}
	
	/**
	 * Return this CodeItem as String
	 * <p>
	 * Format:&lt;codeValue&gt;^&lt;codeMeaning&gt;[&lt;designator&gt;]
	 */
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(codeValue).append('^').append(codeMeaning);
		if ( codeDesignator != null )
			sb.append('^').append(codeDesignator);
		return sb.toString();
	}
}
