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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.icalendar.api.ICalendarElement.Attendee;
import net.bluemind.icalendar.api.ICalendarElement.CUType;

public class VCardAttendeeVEventSanitizer implements ISanitizer<VEvent> {

	private static final Logger logger = LoggerFactory.getLogger(VCardAttendeeVEventSanitizer.class);

	public static class Factory implements ISanitizerFactory<VEvent> {

		@Override
		public Class<VEvent> support() {
			return VEvent.class;
		}

		@Override
		public ISanitizer<VEvent> create(BmContext context, Container container) {
			return new VCardAttendeeVEventSanitizer(context);
		}

	}

	private BmContext ctx;

	public VCardAttendeeVEventSanitizer(BmContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void create(VEvent obj) throws ServerFault {
		sanitize(obj);
	}

	@Override
	public void update(VEvent current, VEvent obj) throws ServerFault {
		sanitize(obj);
	}

	public void sanitize(VEvent vevent) throws ServerFault {

		if (vevent.attendees == null) {
			return;
		}

		List<VEvent.Attendee> ret = new ArrayList<>(vevent.attendees.size());

		for (VEvent.Attendee attendee : vevent.attendees) {
			ItemContainerValue<VCard> vcard = resovleVCard(attendee);

			if (vcard == null) {
				attendee.uri = null;
				ret.add(attendee);
			} else {
				if (vcard.value.kind == Kind.group) {
					ret.addAll(expandDList(vcard, attendee));
				} else {
					ret.add(asAttendee(vcard, attendee));
				}
			}
		}

		vevent.attendees = ret;

	}

	private Collection<? extends Attendee> expandDList(ItemContainerValue<VCard> vcard, Attendee attendee) {
		List<ItemContainerValue<VCard>> vcards = expand(vcard);
		List<Attendee> ret = new ArrayList<>(vcards.size());
		for (ItemContainerValue<VCard> card : vcards) {
			ret.add(asAttendee(card, attendee));
		}
		return ret;
	}

	private List<ItemContainerValue<VCard>> expand(ItemContainerValue<VCard> vcard) {
		// TODO only handle one level
		List<ItemContainerValue<VCard>> ret = new ArrayList<>(vcard.value.organizational.member.size());
		for (net.bluemind.addressbook.api.VCard.Organizational.Member m : vcard.value.organizational.member) {
			ItemValue<VCard> mvcard = null;
			try {
				IAddressBook ab = ctx.provider().instance(IAddressBook.class, m.containerUid);
				if (ab != null) {
					mvcard = ab.getComplete(m.itemUid);
				}
			} catch (ServerFault e) {
				logger.debug("error retrieving vcard {}@{} : {}", m.itemUid, m.containerUid, e.getMessage());
			}

			if (mvcard != null && mvcard.value.kind != VCard.Kind.group) {
				ret.add(ItemContainerValue.create(m.containerUid, mvcard, mvcard.value));
			}
		}
		return ret;
	}

	private ItemContainerValue<VCard> resovleVCard(Attendee attendee) {
		if (attendee.dir != null && !attendee.dir.isEmpty()) {
			return null;
		}
		if (attendee.uri == null) {
			return null;
		}
		String[] uri = attendee.uri.split("/");
		if (uri.length != 2) {
			return null;
		}
		ItemValue<VCard> vcard = null;
		try {
			IAddressBook ab = ctx.provider().instance(IAddressBook.class, uri[0]);
			if (ab != null) {
				vcard = ab.getComplete(uri[1]);
			}
		} catch (ServerFault e) {
			logger.debug("error retrieving vcard {}@{} : {}", uri[1], uri[0], e.getMessage());
		}

		if (vcard != null) {
			return ItemContainerValue.create(uri[0], vcard, vcard.value);
		} else {
			return null;
		}
	}

	private Attendee asAttendee(ItemContainerValue<VCard> m, Attendee attendee) {
		Attendee ret = new VEvent.Attendee();
		ret.uri = m.containerUid + "/" + m.uid;
		ret.mailto = m.value.defaultMail();
		ret.cutype = CUType.Individual;
		ret.commonName = m.value.identification.formatedName.value;
		ret.partStatus = attendee.partStatus;
		ret.role = attendee.role;
		ret.rsvp = attendee.rsvp;
		ret.responseComment = attendee.responseComment;
		ret.internal = attendee.internal;
		ret.sentBy = attendee.sentBy;
		ret.delFrom = attendee.delFrom;
		ret.delTo = attendee.delTo;
		ret.member = attendee.member;
		ret.dir = attendee.dir;
		ret.lang = attendee.lang;
		return ret;
	}

}
