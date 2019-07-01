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
package net.bluemind.ui.adminconsole.system.subscription;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.gwt.serder.SubscriptionInformationsGwtSerDer;
import net.bluemind.ui.adminconsole.base.SubscriptionInfoHolder;
import net.bluemind.ui.adminconsole.system.subscription.l10n.InstallLicenseConstants;
import net.bluemind.ui.adminconsole.system.subscription.l10n.SubscriptionConstants;

public class SubscriptionWidget extends Composite implements IGwtScreenRoot {

	interface SubscriptionBinder extends UiBinder<DockLayoutPanel, SubscriptionWidget> {
	}

	@UiField
	FileUpload uploadLicenseFile;

	@UiField
	FormPanel uploadFormPanel;

	@UiField
	Button uploadLicenseButton;

	@UiField
	Button deleteLicenseButton;

	@UiField
	Grid licenceInformation;

	@UiField
	Label customer;

	@UiField
	Label bmVersion;

	@UiField
	Label maxAccounts;

	@UiField
	Label accounts;

	@UiField
	Label simpleAccounts;

	@UiField
	Label maxSimpleAccounts;

	@UiField
	Label subscriptionStarts;

	@UiField
	Label subscriptionEnds;

	@UiField
	Label subscriptionIdentifier;

	String license;

	String licenseFileName;

	@UiField
	VerticalPanel messagesTable;

	@UiField
	Label simpleAccountsLabel;

	@UiField
	Label maxSimpleAccountsLabel;
	
	@UiField
	HTML subscriptionPostInstallInformations;
	
	@UiField
	HTML noSubContacts;
	
	@UiField
	Label aboutSubContacts;
	
	@UiField
	SubContactTable mailTable;
	
	public interface BBBundle extends ClientBundle {
		@Source("SubscriptionWidget.css")
		BBStyle getStyle();
	}
	
	public interface BBStyle extends CssResource {
		String mailContact();
	}

	public static final BBBundle bundle;
	public static final BBStyle style;
	
	static {
		bundle = GWT.create(BBBundle.class);
		style = bundle.getStyle();
		style.ensureInjected();
	}
	
	private static SubscriptionBinder uiBinder = GWT.create(SubscriptionBinder.class);
	private SubscriptionRemovalWarningDialog srws;
	private ScreenRoot screenRoot;
	public static final String TYPE = "bm.ac.SubscriptionWidget";
	
	public SubscriptionWidget(ScreenRoot screenRoot) {
		this.screenRoot = screenRoot;
		DockLayoutPanel dlp = uiBinder.createAndBindUi(this);
		dlp.setHeight("100%");
		initWidget(dlp);
		
		uploadLicenseButton.setText(InstallLicenseConstants.INST.updateLicenseButton());
		setupUploadForm();
		deleteLicenseButton.setVisible(false);
		srws = new SubscriptionRemovalWarningDialog(new Runnable() {

			@Override
			public void run() {
				save(null, null);
			}
		});		
		
		noSubContacts.setHTML("<h2>"+SubscriptionConstants.INST.noSubscriptionContact()+"</h2>");
		aboutSubContacts.setText(SubscriptionConstants.INST.subscriptionContacts());
		
		licenceInformation.getColumnFormatter().setWidth(0, "300px");
		licenceInformation.getColumnFormatter().setWidth(1, "200px");
		
		mailTable.setInformationsPanels(aboutSubContacts, noSubContacts);
		mailTable.setStyleName(style.mailContact());
	}
	
	public static ScreenElement screenModel() {
		ScreenRoot screenRoot = ScreenRoot.create("subscription", TYPE).cast();
		screenRoot.getHandlers().push(ModelHandler.create(null, SubscriptionModelHandler.TYPE).<ModelHandler>cast());
		return screenRoot;
	}
	

	@Override
	public void attach(Element parent) {
		parent.appendChild(getElement());
		onAttach();	
	}

	@Override
	public void loadModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		
		JavaScriptObject jsSubscription = map.get(SubscriptionKeys.subscription.name());
		JsArrayString jsSubscriptionContacts = map.get(SubscriptionKeys.subscriptionContacts.name()).cast();
		
		SubscriptionInformations sub = null;
		if (jsSubscription != null) {
			GWT.log("Found an installed subscription");
			sub = new SubscriptionInformationsGwtSerDer()
					.deserialize(new JSONObject(jsSubscription.cast()));
			setSubscriptionInformations(sub);
		} else {
			setSubscriptionInformations(null);
		}
		
		if (jsSubscriptionContacts != null) {
			mailTable.display(jsSubscriptionContacts);
			
			if (jsSubscriptionContacts.length() == 0) {
				aboutSubContacts.setVisible(false);
			}
		}
		
