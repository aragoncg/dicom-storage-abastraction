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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.dcm4che.data.Dataset;
import org.dcm4che.data.DcmElement;
import org.dcm4che.data.DcmObjectFactory;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4che.util.UIDGenerator;
import org.dcm4cheri.util.StringUtils;


/**
 * @author franz.willer
 *
 * The Structured Report Manifest Model for Teaching File Selector WEB interface.
 */
public class SRManifestModel {

	private static final String DESIGNATOR_DCM = "DCM";
	private static final String CONTAINS = "CONTAINS";
	private List textValues;
	private int selectedCategory = 0;
	private int selectedLevel = -1;
	private boolean useManifest = false;
	private boolean confirmed = false;
	private String defaultAuthor;

	public static final String[] CATEGORIES = new String[]
	    {"Musculoskeletal",
		"Pulmonary",
		"Cardiovascular",
		"Gastrointestinal",
		"Genitourinary",
		"Neuro",
		"Vascular and Interventional",
		"Nuclear",
		"Ultrasound",
		"Pediatric",
		"Breast"};
	
	public static final String[] CATEGORY_CODES = new String[]
        {"TCE301","TCE302","TCE303","TCE304","TCE305","TCE306",
		"TCE307","TCE308","TCE309","TCE310","TCE311"};

	public static final String[] LEVELS = new String[]
		{"Primary",
		"Intermediate",
		"Advanced"};

	public static final String[] LEVEL_CODES = new String[]{"TCE201","TCE202","TCE203"};
	
	public static final String DESIGNATOR_IHE_RAD = "IHERADTF";

	private static final DcmObjectFactory dof = DcmObjectFactory.getInstance();
	
	private static final int[] BASIC_TAGS = new int[] {
		Tags.PatientID, Tags.PatientName, Tags.IssuerOfPatientID, Tags.PatientBirthDate,
		Tags.StudyInstanceUID, Tags.StudyID, Tags.StudyIDIssuer, Tags.StudyDate, Tags.StudyTime, Tags.StudyDescription,
		Tags.AccessionNumber, Tags.Manufacturer, Tags.SpecificCharacterSet 
	};
	
	/**
	 * @return Returns the mapTextValues.
	 */
	public List getTextFields() {
		return textValues;
	}
	/**
	 * Creates the model.
	 * <p>
	 * @param observerPersonName
	 */
	protected SRManifestModel() {
		initMap();
	}
	
	/**
	 * 
	 */
	private void initMap() {
		textValues = new ArrayList();
		textValues.add(new SRItem("Author", defaultAuthor,true,"TCE101", DESIGNATOR_IHE_RAD, "Author"));
		textValues.add(new SRItem("Abstract", "",false, "TCE104", DESIGNATOR_IHE_RAD, "Abstract" ,3, 60)); //use TextArea here
		textValues.add(new SRItem("Keywords", "",false,"TCE105", DESIGNATOR_IHE_RAD, "Keywords" ));
		textValues.add(new SRItem("History", "",false,"121060", DESIGNATOR_DCM, "History" ));
		textValues.add(new SRItem("Findings", "",false,"121071", DESIGNATOR_DCM, "Finding" ));
		textValues.add(new SRItem("Discussions", "",false,"TCE106", DESIGNATOR_IHE_RAD, "Discussion" ));
		textValues.add(new SRItem("Impressions", "",false,"111023", DESIGNATOR_DCM, "Differential Diagnosis/Impression" ));
		textValues.add(new SRItem("Diagnosis", "",false,"TCE107", DESIGNATOR_IHE_RAD, "Diagnosis" ));
		textValues.add(new SRItem("Anatomy", "",false,"112005", DESIGNATOR_DCM, "Radiographic anatomy" ));
		textValues.add(new SRItem("Pathology", "",false,"111042", DESIGNATOR_DCM, "Pathology" ));
		textValues.add(new SRItem("Organ system", "",false,"TCE108", DESIGNATOR_IHE_RAD, "Organ system" ));
	}

	public void fillParams(HttpServletRequest rq) {
		String key;
		if ( rq.getParameter("useManifest") != null ) {
			useManifest = "yes".equals( rq.getParameter("useManifest") );
		}
		setSelectedCategory(rq.getParameter("category"));
		setSelectedLevel(rq.getParameter("level"));
		SRItem item;
		String missing = null;
		for ( Iterator iter = textValues.iterator() ; iter.hasNext() ; ) {
			item = (SRItem)iter.next();
			item.setValue( rq.getParameter(item.name));
			if ( item.isMandatory() && item.getValue() == null ) {
				missing = missing == null ? item.getName() : missing+","+item.getName();
			}
		}
		if ( rq.getParameter("confirmed") != null ) {
			confirmed = "yes".equals( rq.getParameter("confirmed") );
		}
		if ( missing != null ) {
			throw new IllegalArgumentException("Mandatory field '"+missing+"' missing!");
		}
	}
	

