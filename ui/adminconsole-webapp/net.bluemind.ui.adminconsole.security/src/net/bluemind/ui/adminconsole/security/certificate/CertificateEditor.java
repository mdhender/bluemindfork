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
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.gwt.endpoint.DomainsGwtEndpoint;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtCompositeScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.gwtconsoleapp.base.notification.Notification;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.api.gwt.endpoint.SystemConfigurationGwtEndpoint;
import net.bluemind.ui.adminconsole.base.ui.CrudActionBar;
import net.bluemind.ui.adminconsole.system.certificate.CertificateEditorComponent;
import net.bluemind.ui.adminconsole.system.systemconf.SysConfModel;
import net.bluemind.ui.common.client.forms.Ajax;

public class CertificateEditor extends CompositeGwtWidgetElement implements IGwtCompositeScreenRoot {
	public static final String TYPE = "bm.ac.CertificateEditor";

	private ScreenRoot instance;

	@UiField
	HTMLPanel center;

	@UiField
	CertificateEditorComponent certificateData;

	@UiField
	CrudActionBar actionBar;

	SysConfModel sysConf;

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
		certificateData.init(true);

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
				CertificateDomainEngine sslEngine = certificateData.getSelectedSslCertificateEngine();
				if (sslEngine != null) {
					sysConf.putString(SysConfKeys.ssl_certif_engine.name(), sslEngine.name());
					certificateData.saveCertificate(sslEngine, "global.virt");
				} else {
					Notification.get().reportError("SSL Certificate Engine must not be null");
				}
			}

			@Override
			public void failure(Throwable e) {
				Notification.get().reportError(e);
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
		DomainsGwtEndpoint domainService = new DomainsGwtEndpoint(Ajax.TOKEN.getSessionId());
		sysConf = SysConfModel.from(model);

		domainService.get("global.virt", new DefaultAsyncHandler<ItemValue<Domain>>() {
			@Override
			public void success(ItemValue<Domain> domain) {
				SystemConfigurationGwtEndpoint settings = new SystemConfigurationGwtEndpoint(Ajax.TOKEN.getSessionId());
				settings.getValues(new DefaultAsyncHandler<SystemConf>() {
					@Override
					public void success(SystemConf value) {
						sysConf.putString(SysConfKeys.ssl_certif_engine.name(),
								value.values.get(SysConfKeys.ssl_certif_engine.name()));
						String certifFromSettings = sysConf.get(SysConfKeys.ssl_certif_engine.name());
						String externalUrl = sysConf.get(SysConfKeys.external_url.name());
						CertificateDomainEngine sslCertifEngine = certifFromSettings == null
								? CertificateDomainEngine.FILE
								: CertificateDomainEngine.valueOf(certifFromSettings);
						certificateData.load(sslCertifEngine, "global.virt", domain.value, instance.getModel(),
								externalUrl);
					}
				});
			}
		});
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
