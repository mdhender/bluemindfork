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
package net.bluemind.ui.adminconsole.security.certificate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;

public class CertificateEditor extends CompositeGwtWidgetElement implements IGwtCompositeScreenRoot {
	public static final String TYPE = "bm.ac.CertificateEditor";

	private ScreenRoot instance;

	@UiField
	HTMLPanel center;

	@UiField
	CertificateEditorComponent certificateData;

	@UiField
	CrudActionBar actionBar;

	interface CertificateUiBinder extends UiBinder<DockLayoutPanel, CertificateEditor> {
	}

	private static CertificateUiBinder uiBinder = GWT.create(CertificateUiBinder.class);

	public static void registerType() {
		GwtScreenRoot.registerComposite(TYPE, new IGwtDelegateFactory<IGwtCompositeScreenRoot, ScreenRoot>() {

			@Override
			public IGwtCompositeScreenRoot create(ScreenRoot screenRoot) {
				return new CertificateEditor(screenRoot);
			}
		});
	}

	private CertificateEditor(ScreenRoot screenRoot) {
		this.instance = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		certificateData.setupUploadForms();
		certificateData.enableCheckBoxes(false);

		actionBar.setSaveAction(new ScheduledCommand() {
			@Override
			public void execute() {
				doSave();
			}
		});

		actionBar.setCancelAction(new ScheduledCommand() {
			@Override
			public void execute() {
				doCancel();
			}
		});
	}

	protected void doSave() {
		instance.save(new DefaultAsyncHandler<Void>() {
			@Override
			public void success(Void value) {
				certificateData.saveCertificate("global.virt", true);
			}
		});
	}

	protected void doCancel() {
		back();
	}

	private void back() {
		History.back();
	}

	@Override
	public void saveModel(JavaScriptObject model) {

	}

	@Override
	public void loadModel(JavaScriptObject model) {

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
				Notification.get().reportError(e);
			}
		});
	}

	@Override
	public Element getCenter() {
		return center.getElement();
	}

}
