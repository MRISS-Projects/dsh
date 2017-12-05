package com.mriss.dsh.restapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class VersionUtils {

	private static VersionUtils INSTANCE;
	
	private static String VERSION_RELATIVE_PATH = "WEB-INF"+ File.separator + "classes" + File.separator + "version.properties";
	
	private VersionUtils() {
		
	}
	
	public static VersionUtils getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new VersionUtils();
		}
		return INSTANCE;
	}

	public void updateVersionProperties(String contextPath) {
		InputStream is = null;
		Properties p = new Properties();
		try {
			is = new FileInputStream(new File(contextPath + File.separator + VERSION_RELATIVE_PATH));
			p.load(is);
			String buildNumber = p.getProperty("jenkins.build.number");
			if (buildNumber != null && !"".equals(buildNumber.trim())) {
				String newVersion = removeSnapshotSuffix(p.getProperty("version"));
				String newTimeStamp = getNewTimeStamp();
				writeNewVersionInformation(contextPath, newVersion, newTimeStamp, buildNumber, p);
				//ConfigProperties.getInstance().reloadVersionInfo();
				//log.info("version: " + ConfigProperties.getInstance().getVersion());
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}				
			}
		}
	}

	private void writeNewVersionInformation(String contextPath,
			String newVersion, String newTimeStamp, String buildNumber, Properties p) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(new File(contextPath + File.separator + VERSION_RELATIVE_PATH));
			p.setProperty("version", newVersion);
			p.setProperty("timestamp", newTimeStamp);
			p.setProperty("jenkins.build.number", buildNumber);
			p.store(fos, "Adapting version.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (fos != null) {
				try {
					fos.flush();
					fos.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}		
	}

	private String getNewTimeStamp() {
		DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
		return format.format(new Date(System.currentTimeMillis()));
	}

	private String removeSnapshotSuffix(String property) {
		if (property.indexOf("-SNAPSHOT") != -1)
			return property.substring(0, property.indexOf("-SNAPSHOT"));
		else
			return property;
	}
	
}
