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

package org.dcm4chex.archive.web.maverick;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.web.maverick.util.CodeItem;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 2867 $ $Date: 2006-10-23 17:33:36 +0200 (Mon, 23 Oct 2006) $
 * @since 14.10.2004
 *
 */
public class GPWLFeedDelegate {

    private static Logger log = Logger.getLogger(GPWLFeedDelegate.class);

    private static MBeanServer server;

    private static ObjectName gpwlFeedServiceName;


    void init(ControllerContext ctx) throws Exception {
        if (gpwlFeedServiceName != null) return;
        GPWLFeedDelegate.server = MBeanServerLocator.locate();
        String s = ctx.getServletConfig().getInitParameter("gpwlFeedServiceName");
        GPWLFeedDelegate.gpwlFeedServiceName = new ObjectName(s);
    }

    public List getHumanPerformerList() {
    	Object o = null;
    	List l = new ArrayList();
        try {
            o = server.getAttribute(gpwlFeedServiceName,
                    "HumanPerformerList");
        } catch (Exception e) {
            log.warn("Failed to get list of human performer!", e);
        }
        if ( o != null ) {
        	String[] sa = StringUtils.split( (String) o, ',');
        	for ( int i = 0 ; i < sa.length ; i++ ) {
        		if ( sa[i].length() > 3)
        			l.add( CodeItem.valueofCDM( sa[i] ) );//codeString format: <codeValue>[^<designator>]^<meaning>
        	}
        }
        return l;
    }

    public List getTemplateList() {
    	Object o = null;
        try {
            return (List) server.invoke(gpwlFeedServiceName,
                    "listTemplates",
                    new Object[] {},
                    new String[] {});
        } catch (Exception e) {
            log.warn("Failed to get template list!", e);
        }
        return new ArrayList();
    }
    
    public boolean addWorklistItem( long studyPk, String templateFile, String humanPerformer, long scheduleDate ) {
    	log.info("addWorklistItem: studyPk:"+studyPk+" templateFile:"+templateFile+" humanPerformer:"+humanPerformer+" scheduleDate:"+scheduleDate);
        try {
            	server.invoke(gpwlFeedServiceName,
                    "addWorklistItem",
                    new Object[] { new Long( studyPk ),
            					   templateFile,
								   humanPerformer,
								   new Long(scheduleDate)
            						},
                    new String[] { Long.class.getName(), String.class.getName(), String.class.getName(), Long.class.getName() });
            	
            	return true;
        } catch (Exception e) {
            log.warn("Failed to add new work list item!", e);
            return false;
        }
    }
    
}