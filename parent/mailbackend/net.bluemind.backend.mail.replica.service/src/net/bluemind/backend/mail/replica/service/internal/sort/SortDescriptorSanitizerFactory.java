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

import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.SortDescriptor;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;

public class SortDescriptorSanitizerFactory implements ISanitizerFactory<SortDescriptor> {

	@Override
	public Class<SortDescriptor> support() {
		return SortDescriptor.class;
	}

	@Override
	public ISanitizer<SortDescriptor> create(BmContext context, Container container) {
		return new SortDescriptorSanitizer();
	}

}