	/**
	 * @return Returns the useManifest.
	 */
	public boolean isUseManifest() {
		return useManifest;
	}
	
	public boolean isConfirmed() {
		return confirmed;
	}
	
	public int getSelectedLevel() {
		return selectedLevel;
	}
	/**
	 * @param setSelectedLevel The setSelectedLevel to set.
	 */
	public void setSelectedLevel(String level) {
		if ( level == null ) 
			return;
		try {
			selectedLevel = Integer.parseInt(level);
			if ( selectedLevel < 0 || selectedLevel >= LEVELS.length ) {
				selectedLevel = -1;
			}
		} catch ( Exception x) {
			selectedLevel = -1;
		}
	}
	public int getSelectedCategory() {
		return selectedCategory;
	}
	/**
	 * @param selectedCategory The selectedCategory to set.
	 */
	public void setSelectedCategory(String category) {
		if ( category == null ) 
			return;
		try {
			selectedCategory = Integer.parseInt(category);
			if ( selectedCategory < 0 || selectedCategory >= CATEGORIES.length ) {
				throw new IllegalArgumentException("Illegal Category selection!");
			}
		} catch ( Exception x) {
			throw new IllegalArgumentException("Illegal Category selection! (NaN)");
		}
	}
	
	/**
	 * @return Returns the defaultAuthor.
	 */
	public String getDefaultAuthor() {
		return defaultAuthor;
	}
	/**
	 * @param defaultAuthor The defaultAuthor to set.
	 */
	public void setDefaultAuthor(String defaultAuthor) {
		this.defaultAuthor = defaultAuthor;
	}
	
	public String[] getCategories() {
		return CATEGORIES;
	}
	
	public String[] getLevels() {
		return LEVELS;
	}


	public Dataset getSR(Dataset rootInfo, Collection contents) {
		Dataset ds = null;
		if ( this.isUseManifest() ) {
			ds = newSR();
			ds.putAll(rootInfo.subSet(BASIC_TAGS));
			fillContent( ds, contents );
		}
		return ds;
	}

	private Dataset newSR() {
    	UIDGenerator uidGenerator = UIDGenerator.getInstance();
		Dataset ds = dof.newDataset();
    	String seriesIUID = uidGenerator.createUID();
	    ds.putUI(Tags.SeriesInstanceUID, seriesIUID );
	    ds.putUI(Tags.SOPClassUID, UIDs.BasicTextSR);
	    ds.putUI(Tags.SOPInstanceUID, uidGenerator.createUID());
        ds.putCS(Tags.Modality, "SR");
        ds.putIS(Tags.InstanceNumber, 1);
        ds.putDA(Tags.ContentDate, new Date());
        ds.putTM(Tags.ContentTime, new Date());
    	ds.putSQ(Tags.ContentSeq);
    	ds.putSQ(Tags.CurrentRequestedProcedureEvidenceSeq);
        ds.putCS(Tags.ValueType, "CONTAINER");
    	DcmElement sq = ds.putSQ(Tags.ConceptNameCodeSeq);
    	Dataset item = sq.addNewItem();
    	item.putSH(Tags.CodeValue,"TCE006");
    	item.putSH(Tags.CodingSchemeDesignator,DESIGNATOR_IHE_RAD);
		item.putLO(Tags.CodeMeaning, "Additional Teaching File Information");
	    return ds;
    }

	/**
	 * @param ds
	 */
	private void fillContent(Dataset ds, Collection contents) {
		DcmElement sq = ds.get(Tags.ContentSeq);
		for ( Iterator iter = textValues.iterator() ; iter.hasNext() ; ) {
			addTextItem( sq, CONTAINS, (SRItem)iter.next() );
		}
		addCodeItem( sq, CONTAINS, CATEGORY_CODES[selectedCategory], DESIGNATOR_IHE_RAD, CATEGORIES[selectedCategory], "TCE109", DESIGNATOR_IHE_RAD, "Category" );
		if ( selectedLevel >= 0 ) {
			addCodeItem( sq, CONTAINS, LEVEL_CODES[selectedLevel], DESIGNATOR_IHE_RAD, LEVELS[selectedLevel], "TCE110", DESIGNATOR_IHE_RAD, "Level" );
		}
		addCodeItem( sq, CONTAINS, isConfirmed() ? "R-00339":"R-0038D", "SRT", confirmed ? "Yes":"No", "TCE111", DESIGNATOR_IHE_RAD, "Diagnosis confirmed" );
		if ( contents != null ) {
			for ( Iterator iter = contents.iterator() ; iter.hasNext() ;) {
				sq.addItem((Dataset) iter.next());
			}
		}
	}
	
