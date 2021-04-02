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
package net.bluemind.mailshare.service.internal;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.ISanitizer;
import net.bluemind.core.sanitizer.ISanitizerFactory;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.directory.service.DirDomainValue;
import net.bluemind.mailshare.api.Mailshare;

public class MailshareVCardSanitizer implements ISanitizer<DirDomainValue<Mailshare>> {

	private BmContext bmContext;

	public MailshareVCardSanitizer(BmContext bmContext) {
		this.bmContext = bmContext;
	}

	public static final class Factory implements ISanitizerFactory<DirDomainValue<Mailshare>> {

		@SuppressWarnings("unchecked")
		@Override
		public Class<DirDomainValue<Mailshare>> support() {
			return (Class<DirDomainValue<Mailshare>>) ((Class<?>) DirDomainValue.class);
		}

		@Override
		public ISanitizer<DirDomainValue<Mailshare>> create(BmContext context, Container container) {
			return new MailshareVCardSanitizer(context);
		}

	}

	@Override
	public void create(DirDomainValue<Mailshare> obj) throws ServerFault {
		if (!(obj.value instanceof Mailshare)) {
			return;
		}
		sanitizeVCard(obj.domainUid, obj.entryUid, obj.value);

	}

	@Override
	public void update(DirDomainValue<Mailshare> current, DirDomainValue<Mailshare> obj) throws ServerFault {
		if (!(obj.value instanceof Mailshare)) {
			return;
		}

		if (obj.value.card == null) {
			obj.value.card = current.value.card;
		}

		sanitizeVCard(obj.domainUid, obj.entryUid, obj.value);
	}

	private void sanitizeVCard(String domainUid, String entryUid, Mailshare value) {
		if (value.card == null) {
			value.card = new VCard();
		}
		new Sanitizer(this.bmContext).create(value.card);
	}
}
