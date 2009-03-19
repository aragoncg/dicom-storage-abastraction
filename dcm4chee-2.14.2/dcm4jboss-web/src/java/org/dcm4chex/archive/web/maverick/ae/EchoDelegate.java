/*
 * Created on 29.12.2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.dcm4chex.archive.web.maverick.ae;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;

import org.apache.log4j.Logger;
import org.dcm4chex.archive.ejb.interfaces.AEDTO;
import org.jboss.mx.util.MBeanServerLocator;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class EchoDelegate {
	   private static ObjectName echoServiceName = null;
		private static MBeanServer server;
		
	    private static Logger log = Logger.getLogger( EchoDelegate.class.getName() );

	    /** 
	     * Iinitialize the Echo service delegator.
	     * <p>
	     * Set the name of the EchoService MBean with the servlet config param 'echoServiceName'.
	     * 
	     * @param config The ServletConfig object.
	     */
		public void init( ServletConfig config ) {
	        if (server != null) return;
	        server = MBeanServerLocator.locate();
	        String s = config.getInitParameter("echoServiceName");
	        try {
	        	echoServiceName = new ObjectName(s);
				
			} catch (Exception e) {
				log.error( "Exception in init! ",e );
			}
	       
	    }

		public Logger getLogger() {
			return log;
		}
		
		/**
		 * Makes the MBean call to echo an AE configuration.
		 * 
		 * 
		 * @return An info string for status of echo.
		 */
		public String echo( AEDTO aeData, int nrOfTests ) {
			if ( log.isDebugEnabled() ) log.debug("Send echo to "+aeData);
			String resp = null;
			try {
		        Object o = server.invoke(echoServiceName,
		                "echo",
		                new Object[]{ aeData, new Integer( nrOfTests ) },
		                new String[]{ AEDTO.class.getName(), Integer.class.getName() } );
		        resp = (String) o;
			} catch ( Exception x ) {
				log.error( "Exception occured in echoAE: "+x.getMessage(), x );
			}
			if ( log.isDebugEnabled() ) log.debug("echo response for "+aeData+":"+resp);
	        return resp;
		}

		/**
		 * Makes the MBean call to echo an AET (AE config for given title).
		 * 
		 * 
		 * @return An info string for status of echo.
		 */
		public String echo( String aet, int nrOfTests ) {
			String resp = null;
			try {
		        Object o = server.invoke(echoServiceName,
		                "echo",
		                new Object[]{ aet, new Integer( nrOfTests ) },
		                new String[]{ String.class.getName(), Integer.class.getName() } );
		        resp = (String) o;
			} catch ( Exception x ) {
				log.error( "Exception occured in echo (AET="+aet+"): "+x.getMessage(), x );
			}
	        return resp;
		}

		/**
		 * Makes the MBean call to echoe an AE configuration.
		 * 
		 * 
		 * @return An info string for status of echo.
		 */
		public String[] echoAll( AEDTO aeData ) {
			String[] resp = null;
			try {
		        Object o = server.invoke(echoServiceName,
		                "echoAll",
		                null,
		                null );
		        resp = (String[]) o;
			} catch ( Exception x ) {
				log.error( "Exception occured in echo ("+aeData+"): "+x.getMessage(), x );
			}
	        return resp;
		}
		
}
