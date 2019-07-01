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
package net.bluemind.lib.ical4j.vcard;

import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.ParameterFactory;

public class UnknownParameter extends Parameter {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String value;

	public UnknownParameter(String name, String value) {
		super(name);
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	private static class UParameterFactory implements ParameterFactory<UnknownParameter> {

		private String name;

		public UParameterFactory(String name) {
			this.name = name;
		}

		@Override
		public UnknownParameter createParameter(String value) {
			return new UnknownParameter(name, value);
		}
	}

	public static ParameterFactory<? extends Parameter> createFactory(String name) {
		return new UParameterFactory(name);
	}
}
