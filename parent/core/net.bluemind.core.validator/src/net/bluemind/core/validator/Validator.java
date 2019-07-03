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
package net.bluemind.core.validator;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.rest.BmContext;
import net.bluemind.eclipse.common.RunnableExtensionLoader;

public class Validator {

	private static final Logger logger = LoggerFactory.getLogger(Validator.class);

	private static final List<IValidatorFactory<Object>> validators = loadValidator();

	private static List<IValidatorFactory<Object>> loadValidator() {
		RunnableExtensionLoader<IValidatorFactory<Object>> rel = new RunnableExtensionLoader<IValidatorFactory<Object>>();
		List<IValidatorFactory<Object>> stores = rel.loadExtensions("net.bluemind.core", "validatorfactory",
				"validatorfactory", "implementation");

		for (IValidatorFactory<Object> j : stores) {
			logger.info("validator factory class: {} for: {}", j.getClass().getName(), j.support());
		}

		return stores;
	}

	private final BmContext context;

	public Validator(BmContext context) {
		this.context = context;
	}

	public void create(Object entity) {
		create(entity, Collections.emptyMap());
	}

	public void update(Object current, Object entity) {
		update(current, entity, Collections.emptyMap());
	}

	public void create(Object entity, Map<String, String> params) {
		if (entity == null) {
			return;
		}

		for (IValidatorFactory<Object> validatorFactory : validators) {
			if (entity.getClass() == validatorFactory.support()) {
				validatorFactory.create(context).create(entity, params);
			}
		}
	}

	public void update(Object current, Object entity, Map<String, String> params) {
		if (current == null || entity == null) {
			return;
		}

		for (IValidatorFactory<Object> validatorFactory : validators) {
			if (entity.getClass() == validatorFactory.support()) {
				validatorFactory.create(context).update(current, entity, params);
			}
		}
	}
}
