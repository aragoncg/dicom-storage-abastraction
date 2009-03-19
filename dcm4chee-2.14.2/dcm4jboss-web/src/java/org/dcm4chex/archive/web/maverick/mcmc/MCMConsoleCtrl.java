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

package org.dcm4chex.archive.web.maverick.mcmc;

import java.text.ParseException;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.ejb.interfaces.MediaDTO;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.mcmc.model.MCMFilter;
import org.dcm4chex.archive.web.maverick.mcmc.model.MCMModel;
import org.dcm4chex.archive.web.maverick.mcmc.model.MediaData;

/**
 * @author franz.willer
 *
 * The Maverick controller for Media Creation Manager.
 */
public class MCMConsoleCtrl extends Dcm4cheeFormController {

	/** the view model. */
	private MCMModel model;
	
	private static MCMScuDelegate delegate = null;

	/**
	 * Get the model for the view.
	 */
    protected Object makeFormBean() {
        if ( delegate == null ) {
        	delegate = new MCMScuDelegate();
        	delegate.init( getCtx().getServletConfig() );
        }
        return MCMModel.getModel(getCtx().getRequest());
    }
	

	
    protected String perform() throws Exception {
        try {
            HttpServletRequest request = getCtx().getRequest();
    		model = MCMModel.getModel(request);
    		model.clearPopupMsg();
    		if ( getPermissions().getPermissionsForApp("offline_storage").isEmpty() ) {
    			model.setPopupMsg("mcm.access_denied", model.getCurrentUser());
    			return SUCCESS;
    		}
            if ( request.getParameter("checkMCM") != null ) {
    			model.setMcmNotAvail( ! delegate.checkMcmScpAvail() );
    			model.setCheckAvail( true );
            } else {
            	model.setCheckAvail( false );
            }
            if ( request.getParameter("filter.x") != null ) {//action from filter button
        		checkFilter( request );
            	model.filterMediaList( true );
            } else if ( request.getParameter("nav") != null ) {//action from a nav button. (next or previous)
            	String nav = request.getParameter("nav");
            	if ( nav.equals("prev") ) {
            		model.performPrevious();
            	} else if ( nav.equals("next") ) {
            		model.performNext();
            	}
            } else {
            	String action = request.getParameter("action");
            	if ( action != null ) {
            		performAction( action, request );
            	}
            }
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }


	/**
	 * Performs an action request.
	 * A action request is indicated by an 'action' http parameter.
	 * <p>
	 * Set the MCMModel.ERROR_UNSUPPORTED_ACTION error code in model if <code>action</code> is not defined.
	 *  
	 * @param action	The value of action request patrameter.
	 * @param request	The http request.
	 */
	private void performAction(String action, HttpServletRequest request) {
		if ( action.equalsIgnoreCase("queue") ) {
			model.setMcmNotAvail( ! delegate.checkMcmScpAvail() );
			if ( model.isMcmNotAvail() ) return;
			int mediaPk = Integer.parseInt( request.getParameter("mediaPk"));
           	MediaData md = model.mediaDataFromList( mediaPk );
           	if ( md != null ) {
   			   model.updateMediaStatus( mediaPk, MediaDTO.QUEUED, "" );
	           try {
                       delegate.scheduleMediaCreation(md.asMediaDTO());
				} catch (Exception e) {
					
					model.updateMediaStatus( mediaPk, MediaDTO.ERROR, e.getMessage() );
				}
           	}
		} else if ( action.equalsIgnoreCase("delete") ) {
    		if ( !model.isAdmin() ) {
    			model.setPopupMsg("mcm.err_delete_notadmin", model.getCurrentUser());
    			return;
    		}
			if ( ! delegate.deleteMedia( Integer.parseInt( request.getParameter("mediaPk"))) ) {
                model.setPopupMsg( "mcm.err_delete", request.getParameter("mediaPk") );
			}
			model.filterMediaList( true );
		} else {
            model.setPopupMsg( "mcm.err_unknownAction", action );
		}
		
	}



	/**
	 * Checks the http parameters for filter params and update the filter.
	 * 
	 * @param rq The http request.
	 * 
	 * @return true if filter has been changed.
	 * 
	 * @throws ParseException
	 * 
	 */
	private boolean checkFilter(HttpServletRequest rq) throws ParseException {
		MCMFilter filter = model.getFilter();
		filter.setSelectedStati(rq.getParameterValues("mediaStatus") );
		if ( rq.getParameter("startDate") != null ) filter.setStartDate(rq.getParameter("startDate") );
		if ( rq.getParameter("endDate") != null ) filter.setEndDate(rq.getParameter("endDate") );
		if ( rq.getParameter("createOrUpdateDate") != null ) filter.setCreateOrUpdateDate(rq.getParameter("createOrUpdateDate") );
		return filter.isChanged();
	}
	
	public static MCMScuDelegate getMcmScuDelegate() {
		return delegate;
	}
	
	protected String getCtrlName() {
		return "offline_storage";
	}
	
}
