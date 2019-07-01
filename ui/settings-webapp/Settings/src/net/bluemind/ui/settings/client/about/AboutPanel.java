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
package net.bluemind.ui.settings.client.about;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.gwtconsoleapp.base.editor.ScreenElement;
import net.bluemind.gwtconsoleapp.base.editor.ScreenRoot;
import net.bluemind.gwtconsoleapp.base.editor.WidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.CompositeGwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtWidgetElement;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtWidgetElement;
import net.bluemind.system.api.IInstallationAsync;
import net.bluemind.system.api.InstallationVersion;
import net.bluemind.system.api.SubscriptionInformations;
import net.bluemind.system.api.gwt.endpoint.InstallationGwtEndpoint;
import net.bluemind.ui.common.client.forms.Ajax;

public class AboutPanel extends CompositeGwtWidgetElement {

	@UiField
	Image close;

	@UiField
	Label copyright;

	@UiField
	Label version;

	@UiField
	FlowPanel infosPanel;

	@UiField
	FocusPanel root;

	private static final int BLUEMIND_YEAR_OF_BIRTH = 2012;

	private static final Binder binder = GWT.create(Binder.class);

	private IInstallationAsync installation = new InstallationGwtEndpoint(Ajax.TOKEN.getSessionId());

	interface Binder extends UiBinder<Widget, AboutPanel> {
	}

	public AboutPanel() {
		initWidget(binder.createAndBindUi(this));

		version.setText(AboutConstants.INST.version("", ""));

		Date d = new Date();
		int now = Integer.parseInt(DateTimeFormat.getFormat("yyyy").format(d));

		if (now - BLUEMIND_YEAR_OF_BIRTH == 0) {
			copyright.setText(AboutConstants.INST.copyright(Integer.toString(BLUEMIND_YEAR_OF_BIRTH)));
		} else {
			copyright.setText(AboutConstants.INST.copyright(BLUEMIND_YEAR_OF_BIRTH + " - " + now));

		}

		root.setFocus(true);
		loadServerInfos();

	}

	private void loadServerInfos() {

		installation.getVersion(new AsyncHandler<InstallationVersion>() {

			@Override
			public void success(InstallationVersion value) {
				version.setText(AboutConstants.INST.version(value.versionName, value.softwareVersion));
			}

			@Override
			public void failure(Throwable e) {

			}

		});
		installation.getSubscriptionInformations(new AsyncHandler<net.bluemind.system.api.SubscriptionInformations>() {

			@Override
			public void success(SubscriptionInformations result) {
				infosPanel.add(new Label(LicTexts.INST.lic_signed_customer() + ": " + result.customer));
				infosPanel.add(new Label(LicTexts.INST.lic_signed_customer_code() + ": " + result.customerCode));
				// FIXME add more info ?
			}

			@Override
			public void failure(Throwable e) {
				// lic handler is not available
			}

		});

	}

	@UiHandler("close")
	void handleClick(ClickEvent event) {
		hide();
	}

	@UiHandler("root")
	void handleKeyboardClose(KeyPressEvent event) {
		if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ESCAPE) {
			hide();
		}
	}

	private void hide() {
		History.back();
	}

	public static void registerType() {
		GwtWidgetElement.register("bm.settings.About", new IGwtDelegateFactory<IGwtWidgetElement, WidgetElement>() {

			@Override
			public IGwtWidgetElement create(WidgetElement el) {
				ScreenRoot s = el.cast();
				s.setOverlay(true);
				return new AboutPanel();
			}
		});
	}

	public native static ScreenElement create()
	/*-{
    return {
      'id' : 'about',
      'type' : 'bm.settings.About',
      'overlay' : true
    };
	}-*/;

}
