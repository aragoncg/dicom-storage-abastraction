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

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3160 $ $Date: 2007-02-28 17:36:25 +0100 (Wed, 28 Feb 2007) $
 * @since 14.10.2004
 *
 */
public class ContentEditDelegate {

    private static Logger log = Logger.getLogger(ContentEditDelegate.class);

    private static MBeanServer server;

    private static ObjectName contentEditName;


    public void init(ControllerContext ctx) throws Exception {
        if (contentEditName != null) return;
        ContentEditDelegate.server = MBeanServerLocator.locate();
        String s = ctx.getServletConfig().getInitParameter("contentEditName");
        ContentEditDelegate.contentEditName = new ObjectName(s);
    }

    public Dataset createPatient(Dataset ds) {
    	Object o = null;
        try {
            o = server.invoke(contentEditName,
                    "createPatient",
                    new Object[] { ds },
                    new String[] { Dataset.class.getName() });
        } catch (Throwable t) {
            while ( t.getCause() != null) {
            	t = t.getCause();
            }
            // char ' in message cause trouble with javascript (popupMsg)!
            String msg = t.getMessage()!=null ? t.getMessage().replace('\'','\"') : t.getClass().getName();
            throw new IllegalArgumentException(msg);
        }
        return (Dataset) o;
    }

    public Dataset createStudy(Dataset ds, long patPk) {
    	Object o = null;
        try {
            o = server.invoke(contentEditName,
                    "createStudy",
                    new Object[] { ds, new Long( patPk ) },
                    new String[] { Dataset.class.getName(), Long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to create Study:", e);
        }
        return (Dataset) o;
    }

    public Dataset createSeries(Dataset ds, long studyPk) {
    	Object o = null;
        try {
            o = server.invoke(contentEditName,
                    "createSeries",
                    new Object[] { ds, new Long( studyPk ) },
                    new String[] { Dataset.class.getName(), Long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to create Series:", e);
        }
        return (Dataset) o;
    }

    public Map mergePatients(long pk, long[] patPks) {
        try {
            return (Map) server.invoke(contentEditName,
                    "mergePatients",
                    new Object[] { new Long(pk), patPks },
                    new String[] { Long.class.getName(), long[].class.getName() });
        } catch (Exception e) {
            log.warn("Failed to merge Patients:", e);
            return null;
        }
    }
    
    public void updatePatient(Dataset ds) {
        try {
            server.invoke(contentEditName,
                    "updatePatient",
                    new Object[] { ds },
                    new String[] { Dataset.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to update Patient:", e);
        }
    }
    
    public void updateStudy(Dataset ds) {
        try {
            server.invoke(contentEditName,
                    "updateStudy",
                    new Object[] { ds },
                    new String[] { Dataset.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to update Study:", e);
        }
    }
   
    public void updateSeries(Dataset ds) {
        try {
            server.invoke(contentEditName,
                    "updateSeries",
                    new Object[] { ds },
                    new String[] { Dataset.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to update Series:", e);
        }
    }
 
    public void movePatientToTrash(long pk) {
    	invokeCmd("movePatientToTrash", pk);
    }
    public void moveStudyToTrash(long pk) {
    	invokeCmd("moveStudyToTrash", pk);
    }
    public void moveSeriesToTrash(long pk) {
    	invokeCmd("moveSeriesToTrash", pk);
    }
    public void moveInstanceToTrash(long pk) {
    	invokeCmd("moveInstanceToTrash", pk);
    }

    public void undeletePatient( long pk ) {
    	invokeCmd("undeletePatient", pk);
    }
    public void undeleteStudy( long pk ) {
    	invokeCmd("undeleteStudy", pk);
    }
    public void undeleteSeries( long pk ) {
    	invokeCmd("undeleteSeries", pk);
    }
    public void undeleteInstance( long pk ) {
    	invokeCmd("undeleteInstance", pk);
    }
    
    public void deletePatient( long pk ) {
    	invokeCmd("deletePatient", pk);
    }
    public void deleteStudy( long pk ) {
    	invokeCmd("deleteStudy", pk);
    }
    public void deleteSeries( long pk ) {
    	invokeCmd("deleteSeries", pk);
    }
    public void deleteInstance( long pk ) {
    	invokeCmd("deleteInstance", pk);
    }
    
    public void emptyTrash() {
        try {
            server.invoke(contentEditName,
                    "emptyTrash", null, null);
        } catch (Exception e) {
            log.warn("Failed to invoke command 'emptyTrash'!", e);
        }
    }

    private void invokeCmd( String cmd, long pk ) {
        try {
            server.invoke(contentEditName,
                    cmd,
                    new Object[] { new Long( pk ) },
                    new String[] { long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to invoke command '"+cmd+"' with (pk="+pk+") !", e);
        }
    }
    
    
    public void moveStudies(long[] study_pks, long patient_pk) {
        try {
            server.invoke(contentEditName,
                    "moveStudies",
                    new Object[] { study_pks, new Long(patient_pk) },
                    new String[] { long[].class.getName(), Long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to move Studies:", e);
        }
     	
    }
 
    public void moveSeries(long[] series_pks, long study_pk) {
        try {
            server.invoke(contentEditName,
                    "moveSeries",
                    new Object[] { series_pks, new Long(study_pk) },
                    new String[] { long[].class.getName(), Long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to move Series:", e);
        }
     	
    }

    public void moveInstances(long[] instance_pks, long series_pk) {
        try {
            server.invoke(contentEditName,
                    "moveInstances",
                    new Object[] { instance_pks, new Long(series_pk) },
                    new String[] { long[].class.getName(), Long.class.getName() });
        } catch (Exception e) {
            log.warn("Failed to move Instances:", e);
        }
     	
    }

}