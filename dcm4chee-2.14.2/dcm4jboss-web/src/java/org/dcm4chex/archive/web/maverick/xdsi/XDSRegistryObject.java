package org.dcm4chex.archive.web.maverick.xdsi;

public interface XDSRegistryObject {

    /**
     * Gets the universally unique ID (UUID) for this object.
     * @return
     */
    String getId();
    
    /**
     * Gets the user-friendly name of this object.
     * @return
     */
    String getName();
    
    /**
     * Gets the status of this object.
     * @return
     */
    String getStatus();
}
