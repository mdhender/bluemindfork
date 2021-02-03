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

public class NoteAsHtml extends Property implements Escapable {

	private static final long serialVersionUID = 5032789347021478117L;

	public static final PropertyFactory<Property> FACTORY = new Factory();

	private String value;

	public NoteAsHtml(String value) {
		super("NOTE-HTML");
		this.value = value;
	}

	/**
	 * Factory constructor.
	 * 
	 * @param params property parameters
	 * @param value  string representation of a property value
	 */
	public NoteAsHtml(List<Parameter> params, String value) {
		super("NOTE-HTML", params);
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
		public NoteAsHtml createProperty(final List<Parameter> params, final String value) {
			return new NoteAsHtml(params, value);
		}

		/**
		 * {@inheritDoc}
		 */
		public NoteAsHtml createProperty(final Group group, final List<Parameter> params, final String value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean supports(String id) {
			return id.equals("NOTE-HTML");
		}

	}
}