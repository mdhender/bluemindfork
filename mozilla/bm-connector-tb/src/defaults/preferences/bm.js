/**
 * BEGIN LICENSE
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

// Default preferences
pref("extensions.bm.server", "https://xxxx");
/* Log level
 BMLogger.TRACE = -1;
 BMLogger.DEBUG = 0;
 BMLogger.INFO = 1;
 BMLogger.ERROR = 2;
*/
pref("extensions.bm.log.level", 1);
pref("extensions.bm.log.debug", false);
/* Auto sync every x min */
pref("extensions.bm.refresh.delay", 2);
pref("extensions.install.requireBuiltInCerts", false);
pref("extensions.update.requireBuiltInCerts", false);
/* Open BlueMind in tab ?*/
pref("extensions.bm.openInTab", true);
/* Workaround DHE keys less than 1023-bit are no longer accepted
 * until server JDK updated to JAVA 8 and Tigase to 7.0
 */
pref("security.ssl3.dhe_rsa_aes_128_sha", false);
pref("security.ssl3.dhe_rsa_aes_256_sha", false);
pref("general.useragent.compatMode.firefox", true);
/* Prevent Collected contacts ab to become hidden */
pref("ldap_2.servers.history.position", 2);
/* Remote file chooser can close his window */
pref("dom.allow_scripts_to_close_windows", true);
/* Debug/Dev prefs */
/*pref("nglayout.debug.disable_xul_cache", true);
pref("nglayout.debug.disable_xul_fastload", true);
pref("javascript.options.strict", true);
pref("javascript.options.showInConsole", true);
pref("browser.dom.window.dump.enabled", true);
pref("extensions.logging.enabled", true);*/
pref("extensions.bm.dev.resetOnStart", false);
