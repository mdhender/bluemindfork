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
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.InlineHTML;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.gwt.endpoint.SecurityMgmtGwtEndpoint;
import net.bluemind.ui.adminconsole.base.Actions;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.common.client.forms.Ajax;

public class CertificateEditor extends CompositeGwtWidgetElement implements IGwtCompositeScreenRoot {
	public static final String TYPE = "bm.ac.CertificateEditor";

	private ScreenRoot instance;

	@UiField
	HTMLPanel center;

	private String keyData;

	@UiField
	CheckBox keyFilePresent;

	@UiField
	FormPanel keyUploadForm;

	@UiField
	FileUpload keyFileUpload;

	private String caData;

	@UiField
	CheckBox caFilePresent;

	@UiField
	FormPanel caUploadForm;

	@UiField
	FileUpload caFileUpload;

	private String certData;

	@UiField
	CheckBox certFilePresent;

	@UiField
	FormPanel certUploadForm;

	@UiField
	FileUpload certFileUpload;

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
		setupUploadForms();
		keyFilePresent.setEnabled(false);
		caFilePresent.setEnabled(false);
		certFilePresent.setEnabled(false);

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
				saveCertificate();
			}
		});
	}

	private void saveCertificate() {
		CertData certificateValues = new CertData();
		certificateValues.certificate = certData;
		certificateValues.certificateAuthority = caData;
		certificateValues.privateKey = keyData;

		new SecurityMgmtGwtEndpoint(Ajax.TOKEN.getSessionId()).updateCertificate(certificateValues,
				new DefaultAsyncHandler<Void>() {

					@Override
					public void success(Void value) {
						Notification.get().reportInfo("Certificate has been imported");
						Actions.get().showWithParams2("security", null);
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

	private void setupUploadForms() {
		setupUploadForm(keyUploadForm, UploadType.KEY, keyFileUpload);
		setupUploadForm(caUploadForm, UploadType.CA, caFileUpload);
		setupUploadForm(certUploadForm, UploadType.CERT, certFileUpload);
	}

	private void setupUploadForm(final FormPanel uploadForm, final UploadType type, FileUpload upload) {
		uploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadForm.setMethod(FormPanel.METHOD_POST);
		uploadForm.setAction("utils/textfileupload");
		uploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				uploadForm.reset();
				String fileData = new InlineHTML(event.getResults()).getText();
				JavaScriptObject safeEval = JsonUtils.safeEval(fileData);
				JSONArray fileUploadData = new JSONArray(safeEval);
				switch (type) {
				case KEY:
					keyData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					keyFilePresent.setValue(true);
					break;
				case CA:
					caData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					caFilePresent.setValue(true);
					break;
				case CERT:
					certFilePresent.setValue(true);
					certData = fileUploadData.get(0).isObject().get("content").isString().stringValue();
					break;
				}

			}
		});
		upload.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				uploadForm.submit();
			}
		});
	}

	private enum UploadType {
		KEY, CA, CERT;
	}

}
