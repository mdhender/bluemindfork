/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.authentication.service.tokens;

import java.util.concurrent.TimeUnit;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.lib.vertx.IVerticleFactory;

public class TokenExpireVerticle extends AbstractVerticle {

	public static class Factory implements IVerticleFactory {

		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new TokenExpireVerticle();
		}

	}

	@Override
	public void start() {
		vertx.setPeriodic(TimeUnit.HOURS.toMillis(1), tid -> {
			TokensStore.get().expireOldTokens();
		});
		TokensStore.get().expireOldTokens();
	}

}
