<%@page import="com.mriss.dsh.restapi.ConfigProperties"%>
<html>
<body>
<h2>Hello World!</h2>
<p><%=ConfigProperties.getInstance().getVersion() %></p>
</body>
</html>
