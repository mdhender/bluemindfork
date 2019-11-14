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

/* Bm remote directory */

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/core2/BMContactHome.jsm");
Components.utils.import("resource://bm/core2/BMFolderHome.jsm");
Components.utils.import("resource://bm/core2/BMAuthService.jsm");

function BmDirectory() {
    let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"].getService(Components.interfaces.mozIJSSubScriptLoader);
    loader.loadSubScript("chrome://bm/content/core2/client/AuthenticationClient.js");
    loader.loadSubScript("chrome://bm/content/core2/client/AddressBookClient.js");
    this.mDirectory = null;
    this.wrappedJSObject = this;
}

var gBmDirPrefix = "bmdirectory://";

BmDirectory.prototype = {
    classDescription: "BmDirectory XPCOM Component",
    classID:          Components.ID("{ad4e8d8c-e5a3-4a72-bfc3-35ddb905cebe}"),
    contractID:       "@mozilla.org/addressbook/directory;1?type=bmdirectory",
    QueryInterface: ChromeUtils.generateQI ? 
        ChromeUtils.generateQI([Components.interfaces.nsIAbDirectory,
                                Components.interfaces.nsIAbDirectorySearch,
                                Components.interfaces.nsAbDirSearchListenerContext])
        : XPCOMUtils.generateQI([Components.interfaces.nsIAbDirectory,
                                Components.interfaces.nsIAbDirectorySearch,
                                Components.interfaces.nsAbDirSearchListenerContext]),
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmDirectory: "),
    _abManager: Components.classes["@mozilla.org/abmanager;1"].getService()
                        .QueryInterface(Components.interfaces.nsIAbManager),
    _contactHome: new BMContactHome(),
    mCardCache: new HashMap(),
    /*nsIAbDirectory*/
    get propertiesChromeURI() {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    get dirName() {
        if (Components.interfaces.nsIUTF8ConverterService) {
            let conv = Components.classes["@mozilla.org/intl/utf8converterservice;1"]
                                .createInstance(Components.interfaces.nsIUTF8ConverterService);
            return conv.convertStringToUTF8(this.mDescription, "iso-8859-1", false);
        } else {
            return this.mDescription;
        }
    },
    get dirType() {
        return 0;
    },
    get fileName() {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    mURI: null,
    get URI() {
        return this.mURI;
    },
    get position() {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    get lastModifiedDate() {
        return 0;
    },
    get isMailList() {
        return false;
    },
    get childNodes() {
        let resultArray = Components.classes["@mozilla.org/array;1"]
                                    .createInstance(Components.interfaces.nsIArray);
        return resultArray.enumerate();
    },
    get childCards() {
        this._logger.debug("mQuery:" + this.mQuery);
        let term = this._getSearchTerm();
        if (term) {
            this._searchString = term;
            this.starSearch();
        }
        let resultArray = Components.classes["@mozilla.org/array;1"]
                                .createInstance(Components.interfaces.nsIMutableArray);
        return resultArray.enumerate();
    },
    mQuery: null,
    get isQuery() {
        return (this.mQuery && this.mQuery.length > 0);
    },
    mContainerId: null,
    init: function(aURI) {
        this._logger.debug("init:" + aURI);
        if (aURI.indexOf(gBmDirPrefix) == 0) {
            let prefName = aURI.substr(gBmDirPrefix.length);
            let quMark = aURI.indexOf("?");
            if (quMark > 1) {
                this.mQuery = aURI.substr(quMark);
                prefName = prefName.substr(0, quMark - gBmDirPrefix.length);
            }
            this.mContainerId = prefName.substr("ldap_2.servers.".length).split("$$").join(".");
            this.mURI = gBmDirPrefix + prefName;
            this.mDirPrefId = prefName;
            let prefService = Components.classes["@mozilla.org/preferences-service;1"]
                                    .getService(Components.interfaces.nsIPrefService);
            let prefBranch = prefService.getBranch(prefName + ".");
            this.mDescription = prefBranch.getCharPref("description");
        } else {
            throw "Wrong component for uri: " + aURI;
        }
    },
    deleteDirectory: function(directory) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED; 
    },
    hasCard: function(cards) {
        this._logger.debug("hasCard");
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    hasDirectory: function(dir) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    addCard: function(card) {
        let uid = bmUtils.randomUUID();
        this._logger.debug("add card: " + card.displayName + " uid: " + uid);
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true)) {
            let self = this;
            let result = BMAuthService.login(srv.value, user.value, pwd.value);
            result.then(function(logged) {
                self._authKey = logged.authKey;
                let entry = BMFolderHome._cardToEntry(card);
                let book = new AddressBookClient(srv.value, self._authKey, self.mContainerId);
                book.create(uid, entry.value);
            }).then(function(ok) {
                card.setProperty("bm-id", uid);
                self._abManager.notifyDirectoryItemAdded(self, card);
            },
            function(aRejectReason) {
                self._logger.error(aRejectReason);
            }).catch(function(err) {
                self._logger.error(err);
            });
        } else {
            return Components.results.NS_ERROR_FAILURE;
        }
        return card;
    },
    modifyCard: function(modifiedCard) {
        this._logger.debug("modify card: " + modifiedCard.displayName);
        let uid = modifiedCard.getProperty("bm-id", null);
        if (!uid) {
            return;
        }
        //FIXME modifyCard is called by cardOverlay save listenner after addCard or modifyCard
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true)) {
            let self = this;
            let book;
            let result = BMAuthService.login(srv.value, user.value, pwd.value);
            result.then(function(logged) {
                self._authKey = logged.authKey;
                book = new AddressBookClient(srv.value, self._authKey, self.mContainerId);
                book.getComplete(uid);
            }).then(function(existing) {
                let entry = BMFolderHome._cardToEntry(modifiedCard);
                book.update(uid, entry.value);
            }).then(function(ok) {
                //notify ?
            },
            function(aRejectReason) {
                self._logger.error(aRejectReason);
            }).catch(function(err) {
                self._logger.error(err);
            });
        }
    },
    deleteCards: function(aCards) {
        this._logger.debug("deleteCards: " + aCards.length);
        let applyChanges = {
            "add" : [],
            "modify" : [],
            "delete" : []
        };
        let toRemove = new HashMap();
        for (let i = 0; i < aCards.length; i++) {
			let card = aCards.queryElementAt(i, Components.interfaces.nsIAbCard);
            if (card) {
                let uid = card.getProperty("bm-id", null);
                if (uid) {
                    toRemove.put(uid, card);
                    applyChanges["delete"].push({
                        "uid": uid
                    });
                }
            }
        }
        if (applyChanges["delete"].length == 0) {
            return;
        }
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true)) {
            let self = this;
            let result = BMAuthService.login(srv.value, user.value, pwd.value);
            result.then(function(logged) {
                self._authKey = logged.authKey;
                let book = new AddressBookClient(srv.value, self._authKey, self.mContainerId);
                return book.updates(applyChanges);
            }).then(function(updatesResult) {
                updatesResult.removed.forEach(function(removed) {
                    self._abManager.notifyDirectoryItemDeleted(self, toRemove.get(removed));
                });
            },
            function(aRejectReason) {
                self._logger.error(aRejectReason);
            }).catch(function(err) {
                self._logger.error(err);
            });
        }
    },
    dropCard: function(card, needToCopyCard) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    useForAutocomplete: function(aIdentityKey) {
        // don't allow search during local autocomplete - must
        // use the separate BM autocomplete due to the current interfaces
        return false;
    },
    get supportsMailingLists() {
        return false;
    },
    mAddressLists: [],
    get addressLists() {
        return this.mAddressLists;
    },
    set addressLists(value) {
        return (this.mAddressLists = value);
    },
    addMailList: function(list) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    get listNickName() {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    mDescription: "",
    get description() {
        return this.mDescription;
    },
    editMailListToDatabase: function(listCard) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    copyMailList: function(srcList) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    createNewDirectory: function(aDirName, aURI, aType, aPrefName) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    createDirectoryByURI: function(displayName, aURI) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    mDirPrefId: null,
    get dirPrefId() {
        return this.mDirPrefId;
    },
    set dirPrefId(value) {
        return (this.mDirPrefId = value);
    },
    getIntValue: function(aName, aDefaultValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    getBoolValue: function(aName, aDefaultValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    getStringValue: function(aName, aDefaultValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    getLocalizedStringValue: function(aName, aDefaultValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    setIntValue: function(aName, aValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    setBoolValue: function(aName, aValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    setStringValue: function(aName, aValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    setLocalizedStringValue: function(aName, aValue) {
        throw Components.results.NS_ERROR_NOT_IMPLEMENTED;
    },
    get readOnly() {
        return !bmUtils.getBoolPref(this.URI + ".bm-writable", false);
    },
    get isRemote() {
        return true;
    },
    cardForEmailAddress: function(aEmailAddress) {
        let card = this.mCardCache.get(aEmailAddress);
        if (card && card instanceof Components.interfaces.nsIAbCard) {
            return card;
        }
        return null;
    },
    getCardFromProperty: function(aProperty, aValue, aCaseSensitive) {
        if (aProperty == "PrimaryEmail" || aProperty == "SecondEmail") {
            return this.cardForEmailAddress(aValue);
        }
        return null;
    },
    /* nsIAbDirectorySearch */
    starSearch: function() {
        this.stopSearch();
        this._startSearch();
    },
    stopSearch: function() {
        if (this._searching) {
            this._searching = false;
        }
    },
    onSearchFoundCard: function(/*nsIAbCard*/ aCard) {
        if (!this._searching) return;
        if (this._autoCompleteSearch) {
            this.mCardCache.put(aCard.primaryEmail, aCard);
        }
        this._abManager.notifyDirectoryItemAdded(this, aCard);
        if (this._searchListener) {
            this._searchListener.onSearchFoundCard(aCard, this.URI);
        }
    },
    onSearchFinished: function(aResult, aErrorMsg) {
        if (!this._searching) return;
        this._searching = false;
        if (this._searchListener) {
            this._searchListener.onSearchFinished(aResult, aErrorMsg, this.URI);
        }
    },
    /* BM autocomplete */
    autoCompleteSearch: function(aSearchListener) {
        this._searchString = this._getSearchTerm();
        this._startSearch(aSearchListener);
    },
    /* other */
    _autoCompleteSearch: false,
    _getSearchTerm: function() {
        this._autoCompleteSearch = false;
        let term = null;
        // see http://dxr.mozilla.org/comm-central/source/mailnews/addrbook/src/nsAbQueryStringToExpression.cpp#259
        if (this.mQuery && this.mQuery.length > 0) {
            let prefix = "(DisplayName,c,";
            let dn = this.mQuery.indexOf(prefix);
            if (dn == -1) {
                prefix = "(DisplayName,bw,";
                dn = this.mQuery.indexOf(prefix);
            }
            if (dn == -1) {
                prefix = "(PrimaryEmail,bw,";
                dn = this.mQuery.indexOf(prefix);
                this.mCardCache.clear();
                this._autoCompleteSearch = true;
            }
            if (dn > -1) {
                let start = dn + prefix.length;
                let end = this.mQuery.indexOf(")", start);
                if (end > -1) {
                    term = this.mQuery.substr(start, end - start);
                    if (term.length > 0) {
                        term = decodeURI(term).trim();
                        term = bmUtils.esEscape(term);
                    } else {
                        term = null;
                    }
                }
            }
        }
        this._logger.debug("_getSearchTerm:" + term + ", autocomplete:" + this._autoCompleteSearch);
        return term;
    },
    _startSearch: function(aSearchListener) {
        if (aSearchListener) {
            this._searchListener = aSearchListener;
        } else {
            this._searchListener = null;
        }
        let user = {};
        let pwd = {};
        let srv = {};
        if (bmUtils.getSettings(user, pwd, srv, true)) {
            let self = this;
            let result = BMAuthService.login(srv.value, user.value, pwd.value);
            result.then(function(logged) {
                self._authKey = logged.authKey;
                self._doSearch(srv.value);
            },
            function(aRejectReason) {
                self._logger.error(aRejectReason);
                self.onSearchFinished(Components.results.NS_ERROR_FAILURE, aRejectReason);
            }).catch(function(err) {
                self._logger.error(err);
                self.onSearchFinished(Components.results.NS_ERROR_FAILURE, err);
            });
        }
    },
    _searching: false,
    _doSearch: function(srv) {
        this._searching = true;
        if (!this._searchString) {
            self.onSearchFinished(Components.results.NS_OK);
            return;
        }
        let book = new AddressBookClient(srv, this._authKey, this.mContainerId);
        
        let term = "(" + this._searchString + "*)";
        let q = "value.kind: 'individual'";
        let limit = 100;
        if (this._autoCompleteSearch) {
            q += " AND (value.identification.formatedName.value:" + term
            + " OR value.communications.emails.value:" + term + ")"
            + " AND _exists_:value.communications.emails.value";
            limit = 10;
        } else {
            q += " AND (value.identification.formatedName.value:" + term
            + " OR value.identification.name.familyNames:" + term
            + " OR value.identification.name.givenNames:" + term
            + " OR value.identification.name.additionalNames:" + term
            + " OR value.communications.emails.value:" + term +")";
        }
        let query = {query: q, size: limit, from: 0};
        
        let search = book.search(query);
        let self = this;
        let contacts;
        search.then(function(results){
            self._logger.debug("results:" + results.total);
            contacts = [];
            return results.values.reduce(function(sequence, result) {
                return sequence.then(function() {
                    return book.getComplete(result.uid);
                }).then(function(item) {
                    return contacts.push(self._adaptCardAsEntry(item));
                })
            }, Promise.resolve());
            
        }).then(function() {
            for (let entry of contacts) {
                let card = Components.classes["@mozilla.org/addressbook/cardproperty;1"].createInstance(Components.interfaces.nsIAbCard);
                let contact = new BMContact(card);
                self._contactHome.fillContactFromEntry(entry, contact);
                self.onSearchFoundCard(card);
            }
        }).then(function() {
            q = "value.kind: 'group'";
            if (self._autoCompleteSearch) {
                q += " AND value.communications.emails.value:" + term;
                limit = 10;
            } else {
                q += " AND (value.identification.formatedName.value:" + term
                + " OR value.communications.emails.value:" + term + ")";
            }
            query = {query: q, size: limit, from: 0};
            return book.search(query);
        }).then(function(results){
            self._logger.debug("results:" + results.total);
            for (let result of results.values) {
                if (result.value.mail) {
                    let card = Components.classes["@mozilla.org/addressbook/cardproperty;1"].createInstance(Components.interfaces.nsIAbCard);
                    card.displayName = result.displayName;
                    card.primaryEmail = result.value.mail;
                    self.onSearchFoundCard(card);
                }
            }
            self.onSearchFinished(Components.results.NS_OK);
        },
        function(aRejectReason) {
            self._logger.error(aRejectReason);
            self.onSearchFinished(Components.results.NS_ERROR_FAILURE, aRejectReason);
        }).catch(function(err) {
            self._logger.error(err);
            self.onSearchFinished(Components.results.NS_ERROR_FAILURE, err);
        });
    },
    _adaptCardAsEntry: function(itemValue) {
        let entry = {};
        entry.id = itemValue.uid;
        entry.name = itemValue.displayName;
        entry.value = itemValue.value;
        entry.externalId = itemValue.externalId;
        return entry;
    }
};

var NSGetFactory = XPCOMUtils.generateNSGetFactory([BmDirectory]);
