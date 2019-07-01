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
package net.bluemind.ui.adminconsole.system.domains.create;

import java.util.HashMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.CompositeElement;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot.SizeHint;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.QuickCreateActionBar;

public class QCreateDomainScreen extends Composite implements IGwtCompositeScreenRoot {

	interface QCreateDomainUiBinder extends UiBinder<DockLayoutPanel, QCreateDomainScreen> {
	}

	private static QCreateDomainUiBinder uiBinder = GWT.create(QCreateDomainUiBinder.class);

	public static final String TYPE = "bm.ac.QCreateDomainScreen";

	@UiField
	QuickCreateActionBar actionBar;

	@UiField
	Label center;

	@UiField
	Label errorLabel;

	private DockLayoutPanel dlp;
	private ScreenRoot instance;

	private QCreateDomainScreen(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		actionBar.setCreateAction(new ScheduledCommand() {
			@Override
			public void execute() {

				instance.save(new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						RootPanel.get().getElement()
								.dispatchEvent(Document.get().createHtmlEvent("refresh-domains", true, true));
						Actions.get().showWithParams2("domainsManager", null);
					}

					@Override
					public void failure(Throwable e) {
						errorLabel.setText(e.getMessage());
					}
				});
			}
		});

		actionBar.setCreateAndEditAction(new ScheduledCommand() {
			@Override
			public void execute() {

				instance.save(new AsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						RootPanel.get().getElement()
								.dispatchEvent(Document.get().createHtmlEvent("refresh-domains", true, true));

						JsMapStringJsObject map = instance.getModel().cast();

						QCreateDomainModel mmodel = map.getObject("domainModel");

						HashMap<String, String> params = new HashMap<>();
						params.put("domainUid", mmodel.domainUid);

						Actions.get().showWithParams2("editDomain", params);
					}

					@Override
					public void failure(Throwable e) {
						errorLabel.setText(e.getMessage());
					}
				});
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				Actions.get().showWithParams2("system", null);
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
	}

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public QCreateDomainScreen create(ScreenRoot instance) {
				return new QCreateDomainScreen(instance);
			}
		});
		GWT.log("bm.ac.QCreateDomainScreen registered");
	}

	@Override
	public void loadModel(JavaScriptObject model) {
	}

	@Override
	public void saveModel(JavaScriptObject model) {
	}

	@Override
	public void doLoad(final ScreenRoot instance) {
		instance.load(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}

			@Override
			public void failure(Throwable e) {
				// cannot fail
			}
		});
	}

	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("qcDomain", TYPE).cast();
		JsArrayString roles = JsArrayString.createArray().cast();
		roles.push(BasicRoles.ROLE_MANAGE_DOMAIN);
		screenRoot.setRoles(roles);
		screenRoot.setOverlay(true);
		screenRoot.setSizeHint(SizeHint.create(450, 430));
		screenRoot.getHandlers().push(ModelHandler.create(null, QCreateDomainModelHandler.TYPE).<ModelHandler> cast());
		screenRoot.setContent(ScreenElement.create(null, QCreateDomainWidget.TYPE).<CompositeElement> cast());
		return screenRoot;
	}
}
