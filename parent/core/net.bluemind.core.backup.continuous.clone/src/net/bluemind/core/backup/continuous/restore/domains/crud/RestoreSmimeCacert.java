/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
import net.bluemind.core.backup.continuous.dto.VersionnedItem;
import net.bluemind.core.backup.continuous.restore.domains.RestoreLogger;
import net.bluemind.core.backup.continuous.restore.domains.RestoreState;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;

public class RestoreSmimeCacert extends CrudItemRestore<net.bluemind.smime.cacerts.api.SmimeCacert> {

	private static final ValueReader<VersionnedItem<SmimeCacert>> reader = JsonUtils
			.reader(new TypeReference<VersionnedItem<SmimeCacert>>() {
			});
	private final IServiceProvider target;

	public RestoreSmimeCacert(RestoreLogger log, ItemValue<Domain> domain, IServiceProvider target, RestoreState state) {
		super(log, domain, state);
		this.target = target;
	}

	@Override
	public String type() {
		return ISmimeCacertUids.TYPE;
	}

	@Override
	protected ValueReader<VersionnedItem<SmimeCacert>> reader() {
		return reader;
	}

	@Override
	protected ISmimeCACert api(ItemValue<Domain> domain, RecordKey key) {
		return target.instance(ISmimeCACert.class, key.uid);
	}

}
