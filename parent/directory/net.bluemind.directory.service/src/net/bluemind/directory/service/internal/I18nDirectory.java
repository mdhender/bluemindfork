/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.directory.service.internal;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.i18n.labels.I18nLabels;

public class I18nDirectory implements DirectoryDecorator {

	@Override
	public void decorate(BmContext context, ItemValue<DirEntry> entry) {
		translate(context, entry);
	}

	private void translate(BmContext context, ItemValue<DirEntry> entry) {
		entry.value.displayName = I18nLabels.getInstance().translate(context.getSecurityContext().getLang(),
				entry.displayName);
	}

}
