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
 * Java(TM), hosted at http://sourceforge.net/projects/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See listed authors below. 
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

package org.dcm4chex.archive.web.conf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParserFactory;

import org.dcm4chex.archive.exceptions.ConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Franz Willer <franz.willer@gmail.com>
 * @version $Id:  $
 * @since Oct 19, 2007
 */
public class WebRolesConfig extends DefaultHandler {
    private static final String CONFIG_URL = "resource:dcm4chee-web/dcm4chee-webroles-cfg.xml";

	public static final String DEFAULT_ROLE_TYPE = "WebUser";

    private final Map roles = new LinkedHashMap();
    private final Set types = new HashSet();
    private final Map actions = new LinkedHashMap();
   
    public WebRolesConfig() {
    	loadFrom(CONFIG_URL);
    }

    public Collection getTypes() {
    	return types;
    }

    public Collection getRoles() {
    	return roles.values();
    }
    public Map getActions() {
        return actions;
    }
    
    public WebRole getRole(String name ) {
    	return (WebRole) roles.get(name);
    }
    public void addRole(String name, String descr) {
    	roles.put(name, new WebRole(name, descr));
    }
    public String getDependencyForRole( String name ) {
    	WebRole role = getRole(name);
    	return role == null ? null : role.getDependency();
    }

    public void loadFrom(String url) throws ConfigurationException {
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(url, this);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Failed to load WEB roles config from " + url, e);
        }
    }

    public void startElement(String uri, String localName, String qName,
            Attributes attrs) throws SAXException {
        if (qName.equals("role")) {
            roles.put( attrs.getValue("name"), new WebRole( attrs ) );
        } else if (qName.equals("action")) {
            actions.put( attrs.getValue("name"), attrs.getValue("descr") );
        }
    }
    
    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    public void endDocument() throws SAXException {
    	if (roles.isEmpty()) {
            throw new SAXException("missing role element ");    		
    	}
        if (actions.isEmpty()) {
            throw new SAXException("missing action element");                   
        }
    }
    
    public class WebRole {
    	String name;
    	String displayName;
    	String type;
    	String descr;
    	String dependency;
    	
		public WebRole(Attributes attrs) {
        	name = attrs.getValue("name");
        	displayName = attrs.getValue("display");
        	type = attrs.getValue("type");
        	if ( type == null )
        		type = DEFAULT_ROLE_TYPE;
        	types.add(type);
        	descr = attrs.getValue("descr");
        	dependency = attrs.getValue("dependency");
    	}
		public WebRole(String name, String descr ) {
			this.name = name;
			this.descr = descr;
		}

		public String getName() {
			return name;
		}
		public String getDisplayName() {
			return displayName == null ? name : displayName;
		}

		public String getType() {
			return type;
		}

		public String getDescr() {
			return descr;
		}

		public String getDependency() {
			return dependency;
		}
    }

}
