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
package net.bluemind.eas.backend.bm.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.eas.backend.BackendSession;
import net.bluemind.eas.backend.MailFolder;
import net.bluemind.eas.dto.base.AirSyncBaseResponse;
import net.bluemind.eas.dto.base.BodyOptions;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.base.LazyLoaded;

public class BodyLoaderFactory {

	private static final Logger logger = LoggerFactory.getLogger(BodyLoaderFactory.class);

	private static final class MimeBodyLoader extends LazyLoaded<BodyOptions, AirSyncBaseResponse> {

		private BackendSession bs;
		private MailFolder folder;
		private int id;

		public MimeBodyLoader(BackendSession bs, MailFolder folder, int id, BodyOptions query) {
			super(query);
			this.bs = bs;
			this.folder = folder;
			this.id = id;
		}

		@Override
		public void load(Callback<AirSyncBaseResponse> onLoad) {
			if (logger.isDebugEnabled()) {
				logger.debug("Loading body for {}:{}", folder.fullName, id);
			}
			AirSyncBaseResponse asResp;
			try {
				asResp = EmailManager.getInstance().loadBody(bs, folder, id, query);
				onLoad.onResult(asResp);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				onLoad.onResult(null);
			}

		}

	}

	public static LazyLoaded<BodyOptions, AirSyncBaseResponse> from(BackendSession bs, MailFolder folder, int id,
			BodyOptions bodyOpts) {
		if (logger.isDebugEnabled()) {
			logger.debug("[{}] {}:{} Should get body loader for {}", bs.getLoginAtDomain(), folder.fullName, id,
					bodyOpts);
		}

		return new MimeBodyLoader(bs, folder, id, bodyOpts);
	}

}
