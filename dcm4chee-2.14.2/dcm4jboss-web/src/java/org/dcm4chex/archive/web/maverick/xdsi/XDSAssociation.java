package org.dcm4chex.archive.web.maverick.xdsi;

public class XDSAssociation implements XDSRegistryObject {

    private XDSRegistryObject source;
    private XDSRegistryObject target;
    private String type;
    private String status;

    public XDSAssociation(XDSRegistryObject source, XDSRegistryObject target, String type, String status) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.status = status;
    }
    public String getId() {
        return null;
    }

    public String getName() {
        return null;
    }

    /**
     * Get status of Association as int.
     * 
     * @return
     * @throws JAXRException
     */
    public String getStatus() {
        return status;
    }

    public XDSRegistryObject getSource() {
        return source;
    }

    public XDSRegistryObject getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }
}
