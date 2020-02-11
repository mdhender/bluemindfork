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
package net.bluemind.system.validation;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonObject;
import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.system.validation.IProductValidator.ValidationResult;

public class ProductChecks {

	private static final Logger logger = LoggerFactory.getLogger(ProductChecks.class);
	private static final List<IProductValidator> validators = loadValidators();

	private ProductChecks() {
	}

	static {
		MQ.init(() -> {
			MQ.registerConsumer(Topic.PRODUCT_CHECK_REQUESTS, msg -> {
				String validator = msg.getStringProperty("validator");
				tryValidator(validator);
			});
		});
	}

	public static void validate() {
		blockingCheck();
	}

	public static CompletableFuture<Void> asyncValidate() {
		CompletableFuture<Void> ret = new CompletableFuture<>();
		logger.info("Loaded {} product validators", validators.size());
		MQ.init(() -> VertxPlatform.getVertx().executeBlocking(prom -> blockingCheck(), res -> ret.complete(null)));
		return ret;

	}

	private static void blockingCheck() {
		Producer prod = MQ.getProducer(Topic.PRODUCT_CHECK_RESULTS);
		boolean failed = false;
		boolean blocking = false;
		List<String> failedChecks = new LinkedList<>();
		for (IProductValidator validator : validators) {
			try {
				ValidationResult result = validateAndPublishResult(prod, validator);
				failed |= !result.valid;
				failedChecks.add(validator.getName());
				if (result.blocking) {
					blocking = true;
					break;
				}
			} catch (Exception e) {
				logger.error("Check {} failed ({}), skipping it for now", validator.getName(), e.getMessage());
			}
		}

		if (failed) {
			if (dryMode()) {
				logger.warn("Validation checks have failed ({}) but dry mode is active.", failedChecks);
			} else if (blocking) {
				logger.error("Validation checks have failed ({}). Exiting application....", failedChecks);
				System.exit(1);
			} else {
				logger.error("Non-blocking Validation checks have failed ({}).", failedChecks);
			}
		}
	}

	private static ValidationResult validateAndPublishResult(Producer prod, IProductValidator validator) {
		ValidationResult result = validator.validate();
		logger.info("Validator {} : Valid: {}, Message: {}", validator.getName(), result.valid, result.message);
		JsonObject toPublish = new JsonObject()//
				.put("validator", validator.getName())//
				.put("valid", result.valid)//
				.put("blocking", result.blocking)//
				.put("origin", System.getProperty("net.bluemind.property.product", "unknown"));
		prod.send(toPublish);
		return result;
	}

	private static ValidationResult tryValidator(String validator) {
		return validators.stream().filter(v -> validator.equals(v.getName())).findAny()
				.map(v -> validateAndPublishResult(MQ.getProducer(Topic.PRODUCT_CHECK_RESULTS), v)).orElse(null);
	}

	private static boolean dryMode() {
		return (System.getProperty("bm-non-blocking-checks") != null
				|| new File("/etc/bm/non.blocking.checks").exists());
	}

	private static List<IProductValidator> loadValidators() {
		RunnableExtensionLoader<IProductValidator> epLoader = new RunnableExtensionLoader<>();
		return epLoader.loadExtensions("net.bluemind.system.validation", "productvalidation", "validator", "impl");
	}

}
