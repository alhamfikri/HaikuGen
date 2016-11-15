package com.sodash.web;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.coinvent.haiku.Poem;
import org.eclipse.jetty.util.ajax.JSON;

import winterwell.jtwitter.AStream;
import winterwell.jtwitter.OAuthSignpostClient;
import winterwell.jtwitter.Twitter;
import winterwell.jtwitter.TwitterEvent;
import winterwell.jtwitter.TwitterTest;
import winterwell.jtwitter.UserStream;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.utils.reporting.Log;
import winterwell.utils.time.Dt;
import winterwell.utils.time.TUnit;
import winterwell.utils.time.Time;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.Utils;
import com.winterwell.utils.io.CSVReader;
import com.winterwell.utils.io.CSVWriter;
import com.winterwell.utils.io.FileUtils;
import com.winterwell.utils.threads.Actor;
import com.winterwell.utils.web.SimpleJson;
import com.winterwell.utils.web.XStreamUtils;
import com.winterwell.web.FakeBrowser;

import creole.data.XId;

public class ListenForRequests {
	

	static final String TOKEN0 = "3831720977-QdWgaVXbum32MDijwcMWGk3u2LoBFgSIDZSKHTr"; 
	static final String TOKEN1 = "rRxK8e6Om4HTiJvNJR1rhkeFhUj4nrC3VATy4CXuCW9W7";
	
	static Twitter jtwit = new Twitter("aihaiku", 
			new OAuthSignpostClient(
					OAuthSignpostClient.JTWITTER_OAUTH_KEY,
					OAuthSignpostClient.JTWITTER_OAUTH_SECRET,
					TOKEN0, TOKEN1));
	
	Actor actor = new ListenActor();
	
	Timer time = new Timer();
	private UserStream userStream;
	
	public void start() {
		// listen to @sodash #poem
//		time.schedule(new ListenTask(actor), new Dt(1,TUnit.MINUTE).getMillisecs(), new Dt(1,TUnit.MINUTE).getMillisecs());
		
		// listen to @aihaiku
		userStream = new UserStream(jtwit);
		userStream.setAutoReconnect(true);
		userStream.addListener(new AStream.IListen() {			
			@Override
			public boolean processTweet(ITweet tweet) {
				Log.d("haiku", "Got tweet to @aihaiku: "+tweet);
				if (tweet.getText().startsWith("RT ")) return true;
				if ( ! tweet.getText().toLowerCase().contains("@aihaiku")) return true;
				actor.send(tweet);
				return true;
			}
			
			@Override
			public boolean processSystemEvent(Object[] obj) {
				return true;
			}
			
			@Override
			public boolean processEvent(TwitterEvent event) {
				return true;
			}
		});
		userStream.connect();
	}

}

class ListenActor extends Actor<Object> {

	HashSet<String> msgsRepliedTo = new HashSet();
	File f = new File("msgsRepliedTo.csv");
	
	public ListenActor() {
		// HACK		
		if (f.isFile()) {
			CSVReader r = new CSVReader(f, '\t');
			for (String[] row : r) {
				msgsRepliedTo.add(row[0].trim());	
			}	
			r.close();
		}
	}
	
	@Override
	protected void receive(Object msg, Actor sender) {
		if (msg instanceof Map) {
			Map m = (Map) msg;
			String text = (String) m.get("contents");
			if (text==null || text.startsWith("RT ") || ! text.toLowerCase().contains("poem")) {
				return;
			}
			// A poem request!
			String inxid = (String) m.get("xid");
			if (msgsRepliedTo.contains(inxid)) {
				return;
			}
			Log.d("listen", "process poem request "+msg);
			HaikuServlet hs = new HaikuServlet();
			String topic = text.toLowerCase().replaceAll("@sodash", "");
			topic = topic.replaceAll("#?poem", "");
			topic = StrUtils.toCanonical(topic);
			if (Utils.isBlank(topic)) topic = null;
			String topic2 = null;
			XId tweep = XId.xid(SimpleJson.get(m, "owner", "xid"));
			List<Poem> poems = hs.doWritePoem(topic, topic2, tweep, false);
			Poem poem = poems.get(0);
			if (tweep!=null) {
				String txt = "#poem for @"+tweep.getName()+":\n"+poem;
				if (txt.length() > 140) txt = "@"+tweep.getName()+": "+poem;
				ListenForRequests.jtwit.setStatus(txt);
				msgsRepliedTo.add(inxid);
				CSVWriter w = new CSVWriter(f, '\t', true);
				w.write(inxid, poem, new Time());
				w.close();
			}
		}
	}
	
}
class ListenTask extends TimerTask {

	private Actor actor;

	public ListenTask(Actor actor) {
		this.actor = actor;
	}

	@Override
	public void run() {
		// fetch from sales.soda.sh
		try {
			FakeBrowser fb = new FakeBrowser();
			String json = fb.getPage("http://sales.soda.sh/inbound.json?to=sodash@twitter&urisig=fd9628e2aff554a92ca124db5fcc974d&as=sodash%40twitter");
			Map jobj = (Map) JSON.parse(json);
			Object cargo = jobj.get("cargo");
			Object[] msgs = (Object[]) cargo;
			for (Object msg : msgs) {
				if (msg instanceof Map) {
					Map m = (Map) msg;
					String text = (String) m.get("contents");
					if (text==null || text.startsWith("RT ") || ! text.toLowerCase().contains("poem")) {
						continue;
					}
					actor.send(msg);
				}
			}
//			Log.d("ListenTask", "ran, found "+msgs.length);							
		} catch(Throwable ex) {
			Log.e("ListenTask", ex);
		}
	}
	
}