		if (jsSubscription == null || !sub.valid) {
			mailTable.setVisible(false);
			noSubContacts.setVisible(false);
			aboutSubContacts.setVisible(false);
		} else {
			mailTable.setVisible(true);
			if (mailTable.hasSubContact()) {
				noSubContacts.setVisible(false);
				aboutSubContacts.setVisible(true);
			} else {
				noSubContacts.setVisible(true);
				aboutSubContacts.setVisible(false);
			}
		}
	}

	@Override
	public void saveModel(JavaScriptObject model) {
		final JsMapStringJsObject map = model.cast();
		map.remove("deleteSubscription");
		map.remove(SubscriptionKeys.license.name());
		if (license == null) {
			map.putString("deleteSubscription", "true");
		} else {
			GWT.log("installing license...");
			map.putString(SubscriptionKeys.license.name(), license);
			map.putString(SubscriptionKeys.filename.name(), licenseFileName);
		}
	}

	@Override
	public void doLoad(ScreenRoot instance) {
		instance.load(new DefaultAsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				instance.loadModel(instance.getModel());
			}
		});
	}
	
	public static void registerType() {
		GwtScreenRoot.register(TYPE, new IGwtDelegateFactory<IGwtScreenRoot, ScreenRoot>() {

			@Override
			public IGwtScreenRoot create(ScreenRoot screenRoot) {
				return new SubscriptionWidget(screenRoot);
			}
		});
	}
	
	public void setSubscriptionInformations(SubscriptionInformations sub) {
		showLicenseInfo(sub);
		
		if (sub.kind == SubscriptionInformations.Kind.NONE) {
			deleteLicenseButton.setVisible(false);
		} else {
			deleteLicenseButton.setVisible(true);
		}
		
		deleteLicenseButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {

				final DialogBox os = new DialogBox();
				os.addStyleName("dialog");
				srws.setSize("800px", "250px");
				srws.setOverlay(os);
				os.setWidget(srws);
				os.setGlassEnabled(true);
				os.setAutoHideEnabled(false);
				os.setGlassStyleName("modalOverlay");
				os.setModal(false);
				os.center();
				os.show();
			}
		});
	}
	
	private void save(String licence, String filename) {
		this.license = licence;
		this.licenseFileName = filename;
		screenRoot.save(new AsyncHandler<Void>() {

			@Override
			public void success(Void value) {
				SubscriptionInfoHolder.get().init();
				Notification.get().reportInfo(InstallLicenseConstants.INST.subscriptionUpdated());
				doLoad(screenRoot);
			}

			@Override
			public void failure(Throwable e) {
				SubscriptionInfoHolder.get().init();
				Notification.get().reportError(e);
				doLoad(screenRoot);
			}
		});
	}

	private void showLicenseInfo(SubscriptionInformations subscription) {
		messagesTable.clear();
		for (SubscriptionInformations.Message m : subscription.messages) {
			Label label = new Label();
			label.setText(m.message);
			if (m.kind == SubscriptionInformations.Message.Kind.Error) {
				label.setStyleName("subscription message error");
			} else {
				label.setStyleName("subscription message warning");
			}
			messagesTable.add(label);
		}
		customer.setText(subscription.customer);
		bmVersion.setText(subscription.version);

		maxAccounts.setText("-");
		accounts.setText("-");
		maxSimpleAccounts.setText("-");
		simpleAccounts.setText("-");
		for (SubscriptionInformations.InstallationIndicator indicator : subscription.indicator) {
			switch (indicator.kind) {
			case FullUser:
				maxAccounts.setText(indicator.maxValue != null ? indicator.maxValue.toString() : "-");
				accounts.setText(indicator.currentValue != null ? indicator.currentValue.toString() : "-");

				break;
			case SimpleUser:
				maxSimpleAccounts.setText(indicator.maxValue != null ? indicator.maxValue.toString() : "-");
				simpleAccounts.setText(indicator.currentValue != null ? indicator.currentValue.toString() : "-");
				break;

			default:
				break;
			}
		}

		subscriptionStarts.setText(subscription.starts != null
				? DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(subscription.starts)
				: "-");
		subscriptionEnds.setText(subscription.ends != null
				? DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT).format(subscription.ends)
				: "-");
		subscriptionIdentifier.setText(subscription.customerCode);

		boolean simple = SubscriptionInfoHolder.subIncludesSimpleAccount();
		simpleAccounts.setVisible(simple);
		maxSimpleAccounts.setVisible(simple);
		simpleAccountsLabel.setVisible(simple);
		maxSimpleAccountsLabel.setVisible(simple);
	}

	private void setupUploadForm() {
		uploadFormPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
		uploadFormPanel.setMethod(FormPanel.METHOD_POST);
		String uploadUrl = GWT.getModuleBaseURL();
		uploadUrl = uploadUrl.substring(0, uploadUrl.lastIndexOf("/"));
		uploadUrl = uploadUrl.substring(0, uploadUrl.lastIndexOf("/") + 1);
		uploadFormPanel.setAction(uploadUrl + "fileupload");

		uploadLicenseButton.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				uploadLicenseFile.click();
			}
		});

		uploadLicenseFile.addChangeHandler(new ChangeHandler() {

			@Override
			public void onChange(ChangeEvent event) {
				if (!uploadLicenseFile.getFilename().isEmpty()) {
					uploadFormPanel.submit();
				}
			}
		});

		uploadFormPanel.addSubmitCompleteHandler(new SubmitCompleteHandler() {

			@Override
			public void onSubmitComplete(SubmitCompleteEvent event) {
				uploadFormPanel.reset();
				String ret = event.getResults();
				String replaced = ret.replaceAll("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", "");
				JavaScriptObject safeEval = JsonUtils.safeEval(replaced);
				JSONObject fileUploadData = new JSONObject(safeEval);
				license = fileUploadData.get("data").isString().stringValue();
				licenseFileName = fileUploadData.get("filename").isString().stringValue();

				save(license, licenseFileName);
				subscriptionPostInstallInformations
						.setHTML(InstallLicenseConstants.INST.subscriptionPostInstallInformations());
				subscriptionPostInstallInformations.setVisible(true);
			}
		});
	}
}
