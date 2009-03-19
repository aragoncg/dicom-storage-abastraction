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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
class ConceptNameCodeConfig {

    private static final String ALL = "A";
    private static final String RADIOLOGY = "R";
    private static final String CARDIOLOGY = "C";
    private static final String CARDIOLOGY_ECG = "ECG";
    private static final String LABORATORY = "L";
    private static final String SURGERY = "S";
    private static final String EMERGENCY = "E";
    private static final String DISCHARGE = "D";
    private static final String ICU = "I";//Intensive Care Report
    private static final String RX = "P";//Prescription Report

    private static final DcmObjectFactory factory = DcmObjectFactory.getInstance();

    private Map<String, Map<String, List<Dataset>>> conceptNameCodeLists = new HashMap<String, Map<String, List<Dataset>>>();

    private static final Map<String, String> SUMMARY_ID_MAP = new HashMap<String, String>();
    static {
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY, ALL);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_RADIOLOGY, RADIOLOGY);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_CARDIOLOGY, CARDIOLOGY);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_CARDIOLOGY_ECG, CARDIOLOGY_ECG);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_LABORATORY, LABORATORY);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_SURGERY, SURGERY);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_EMERGENCY, EMERGENCY);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_DISCHARGE, DISCHARGE);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_ICU, ICU);
        SUMMARY_ID_MAP.put(RIDSupport.SUMMARY_RX, RX);
    }
    
    public static String getSummaryID(String reqType) {
        return SUMMARY_ID_MAP.get(reqType);
    }
    /**
     * @return Returns the concept name codes for given summary request type.
     * 
     * @return Map with CUID group ID and List of Code Dataset fragments.
     */
    public Map<String, List<Dataset>> getConceptNameCodes(String key) {
        return conceptNameCodeLists.get(SUMMARY_ID_MAP.get(key));
    }

    public void setConceptNameCodes(String conceptNames) {
        if ( conceptNames == null || conceptNames.trim().length() < 1 ) {
            resetToDefaultCodes();
        } else {
            conceptNameCodeLists = parseConceptNameCodeString( conceptNames );
        }
    }

    public String getConceptNameCodes() {
        String assign, grpID, code;
        Map<String, List<Dataset>> map;
        TreeMap<String, Map<String, List<String>>> resultMap =new TreeMap<String, Map<String, List<String>>>();
        List<String> cuidList;
        Map<String,List<String>> cuidMap;
        for ( Map.Entry entry : conceptNameCodeLists.entrySet() ) {
            assign = (String) entry.getKey();
            map = (Map<String, List<Dataset>>) entry.getValue();
            for ( Map.Entry entryCuidCode : map.entrySet() ) {
                grpID = (String)entryCuidCode.getKey();
                for ( Dataset ds : (List<Dataset>) entryCuidCode.getValue() ) {
                    code = toCodeString(ds);
                    cuidMap = resultMap.get(code);
                    if ( cuidMap == null ) {
                        cuidMap = new TreeMap<String, List<String>>();
                        resultMap.put( code, cuidMap);
                    }
                    cuidList = cuidMap.get(assign);
                    if ( cuidList == null ) {
                        cuidList = new ArrayList<String>();
                        cuidMap.put(assign, cuidList);
                    }
                    if ( grpID != null )
                        cuidList.add(grpID);
                }
            }
        }
        StringBuffer sb = new StringBuffer();
        for ( Map.Entry entry : resultMap.entrySet() ) {
            code = (String)entry.getKey();
            sb.append(code).append(':');
            for ( Map.Entry entryAssign : ((Map<String, List<String>>) entry.getValue()).entrySet() ) {
                sb.append(entryAssign.getKey());
                cuidList = (List<String>)entryAssign.getValue();
                if ( cuidList.size() > 0 ) {
                    sb.append('(');
                    for ( String id : cuidList ) {
                        sb.append(id).append(',');
                    }
                    sb.setLength(sb.length()-1);
                    sb.append(')');
                }
                sb.append(',');
            }
            sb.setLength(sb.length()-1); 
            sb.append(System.getProperty("line.separator", "\n"));
        }
        return sb.toString();
    }
    private void resetToDefaultCodes() {
        conceptNameCodeLists.clear();
        HashMap<String, List<Dataset>> mapAll = new HashMap<String, List<Dataset>>();
        HashMap<String, List<Dataset>> mapR = new HashMap<String, List<Dataset>>();
        mapR.put(null, ConceptNameCodeConfig.getDefaultRadiologyConceptNameCodes());
        mapAll.put(null, ConceptNameCodeConfig.getDefaultRadiologyConceptNameCodes());
        conceptNameCodeLists.put(ALL, mapAll);
        conceptNameCodeLists.put(RADIOLOGY, mapR);
        HashMap<String, List<Dataset>> mapC = new HashMap<String, List<Dataset>>();
        mapC.put(null, ConceptNameCodeConfig.getDefaultCardiologyConceptNameCodes());
        mapAll.get(null).addAll(ConceptNameCodeConfig.getDefaultCardiologyConceptNameCodes());
        conceptNameCodeLists.put(CARDIOLOGY, mapC);
    }

    /**
     * 
     * Map<summary_id, Map<cuid_grp_id, List<code_dataset>> 
     * @param conceptNames Configuration String.
     * @return
     */
    public Map<String, Map<String, List<Dataset>>> parseConceptNameCodeString( String conceptNames ) {
        Map<String, Map<String, List<Dataset>>> map = new HashMap<String, Map<String, List<Dataset>>>();
        if ( conceptNames != null && conceptNames.trim().length() > 0 ) {
            StringTokenizer stCode, stAssign;
            String line, assign;
            Dataset code;
            int pos, pos1;
            for ( StringTokenizer stLine = new StringTokenizer(conceptNames, "\r\n;") ; stLine.hasMoreTokens() ; ) {
                line = stLine.nextToken();
                pos = line.indexOf(':');
                stCode = new StringTokenizer( line.substring(0,pos), "^");
                code = createCodeDS( stCode.nextToken(), 
                        stCode.hasMoreTokens() ? stCode.nextToken():"LN",
                                stCode.hasMoreTokens() ? stCode.nextToken():null );

                for ( stAssign = new StringTokenizer( line.substring(++pos), ",") ; stAssign.hasMoreTokens() ; ) {
                    assign = stAssign.nextToken();
                    if ( (pos1 = assign.indexOf('(')) != -1) {
                        String cuidGrps = assign.substring(++pos1,assign.length()-1);
                        assign = assign.substring(0, --pos1);
                        for ( StringTokenizer stCuidGrp = new StringTokenizer( cuidGrps , ",") ; 
                        stCuidGrp.hasMoreTokens() ; ) {
                            addAssign(assign, stCuidGrp.nextToken(), code, map);
                        }
                    } else {
                        addAssign(assign, null, code, map);
                    }
                }
            }
            return map;
        }
        return null;

    }

    private void addAssign(String assign, String cuidGrpId, Dataset code,
            Map<String, Map<String, List<Dataset>>> map) {
        Map<String, List<Dataset>> map1 = map.get(assign);
        if ( map1==null) {
            map1 = new HashMap<String, List<Dataset>>();
            map.put(assign, map1);
        }
        List<Dataset> l = map1.get(cuidGrpId);
        if ( l == null ) {
            l = new ArrayList<Dataset>();
            map1.put(cuidGrpId, l);
        }
        l.add(code);
    }


    public String toCodeString(Dataset ds) {
        StringBuffer sb = new StringBuffer();
        sb.append(ds.getString(Tags.CodeValue)).append('^');
        sb.append(ds.getString(Tags.CodingSchemeDesignator));
        if (ds.getString(Tags.CodeMeaning) != null )
            sb.append('^').append(ds.getString(Tags.CodeMeaning));
        return sb.toString();
    }

    public static List<Dataset> getDefaultCardiologyConceptNameCodes() {
        List<Dataset> cardiologyConceptNameCodes = new ArrayList<Dataset>();
        cardiologyConceptNameCodes.add( createCodeDS( "18745-0", "LN", "Cardiac Catheteization Report" ) );
        cardiologyConceptNameCodes.add( createCodeDS( "11522-0", "LN", "Echocardiography Report" ) );
        cardiologyConceptNameCodes.add( createCodeDS( "10001", "99SUPP97", "Quantitative Arteriography report" ) );
        cardiologyConceptNameCodes.add( createCodeDS( "122291", "DCM", "CT/MR Cardiovascular Report" ) );
        cardiologyConceptNameCodes.add( createCodeDS( "122292", "DCM", "Quantitative Ventriculography Report" ) );
        cardiologyConceptNameCodes.add( createCodeDS( "125200", "DCM", "Adult Echocardiography Procedure Report" ) );
        return cardiologyConceptNameCodes;
    }
    public static List<Dataset> getDefaultRadiologyConceptNameCodes() {
        List<Dataset> radiologyConceptNameCodes = new ArrayList<Dataset>();
        radiologyConceptNameCodes.add( createCodeDS( "11540-2", "LN", "CT Abdomen Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "11538-6", "LN", "CT Chest Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "11539-4", "LN", "CT Head Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18747-6", "LN", "CT Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18748-4", "LN", "Diagnostic Imaging Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18760-9", "LN", "Ultrasound Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "11541-0", "LN", "MRI Head Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18755-9", "LN", "MRI Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18756-7", "LN", "MRI Spine Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18757-5", "LN", "Nuclear Medicine Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "11525-3", "LN", "Ultrasound Obstetric and Gyn Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "18758-3", "LN", "PET Scan Report" ) );
        radiologyConceptNameCodes.add( createCodeDS( "11528-7", "LN", "Radiology Report" ) );
        return radiologyConceptNameCodes;
    }

    private static Dataset createCodeDS( String value, String design, String meaning ) {
        Dataset ds = factory.newDataset();
        ds.putSH(Tags.CodeValue, value);
        ds.putSH(Tags.CodingSchemeDesignator, design);
        if (meaning != null) {
            ds.putLO(Tags.CodeMeaning, meaning);
        }
        return ds;
    }

}
