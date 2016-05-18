import java.io.File;

import jobs.BuildDepot;
import jobs.BuildMaths;
import jobs.BuildNLP;
import jobs.BuildUtils;
import jobs.BuildWeb;
import jobs.BuildWinterwellProject;
import winterwell.bob.BuildTask;
import winterwell.bob.tasks.GitTask;
import winterwell.bob.tasks.JarTask;
import winterwell.bob.tasks.RSyncTask;

import com.sodash.web.PoetryServer;
import com.winterwell.utils.io.FileUtils;

import winterwell.utils.time.Time;


public class PublishPoetryServer extends BuildTask {

	@Override
	protected void doTask() throws Exception {
		BuildWinterwellProject[] projects = new BuildWinterwellProject[]{ 
			new BuildUtils(),
			new BuildWeb(),
			new BuildMaths(),
			new BuildNLP(),
			new BuildDepot()
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
		// run with  
		// java -cp simple-server.jar:web/WEB-INF/lib/* org.coinvent.SimpleServe
		
		RSyncTask rsync = new RSyncTask(projectDir.getAbsolutePath()+"/", 
				"winterwell@socrash.soda.sh:~/poetry-server", false);
		rsync.run();
	}
}
