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

package org.dcm4chex.archive.web.maverick.gpwl;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.gpwl.model.GPWLEntry;
import org.dcm4chex.archive.web.maverick.gpwl.model.GPWLFilter;
import org.dcm4chex.archive.web.maverick.gpwl.model.GPWLModel;
import org.dcm4chex.archive.web.maverick.gpwl.GPWLScuDelegate;

/**
 * @author franz.willer
 *
 * The Maverick controller for General Purpose Worklist.
 */
public class GPWLConsoleCtrl extends Dcm4cheeFormController {


	/** the view model. */
	private GPWLModel model;
	
	private static GPWLScuDelegate delegate = null;

	/**
	 * Get the model for the view.
	 */
    protected Object makeFormBean() {
        if ( delegate == null ) {
        	delegate = new GPWLScuDelegate();
        	delegate.init( getCtx().getServletConfig() );
        }
        model =  GPWLModel.getModel(getCtx().getRequest());
        return model;
    }
	

	
    protected String perform() throws Exception {
        try {
            HttpServletRequest request = getCtx().getRequest();
    		model = GPWLModel.getModel(request);
    		model.clearPopupMsg();
            if ( request.getParameter("filter.x") != null ) {//action from filter button
            	try {
	        		checkFilter( request );
	            	model.filterWorkList( true );
            	} catch ( Exception x ) {
            		model.setPopupMsg( "folder.err_datetime", "yyyy/MM/dd" );
            	}
            } else if ( request.getParameter("nav") != null ) {//action from a nav button. (next or previous)
            	String nav = request.getParameter("nav");
            	if ( nav.equals("prev") ) {
            		model.performPrevious();
            	} else if ( nav.equals("next") ) {
            		model.performNext();
            	}
            } else if ( request.getParameter("del.x") != null ) {//action from delete button.
        		String[] gpspsIDs = getSPSIds(request);
        		if ( gpspsIDs == null || gpspsIDs.length < 1) {
        			model.setPopupMsg("gpwl.err_delete_selection", "");
        		} else {
        			for ( int i = 0 ; i < gpspsIDs.length ; i++ ) {
        				delegate.deleteGPWLEntry( gpspsIDs[i] );
        			}
        			model.filterWorkList( false );
        		}
            } else {
            	String action = request.getParameter("action");
            	if ( action != null ) {
            		return performAction( action, request );
            	}
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

	/**
	 * @param action
	 * @param request
	 * @throws ParseException
	 */
	private String performAction(String action, HttpServletRequest request) throws ParseException {
		if ( "delete".equalsIgnoreCase( action ) ) {
			getGPWLScuDelegate().deleteGPWLEntry(request.getParameter(""));
		} else if ( "inspect".equals(action)) {
		    return inspect(request.getParameter("gpwlIUID"));
        }
		return SUCCESS;
	}

    private String inspect(String iuid) {
        if ( iuid != null ) {
            GPWLEntry entry = model.getGPWLEntry(iuid);
            if ( entry != null ) {
                this.getCtx().getRequest().getSession().setAttribute("dataset2view", entry.toDataset());
                return INSPECT;
            } else {
                model.setPopupMsg("gpwl.err_inspect",iuid);
            }
        }
        return SUCCESS;   
    }
    
	/**
	 * @param request
	 * @return
	 */
	private String[] getSPSIds(HttpServletRequest request) {
		String[] result;
		if ( request.getParameter("gpspsID") != null ) {
			result = new String[]{request.getParameter("gpspsID")};
		} else {
			result = request.getParameterValues("sticky");
		}
		return result;
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
		GPWLFilter filter = model.getFilter();
		if ( rq.getParameter("iuid") != null ) {
			filter.clear();
		}
		filter.setIUID(rq.getParameter("iuid") );
		if ( rq.getParameter("patientName") != null ) filter.setPatientName(rq.getParameter("patientName") );
		if ( rq.getParameter("SPSStartDate") != null ) filter.setSPSStartDate(rq.getParameter("SPSStartDate") );
		if ( rq.getParameter("workitemCode") != null ) filter.setWorkitemCode(rq.getParameter("workitemCode") );
		if ( rq.getParameter("inputAvail") != null ) filter.setInputAvailability(rq.getParameter("inputAvail") );
		if ( rq.getParameter("status") != null ) filter.setStatus(rq.getParameter("status") );
		if ( rq.getParameter("priority") != null ) filter.setPriority(rq.getParameter("priority") );
		if ( rq.getParameter("accessionNumber") != null ) filter.setAccessionNumber(rq.getParameter("accessionNumber") );
	}

	/**
	 * Returns the delegater that is used to query the GPWLSCP or delete an GPWL Entry (only if GPWLSCP AET is local)
	 * 
	 * @return The delegator.
	 */
	public static GPWLScuDelegate getGPWLScuDelegate() {
		return delegate;
	}

	protected String getCtrlName() {
		return "gpwl_console";
	}
	
}
