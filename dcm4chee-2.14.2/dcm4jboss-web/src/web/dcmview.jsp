<%--
	$Id: dcmview.jsp,v 1.2 2004/02/18 02:35:05 javawilli Exp $
	$Source: /cvsroot/dcm4che/dcm4jboss-web/src/web/dcmview.jsp,v $
	
	Dicom Header View. Use Dataset stored in session attribute 'dataset2view'
--%>
<%@ page import='java.io.OutputStream' %>

<%@ page import='javax.xml.transform.TransformerFactory' %>
<%@ page import='javax.xml.transform.sax.SAXTransformerFactory' %>
<%@ page import='javax.xml.transform.sax.TransformerHandler' %>
<%@ page import='javax.xml.transform.stream.StreamSource' %>
<%@ page import='javax.xml.transform.stream.StreamResult' %>
<%@ page import='javax.xml.transform.Templates' %>

<%@ page import='org.dcm4che.data.Dataset' %>
<%@ page import='org.dcm4che.dict.DictionaryFactory' %>
<%@ page import='org.dcm4che.dict.TagDictionary' %>
<%@ page import='org.dcm4che.data.Dataset' %>


<%
	Dataset ds = (Dataset) request.getSession().getAttribute("dataset2view");
	if ( ds != null ) {
		TagDictionary dict = DictionaryFactory.getInstance().getDefaultTagDictionary();
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
		Templates stylesheet = tf.newTemplates(new StreamSource("resource:dicom_html.xsl"));
		TransformerHandler th = tf.newTransformerHandler(stylesheet);
		String title = (String) request.getSession().getAttribute("titleOfdataset2view");
		if ( title != null )
			th.getTransformer().setParameter("title", title);
		th.setResult( new StreamResult(out));
		ds.writeDataset2( th, dict, null, Integer.MAX_VALUE, null);
		out.flush();
	} else {
%>
<html>
<head>
	<title>DICOM View: failed</title>
	<link href="dcm_style.css" rel="stylesheet" type="text/css">
</head>
<body>
	<h1>
		No Dataset object stored in session attribute 'dataset2view' !
	</h1>
</body>
</html>
<% } %>
