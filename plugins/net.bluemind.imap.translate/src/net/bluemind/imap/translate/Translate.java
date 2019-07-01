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
package net.bluemind.imap.translate;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.imap.translate.impl.DummyTranslation;
import net.bluemind.imap.translate.impl.FileTranslation;
import net.bluemind.imap.translate.impl.Translation;

public final class Translate {

	private static final ConcurrentHashMap<String, Translation> trans = new ConcurrentHashMap<String, Translation>();
	private static final Logger logger = LoggerFactory.getLogger(Translate.class);

	public static String toImap(String lang, String downstreamFolder) {
		return get(lang).toImap(downstreamFolder);
	}

	public static String toUser(String lang, String imapFolder) {
		String ret = get(lang).toUser(imapFolder);
		logger.debug("'{}' in {} => {}", imapFolder, lang, ret);
		return ret;
	}

	private static Translation get(String lang) {
		Translation ret = trans.get(lang);
		if (ret == null) {

			File f = new File("/etc/bm/imap.i18n." + lang);
			if (!f.exists()) {
				f = new File("/usr/share/bm-conf/i18n/imap.i18n." + lang);
			}

			if (!f.exists()) {
				ret = new DummyTranslation();
			} else {
				try {
					ret = new FileTranslation(f);
					logger.info("Using file translation for {}", lang);
					trans.put(lang, ret);
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					ret = new DummyTranslation();
				}
			}
		}
		return ret;
	}

	/**
	 * @param lang
	 * @param name
	 * @return
	 */
	public static boolean isTranslated(String lang, String name) {
		return get(lang).isTranslated(name);
	}
}
