package com.sodash.web;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.HaikuGenerator;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;

import winterwell.utils.Proc;
import winterwell.utils.Utils;

import com.winterwell.utils.io.FileUtils;

import winterwell.utils.containers.Containers;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.web.ajax.JsonResponse;
import winterwell.web.app.WebRequest;

import com.winterwell.utils.web.WebUtils2;

/**
 * A dummy servlet to demonstrate running a command.
 * 
 * Try: http://localhost:8300/cmd/ls.json?dir=web/static
 * @author daniel
 *
 */
public class HaikuServlet implements IServlet {

	private static boolean initFlag;
	private static LanguageModel languageModel;
	private static Haiku[] haikus;

	static void init() {
		if (initFlag) return;
		initFlag = true;
		haikus = HaikuMain.loadHaikus();
		languageModel = HaikuMain.loadCorpus();
	}
	
	public HaikuServlet() {
		init();
	}
	
	@Override
	public void doPost(WebRequest webRequest) throws Exception {
		
		String topic = webRequest.get("topic");
		int constraint[] = {5,7,5};

		HaikuGenerator generator = new HaikuGenerator(languageModel, haikus, constraint);
		ArrayList<Haiku> candidates = new ArrayList<Haiku>();
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
		}

		System.out.print("Creating Haiku");
		for (int i=0;i<=100;i++){			
			Haiku res = generator.generate(topic);			
			if (res==null) continue;
			res.setTopics(topic);
			candidates.add(res);
		}
				
		Collections.sort(candidates);
		
		List<Haiku> candidates10 = Containers.subList(candidates, 0, 10);
		
		JsonResponse out = new JsonResponse(webRequest, candidates10);
		WebUtils2.sendJson(out, webRequest);
	}

}
