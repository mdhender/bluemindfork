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
package net.bluemind.addressbook.service.internal;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBooks;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.addressbook.api.VCardQuery.OrderBy;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerSubscriptionDescriptor;
import net.bluemind.core.container.model.ItemContainerValue;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.user.api.IUserSubscription;

public class AddressBooksService implements IAddressBooks {

	private static final Logger logger = LoggerFactory.getLogger(AddressBooksService.class);

	private SecurityContext securityConext;
	private BmContext context;

	public AddressBooksService(BmContext context) {
		this.context = context;
		this.securityConext = context.getSecurityContext();
	}

	@Override
	public ListResult<ItemContainerValue<VCardInfo>> search(VCardQuery query) throws ServerFault {

		List<ContainerSubscriptionDescriptor> containers = context.getServiceProvider()
				.instance(IUserSubscription.class, context.getSecurityContext().getContainerUid())
				.listSubscriptions(securityConext.getSubject(), IAddressBookUids.TYPE);

		// make container order pertinant too
		if (query.orderBy == OrderBy.Pertinance) {
			Collections.sort(containers, (a, b) -> {
				// domain addressbook first
				int ret = -Boolean.compare(a.owner.equals(securityConext.getContainerUid()),
						b.owner.equals(securityConext.getContainerUid()));
				if (ret != 0) {
					return ret;
				}

				// then user container
				ret = -Boolean.compare(a.owner.equals(securityConext.getSubject()),
						b.owner.equals(securityConext.getSubject()));
				if (ret != 0) {
					return ret;
				}

				// finally default container
				return -Boolean.compare(a.defaultContainer, b.defaultContainer);
			});
		}
		List<ItemContainerValue<VCardInfo>> res = new LinkedList<>();
		int total = 0;
		for (ContainerSubscriptionDescriptor container : containers) {

			try {
				IAddressBook service = context.getServiceProvider().instance(IAddressBook.class,
						container.containerUid);

				ListResult<ItemValue<VCardInfo>> vcards = service.search(query);
				for (ItemValue<VCardInfo> vcardInfo : vcards.values) {
					if (vcardInfo != null) {
						ItemContainerValue<VCardInfo> v = adaptAsItemContainerValue(container.containerUid, vcardInfo);
						res.add(v);
					}
				}
				total += vcards.total;
			} catch (ServerFault e) {
				if (e.getCode() == ErrorCode.PERMISSION_DENIED) {
					logger.warn("user {} try to access {} but doesnt have right",
							securityConext.getSubject() + "@" + securityConext.getContainerUid(),
							container.containerUid);
				} else {
					throw e;
				}
			}
			if (query.size > 0 && res.size() >= query.size) {
				break;
			}
		}

		if (query.orderBy != OrderBy.Pertinance) {
			Collections.sort(res, new Comparator<ItemContainerValue<VCardInfo>>() {

				@Override
				public int compare(ItemContainerValue<VCardInfo> o1, ItemContainerValue<VCardInfo> o2) {
					return o1.displayName.compareTo(o2.displayName);
				}
			});
		}
		ListResult<ItemContainerValue<VCardInfo>> ret = new ListResult<>();
		ret.values = res;
		ret.total = total;
		return ret;
	}

	private ItemContainerValue<VCardInfo> adaptAsItemContainerValue(String containerUid, ItemValue<VCardInfo> item) {

		ItemContainerValue<VCardInfo> ret = new ItemContainerValue<>();
		ret.created = item.created;
		ret.updated = item.updated;
		ret.createdBy = item.createdBy;
		ret.updatedBy = item.updatedBy;
		ret.uid = item.uid;
		ret.version = item.version;
		ret.externalId = item.externalId;
		ret.displayName = item.displayName;
		ret.value = item.value;
		ret.containerUid = containerUid;
		return ret;
	}

}
