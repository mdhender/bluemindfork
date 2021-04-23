/* eslint-disable object-shorthand */

// Get various parts of the WebExtension framework that we need.
var { ExtensionCommon } = ChromeUtils.import("resource://gre/modules/ExtensionCommon.jsm");
var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

/* Bm autocomplete source */

var AutocompleteApi = class extends ExtensionCommon.ExtensionAPI {
    getAPI(context) {
        return {
            AutocompleteApi: {
                init: async function () {

                    try {
                        var { ComponentUtils } = ChromeUtils.import("resource://gre/modules/ComponentUtils.jsm");
                    } catch(e) {
                    //TB 78
                        var { XPCOMUtils } = ChromeUtils.import("resource://gre/modules/XPCOMUtils.jsm");
                    }

                    let classID = Components.ID("{ddb5a551-9df4-4e2b-8f79-b9e16d2c28f5}");
                    let contractID = "@mozilla.org/autocomplete/search;1?name=bm-search";

                    var { MailServices } = ChromeUtils.import("resource:///modules/MailServices.jsm");

                    var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
                    var { BMContactHome } = ChromeUtils.import("chrome://bm/content/modules/core2/BMContactHome.jsm");
                    var { BMAuthService } = ChromeUtils.import("chrome://bm/content/modules/core2/BMAuthService.jsm");

                    const ACR = Components.interfaces.nsIAutoCompleteResult;
                    const nsIAbAutoCompleteResult = Components.interfaces.nsIAbAutoCompleteResult;

                    function BmAutocompleteResult(aSearchString) {
                        this._searchResults = new Array();
                        this.searchString = aSearchString;
                    }

                    BmAutocompleteResult.prototype = {
                        QueryInterface: ChromeUtils.generateQI([Components.interfaces.nsIAbAutoCompleteResult]),
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
                            return this._searchResults[aIndex].value;
                        },
                        getLabelAt: function getLabelAt(aIndex) {
                            return this.getValueAt(aIndex);
                        },
                        getCommentAt: function getCommentAt(aIndex) {
                            return this._searchResults[aIndex].comment;
                        },
                        getStyleAt: function getStyleAt(aIndex) {
                            return "local-abook";
                        },
                        getImageAt: function getImageAt(aIndex) {
                            return "";
                        },
                        getFinalCompleteValueAt: function (aIndex) {
                            return this.getValueAt(aIndex);
                        },
                        removeValueAt: function removeValueAt(aRowIndex, aRemoveFromDB) { },

                        // nsIAbAutoCompleteResult
                        getCardAt: function getCardAt(aIndex) {
                            return this._searchResults[aIndex].card;
                        },
                        getEmailToUse: function getEmailToUse(aIndex) {
                            return this._searchResults[aIndex].emailToUse;
                        },
                    };

                    function BmAutocompleteSearch() {
                        let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
                        loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");
                        loader.loadSubScript("chrome://bm/content/core2/client/AddressBookClient.js");
                    }

                    BmAutocompleteSearch.prototype = {
                        classDescription: "BmAutocompleteSearch XPCOM Component",
                        classID: classID,
                        contractID: contractID,
                        QueryInterface: ChromeUtils.generateQI([Components.interfaces.nsIAutoCompleteSearch,
                        Components.interfaces.nsIAbDirectorySearch]),
                        _parser: MailServices.headerParser,
                        _abManager: MailServices.ab,
                        _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmAutocompleteSearch: "),
                        _result: null,
                        _fullString: null,
                        _directories: new HashMap(),

                        startSearch: function (searchString, searchParam, previousResult, listener) {
                            this._logger.debug("search: " + searchString);
                            this._result = new BmAutocompleteResult(searchString);
                            if (!searchString || /,/.test(searchString)) {
                                this._result.searchResult = ACR.RESULT_IGNORED;
                                listener.onSearchResult(this, this._result);
                                return;
                            }
                            this.stopSearch();
                            this._listener = listener;
                            this._fullString = searchString.toLocaleLowerCase();

                            this._searchBlueMind();
                        },
                        /* Start searchs in remote dirs */
                        _searchBlueMind: function () {
                            let uris = this._getDirectoriesUris();
                            let domainAbFound = false;
                            for (let uri of uris) {
                                if (!domainAbFound) {
                                    domainAbFound = bmUtils.getBoolPref(uri + ".bm-domain-ab", false);
                                }
                            }

                            if (!domainAbFound) {
                                this._directories.put("FakeRemoteContactsDir", new FakeRemoteContactsDir(this._fullString));
                            }
                            this._directories.put("FakeRemoteGroupsDir", new FakeRemoteGroupsDir(this._fullString));

                            for (let dir of this._directories.values()) {
                                dir.wrappedJSObject.autoCompleteSearch(this);
                            }
                        },
                        _getDirectoriesUris: function () {
                            let uris = [];
                            let it = MailServices.ab.directories;
                            for (let directory of it) {
                                if (directory instanceof Components.interfaces.nsIAbDirectory) {
                                    let uri = directory.URI;
                                    this._logger.debug("_getDirectoriesUris uri:" + uri);
                                    let id = bmUtils.getCharPref(uri + ".bm-id", null);
                                    if (id) {
                                        uris.push(uri);
                                    }
                                }
                            }
                            return uris;
                        },
                        _checkDuplicate: function (card, emailAddress) {
                            let lcEmailAddress = emailAddress.toLocaleLowerCase();
                            return this._result._searchResults.some(function (result) {
                                return result.value.toLocaleLowerCase() == lcEmailAddress;
                            });
                        },
                        _addToResult: function (card, dirName) {
                            let mbox = this._parser.makeMailboxObject(card.displayName,
                                card.isMailList ? card.getProperty("Notes", "") || card.displayName :
                                    card.primaryEmail);
                            if (!mbox.email) return;
                            let emailAddress = mbox.toString();
                            // If it is a duplicate, then just return and don't add it. The
                            // _checkDuplicate function deals with it all for us.
                            if (this._checkDuplicate(card, emailAddress)) return;
                            // Find out where to insert the card.
                            let insertPosition = 0;
                            // Next sort on full address
                            while (insertPosition < this._result._searchResults.length &&
                                emailAddress > this._result._searchResults[insertPosition].value)
                                ++insertPosition;
                            this._result._searchResults.splice(insertPosition, 0, {
                                value: emailAddress,
                                card: card,
                                comment: dirName,
                                emailToUse: emailAddress
                            });
                        },
                        stopSearch: function () {
                            if (this._listener) {
                                for (let dir of this._directories.values()) {
                                    dir.wrappedJSObject.stopSearch();
                                }
                                this._listener = null;
                            }
                        },
                        onSearchFoundCard: function onSearchFoundCard(aCard, aUri) {
                            this._logger.debug("onSearchFoundCard: " + aCard.primaryEmail + " in: " + aUri);
                            if (!this._listener) return;
                            this._addToResult(aCard, this._directories.get(aUri).dirName);
                        },
                        onSearchFinished: function onSearchFinished(aResult, aErrorMsg, aUri) {
                            this._logger.debug("onSearchFinished in: " + aUri);
                            if (!this._listener) return;
                            if (aResult == Components.results.NS_OK) {
                                if (this._result.matchCount) {
                                    this._result.searchResult = ACR.RESULT_SUCCESS;
                                    this._result.defaultIndex = 0;
                                } else {
                                    this._result.searchResult = ACR.RESULT_NOMATCH;
                                }
                            } else if (aResult == Components.results.NS_ERROR_FAILURE) {
                                this._result.searchResult = ACR.RESULT_FAILURE;
                                this._result.defaultIndex = 0;
                            }
                            this._directories.remove(aUri);
                            this._logger.debug("dirs searching: " + this._directories.keys().length);
                            if (this._directories.keys().length == 0) {
                                this._listener.onSearchResult(this, this._result);
                                this._listener = null;
                            }
                        }
                    };

                    function FakeRemoteGroupsDir(aFullString) {
                        this._fullString = aFullString;
                        this.wrappedJSObject = this;
                    }

                    FakeRemoteGroupsDir.prototype = {
                        _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("FakeRemoteGroupsDir: "),
                        get URI() {
                            return "FakeRemoteGroupsDir";
                        },
                        get dirName() {
                            return "BlueMind";
                        },
                        _searchListener: null,
                        _searching: false,
                        autoCompleteSearch: function (aSearchListener) {
                            this._searchListener = aSearchListener;
                            let user = {};
                            let pwd = {};
                            let srv = {};
                            if (bmUtils.getSettings(user, pwd, srv, true)) {
                                let self = this;
                                let result = BMAuthService.login(srv.value, user.value, pwd.value);
                                result.then(function (logged) {
                                    self._searching = true;
                                    let client = new AddressBookClient(srv.value, logged.authKey, "addressbook_" + logged.authUser.domainUid);
                                    let q = "value.kind: 'group' AND value.identification.formatedName.value:" + self._fullString + "*";
                                    return client.search({ query: q, size: 10, from: 0 });
                                }).then(function (results) {
                                    self._logger.debug("results:" + results.total);
                                    for (let iv of results.values) {
                                        if (iv.value.mail) {
                                            let card = Components.classes["@mozilla.org/addressbook/cardproperty;1"]
                                                .createInstance(Components.interfaces.nsIAbCard);
                                            card.displayName = iv.value.formatedName;
                                            card.primaryEmail = iv.value.mail;
                                            self.onSearchFoundCard(card);
                                        }
                                    }
                                    self.onSearchFinished(Components.results.NS_OK);
                                }, function (aRejectReason) {
                                    self._logger.error(aRejectReason);
                                    self.onSearchFinished(Components.results.NS_ERROR_FAILURE, aRejectReason);
                                }).catch(function (err) {
                                    self._logger.error(err);
                                    self.onSearchFinished(Components.results.NS_ERROR_FAILURE, err);
                                });
                            } else {
                                this.onSearchFinished(Components.results.NS_OK);
                            }
                        },
                        stopSearch: function () {
                            if (this._searching) {
                                this._searching = false;
                            }
                        },
                        onSearchFoundCard: function (/*nsIAbCard*/ aCard) {
                            if (!this._searching) return;
                            if (this._searchListener) {
                                this._searchListener.onSearchFoundCard(aCard, this.URI);
                            }
                        },
                        onSearchFinished: function (aResult, aErrorMsg) {
                            if (!this._searching) return;
                            this._searching = false;
                            if (this._searchListener) {
                                this._searchListener.onSearchFinished(aResult, aErrorMsg, this.URI);
                            }
                        },
                    };

                    function FakeRemoteContactsDir(aFullString) {
                        this._fullString = aFullString;
                        this.wrappedJSObject = this;
                    }

                    FakeRemoteContactsDir.prototype = {
                        _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("FakeRemoteContactsDir: "),
                        get URI() {
                            return "FakeRemoteContactsDir";
                        },
                        get dirName() {
                            return "BlueMind";
                        },
                        _searchListener: null,
                        _searching: false,
                        _contactHome : new BMContactHome(),
                        autoCompleteSearch: function (aSearchListener) {
                            this._searchListener = aSearchListener;
                            let user = {};
                            let pwd = {};
                            let srv = {};
                            if (bmUtils.getSettings(user, pwd, srv, true)) {
                                let self = this;
                                let result = BMAuthService.login(srv.value, user.value, pwd.value);
                                result.then(function (logged) {
                                    self._searching = true;
                                    let client = new AddressBookClient(srv.value, logged.authKey, "addressbook_" + logged.authUser.domainUid);
                                    let term = self._fullString + "*"; 
                                    let q = "value.kind: 'individual'"
                                        + " AND (value.identification.formatedName.value:" + term
                                        + " OR value.communications.emails.value:" + term +")"
                                        + " AND _exists_:value.communications.emails.value";
                                    return client.search({query: q, size:10, from:0});
                                }).then(function (results){
                                    self._logger.debug("results:" + results.total);
                                    for (let iv of results.values) {
                                        if (iv.value.mail) {
                                            let card = Components.classes["@mozilla.org/addressbook/cardproperty;1"]
                                                .createInstance(Components.interfaces.nsIAbCard);
                                            card.displayName = iv.value.formatedName;
                                            card.primaryEmail = iv.value.mail;
                                            self.onSearchFoundCard(card);
                                        }
                                    }
                                    self.onSearchFinished(Components.results.NS_OK);
                                }, function (aRejectReason) {
                                    self._logger.error(aRejectReason);
                                    self.onSearchFinished(Components.results.NS_ERROR_FAILURE, aRejectReason);
                                }).catch(function (err) {
                                    self._logger.error(err);
                                    self.onSearchFinished(Components.results.NS_ERROR_FAILURE, err);
                                });
                            } else {
                                this.onSearchFinished(Components.results.NS_OK);
                            }
                        },
                        stopSearch: function () {
                            if (this._searching) {
                                this._searching = false;
                            }
                        },
                        onSearchFoundCard: function (/*nsIAbCard*/ aCard) {
                            if (!this._searching) return;
                            if (this._searchListener) {
                                this._searchListener.onSearchFoundCard(aCard, this.URI);
                            }
                        },
                        onSearchFinished: function (aResult, aErrorMsg) {
                            if (!this._searching) return;
                            this._searching = false;
                            if (this._searchListener) {
                                this._searchListener.onSearchFinished(aResult, aErrorMsg, this.URI);
                            }
                        },
                    }

                    console.trace("Register component");

                    let factory;
                    if (ComponentUtils) {
                        factory = ComponentUtils.generateNSGetFactory([BmAutocompleteSearch])(classID);
                    } else {
                        factory = XPCOMUtils.generateNSGetFactory([BmAutocompleteSearch])(classID);
                    }
                    // WARNING: this assumes that Thunderbird is already running, as
                    // Components.manager.registerFactory will be unavailable for a few
                    // milliseconds after startup.
                    Components.manager.registerFactory(classID, "BmAutocompleteSearch", contractID,
                        factory);
                    context.callOnClose({
                        close() {
                            Components.manager.unregisterFactory(classID, factory);
                        }
                    });

                }
            }
        }
    }
};
