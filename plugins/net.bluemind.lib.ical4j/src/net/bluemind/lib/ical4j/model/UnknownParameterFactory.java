/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lib.ical4j.model;

import java.net.URISyntaxException;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactory;

@SuppressWarnings("serial")
public class UnknownParameterFactory implements ParameterFactory<Parameter> {
	private final String name;

	public UnknownParameterFactory(String name) {
		this.name = name;
	}

	@Override
	public Parameter createParameter(String value) throws URISyntaxException {
		return new Parameter(name, this) {

			@Override
			public String getValue() {
				return value.replaceAll("\"", "");
			}
		};
	}

	@Override
	public boolean supports(String name) {
		return name.equals(this.name);
	}
}
