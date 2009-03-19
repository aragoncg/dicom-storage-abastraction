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

package org.dcm4chex.rid.mbean;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.management.ObjectName;
import javax.xml.transform.TransformerConfigurationException;

import org.dcm4che.dict.UIDs;
import org.dcm4chex.rid.common.RIDRequestObject;
import org.dcm4chex.rid.common.RIDResponseObject;
import org.dcm4chex.rid.mbean.factory.RIDSupportFactoryUtil;
import org.jboss.system.ServiceMBeanSupport;
import org.xml.sax.SAXException;

/**
 * @author franz.willer
 *
 * The MBean to manage the IHE RID (Retrieve Information for Display service.
 * <p>
 * This class use RIDSupport for the Retrieve Information for Display methods.
 */
public class RIDService extends ServiceMBeanSupport  {

    private static final String NONE = "NONE";
    
	//Modified by YangLin@cn-arg.com on 03.04.2009
//	private RIDSupport support = new RIDSupport( this );
	private RIDSupport support = RIDSupportFactoryUtil.getRIDSupportFactory().
	                             getRIDSupport(this);
    
    private float waveformCorrection = 1f;

    public RIDService() {
    }

    public float getWaveformCorrection() {
        return waveformCorrection;
    }
    public void setWaveformCorrection(float waveformCorrection) {
        this.waveformCorrection = waveformCorrection;
    }

    public String getWadoURL() {
        return support.getWadoURL();
    }
    public void setWadoURL( String wadoURL ) {
        support.setWadoURL( wadoURL );
    }

    public String getRIDSummaryXsl() {
        String s = support.getRIDSummaryXsl();
        if ( s == null ) s = "";
        return s;
    }

    public void setRIDSummaryXsl( String xslFile ) {
        if ( xslFile != null && xslFile.trim().length() < 1 ) xslFile = null;
        support.setRIDSummaryXsl( xslFile );
    }

    /**
     * @return Returns the useXSLInstruction.
     */
    public boolean isUseXSLInstruction() {
        return support.isUseXSLInstruction();
    }
    /**
     * @param useXSLInstruction The useXSLInstruction to set.
     */
    public void setUseXSLInstruction(boolean useXSLInstruction) {
        support.setUseXSLInstruction( useXSLInstruction );
    }

    /**
     * @return Returns the useOrigFile.
     */
    public boolean isUseOrigFile() {
        return support.isUseOrigFile();
    }
    /**
     * @param useOrigFile The useOrigFile to set.
     */
    public void setUseOrigFile(boolean useOrigFile) {
        support.setUseOrigFile(useOrigFile);
    }

    public String getSrImageRows() {
        String rows = support.getSrImageRows();
        return rows == null ? NONE : support.getSrImageRows();
    }

    public void setSrImageRows(String srImageRows) {
        support.setSrImageRows( NONE.equals(srImageRows) ? null : srImageRows );
    }

    /**
     * Returns a String with all defined SOP Class UIDs that are used to find ECG documents.
     * <p>
     * The uids are separated with semicolon or newline.
     * 
     * @return SOP Class UIDs to find ECG related files.
     */
    public String getECGSopCuids() {
        return toString(support.getECGSopCuids());
    }

    /**
     * Set a list of SOP Class UIDs that are used to find ECG documents.
     * <p>
     * The UIDs are separated with semicolon or newline.
     * 
     * @param sopCuids String with SOP class UIDs
     */
    public void setECGSopCuids( String cuids ) {
        support.setECGSopCuids( toUidMap(cuids) );
    }

    /**
     * Returns a String with all defined SOP Class UIDs that are used to find SR documents.
     * <p>
     * The uids are separated with semicolon or newline.
     * 
     * @return SOP Class UIDs to find Structured Reports.
     */
    public String getSRSopCuids() {
        return toString(support.getSRSopCuids());
    }

