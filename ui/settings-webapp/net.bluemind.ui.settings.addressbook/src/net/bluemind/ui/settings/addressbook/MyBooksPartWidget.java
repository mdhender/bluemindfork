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
package net.bluemind.ui.settings.addressbook;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.gwtuser.client.UserBooksSharingModelHandler;
import net.bluemind.ui.settings.addressbook.management.BookManagement;

public class MyBooksPartWidget extends CompositeGwtWidgetElement {

	private JavaScriptObject model;

	public MyBooksPartWidget() {
		initWidget(new BookManagement(Notification.get()).asWidget());

	}

	@Override
	public void attach(Element parent) {
		// TODO Auto-generated method stub
		super.attach(parent);
		Event.setEventListener(getElement(), new EventListener() {

			@Override
			public void onBrowserEvent(Event event) {
				UserBooksSharingModelHandler modelHandler = new UserBooksSharingModelHandler();
				modelHandler.reload(model, new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						getElement().dispatchEvent(Document.get().createHtmlEvent("refresh", true, true));
					}
				});

			}
		});

		DOM.sinkBitlessEvent(getElement(), "refresh-container");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		super.loadModel(model);
		this.model = model;
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		super.saveModel(model);
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.settings.MyBooksEditor",
				new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

					@Override
					public IGwtWidgetElement create(WidgetElement e) {
						return new MyBooksPartWidget();
					}
				});

	}
}
