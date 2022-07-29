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
package net.bluemind.ui.adminconsole.base.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.Actions;

public abstract class BaseEditScreen extends Composite implements IGwtCompositeScreenRoot {

	interface BaseEditScreenUiBinder extends UiBinder<DockLayoutPanel, BaseEditScreen> {

	}

	private BaseEditScreenUiBinder binder = GWT.create(BaseEditScreenUiBinder.class);

	@UiField
	CrudActionBar actionBar;

	@UiField
	protected SpanElement title;

	@UiField
	protected Label icon;

	@UiField
	protected SimplePanel center;

	private ScreenRoot instance;

	protected BaseEditScreen(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		DockLayoutPanel dlp = binder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);

		actionBar.setSaveAction(this::saveClicked);
		actionBar.setCancelAction(this::doCancel);
	}

	protected abstract void doCancel();

	private void saveClicked() {
		instance.save(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				Notification.get().reportInfo("saved");
				Actions.get().reload();
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
			}
		});
	}

	@Override
	public Element getCenter() {
		return center.getElement();
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();

		Event.setEventListener(getElement(), new EventListener() {

			@Override
			public void onBrowserEvent(Event event) {
				instance.saveModel(instance.getModel());
				instance.loadModel(instance.getModel());
			}
		});

		DOM.sinkBitlessEvent(getElement(), "refresh");

	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	@Override
	public void doLoad(ScreenRoot screenRoot) {
		instance.load(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}

			@Override
			public void failure(Throwable e) {
				GWT.log("load failure", e);
				Notification.get().reportError(e);
			}
		});
	}

}
