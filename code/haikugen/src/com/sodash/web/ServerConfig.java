package com.sodash.web;

import java.io.File;

import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.Option;


/**
 * These values are often replaced by the ones in config/ServerConfig.properties
 */
public class ServerConfig {

	@Option
	public int port = 8642;
	
	@Option
	public File webAppDir = FileUtils.getWorkingDirectory();	

	@Option
	public String baseUrl = "http://localhost:"+port;
	
}
