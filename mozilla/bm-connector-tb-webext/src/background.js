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

async function waitForLoad() {
	let windows = await browser.windows.getAll({windowTypes:["normal"]});
	if (windows.length > 0) {
		return false;
	}
	
	return new Promise(function(resolve, reject) {
		function listener() {
			browser.windows.onCreated.removeListener(listener);
			resolve(true);
		}
		browser.windows.onCreated.addListener(listener);
	});
}

async function getTbirdVersion() {
  let info = await messenger.runtime.getBrowserInfo();
  return parseInt(info.version.split(".")[0]);
}

let searchBooks = [];
let searchBooksListeners = [];
let tbirdVersion;

async function main() {

  tbirdVersion = await getTbirdVersion();

  // setup ConversionHelper
  await messenger.WindowListener.registerChromeUrl([["content", "bm", "content/"]]);
  await messenger.ConversionHelper.registerChromeUrl([["content", "bm", "content/"]]);
  await messenger.ConversionHelper.registerApiFolder("chrome://bm/content/api/ConversionHelper/");

  // register and activate overlays
  await messenger.WindowListener.registerWindow("chrome://messenger/content/messengercompose/messengercompose.xhtml", "chrome://bm/content/compose-inject.js");
  await messenger.WindowListener.registerWindow("chrome://messenger/content/messenger.xhtml", "chrome://bm/content/messenger-inject.js");

  await messenger.WindowListener.registerWindow("chrome://messenger/content/addressbook/addressbook.xhtml", "chrome://bm/content/addressbook-inject.js");
  await messenger.WindowListener.registerWindow("chrome://messenger/content/addressbook/abNewCardDialog.xhtml", "chrome://bm/content/abNewCard-inject.js");
  await messenger.WindowListener.registerWindow("chrome://messenger/content/addressbook/abEditCardDialog.xhtml", "chrome://bm/content/abEditCard-inject.js");
  await messenger.WindowListener.registerWindow("chrome://messenger/content/addressbook/abEditListDialog.xhtml", "chrome://bm/content/abList-inject.js");

  await messenger.DefaultPrefsApi.setExtensionDefaultPrefs();

  // wait for thunderbird to be started to be able to register xpcoms
  await waitForLoad();

  // init XPCOM components
  await messenger.LoggerApi.init();
  await messenger.RPCClientApi.init();
  await messenger.AutocompleteApi.init();
  await messenger.BmDirWrapperApi.init();

  // register a script which is called upon add-on unload
  await messenger.WindowListener.registerShutdownScript("chrome://bm/content/unload.js");

  // load list of remote search books
  let stored = await messenger.storage.local.get("searchBooks");
  searchBooks = stored.searchBooks ?? [];
  
  await messenger.WindowListener.startListening();
}

main();

messenger.NotifyTools.onNotifyBackground.addListener(async (info) => {
  switch (info.command) {
    case "openTab":
      let newTab = await messenger.tabs.create({active: !info.openInBackGround, url: info.url});
      return newTab;
    case "getBmTab":
      let bmTabs = await messenger.tabs.query({url: info.matchUrl});
      return bmTabs;
    case "activateTab":
      let activeTab = await messenger.tabs.query({url: info.matchUrl});
      return await messenger.tabs.update(activeTab[0].id, {active: true});
    case "closeTab":
      await messenger.tabs.remove(info.tabId);
      return true;
    case "startAbListening":
      isAbListening = true;
      if (tbirdVersion >= 91) {
        enableSearchBooks();
      }
      break; 
    case "stopAbListening":
      isAbListening = false;
      if (tbirdVersion >= 91) {
        disableSearchBooks();
      }
      break;
    case "injectCalTabScript":
      let calTab = await messenger.tabs.query({url: info.matchUrl});
      await messenger.tabs.executeScript(calTab[0].id, {
        file: "/content/calTabScript.js",
        allFrames: true
      });
      return true;
    case "i18n":
      let msg = messenger.i18n.getMessage("extensions.bm." + info.key);
      return msg;
    case "openPopup":
      await browser.windows.create({
        width: 800,
        height: 500,
        type: "popup",
        allowScriptsToClose: true,
        url: info.url
      });
      break;
    case "setupSearchBooks":
      if (tbirdVersion >= 91) {
        setupSearchBooks(info.books);
      }
      break;
  }
});

let isAbListening = false;

// contacts
messenger.contacts.onCreated.addListener(async (node, id) => {
  if (!isAbListening) {
    return;
  }
  console.log("contact created:", node);
  await messenger.contacts.update(node.id, {bmNewId: node.id});
  await messenger.NotifyTools.notifyExperiment({command: "onContactCreated", contact: node});
});

