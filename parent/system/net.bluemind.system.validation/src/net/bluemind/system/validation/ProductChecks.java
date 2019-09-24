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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eclipse.common.RunnableExtensionLoader;
import net.bluemind.system.validation.IProductValidator.ValidationResult;

public class ProductChecks {

	private static final Logger logger = LoggerFactory.getLogger(ProductChecks.class);

	public static void validate() {

		List<IProductValidator> validators = loadValidators();

		logger.info("Loaded {} product validators", validators.size());

		boolean failed = false;
		for (IProductValidator validator : validators) {
			ValidationResult result = validator.validate();
			logger.info("Validator {} : Valid: {}, Message: {}", validator.getName(), result.valid, result.message);
			failed |= !result.valid;
		}

		if (failed) {
			logger.warn("Validation checks have failed. Exiting application....");
			System.exit(1);
		}

	}

	private static List<IProductValidator> loadValidators() {
		RunnableExtensionLoader<IProductValidator> epLoader = new RunnableExtensionLoader<>();
		return epLoader.loadExtensions("net.bluemind.system.validation", "productvalidation", "validator", "impl");
	}

}