    /**
     * Set a list of SOP Class UIDs that are used to find SR documents.
     * <p>
     * The UIDs are separated with semicolon or newline.
     * 
     * @param sopCuids String with SOP class UIDs
     */
    public void setSRSopCuids( String cuids ) {
        support.setSRSopCuids( toUidMap(cuids) );
    }

    /**
     * Returns a String with all defined SOP Class UIDs that are used to find SR documents.
     * <p>
     * The uids are separated with semicolon or newline.
     * 
     * @return SOP Class UIDs to find Structured Reports.
     */
    public String getEncapsulatedDocumentSopCuids() {
        return toString(support.getEncapsulatedDocumentSopCuids());
    }

    /**
     * Set a list of SOP Class UIDs that are used to find encapsulated documents.
     * <p>
     * The UIDs are separated with semicolon or newline.
     * 
     * @param sopCuids String with SOP class UIDs
     */
    public void setEncapsulatedDocumentSopCuids( String cuids ) {
        support.setEncapsulatedDocumentSopCuids( toUidMap(cuids) );
    }
    
    public String getCuidsForSummaryAll() {
        return toString( support.getCuidsForSummaryAll() );
    }
    
    /**
     * Set a list of SOP Class UIDs that are added to document list for requestType=SUMMARY.
     * <p>
     * The UIDs are separated with semicolon or newline.
     * 
     * @param sopCuids String with SOP class UIDs
     */
    public void setCuidsForSummaryAll(String cuids) {
        support.setCuidsForSummaryAll(toUidMap(cuids));
    }
    
    /**
     * Returns a String with all defined SOP Class UIDs Groups.
     * <p/>
     * The uids are separated with semicolon or newline.
     * 
     * @return SOP Class UIDs to find Structured Reports.
     */
    public String getSopCuidGroups() {
        return toUidGrpString(support.getSopCuidGroups());
    }

    /**
     * Set a list of SOP Class UIDs Groups.
     * <p/>
     * Format: <grpId>:<UID>
     * <p/>
     * The UIDs are separated with semicolon or newline.
     * 
     * @param sopCuids String with SOP class UIDs
     */
    public void setSopCuidGroups( String cuidGrps ) {
        support.updateSopCuidGroups( toUidGrpMap(cuidGrps) );
    }

    private Map toUidMap(String cuids) {
        if ( NONE.equalsIgnoreCase(cuids)) return null;
        StringTokenizer st = new StringTokenizer(cuids, "\r\n;");
        String uid,name;
        Map map = new TreeMap();
        int i = 0;
        while ( st.hasMoreTokens() ) {
            uid = st.nextToken().trim();
            name = uid;
            if ( isDigit(uid.charAt(0) ) ) {
                if ( ! UIDs.isValid(uid) ) 
                    throw new IllegalArgumentException("UID "+uid+" isn't a valid UID!");
            } else {
                uid = UIDs.forName( name );
            }
            map.put(name,uid);
        }
        return map;
    }

