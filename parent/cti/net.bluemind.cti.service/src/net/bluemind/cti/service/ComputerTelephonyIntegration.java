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
package net.bluemind.cti.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.cti.api.IComputerTelephonyIntegration;
import net.bluemind.cti.api.Status;
import net.bluemind.cti.api.Status.PhoneState;
import net.bluemind.cti.api.Status.Type;
import net.bluemind.cti.backend.ICTIBackend;
import net.bluemind.cti.service.internal.CTIStatusManager;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.OOPMessage;
import net.bluemind.hornetq.client.Topic;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class ComputerTelephonyIntegration implements IComputerTelephonyIntegration {

	private static final Logger logger = LoggerFactory.getLogger(ComputerTelephonyIntegration.class);

	private BmContext context;
	private String domainUid;
	private String userUid;

	private ICTIBackend backend;

	private CTIStatusManager statusManager;

	public ComputerTelephonyIntegration(BmContext context, CTIStatusManager statusManager, String domainUid,
			String userUid, ICTIBackend backend) {
		this.context = context;
		this.domainUid = domainUid;
		this.userUid = userUid;
		this.backend = backend;
		this.statusManager = statusManager;
	}

	private ItemValue<User> getUserOrFail() throws ServerFault {
		ItemValue<User> user = context.su().provider().instance(IUser.class, domainUid).getComplete(userUid);
		if (user == null) {
			throw new ServerFault("user " + userUid + " not found in domain " + domainUid);
		}
		return user;

	}

	@Override
	public void dial(String number) throws ServerFault {
		checkAccess();
		ItemValue<User> user = getUserOrFail();
		backend.dial(domainUid, user, number);

	}

	private void checkAccess() throws ServerFault {
		boolean self = context.getSecurityContext().getContainerUid().equals(domainUid)
				&& context.getSecurityContext().getSubject().equals(userUid);

		if (!self && !context.getSecurityContext().isDomainAdmin(domainUid)) {
			throw new ServerFault("Cannot dial from someone else phone", ErrorCode.PERMISSION_DENIED);
		}
	}

	@Override
	public Status getStatus() throws ServerFault {
		// FIXME every body can know status
		if (!context.getSecurityContext().getContainerUid().equals(domainUid)) {
			throw new ServerFault("Cannot retrieve status for domain " + domainUid, ErrorCode.PERMISSION_DENIED);
		}
		ItemValue<User> user = getUserOrFail();

		PhoneState phoneState = backend.getPhoneState(domainUid, user);
		if (phoneState == PhoneState.Unknown) {
			return Status.unexisting();
		}

		Status status = statusManager.getStatus(domainUid, user.uid);
		if (phoneState == PhoneState.DoNotDisturb) {
			return Status.create(Type.DoNotDisturb, phoneState, status.message);
		} else if (phoneState != PhoneState.Available) {
			return Status.create(Type.Busy, phoneState, status.message);
		} else {
			return Status.create(status.type, phoneState, status.message);
		}
	}

	@Override
	public void setStatus(String component, Status status) throws ServerFault {
		checkAccess();
		ItemValue<User> user = getUserOrFail();

		Status currentStatus = statusManager.getStatus(domainUid, user.uid);
		Type stackedStatus = statusManager.updateStatus(domainUid, user.uid, component, status).type;
		Status.PhoneState phoneState = backend.getPhoneState(domainUid, user);
		if (phoneState != PhoneState.Unknown) {
			if (stackedStatus == Type.DoNotDisturb && currentStatus.type != stackedStatus) {
				backend.dnd(domainUid, user, true);
			} else if (stackedStatus == Type.Available && currentStatus.type != Type.Available) {
				backend.dnd(domainUid, user, false);
			}
		}

		logger.debug("status is now {} (asked was {}, current was {})", stackedStatus, status.type, currentStatus.type);
		if (status.type != currentStatus.type) {

			// notify on MQ
			OOPMessage hqMsg = MQ.newMessage();

			hqMsg.putStringProperty("latd", user.value.login + "@" + domainUid);
			hqMsg.putStringProperty("status", "" + stackedStatus.code());
			hqMsg.putStringProperty("operation", "xivo.updatePhoneStatus");
			logger.debug("notify status changed for {}", user.value.login + "@" + domainUid);
			MQ.getProducer(Topic.XIVO_PHONE_STATUS).send(hqMsg);
		}

	}

	@Override
	public void forward(String component, String number) throws ServerFault {

		checkAccess();

		ItemValue<User> user = getUserOrFail();

		String forward = statusManager.updateForward(domainUid, user.uid, component, number);
		backend.forward(domainUid, user, forward);
	}
}
