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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.data.FileFormat;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.FileUtils;
import org.dcm4chex.archive.web.maverick.Dcm4cheeFormController;
import org.dcm4chex.archive.web.maverick.FolderForm;
import org.dcm4chex.archive.web.maverick.model.StudyModel;

/**
 * 
 * @author franz.willer@agfa.com
 * @version $Revision: 7709 $ $Date: 2008-10-22 16:41:47 +0200 (Wed, 22 Oct 2008) $
 * @since 05.04.2007
 */
public class ShowManifestCtrl extends Dcm4cheeFormController {

    private String documentID;
    private String url;
    private String repositoryUID;

    List wadoUrls = new ArrayList();

    private static Properties repositoryHostMapping;
    private static Properties aet2wado;
    private static Properties cuidTypes;

    private static Logger log = Logger.getLogger( ShowManifestCtrl.class.getName() );

    public final void setDocumentID(String id) {
        this.documentID = id;
    }
    public String getDocumentID() {
        return documentID;
    }
    public final void setUrl(String url) {
        this.url = url;
    }

    public void setRepositoryUID(String repositoryUID) {
        this.repositoryUID = repositoryUID;
    }
    protected String perform() throws Exception {
        if ( getCtx().getRequest().getParameter("clear") != null ) {
            repositoryHostMapping = null;
            aet2wado = null;
            cuidTypes = null;
        }
        return url != null ? showManifestPage(loadDataset(new URL(url))) : 
            repositoryUID != null ? showManifestPage(null) : SUCCESS;
    }

    private String showManifestPage(Dataset ds) throws Exception {
        wadoUrls.clear();
        FolderForm model = FolderForm.getFolderForm(getCtx());
        if ( ! UIDs.KeyObjectSelectionDocument.equals(ds.getString(Tags.SOPClassUID))) {
            model.setPopupMsg("xdsi.err", "Not a Manifest (DICOM Key Selection Object)!");
            return ERROR;
        } 
        DcmElement evSq = ds.get(Tags.CurrentRequestedProcedureEvidenceSeq);
        String studyIUID, aet, seriesIUID, iuid, cuid, wadoUrl;
        Dataset dsEvSq, dsRefSer;
        DcmElement refSerSq, refSopSq;
        for ( int i = 0, len = evSq.countItems() ; i < len ; i++ ) {
            dsEvSq = evSq.getItem(i);
            studyIUID = dsEvSq.getString(Tags.StudyInstanceUID);
            refSerSq = dsEvSq.get(Tags.RefSeriesSeq);
            for ( int j = 0, lenj = refSerSq.countItems() ; j < lenj ; j++ ) {
                dsRefSer = refSerSq.getItem(j);
                aet = dsRefSer.getString(Tags.RetrieveAET);
                seriesIUID = dsRefSer.getString(Tags.SeriesInstanceUID);
                refSopSq = dsRefSer.get(Tags.RefSOPSeq);
                for ( int k = 0, lenk = refSopSq.countItems() ; k < lenk ; k++ ) {
                    iuid = refSopSq.getItem(k).getString(Tags.RefSOPInstanceUID);
                    cuid = refSopSq.getItem(k).getString(Tags.RefSOPClassUID);
                    wadoUrl = getWadoUrl(aet, studyIUID, seriesIUID, iuid);
                    wadoUrls.add( new ManifestInstance( iuid, cuid, wadoUrl));
                }

            }
        }
        return SUCCESS;
    }

    public List getWadoUrls() {
        return wadoUrls;
    }


    /**
     * Read a Dataset object from given URL.
     * 
     * @param url
     * @return
     * @throws IOException
     */
    private Dataset loadDataset(URL url) throws IOException {
        url = checkHost(url);
        java.net.HttpURLConnection httpUrlConn = (java.net.HttpURLConnection) url.openConnection();
        InputStream bis = httpUrlConn.getInputStream();
        Dataset ds = DcmObjectFactory.getInstance().newDataset();
        try {
            ds.readFile(bis, FileFormat.DICOM_FILE, -1);
        } finally {
            try {
                bis.close();
            } catch (IOException ignore) {
            }
        }
        return ds;
    }