    private String toString(Map uids) {
        if ( uids == null || uids.isEmpty() ) return NONE;
        StringBuffer sb = new StringBuffer( uids.size() << 5);//StringBuffer initial size: nrOfUIDs x 32
        Iterator iter = uids.keySet().iterator();
        while ( iter.hasNext() ) {
            sb.append(iter.next()).append(System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    private String toUidGrpString(Map<String, Map<String, String>> uids) {
        StringBuffer sb = new StringBuffer();
        String grpId;
        for ( Map.Entry entry : uids.entrySet() ) {
            grpId = (String) entry.getKey();
            for ( String uid : ((Map<String, String>) entry.getValue()).keySet() ) {
                sb.append(grpId).append(':').append(uid).append(System.getProperty("line.separator", "\n"));
            }
        }
        return sb.toString();
    }

    

    private Map<String, Map<String, String>> toUidGrpMap(String cuidGrps) {
        StringTokenizer st = new StringTokenizer(cuidGrps, "\r\n;");
        String s,grp,uid,name;
        Map<String, Map<String, String>> mapGrps = new HashMap<String, Map<String, String>>();
        Map<String,String> map;
        int i = 0, pos;
        while ( st.hasMoreTokens() ) {
            s = st.nextToken().trim();
            pos = s.indexOf(':');
            if ( pos != -1 ) {
                grp = s.substring(0,pos);
                if ( grp.equals(RIDSupport.CUID_GRP_SR) 
                        || grp.equals(RIDSupport.CUID_GRP_ECG) 
                        || grp.equals(RIDSupport.CUID_GRP_DOC))
                    continue;
                uid = s.substring(++pos);
            } else {
                grp = "unknown";
                uid=s;
            }
            name = uid;
            if ( isDigit(uid.charAt(0) ) ) {
                if ( ! UIDs.isValid(uid) ) 
                    throw new IllegalArgumentException("UID "+uid+" isn't a valid UID!");
            } else {
                uid = UIDs.forName( name );
            }
            map = mapGrps.get(grp);
            if ( map == null ) {
                map = new TreeMap<String, String>();
                mapGrps.put(grp, map);
            }
            map.put(name,uid);
        }
        return mapGrps;
    }

    public String getConceptNameCodes() {
        return support.getConceptNameCodes();
    }

    public void setConceptNameCodes( String conceptNames ) {
        support.setConceptNameCodes( conceptNames );
    }

    public String getDocTitlePatterns() {
        Map<String,String> m = support.getDocTitlePatterns();
        if (m == null) return NONE;
        StringBuffer sb = new StringBuffer();
        for (Map.Entry e : m.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }

    public void setDocTitlePatterns(String docTitlePatterns) {
        Map<String,String> m = null;
        if (docTitlePatterns != null && !docTitlePatterns.trim().equalsIgnoreCase(NONE) ) {
            m = new HashMap<String,String>();
            StringTokenizer st = new StringTokenizer(docTitlePatterns, "\r\n;=");
            while ( st.hasMoreElements() ) {
                m.put( st.nextToken(), st.nextToken());
            }
        }
        support.setDocTitlePatterns(m);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    /**
     * Set the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @param name The Audit Logger Name to set.
     */
    public void setAuditLoggerName( ObjectName name ) {
        support.setAuditLoggerName( name );
    }

    public ObjectName getQueryRetrieveScpName() {
        return support.getQueryRetrieveScpName();
    }

    public void setQueryRetrieveScpName(ObjectName name) {
        support.setQueryRetrieveScpName(name);
    }

    /**
     * Get the name of the AuditLogger MBean.
     * <p>
     * This bean is used to create Audit Logs.
     * 
     * @return Returns the name of the Audit Logger MBean.
     */
    public ObjectName getAuditLoggerName() {
        return support.getAuditLoggerName();
    }

    /**
     * Get the requested Summary information object as Stream packed in a RIDResponseObject.
     * <p>
     *  
     * @param reqVO The request parameters packed in an value object.
     * 
     * @return The value object containing the retrieved object or an error.
     * @throws SQLException
     * @throws SAXException
     * @throws IOException
     * @throws TransformerConfigurationException
     */
    public RIDResponseObject getRIDSummary( RIDRequestObject reqVO ) throws SQLException, TransformerConfigurationException, IOException, SAXException {
        if ( log.isDebugEnabled() ) log.debug( "getRIDSummary:"+reqVO );
        return support.getRIDSummary( reqVO );
    }

    /**
     * Get the requested Document object as Stream packed in a RIDResponseObject.
     * <p>
     *  
     * @param reqVO The request parameters packed in an value object.
     * 
     * @return The value object containing the retrieved object or an error.
     */
    public RIDResponseObject getRIDDocument( RIDRequestObject reqVO ) {
        if ( log.isDebugEnabled() ) log.debug("getRIDDocument:"+reqVO );
        return support.getRIDDocument( reqVO );
    }

    public DataHandler getDocumentDataHandler(String objectUID, String contentType) throws IOException {
        return support.getOrCreateDocument(objectUID,contentType).getDataHandler();
    }
}
