import java.io.File;

import jobs.BuildBob;
import jobs.BuildDepot;
import jobs.BuildMaths;
import jobs.BuildNLP;
import jobs.BuildStat;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;
import winterwell.bob.BuildTask;
import winterwell.bob.tasks.GitTask;
import winterwell.bob.tasks.JarTask;
import winterwell.bob.tasks.RSyncTask;

import com.sodash.web.PoetryServer;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.time.Time;

/**
 * Run via tmux with 
 * 
 * java -cp poetry-server.jar:lib/* com.sodash.web.PoetryServer
 * or
 * java -cp poetry-server.jar:bin:lib/*:~/sodash/web/WEB-INF/lib/* com.sodash.web.PoetryServer
 * (not sure which will work)
 * 
 * @author daniel
 *
 */
public class PublishPoetryServer extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		BuildWinterwellProject[] projects = new BuildWinterwellProject[]{ 
			new BuildUtils(),
			new BuildWeb(),
			new BuildMaths(),
			new BuildNLP(),
			new BuildDepot(),
			new BuildBob(),
			new BuildStat(),
			new BuildSens()
		};
		for (BuildWinterwellProject bwp : projects) {
			File jar = bwp.getJar();
//			if ( ! jar.isFile()) 
			bwp.run();
			FileUtils.copy(jar, new File("lib"));
		}
		
		File projectDir = FileUtils.getWorkingDirectory();
		assert new File(projectDir, "web/static").isDirectory() : projectDir.getAbsolutePath();
		// Jar
		File jarFile = new File(projectDir, "poetry-server.jar");
		JarTask jar = new JarTask(jarFile, new File(projectDir, "bin"));
		jar.setManifestProperty(jar.MANIFEST_MAIN_CLASS, PoetryServer.class.getName());
		jar.setAppend(false);
		// Version = date Is this good or bogus?
		Time vt = new Time();
		jar.setManifestProperty(JarTask.MANIFEST_IMPLEMENTATION_VERSION, vt.ddMMyyyy());
		jar.run();
		
		// NB: use tmux on the server to go into a persistent screen
		
		RSyncTask rsync = new RSyncTask(projectDir.getAbsolutePath()+"/", 
				"winterwell@socrash.soda.sh:~/poetry-server", false);
		rsync.run();
	}
}
