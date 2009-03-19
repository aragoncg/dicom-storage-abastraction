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

package org.dcm4chex.wado.mbean.xml;

import java.util.Iterator;
import java.util.Properties;

import javax.xml.transform.sax.TransformerHandler;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class XMLUtil {
	javax.swing.AbstractButton v;
	public static final AttributesImpl EMPTY_ATTRIBUTES = new AttributesImpl();
	
    private TransformerHandler th = null;
    
    private String nameSpacePrefix = null;
	
    public XMLUtil( TransformerHandler handle ) {
    	th = handle;
    }

    public XMLUtil( TransformerHandler handle, String nameSpacePrefix ) {
    	th = handle;
    	setNameSpace( nameSpacePrefix );
    }
    
    public void setNameSpace( String ns ) {
    	if ( ns != null && ! ns.endsWith( ":" ) ) ns = ns + ":";
    	nameSpacePrefix = ns;
    }

    public void startElement( String name, Attributes attr ) throws SAXException {
    	if ( nameSpacePrefix != null ) name = nameSpacePrefix + name; 
    	if ( attr == null ) attr = XMLUtil.EMPTY_ATTRIBUTES;
	    th.startElement("", name, name, attr );
	}
    
    public void startElement( String name, String attrName, String attrValue ) throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        addAttribute( attr, attrName, attrValue );
        startElement( name, attr );
    }
    
    public void endElement( String name ) throws SAXException {
    	if ( nameSpacePrefix != null ) name = nameSpacePrefix + name; 
    	th.endElement("", name, name );
	}
    
    public void singleElement( String name, Attributes attr, String value ) throws SAXException {
    	startElement( name, attr );
    	addValue( value );
    	endElement( name );
    }
    
    public AttributesImpl newAttribute( String name, String value ) {
        AttributesImpl attr = new AttributesImpl();
        addAttribute( attr, name, value );
    	return attr;
    }
	
    public void addAttribute( AttributesImpl attr, String name, String value ) {
		if ( value == null ) return;
		attr.addAttribute("", name, name, "", value);		
	}

    public void addAttributes( AttributesImpl attr, Properties props ) {
     if ( props != null ) {
     	Iterator iter = props.keySet().iterator();
     	String key;
     	while ( iter.hasNext() ) {
     		key = iter.next().toString();
     		addAttribute( attr, key, props.getProperty( key ) );
     	}
     }
	}
	
    public void addValue( String value ) throws SAXException {
    	if ( value != null )
    		th.characters(value.toCharArray(), 0, value.length() );
	}

}
