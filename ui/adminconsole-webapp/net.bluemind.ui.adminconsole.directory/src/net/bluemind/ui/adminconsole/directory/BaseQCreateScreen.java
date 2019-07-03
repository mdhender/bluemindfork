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
package net.bluemind.ui.adminconsole.directory;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.gwt.serder.ItemValueGwtSerDer;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.serder.DomainGwtSerDer;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.ui.adminconsole.base.DomainsHolder;
import net.bluemind.ui.adminconsole.base.ui.QuickCreateActionBar;

public abstract class BaseQCreateScreen extends Composite implements IGwtCompositeScreenRoot {

	interface BaseQCreateScreenUiBinder extends UiBinder<DockLayoutPanel, BaseQCreateScreen> {
	}

	private static BaseQCreateScreenUiBinder uiBinder = GWT.create(BaseQCreateScreenUiBinder.class);

	@UiField
	QuickCreateActionBar actionBar;

	@UiField
	HTMLPanel center;

	@UiField
	Label errorLabel;

	@UiField
	protected Label icon;

	@UiField
	protected SpanElement title;

	private DockLayoutPanel dlp;

	protected ScreenRoot rootScreen;

	public BaseQCreateScreen(ScreenRoot screen) {
		this(screen, true);
	}

	public BaseQCreateScreen(ScreenRoot screen, boolean createAndEdit) {
		this.rootScreen = screen;
		dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		actionBar.setCreateAction(new ScheduledCommand() {
			@Override
			public void execute() {
				rootScreen.save(new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						doCancel();
					}

					@Override
					public void failure(Throwable e) {
						errorLabel.setText(e.getMessage());
					}
				});
			}
		});

		if (createAndEdit) {
			actionBar.setCreateAndEditAction(new ScheduledCommand() {
				@Override
				public void execute() {
					rootScreen.save(new AsyncHandler<Void>() {

						@Override
						public void success(Void value) {
							doEditCreated();
						}

						@Override
						public void failure(Throwable e) {
							errorLabel.setText(e.getMessage());
						}
					});

				}
			});
		}

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				doCancel();
			}
		});
	}

	protected abstract void doCancel();

	protected abstract void doEditCreated();

	@Override
	public Element getCenter() {
		return center.getElement();
	}

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}
	
	@Override
	public void saveModel(JavaScriptObject model) {
	}

	@Override
	public void doLoad(ScreenRoot instance) {
		JsMapStringJsObject map = instance.getModel().cast();
		ItemValue<Domain> d = DomainsHolder.get().getSelectedDomain();

		map.put("domain",
				new ItemValueGwtSerDer<>(new DomainGwtSerDer()).serialize(d).isObject().getJavaScriptObject());
		map.put("domainUid", d.uid);
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				rootScreen.loadModel(rootScreen.getModel());
			}
		});
	}

}
