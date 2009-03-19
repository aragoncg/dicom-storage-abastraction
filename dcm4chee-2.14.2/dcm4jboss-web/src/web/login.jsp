<%--
	$Id: login.jsp 3384 2007-06-13 12:27:53Z javawilli $
	$Source$
	
	Autologin for WebViewer:
	To allow external Webviewer access without login you can use request parameter to achieve autologin.
	1) accNr (Accession number) not null! 
	2) loginuser (user name)
	3) passwd4user (password for username in Base64)
	Be aware that this critical infos are visible in browser window!
	Therefore this user should only have WebUser role.
--%>
<%@ page import='org.dcm4che.util.Base64' %>
<%@ page import='java.net.InetAddress' %>
<html>
<head>
  <title>Login</title>
  <link href="style.css" rel="stylesheet" type="text/css">
</head>

<%
	String nodeInfo = System.getProperty("dcm4che.archive.nodename", InetAddress.getLocalHost().getHostName() );
%>
<body onload="self.focus();document.login.j_username.focus()">
<table border="0" cellspacing="0" cellpadding="0" width="100%">
 <tr>
  <td><img src="white48.jpg" width="100%" height="5px"></td>
 </tr>
 <tr>
  <td background="white48.jpg">
    <img src="white48.jpg" width="10px" height="24px"><img src="logo.gif" alt="DCM4CHEE">
  </td>
 </tr>
 <tr>
  <td><img src="line.jpg" width="100%" height="20px" alt="line"></td>
 </tr>
</table>
<center>
<h1>User Login at <%= nodeInfo %></h1>
<br>

<form name="login" method="POST" action="j_security_check">
<table>
	<tr valign="middle">
	  <td><div class="text">Name:</div></td>
	  <td><input class="textfield" type="text" name="j_username" value=""/></td>
	</tr>
	<tr valign="middle">
	  <td><div class="text">Password:</div></td>
	  <td><input class="textfield" type="password" name="j_password" value=""/></td>
	</tr>
	<tr><td>&nbsp;</td></tr>
	<tr valign="middle">
	  <td>&nbsp;</td>
	  <td align="center"><input class="button" type="submit" value="Log in"></td>
	</tr>
</table>
</center>
</form>
</body>
</html>
