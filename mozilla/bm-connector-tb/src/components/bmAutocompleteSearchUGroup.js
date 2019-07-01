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

/* Bm autocomplete source */
Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
Components.utils.import("resource://gre/modules/Services.jsm");
if (ChromeUtils.generateQI) {
    //TB >= 65
    Components.utils.import("resource:///modules/MailServices.jsm");
} else {
    Components.utils.import("resource:///modules/mailServices.js");
}
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/core2/BMAuthService.jsm");

const ACR = Components.interfaces.nsIAutoCompleteResult;
const nsIAbAutoCompleteResult = Components.interfaces.nsIAbAutoCompleteResult;

function BmAutocompleteResultUGroup(aSearchString) {
    this._searchResults = new Array();
    this.searchString = aSearchString;
}

BmAutocompleteResultUGroup.prototype = {
    QueryInterface: ChromeUtils.generateQI ? 
        ChromeUtils.generateQI([Components.interfaces.nsIAbAutoCompleteResult])
        : XPCOMUtils.generateQI([Components.interfaces.nsIAbAutoCompleteResult]),
    _searchResults: null,
    
    // nsIAutoCompleteResult
    searchString: null,
    searchResult: ACR.RESULT_NOMATCH,
    defaultIndex: -1,
    errorDescription: null,
    
    get matchCount() {
        return this._searchResults.length;
    },
    getValueAt: function getValueAt(aIndex) {
        return this._searchResults[aIndex].getUid();
    },
    getLabelAt: function getLabelAt(aIndex) {
        return this._searchResults[aIndex].toString();
    },
    getCommentAt: function getCommentAt(aIndex) {
        return "";
    },
    getStyleAt: function getStyleAt(aIndex) {
        return "";
    },
    getImageAt: function getImageAt(aIndex) {
        return "";
    },
    getFinalCompleteValueAt: function(aIndex) {
        return this.getValueAt(aIndex);
    },
    removeValueAt: function removeValueAt(aRowIndex, aRemoveFromDB) {},
};

function BmAutocompleteSearchUGroup() {
  let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
  loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");
  loader.loadSubScript("chrome://bm/content/core2/client/DirectoryClient.js");
}

BmAutocompleteSearchUGroup.prototype = {
    classDescription: "BmAutocompleteSearchUGroup XPCOM Component",
    classID: Components.ID("{9b88bbcc-9f6f-4f1a-ba61-6b2589c16094}"),
    contractID: "@mozilla.org/autocomplete/search;1?name=bm-search-ugroup",
    QueryInterface: ChromeUtils.generateQI ?
        ChromeUtils.generateQI([Components.interfaces.nsIAutoCompleteSearch])
        : XPCOMUtils.generateQI([Components.interfaces.nsIAutoCompleteSearch]),
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmAutocompleteSearchUGroup: "),
    serverUrl: null,
    _login: {},
    _pwd: {},
    _srv: {},

    startSearch: function(searchString, searchParam, previousResult, listener) {
        this._logger.debug("search: " + searchString);
        if (!searchString || /,/.test(searchString)) {
            let result = new BmAutocompleteResultUGroup(searchString);
            result.searchResult = ACR.RESULT_IGNORED;
            listener.onSearchResult(this, result);
            return;
        }
        this._searchString = searchString;
        this._searchParam = searchParam;
        this._previousResult = previousResult;
        this._listener = listener;
        if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
            this._listener.onSearchResult(this, null);
        } else {
            this._search();
        }
    },
    _search: function() {
      let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
      let self = this;
      result.then(function(logged) {
          self._authKey = logged.authKey;
          self._user = logged.authUser;
          let client = new DirectoryClient(self._srv.value, self._authKey, self._user.domainUid);
          return client.search({"kindsFilter": ["USER", "GROUP"], "nameFilter":self._searchString});
      }).then(function(res) {
        let entries = [];
        for (let item of res.values) {
          let entry = new DirectoryEntry(item.value.entryUid, item.value.displayName);
          entries.push(entry);
        }
        return entries;
      }).then(function(entries) {
        let result = new BmAutocompleteResultUGroup(self._searchString);
        for (let entry of entries) {
            result._searchResults.push(entry);
        }
        if (result.matchCount) {
            result.searchResult = ACR.RESULT_SUCCESS;
            result.defaultIndex = 0;
        }
        self._listener.onSearchResult(self, result);
      }).catch(function(err) {
          self._logger.error(err);
      });
    }
};


/**
 * search entry
 */
var DirectoryEntry = function(uid, displayName) {
  this._uid = uid;
  this._displayName = displayName
};

DirectoryEntry.prototype._uid;
DirectoryEntry.prototype._displayName;

DirectoryEntry.prototype.toString = function() {
  return this._displayName;
};

DirectoryEntry.prototype.getUid = function() {
  return this._uid;
};


let NSGetFactory = XPCOMUtils.generateNSGetFactory([BmAutocompleteSearchUGroup]);

