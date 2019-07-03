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
package net.bluemind.ui.settings.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.ui.DeckLayoutPanel;

import net.bluemind.gwtconsoleapp.base.editor.ContainerElement;
import net.bluemind.gwtconsoleapp.base.editor.JsHelper;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement;

public final class RootScreen extends GwtContainerElement {

	private Map<String, SectionScreen> sections = new HashMap<>();

	private DeckLayoutPanel deckPanel;

	private SectionScreen current;

	private SectionScreen lastSection;

	public RootScreen(ContainerElement container) {
		super(container);
		deckPanel = new DeckLayoutPanel();
		deckPanel.setHeight("100%");
		initWidget(deckPanel);
	}

	public static native void show(WidgetElement rootScreen, String id)
	/*-{
		rootScreen.show(id);
	}-*/;

	protected final void showElement(String id) {
		SectionScreen section = sections.get(id);
		if (section == null) {
			GWT.log("unkown screen " + id + " for " + sections.keySet());
		}

		if (lastSection != null && lastSection.isOverlay()) {
			lastSection.hide();
		}
		if (section.isOverlay()) {
			section.show();
		} else {
			deckPanel.showWidget(sections.get(id));
		}

		lastSection = section;
	}

	@Override
	protected void attachChild(WidgetElement widgetElement) {
		SectionScreen sectionScreen = new SectionScreen(widgetElement);
		if (!sectionScreen.isOverlay()) {
			deckPanel.add(sectionScreen);
			sectionScreen.setVisible(false);
			sectionScreen.attach();
		}
		sections.put(widgetElement.getId(), sectionScreen);
	}

	public static void registerType() {
		JavaScriptObject jsPackage = JsHelper.createPackage("bm.settings");
		registerJs(jsPackage);
	}

	private static native void registerJs(JavaScriptObject jsPackage)
	/*-{
		
		jsPackage['RootScreen'] = function(model, ctx) {
			$wnd.bm.ContainerElement.call(this, model, ctx);
			var nat = new @net.bluemind.ui.settings.client.RootScreen::new(Lnet/bluemind/gwtconsoleapp/base/editor/ContainerElement;)(this);
			this["widget_"] = nat;
		}
	
		jsPackage['RootScreen'].prototype = new $wnd.bm.ContainerElement();
	
		jsPackage['RootScreen'].prototype.show = function(id) {
			this["widget_"].@net.bluemind.ui.settings.client.RootScreen::showElement(Ljava/lang/String;)(id);
		};
		
		
		jsPackage['RootScreen'].prototype.attach = function(parent) {
			this["widget_"].@net.bluemind.gwtconsoleapp.base.editor.gwt.GwtContainerElement::attach(Lcom/google/gwt/dom/client/Element;)(parent);
		};
	
		jsPackage['RootScreen'].contribute = function(elt, attribute, contribution) {
			@net.bluemind.gwtconsoleapp.base.editor.ContainerElement::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/ContainerElement;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt,attribute,contribution);
		};
	}-*/;

	public static native ScreenElement create(String id)
	/*-{ 
		return {'id':id, 'type':'bm.settings.RootScreen', 'childrens':[]}; 
	}-*/;

}