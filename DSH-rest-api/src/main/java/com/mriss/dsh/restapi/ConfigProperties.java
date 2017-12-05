package com.mriss.dsh.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {

	private static ConfigProperties instance;
	private static Object lock = new Object();
	Properties version;

	private ConfigProperties() {
		version = new Properties();
		InputStream resourceAsStream = null;
		try {
			resourceAsStream = this.getClass().getClassLoader().getResourceAsStream("version.properties");
			version.load(resourceAsStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (resourceAsStream != null) resourceAsStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static ConfigProperties getInstance() {
		synchronized (lock) {
			if (instance == null) {
				System.out.println("ConfigProperties.getInstance");
				instance = new ConfigProperties();
			}
			return instance;
		}	
	}
	
	public String getVersion() {
		return version.getProperty("version") + "-" + version.getProperty("jenkins.build.number") + "-" + version.getProperty("timestamp");
	}

}
