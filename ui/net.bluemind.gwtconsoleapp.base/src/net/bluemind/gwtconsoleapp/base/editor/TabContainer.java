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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;

public class TabContainer extends WidgetElement {

	public static class TabContainerWidget extends TabLayoutPanel {

		private TabContainer container;

		public TabContainerWidget(TabContainer container, double barHeight, Unit barUnit) {
			super(barHeight, barUnit);
			this.container = container;
			addSelectionHandler(new SelectionHandler<Integer>() {

				@Override
				public void onSelection(SelectionEvent<Integer> event) {
					TabContainerWidget.this.container.getTabs().get(event.getSelectedItem()).show();
				}
			});
		}

		public void attach(Element parent) {
			parent.appendChild(getElement());
			onAttach();
		}
	}

	protected TabContainer() {
	}

	public final native JsArray<Tab> getTabs()
	/*-{
		return this["tabs"];
	}-*/;

	public static void registerType() {
		GWT.log("bm.Tabs registred");
		JsHelper.createPackage("bm");
		rt();
	}

	private static native void rt()
	/*-{
		if (!$wnd.bm) {
			$wnd['bm'] = {};
		}
	
		$wnd.bm.Tabs = function(model, context) {
			$wnd.bm.WidgetElement.call(this, model, context);
			if (model) {
				if (model['tabs']) {
					this['tabs'] = []
					for (var i = 0; i < model['tabs'].length; i++) {
						this['tabs'].push(new $wnd.bm.Tab(model['tabs'][i], context));
					}
				}
			}
		}
	
		$wnd.bm.Tabs.prototype = new $wnd.bm.WidgetElement();
		$wnd.bm.Tabs.prototype.attach = function(parent) {
			@net.bluemind.gwtconsoleapp.base.editor.TabContainer::attachImpl(Lnet/bluemind/gwtconsoleapp/base/editor/TabContainer;Lcom/google/gwt/dom/client/Element;)(this, parent);
		};
	
		$wnd.bm.Tabs.contribute = function(elt, attribute, contribution) {
			@net.bluemind.gwtconsoleapp.base.editor.TabContainer::contribute(Lnet/bluemind/gwtconsoleapp/base/editor/TabContainer;Ljava/lang/String;Lnet/bluemind/gwtconsoleapp/base/editor/ScreenElement;)(elt,attribute,contribution);
		};
	
		$wnd.bm.Tabs.prototype.saveModel = function(model) {
			@net.bluemind.gwtconsoleapp.base.editor.TabContainer::saveModelImpl(Lnet/bluemind/gwtconsoleapp/base/editor/TabContainer;Lcom/google/gwt/core/client/JavaScriptObject;)(this,model);
		};
		$wnd.bm.Tabs.prototype.loadModel = function(model) {
			@net.bluemind.gwtconsoleapp.base.editor.TabContainer::loadModelImpl(Lnet/bluemind/gwtconsoleapp/base/editor/TabContainer;Lcom/google/gwt/core/client/JavaScriptObject;)(this,model);
		};
	}-*/;

	protected static void saveModelImpl(TabContainer tabContainer, JavaScriptObject model) {
		JsArray<Tab> tabs = tabContainer.getTabs();
		for (int i = 0; i < tabs.length(); i++) {
			Tab tab = tabs.get(i);
			tab.saveModel(model);
		}

	}

	protected static void loadModelImpl(TabContainer tabContainer, JavaScriptObject model) {
		GWT.log("TabContainer load model");
		JsArray<Tab> tabs = tabContainer.getTabs();
		for (int i = 0; i < tabs.length(); i++) {
			Tab tab = tabs.get(i);
			tab.loadModel(model);
		}
	}

	protected static void attachImpl(TabContainer tabContainer, Element parent) {
		final TabContainerWidget panel = new TabContainerWidget(tabContainer, 25, Unit.PX);

		panel.setHeight("100%");
		panel.setWidth("100%");
		panel.attach(parent);

		JsArray<Tab> tabs = tabContainer.getTabs();
		for (int i = 0; i < tabs.length(); i++) {
			Tab tab = tabs.get(i);
			ScrollPanel l = new ScrollPanel();
			panel.add(l, tab.getTitle());
			tab.attach(l.getElement());
		}

	}

	private static void contribute(TabContainer tabContainer, String attribute, ScreenElement contribution) {

		if ("tabs".equals(attribute) || attribute == null) {
			Tab tab = contribution.castElement("Tab");
			tabContainer.getTabs().push(tab);
		}

	}

	public static native TabContainer create(String id, JsArray<Tab> tabs)
	/*-{
		return {
			'id' : id,
			'type' : 'bm.Tabs',
			'tabs' : tabs
		};
	}-*/;
}
