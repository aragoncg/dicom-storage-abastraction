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

package org.dcm4chex.archive.web.maverick.gppps;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.gppps.model.GPPPSEntry;
import org.dcm4chex.archive.web.maverick.gppps.model.GPPPSModel;

/**
 * @author franz.willer
 *
 * The Maverick controller for Media Creation Manager.
 */
public class GPPPSConsoleCtrl extends Dcm4cheeFormController {


	/** the view model. */
	private GPPPSModel model;
	
	private static GPPPSDelegate delegate = null;

	/**
	 * Get the model for the view.
	 */
    protected Object makeFormBean() {
        if ( delegate == null ) {
        	delegate = new GPPPSDelegate();
        	delegate.init( getCtx().getServletConfig() );
        }
        model =  GPPPSModel.getModel(getCtx().getRequest());
        return model;
    }
	

	
    protected String perform() throws Exception {
        try {
            HttpServletRequest request = getCtx().getRequest();
    		model = GPPPSModel.getModel(request);
    		model.clearPopupMsg();
    		model.setGpppsIUIDs( request.getParameterValues("gpppsIUID"), false );
            if ( request.getParameter("filter.x") != null ) {//action from filter button
            	try {
	        		checkFilter( request );
	            	model.filterWorkList( true );
            	} catch ( Exception x ) {
            		model.setPopupMsg("folder.err_datetime", "yyyy/MM/dd" );
            	}
            } else if ( request.getParameter("prev.x") != null ) { 
        		model.performPrevious();
            } else if ( request.getParameter("next.x") != null ) { 
        		model.performNext();
            } else if ( request.getParameter("link.x") != null ) {//action from link button. (sticky support;redirect to gpwl ctrl.)
            	model.setGpppsIUIDs( request.getParameterValues( "gpppsIUID" ), true );
            	model.getFilter().setPatientName( request.getParameter("patientName"));
            	return "link";
            } else if ( request.getParameter("del.x") != null ) {//action from delete button.
            	if ( model.getGpppsIUIDs() != null ) {
	            	delegate.deleteGPPPSEntries(model.getGpppsIUIDs());
	            	model.setGpppsIUIDs(null, false);
	            	model.filterWorkList( true );
            	} else {
            		model.setPopupMsg("gppps.err_delete_selection","");
            	}
            } else {
            	String action = request.getParameter("action");
            	if ( action != null ) {
            		return performAction( action, request );
            	}
            }
            return SUCCESS;
        } catch (Exception e) {
            model.setPopupMsg("gppps.err", e.getMessage());
            return "error";
        }
    }

	/**
	 * @param action
	 * @param request
	 */
	private String performAction(String action, HttpServletRequest request) {
		if ( "linkDone".equals(action) ) {
        	model.filterWorkList( true );
		} else if ( "inspect".equals(action) ) {
            return inspect(request.getParameter("gpppsIUID"));
        }
        return SUCCESS;
	}


    private String inspect(String iuid) {
        if ( iuid != null ) {
            GPPPSEntry entry = model.getGPPPSEntry(iuid);
            if ( entry != null ) {
                this.getCtx().getRequest().getSession().setAttribute("dataset2view", entry.toDataset());
                return INSPECT;
            } else {
                model.setPopupMsg("gppps.err_inspect",iuid);
            }
        }
        return SUCCESS;   
    }

	/**
	 * Checks the http parameters for filter params and update the filter.
	 * 
	 * @param rq The http request.
	 * 
	 * @throws ParseException
	 * 
	 */
	private void checkFilter(HttpServletRequest rq) throws ParseException {
		PPSFilter filter = model.getFilter();
		if ( rq.getParameter("patientName") != null ) filter.setPatientName(rq.getParameter("patientName") );
		if ( rq.getParameter("startDate") != null ) filter.setStartDate(rq.getParameter("startDate") );
		if ( rq.getParameter("status") != null ) filter.setStatus(rq.getParameter("status") );
	}

	/**
	 * Returns the delegater that is used to query the MWLSCP or delete an MWL Entry (only if MWLSCP AET is local)
	 * 
	 * @return The delegator.
	 */
	public static GPPPSDelegate getGPPPSDelegate() {
		return delegate;
	}
	
	protected String getCtrlName() {
		return "gppps_console";
	}
	
}
