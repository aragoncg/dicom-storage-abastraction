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

package org.dcm4chex.archive.web.maverick.tf;

import java.util.Collection;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.dcm4chex.archive.web.maverick.BasicFormModel;

/**
 * @author franz.willer
 *
 * The Model for Teaching File Selector WEB interface.
 */
public class TFModel extends BasicFormModel {

    public static final String[] DOC_TITLES = new String[] {
        "TCE001", "IHERADTF", "For Teaching File Export",
        "TCE002", "IHERADTF", "For Clinical Trial Export",
        "TCE007", "IHERADTF", "For Research Collection Export",
        "113019", "99DCM4CHE", "For Media Export"};
    
    public static final String[] DELAY_REASONS = new String[] {
        "TCE011" ,"IHERADTF", "Delay export until final report is available",
        "TCE012", "IHERADTF", "Delay export until clinical information is available",
        "TCE013", "IHERADTF", "Delay export until confirmation of diagnosis is available",
        "TCE014", "IHERADTF", "Delay export until histopathology is available",
        "TCE015", "IHERADTF", "Delay export until other laboratory results is available",
        "TCE016", "IHERADTF", "Delay export until patient is discharged",
        "TCE017", "IHERADTF", "Delay export until patient dies",
        "TCE018", "IHERADTF", "Delay export until expert review is available"};
    
    private static final int CODE = 0;
    private static final int DESIGNATOR = 1;
	private static final int MEANING = 2;

    /** The session attribute name to store the model in http session. */
	public static final String TF_ATTR_NAME = "tfModel";
	
    private SRManifestModel manifestModel;

	private String user;
	
	private Set instances;
	
	private int selectedTitle;
	private int selectedDelayReason = -1;
	private Collection dispositions;
	private String disposition;

	/**
	 * Creates the model.
	 * <p>
	 */
	private TFModel(String user, HttpServletRequest request) {
		super(request);
		this.user = user;
		manifestModel = new SRManifestModel();
	}
	
	
	/**
	 * @return Returns the user.
	 */
	public String getUser() {
		return user;
	}
	/**
	 * @return Returns the manifestModel.
	 */
	public SRManifestModel getManifestModel() {
		return manifestModel;
	}
	/**
	 * @param disposition The disposition to set.
	 */
	public void setDisposition(String disposition) {
		if ( disposition != null ) {
			this.disposition = disposition;
		}
	}
	/**
	 * @param selectedDelayReason The selectedDelayReason to set.
	 */
	public void setSelectedDelayReason(int selectedDelayReason) {
		this.selectedDelayReason = selectedDelayReason;
	}
	/**
	 * @param selectedTitle The selectedTitle to set.
	 */
	public void setSelectedTitle(int selectedTitle) {
		this.selectedTitle = selectedTitle;
	}
	
	/**
	 * Get the model for an http request.
	 * <p>
	 * Look in the session for an associated model via <code>TF_ATTR_NAME</code><br>
	 * If there is no model stored in session (first request) a new model is created and stored in session.
	 * 
	 * @param request A http request.
	 * 
	 * @return The model for given request.
	 */
	public static final TFModel getModel( HttpServletRequest request ) {
		TFModel model = (TFModel) request.getSession().getAttribute(TF_ATTR_NAME);
		if (model == null) {
				model = new TFModel(request.getUserPrincipal().getName(), request);
				request.getSession().setAttribute(TF_ATTR_NAME, model);
		}
		return model;
	}

	public String getModelName() { return "TF"; }

    private String selectFrom(int off, int index, String[] values) {
        return index < 0 ? null : values[off + index * 3];
    }

    private String[] meaning(String[] values) {
        String[] ss = new String[values.length / 3];
        for (int i = 0; i < ss.length; i++) {
            ss[i] = selectFrom(MEANING, i, values);           
        }
        return ss;
    }

    
	public String[] getDocTitles() {
		return meaning(DOC_TITLES);
	}
	
	public String[] getDelayReasons() {
		return meaning(DELAY_REASONS);
	}

	/**
	 * @return Returns the instances.
	 */
	public Set getInstances() {
		return instances;
	}
	/**
	 * @param instances The instances to set.
	 */
	public void setInstances(Set instances) {
		this.instances = instances;
	}
	
	public int getNumberOfInstances() {
		return instances == null ? 0 : instances.size();
	}

	public int getSelectedDocTitle() {
		return selectedTitle;
	}

    public String selectedDocTitle() {
        return selectFrom(MEANING, selectedTitle, DOC_TITLES);
	}    

    public String selectedDocTitleCode() {
        return selectFrom(CODE, selectedTitle, DOC_TITLES);
	}
	
	public String selectedDocTitleDesignator() {
        return selectFrom(DESIGNATOR, selectedTitle, DOC_TITLES);
	}

	public int getSelectedDelayReason() {
		return selectedDelayReason;
	}

    public String selectedDelayReason() {
        return selectFrom(MEANING, selectedDelayReason, DELAY_REASONS);
	}
    
	public String selectedDelayReasonCode() {
        return selectFrom(CODE, selectedDelayReason, DELAY_REASONS);
	}
	
	public String selectedDelayReasonDesignator() {
        return selectFrom(DESIGNATOR, selectedDelayReason, DELAY_REASONS);
	}
	
	public String getDisposition() {
		return this.disposition;
	}
	/**
	 * @param configuredDispositions
	 */
	public void setDispositions(Collection dispositions) {
		this.dispositions = dispositions;
	}
	/**
	 * @return Returns the list of configured dispositions.
	 */
	public Collection getDispositions() {
		return dispositions;
	}


	/**
	 * 
	 */
	public void clear() {
		selectedTitle = 0;
		selectedDelayReason = -1;
		disposition = null;
		manifestModel.clear();
	}
	
}
