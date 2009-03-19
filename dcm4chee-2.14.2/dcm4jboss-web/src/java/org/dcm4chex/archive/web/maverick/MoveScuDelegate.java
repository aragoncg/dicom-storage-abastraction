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

package org.dcm4chex.archive.web.maverick;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.infohazard.maverick.flow.ControllerContext;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * @version $Id: MoveScuDelegate.java 2836 2006-10-09 14:31:41Z gunterze $
 * @since Oct 9, 2006
 */
class MoveScuDelegate {

    private static MBeanServer server = MBeanServerLocator.locate();
    private final ObjectName moveScuServiceName;
    
    public MoveScuDelegate(ControllerContext ctx) throws Exception {
        this.moveScuServiceName = new ObjectName(
                ctx.getServletConfig().getInitParameter("moveScuServiceName"));
    }

    public void scheduleMove(String destAET, int move_prior,
            String[] studyIuids, String[] seriesIuids, String[] sopIuids)
    throws Exception {
        server.invoke(moveScuServiceName,
                "scheduleMove",
                new Object[] { null, destAET, new Integer(move_prior), null,
                studyIuids, seriesIuids, sopIuids, new Long(0L)},
                new String[] { String.class.getName(), String.class.getName(),
                    int.class.getName(), String.class.getName(),
                    String[].class.getName(), String[].class.getName(),
                    String[].class.getName(), long.class.getName() });
        
    }   
    
}
