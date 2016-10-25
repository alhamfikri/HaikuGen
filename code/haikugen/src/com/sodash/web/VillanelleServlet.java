package com.sodash.web;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;

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
 
 *
 */
public class VillanelleServlet implements IServlet {

	private static boolean initFlag;
	private static LanguageModel languageModel;

	static void init() {
		if (initFlag) return;
		synchronized (VillanelleServlet.class) {
			if (initFlag) return;			
			languageModel = LanguageModel.get();
			initFlag = true;
		}
	}
	
	public VillanelleServlet() {
		init();
	}
	
	@Override
	public void doPost(WebRequest webRequest) throws Exception {
		assert initFlag : "not init!?";
		assert languageModel!=null : "no model?!";
		String topic = webRequest.get("topic");
		int constraint[] = {5,7,5}; // TODO

		ArrayList<Haiku> candidates = new ArrayList<Haiku>();
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
		}

		Log.d("haiku", "Creating Haiku topic:"+topic);
		for (int i=0;i<=100;i++){			
//			candidates.add(res);
		}
				
		Collections.sort(candidates);
		
		List<Haiku> candidates10 = Containers.subList(candidates, 0, 10);
		Log.d("poetry", "Sending top 10 results");
		JsonResponse out = new JsonResponse(webRequest, candidates10);
		WebUtils2.sendJson(out, webRequest);
	}

}
