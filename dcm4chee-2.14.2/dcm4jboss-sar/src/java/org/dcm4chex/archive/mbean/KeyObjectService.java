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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.UIDGenerator;
import org.dcm4chex.archive.ejb.interfaces.ContentManager;
import org.dcm4chex.archive.ejb.interfaces.ContentManagerHome;
import org.dcm4chex.archive.util.EJBHomeFactory;
import org.dcm4chex.archive.util.HomeFactoryException;
import org.jboss.system.ServiceMBeanSupport;

/**
 * @author franz.willer@tiani.com
 * @version $Revision: 5547 $ $Date: 2007-11-26 17:39:06 +0100 (Mon, 26 Nov 2007) $
 * @since 04.01.2006
 */
public class KeyObjectService extends ServiceMBeanSupport {

	private DcmObjectFactory dof = DcmObjectFactory.getInstance();
	
	private Map waveformSopClassUIDs = new TreeMap();
	private Map compositSopClassUIDs = new TreeMap();
	
    private static Logger log = Logger.getLogger( KeyObjectService.class.getName() );

	private ContentManager contentMgr;
	
	public KeyObjectService() {
    }
    
    protected void startService() throws Exception {
    }

    protected void stopService() throws Exception {
    }
    
	/**
	 * @return Returns the compositSopClassUIDs.
	 */
	public Map getCompositSopClassUIDs() {
		return compositSopClassUIDs;
	}
	/**
	 * @param compositSopClassUIDs The compositSopClassUIDs to set.
	 */
	public void setCompositSopClassUIDs(Map compositSopClassUIDs) {
		this.compositSopClassUIDs = compositSopClassUIDs;
	}
	/**
	 * @return Returns the waveformSopClassUIDs.
	 */
	public String getWaveformSopClassUIDs() {
		return getCUIDs( waveformSopClassUIDs );
	}
	/**
	 * @param waveformSopClassUIDs The waveformSopClassUIDs to set.
	 */
	public void setWaveformSopClassUIDs(String sopCuids) {
		if ( sopCuids == null || sopCuids.trim().length() == 0 ) {
			setDefaultWaveformSopCuids();
		} else {
			waveformSopClassUIDs = setCUIDs(sopCuids);
		}
	}

	/**
	 * @return Returns the waveformSopClassUIDs.
	 */
	public String getCompositeSopClassUIDs() {
		return getCUIDs( compositSopClassUIDs );
	}
	/**
	 * @param waveformSopClassUIDs The waveformSopClassUIDs to set.
	 */
	public void setCompositeSopClassUIDs(String sopCuids) {
		if ( sopCuids == null || sopCuids.trim().length() == 0 ) {
			setDefaultCompositeSopCuids();
		} else {
			compositSopClassUIDs = setCUIDs(sopCuids);
		}
	}

	private String getCUIDs(Map map) {
		if ( map == null || map.isEmpty() ) return "";
		StringBuffer sb = new StringBuffer( map.size() << 5);//StringBuffer initial size: nrOfUIDs x 32
		Iterator iter = map.keySet().iterator();
		while ( iter.hasNext() ) {
			sb.append(iter.next()).append(System.getProperty("line.separator", "\n"));
		}
		return sb.toString();
	}
	
	private Map setCUIDs( String sopCuids ) {
        StringTokenizer st = new StringTokenizer(sopCuids, "\r\n;");
        String uid,name;
        Map map = new TreeMap();
        int i = 0;
        char ch;
        while ( st.hasMoreTokens() ) {
        	uid = st.nextToken().trim();
    		name = uid;
    		ch = uid.charAt(0);
        	if ( ch >= '0' && ch <= '9' ) {
        		if ( ! UIDs.isValid(uid) ) 
        			throw new IllegalArgumentException("UID "+uid+" isn't a valid UID!");
        	} else {
        		uid = UIDs.forName( name );
        	}
        	map.put(name,uid);
        }
		return map;
	}
	/**
	 * 
	 */
	private void setDefaultWaveformSopCuids() {
		waveformSopClassUIDs.clear();
		waveformSopClassUIDs.put( "TwelveLeadECGWaveformStorage", UIDs.TwelveLeadECGWaveformStorage );
		waveformSopClassUIDs.put( "GeneralECGWaveformStorage", UIDs.GeneralECGWaveformStorage );
		waveformSopClassUIDs.put( "AmbulatoryECGWaveformStorage", UIDs.AmbulatoryECGWaveformStorage );
		waveformSopClassUIDs.put( "HemodynamicWaveformStorage", UIDs.HemodynamicWaveformStorage );
		waveformSopClassUIDs.put( "CardiacElectrophysiologyWaveformStorage", UIDs.CardiacElectrophysiologyWaveformStorage );
	}
    
