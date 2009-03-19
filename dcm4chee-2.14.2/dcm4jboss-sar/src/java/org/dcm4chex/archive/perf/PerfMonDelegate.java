package org.dcm4chex.archive.perf;

import javax.management.ObjectName;

import org.dcm4che.net.ActiveAssociation;
import org.dcm4che.net.Association;
import org.dcm4che.net.Dimse;
import org.jboss.system.ServiceMBeanSupport;

/**
 * This delegate passes all method calls to the real performance monitoring 
 * MBean service.
 * 
 * @author Fang Yang (fang.yang@agfa.com)
 * @version $Id: PerfMonDelegate.java 2953 2006-11-14 08:47:01Z gunterze $
 * @since Nov 2, 2006
 */
public class PerfMonDelegate {
	
	private ServiceMBeanSupport service;
	
	private ObjectName perfMonServiceName;
	
	public PerfMonDelegate(ServiceMBeanSupport service) {
		this.service = service;
	}

	public final ObjectName getPerfMonServiceName() {
		return perfMonServiceName;
	}

	/**
	 * Set the service name of performance monitoring.
	 * <p>
	 * Note that the empty string "" in xmdesc file will create ObjectName with canonical name "*:*"
	 * 
	 * @param perfMonServiceName
	 */
	public final void setPerfMonServiceName(ObjectName perfMonServiceName) {
		if(perfMonServiceName.getCanonicalName().equals("*:*"))
			this.perfMonServiceName = null;
		else
			this.perfMonServiceName = perfMonServiceName;
	}
	
	public void assocEstStart(Association assoc, int command) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"assocEstStart",
                    new Object[] { assoc, new Integer(command) },
                    new String[] { Association.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation assocEstStart on "
                    + perfMonServiceName, e);
        }
	}
	
	public void assocEstEnd(Association assoc, int command) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"assocEstEnd",
                    new Object[] { assoc, new Integer(command) },
                    new String[] { Association.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation assocEstEnd on "
                    + perfMonServiceName, e);
        }
	}
	
	public void assocRelStart(Association assoc, int command) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"assocRelStart",
                    new Object[] { assoc, new Integer(command) },
                    new String[] { Association.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation assocRelStart on "
                    + perfMonServiceName, e);
        }
	}
	
	public void assocRelEnd(Association assoc, int command) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"assocRelEnd",
                    new Object[] { assoc, new Integer(command) },
                    new String[] { Association.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation assocRelEnd on "
                    + perfMonServiceName, e);
        }
	}
	
	public void start(ActiveAssociation assoc, Dimse rq, int counterEnum ) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"start",
                    new Object[] { assoc, rq, new Integer(counterEnum) },
                    new String[] { ActiveAssociation.class.getName(), Dimse.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().error("Failed to invoke operation start on "
                    + perfMonServiceName, e);
        }
	}
	
	/**
	 * Set the value to a performance monitoring property. It's up to the counter to utilize the value.
	 * 
	 * @param assoc The ActiveAssociation object
	 * @param rq The Dimse object
	 * @param perfPropEnum The performance property enum
	 * @param data The value
	 * @throws Exception
	 */
	public void setProperty(ActiveAssociation assoc, Dimse rq, int perfPropEnum, Object data) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"setProperty",
                    new Object[] { assoc, rq, new Integer(perfPropEnum), data },
                    new String[] { ActiveAssociation.class.getName(), Dimse.class.getName(), int.class.getName(), Object.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation start on "
                    + perfMonServiceName, e);
        }
	}
	
	public void stop(ActiveAssociation assoc, Dimse rq, int counterEnum) {
		if(perfMonServiceName == null) 
			return;
			
        try {
            service.getServer().invoke(perfMonServiceName,"stop",
                    new Object[] { assoc, rq, new Integer(counterEnum) },
                    new String[] { ActiveAssociation.class.getName(), Dimse.class.getName(), int.class.getName()});
        } catch (Exception e) {
            service.getLog().fatal("Failed to invoke operation stop on "
                    + perfMonServiceName, e);
        }
	}	
}
