package com.sodash.web;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.util.ajax.JSON;

import winterwell.utils.reporting.Log;

import com.winterwell.utils.StrUtils;
import com.winterwell.utils.threads.Actor;
import com.winterwell.web.FakeBrowser;

import creole.data.XId;

public class ListenForRequests {
	
	Actor actor = new ListenActor();
	
	Timer time = new Timer();
	
	public void start() {
		time.schedule(new ListenTask(actor), 10000, 10000);
	}

}

class ListenActor extends Actor<Object> {

	@Override
	protected void receive(Object msg, Actor sender) {
		Log.d("listen", "process poem request "+msg);
		if (msg instanceof Map) {
			Map m = (Map) msg;
			String text = (String) m.get("contents");
			if (text==null || text.startsWith("RT ") || ! text.toLowerCase().contains("poem")) {
				return;
			}
			// A poem request!
			HaikuServlet hs = new HaikuServlet();
			String topic = text.toLowerCase().replaceAll("@sodash", "");
			topic = topic.replaceAll("#?poem", "");
			topic = StrUtils.toCanonical(topic);
			String topic2 = null;
			XId tweep = null;
			hs.doWritePoem(topic, topic2, tweep);			
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
				actor.send(msg);
			}
			Log.d("ListenTask", "ran, found "+msgs.length);							
		} catch(Throwable ex) {
			Log.e("ListenTask", ex);
		}
	}
	
}
