/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.calendar.service.deferredaction;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.james.mime4j.MimeException;
import org.elasticsearch.common.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.TemplateException;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.helper.mail.CalendarMailHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.deferredaction.api.DeferredAction;
import net.bluemind.deferredaction.api.IDeferredAction;
import net.bluemind.deferredaction.api.IDeferredActionContainerUids;
import net.bluemind.deferredaction.registry.IDeferredActionExecutor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.icalendar.api.ICalendarElement.VAlarm;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUserSettings;

public class DeferredActionEventExecutor implements IDeferredActionExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DeferredActionEventExecutor.class);

	private IServiceProvider provider;
	private IDomains domainsService;
	EventMailHelper mailHelper;

	public DeferredActionEventExecutor() {
		provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		domainsService = provider.instance(IDomains.class);
		mailHelper = new EventMailHelper();
	}

	@Override
	public void execute(ZonedDateTime executionDate) {
		domainsService.all().stream().filter(DeferredActionEventExecutor::isNotGlobalVirt)
				.forEach(executeForDomain(executionDate));
	}

	private Consumer<? super ItemValue<Domain>> executeForDomain(ZonedDateTime executionDate) {
		return domain -> {
			String deferredActionUid = IDeferredActionContainerUids.uidForDomain(domain.uid);
			IDeferredAction deferredActionService = provider.instance(IDeferredAction.class, deferredActionUid);
			IMailboxes mailboxesService = provider.instance(IMailboxes.class, domain.uid);
			IUserSettings userSettingsService = provider.instance(IUserSettings.class, domain.uid);

			List<ItemValue<DeferredAction>> deferredActions = deferredActionService
					.getByActionId(EventDeferredAction.ACTION_ID, executionDate.toInstant().toEpochMilli());

			logger.info("Found {} deferred actions of type {}", deferredActions.size(), EventDeferredAction.ACTION_ID);

			deferredActions.stream().map(DeferredActionEventExecutor::from).forEach(action -> {
				VertxPlatform.getVertx().setTimer(
						Math.max(1, action.value.executionDate.getTime() - new Date().getTime()),
						(timerId) -> executeAction(deferredActionService, action, mailboxesService,
								userSettingsService));
			});
		};
	}

	private void executeAction(IDeferredAction deferredActionService, ItemValue<EventDeferredAction> deferredAction,
			IMailboxes mailboxesService, IUserSettings userSettingsService) {
		try {
			ItemValue<net.bluemind.mailbox.api.Mailbox> userMailbox = mailboxesService
					.getComplete(deferredAction.value.ownerUid);

			VEvent event = deferredAction.value.vevent;
			VAlarm alarm = deferredAction.value.valarm;

			Map<String, String> userSettings = userSettingsService.get(userMailbox.uid);
			Map<String, Object> data = buildData(event, alarm, userSettings);
			logger.info("Send deferred action to {} for entity {}", userMailbox.displayName, deferredAction.uid);
			sendNotificationEmail(data, userMailbox, userSettings);
		} catch (Exception e) {
			logger.error("Impossible to send deferred action for entity: {}", deferredAction.uid, e);
		} finally {
			try {
				if (deferredAction.value.isRecurringEvent()) {
					storeTrigger(deferredAction.value, deferredActionService);
				}
			} catch (Exception e) {
				logger.error("Error when registering the next alarm trigger for entity: {}", deferredAction.uid, e);
			} finally {
				logger.info("Delete deferred action {} for {}: {}", deferredAction.value.actionId,
						deferredAction.value.executionDate, deferredAction.uid);
				deferredActionService.delete(deferredAction.uid);
			}
		}
	}

	private void storeTrigger(EventDeferredAction deferredAction, IDeferredAction service) {
		deferredAction.nextExecutionDate()
				.map(executionDate -> deferredAction.copy(Date.from(executionDate.toInstant())))
				.ifPresent(nextDeferredAction -> service.create(UUID.randomUUID().toString(), nextDeferredAction));
	}

	private void sendNotificationEmail(Map<String, Object> data, ItemValue<Mailbox> userMailbox,
			Map<String, String> userSettings) throws MimeException, IOException, TemplateException {
		Locale locale = getLocale(userSettings);
		mailHelper.send(locale, data, userMailbox);
	}

	private Map<String, Object> buildData(VEvent event, VAlarm alarm, Map<String, String> userSettings) {
		Map<String, Object> data = new CalendarMailHelper().extractVEventDataToMap(event, alarm);

		String dateFormat = getValue(userSettings, "date_format", "date", "dateformat").orElse("yyyy-MM-dd");
		String timeFormat = getValue(userSettings, "time_format", "timeformat", "time").orElse("HH:mm");
		TimeZone timezone = TimeZone.getTimeZone(userSettings.get("timezone"));

		data.put("datetime_format", dateFormat + " " + timeFormat);
		data.put("time_format", timeFormat);
		// TODO Cargo cult from net.bluemind.reminder.job.ReminderJob#sendMessage: why
		// specifying another format?
		if ("fr".equals(userSettings.get("lang"))) {
			data.put("date_format", "EEEE d MMMM yyyy");
		} else {
			data.put("date_format", "EEE, MMMM dd, yyyy");
		}

		data.put("timezone", timezone.getID());

		// TODO Cargo cult: why we put "tz" value only if timezone differs between event
		// and settings?
		if (event.timezone() != null && !event.timezone().equals(userSettings.get("timezone"))) {
			data.put("tz", timezone.getDisplayName(getLocale(userSettings)));
		}
		return data;
	}

	@SuppressWarnings("unchecked")
	static <K, T> Optional<T> getValue(Map<K, T> map, K... keys) {
		return Arrays.asList(keys).stream().filter(map::containsKey).findFirst().map(map::get);
	}

	private static boolean isNotGlobalVirt(ItemValue<Domain> domain) {
		return !"global.virt".equals(domain.value.name);
	}

	static Locale getLocale(Map<String, String> userSettings) {
		String language = userSettings.get("lang");
		if (Strings.isNullOrEmpty(language)) {
			return Locale.ENGLISH;
		}
		return new Locale(language);
	}

	public static ItemValue<EventDeferredAction> from(ItemValue<DeferredAction> deferredAction) {
		EventDeferredAction eventDeferredAction = new EventDeferredAction(deferredAction.value);
		return ItemValue.create(deferredAction.uid, eventDeferredAction);
	}
}
