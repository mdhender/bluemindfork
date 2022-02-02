/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.restore.domains.crud;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.tag.api.ITagUids;
import net.bluemind.tag.api.ITags;
import net.bluemind.tag.api.Tag;

public class RestoreTags extends CrudRestore<Tag> {

	private static final ValueReader<ItemValue<Tag>> reader = JsonUtils.reader(new TypeReference<ItemValue<Tag>>() {
	});
	private final IServiceProvider target;

	public RestoreTags(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target) {
		super(log, domain);
		this.target = target;
	}

	@Override
	public String type() {
		return ITagUids.TYPE;
	}

	@Override
	protected ValueReader<ItemValue<Tag>> reader() {
		return reader;
	}

	@Override
	protected ITags api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(ITags.class, key.uid);
	}

}
