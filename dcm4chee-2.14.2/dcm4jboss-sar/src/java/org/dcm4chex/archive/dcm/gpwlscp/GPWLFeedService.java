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

package org.dcm4chex.archive.dcm.gpwlscp;

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ejb.CreateException;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.common.DatasetUtils;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.ejb.interfaces.GPWLManager;
import org.dcm4chex.archive.ejb.interfaces.GPWLManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;
import org.xml.sax.InputSource;

/**
 * @author gunter.zeilinger@tiani.com
 * @version $Revision: 3345 $ $Date: 2007-05-18 10:53:26 +0200 (Fri, 18 May 2007) $
 * @since 05.04.2005
 * 
 */

public class GPWLFeedService extends ServiceMBeanSupport {

    private static final int[] PAT_ATTR_TAGS = { Tags.PatientName,
            Tags.PatientID, Tags.PatientBirthDate, Tags.PatientSex, };

    private Map humanPerformer = null;

    private List templates = null;

    private File templatePath = null;

    private static DcmObjectFactory dof = DcmObjectFactory.getInstance();

    /**
     * @return Returns the physicians.
     */
    public String getHumanPerformer() {
        return codes2String(humanPerformer);
    }

    /**
     * @param performer
     *            The human performer(s) to set.
     */
    public void setHumanPerformer(String performer) {
        this.humanPerformer = string2Codes(performer, "DCM4CHEE");
    }

    /**
     * @return Returns the configURL.
     */
    public String getTemplatePath() {
        return templatePath.getPath();
    }

    /**
     * @param configURL
     *            The configURL to set.
     * @throws MalformedURLException
     */
    public void setTemplatePath(String path) throws MalformedURLException {
        templatePath = new File(path.replace('/', File.separatorChar));
    }

    private String codes2String(Map codes) {
        if (codes == null || codes.isEmpty())
            return "";
        StringBuffer sb = new StringBuffer();
        Dataset ds;
        String design;
        for (Iterator iter = codes.values().iterator(); iter.hasNext();) {
            ds = (Dataset) iter.next();
            design = ds.getString(Tags.CodingSchemeDesignator);
            sb.append(ds.getString(Tags.CodeValue)).append("^");
            if (design != null)
                sb.append(design).append("^");
            sb.append(ds.getString(Tags.CodeMeaning)).append(",");
        }

        return sb.substring(0, sb.length() - 1);
    }

    private Map string2Codes(String codes, String defaultDesign) {
        StringTokenizer st = new StringTokenizer(codes, ",");
        Map map = new HashMap();
        int nrOfTokens;
        StringTokenizer stCode;
        Dataset ds;
        String codeValue;
        while (st.hasMoreTokens()) {
            stCode = new StringTokenizer(st.nextToken(), "^");
            nrOfTokens = stCode.countTokens();
            if (nrOfTokens < 2) {
                throw new IllegalArgumentException(
                        "Wrong format of human performer configuration! (<codeValue>[^<designator>]^<meaning>)");
            }
            ds = dof.newDataset();
            codeValue = stCode.nextToken();
            ds.putSH(Tags.CodeValue, codeValue);
            if (nrOfTokens > 2) {
                ds.putSH(Tags.CodingSchemeDesignator, stCode.nextToken());
            } else if (defaultDesign != null) {
                ds.putSH(Tags.CodingSchemeDesignator, defaultDesign);
            }
            ds.putLO(Tags.CodeMeaning, stCode.nextToken());
            map.put(codeValue, ds);
        }
        return map;
    }

    public List listTemplates() {
        if (templates == null) {
            File tmplPath = FileUtils.resolve(templatePath);
            File[] files = tmplPath.listFiles();
            templates = new ArrayList();
            String fn;
            for (int i = 0; i < files.length; i++) {
                fn = files[i].getName();
                if (fn.endsWith(".xml")) {
                    templates.add(fn.substring(0, fn.length() - 4));
                }
            }
        }
        log.info("Template List:" + templates);
        return templates;
    }

    public void clearTemplateList() {
        templates = null;
    }

    public void addWorklistItem(Long studyPk, String templateFile,
            String humanPerformerCode, Long scheduleDate) throws Exception {
        String uri = FileUtils.resolve(
                new File(templatePath, templateFile + ".xml")).toURI()
                .toString();
        if (log.isDebugEnabled())
            log.debug("load template file: " + uri);
        Dataset ds = DatasetUtils.fromXML(new InputSource(uri));

        ContentManager cm = getContentManager();
        // patient
        Dataset patDS = cm.getPatientForStudy(studyPk.longValue());
        if (log.isDebugEnabled()) {
            log.debug("Patient Dataset:");
            log.debug(patDS);
        }

        ds.putAll(patDS.subSet(PAT_ATTR_TAGS));
        //
        Dataset sopInstRef = cm.getSOPInstanceRefMacro(studyPk.longValue(),
                false);
        String studyIUID = sopInstRef.getString(Tags.StudyInstanceUID);
        ds.putUI(Tags.SOPInstanceUID, UIDGenerator.getInstance().createUID());
        ds.putUI(Tags.StudyInstanceUID, studyIUID);
        DcmElement inSq = ds.putSQ(Tags.InputInformationSeq);
        inSq.addItem(sopInstRef);

        // Scheduled Human Performer Seq
        DcmElement schedHPSq = ds.putSQ(Tags.ScheduledHumanPerformersSeq);
        Dataset item = schedHPSq.addNewItem();
        DcmElement hpCodeSq = item.putSQ(Tags.HumanPerformerCodeSeq);
        Dataset dsCode = (Dataset) this.humanPerformer.get(humanPerformerCode);
        log.info(dsCode);
        if (dsCode != null) {
            hpCodeSq.addItem(dsCode);
            item.putPN(Tags.HumanPerformerName, dsCode
                    .getString(Tags.CodeMeaning));
        }

        // Scheduled Procedure Step Start Date and Time
        ds.putDT(Tags.SPSStartDateAndTime, new Date(scheduleDate.longValue()));

        if (log.isDebugEnabled()) {
            log.debug("GPSPS Dataset:");
            log.debug(ds);
        }

        addWorklistItem(ds);
    }

    private void addWorklistItem(Dataset ds) {
        if (ds == null)
            return;
        try {
            getGPWLManager().addWorklistItem(ds);
        } catch (Exception e) {
            log.error("Failed to add Worklist Item:", e);
        }
    }

    private GPWLManager getGPWLManager() throws CreateException,
            RemoteException, HomeFactoryException {
        return ((GPWLManagerHome) EJBHomeFactory.getFactory().lookup(
                GPWLManagerHome.class, GPWLManagerHome.JNDI_NAME)).create();
    }


    private ContentManager getContentManager() throws Exception {
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory
                .getFactory().lookup(ContentManagerHome.class,
                        ContentManagerHome.JNDI_NAME);
        return home.create();
    }

}