    /**
     * Check if host of given url has to be changed.
     * <p>
     * If hostname of <code>url</code> is a property in 'conf/dcm4chee-xdsi/repository.properties', the value
     * (a URL) is used to change protocol, hostname and port.
     * 
     * @param url The URL which host is checked.
     * 
     * @return The modified URL if host is defined in property file
     * 
     * @throws IOException
     * @throws FileNotFoundException
     * @throws MalformedURLException
     */
    private URL checkHost(URL url) throws IOException, FileNotFoundException, MalformedURLException {
        if ( repositoryHostMapping == null ) {
            repositoryHostMapping = new Properties();
            try {
                File f = FileUtils.resolve(new File("conf/dcm4chee-xdsi/repository.properties"));
                if ( f.exists()) {
                    repositoryHostMapping.load( new FileInputStream(f));
                }
            } catch (Exception e) {
                log.warn("Initialize repositoryHostMapping failed!", e);
            }
        }
        String newHost = repositoryHostMapping.getProperty(url.getHost());
        if ( newHost != null ) {
            URL newHostUrl = new URL(newHost);
            url = new URL(newHostUrl.getProtocol(),newHostUrl.getHost(), newHostUrl.getPort(), url.getFile() );
        }
        return url;
    }

    /**
     * Get WADO URL for given retrieveAET and uids.
     * <p>
     * The mapping between <code>aet</code> and base URL (something like http://&lt;hostname&gt;:&lt;port&gt;/wado?requestType=WADO) 
     * must be defined in 'conf/dcm4chee-xdsi/aet2wado.properties'.
     * 
     * @param aet           Retrieve AET
     * @param studyIUID     Study Instance UID
     * @param seriesIUID    Series Instance UID
     * @param iuid          SOP Instance UID
     * @return The WADO URL to retrieve the DICOM object.
     */
    private String getWadoUrl(String aet, String studyIUID, String seriesIUID, String iuid) {
        StringBuffer sb = new StringBuffer(getWadoBaseURL(aet));
        sb.append("&amp;studyUID=").append(studyIUID).append("&amp;seriesUID=").append(seriesIUID);
        sb.append("&amp;objectUID=").append(iuid);
        return sb.toString();
    }

    /**
     * Get the Base WADO URL for given AET.
     * <p>
     * When <code>aet</code> is null or not defined in aet2wado.properties, a default URL
     * (defined with pseudo AET 'default_wado_url') is used instead.<br>
     * If the pseudo AET is also not defined, the URL to the local WADO service is used.
     *  
     * @param aet Retrieve AET
     * 
     * @return The Base WADO URL for given AET.
     */
    private String getWadoBaseURL(String aet) {
        if ( aet == null ) {
            log.warn("Retrieve AET missing! Use pseudo AET for default URL instead!");
            aet="default_wado_url";
        }
        if (aet2wado == null ) {
            aet2wado = new Properties();
            try {
                File f = FileUtils.resolve(new File("conf/dcm4chee-xdsi/aet2wado.properties"));
                aet2wado.load(new FileInputStream(f));
            } catch (Exception e) {
                log.warn("Initialize AET to WADO URL mapping failed!", e);
            }
        }
        String url = aet2wado.getProperty(aet);
        if ( url == null ) {
            log.warn("Retrieve AET not defined in 'conf/dcm4chee-xdsi/aet2wado.properties'! Use pseudo AET for default URL instead!");
            url = aet2wado.getProperty("default_wado_url","http://127.0.0.1:8080/wado?requestType=WADO");
        }
        return url;
    }

    public class ManifestInstance {
        private String url;
        private String iuid;
        private String cuid;
        private String type;

        public ManifestInstance( String iuid, String cuid, String url) {
            this.url = url;
            this.iuid = iuid;
            this.cuid = cuid;
            init();
        }

        private void init() {
            if ( cuidTypes == null ) {
                cuidTypes = new Properties();
                try {
                    File f = FileUtils.resolve(new File("conf/dcm4chee-xdsi/cuid2type.properties"));
                    if ( f.exists() ) {
                        cuidTypes.load(new FileInputStream(f));
                        Entry entry;
                        String key;
                        Properties chgd = new Properties();
                        for ( Iterator iter = cuidTypes.entrySet().iterator() ; iter.hasNext() ;) {
                            entry = (Entry) iter.next();
                            key = (String)entry.getKey();
                            if ( ! Character.isDigit( (key).charAt(0)) ) {
                                key = UIDs.forName( key );
                                chgd.put(key,entry.getValue() );
                            }
                        }
                        cuidTypes.putAll(chgd);
                    } else {
                        log.warn("'SOP Class UID to type' property file ('conf/dcm4chee-xdsi/cuid2type.properties') missing! all ManifestInstances will have type 'unknown'!");
                    }
                } catch (Exception e) {
                    log.warn("Initialize 'SOP Class UID to type' mapping failed!", e);
                }
            }
            type = cuidTypes.getProperty(cuid, "unknown");
        }

        public String getCuid() {
            return cuid;
        }

        public String getIuid() {
            return iuid;
        }

        public String getUrl() {
            return url;
        }

        public String getType() {
            return type;
        }
    }
}