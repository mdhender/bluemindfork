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
package net.bluemind.lib.ical4j.vcard.property;

import java.util.List;

import net.fortuna.ical4j.model.Escapable;
import net.fortuna.ical4j.vcard.Group;
import net.fortuna.ical4j.vcard.Parameter;
import net.fortuna.ical4j.vcard.Property;
import net.fortuna.ical4j.vcard.PropertyFactory;

public class ABShowAs extends Property implements Escapable {

	private static final long serialVersionUID = 2721641714208540091L;

	public static final PropertyFactory<Property> FACTORY = new Factory();

	private String value;

	public ABShowAs(String value) {
		super("ABShowAs");
		this.value = value;
	}

	/**
	 * Factory constructor.
	 * 
	 * @param params property parameters
	 * @param value  string representation of a property value
	 */
	public ABShowAs(List<Parameter> params, String value) {
		super("ABShowAs", params);
		this.value = value;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public void validate() {

	}

	private static class Factory implements PropertyFactory<Property> {

		/**
		 * {@inheritDoc}
		 */
		public ABShowAs createProperty(final List<Parameter> params, final String value) {
			return new ABShowAs(params, value);
		}

		/**
		 * {@inheritDoc}
		 */
		public ABShowAs createProperty(final Group group, final List<Parameter> params, final String value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean supports(String id) {
			return id.equals("ABShowAs");
		}
	}
}