package com.sodash.web;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.coinvent.haiku.Haiku;
import org.coinvent.haiku.PoemGenerator;
import org.coinvent.haiku.HaikuMain;
import org.coinvent.haiku.LanguageModel;
import org.coinvent.haiku.Poem;
import org.coinvent.haiku.PoemVocab;
import org.coinvent.haiku.VocabFromTwitterProfile;

import com.winterwell.utils.Proc;
import com.winterwell.utils.ReflectionUtils;

import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterTest;
import winterwell.maths.stats.distributions.cond.WWModel;
import winterwell.nlp.docmodels.IDocModel;
import winterwell.nlp.io.Tkn;
import winterwell.utils.FailureException;
import winterwell.utils.Utils;

import com.winterwell.utils.io.FileUtils;

import winterwell.utils.containers.Containers;
import winterwell.utils.reporting.Log;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.web.app.WebRequest;
import winterwell.web.fields.Checkbox;

import com.winterwell.utils.web.WebUtils2;
import com.winterwell.web.ajax.JsonResponse;

import creole.data.XId;

/**
  @author daniel
 *
 */
public class HaikuServlet implements IServlet {

	private static final String LOGTAG = "haiku";
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
	
	static List<Poem> recent = new ArrayList();
	
	@Override
	public void doPost(WebRequest webRequest) throws Exception {
		if ( ! PoetryServer.ready) {
			throw new FailureException("Not ready yet...");
		}
		// send the most recent?
		if (webRequest.actionIs("recent")) {
			List<Map> jsoncandidates10 = new ArrayList();
			for (Poem poem : recent) {
				jsoncandidates10.add(poem.toJson2());
			}
			JsonResponse out = new JsonResponse(webRequest, jsoncandidates10);
			WebUtils2.sendJson(out, webRequest);
			return;
		}
		
		assert initFlag : "not init!?";
		assert languageModel!=null : "no model?!";
		String topic = webRequest.get("topic");
		String topic2 = webRequest.get("topic2");
		String tweep = webRequest.get("tweep");
		Boolean rhyme = webRequest.get(new Checkbox("rhyme"));
		XId xid = null;
		if (tweep!=null) {
			tweep = tweep.trim();
			if (tweep.startsWith("@")) {
				tweep = tweep.substring(1);
			}
			xid = new XId(tweep.toLowerCase(), "twitter");
		}
		List<Poem> poems = doWritePoem(topic, topic2, xid, rhyme);
		
		List<Map> jsoncandidates10 = new ArrayList();
		for (Poem poem : poems) {
			jsoncandidates10.add(poem.toJson2());
		}
		Log.d("haiku", "Sending top 10 results");
		JsonResponse out = new JsonResponse(webRequest, jsoncandidates10);
		WebUtils2.sendJson(out, webRequest);
	}
	

	List<Poem> doWritePoem(String topic, String topic2, XId tweep, Boolean rhyme) {
		Log.d(LOGTAG, "doWritePoem "+topic+" "+topic2+" "+tweep+" "+ReflectionUtils.getSomeStack(6));
		int constraint[] = {5,7,5};

		PoemGenerator generator = new PoemGenerator(languageModel, haikus, constraint);

		if (Utils.yes(rhyme)) {
			generator.setRhymeConstraint(Arrays.asList(new int[]{0,1,2}));
		}
		// TODO for a Twitter profile?
		
		Map tweepInfo = null; 
		if (tweep!=null) {			
			Twitter jtwit = Utils.getRandomChoice(0.5)? TwitterTest.newTestTwitter() : TwitterTest.newTestTwitter2();
			VocabFromTwitterProfile vftp = new VocabFromTwitterProfile(jtwit, tweep);
			vftp.run();
			PoemVocab vocab = vftp.getVocab();
			generator.setVocab(vocab);
			WWModel<Tkn> wordGen = vftp.getWordModel();
			generator.setWordGen(wordGen);
			tweepInfo = vftp.getTweepInfo();
		} else {
			PoemVocab vocab = LanguageModel.get().getAllVocab();
			generator.setVocab(vocab);
			WWModel<Tkn> wordGen = LanguageModel.get().getAllWordModel();
			generator.setWordGen(wordGen);
		}

		
		ArrayList<Poem> candidates = new ArrayList<>();
		if (Utils.isBlank(topic)) {
			topic = languageModel.getRandomTopic();
			assert topic != null : languageModel;
		}
//		if (topic2!=null) topic = topic+" "+topic2;

		Log.d("haiku", "Creating Haiku topic:"+topic+" "+topic2);
		for (int i=0;i<=10;i++){			
			Poem res = generator.generate(topic, topic2);			
			if (res==null) continue;
			res.setTopics(topic);
			candidates.add(res);
			Log.d("haiku", "..."+res);
		}
				
		Collections.sort(candidates);
		
		List<Poem> candidates10 = Containers.subList(candidates, 0, 10);
		for (Poem poem : candidates10) {
			poem.setFor(tweepInfo);
		}		
		addRecent(candidates10.get(0));
		return candidates10;
	}

	synchronized void addRecent(Poem poem) {
		recent.add(0, poem);
		// prune
		while(recent.size() > 20) {
			recent.remove(recent.size()-1);
		}
	}

}
