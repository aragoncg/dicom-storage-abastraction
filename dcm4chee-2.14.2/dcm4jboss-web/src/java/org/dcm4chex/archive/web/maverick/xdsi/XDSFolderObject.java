package org.dcm4chex.archive.web.maverick.xdsi;

import org.dcm4chee.xds.infoset.v30.RegistryPackageType;
import org.dcm4chee.xds.infoset.v30.util.InfoSetUtil;

public class XDSFolderObject implements XDSRegistryObject {
    private RegistryPackageType rp;

    public XDSFolderObject( RegistryPackageType registryPackageType ) {
        this.rp = registryPackageType;
    }

    public String getId() {
        return rp.getId();
    }
    public String getName() {
        return InfoSetUtil.getString(rp.getName(), null);
    }
    public String getStatus() {
        return rp.getStatus();
    }
    
    public RegistryPackageType getRegistryPackage() {
        return rp;
    }
}