	private void addCodeItem(DcmElement sq, String relation, String codeValue, String designator, String codeMeaning, String conceptCode, String conceptCodeDesignator, String conceptNameMeaning) {
		Dataset ds = sq.addNewItem();
		ds.putCS(Tags.RelationshipType, relation);
		ds.putCS(Tags.ValueType,"CODE");
		DcmElement cnSq1 = ds.putSQ(Tags.ConceptNameCodeSeq);
		Dataset cnDS1 = cnSq1.addNewItem();
		cnDS1.putSH(Tags.CodeValue, conceptCode);
		cnDS1.putSH(Tags.CodingSchemeDesignator, conceptCodeDesignator);
		cnDS1.putLO(Tags.CodeMeaning, conceptNameMeaning);
		DcmElement ccSq1 = ds.putSQ(Tags.ConceptCodeSeq);
		Dataset ccDS1 = ccSq1.addNewItem();
		ccDS1.putSH(Tags.CodeValue, codeValue);
		ccDS1.putSH(Tags.CodingSchemeDesignator, designator);
		ccDS1.putLO(Tags.CodeMeaning, codeMeaning);
		
	}

	private void addTextItem(DcmElement sq, String relation, SRItem item) {
		if ( item == null || item.getValue() == null || item.getValue().length() < 1 ) return;
		String[] values = StringUtils.split( item.getValue(), '|');
		String cnCode = item.cnCode;
		String cnCodeDesignator = item.cnCodeDesign;
		String cnMeaning = item.cnMeaning;
		Dataset ds;
		for ( int i = 0 ; i < values.length ; i++ ) {
			ds = sq.addNewItem();
			ds.putCS(Tags.RelationshipType, relation);
			ds.putCS(Tags.ValueType,"TEXT");
			DcmElement cnSq1 = ds.putSQ(Tags.ConceptNameCodeSeq);
			Dataset cnDS1 = cnSq1.addNewItem();
			cnDS1.putSH(Tags.CodeValue, cnCode);
			cnDS1.putSH(Tags.CodingSchemeDesignator, cnCodeDesignator);
			cnDS1.putLO(Tags.CodeMeaning, cnMeaning);
			ds.putLO(Tags.TextValue, values[i] );
		}
	}

	public class SRItem {
		private String name, value, cnCode, cnCodeDesign, cnMeaning;
		private boolean mandatory;
		private int rows = -1; //INPUT type=text
		private int size = 60;
		
		public SRItem( String name, String value, boolean mandatory, String cnCode, String cnCodeDesign, String cnMeaning) {
			this.name = name;
			this.value = value;
			this.mandatory = mandatory;
			this.cnCode = cnCode;
			this.cnCodeDesign = cnCodeDesign;
			this.cnMeaning = cnMeaning;
		}
	
		public SRItem( String name, String value, boolean mandatory, String cnCode, String cnCodeDesign, String cnMeaning, int rows, int size) {
			this( name, value, mandatory, cnCode, cnCodeDesign, cnMeaning);
			this.rows = rows;
			this.size = size;
		}
		/**
		 * @return
		 */
		public boolean isMandatory() {
			return mandatory;
		}

		/**
		 * @return Returns the value.
		 */
		public String getValue() {
			return value;
		}
		/**
		 * @param value The value to set.
		 */
		public void setValue(String value) {
			if ( value != null ) {
				this.value = value;
			}
		}
		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}
		
		public int getSize() {
			return size;
		}
		
		public int getRows() {
			return rows;
		}
		/**
		 * @return Returns the cnCode.
		 */
		public String cnCode() {
			return cnCode;
		}
		/**
		 * @return Returns the cnCodeDesign.
		 */
		public String cnCodeDesign() {
			return cnCodeDesign;
		}
		/**
		 * @return Returns the cnMeaning.
		 */
		public String cnMeaning() {
			return cnMeaning;
		}
	}

	/**
	 * 
	 */
	public void clear() {
		selectedCategory = 0;
		selectedLevel = -1;
		useManifest = false;
		confirmed = false;
		initMap();
	}
}
