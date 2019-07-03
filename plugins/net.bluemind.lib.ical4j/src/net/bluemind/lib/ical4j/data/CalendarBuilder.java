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
package net.bluemind.lib.ical4j.data;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.ContentHandler;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.data.UnfoldingReader;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.CalendarException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentFactory;
import net.fortuna.ical4j.model.Escapable;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterFactory;
import net.fortuna.ical4j.model.ParameterFactoryRegistry;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.component.VAvailability;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.component.VToDo;
import net.fortuna.ical4j.model.parameter.TzId;
import net.fortuna.ical4j.model.property.DateListProperty;
import net.fortuna.ical4j.model.property.DateProperty;
import net.fortuna.ical4j.model.property.XProperty;
import net.fortuna.ical4j.util.CompatibilityHints;
import net.fortuna.ical4j.util.Constants;
import net.fortuna.ical4j.util.Strings;

public class CalendarBuilder extends net.fortuna.ical4j.data.CalendarBuilder {

	private static final Logger logger = LoggerFactory.getLogger(CalendarBuilder.class);
	private CalendarParser parser;
	private TimeZoneRegistry tzRegistry;
	private ContentHandlerImpl contentHandler;
	private List<Property> datesMissingTimezones;

	public CalendarBuilder() {
		super();
	}

	/**
	 * @param parser
	 * @param propertyFactory
	 * @param parameterFactory
	 * @param tz
	 */
	public CalendarBuilder(CalendarParser parser, PropertyFactoryImpl propertyFactory,
			ParameterFactoryRegistry parameterFactory, TimeZoneRegistry tz) {
		this.parser = parser;
		this.tzRegistry = tz;
		this.contentHandler = new ContentHandlerImpl(ComponentFactory.getInstance(), propertyFactory, parameterFactory);
	}

	public Calendar build(final UnfoldingReader uin) throws IOException, ParserException {
		// re-initialise..
		calendar = null;
		component = null;
		subComponent = null;
		property = null;
		datesMissingTimezones = new ArrayList<Property>();

		parser.parse(uin, contentHandler);

		if (datesMissingTimezones.size() > 0 && tzRegistry != null) {
			resolveTimezones();
		}

		return calendar;
	}

	private class ContentHandlerImpl implements ContentHandler {

		private final ComponentFactory componentFactory;

		private final PropertyFactory propertyFactory;

		private final ParameterFactory parameterFactory;

		public ContentHandlerImpl(ComponentFactory componentFactory, PropertyFactory propertyFactory,
				ParameterFactory parameterFactory) {

			this.componentFactory = componentFactory;
			this.propertyFactory = propertyFactory;
			this.parameterFactory = parameterFactory;
		}

		public void endCalendar() {
			// do nothing..
		}

		public void endComponent(final String name) {
			assertComponent(component);

			if (subComponent != null) {
				if (component instanceof VTimeZone) {
					((VTimeZone) component).getObservances().add(subComponent);
				} else if (component instanceof VEvent) {
					((VEvent) component).getAlarms().add(subComponent);
				} else if (component instanceof VToDo) {
					((VToDo) component).getAlarms().add(subComponent);
				} else if (component instanceof VAvailability) {
					((VAvailability) component).getAvailable().add(subComponent);
				}
				subComponent = null;
			} else {
				calendar.getComponents().add(component);
				if (component instanceof VTimeZone && tzRegistry != null) {
					// register the timezone for use with iCalendar objects..
					tzRegistry.register(new TimeZone((VTimeZone) component));
				}
				component = null;
			}
		}

		public void endProperty(final String name) {
			assertProperty(property);

			// replace with a constant instance if applicable..
			property = Constants.forProperty(property);
			if (component != null) {
				if (subComponent != null) {
					subComponent.getProperties().add(property);
				} else {
					component.getProperties().add(property);
				}
			} else if (calendar != null) {
				calendar.getProperties().add(property);
			}

			property = null;
		}

		public void parameter(final String name, final String value) throws URISyntaxException {
			assertProperty(property);

			// parameter names are case-insensitive, but convert to upper case
			// to simplify further processing
			final Parameter param = parameterFactory.createParameter(name.toUpperCase(), Strings.escapeNewline(value));
			property.getParameters().add(param);
			if (param instanceof TzId && tzRegistry != null && !(property instanceof XProperty)) {
				final TimeZone timezone = tzRegistry.getTimeZone(param.getValue());
				if (timezone != null) {
					updateTimeZone(property, timezone);
				} else {
					// VTIMEZONE may be defined later, so so keep
					// track of dates until all components have been
					// parsed, and then try again later
					datesMissingTimezones.add(property);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void propertyValue(final String value) throws URISyntaxException, ParseException, IOException {

			assertProperty(property);
			try {
				if (property instanceof Escapable) {
					property.setValue(Strings.unescape(value));
				} else {
					property.setValue(value);
				}
			} catch (Exception e) {
				logger.warn("Error setValue for property {} to {} : {}", property.getName(), value, e.getMessage());
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void startCalendar() {
			calendar = new Calendar();
		}

		/**
		 * {@inheritDoc}
		 */
		public void startComponent(final String name) {
			if (component != null) {
				subComponent = componentFactory.createComponent(name);
			} else {
				component = componentFactory.createComponent(name);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		public void startProperty(final String name) {
			// property names are case-insensitive, but convert to upper case to
			// simplify further processing
			property = propertyFactory.createProperty(name.toUpperCase());
		}
	}

	private void updateTimeZone(Property property, TimeZone timezone) {
		try {
			((DateProperty) property).setTimeZone(timezone);
		} catch (ClassCastException e) {
			try {
				((DateListProperty) property).setTimeZone(timezone);
			} catch (ClassCastException e2) {
				if (CompatibilityHints.isHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING)) {
					Log log = LogFactory.getLog(CalendarBuilder.class);
					log.warn("Error setting timezone [" + timezone.getID() + "] on property [" + property.getName()
							+ "]", e);
				} else {
					throw e2;
				}
			}
		}
	}

	private void assertComponent(Component component) {
		if (component == null) {
			throw new CalendarException("Expected component not initialised");
		}
	}

	private void assertProperty(Property property) {
		if (property == null) {
			throw new CalendarException("Expected property not initialised");
		}
	}

	private void resolveTimezones() throws IOException {

		// Go through each property and try to resolve the TZID.
		for (final Iterator<Property> it = datesMissingTimezones.iterator(); it.hasNext();) {
			final Property property = (Property) it.next();
			final Parameter tzParam = property.getParameter(Parameter.TZID);

			// tzParam might be null:
			if (tzParam == null) {
				continue;
			}

			// lookup timezone
			final TimeZone timezone = tzRegistry.getTimeZone(tzParam.getValue());

			// If timezone found, then update date property
			if (timezone != null) {
				// Get the String representation of date(s) as
				// we will need this after changing the timezone
				final String strDate = property.getValue();

				// Change the timezone
				if (property instanceof DateProperty) {
					((DateProperty) property).setTimeZone(timezone);
				} else if (property instanceof DateListProperty) {
					((DateListProperty) property).setTimeZone(timezone);
				}

				// Reset value
				try {
					property.setValue(strDate);
				} catch (ParseException e) {
					// shouldn't happen as its already been parsed
					throw new CalendarException(e);
				} catch (URISyntaxException e) {
					// shouldn't happen as its already been parsed
					throw new CalendarException(e);
				}
			}
		}
	}
}
