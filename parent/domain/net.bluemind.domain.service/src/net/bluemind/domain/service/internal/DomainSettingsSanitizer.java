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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.domain.service.internal;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.domain.api.DomainSettingsKeys;

public class DomainSettingsSanitizer {

	public void sanitize(Map<String, String> settings) throws ServerFault {
		checkNullValues(settings);
	}

	private void checkNullValues(Map<String, String> settings) {
		Iterator<Map.Entry<String, String>> itr = settings.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, String> curr = itr.next();
			try {
				Annotation[] annos = DomainSettingsKeys.class.getField(curr.getKey()).getAnnotations();
				for (Annotation annot : annos) {
					if (annot.annotationType().isAssignableFrom(NotNull.class)) {
						if (curr.getValue() == null || curr.getValue().isBlank()) {
							itr.remove();
						}
					}
				}
			} catch (Exception e) {
				// ignore, key not part of enum
			}
		}
	}
}
