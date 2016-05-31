package com.sodash.web;

import java.io.File;
import java.util.logging.Level;

import org.coinvent.haiku.LanguageModel;

import winterwell.utils.gui.GuiUtils;
import winterwell.utils.reporting.LogFile;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.web.app.FileServlet;
import winterwell.web.app.JettyLauncher;

import com.winterwell.utils.Utils;
import com.winterwell.utils.io.ArgsParser;
import com.winterwell.utils.log.Log;
import com.winterwell.utils.web.WebUtils;

/**
 * Run this! It starts up a web-server.
 * 
 * java -Xmx3g -cp poetry-server.jar:lib/* com.sodash.web.PoetryServer
 * 
 * 
 * @author Daniel
 *
 */
public class PoetryServer {

	private ServerConfig config;
	private JettyLauncher jl;

	public PoetryServer(ServerConfig config) {
		this.config = config;		
	}
	
	static LogFile logFile;
	
	public static PoetryServer app;
	
	public ServerConfig getConfig() {
		return config;
	}

	public static void main(String[] args) {
		// Load config, if we have any
		ServerConfig config = new ServerConfig();
		File props = new File("config/haiku.properties");		
		config = ArgsParser.getConfig(config, args, props, null);
		
		// Log file - if not already set by run()
		assert logFile==null;
		logFile = new LogFile(new File(config.webAppDir, "PoetryServer.log"));
		// keep 14 days of log files
		logFile.setLogRotation(new Dt(24, TUnit.HOUR), 14);
		
		// Run it!
		app = new PoetryServer(config);
		app.run();
		
		// Open test view?
		if (GuiUtils.isInteractive()) {
			Log.i("init", "Open links in local browser...");
			WebUtils.display(WebUtils.URI("http://localhost:"+config.port+"/static/haiku/index.html"));
		}
		// prod data loading, by asking "what is love?"
		LanguageModel lm = LanguageModel.get();
		lm.getVector("love");
	}

	public void run() {
		// Spin up a Jetty server with reflection-based routing to servlets
		Log.d("web", "Setting up web server...");
		jl = new JettyLauncher(config.webAppDir, config.port);
		jl.setWebXmlFile(null);
		jl.setCanShutdown(false);
		jl.setup();
		DynamicHttpServlet dynamicRouter = new DynamicHttpServlet();
		jl.addServlet("/*", dynamicRouter);
		jl.addServlet("/static/*", new FileServlet());
		Log.report("web", "...Launching Jetty web server on port "+config.port, Level.INFO);
		jl.run();
		
		// Put in an index file
		dynamicRouter.putSpecialStaticFile("/", new File("web/index.html"));
	}

	public void stop() {
		try {
			jl.getServer().stop();
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}
	
}

