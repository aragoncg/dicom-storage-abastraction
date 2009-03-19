<%--
	$Id: error.jsp 2613 2006-07-11 13:12:28Z gunterze $
	$Source$
--%>
<html>
<head>
	<title>Login</title>
	<link href="style.css" rel="stylesheet" type="text/css">
</head>
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
<h1>Login Failed!</h1>
<br>
<form name="login" method="POST" action="j_security_check">
<table>
  <tr valign="middle">
    <td><div class="text">Name:</div></td>
    <td><input class="textfield" type="text" name="j_username"/></td>
  </tr>
  <tr valign="middle">
    <td><div class="text">Password:</div></td>
    <td><input class="textfield" type="password" name="j_password"/></td>
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
