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
package net.bluemind.gwtconsoleapp.base.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;

import net.bluemind.ui.extensions.gwt.UIExtension;
import net.bluemind.ui.extensions.gwt.UIExtensionPoint;
import net.bluemind.ui.extensions.gwt.UIExtensionsManager;

public class ScreenElements {

	static {
		BasePlugin.install();
	}

	private Map<String, ScreenElement> elementsIndex = new HashMap<String, ScreenElement>();

	public ScreenElements(String extensionPointId) {
		buildScreenElements(extensionPointId);
	}

	public ScreenElement getElement(String elementId) {
		GWT.log("index " + elementsIndex.keySet());
		return elementsIndex.get(elementId);
	}

	public void register(ScreenElement elt) {
		GWT.log("register elt " + elt.getId());
		elementsIndex.put(elt.getId(), elt);
		GWT.log("index " + elementsIndex.keySet());
	}

	public Set<String> screens() {
		return elementsIndex.keySet();
	}

	private void buildScreenElements(String extensionPointId) {
		List<ScreenElementContributor> contributors = getContributors(extensionPointId);

		List<ScreenElementContribution> contributions = new ArrayList<>();
		for (ScreenElementContributor contributor : contributors) {
			JsArray<ScreenElementContribution> c = contributor.contribution();
			for (int i = 0; i < c.length(); i++) {
				ScreenElementContribution contrib = c.get(i);
				contributions.add(contrib);
			}
		}
		int last = 0;

		GWT.log("build screens...");
		while (!contributions.isEmpty() && last != contributions.size()) {
			GWT.log("build screens contrib " + contributions.size() + " last " + last);
			last = contributions.size();
			for (Iterator<ScreenElementContribution> it = contributions.iterator(); it.hasNext();) {
				ScreenElementContribution contribution = it.next();
				if (contribution.getContributedElementId() == null) {
					// root
					ScreenElement e = contribution.getContribution();
					GWT.log("contribute to root " + e.getId() + " [" + e.getType() + "]");
					registerScreenElement(e);
					it.remove();
				} else {
					ScreenElement contributed = elementsIndex.get(contribution.getContributedElementId());
					GWT.log("********contribute to ************" + contribution.getContributedElementId());
					if (contributed != null) {
						ScreenElement c = contribution.getContribution();
						ScreenElement.contribute(contributed, contribution.getContributedAttribute(), c);
						registerScreenElement(c);
						it.remove();
					} else {
						GWT.log("contribute element " + contribution.getContributedElementId()
								+ ", maybe resolve it on next round");
					}
				}
			}
		}

		GWT.log("build screens completed.");
	}

	private void registerScreenElement(ScreenElement e) {
		elementsIndex.put(e.getId(), e);
		// DIRTY FIX
		exploreObjectToFindId(new JSONObject(e));
	}

	private void exploreObjectToFindId(JSONObject jsonObject) {
		if (jsonObject.containsKey("id") && jsonObject.get("id") != null && jsonObject.get("id").isString() != null) {
			String id = jsonObject.get("id").isString().stringValue();
			GWT.log("dirty and screenElement " + id);
			ScreenElement e = jsonObject.getJavaScriptObject().<ScreenElement> cast();
			elementsIndex.put(id, e);
		}
		for (String k : jsonObject.keySet()) {
			if (jsonObject.get(k) == null) {
				continue;
			}

			if (k.endsWith("_")) {
				continue;
			}
			if (jsonObject.get(k).isObject() != null) {
				exploreObjectToFindId(jsonObject.get(k).isObject());
			} else if (jsonObject.get(k).isArray() != null) {
				JSONArray arr = jsonObject.get(k).isArray();
				for (int i = 0; i < arr.size(); i++) {
					JSONValue value = arr.get(i);
					if (value.isObject() != null) {
						exploreObjectToFindId(value.isObject());
					}
				}
			}
		}

	}

	private static List<ScreenElementContributor> getContributors(String extensionPointId) {
		UIExtensionsManager manager = new UIExtensionsManager();
		UIExtensionPoint ep = manager.getExtensionPoint(extensionPointId);

		List<ScreenElementContributor> ret = new ArrayList<>();
		for (UIExtension e : ep.getExtensions()) {
			String func = e.getConfigurationElements("contributor")[0].getAttribute("function");
			ScreenElementContributor contributor;
			try {
				contributor = call(func);
				ret.add(contributor);
			} catch (Exception e1) {
				GWT.log("error loading contrubtor [func:" + func + "] : ", e1);
			}

		}

		return ret;
	}

	private native static ScreenElementContributor call(String func) throws Exception
	/*-{
		return $wnd[func].apply();
	}-*/;

}