	/**
	 * 
	 */
	private void setDefaultCompositeSopCuids() {
		compositSopClassUIDs.clear();
		compositSopClassUIDs.put( "BasicTextSR", UIDs.BasicTextSR );
		compositSopClassUIDs.put( "EnhancedSR", UIDs.EnhancedSR );
		compositSopClassUIDs.put( "ComprehensiveSR", UIDs.ComprehensiveSR );
		compositSopClassUIDs.put( "KeyObjectSelectionDocument", UIDs.KeyObjectSelectionDocument );
	}
    
    
	public Dataset getKeyObject(String iuids) throws RemoteException, FinderException, HomeFactoryException, CreateException {
    	StringTokenizer st = new StringTokenizer(iuids,",|");
    	List l = new ArrayList();
    	while ( st.hasMoreTokens() ) {
    		l.add(st.nextToken());
    	}
    	return getKeyObject(l,null, null);
    }
    /**
     * Creates a new Key Object Selection Dataset.
     * <p>
     * The content sequence will contain all items from <code>contentItems</code> and references 
     * to all instances given in iuids.
     * <p>
     * If instances from differet studies (but same patient) are given, the dataset will contain the Identical Document sequence with
     * references to copies of this Key Object in the approbiate study. (new generated Series and SOP Instance UID).
     * This Sequence can be used to create the copies of the Key object.
     * <p>
     * If instances from different patients are given, an IllegalArgumentException will be thrown.
     *  
     * @param iuids List of instances referenced by this Key Object.
     * @param rootInfo Dataset with Attributes for root node. (e.g. Concept Name code sequence for title)
     * @param contentItems List of Datasets. each dataset is an item of the Content Sequence.
     * 
     * @return The Key Object Selection Dataset.
     * 
     * @throws RemoteException
     * @throws FinderException
     * @throws HomeFactoryException
     * @throws CreateException
     */
	public Dataset getKeyObject(Collection iuids, Dataset rootInfo, Collection contentItems) throws RemoteException, FinderException, HomeFactoryException, CreateException{
    	Collection col = lookupContentManager().getSOPInstanceRefMacros(iuids);
    	Dataset ds;
    	if ( ! col.isEmpty() ) {
    		String studyIUID = ((Dataset)col.iterator().next()).getString(Tags.StudyInstanceUID);
        	ds = newKeyObject( studyIUID, rootInfo );
        	addPatInfo( ds, studyIUID );
        	addContentSequence(ds, contentItems, col);
        	addReqProcEvidenceSequence(ds,col);
    		
    	} else {
    		ds = dof.newDataset();
    	}
    	if ( log.isDebugEnabled() ) {
    		log.debug("Key Object Dataset:"); log.debug(ds);
    	}
    	return ds;
    }
    
    private Dataset newKeyObject(String studyIUID, Dataset rootInfo) {
    	UIDGenerator uidGenerator = UIDGenerator.getInstance();
		Dataset ds = dof.newDataset();
    	if ( rootInfo != null ) {
    		ds.putAll(rootInfo);
    	}
        ds.putUI(Tags.StudyInstanceUID, studyIUID != null ? studyIUID : uidGenerator.createUID());
    	String seriesIUID = uidGenerator.createUID();
        ds.putUI(Tags.SeriesInstanceUID, seriesIUID );
        ds.putUI(Tags.SOPClassUID, UIDs.KeyObjectSelectionDocument);
        ds.putUI(Tags.SOPInstanceUID, uidGenerator.createUID());
        ds.putCS(Tags.Modality, "KO");
        ds.putIS(Tags.InstanceNumber, 1);
        ds.putDA(Tags.ContentDate, new Date());
        ds.putTM(Tags.ContentTime, new Date());
        ds.putCS(Tags.ValueType, "CONTAINER");
    	Dataset tmplDS = ds.putSQ(Tags.ContentTemplateSeq).addNewItem();
    	tmplDS.putCS(Tags.TemplateIdentifier, "2010");
    	tmplDS.putCS(Tags.MappingResource, "DCMR");
        ds.putSQ(Tags.RefPPSSeq);//Type 2 Attr
	return ds;
    }
    
