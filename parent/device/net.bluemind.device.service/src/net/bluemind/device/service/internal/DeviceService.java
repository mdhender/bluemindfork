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
package net.bluemind.device.service.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.device.api.WipeMode;
import net.bluemind.role.api.BasicRoles;

// FIXME zero check !! ( sanitizer, validator, check if device exists etc...)
public class DeviceService implements IDevice {

	private DeviceStoreService storeService;
	private DeviceEventProducer eventProducer;
	private RBACManager rbacManager;
	private BmContext context;

	private String userUid;

	public DeviceService(BmContext context, Container container, String userUid) throws ServerFault {
		this.userUid = userUid;
		this.context = context;

		storeService = new DeviceStoreService(context.getDataSource(), context.getSecurityContext(), container);

		rbacManager = new RBACManager(context).forContainer(container.uid).forEntry(userUid);
		eventProducer = new DeviceEventProducer();
	}

	@Override
	public void create(String uid, Device device) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);
		storeService.create(uid, device.identifier, device);
	}

	@Override
	public void update(String uid, Device device) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);
		storeService.update(uid, device.identifier, device);
	}

	@Override
	public void delete(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);
		doDelete(uid);
	}

	private void doDelete(String uid) throws ServerFault {
		storeService.delete(uid);
		eventProducer.deleted(uid);
	}

	private ItemValue<Device> getOrFail(String uid) throws ServerFault {
		ItemValue<Device> device = storeService.get(uid, null);
		if (device == null) {
			throw new ServerFault("device " + uid + " for user " + userUid + " not found", ErrorCode.NOT_FOUND);
		}
		return device;
	}

	@Override
	public void deleteAll() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);
		List<String> uids = storeService.allUids();
		for (String uid : uids) {
			doDelete(uid);
		}
	}

	@Override
	public ItemValue<Device> getComplete(String uid) throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGE_USER_DEVICE);
		return storeService.get(uid, null);
	}

	@Override
	public ListResult<ItemValue<Device>> list() throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ListResult<ItemValue<Device>> ret = new ListResult<>();

		List<String> allUids = storeService.allUids();
		List<ItemValue<Device>> values = new ArrayList<>(allUids.size());

		for (String uid : allUids) {
			values.add(storeService.get(uid, null));
		}
		ret.total = values.size();
		ret.values = values;

		return ret;
	}

	@Override
	public void wipe(String uid, WipeMode mode) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ItemValue<Device> item = getOrFail(uid);

		if (mode == WipeMode.AccountOnlyRemoteWipe && item.value.protocolVersion < 16.1d) {
			throw new ServerFault("Wipe mode AccountOnlyRemoteWipe is not supported in protocol version "
					+ item.value.protocolVersion);
		}

		item.value.isWiped = true;
		item.value.wipeMode = mode;
		item.value.wipeDate = new Date();
		item.value.wipeBy = context.getSecurityContext().getSubject();

		update(uid, item.value);

		eventProducer.wipe(item.value.identifier);

	}

	@Override
	public void unwipe(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ItemValue<Device> item = getOrFail(uid);
		item.value.isWiped = false;
		item.value.wipeMode = null;
		item.value.unwipeDate = new Date();
		item.value.unwipeBy = context.getSecurityContext().getSubject();

		update(uid, item.value);

		eventProducer.unwipe(item.value.identifier);

	}

	@Override
	public void setPartnership(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ItemValue<Device> item = getOrFail(uid);
		item.value.hasPartnership = true;
		storeService.update(uid, item.value.identifier, item.value);

	}

	@Override
	public void unsetPartnership(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ItemValue<Device> item = getOrFail(uid);
		item.value.hasPartnership = false;
		storeService.update(uid, item.value.identifier, item.value);

	}

	@Override
	public void updateLastSync(String uid) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);

		ItemValue<Device> item = getOrFail(uid);
		item.value.lastSync = new Date();
		storeService.update(uid, item.value.identifier, item.value);

	}

	@Override
	public ItemValue<Device> byIdentifier(String identifier) throws ServerFault {
		rbacManager.check(Verb.Read.name(), BasicRoles.ROLE_MANAGE_USER_DEVICE);
		return storeService.byIdentifier(identifier);
	}

	@Override
	public Device get(String uid) {
		ItemValue<Device> item = getComplete(uid);
		return item != null ? item.value : null;
	}

	@Override
	public void restore(ItemValue<Device> deviceItem, boolean isCreate) {
		rbacManager.check(BasicRoles.ROLE_MANAGE_USER_DEVICE);
		if (isCreate) {
			storeService.create(deviceItem.item(), deviceItem.value);
		} else {
			storeService.update(deviceItem.item(), deviceItem.value.identifier, deviceItem.value);
		}
	}

}
