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

import org.joda.time.DateTime;

import net.bluemind.calendar.api.IFreebusyUids;
import net.bluemind.calendar.api.IPublicFreebusy;
import net.bluemind.calendar.api.IVFreebusy;
import net.bluemind.calendar.api.VFreebusy;
import net.bluemind.calendar.api.VFreebusyQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.date.BmDateTime.Precision;
import net.bluemind.core.api.date.BmDateTimeWrapper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;

public class PublicFreebusyService implements IPublicFreebusy {

	private BmContext context;

	public PublicFreebusyService(BmContext context) {
		this.context = context.su();
	}

	@Override
	public String simple(String email, String callerUserUid, String callerDomain) throws ServerFault {
		DateTime start = DateTime.now().minusMonths(1);
		DateTime end = DateTime.now().plusMonths(2);

		return getAsString(email, callerUserUid, callerDomain,
				VFreebusyQuery.create(BmDateTimeWrapper.fromTimestamp(start.getMillis(), null, Precision.Date),
						BmDateTimeWrapper.fromTimestamp(end.getMillis(), null, Precision.Date)));
	}

	private ItemValue<DirEntry> getDirEntry(String email) throws ServerFault {

		String[] splittedEmail = email.split("@");
		String domain = splittedEmail[1];

		ItemValue<Domain> dom = context.provider().instance(IDomains.class).findByNameOrAliases(domain);
		if (null == dom) {
			// skip checking external email
			return null;
		}

		ListResult<ItemValue<DirEntry>> entries = context.provider().instance(IDirectory.class, dom.uid)
				.search(DirEntryQuery.filterEmail(email));
		if (entries.total == 1) {
			return entries.values.get(0);
		}

		return null;

	}

	@Override
	public VFreebusy get(String email, String callerUserUid, String callerDomain, VFreebusyQuery query)
			throws ServerFault {
		ParametersValidator.nullOrNotEmpty(email);
		ParametersValidator.nullOrNotEmpty(callerUserUid);
		ParametersValidator.nullOrNotEmpty(callerDomain);

		BmContext callerContext = context.su(callerUserUid, callerDomain);
		ItemValue<DirEntry> dirEntry = getDirEntry(email);
		if (dirEntry == null) {
			// TODO is it good enough ?
			return null;
		}

		IVFreebusy freebusy = callerContext.provider().instance(IVFreebusy.class,
				IFreebusyUids.getFreebusyContainerUid(dirEntry.uid));

		return freebusy.get(query);
	}

	@Override
	public String getAsString(String email, String callerUserUid, String callerDomain, VFreebusyQuery query)
			throws ServerFault {
		ParametersValidator.nullOrNotEmpty(email);
		ParametersValidator.nullOrNotEmpty(callerUserUid);
		ParametersValidator.nullOrNotEmpty(callerDomain);

		BmContext callerContext = context.su(callerUserUid, callerDomain);
		ItemValue<DirEntry> dirEntry = getDirEntry(email);
		if (dirEntry == null) {
			// TODO is it good enough ?
			return null;
		}

		IVFreebusy freebusy = callerContext.provider().instance(IVFreebusy.class,
				IFreebusyUids.getFreebusyContainerUid(dirEntry.uid));

		return freebusy.getAsString(query);
	}

}
