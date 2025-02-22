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
package net.bluemind.central.reverse.proxy.stream;

import com.typesafe.config.Config;

import io.vertx.core.Verticle;
import net.bluemind.central.reverse.proxy.common.config.CrpConfig;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordKeyMapper;
import net.bluemind.central.reverse.proxy.model.common.mapper.RecordValueMapper;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class DirEntriesStreamVerticleFactory implements IVerticleFactory, IUniqueVerticleFactory {

	public static final Config config = CrpConfig.get("Stream", DirEntriesStreamVerticleFactory.class.getClassLoader());

	@Override
	public boolean isWorker() {
		return false;
	}

	@Override
	public Verticle newInstance() {
		return new DirEntriesStreamVerticle(config, RecordKeyMapper.byteArray(), RecordValueMapper.byteArray());
	}

}
