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
package net.bluemind.ui.mailbox.sharing;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import net.bluemind.core.api.AsyncHandler;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainersAsync;
import net.bluemind.core.container.api.gwt.endpoint.ContainersGwtEndpoint;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.gwtconsoleapp.base.editor.gwt.GwtModelHandler;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtDelegateFactory;
import net.bluemind.gwtconsoleapp.base.editor.gwt.IGwtModelHandler;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.ui.common.client.forms.Ajax;
import net.bluemind.ui.gwtsharing.client.BaseSharingsModelHandler;

public class MailboxesSharingsModelHandler extends BaseSharingsModelHandler {

	public static final String MODEL_ID = "mails-sharing";

	public MailboxesSharingsModelHandler() {
		super(MODEL_ID);
	}

	@Override
	protected void loadContainers(JavaScriptObject model, AsyncHandler<List<ContainerDescriptor>> handler) {
		IContainersAsync containers = new ContainersGwtEndpoint(Ajax.TOKEN.getSessionId());

		ContainerQuery query = ContainerQuery.type(IMailboxAclUids.TYPE);
		query.verb = Arrays.asList(new Verb[] { Verb.All });
		containers.all(query, handler);
	}

	public static final String TYPE = "bm.mailbox.MailboxesSharingsModelHandler";

	public static void registerType() {
		GwtModelHandler.register(TYPE,
				new IGwtDelegateFactory<IGwtModelHandler, net.bluemind.gwtconsoleapp.base.editor.ModelHandler>() {

					@Override
					public IGwtModelHandler create(net.bluemind.gwtconsoleapp.base.editor.ModelHandler model) {
						return new MailboxesSharingsModelHandler();
					}
				});
	}
}
