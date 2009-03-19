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
package org.dcm4chex.archive.xdsi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.dcm4che.data.Dataset;
import org.dcm4che.dict.Tags;
import org.dcm4che.dict.UIDs;
import org.dcm4cheri.util.StringUtils;
import org.dcm4cheri.util.UIDGeneratorImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * @author franz.willer@gwi-ag.com
 * @version $Revision: 3288 $ $Date: 2007-04-23 12:00:02 +0200 (Mon, 23 Apr 2007) $
 * @since Feb 15, 2006
 */
public class XDSMetadata {
	private static final String TAG_ASSOCIATION = "Association";
	private static final String DEFAULT_TIME_STRING = "000000";
	private static final String DEFAULT_DATE_STRING = "11111111";
	private static final String ATTR_CLASSIFICATION_NODE = "classificationNode";
	private static final String ATTR_NODE_REPRESENTATION = "nodeRepresentation";
	private static final String ATTR_CLASSIFICATION_SCHEME = "classificationScheme";
	private static final String ATTR_CLASSIFIED_OBJECT = "classifiedObject";
	private static final String ATTR_OBJECTTYPE = "objectType";
	private static final String ATTR_MIMETYPE = "mimeType";
	private static final String TAG_EXTRINSICOBJECT = "ExtrinsicObject";
	private static final String TAG_LEAFREGISTRYOBJECTLIST = "LeafRegistryObjectList";
	private static final String TAG_LOCALIZEDSTRING = "LocalizedString";
	private static final String TAG_REGISTRYPACKAGE = "RegistryPackage";
	private static final String TAG_SLOT = "Slot";
	private static final String TAG_VALUELIST = "ValueList";
	private static final String TAG_NAME = "Name";
	private static final String TAG_DESCRIPTION = "Description";
	private static final String TAG_VALUE = "Value";
	private static final String TAG_OBJECTREF = "ObjectRef";
	private static final String TAG_CLASSIFICATION = "Classification";
	private static final String TAG_EXTERNAL_IDENTIFIER = "ExternalIdentifier";
	private static final String XMLNS_DEFAULT = "xmlns";
	private static final String XMLNS_RIM = "xmlns:rim";
	private static final String XMLNS_RS = "xmlns:rs";
	private static final String XMLNS_XSI = "xmlns:xsi";
	private static final String TAG_SUBMITOBJECTSREQUEST = "rs:SubmitObjectsRequest";
	
	private static final String URN_RIM = "urn:oasis:names:tc:ebxml-regrep:rim:xsd:2.1";
	private static final String URN_RS = "urn:oasis:names:tc:ebxml-regrep:registry:xsd:2.1";
	
	private static final AttributesImpl EMPTY_ATTRIBUTES = new AttributesImpl();
	private TransformerHandler th;
	
	private static Logger log = Logger.getLogger(XDSIService.class.getName());

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private Dataset dsKO;
	private Properties mdValues;
    XDSIDocument[] docs;
	private URL xslt;
    
    private boolean newFolder = false;

	public XDSMetadata( Dataset ds, Properties props, XDSIDocument[] docs ) {
		dsKO = ds;
        this.docs = docs;
		this.mdValues = props != null ? props : new Properties();
		initProperties(props);            
        newFolder = mdValues.getProperty("folder.uniqueId") != null;
	}
	/**
	 * 
	 */
	private void initTransformHandler() {
		try {
	        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
	        if (xslt != null) {
	        	try {
	        		th = tf.newTransformerHandler(new StreamSource(xslt.openStream(),
	                    xslt.toExternalForm()));
	        	} catch ( IOException x ) {
	        		log.error("Cant open xsl file:"+xslt, x );
	        	}
	        } else {
	        	th = tf.newTransformerHandler();
	        	th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
	        }
		} catch ( Throwable t ) {
			t.printStackTrace();
		}
	}
	/**
	 * @param props
	 */
	private void initProperties(Properties props) {
		if ( props == null ) return;
		try {
			setXSLT( props.getProperty("XSLT_URL"));
		} catch (MalformedURLException x) {
			log.error("Wrong XSLT URL for XDS Metadata! Additional transformation disabled!", x);
		}
	}
	/**
	 * @param xslt
	 * @throws MalformedURLException
	 */
	private void setXSLT(String xslt) throws MalformedURLException {
		if ( xslt != null ) {
			this.xslt = new URL( xslt );
		} else {
			this.xslt = null;
		}
	}
	/**
	 * 
	 * @return
	 */
	public Document getMetadata() {
		initTransformHandler();
    	DOMResult result = new DOMResult();
        th.setResult( result );
        try {
			th.startDocument();
			addSubmitObjectRequest();
			th.endDocument();
		} catch (SAXException x) {
			log.error( "Cant build metadata!",x);
			return null;
		}
		return (Document) result.getNode();
	}

