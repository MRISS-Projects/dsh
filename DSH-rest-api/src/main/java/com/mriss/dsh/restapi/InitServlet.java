package com.mriss.dsh.restapi;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class InitServlet extends HttpServlet {
	
	private static final long serialVersionUID = 2492719820082546231L;

	@Override
	public void init() throws ServletException {
		VersionUtils.getInstance().updateVersionProperties(this.getServletContext().getRealPath(""));
	}
	
}

