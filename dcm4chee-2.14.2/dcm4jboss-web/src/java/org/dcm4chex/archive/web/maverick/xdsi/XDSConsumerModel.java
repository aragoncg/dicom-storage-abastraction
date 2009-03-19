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

package org.dcm4chex.archive.web.maverick.xdsi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.web.maverick.BasicFormModel;

/**
 * @author franz.willer
 *
 */
public class XDSConsumerModel extends BasicFormModel {

    /** The session attribute name to store the model in http session. */
    public static final String XDS_CONSUMER_ATTR_NAME = "xdsConsumerModel";

    private static Logger log = Logger.getLogger( XDSConsumerModel.class.getName() );

    private Map patDocuments = new HashMap();
    private Map patFolders = new HashMap();

    /**
     * Creates the model.
     * <p>
     */
    private XDSConsumerModel(String user, HttpServletRequest request) {
        super(request);
    }

    /**
     * Get the model for an http request.
     * <p>
     * Look in the session for an associated model via <code>XDS_CONSUMER_ATTR_NAME</code><br>
     * If there is no model stored in session (first request) a new model is created and stored in session.
     * 
     * @param request A http request.
     * 
     * @return The model for given request.
     */
    public static final XDSConsumerModel getModel( HttpServletRequest request ) {
        XDSConsumerModel model = (XDSConsumerModel) request.getSession().getAttribute(XDS_CONSUMER_ATTR_NAME);
        if (model == null) {
            model = new XDSConsumerModel(request.getUserPrincipal().getName(), request);
            request.getSession().setAttribute(XDS_CONSUMER_ATTR_NAME, model);
        }
        return model;
    }

    public String getModelName() { return "XDSConsumer"; }


    public List getDocuments(String patId) {
        return (List) patDocuments.get(patId);
    }
    public void addDocuments(String patId, List docs) {
        patDocuments.put(patId, docs);
    }
    public List getFolders(String patId) {
        return (List) patFolders.get(patId);
    }
    public void addFolders(String patId, List folders) {
        patFolders.put(patId, folders);
    }

    public String toString() {
        return "XDSConsumerModel: documents:"+patDocuments+"\nfolders:"+patFolders;
    }
}
