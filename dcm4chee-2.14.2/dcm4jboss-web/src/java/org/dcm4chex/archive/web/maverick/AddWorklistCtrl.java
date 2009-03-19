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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;


/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 2575 $ $Date: 2006-06-28 18:51:48 +0200 (Wed, 28 Jun 2006) $
 * @since 5.10.2004
 *
 */
public class AddWorklistCtrl extends Dcm4cheeFormController {

    /** Popup message */
    private String popupMsg = null;

    private long studyPk;
	private static GPWLFeedDelegate delegate = null;;

	private String template;
	private String humanPerformer;
	private long scheduleDate = System.currentTimeMillis()+60000;
	
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    public final long getStudyPk() {
        return studyPk;
    }

    public final void setStudyPk(long pk) {
        this.studyPk = pk;
    }

	/**
	 * @return Returns the humanPerformer.
	 */
	public String getHumanPerformer() {
		return humanPerformer;
	}
	/**
	 * @param humanPerformer The humanPerformer to set.
	 */
	public void setHumanPerformer(String humanPerformer) {
		this.humanPerformer = humanPerformer;
	}
	/**
	 * @return Returns the scheduleDate.
	 */
	public String getScheduleDate() {
		return formatter.format( new Date(scheduleDate) );
	}
	/**
	 * @param scheduleDate The scheduleDate to set.
	 * @throws ParseException
	 */
	public void setScheduleDate(String scheduleDate) throws ParseException {
		this.scheduleDate = formatter.parse(scheduleDate).getTime();
	}
	/**
	 * @return Returns the template.
	 */
	public String getTemplate() {
		return template;
	}
	/**
	 * @param template The template to set.
	 */
	public void setTemplate(String template) {
		this.template = template;
	}
    public List getHumanPerformerList() {
    	return delegate.getHumanPerformerList();
    }
    public List getTemplateList() {
    	return delegate.getTemplateList();
    }
    
	/**
	 * @return Returns the popupMsg.
	 */
	public String getPopupMsg() {
		return popupMsg;
	}

    protected void init() {
    	popupMsg = null;
        if ( delegate  == null ) {
        	delegate = new GPWLFeedDelegate();
        	try {
        		delegate.init( getCtx() );
        	} catch( Exception x ) {
        		x.printStackTrace();
        	}
        }
    }

    protected String perform() throws Exception {
    	init();
        HttpServletRequest rq = getCtx().getRequest();
        if (rq.getParameter("add") != null
                || rq.getParameter("add.x") != null) { return addWorklistItem(); }
        if (rq.getParameter("cancel") != null
                || rq.getParameter("cancel.x") != null) { return "cancel"; }
    	return SUCCESS;
    }

	/**
	 * @return
	 * @throws ParseException
	 */
	private String addWorklistItem() throws ParseException {
		if ( delegate.addWorklistItem( studyPk, template, humanPerformer, scheduleDate ) ) {
			//popupMsg = "New worklist item added!"; (see maverick.xml view type folder in command addWorklist)
			return "folder";
		} else {
			popupMsg = "Failed to add new worklist item!";
			return SUCCESS;//to open AddWorklistCtrl!
		}
	}
    
}