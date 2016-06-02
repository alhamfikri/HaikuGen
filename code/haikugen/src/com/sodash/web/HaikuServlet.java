package com.sodash.web;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.PoemGenerator;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;
import org.coinvent.haiku.Poem;

import com.winterwell.utils.Proc;

import winterwell.utils.Utils;

import com.winterwell.utils.io.FileUtils;

import winterwell.utils.containers.Containers;
import winterwell.utils.reporting.Log;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.web.app.WebRequest;

import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;

/**
  @author daniel
 *
 */
public class HaikuServlet implements IServlet {

	private static boolean initFlag;
	private static LanguageModel languageModel;
	private static List<Haiku> haikus;

	static void init() {
		if (initFlag) return;
		synchronized (HaikuServlet.class) {
			if (initFlag) return;
			haikus = HaikuMain.loadHaikus();
			languageModel = LanguageModel.get();
			initFlag = true;
		}
	}
	
	public HaikuServlet() {
		init();
	}
	
	@Override
	public void doPost(WebRequest webRequest) throws Exception {
		assert initFlag : "not init!?";
		assert languageModel!=null : "no model?!";
		String topic = webRequest.get("topic");
		String topic2 = webRequest.get("topic2");
		int constraint[] = {5,7,5};

		PoemGenerator generator = new PoemGenerator(languageModel, haikus, constraint);
		ArrayList<Poem> candidates = new ArrayList<>();
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
		}
//		if (topic2!=null) topic = topic+" "+topic2;

		Log.d("haiku", "Creating Haiku topic:"+topic+" "+topic2);
		for (int i=0;i<=100;i++){			
			Poem res = generator.generate(topic, topic2);			
			if (res==null) continue;
			res.setTopics(topic);
			candidates.add(res);
			Log.d("haiku", "..."+res);
		}
				
		Collections.sort(candidates);
		
		List<Poem> candidates10 = Containers.subList(candidates, 0, 10);
		Log.d("haiku", "Sending top 10 results");
		JsonResponse out = new JsonResponse(webRequest, candidates10);
		WebUtils2.sendJson(out, webRequest);
	}

}