messenger.contacts.onUpdated.addListener(async (node) => {
  if (!isAbListening) {
    return;
  }
  console.log("contact updated:", node);
  await messenger.NotifyTools.notifyExperiment({command: "onContactUpdated", contact: node});
});

messenger.contacts.onDeleted.addListener(async (parentId, id) => {
  if (!isAbListening) {
    return;
  }
  console.log("contact deleted:" + id + " in parent:" + parentId);
  await messenger.NotifyTools.notifyExperiment({command: "onContactDeleted", contact: {parentId: parentId, id: id}});
});

// mailing lists
messenger.mailingLists.onCreated.addListener(async (node) => {
  if (!isAbListening) {
    return;
  }
  console.log("mail list created:", node);
  await messenger.NotifyTools.notifyExperiment({command: "onListCreated", list: node});
});

messenger.mailingLists.onUpdated.addListener(async (node) => {
  if (!isAbListening) {
    return;
  }
  console.log("mail list updated:", node);
  await messenger.NotifyTools.notifyExperiment({command: "onListUpdated", list: node});
});

messenger.mailingLists.onDeleted.addListener(async (parentId, id) => {
  if (!isAbListening) {
    return;
  }
  console.log("mail list deleted:" + id + " in parent:" + parentId);
  await messenger.NotifyTools.notifyExperiment({command: "onListDeleted", list: {parentId: parentId, id: id}});
});

// mailing lists members
messenger.mailingLists.onMemberAdded.addListener(async (node) => {
  if (!isAbListening) {
    return;
  }
  console.log("mail list member added:", node);
  let list = await messenger.mailingLists.get(node.parentId);
  await messenger.NotifyTools.notifyExperiment({command: "onListUpdated", list: {parentId: list.parentId, id: list.id}});
});

messenger.mailingLists.onMemberRemoved.addListener(async (parentId, id) => {
  if (!isAbListening) {
    return;
  }
  console.log("mail list member removed:" + id + " in:" + parentId);
  let list = await messenger.mailingLists.get(parentId);
  await messenger.NotifyTools.notifyExperiment({command: "onListUpdated", list: {parentId: list.parentId, id: list.id}});
});

let bmCalNotificationId = "bm-cal-notif";

// commands from injected content scripts
messenger.runtime.onMessage.addListener(async (message) => {
  console.log("message received:", message);
  switch (message.type) {
    case "notification":
      messenger.notifications.create(bmCalNotificationId, {
        type: "basic",
        title: messenger.i18n.getMessage("extensions.bm.notification.title"),
        message: message.text,
        iconUrl: "/content/skin/BM_Icone01_16.png"
      });
      break;
    case "remotechooser-success":
      await messenger.NotifyTools.notifyExperiment({command: "onRemotechooserSuccess", files: message.files});
      break;
  }
});

messenger.notifications.onClicked.addListener(async (id) => {
  console.log("notification clicked", id);
  if (id == bmCalNotificationId) {
    let win = await messenger.windows.getAll({windowTypes: ["normal"]});
    if (win && win.length > 0) {
      await messenger.windows.update(win[0].id, {focused: true});
      let calTab = await messenger.tabs.query({url: ["https://*/cal/*", "https://*/webapp/calendar/*"]});
      if (calTab && calTab.length > 0) {
        await messenger.tabs.update(calTab[0].id, {active: true});
      }
    }
  }
});

function setupSearchBooks(books) {
  searchBooks = books;
  messenger.storage.local.set({ searchBooks: books });
}

async function searchBookListener(searchString, id) {
  console.log("search [" + searchString + "] in:" + id);
  let matches = await messenger.NotifyTools.notifyExperiment({
    command: "onSearchBook",
    search: {
      id: id,
      searchString: searchString
    }
  });
  return {
    isCompleteResult: true,
    results: matches
  }
}

function enableSearchBooks() {
  searchBooksListeners = [];
  for (let book of searchBooks) {
    let listener = async function(node, searchString, query) {
      return searchBookListener(searchString, book.id);
    };
    searchBooksListeners.push({id: book.id, listener: listener});
    messenger.addressBooks.provider.onSearchRequest.addListener(listener, {
      addressBookName: book.name,
      isSecure: true
    });
  }
}

function disableSearchBooks() {
  for (let listen of searchBooksListeners) {
    messenger.addressBooks.provider.onSearchRequest.removeListener(listen.listener);
  }
}