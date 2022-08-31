/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.lmtp.filter.imip;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.file.OpenOptions;
import net.bluemind.attachment.api.AttachedFile;
import net.bluemind.attachment.api.IAttachment;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.core.api.Stream;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.imip.parser.IMIPInfos.Cid;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class EventAttachmentHandler {

	private final IServiceProvider provider;
	private final String coreUrl;
	private Optional<Boolean> canAttach;

	public EventAttachmentHandler(IServiceProvider provider, String coreUrl) {
		this.provider = provider;
		this.coreUrl = coreUrl;
		this.canAttach = Optional.empty();
	}

	private static final Logger logger = LoggerFactory.getLogger(EventAttachmentHandler.class);

	public void detachCidAttachments(VEventSeries series, List<ItemValue<VEventSeries>> existingSeries,
			Map<String, Cid> cids, Optional<String> userLogin, String domain) {

		if (!userLogin.isPresent()) {
			logger.debug("Cannot detach event attachments. Unresolved recipient user login");
			return;
		}

		try {
			series.flatten().forEach(event -> {
				event.attachments.forEach(att -> {
					if (att.cid != null && (att.publicUrl == null || att.publicUrl.equals(att.cid))) {
						Optional<AttachedFile> attachedFile = findByCid(att.cid, existingSeries);
						if (!attachedFile.isPresent()) {
							if (canAttach(userLogin.get(), domain)) {
								attachedFile = detach(att.cid, cids, userLogin.get(), domain);
							} else {
								logger.info(
										"Cannot detach event attachments. User {}@{} is missing required role canRemoteAttach",
										userLogin, domain);
							}
						}
						attachedFile.ifPresent(file -> {
							att.publicUrl = file.publicUrl;
							att.name = file.name;
						});

					}
				});
			});
		} catch (Exception e) {
			logger.warn("Cannot detach event attachments: {}", e.getMessage(), e);
		}

	}

	private boolean canAttach(String userLogin, String domain) {
		if (!canAttach.isPresent()) {
			IUser service = provider.instance(IUser.class, domain);
			ItemValue<User> user = service.byLogin(userLogin);
			canAttach = Optional.of(service.getResolvedRoles(user.uid).contains("canRemoteAttach"));
		}
		return canAttach.get();
	}

	private Optional<AttachedFile> detach(String cid, Map<String, Cid> cids, String userLogin, String domain) {
		Optional<Cid> data = findDataByCid(cid, cids);
		return data.map(cidValue -> {
			String authKey = provider.instance(IAuthentication.class).su(userLogin + "@" + domain).authKey;
			ClientSideServiceProvider userProvider = ClientSideServiceProvider.getProvider(coreUrl, authKey);
			IAttachment attachApi = userProvider.instance(IAttachment.class, domain);
			Stream stream = VertxStream.stream(VertxPlatform.getVertx().fileSystem()
					.openBlocking(new File(cidValue.tmpFile).getAbsolutePath(), new OpenOptions()));
			String url = attachApi.share(cid, stream).publicUrl;
			AttachedFile file = new AttachedFile();
			file.name = cidValue.name;
			file.publicUrl = url;
			file.cid = cid;
			return file;
		});
	}

	private Optional<Cid> findDataByCid(String cid, Map<String, Cid> cids) {
		cid = cid.replaceFirst("(?i)cid:", "");
		cid = "<" + cid + ">";
		if (cids.containsKey(cid)) {
			return Optional.of(cids.get(cid));
		}
		return Optional.empty();
	}

	private Optional<AttachedFile> findByCid(String cid, List<ItemValue<VEventSeries>> existingSeries) {
		for (ItemValue<VEventSeries> series : existingSeries) {
			for (VEvent event : series.value.flatten()) {
				for (AttachedFile attachment : event.attachments) {
					if (attachment.cid != null && attachment.cid.equals(cid)) {
						return Optional.ofNullable(attachment);
					}
				}
			}
		}

		return Optional.empty();
	}

}
