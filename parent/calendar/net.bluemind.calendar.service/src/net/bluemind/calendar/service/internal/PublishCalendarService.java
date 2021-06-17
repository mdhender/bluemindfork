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
package net.bluemind.calendar.service.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.google.common.collect.Lists;

import io.vertx.core.buffer.Buffer;
import net.bluemind.calendar.api.IPublishCalendar;
import net.bluemind.calendar.api.PublishMode;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.persistence.VEventSeriesStore;
import net.bluemind.core.api.Stream;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.Classification;
import net.bluemind.icalendar.api.ICalendarElement.ParticipationStatus;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.XProperty;

public class PublishCalendarService implements IPublishCalendar {

	private VEventSeriesStore veventStore;
	private VEventContainerStoreService storeService;
	private Container container;
	private BmContext context;

	private static final String TOKEN_PREFIX = "x-calendar";
	private static final String PRIVATE_URL_PREFIX = TOKEN_PREFIX + "-private-";
	private static final String PUBLIC_URL_PREFIX = TOKEN_PREFIX + "-public-";

	public PublishCalendarService(BmContext context, DataSource ds, Container container) throws ServerFault {
		BmContext admin = context.su();

		veventStore = new VEventSeriesStore(ds, container);
		storeService = new VEventContainerStoreService(context, ds, admin.getSecurityContext(), container, veventStore);
		this.container = container;

		this.context = context;

	}

	@Override
	public String generateUrl(PublishMode mode) throws ServerFault {
		RBACManager.forSecurityContext(context.getSecurityContext()).forContainer(container).check(Verb.Manage.name());
		return setAclFor(mode, generateToken());
	}

	@Override
	public List<String> getGeneratedUrls(PublishMode mode) throws ServerFault {
		RBACManager.forSecurityContext(context.getSecurityContext()).forContainer(container).check(Verb.Manage.name());

		IContainerManagement service = context.su().provider().instance(IContainerManagement.class, container.uid);
		List<AccessControlEntry> accessControlList = service.getAccessControlList();

		return accessControlList.stream().filter(accessControlEntry -> accessControlEntry.verb == Verb.Read
				&& accessControlEntry.subject.startsWith(TOKEN_PREFIX)
				&& (accessControlEntry.subject.startsWith(PRIVATE_URL_PREFIX) && mode == PublishMode.PRIVATE
						|| accessControlEntry.subject.startsWith(PUBLIC_URL_PREFIX) && mode == PublishMode.PUBLIC))
				.map(acl -> computeUrl(mode, acl.subject)).collect(Collectors.toList());
	}

	@Override
	public void disableUrl(String url) throws ServerFault {
		RBACManager.forSecurityContext(context.getSecurityContext()).forContainer(container).check(Verb.Manage.name());

		String token = url.substring(url.lastIndexOf(TOKEN_PREFIX));
		IContainerManagement service = context.su().provider().instance(IContainerManagement.class, container.uid);
		List<AccessControlEntry> accessControlList = new ArrayList<>(service.getAccessControlList());

		List<AccessControlEntry> filtered = accessControlList.stream().filter(acl -> !acl.subject.equals(token))
				.collect(Collectors.toList());

		if (filtered.size() != accessControlList.size()) {
			service.setAccessControlList(filtered);
		}

	}

	@Override
	public Stream publish(String token) throws ServerFault {
		if (!token.startsWith(TOKEN_PREFIX)) {
			throw new ServerFault("Invalid token");
		}

		SecurityContext tok = new SecurityContext(null, token, Arrays.<String>asList(), Arrays.<String>asList(),
				Collections.emptyMap(), SecurityContext.TOKEN_FAKE_DOMAIN, "en", "token");

		RBACManager.forSecurityContext(tok).forContainer(container).check(Verb.Read.name());

		return getIcs(token.startsWith(PRIVATE_URL_PREFIX) ? PublishMode.PRIVATE : PublishMode.PUBLIC);
	}

	private String setAclFor(PublishMode mode, String token) {
		String prefix = mode == PublishMode.PUBLIC ? PUBLIC_URL_PREFIX : PRIVATE_URL_PREFIX;
		String aclSubject = String.format("%s%s", prefix, token);
		AccessControlEntry entry = AccessControlEntry.create(aclSubject, Verb.Read);

		IContainerManagement service = context.su().provider().instance(IContainerManagement.class, container.uid);
		List<AccessControlEntry> accessControlList = new ArrayList<>(service.getAccessControlList());
		accessControlList.add(entry);
		service.setAccessControlList(accessControlList);

		return this.computeUrl(mode, aclSubject);
	}

	private String computeUrl(PublishMode mode, String aclSubject) {
		String externalUrl = context.su().provider().instance(ISystemConfiguration.class).getValues().values
				.get(SysConfKeys.external_url.name());
		return String.format("https://%s/api/calendars/publish/%s/%s", externalUrl, container.uid, aclSubject);
	}

	private String generateToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * @return
	 * @throws ServerFault
	 */
	private Stream getIcs(PublishMode mode) throws ServerFault {

		List<String> allUids = storeService.allUids();
		List<List<String>> partitioned = Lists.partition(allUids, 30);
		AtomicInteger index = new AtomicInteger(0);

		GenericStream<String> stream = new GenericStream<String>() {

			@Override
			protected StreamState<String> next() throws Exception {
				if (index.get() == partitioned.size()) {
					return StreamState.end();
				}
				List<String> uids = partitioned.get(index.get());
				List<ItemValue<VEventSeries>> events = storeService.getMultiple(uids).stream() //
						.map(event -> {
							if (mode == PublishMode.PUBLIC) {
								if (event.value.main != null
										&& event.value.main.classification != Classification.Public) {
									event.value.main = event.value.main.filtered();
									event.value.occurrences = event.value.occurrences.stream().map(occurrence -> {
										return occurrence.filtered();
									}).collect(Collectors.toList());
								}
							}
							return event;
						}).filter(event -> {
							if (event.value.main != null) {
								List<Attendee> attendees = event.value.main.attendees;
								for (Attendee a : attendees) {
									if (("bm://" + container.domainUid + "/users/" + container.owner).equals(a.dir)) {
										if (a.partStatus == ParticipationStatus.Declined) {
											return false;
										}
									}
								}
							}
							return true;
						}).collect(Collectors.toList());
				String ics = VEventServiceHelper.convertToIcsWithProperty(Method.PUBLISH, events,
						new XProperty("X-WR-CALNAME", container.name));
				if (index.get() != 0) {
					ics = IcsUtil.stripHeader(ics);
				}
				if (index.get() < partitioned.size() - 1) {
					ics = IcsUtil.stripFooter(ics);
				}
				index.incrementAndGet();
				return StreamState.data(ics);
			}

			@Override
			protected Buffer serialize(String n) throws Exception {
				return Buffer.buffer(n.getBytes());
			}

		};

		return VertxStream.stream(stream);
	}

	@Override
	public String createUrl(PublishMode mode, String token) throws ServerFault {
		RBACManager.forSecurityContext(context.getSecurityContext()).forContainer(container).check(Verb.Manage.name());
		String sanitizedToken;
		try {
			sanitizedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString());
			return setAclFor(mode, sanitizedToken);
		} catch (UnsupportedEncodingException e) {
			throw new ServerFault(e);
		}
	}
}
