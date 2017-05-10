package org.coinvent.haiku;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import com.winterwell.utils.Utils;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.io.LineReader;
import com.winterwell.utils.log.Log;


public class HaikuMain {

	
	public static List<Haiku> loadHaikus() {		
		File currentDirectory = FileUtils.getWorkingDirectory();
		File fhaikus = new File(currentDirectory, "res/model/haiku");
		List<Haiku> haikus = new ArrayList();
		LineReader lr = new LineReader(fhaikus);
		for (String line : lr) {
			if (Utils.isBlank(line)) continue;
			haikus.add(new Haiku(line));
		}
		lr.close();
		Log.d("haikus", "loaded corpus of "+haikus.size()+" haikus");
		return haikus;
	}

}