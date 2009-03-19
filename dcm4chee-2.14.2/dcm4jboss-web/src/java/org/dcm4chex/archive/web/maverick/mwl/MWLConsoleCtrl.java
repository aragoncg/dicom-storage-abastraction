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

package org.dcm4chex.archive.web.maverick.mwl;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.mpps.model.MPPSModel;
import org.dcm4chex.archive.web.maverick.mwl.model.MWLEntry;
import org.dcm4chex.archive.web.maverick.mwl.model.MWLFilter;
import org.dcm4chex.archive.web.maverick.mwl.model.MWLModel;

/**
 * @author franz.willer
 *
 * The Maverick controller for Media Creation Manager.
 */
public class MWLConsoleCtrl extends Dcm4cheeFormController {


	/** the view model. */
	private MWLModel model;
	
	private static MWLScuDelegate delegate = null;

	/**
	 * Get the model for the view.
	 */
    protected Object makeFormBean() {
        if ( delegate == null ) {
        	delegate = new MWLScuDelegate();
        	delegate.init( getCtx().getServletConfig() );
        }
        model =  MWLModel.getModel(getCtx().getRequest());
        model.getFilter().setStationAetGroups(getStationAEFilterGroups(), getStationAEFilter()!=null);
        return model;
    }
	

	
    protected String perform() throws Exception {
        try {
            HttpServletRequest request = getCtx().getRequest();
    		model = MWLModel.getModel(request);
    		model.clearPopupMsg();
    		model.setPatMergeAttributes(null);
            if ( request.getParameter("filter.x") != null ) {//action from filter button
            	try {
	        		checkFilter( request );
	            	model.filterWorkList( true );
            	} catch ( Exception x ) {
            		model.setPopupMsg("folder.err_datetime", "yyyy/MM/dd" );
            	}
            } else if ( request.getParameter("nav") != null ) {//action from a nav button. (next or previous)
            	String nav = request.getParameter("nav");
            	if ( nav.equals("prev") ) {
            		model.performPrevious();
            	} else if ( nav.equals("next") ) {
            		model.performNext();
            	}
            } else if ( request.getParameter("link.x") != null ) {//action from a global link button.
            	return performAction( "link", request );
            } else if ( request.getParameter("doLink.x") != null ) {
            	return performAction( "doLink", request );
            } else if ( request.getParameter("del.x") != null ) {//action from delete button.
        		String[] spsIDs = getSPSIds(request);
        		if ( spsIDs == null || spsIDs.length < 1) {
        			model.setPopupMsg("mwl.err_delete", "");
        		} else {
        			for ( int i = 0 ; i < spsIDs.length ; i++ ) {
        				delegate.deleteMWLEntry( spsIDs[i] );
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
			if ( delegate.deleteMWLEntry( request.getParameter("spsid") ) ) {
				model.filterWorkList( false );
			} else {
				model.setPopupMsg( "mwl.err_delete_failed", request.getParameter("spsid") );
			}
		} else if ("link".equals(action)) {
			MWLFilter filter = model.getFilter();
            MPPSModel mppsModel = MPPSModel.getModel(request);
            String mppsIUID = request.getParameter("mppsIUID");
			if ( mppsIUID != null ) {
				model.setMppsIDs( request.getParameterValues("mppsIUID")); // direct url call
			} else {
				model.setMppsIDs( mppsModel.getMppsIUIDs() );//redirect from mpps controller
			}
            prepareLinkPreset(filter, mppsModel, mppsIUID);
			filter.setAccessionNumber(null);
			filter.setStationAET(request.getParameter("stationAET"));
			model.filterWorkList( true );
		} else if ("doLink".equals(action)) {
			return doLink(request);
		} else if ("cancelLink".equals(action)) {
			model.setMppsIDs(null);
			return "cancelLink";
        } else if ("inspect".equals(action)) {
            return inspect(request.getParameter("spsID"));
		}
		return SUCCESS;
	}
    
    private void prepareLinkPreset(MWLFilter filter, MPPSModel mppsModel, String mppsIUID) throws ParseException {
        String patRule = getCtx().getServletConfig().getInitParameter("linkPresetPatientName");
        if ( patRule != null ) {
            String pat = null;
            if ( ! "delete".equalsIgnoreCase(patRule) ) {
                pat = mppsModel.getPatientOfSelectedMpps(mppsIUID);
                if ( pat != null && !"*".equals(patRule) ) {
                    int len = Integer.parseInt(patRule);
                    if ( len >= 0 && len < pat.length()) {
                        pat = pat.substring(0,len);
                    }
                }
            }
            filter.setPatientName(pat);
        }
        String dateRule = getCtx().getServletConfig().getInitParameter("linkPresetStartDate");
        if ( dateRule != null ) {
            String date = null;
            if ( "today".equalsIgnoreCase(dateRule) ) {
                date = MWLFilter.getDateString(new Date());
            } else if ( !"delete".equalsIgnoreCase(dateRule)) {
                 date = mppsModel.getStartDateOfSelectedMpps(mppsIUID);
                 if ( date == null ) {
                     date = MWLFilter.getDateString(new Date());//set today
                 }
            }
            filter.setStartDate(date);
        }
        String modalityRule = getCtx().getServletConfig().getInitParameter("linkPresetModality");
        if ( modalityRule != null ) {
            String modality = null;
            if ( ! "delete".equalsIgnoreCase(modalityRule) ) {
                modality = mppsModel.getModalityOfSelectedMpps(mppsIUID);
                filter.setModality(modality);
            }
        }
    }



	/**
	 * @param request
	 * @return
	 */
	private String doLink(HttpServletRequest request) {
		String[] mppsIUIDs = model.getMppsIDs();
		model.setMppsIDs(null);
		String[] spsIDs = getSPSIds(request);
		if ( spsIDs == null || spsIDs.length < 1) {
			model.setPopupMsg("mwl.err_link_selection", "");
			return SUCCESS;
		} else if ( spsIDs.length > 1 ) {
			String patID = (String) model.getMWLEntry(spsIDs[0]).getPatientID();
			for ( int i = 1 ; i < spsIDs.length ; i++ ) {
				if ( ! patID.equals(model.getMWLEntry(spsIDs[i]).getPatientID())) {
					model.setPopupMsg("mwl.err_link_pat", 
                            new String[]{patID,model.getMWLEntry(spsIDs[i]).getPatientID()});
					return SUCCESS;
				}
			}
		}
		if ( mppsIUIDs != null ) {
			Map map;
            if ( model.isLocal() ) {
                map= delegate.linkMppsToMwl( spsIDs, mppsIUIDs );
            } else {
                map= delegate.linkMppsToMwl( model.getMWLAttributes(spsIDs), mppsIUIDs );
            }
			if ( map == null ) {
				MPPSModel.getModel(request).setExternalPopupMsg("mwl.err_link_failed", null);
			} else if ( map.get("dominant") != null ) {
				model.setPatMergeAttributes(map);
				return "mergePatient";
			}
		}
		return "linkDone";
	}

    private String inspect(String spsID) {
	    if ( spsID != null ) {
	        MWLEntry entry = model.getMWLEntry(spsID);
            if ( entry != null ) {
                this.getCtx().getRequest().getSession().setAttribute("dataset2view", entry.toDataset());
                return INSPECT;
            } else {
                model.setPopupMsg("mwl.err_inspect",spsID);
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
		if ( request.getParameter("spsID") != null ) {
			result = new String[]{request.getParameter("spsID")};
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
		MWLFilter filter = model.getFilter();
		if ( rq.getParameter("patientName") != null ) filter.setPatientName(rq.getParameter("patientName") );
		if ( rq.getParameter("startDate") != null ) filter.setStartDate(rq.getParameter("startDate") );
		if ( rq.getParameter("modality") != null ) filter.setModality(rq.getParameter("modality") );
		filter.setStationAET(rq.getParameter("stationAET") );//we need always set StationAET to update also with StationAET groups
		if ( rq.getParameter("accessionNumber") != null ) filter.setAccessionNumber(rq.getParameter("accessionNumber") );
	}

	/**
	 * Returns the delegater that is used to query the MWLSCP or delete an MWL Entry (only if MWLSCP AET is local)
	 * 
	 * @return The delegator.
	 */
	public static MWLScuDelegate getMwlScuDelegate() {
		return delegate;
	}
	
	protected String getCtrlName() {
		return "mwl_console";
	}
	
}