	/**
	 * @param ds
	 * @param col
	 */
	private void addContentSequence(Dataset ds, Collection conceptNames, Collection refSopInstances) {
    	DcmElement sq = ds.putSQ(Tags.ContentSeq);
    	if ( conceptNames != null ) {
    		for ( Iterator iter = conceptNames.iterator() ; iter.hasNext() ; ) {
    			sq.addItem( (Dataset) iter.next() );
    		}
    	}
    	Dataset sopInstRef, refSerItem, refSopItem, item;
    	DcmElement refSerSq,refSopSq;
    	for ( Iterator iter = refSopInstances.iterator() ; iter.hasNext() ; ) {
    		sopInstRef = (Dataset) iter.next();
    		refSerSq = sopInstRef.get(Tags.RefSeriesSeq);
    		for ( int i = 0,len = refSerSq.countItems() ; i < len ; i++ ) {
    			refSerItem = refSerSq.getItem(i);
    			refSopSq = refSerItem.get(Tags.RefSOPSeq);
    			for ( int j=0,n=refSopSq.countItems() ; j < n ; j++ ) {
    				refSopItem = refSopSq.getItem(j);
    				item = sq.addNewItem();
    				item.putCS(Tags.RelationshipType, "CONTAINS");
    				item.putCS(Tags.ValueType, getValueType( refSopItem.getString(Tags.RefSOPClassUID)));
    				Dataset sopDS = item.putSQ(Tags.RefSOPSeq).addNewItem();
    				sopDS.putUI(Tags.RefSOPClassUID, refSopItem.getString(Tags.RefSOPClassUID));
    				sopDS.putUI(Tags.RefSOPInstanceUID, refSopItem.getString(Tags.RefSOPInstanceUID));
    			}
    		}
    	}
	}

	/**
	 * @param string
	 * @return
	 */
	private String getValueType(String cuid) {
		if ( this.compositSopClassUIDs.containsValue(cuid)) return "COMPOSITE";
		if ( this.waveformSopClassUIDs.containsValue(cuid)) return "WAVEFORM";
		return "IMAGE";
	}

	/**
	 * @param ds
	 * @param col
	 * @throws CreateException
	 * @throws HomeFactoryException
	 * @throws FinderException
	 * @throws RemoteException
	 */
	private void addReqProcEvidenceSequence(Dataset ds, Collection col) throws RemoteException, FinderException, HomeFactoryException, CreateException {
		DcmElement sq = ds.putSQ(Tags.CurrentRequestedProcedureEvidenceSeq);
		if ( col.isEmpty() ) return;
		Iterator iter = col.iterator();
		sq.addItem( (Dataset) iter.next() );
		if ( iter.hasNext() ) {
			DcmElement identicalSq = ds.putSQ(Tags.IdenticalDocumentsSeq);
			Dataset studyDS;
			String patID = ds.getString(Tags.PatientID);
			String patID1;
			for (  ; iter.hasNext() ; ) { //loop only if this key object selection contains instances in different studies
				studyDS = (Dataset) iter.next();
				patID1 = lookupContentManager().getPatientForStudy(studyDS.getString(Tags.StudyInstanceUID)).getString(Tags.PatientID);
				if ( ! patID.equals(patID1)) {
					throw new IllegalArgumentException("Instances of different patients are not allowed!");
				}
				sq.addItem( studyDS );//add to ReqProcEvidenceSequence
				addSOPInstanceRef(identicalSq, studyDS);//add copy reference to identical document sequence
			}
		}
	}

	/**
	 * @param identicalSq
	 * @param studyDS
	 */
	private void addSOPInstanceRef(DcmElement sq, Dataset studyDS) {
		Dataset ds = sq.addNewItem();
		ds.putUI( Tags.StudyInstanceUID, studyDS.getString(Tags.StudyInstanceUID) );
		DcmElement serSq = ds.putSQ(Tags.RefSeriesSeq);
		Dataset serDS = serSq.addNewItem();
		serDS.putUI(Tags.SeriesInstanceUID, UIDGenerator.getInstance().createUID());
		DcmElement refSopSq = serDS.putSQ(Tags.RefSOPSeq);
		Dataset instDS = refSopSq.addNewItem();
		instDS.putUI( Tags.RefSOPInstanceUID, UIDGenerator.getInstance().createUID() );
		instDS.putUI( Tags.RefSOPClassUID, UIDs.KeyObjectSelectionDocument );
	}

	/**
	 * @param ds
	 * @param studyIUID
	 * @throws CreateException
	 * @throws HomeFactoryException
	 * @throws FinderException
	 * @throws RemoteException
	 */
	private void addPatInfo(Dataset ds, String studyIUID) throws RemoteException, FinderException, HomeFactoryException, CreateException {
		Dataset patDS = lookupContentManager().getPatientForStudy(studyIUID);
		ds.putAll(patDS);
		
	}

    private ContentManager lookupContentManager() throws HomeFactoryException, RemoteException, CreateException  {
    	if ( contentMgr != null ) return contentMgr;
        ContentManagerHome home = (ContentManagerHome) EJBHomeFactory.getFactory()
                .lookup(ContentManagerHome.class, ContentManagerHome.JNDI_NAME);
        contentMgr = home.create();
        return contentMgr;
    }
}
