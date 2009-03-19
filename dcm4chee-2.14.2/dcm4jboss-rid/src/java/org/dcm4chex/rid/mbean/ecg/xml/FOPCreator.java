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

package org.dcm4chex.rid.mbean.ecg.xml;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.dcm4chex.rid.mbean.ECGSupport;
import org.dcm4chex.rid.mbean.ecg.WaveformGroup;
import org.dcm4chex.rid.mbean.ecg.WaveformInfo;
import org.dcm4chex.rid.mbean.ecg.xml.SVGCreator;
import org.dcm4chex.rid.mbean.xml.XMLResponseObject;
import org.dcm4chex.rid.mbean.xml.XMLUtil;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * @author franz.willer
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FOPCreator implements XMLResponseObject{

	private static final NumberFormat cmFormatter = new DecimalFormat("##.##cm", new DecimalFormatSymbols( new Locale( "en", "us")));
	private static Logger log = Logger.getLogger( ECGSupport.class.getName() );
	
	private TransformerHandler th;
	private XMLUtil util;

	private Float pageHeight;
	private Float pageWidth;
	private Float graphHeight;
	private Float graphWidth;
	private WaveformGroup[] waveformGroups;
	private WaveformInfo info;

	/**
	 * @param wfgrps
	 * @param wfInfo
	 * @param float1
	 * @param float2
	 */
	public FOPCreator(WaveformGroup[] wfgrps, WaveformInfo wfInfo, Float width, Float height) {
		waveformGroups = wfgrps;
		info = wfInfo;
		pageWidth = width;
		pageHeight = height;
		graphHeight = new Float( pageHeight.floatValue() - 4.0f );
		graphWidth = new Float( pageWidth.floatValue() - 2.0f );
		if ( log.isDebugEnabled() ) log.debug("page (h*w):"+pageHeight+"*"+pageWidth);
		if ( log.isDebugEnabled() ) log.debug("graph (h*w):"+graphHeight+"*"+graphWidth);
	}

	public void toXML( OutputStream out ) throws TransformerConfigurationException, SAXException {
        SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();

		th = tf.newTransformerHandler();
		th.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
		th.setResult( new StreamResult(out) );
		th.startDocument();

		toXML();
		
		th.endDocument();
	    try {
			out.flush();
			out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void embedXML( TransformerHandler th ) throws TransformerConfigurationException, SAXException {
		this.th = th;
		toXML();
	}
	
	
	private void toXML() throws SAXException, TransformerConfigurationException {
		util = new XMLUtil( th );
		util.startElement( "fo:root", "xmlns:fo", "http://www.w3.org/1999/XSL/Format" );
		{
			addLayoutMasterSet();
			util.startElement( "fo:page-sequence", "master-reference", "page" );
			{
				addHeader();
				util.startElement( "fo:flow", "flow-name", "xsl-region-body");
				int len = waveformGroups.length;
				for ( int i = 0 ; i < len ; i++ ) {
					addGraphicHeader( waveformGroups[i] );
					addGraphic( waveformGroups[i] );
					addGraphicFooter( waveformGroups[i] );
				}
				addFooter();
				util.endElement( "fo:flow" );
			}
			util.endElement( "fo:page-sequence" );
			//addTestPage();
		}
		util.endElement( "fo:root" );
	}

	/**
	 * @throws SAXException
	 * 
	 */
	private void addTestPage() throws SAXException {
		util.startElement( "fo:page-sequence", "master-reference", "test" );
		util.startElement( "fo:flow", "flow-name", "xsl-region-body");
		util.startElement( "fo:block", XMLUtil.EMPTY_ATTRIBUTES );
		util.startElement( "fo:instream-foreign-object", "xmlns:svg", "http://www.w3.org/2000/svg" );
		SVGCreator creator = new SVGCreator( null, null, graphWidth, graphHeight  );
			creator.setXMLUtil( new XMLUtil( th, "svg" ) );
			creator.addSVGStart();
			creator.addDefs();
			creator.addGrid(pageWidth.intValue(), pageHeight.intValue());
			util.endElement("svg:svg");
		util.endElement( "fo:instream-foreign-object" );
	util.endElement( "fo:block" );
		util.endElement( "fo:flow" );
		util.endElement( "fo:page-sequence" );
	}

	/**
	 * @throws SAXException
	 * 
	 */
	private void addLayoutMasterSet() throws SAXException {
		util.startElement( "fo:layout-master-set", XMLUtil.EMPTY_ATTRIBUTES );
		{
			AttributesImpl attr = util.newAttribute( "master-name", "page");
			util.addAttribute( attr, "page-height", cmFormatter.format( pageHeight ) );
			util.addAttribute( attr, "page-width", cmFormatter.format( pageWidth ) );
			util.addAttribute( attr, "margin-left", "5mm" );
			util.addAttribute( attr, "margin-right", "5mm" );
			util.addAttribute( attr, "margin-top", "10mm" );
			util.addAttribute( attr, "margin-bottom", "0mm" );
			util.startElement( "fo:simple-page-master", attr );
			{
				util.startElement( "fo:region-before", "extent", "1cm" );
				util.endElement( "fo:region-before" );
				util.startElement( "fo:region-body", "margin-top", "1cm" );
				util.endElement( "fo:region-body" );
				util.startElement( "fo:region-after", "extent", "0.5cm" );
				util.endElement( "fo:region-after" );
			}
			util.endElement( "fo:simple-page-master" );
			AttributesImpl attr2 = util.newAttribute( "master-name", "test");
			util.addAttribute( attr2, "page-height", cmFormatter.format( pageHeight ) );
			util.addAttribute( attr2, "page-width", cmFormatter.format( pageWidth ) );
			util.addAttribute( attr2, "margin-left", "0mm" );
			util.addAttribute( attr2, "margin-right", "0mm" );
			util.addAttribute( attr2, "margin-top", "0mm" );
			util.addAttribute( attr2, "margin-bottom", "0mm" );
			util.startElement( "fo:simple-page-master", attr2 );
			{
				util.startElement( "fo:region-before", "extent", "0cm" );
				util.endElement( "fo:region-before" );
				util.startElement( "fo:region-body", "margin-top", "0cm" );
				util.endElement( "fo:region-body" );
				util.startElement( "fo:region-after", "extent", "0cm" );
				util.endElement( "fo:region-after" );
			}
			util.endElement( "fo:simple-page-master" );
		}
		util.endElement( "fo:layout-master-set" );
	}
	
	private void addHeader() throws SAXException {
		util.startElement( "fo:static-content", "flow-name", "xsl-region-before");
		{
			AttributesImpl attr = util.newAttribute( "font-size", "12pt");
			util.addAttribute( attr, "text-align", "center" );
			util.addAttribute( attr, "font-weight", "bold" );
			util.startElement( "fo:block", attr);
			util.addValue( info.getPatientName() );
				util.endElement( "fo:block");
				AttributesImpl attr1 = util.newAttribute( "font-size", "10pt");
				util.addAttribute( attr1, "text-align", "center" );
				util.startElement( "fo:block", attr1);
				util.addValue( "("+info.getPatientID()+")" );
			util.endElement( "fo:block");
		}
		util.endElement( "fo:static-content" );
	}
	/**
	 * @param group
	 * @throws SAXException
	 * 
	 */
	private void addGraphicHeader(WaveformGroup group) throws SAXException {
		AttributesImpl attr = util.newAttribute( "font-size", "10pt");
		util.startElement( "fo:block", attr);
		addTable( "none",
				  new String[]{"15mm","40mm","20mm","40mm","15mm","25mm","50mm","20mm","3mm","20mm"},
				  new String[]{"left","right","left","left","left","left","left","right","left","left"},
				  new String[][]{ {"","Acquis. Date:",info.getAcquisDate(),info.getAcquisTime(),null,"CONFIRMED",null,"Department:",null,"" },
								  {null,null,null,null,null,null,null,"Room:",null,"" },
				  				  {null,"Birth Date:",info.getBirthday(),info.getSex(),"0/0",info.getPatientSize(),info.getPatientWeight(),"Operator:",null,"" }
								  				}
				);
		util.endElement( "fo:block");
	}

	/**
	 * @param group
	 * @param out
	 * @throws SAXException
	 * @throws TransformerConfigurationException
	 * 
	 */
	private void addGraphic(WaveformGroup group) throws TransformerConfigurationException, SAXException {
		util.startElement( "fo:block", XMLUtil.EMPTY_ATTRIBUTES );
			util.startElement( "fo:instream-foreign-object", "xmlns:svg", "http://www.w3.org/2000/svg" );
			SVGCreator creator = new SVGCreator( group, null, graphWidth, graphHeight  );
				creator.embedXML( th );
			util.endElement( "fo:instream-foreign-object" );
		util.endElement( "fo:block" );
	}

	/**
	 * @throws SAXException
	 * 
	 */
	private void addGraphicFooter( WaveformGroup group ) throws SAXException {
		AttributesImpl attr = util.newAttribute( "font-size", "10pt");
		util.addAttribute( attr, "text-align", "center" );
		util.startElement( "fo:block", attr);
		{
			if ( group.getNrOfChannels() == 12 ) { //TODO: Hack, only know that 12lead use this scaling, also should be checked via CUID!
				util.singleElement("fo:inline", null, "25mm/sec" );
				util.singleElement("fo:inline", null, "10mm/mV" );
			}
			util.singleElement("fo:inline", null, group.getFilterText() );
		}
		util.endElement( "fo:block");
	}
	
	private void addTable( String border, String[] columnWidths, String[] textAligns, String[][] data) throws SAXException {
		AttributesImpl tblAttr = util.newAttribute( "border-style", border);
		util.addAttribute( tblAttr, "table-layout", "fixed" );
		util.startElement( "fo:table", tblAttr);
		AttributesImpl colAttr = util.newAttribute( "column-number", border);
		for ( int i = 0 ; i < columnWidths.length ; i++ ) {
			util.singleElement("fo:table-column", util.newAttribute( "column-width", columnWidths[i]), null );
		}
		util.startElement( "fo:table-body", null);
		int rows = data.length;
		int cols = data[0].length;
		AttributesImpl cellAttr = util.newAttribute( "border-style", "none");
		for ( int row = 0 ; row < rows ; row++ ) {
			util.startElement( "fo:table-row", null);
			for ( int col = 0 ; col < cols ; col++ ) {
				util.startElement( "fo:table-cell", cellAttr );
				util.singleElement("fo:block", util.newAttribute( "text-align", textAligns[col]), data[row][col] );
				util.endElement( "fo:table-cell");
			}
			util.endElement( "fo:table-row");
		}
		util.endElement( "fo:table-body");
		util.endElement( "fo:table");
	}

	/**
	 * 
	 */
	private void addFooter() {
		// TODO Auto-generated method stub
		
	}


}
