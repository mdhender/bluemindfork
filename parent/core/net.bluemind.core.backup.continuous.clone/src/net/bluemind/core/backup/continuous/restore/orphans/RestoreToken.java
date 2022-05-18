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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.dto.CoreTok;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;

public class RestoreToken {

	private static final Logger logger = LoggerFactory.getLogger(RestoreToken.class);

	public RestoreToken() {
	}

	public String restore(IServerTaskMonitor monitor, List<DataElement> maybeTok) {
		ValueReader<ItemValue<CoreTok>> scReader = JsonUtils.reader(new TypeReference<ItemValue<CoreTok>>() {
		});

		Optional<DataElement> lastDe = maybeTok.stream()
				.filter(de -> de.key.valueClass.equals(CoreTok.class.getCanonicalName()))
				.collect(Collectors.maxBy((de1, de2) -> Long.compare(de1.offset, de2.offset)));
		Optional<CoreTok> lastTok = lastDe.map(de -> scReader.read(new String(de.payload)).value);
		monitor.log("Got core.tok " + lastTok);
		return lastTok.map(c -> c.key).orElse(null);
	}

}
