/**
 * BEGIN LICENSE
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

// WebExtension entry point

function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {

  // setup ConversionHelper
  await browser.ConversionHelper.registerChromeUrl([["content", "bm", "content/"]]);
  await browser.ConversionHelper.registerApiFolder("chrome://bm/content/api/ConversionHelper/");

  await browser.ConversionHelper.setOverlayVerbosity(9);

  // register and activate overlays
  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/messengercompose/messengercompose.xhtml", "chrome://bm/content/compose.xul");
  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/messenger.xhtml", "chrome://bm/content/messenger.xul");

  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/addressbook/addressbook.xhtml", "chrome://bm/content/addressbook.xul");
  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/addressbook/abNewCardDialog.xhtml", "chrome://bm/content/abNewCard.xul");
  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/addressbook/abEditCardDialog.xhtml", "chrome://bm/content/abCard.xul");
  await browser.ConversionHelper.registerOverlay("chrome://messenger/content/addressbook/abEditListDialog.xhtml", "chrome://bm/content/abList.xul");

  await browser.DefaultPrefsApi.setExtensionDefaultPrefs();

  // wait for thunderbird to be started to be able to register xpcoms
  await sleep(1000);

  // init XPCOM components
  await browser.LoggerApi.init();
  await browser.RPCClientApi.init();
  await browser.AutocompleteApi.init();

  // register a script which is called upon add-on unload
  await browser.ConversionHelper.registerUnloadScript("chrome://bm/content/unload.js");

  // activate all registered overlays
  await browser.ConversionHelper.activateOverlays();

  await browser.ConversionHelper.notifyStartupCompleted();
}

main();