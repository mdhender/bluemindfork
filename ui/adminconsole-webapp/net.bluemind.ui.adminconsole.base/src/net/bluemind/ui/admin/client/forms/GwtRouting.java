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
package net.bluemind.ui.admin.client.forms;

import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.text.shared.Renderer;
import com.google.gwt.view.client.ProvidesKey;

import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.ui.admin.client.forms.l10n.mailbox.MailboxConstants;

public class GwtRouting {

	private static final MailboxConstants constants = GWT.create(MailboxConstants.class);

	public static final Renderer<Routing> RENDERER = new AbstractRenderer<Routing>() {

		@Override
		public String render(Routing object) {
			if (object == null) {
				return "";
			} else {
				switch (object) {
				case external:
					return constants.externalRouting();
				case internal:
					return constants.internalRouting();
				case none:
					return constants.noneRouting();
				}

				return "";
			}
		}
	};

	public static final ProvidesKey<Routing> KEYPROVIDER = new ProvidesKey<Routing>() {

		@Override
		public Object getKey(Routing item) {
			if (item == null) {
				return null;
			}

			return item.name();
		}
	};
}
