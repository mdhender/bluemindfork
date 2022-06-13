/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.backend.mail.replica.service.internal.sort;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.container.model.SortDescriptor.Direction;
import net.bluemind.core.container.model.SortDescriptor.Field;
import net.bluemind.core.sanitizer.ISanitizer;

public class SortDescriptorSanitizer implements ISanitizer<SortDescriptor> {

	protected static final Logger logger = LoggerFactory.getLogger(SortDescriptorSanitizer.class);

	public SortDescriptorSanitizer() {
	}

	private void sanitize(SortDescriptor sortDesc) {
		if (sortDesc == null) {
			sortDesc = new SortDescriptor();
		}

		if (sortDesc.fields.isEmpty()) {
			Field sortingDate = new Field();
			sortingDate.column = "internal_date";
			sortingDate.dir = Direction.Desc;
			sortDesc.fields = Arrays.asList(sortingDate);
		}

		if (sortDesc.filter == null) {
			sortDesc.filter = ItemFlagFilter.create().mustNot(ItemFlag.Deleted);
		}
	}

	@Override
	public void create(SortDescriptor sortDesc) {
		sanitize(sortDesc);
	}

	@Override
	public void update(SortDescriptor current, SortDescriptor obj) {
		throw new ServerFault("update not implemented");
	}

}