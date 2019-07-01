/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.ui.adminconsole.base;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;

import net.bluemind.ui.adminconsole.base.ui.AppScreen;
import net.bluemind.ui.adminconsole.base.ui.ScreenContext;
import net.bluemind.ui.adminconsole.base.ui.ScreenShowRequest;

public class Actions {

	private static Actions inst = new Actions();

	public Actions() {
		History.addValueChangeHandler(new ValueChangeHandler<String>() {

			@Override
			public void onValueChange(ValueChangeEvent<String> event) {
				updateLocation();
			}
		});

	}

	public static Actions get() {
		return inst;
	}

	private Map<String, IWebAction> webActions = new HashMap<>();

	private Map<String, ScreenAction> actions = new HashMap<String, ScreenAction>();

	public HistToken getHistoryToken() {
		String history = History.getToken();
		HistToken token = new HistToken();
		token.screen = getHistoryTokenAnchor(history);
		token.req = getHistoryTokenParameters(history);
		return token;
	}

	private String getHistoryTokenAnchor(String historyToken) {
		// skip if there is no question mark
		if (!historyToken.contains("?")) {
			return historyToken;
		}

		// get just the historyToken/anchor tag
		String[] arStr = historyToken.split("\\?");
		String ht = arStr[0];

		GWT.log("getHT_Anchor " + historyToken + " => " + ht);

		return ht;
	}

	/**
	 * get historyToken parameters
	 * 
	 * like domain.tld#historyToken?[?params1=a&params2=b]
	 * 
	 * @param historyToken
	 *            (anchor tag)
	 * @return HashMap of the parameters ([varName, var] OR [key, value])
	 */
	private Map<String, String> getHistoryTokenParameters(String historyToken) {

		// skip if there is no question mark
		if (!historyToken.contains("?")) {
			return new HashMap<String, String>();
		}
		int questionMarkIndex = historyToken.indexOf("?") + 1;

		// get the sub string of parameters var=1&var2=2&var3=3...
		// params1=a&params2=b...
		String[] arStr = historyToken.substring(questionMarkIndex, historyToken.length()).split("&");
		HashMap<String, String> params = new HashMap<String, String>();
		for (int i = 0; i < arStr.length; i++) {
			String[] substr = arStr[i].split("=");
			params.put(substr[0], substr[1]);
		}

		return params;
	}

	public void registerAction(String actionId, IWebAction action) {
		GWT.log("register actionId " + actionId);
		webActions.put(actionId, action);
	}

	private void doAction(String actionId, String path, Map<String, String> params) {
		IWebAction action = webActions.get(actionId);
		if (action == null) {
			GWT.log("unknown action '" + actionId + "'");
		} else {
			action.run(path, params);
		}
	}

	public void showWithParams2(String scr, Map<String, String> req) {
		GWT.log("newItem: " + scr + ", params: " + req);

		if (req == null) {
			req = new HashMap<>();
		}
		String token = scr + "?";
		for (Entry<String, String> entry : req.entrySet()) {
			token += entry.getKey() + "=" + entry.getValue() + "&";
		}
		History.newItem(token);
	}

	public void show(String scr, ScreenShowRequest ssr) {
		new Exception().printStackTrace();
		GWT.log("newItem: " + scr);
		History.newItem(scr);
	}

	public AppScreen screen(String action, ScreenContext sc) {
		ScreenAction sectionAction = actions.get(action);
		if (sectionAction == null) {
			return null;
		}

		return sectionAction.appScreen(sc);
	}

	public void cancelLast() {

	}

	public boolean isKnown(String s) {
		return webActions.containsKey(s);
	}

	public void updateLocation() {

		HistToken hist = Actions.get().getHistoryToken();
		if (hist.screen == null || hist.screen.isEmpty()) {
			hist.screen = "root";
		}
		doAction(hist.screen, hist.screen, hist.req);

	}

	public void reload() {
		updateLocation();
	}
}
