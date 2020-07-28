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
package net.bluemind.backend.postfix;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemHook;

public class SettingsHook extends DefaultServerHook implements ISystemConfigurationObserver, ISystemHook {
	Logger logger = LoggerFactory.getLogger(SettingsHook.class.getName());

	private interface SmtpAction {
		void run(ItemValue<Server> s) throws ServerFault;
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		MessageSizeValue messageSizeLimit = getMessageSizeLimit(previous, conf);
		if (!messageSizeLimit.hasChanged() && !myNetworksChanged(previous, conf) && !relayHostChanged(previous, conf)) {
			logger.info("Postfix config has not changed");
			return;
		}

		updateSmtpServers(getTaggedServers(context, TagDescriptor.mail_smtp.getTag()), conf);
		updateSmtpEdgeServers(getTaggedServers(context, TagDescriptor.mail_smtp_edge.getTag()), conf);
	}

	private boolean relayHostChanged(SystemConf previous, SystemConf conf) {
		String prevValue = null == previous.stringValue(SysConfKeys.relayhost.name()) ? ""
				: previous.stringValue(SysConfKeys.relayhost.name());
		String currentValue = null == conf.stringValue(SysConfKeys.relayhost.name()) ? ""
				: conf.stringValue(SysConfKeys.relayhost.name());
		return !prevValue.equals(currentValue);
	}

	private void updateSmtpEdgeServers(List<ItemValue<Server>> postfixs, SystemConf conf) throws ServerFault {
		for (ItemValue<Server> postfix : postfixs) {
			logger.info("Distributing new settings to {}:{}", postfix.value.name, postfix.value.ip);
			logger.info("Setting mynetworks to '{}'", conf.stringValue(SysConfKeys.mynetworks.name()));
			logger.info("Setting messageSizeLimit to '{}'", conf.stringValue(SysConfKeys.message_size_limit.name()));

			INodeClient nc = NodeActivator.get(postfix.value.address());

			TaskRef tr = nc.executeCommandNoOut("/usr/sbin/postconf -e '" + SysConfKeys.mynetworks.name() + " = "
					+ conf.stringValue(SysConfKeys.mynetworks.name()) + "'");
			NCUtils.waitFor(nc, tr);

			tr = nc.executeCommandNoOut("/usr/sbin/postconf -e '" + SysConfKeys.message_size_limit.name() + " = "
					+ conf.stringValue(SysConfKeys.message_size_limit.name()) + "'");
			NCUtils.waitFor(nc, tr);
			tr = nc.executeCommandNoOut("/usr/sbin/postconf -e 'mailbox_size_limit = "
					+ conf.stringValue(SysConfKeys.message_size_limit.name()) + "'");
			NCUtils.waitFor(nc, tr);

			tr = nc.executeCommandNoOut("service postfix restart");
			NCUtils.waitFor(nc, tr);

			if (logger.isDebugEnabled()) {
				logger.debug(new String(nc.read("/etc/postfix/main.cf")));
			}
		}
	}

	private void updateSmtpServers(List<ItemValue<Server>> postfixs, SystemConf conf) throws ServerFault {
		for (ItemValue<Server> postfix : postfixs) {
			logger.info("Distributing new settings to {}:{}", postfix.value.name, postfix.value.ip);
			logger.info("Setting mynetworks to '{}'", conf.stringValue(SysConfKeys.mynetworks.name()));
			logger.info("Setting messageSizeLimit to '{}'", conf.stringValue(SysConfKeys.message_size_limit.name()));
			logger.info("Setting relayhost to '{}'", conf.stringValue(SysConfKeys.relayhost.name()));

			INodeClient nc = NodeActivator.get(postfix.value.address());

			TaskRef tr = nc.executeCommandNoOut("/usr/sbin/postconf -e '" + SysConfKeys.mynetworks.name() + " = "
					+ conf.stringValue(SysConfKeys.mynetworks.name()) + "'");
			NCUtils.waitFor(nc, tr);

			tr = nc.executeCommandNoOut("/usr/sbin/postconf -e '" + SysConfKeys.message_size_limit.name() + " = "
					+ conf.stringValue(SysConfKeys.message_size_limit.name()) + "'");
			NCUtils.waitFor(nc, tr);
			tr = nc.executeCommandNoOut("/usr/sbin/postconf -e 'mailbox_size_limit = "
					+ conf.stringValue(SysConfKeys.message_size_limit.name()) + "'");
			NCUtils.waitFor(nc, tr);

			tr = nc.executeCommandNoOut(
					"/usr/sbin/postconf -e 'relayhost = " + (conf.stringValue(SysConfKeys.relayhost.name()) == null ? ""
							: conf.stringValue(SysConfKeys.relayhost.name()).trim()) + "'");
			NCUtils.waitFor(nc, tr);

			new PostfixService().reloadPostfix(postfix);

			if (logger.isDebugEnabled()) {
				logger.debug(new String(nc.read("/etc/postfix/main.cf")));
			}
		}
	}

	private boolean myNetworksChanged(SystemConf previous, SystemConf conf) {
		String prevValue = null == previous.stringValue(SysConfKeys.mynetworks.name()) ? ""
				: previous.stringValue(SysConfKeys.mynetworks.name());
		String currentValue = null == conf.stringValue(SysConfKeys.mynetworks.name()) ? ""
				: conf.stringValue(SysConfKeys.mynetworks.name());
		return !prevValue.equals(currentValue);
	}

	private List<ItemValue<Server>> getTaggedServers(BmContext context, String... tag) throws ServerFault {

		IServer serverService = context.provider().instance(IServer.class, "default");

		List<ItemValue<Server>> all = serverService.allComplete();
		List<ItemValue<Server>> ret = new ArrayList<>();
		for (ItemValue<Server> server : all) {
			for (int i = 0; i < tag.length; i++) {
				if (server.value.tags.contains(tag[i])) {
					ret.add(server);
				}
			}
		}
		return ret;
	}

	private MessageSizeValue getMessageSizeLimit(SystemConf previous, SystemConf conf) {
		long prevMessageSizeLimit = previous.convertedValue(SysConfKeys.message_size_limit.name(),
				val -> Long.parseLong(val), 0l);
		long messageSizeLimit = conf.convertedValue(SysConfKeys.message_size_limit.name(), val -> Long.parseLong(val),
				0l);

		return new MessageSizeValue(prevMessageSizeLimit, messageSizeLimit);
	}

	static class MessageSizeValue {
		public final long oldValue;
		public final long newValue;

		public MessageSizeValue(long oldValue, long newValue) {
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		public static MessageSizeValue create(long newValue) {
			return new MessageSizeValue(0, newValue);
		}

		public boolean isSet() {
			return newValue > 0;
		}

		private boolean hasChanged() {
			return oldValue != newValue;
		}
	}

	@Override
	public void onCertificateUpdate() throws ServerFault {
		forEachSmtp(new SmtpAction() {
			@Override
			public void run(ItemValue<Server> server) throws ServerFault {
				new PostfixService().reloadPostfix(server);
			}
		});
	}

	private void forEachSmtp(SmtpAction action) throws ServerFault {
		IServer srvApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		for (ItemValue<Server> h : srvApi.allComplete()) {
			if (h.value.tags.stream().anyMatch(SmtpTagServerHook.TAGS::contains)) {
				action.run(h);
			}
		}
	}
}
