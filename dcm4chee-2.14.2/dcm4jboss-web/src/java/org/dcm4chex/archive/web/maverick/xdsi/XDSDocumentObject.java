package org.dcm4chex.archive.web.maverick.xdsi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import javax.xml.registry.JAXRException;

import org.apache.log4j.Logger;
import org.dcm4chee.xds.infoset.v30.ClassificationType;
import org.dcm4chee.xds.infoset.v30.ExtrinsicObjectType;
import org.dcm4chee.xds.infoset.v30.SlotType1;
import org.dcm4chee.xds.infoset.v30.util.InfoSetUtil;
import org.dcm4chex.archive.xdsi.UUID;

public class XDSDocumentObject implements XDSRegistryObject {
    private ExtrinsicObjectType eo;
    private Map<String, SlotType1> slots;
    private String uri, url, creationTime;
    private Map<String, ClassificationType> classifications;

    private static Logger log = Logger.getLogger( XDSDocumentObject.class.getName() );
    public XDSDocumentObject( ExtrinsicObjectType extrinsicObjectType ) {
        this.eo = extrinsicObjectType;
        init();
    }

    private void init() {
        slots = InfoSetUtil.getSlotsFromRegistryObject(eo);
        classifications = InfoSetUtil.getClassificationsFromRegistryObject(eo);
        uri = getLongURI();
        if ( uri != null ) {
            try {
                url = URLEncoder.encode(uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                log.error("URL encoding of URI failed! UTF-8 encoding not supported! Use URI unencoded!");
                url = uri;
            }
        }
        creationTime = getSlotValue("creationTime", null);
    }

    /**
     * @return
     * @throws JAXRException
     */
    private String getLongURI() {
        return InfoSetUtil.getLongSlotValue(getSlotValues("URI"));
    }

    public String getId() {
        return eo.getId();
    }
    /**
     * Get Document Title (name of ExtrinsicObject)
     * 
     * @return
     */
    public String getName() {
        return InfoSetUtil.getString(eo.getName(), null);
    }

    /**
     * Get MimeType of this document.
     * 
     * @return
     */
    public String getMimeType() {
        return eo.getMimeType();
    }

    public String getCreationTime() {
        return creationTime;
    }
    /**
     * Get URI.
     * @return
     */
    public String getURI() {
        return uri;
    }

    /**
     * Get the URL encoded String of URI.
     * @return
     */
    public String getURL() {
        return url;
    }

    /**
     * Get the Unique ID of the Repository for XDS.b retrieve.
     * @return Value of Slot repositoryUniqueId.
     * 
     */
    public String getRepositoryUID() {
        return getSlotValue("repositoryUniqueId", null);
    }

    /**
     * Get status of document as String.
     * 
     * @return
     */
    public String getStatus() {
        return eo.getStatus();
    }

    public String getAuthorInstitution() {
        return getSlotValue("authorInstitution", null);
    }

    public String getAuthorPerson() {
        return getSlotValue("authorPerson", null);
    }
    public String getAuthorRole() {
        return getSlotValue("authorRole", null);
    }

    public XDSClassification getClassCode() {
        ClassificationType cl = classifications.get(UUID.XDSDocumentEntry_classCode);
        return cl == null ? null : new XDSClassification(cl);
    }
    public XDSClassification getTypeCode() {
        ClassificationType cl = classifications.get(UUID.XDSDocumentEntry_typeCode);
        return cl == null ? null : new XDSClassification(cl);
    }
    public XDSClassification getFormatCode() {
        ClassificationType cl = classifications.get(UUID.XDSDocumentEntry_formatCode);
        return cl == null ? null : new XDSClassification(cl);
    }

    private String getSlotValue(String name, String def) {
        List<String> values = getSlotValues(name);
        return ( values == null || values.isEmpty() )? def : values.get(0);
    }
    private List<String> getSlotValues(String name) {
        SlotType1 slot = slots.get(name);
        return slot == null ? null : slot.getValueList().getValue();
    }
    
    public ExtrinsicObjectType getExtrinsicObjetc() {
        return eo;
    }

}
