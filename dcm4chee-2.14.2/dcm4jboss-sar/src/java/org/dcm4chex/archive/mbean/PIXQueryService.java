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

package org.dcm4chex.archive.mbean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.management.ObjectName;

import org.dcm4cheri.util.StringUtils;
import org.dcm4chex.archive.ejb.interfaces.PIXQuery;
import org.dcm4chex.archive.ejb.interfaces.PIXQueryHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.jboss.system.ServiceMBeanSupport;

public class PIXQueryService extends ServiceMBeanSupport {

    private ObjectName hl7SendServiceName;
    private String pixQueryName;
    private String pixManager;
    private List mockResponse;
    private List issuersOfOnlyOtherPatientIDs;
    private List issuersOfOnlyPrimaryPatientIDs;

    public final String getIssuersOfOnlyPrimaryPatientIDs() {
        return toString(issuersOfOnlyPrimaryPatientIDs);
    }

    public final void setIssuersOfOnlyPrimaryPatientIDs(String s) {
        this.issuersOfOnlyPrimaryPatientIDs = toList(s);
    }

    public final String getIssuersOfOnlyOtherPatientIDs() {
        return toString(issuersOfOnlyOtherPatientIDs);
    }

    public final void setIssuersOfOnlyOtherPatientIDs(String s) {
        issuersOfOnlyOtherPatientIDs = toList(s);
    }

    private String toString(List list) {
        if (list == null || list.isEmpty()) {
            return "-";
        }
        Iterator iter = list.iterator();
        StringBuffer sb = new StringBuffer((String) iter.next());
        while (iter.hasNext()) {
            sb.append(',').append((String) iter.next());
        }
        return sb.toString();
    }

    private List toList(String s) {
        if (s.trim().equals("-")) {
            return null;
        }
        String[] a = StringUtils.split(s, ',');
        ArrayList list = new ArrayList(a.length);
        for (int i = 0; i < a.length; i++) {
            list.add(a[i].trim());
        }
        return list;
    }

    public final ObjectName getHL7SendServiceName() {
        return hl7SendServiceName;
    }

    public final void setHL7SendServiceName(ObjectName name) {
        this.hl7SendServiceName = name;
    }

    public final String getPIXManager() {
        return pixManager;
    }

    public final void setPIXManager(String pixManager) {
        this.pixManager = pixManager;
    }
    
    public final boolean isPIXManagerLocal() {
        return "LOCAL".equalsIgnoreCase(pixManager);
    }

    public final String getPIXQueryName() {
        return pixQueryName;
    }

    public final void setPIXQueryName(String pixQueryName) {
        this.pixQueryName = pixQueryName;
    }

    public final String getMockResponse() {
        return mockResponse == null ? "-" : pids2cx(mockResponse);
    }

    private String pids2cx(List pids) {
        StringBuffer sb = new StringBuffer();
        for (Iterator iter = pids.iterator(); iter.hasNext();) {
            if (sb.length() > 0) {
                sb.append('~');
            }
            String[] pid = (String[]) iter.next();
            sb.append(pid[0]).append("^^^").append(pid[1]);
            for (int i = 2; i < pid.length; i++) {
                sb.append('&').append(pid[i]);                
            }
        }
        return sb.toString();
    }

    public final void setMockResponse(String mockResponse) {
        String trim = mockResponse.trim();
        this.mockResponse = "-".equals(trim) ? null : cx2pids(trim);
    }

    private List cx2pids(String s) {
        String[] cx = StringUtils.split(s, '~');
        List l = new ArrayList(cx.length);
        for (int i = 0; i < cx.length; i++) {
            String[] comps = StringUtils.split(s, '^');
            String[] subcomps = StringUtils.split(comps[3], '&');
            String[] pid = new String[1 + subcomps.length];
            pid[0] = comps[0];
            System.arraycopy(subcomps, 0, pid, 1, subcomps.length);
            l.add(pid);
        }
        return l;
    }

    public String showCorrespondingPIDs(String patientID, String issuer) {
        try {
            return pids2cx(queryCorrespondingPIDs(patientID, issuer, null));
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    public List queryCorrespondingPIDs(String patientID, String issuer,
            String[] domains) throws Exception {
        if (mockResponse != null) {
            return mockResponse;
        }
        if (isPIXManagerLocal()) {
             if (issuersOfOnlyPrimaryPatientIDs.contains(issuer)) {
                return pixQuery().queryCorrespondingPIDsByPrimaryPatientID(
                        patientID, issuer, domains);
            } else if (issuersOfOnlyOtherPatientIDs.contains(issuer)) {
                return pixQuery().queryCorrespondingPIDsByOtherPatientID(
                        patientID, issuer, domains);
            } else {
                return pixQuery().queryCorrespondingPIDs(
                        patientID, issuer, domains);
            }           
        }
        return (List) server.invoke(hl7SendServiceName, "sendQBP_Q23",
                new Object[] {
                    pixManager,
                    pixQueryName,
                    patientID,
                    issuer,
                    domains  },
                new String[] {
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String.class.getName(),
                    String[].class.getName(),
        });
    }

    private PIXQuery pixQuery() throws Exception {
        return ((PIXQueryHome) EJBHomeFactory.getFactory().lookup(
                PIXQueryHome.class, PIXQueryHome.JNDI_NAME)).create();
    }
}