	private void addSubmitObjectRequest() throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		//attr.addAttribute("", XMLNS_XSI, XMLNS_XSI, "", "http://www.w3.org/2001/XMLSchema-instance");		
		th.startPrefixMapping(XMLNS_RS,URN_RS);
		th.startPrefixMapping(XMLNS_DEFAULT,URN_RIM);
    	th.startElement(URN_RS, TAG_SUBMITOBJECTSREQUEST, TAG_SUBMITOBJECTSREQUEST, attr );
    	addLeafRegistryObjectList();
    	th.endElement(URN_RS, TAG_SUBMITOBJECTSREQUEST, TAG_SUBMITOBJECTSREQUEST );
		th.endPrefixMapping(XMLNS_RS);
		th.endPrefixMapping(XMLNS_DEFAULT);
	}
	
	private void addLeafRegistryObjectList() throws SAXException {
    	th.startElement(URN_RIM, TAG_LEAFREGISTRYOBJECTLIST, TAG_LEAFREGISTRYOBJECTLIST, EMPTY_ATTRIBUTES );
    	addObjectRef(UUID.XDSSubmissionSet);
    	addObjectRef(UUID.XDSSubmissionSet_authorPerson);
    	addObjectRef(UUID.XDSSubmissionSet_uniqueId);
    	addObjectRef(UUID.XDSSubmissionSet_sourceId);
    	addObjectRef(UUID.XDSSubmissionSet_patientId); 
    	addObjectRef(UUID.XDSSubmissionSet_contentTypeCode); 
    	addObjectRef(UUID.XDSDocumentEntry_authorPerson);
    	addObjectRef(UUID.XDSDocumentEntry_uniqueId);
    	addObjectRef(UUID.XDSDocumentEntry_patientId); 
    	addObjectRef(UUID.XDSDocumentEntry_classCode); 
    	addObjectRef(UUID.XDSDocumentEntry_confidentialityCode); 
    	addObjectRef(UUID.XDSDocumentEntry_formatCode); 
    	addObjectRef(UUID.XDSDocumentEntry_eventCodeList); 
    	addObjectRef(UUID.XDSDocumentEntry_healthCareFacilityTypeCode); 
    	addObjectRef(UUID.XDSDocumentEntry_practiceSettingCode); 
        addObjectRef(UUID.XDSDocumentEntry_typeCode); 
    	addClassification("SubmissionSet", UUID.XDSSubmissionSet);
        if ( newFolder ) {
            addObjectRef(UUID.XDSFolder); 
            addObjectRef(UUID.XDSFolder_uniqueId); 
            addObjectRef(UUID.XDSFolder_patientId); 
            addClassification("Folder", UUID.XDSFolder);
        }
        String title;
        if ( this.dsKO != null ) {
            for ( int i = 0 ; i < docs.length ; i++ ) {
                addExtrinsicObject(docs[i]);
            }
            Dataset cnDS = dsKO.getItem(Tags.ConceptNameCodeSeq);
            title = cnDS != null ? cnDS.getString(Tags.CodeMeaning) : mdValues.getProperty("docTitle","Title?");
        } else {
            title = mdValues.getProperty("submissionSetTitle","TITLE");
        }
        addSubmissionRegistryPackage("SubmissionSet", title);
        if ( newFolder ) {
            addFolderRegistryPackage("Folder", mdValues.getProperty("folder.name","FOLDER"));
            addAssociation("SubmissionSet","Folder", "HasMember", null);
            if ( docs != null ) {
            for ( int i = 0 ; i < docs.length ; i++ ) {
                addAssociation("Folder", docs[i].getDocumentID(),"HasMember", null);
            }
        }
        }
        if ( docs != null ) {
            for ( int i = 0 ; i < docs.length ; i++ ) {
                addAssociation("SubmissionSet",docs[i].getDocumentID(), "HasMember", "Original");
            }

            String folders = mdValues.getProperty("folder_assoc.uniqueId");
            if ( folders != null) {
                StringTokenizer st = new StringTokenizer(folders, "|");
                String uuid;
                while ( st.hasMoreTokens() ) {
                    uuid = st.nextToken();
                    for ( int i = 0 ; i < docs.length ; i++ ) {
                        addAssociation(uuid,docs[i].getDocumentID(), "HasMember", null);
                    }
                    addObjectRef(uuid);
                }
            }
        }
        addDocRefs();
    	th.endElement(URN_RIM, TAG_LEAFREGISTRYOBJECTLIST, TAG_LEAFREGISTRYOBJECTLIST );
		
	}
	
	private void addDocRefs() throws SAXException {
        if ( docs != null ) {
            XDSIDocument doc;
            List assocs;
            XDSIDocument.Association assoc;
            String uuid;
            List objectRefs = new ArrayList();
            for ( int i = 0 ; i < docs.length ; i++ ) {
                assocs = docs[i].getAssociations();
                if ( assocs != null ) {
                    for ( Iterator iter = assocs.iterator() ; iter.hasNext() ; ) {
                        assoc = (XDSIDocument.Association) iter.next();
                        uuid = assoc.getUUID();
                        addAssociation(docs[i].getDocumentID(),uuid, assoc.getType(), assoc.getStatus());
                        if ( !objectRefs.contains(uuid)){
                            addObjectRef(uuid);
                            objectRefs.add(uuid);
                        }
                    }
                }
            }
        }
    }
    /**
	 * @param targetObject
	 * @param status
	 * @throws SAXException
	 */
	private void addAssociation(String sourceObject, String targetObject, String type, String status) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "associationType", "associationType", "", type);		
		attr.addAttribute("", "sourceObject", "sourceObject", "", sourceObject);		
		attr.addAttribute("", "targetObject", "targetObject", "", targetObject);		
    	th.startElement("", TAG_ASSOCIATION, TAG_ASSOCIATION, attr );
    	if ( status != null ) addSlot("SubmissionSetStatus", status);
    	th.endElement("", TAG_ASSOCIATION, TAG_ASSOCIATION );
		
	}
	/**
	 * @throws SAXException
	 * 
	 */
	private void addObjectRef(String id) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "id", "id", "", id);		
    	th.startElement("", TAG_OBJECTREF, TAG_OBJECTREF, attr );
    	th.endElement("", TAG_OBJECTREF, TAG_OBJECTREF );
		
	}

	private void addClassification(String object,String node) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", ATTR_CLASSIFIED_OBJECT, ATTR_CLASSIFIED_OBJECT, "", object);		
		attr.addAttribute("", ATTR_CLASSIFICATION_NODE, ATTR_CLASSIFICATION_NODE, "", node);		
    	th.startElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION, attr );
    	th.endElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION );
		
	}
	
	private void addClassification(String scheme, String object, String node, String name, String codingSchemeOID) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", ATTR_CLASSIFIED_OBJECT, ATTR_CLASSIFIED_OBJECT, "", object);		
		attr.addAttribute("", ATTR_CLASSIFICATION_SCHEME, ATTR_CLASSIFICATION_SCHEME, "", scheme);
		attr.addAttribute("", ATTR_NODE_REPRESENTATION, ATTR_NODE_REPRESENTATION, "", node);		
    	th.startElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION, attr );
    	addLocalized(TAG_NAME,EMPTY_ATTRIBUTES,name);
    	this.addSlot("codingScheme",new String[]{codingSchemeOID});
    	th.endElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION );
		
	}

	private void addAuthor(String scheme, String object, String node, String authorPerson, String authorInstitution,
			String authorRole, String authorSpecialty) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", ATTR_CLASSIFIED_OBJECT, ATTR_CLASSIFIED_OBJECT, "", object);		
		attr.addAttribute("", ATTR_CLASSIFICATION_SCHEME, ATTR_CLASSIFICATION_SCHEME, "", scheme);
		attr.addAttribute("", ATTR_NODE_REPRESENTATION, ATTR_NODE_REPRESENTATION, "", node);		
    	th.startElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION, attr );
    	addSlot("authorPerson",new String[]{authorPerson});
    	if ( authorInstitution != null ) 
    		addSlot("authorInstitution",new String[]{authorInstitution});
    	if ( authorRole != null ) 
    		addSlot("authorRole", new String[]{authorRole});
    	if ( authorSpecialty != null ) 
    	addSlot("authorSpecialty", new String[]{authorSpecialty});
    	th.endElement("", TAG_CLASSIFICATION, TAG_CLASSIFICATION );
		
	}
	
	/**
	 * @param doc 
	 * @throws SAXException
	 * 
	 */
	private void addExtrinsicObject(XDSIDocument doc) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "id", "id", "", doc.getDocumentID());
        //String mime = mdValues.getProperty("mimetype","application/dicom");
        String mime = doc.getMimeType();
		attr.addAttribute("", ATTR_MIMETYPE, ATTR_MIMETYPE, "", mime);		
		attr.addAttribute("", ATTR_OBJECTTYPE, ATTR_OBJECTTYPE, "", UUID.XDSDocumentEntry);		
    	th.startElement("", TAG_EXTRINSICOBJECT, TAG_EXTRINSICOBJECT, attr );
    	Dataset cnDS = dsKO.getItem(Tags.ConceptNameCodeSeq);
    	String title = cnDS != null ? cnDS.getString(Tags.CodeMeaning) : mdValues.getProperty("docTitle","Title?");
    	addLocalized(TAG_NAME, EMPTY_ATTRIBUTES, title);
    	addLocalized("Description", EMPTY_ATTRIBUTES, mdValues.getProperty("description",null));
    	addExtrinsicEntries(doc);
    	th.endElement("", TAG_EXTRINSICOBJECT, TAG_EXTRINSICOBJECT );
	}
	/**
	 * @throws SAXException
	 * 
	 */
	private void addExtrinsicEntries(XDSIDocument doc) throws SAXException {
		//addSlot("test", "tval");
		addSlot( "creationTime", getTime(Tags.InstanceCreationDate,Tags.InstanceCreationTime, Tags.ContentDate, Tags.ContentTime));
		addSlot( "languageCode", mdValues.getProperty("languageCode", "en-us"));
		addSlot( "serviceStartTime", getTime(Tags.StudyDate,Tags.StudyTime, -1,-1));
		addSlot( "sourcePatientId", getPatID(dsKO.getString(Tags.PatientID), dsKO.getString(Tags.IssuerOfPatientID)));
		addSlot( "sourcePatientInfo", getPatientInfo());
		if( "true".equals( mdValues.getProperty("useOldAuthorSlot") ) ) {
			addSlot( XDSIService.AUTHOR_SPECIALITY, mdValues.getProperty(XDSIService.AUTHOR_SPECIALITY, null));
			addSlot( "authorInstitution", mdValues.getProperty(XDSIService.AUTHOR_INSTITUTION, null));
			addSlot( XDSIService.AUTHOR_PERSON, mdValues.getProperty(XDSIService.AUTHOR_PERSON, null));
			addSlot( XDSIService.AUTHOR_ROLE, mdValues.getProperty(XDSIService.AUTHOR_ROLE, null));
			addSlot( XDSIService.AUTHOR_ROLE_DISPLAYNAME, mdValues.getProperty(XDSIService.AUTHOR_ROLE_DISPLAYNAME, null));
		} else {
			addAuthor( UUID.XDSDocumentEntry_authorPerson, doc.getDocumentID(), "",
				mdValues.getProperty(XDSIService.AUTHOR_PERSON, null),
				mdValues.getProperty(XDSIService.AUTHOR_INSTITUTION, null),
				mdValues.getProperty(XDSIService.AUTHOR_ROLE, null),
				mdValues.getProperty(XDSIService.AUTHOR_SPECIALITY, null));
		}

		addClassification(UUID.XDSDocumentEntry_classCode, doc.getDocumentID(), 
				mdValues.getProperty("classCode","Education"),
				mdValues.getProperty("classCodeDisplayName","Education"), 
				mdValues.getProperty("classCodeCodingSchemeOID","Connect-a-thon classCodes"));		
		addClassification(UUID.XDSDocumentEntry_confidentialityCode, doc.getDocumentID(), 
				mdValues.getProperty("confidentialityCode","C"),
				mdValues.getProperty("confidentialityCodeDN","Celebrity"),
				mdValues.getProperty("confidentialityCodeCodingSchemeOID","Connect-a-thon confidentialityCodes"));

		addEventCodeList(doc);
		String mime = doc.getMimeType();
		if ( "application/dicom".equals(mime)) {
			addClassification(UUID.XDSDocumentEntry_formatCode, doc.getDocumentID(), 
					UIDs.KeyObjectSelectionDocument,
					"Key Object Selection Document", 
					"1.2.840.10008.2.6.1" );
		} else {
			addClassification(UUID.XDSDocumentEntry_formatCode, doc.getDocumentID(), 
					mdValues.getProperty("formatCode."+mime,"PDF"),
					mdValues.getProperty("formatCodeDN."+mime,"PDF Document"), 
					mdValues.getProperty("formatCodeCodingSchemeOID."+mime,"Connect-a-thon formatCodes") );
		}
		addClassification(UUID.XDSDocumentEntry_healthCareFacilityTypeCode, doc.getDocumentID(), 
				mdValues.getProperty("healthCareFacilityTypeCode","Assisted Living"),
				mdValues.getProperty("healthCareFacilityTypeCodeDN","Assisted Living"),
				mdValues.getProperty("healthCareFacilityTypeCodeCodingSchemeOID","Connect-a-thon healthcareFacilityTypeCodes"));
		//TODO parentDocumentRelationship
		
		addClassification(UUID.XDSDocumentEntry_practiceSettingCode, doc.getDocumentID(), 
				mdValues.getProperty("practiceSettingCode","Cardiology"),
				mdValues.getProperty("practiceSettingCodeDN","Cardiology"),
				mdValues.getProperty("practiceSettingCodeCodingSchemeOID","Connect-a-thon practiceSettingCodes"));		
		//TODO Optional title
		addClassification(UUID.XDSDocumentEntry_typeCode, doc.getDocumentID(), 
				mdValues.getProperty("typeCode","34098-4"),
				mdValues.getProperty("typeCodeDN","Conference Evaluation Note"),
				mdValues.getProperty("typeCodeCodingSchemeOID","LOINC"));		
		
		addExternalIdentifier(UUID.XDSDocumentEntry_uniqueId,"XDSDocumentEntry.uniqueId", doc.getUniqueID());
		addExternalIdentifier(UUID.XDSDocumentEntry_patientId,"XDSDocumentEntry.patientId", 
				mdValues.getProperty("xadPatientID"));
	}
	
	/**
	 * @throws SAXException
	 * 
	 */
	private void addEventCodeList(XDSIDocument doc) throws SAXException {
		String eventCodeList = mdValues.getProperty("eventCodeList");
		if ( eventCodeList == null || eventCodeList.trim().length() < 1 ) return;
		
		StringTokenizer st = new StringTokenizer( eventCodeList, "|");
		String[] code;//0..codeValue, 1.. codeMeaning (2..codeDesignator)
		int pos;
		while ( st.hasMoreTokens() ) {
			code = StringUtils.split(st.nextToken(), '^');
			addClassification(UUID.XDSDocumentEntry_eventCodeList, doc.getDocumentID(), 
					code[0],
					code.length > 1 ? code[1] : code[0],
					code.length > 2 ? code[2] : mdValues.getProperty("eventCodeListCodingSchemeOID","Connect-a-thon confidentialityCodes"));
		}
	}
	private String getTime(int dateTag, int timeTag, int alternateDateTag, int alternateTimeTag) {
		String date = dsKO.getString(dateTag);
		String time;
		if ( date == null ) {
			if ( alternateDateTag > 0) {
				date = dsKO.getString(alternateDateTag);
			}
			if (date == null ) {
				return DEFAULT_DATE_STRING+DEFAULT_TIME_STRING;
			} else if (alternateTimeTag != -1){
				time = dsKO.getString(alternateTimeTag);
				if ( time == null ) time =DEFAULT_TIME_STRING;
			} else {
				time =DEFAULT_TIME_STRING;
			}
		} else if (timeTag != -1) {
			time = dsKO.getString(timeTag);
			if ( time == null ) time =DEFAULT_TIME_STRING;
			
		} else {
			time = DEFAULT_TIME_STRING;
		}
		return date+time.substring(0,6);//cut fraction part of time
	}
	/**
	 * @return
	 */
	private String[] getPatientInfo() {
		String[] values = new String[5];
		values[0] = "PID-3|"+getPatID(dsKO.getString(Tags.PatientID), dsKO.getString(Tags.IssuerOfPatientID));
		values[1] = "PID-5|"+dsKO.getString(Tags.PatientName);
		String birthdate = dsKO.getString(Tags.PatientBirthDate);
		if (birthdate == null ) {
			values[2] = null;
		} else {
			String birthtime = dsKO.getString(Tags.PatientBirthTime);
			values[2] = "PID-7|"+birthdate+(birthtime != null ? birthtime : DEFAULT_TIME_STRING);
		}
		String sex = dsKO.getString(Tags.PatientSex);
		values[3] = "PID-8|"+(sex != null ? sex : "U");
		String addr = dsKO.getString(Tags.PatientAddress);
		if (addr == null) {
			values[4] = null;
		} else {
			values[4] = "PID-11|"+dsKO.getString(Tags.PatientAddress);
		}
		return values;
	}
	/**
	 * @param string
	 * @param string2
	 * @return
	 */
	private String getPatID(String patID, String issuer) {
		patID += "^^^";
		if ( issuer == null ) return patID;
		return patID+issuer;
	}
	/**
	 * @param string
	 * @param property
	 * @throws SAXException
	 */
	private void addSlot(String name, String value) throws SAXException {
		addSlot(name, value == null ? null : new String[]{value});
	}
	
	private void addSlot(String name, String[] values) throws SAXException {
		if ( values != null ) {
			AttributesImpl attr = new AttributesImpl();
			attr.addAttribute("", "name", "name", "", name);		
	    	th.startElement("", TAG_SLOT, TAG_SLOT, attr );
	    	th.startElement("", TAG_VALUELIST, TAG_VALUELIST, EMPTY_ATTRIBUTES );
	    	for ( int i = 0 ; i < values.length ; i++ ) {
	    		if ( values[i] != null ) {
		    		th.startElement("", TAG_VALUE, TAG_VALUE, EMPTY_ATTRIBUTES );
		    		th.characters(values[i].toCharArray(),0,values[i].length());
			    	th.endElement("", TAG_VALUE, TAG_VALUE );
	    		}
	    	}
	    	th.endElement("", TAG_VALUELIST, TAG_VALUELIST );
	    	th.endElement("", TAG_SLOT, TAG_SLOT );
		}
		
	}
	/**
	 * @throws SAXException
	 * 
	 */
	private void addSubmissionRegistryPackage(String id, String title) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "id", "id", "", id);		
    	th.startElement("", TAG_REGISTRYPACKAGE, TAG_REGISTRYPACKAGE, attr );
    	addLocalized(TAG_NAME, EMPTY_ATTRIBUTES, title);
    	addLocalized(TAG_DESCRIPTION, EMPTY_ATTRIBUTES, mdValues.getProperty("comments",null));
    	addRegistryEntries(id);
    	th.endElement("", TAG_REGISTRYPACKAGE, TAG_REGISTRYPACKAGE );
	}
    /**
     * @throws SAXException
     * 
     */
    private void addRegistryEntries(String id) throws SAXException {
        String time = mdValues.getProperty("submissionTime");
        if ( time == null ) time = formatter.format(new Date());
        addSlot( "submissionTime", time);
        
		if( "true".equals( mdValues.getProperty("useOldAuthorSlot") ) ) {
	        addSlot( "authorDepartment", mdValues.getProperty("authorDepartment", null));
	        addSlot( XDSIService.AUTHOR_INSTITUTION, mdValues.getProperty(XDSIService.AUTHOR_INSTITUTION, null));
	        addSlot( XDSIService.AUTHOR_PERSON, mdValues.getProperty(XDSIService.AUTHOR_PERSON, null));
		} else {
			addAuthor( UUID.XDSSubmissionSet_authorPerson, id, "",
				mdValues.getProperty(XDSIService.AUTHOR_PERSON, null),
				mdValues.getProperty(XDSIService.AUTHOR_INSTITUTION, null),
				mdValues.getProperty(XDSIService.AUTHOR_ROLE, null),
				mdValues.getProperty(XDSIService.AUTHOR_SPECIALITY, null));
		}
        
        addClassification(UUID.XDSSubmissionSet_contentTypeCode, "SubmissionSet", 
                mdValues.getProperty("contentTypeCode","Group counseling"),
                mdValues.getProperty("contentTypeCodeDN","Group counseling"),
                mdValues.getProperty("contentTypeCodeCodingSchemeOID","Connect-a-thon contentTypeCodes"));

        String uniqueId = mdValues.getProperty("uniqueId");
        if ( uniqueId == null ) {
            uniqueId = UIDGeneratorImpl.getInstance().createUID();
        }
        addExternalIdentifier(UUID.XDSSubmissionSet_uniqueId,"XDSSubmissionSet.uniqueId", uniqueId);
        addExternalIdentifier(UUID.XDSSubmissionSet_sourceId,"XDSSubmissionSet.sourceId", mdValues.getProperty("sourceId", uniqueId));
        addExternalIdentifier(UUID.XDSSubmissionSet_patientId,"XDSSubmissionSet.patientId", 
                mdValues.getProperty("xadPatientID"));
    }

    private void addFolderRegistryPackage(String id, String title) throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        attr.addAttribute("", "id", "id", "", id);      
        th.startElement("", TAG_REGISTRYPACKAGE, TAG_REGISTRYPACKAGE, attr );
        addLocalized(TAG_NAME, EMPTY_ATTRIBUTES, title);
        addLocalized(TAG_DESCRIPTION, EMPTY_ATTRIBUTES, mdValues.getProperty("folder.comments",null));
        addFolderEntries();
        th.endElement("", TAG_REGISTRYPACKAGE, TAG_REGISTRYPACKAGE );
    }
    
	/**
	 * @throws SAXException
	 * 
	 */
	private void addFolderEntries() throws SAXException {
		String uniqueId = mdValues.getProperty("xdsfolder.uniqueId");
		if ( uniqueId == null ) {
			uniqueId = UIDGeneratorImpl.getInstance().createUID();
		}
		addExternalIdentifier(UUID.XDSFolder_uniqueId,"XDSFolder.uniqueId", uniqueId);
		addExternalIdentifier(UUID.XDSFolder_patientId,"XDSFolder.patientId", 
				mdValues.getProperty("xadPatientID"));
	}
	/**
	 * @param property
	 * @return
	 */
	private String[] split(String s, String delim) {
		StringTokenizer st = new StringTokenizer( s, delim);
		String[] sa = new String[ st.countTokens() ];
		for ( int i = 0; i < sa.length ; i++ ) {
			sa[i] = st.nextToken();
		}
		return sa;
	}
	/**
	 * @param string
	 * @param string2
	 * @param string3
	 * @throws SAXException
	 */
	private void addExternalIdentifier(String scheme, String name, String value) throws SAXException {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "identificationScheme", "identificationScheme", "", scheme);		
		attr.addAttribute("", "value", "value", "", value);		
    	th.startElement("", TAG_EXTERNAL_IDENTIFIER, TAG_EXTERNAL_IDENTIFIER, attr );
    	addLocalized(TAG_NAME, EMPTY_ATTRIBUTES, name);
    	th.endElement("", TAG_EXTERNAL_IDENTIFIER, TAG_EXTERNAL_IDENTIFIER );
		
	}
	private void addLocalized(String name, Attributes attr, String value ) throws SAXException {
    	th.startElement("", name, name, attr );
    	if ( value != null ) {
			AttributesImpl valAttr = new AttributesImpl();
			valAttr.addAttribute("", "value", "value", "", value);		
	    	th.startElement("", TAG_LOCALIZEDSTRING, TAG_LOCALIZEDSTRING, valAttr );
	    	th.endElement("", TAG_LOCALIZEDSTRING, TAG_LOCALIZEDSTRING );
    	}
    	th.endElement("", name, name );
	}
	private static void dumpNode( Node n, String ident ) {
		log.info(ident+"node:"+n.getNodeName());
		NodeList nl = n.getChildNodes();
		String ident1 = ident+"  ";
		for ( int i = 0, len = nl.getLength() ; i < len ; i++ ) {
			log.info(ident+"child("+i+"):");
			dumpNode(nl.item(i), ident1);
		}
	}
	

}
