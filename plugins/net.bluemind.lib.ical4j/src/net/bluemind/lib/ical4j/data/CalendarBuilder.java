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
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.lib.ical4j.model.PropertyFactoryRegistry;
import net.fortuna.ical4j.data.CalendarParser;
import net.fortuna.ical4j.data.CalendarParserFactory;
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
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
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

public class CalendarBuilder {

	private static final Logger logger = LoggerFactory.getLogger(CalendarBuilder.class);
	private final Optional<List<VTimeZone>> tz;

	public CalendarBuilder(List<VTimeZone> tz) {
		this.tz = Optional.of(tz);
	}

	public CalendarBuilder() {
		this.tz = Optional.empty();
	}

	public void build(UnfoldingReader uin, BiConsumer<Calendar, Component> consumer)
			throws IOException, ParserException {
		new CalendarBuilderImpl(consumer, tz).build(uin);
	}

	public class CalendarBuilderImpl extends net.fortuna.ical4j.data.CalendarBuilder {

		private CalendarParser parser;
		private TimeZoneRegistry tzRegistry;
		private ContentHandlerImpl contentHandler;

		/**
		 * @param tz
		 * @param parser
		 * @param propertyFactory
		 * @param parameterFactory
		 * @param tz
		 */
		private CalendarBuilderImpl(BiConsumer<Calendar, Component> consumer, Optional<List<VTimeZone>> tz) {
			this.tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
			this.parser = CalendarParserFactory.getInstance().createParser();
			this.contentHandler = new ContentHandlerImpl(consumer);
			tz.ifPresent(tzList -> {
				tzList.forEach(timezone -> {
					tzRegistry.register(new TimeZone(timezone));
				});
			});
		}

		public Calendar build(UnfoldingReader uin) throws IOException, ParserException {
			// re-initialise..
			calendar = null;
			component = null;
			subComponent = null;
			property = null;

			parser.parse(uin, contentHandler);

			return null;
		}

		private class ContentHandlerImpl implements ContentHandler {

			private final ComponentFactory componentFactory;

			private final PropertyFactory propertyFactory;

			private final ParameterFactory parameterFactory;

			private final BiConsumer<Calendar, Component> consumer;

			public ContentHandlerImpl(BiConsumer<Calendar, Component> consumer) {
				this.consumer = consumer;
				this.componentFactory = ComponentFactory.getInstance();
				this.propertyFactory = new PropertyFactoryRegistry();
				this.parameterFactory = new ParameterFactoryRegistry();
			}

			public void endCalendar() {
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
					consumer.accept(calendar, component);
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
				final Parameter param = parameterFactory.createParameter(name.toUpperCase(),
						Strings.escapeNewline(value));
				property.getParameters().add(param);
				if (param instanceof TzId && tzRegistry != null && !(property instanceof XProperty)) {
					final TimeZone timezone = tzRegistry.getTimeZone(param.getValue());
					if (timezone != null) {
						updateTimeZone(property, timezone);
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
						final Logger logger = LoggerFactory.getLogger(CalendarBuilder.class);
						logger.warn("Error setting timezone [" + timezone.getID() + "] on property ["
								+ property.getName() + "]", e);
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

	}
}
