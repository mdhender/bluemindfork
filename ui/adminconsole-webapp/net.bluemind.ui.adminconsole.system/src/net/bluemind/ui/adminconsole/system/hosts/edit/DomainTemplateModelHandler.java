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
package net.bluemind.ui.adminconsole.system.hosts.edit;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.commons.gwt.JsMapStringJsObject;
import net.bluemind.gwtconsoleapp.base.editor.ModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.gwtconsoleapp.base.handler.DefaultAsyncHandler;
import net.bluemind.system.api.DomainTemplate;
import net.bluemind.system.api.gwt.endpoint.DomainTemplateGwtEndpoint;
import net.bluemind.system.api.gwt.js.JsDomainTemplate;
import net.bluemind.system.api.gwt.serder.DomainTemplateGwtSerDer;
import net.bluemind.ui.adminconsole.system.hosts.HostKeys;
import net.bluemind.ui.common.client.forms.Ajax;

public class DomainTemplateModelHandler implements IGwtModelHandler {

	public static String TYPE = "bm.ac.DomainTemplateModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE, new IGwtDelegateFactory<IGwtModelHandler, ModelHandler>() {

			@Override
			public IGwtModelHandler create(ModelHandler modelHandler) {
				return new DomainTemplateModelHandler();
			}
		});
		GWT.log("bm.ac.DomainTemplateModelHandler registered");
	}

	@Override
	public void load(JavaScriptObject model, final AsyncHandler<Void> handler) {
		final JsMapStringJsObject map = model.cast();
		DomainTemplateGwtEndpoint domainTemplateService = new DomainTemplateGwtEndpoint(Ajax.TOKEN.getSessionId());
		domainTemplateService.getTemplate(new DefaultAsyncHandler<DomainTemplate>(handler) {

			@Override
			public void success(DomainTemplate value) {
				JsDomainTemplate domainTemplate = new DomainTemplateGwtSerDer().serialize(value).isObject()
						.getJavaScriptObject().cast();
				map.put(HostKeys.domainTemplate.name(), domainTemplate);
				handler.success(null);
			}
		});
	}

	@Override
	public void save(JavaScriptObject model, final AsyncHandler<Void> handler) {
		handler.success(null);
	}

}
