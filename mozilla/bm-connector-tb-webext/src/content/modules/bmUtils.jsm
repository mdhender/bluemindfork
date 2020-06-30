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

/* Utils module */
var { AddonManager } = ChromeUtils.import("resource://gre/modules/AddonManager.jsm");
var { MailServices } = ChromeUtils.import("resource:///modules/MailServices.jsm");

var { ConversionHelper } = ChromeUtils.import("chrome://bm/content/api/ConversionHelper/ConversionHelper.jsm");
var { bmPhotos } = ChromeUtils.import("chrome://bm/content/modules/bmPhotos.jsm");

this.EXPORTED_SYMBOLS = ["bmUtils","HashMap", "BMXPComObject", "BmPrefListener", "BMError"];

const historyDirUri = "jsaddrbook://history.sqlite";
const prevHistoryDirUri = "moz-abmdbdirectory://history.mab";

function BMXPComObject() {
    this.wrappedJSObject = this;
}
BMXPComObject.prototype.QueryInterface = function(aIID) {
    if (!aIID.equals(Components.interfaces.nsISupports)) {
        throw Components.results.NS_ERROR_NO_INTERFACE;
    }
    return this;
}

function BMError(aMessage) {
    this.name = "BMError";
    this.message = aMessage;
    this.stack = (new Error()).stack;
}
BMError.prototype = Object.create(Error.prototype);
BMError.prototype.constructor = BMError;

let bmUtils = {
    session: {},
    _getPrefService: function() {
        return Components.classes["@mozilla.org/preferences-service;1"]
            .getService(Components.interfaces.nsIPrefBranch);
    },
    _uuidGenerator : Components.classes["@mozilla.org/uuid-generator;1"]
        .getService(Components.interfaces.nsIUUIDGenerator),
    _parserUtils: Components.classes["@mozilla.org/parserutils;1"]
        .getService(Components.interfaces.nsIParserUtils),
    promptService: Components.classes["@mozilla.org/embedcomp/prompt-service;1"]
        .getService(Components.interfaces.nsIPromptService),
    log: function(aMessage) {
        console.log(aMessage);
    },
    getCharPref : function (aPrefName, aDefaultValue) {
        let prefValue;
        let prefService = new bmUtils._getPrefService();
        try {
          prefValue = prefService.getCharPref(aPrefName);
        } catch (e) {
            if (e) {
                prefValue = aDefaultValue;
            } else {
                throw(e);
            }
        }
        return prefValue;
    },
    setCharPref: function(aPrefName, aValue) {
        let prefService = new bmUtils._getPrefService();        
        prefService.QueryInterface(Components.interfaces.nsIPrefService);
        prefService.setCharPref(aPrefName, aValue);
        prefService.savePrefFile(null);
    },
    getBoolPref : function (aPrefName, aDefaultValue) {
        let prefValue;
        let prefService = new bmUtils._getPrefService();
        try {
          prefValue = prefService.getBoolPref(aPrefName);
        } catch (e) {
            if (e) {
                prefValue = aDefaultValue;
            } else {
                throw(e);
            }
        }
        return prefValue;
    },
    setBoolPref: function(aPrefName, aValue) {
        let prefService = new bmUtils._getPrefService();        
        prefService.QueryInterface(Components.interfaces.nsIPrefService);
        prefService.setBoolPref(aPrefName, aValue);
        prefService.savePrefFile(null);
    },
    getIntPref: function(aPrefName, aDefaultValue) {
	let prefValue;
        let prefService = new bmUtils._getPrefService();
        try {
          prefValue = prefService.getIntPref(aPrefName);
        } catch (e) {
            if (e) {
                prefValue = aDefaultValue;
            } else {
                throw(e);
            }
        }
        return prefValue;
    },
    setIntPref: function(aPrefName, aValue) {
        let prefService = new bmUtils._getPrefService();
        prefService.QueryInterface(Components.interfaces.nsIPrefService);
        prefService.setIntPref(aPrefName, aValue);
        prefService.savePrefFile(null);
    },
    deletePrefBranch: function(aPrefBranch) {
        let prefService = new bmUtils._getPrefService();
        try {
            prefService.deleteBranch(aPrefBranch);
        } catch(e) {
            //not exist
        }
    },
    getCredentialStored: function(aHostname, aUser, aPassword) {
        let passwordManager = Components.classes["@mozilla.org/login-manager;1"]
                               .getService(Components.interfaces.nsILoginManager);
        let logins = passwordManager.getAllLogins({});
        for (var i = 0; i < logins.length; i++) {
            if (logins[i].hostname == aHostname) {            
               aPassword.value = logins[i].password;
               aUser.value = logins[i].username; 
               break;
            }
        }
    },
    removeCredentialStored: function(aHostname, aGoodLogin) {
        let passwordManager = Components.classes["@mozilla.org/login-manager;1"]
            .getService(Components.interfaces.nsILoginManager);
        let logins = passwordManager.getAllLogins({});
        for (var i = 0; i < logins.length; i++) {
            if (logins[i].hostname == aHostname && (!aGoodLogin || aGoodLogin != logins[i].username)) {            
               passwordManager.removeLogin(logins[i]);
            }
        }
    },
    updateCredentialsStored: function(aHostname, aLefPartLogin, aFullLogin) {
        let passwordManager = Components.classes["@mozilla.org/login-manager;1"]
                               .getService(Components.interfaces.nsILoginManager);
        let logins = passwordManager.getAllLogins({});
        for (var i = 0; i < logins.length; i++) {
            if (logins[i].hostname == aHostname && logins[i].username == aLefPartLogin) {
               let updated = logins[i].clone();
               updated.username = aFullLogin;
               passwordManager.modifyLogin(logins[i], updated);
               break;
            }
        }
    },
    getSettings: function(/*object*/ user, /*object*/ pwd, /*object*/ srv, displayWindows, parentWin) {
        let server = this.getCharPref("extensions.bm.server");
        if (!server || server == "https://xxxx") {
            if (!displayWindows) return false;
            var check = {value: false};
            var input = {value: "https://xxxx"};
            this.promptService.prompt(null, this.getLocalizedString("dialogs.title"),
                this.getLocalizedString("serverprompt.message"), input, null, check);
            
            server = input.value;
            if (!server || server == "https://xxxx") return false;
            this.setCharPref("extensions.bm.server", server);
        }
        srv.value = server;
        this.getCredentialStored(server, user, pwd);
        if (!user.value || !pwd.value) {
            user.value = bmUtils.session.user;
            pwd.value = bmUtils.session.password;
        }
        if (!user.value || !pwd.value) {
            this._getSyncSettingsFromImapAccount(server, user, pwd);
        }
        if (!user.value || !pwd.value) {
            if (!displayWindows) return false;
            let ww = Components.classes["@mozilla.org/embedcomp/window-watcher;1"].getService(Components.interfaces.nsIWindowWatcher);
            let authPrompt = ww.getNewAuthPrompter(parentWin ? parentWin : null);
            
            authPrompt.promptUsernameAndPassword(this.getLocalizedString("dialogs.title"),
                                                this.getLocalizedString("authprompt.message") + " " + server,
                                                server,
                                                Components.interfaces.nsIAuthPrompt.SAVE_PASSWORD_PERMANENTLY,
                                                user, pwd);
        }
        if (!user.value || !pwd.value) return false;
        this.session.user = user.value;
        this.session.password = pwd.value;
        return true;
    },
    _getBmImapAccount: function(aBmSrv) {
		let acctMgr = Components.classes["@mozilla.org/messenger/account-manager;1"].getService(Components.interfaces.nsIMsgAccountManager);
		let accounts = acctMgr.accounts;
		let matchingAccounts = [];
		for (let acc of accounts) {
			if (acc.incomingServer.type == "imap")  {
				let server = acc.incomingServer;
				let isBmServer = ("https://" + server.hostName) == aBmSrv;
				if (server.valid && isBmServer) {
					matchingAccounts.push(acc);
				}
			}
		}
		if (matchingAccounts.length == 0) {
			return null;
		}
		if (matchingAccounts.length == 1) {
			return matchingAccounts[0];
		}
		let defaultAccount = this.getCharPref("mail.accountmanager.defaultaccount", null);
		this.log("defaultAccount:" + defaultAccount);
		if (!defaultAccount) {
			this.log("multiple BM IMAP accounts and no default: cannot choose witch one to use login/password from");
			return null;
		}
		for (let match of matchingAccounts) {
			if (match.key == defaultAccount) {
				return match;
			}
		}
		this.log("multiple BM IMAP accounts: cannot choose witch one to use login/password from");
		return null;
	},
	_getSyncSettingsFromImapAccount: function(aBmSrv, user, pwd) {
		let account = this._getBmImapAccount(aBmSrv);
		if (account) {
			let server = account.incomingServer;
			if (server.password) {
				let passwordManager = Components.classes["@mozilla.org/login-manager;1"].getService(Components.interfaces.nsILoginManager);
				let loginInfo = Components.classes["@mozilla.org/login-manager/loginInfo;1"].createInstance(Components.interfaces.nsILoginInfo);
				loginInfo.username=server.username;
				loginInfo.password=server.password;
				loginInfo.hostname="https://" + server.hostName;
				loginInfo.formSubmitURL=null;
				loginInfo.httpRealm="https://" + server.hostName;
				loginInfo.usernameField="";
                loginInfo.passwordField="";
                loginInfo.origin="https://" + server.hostName;
				passwordManager.addLogin(loginInfo);
			} else {
				this.session.user = server.username;
			}
			user.value = server.username;
			pwd.value = server.password;
		}
    },
    getEmailOfImapAccount: function(aBmSrv) {
		let account = this._getBmImapAccount(aBmSrv);
		if (account) {
			let server = account.incomingServer;
			let isBmServer = ("https://" + server.hostName) == aBmSrv;
			if (server.valid && isBmServer) {
			return account.defaultIdentity.email;
			}
		}
		return null;
    },
    getLocalizedString: function(aStringName) {
        return ConversionHelper.i18n.getMessage("extensions.bm." + aStringName);
    },
    getBmAddressbookName: function(aName, isDefault) {
        let name = "";
        if (isDefault) {
            if (aName == "users" || aName == "contacts" || aName == "public_contacts") {
                name += this.getLocalizedString("defaults.directories." + aName);
            } else {
                name += aName;
            }
        } else {
            name += aName;
        }
        return name;
    },
    deleteAllBmDirectories: function() {
        let abManager = Components.classes["@mozilla.org/abmanager;1"]
                                .getService().QueryInterface(Components.interfaces.nsIAbManager);
        let it = abManager.directories;
        while (it.hasMoreElements()) {
            let directory = it.getNext();
            if (directory instanceof Components.interfaces.nsIAbDirectory) {
                let id = this.getCharPref(directory.URI + ".bm-id", null);
                if (id != null) {
                    if (directory.URI == historyDirUri || directory.URI == prevHistoryDirUri) {
                        this.deletePrefBranch(directory.URI);
                        //delete bm cards
                        let cardsToDel = [];
                        let cards = directory.childCards;
                        for (let card of cards) {
                            let id = card.getProperty("bm-id", null);
                            if (id) {
                                cardsToDel.push(card);
                            }
                        }
                        directory.deleteCards(cardsToDel);
                    } else {
                        let cards = directory.childCards;
                        for (let card of cards) {
                            if (card) bmPhotos.removePhoto(card);
                        }
                        let uri = directory.URI;
                        this.deletePrefBranch(uri);
                        abManager.deleteAddressBook(uri);
                    }
                }
            }
        }
    },
    isBmDefaultAddressbook: function(directory) {
        let ret = false;
        let id = this.getCharPref(directory.URI + ".bm-id", null);
        if (id != null) {
            ret = this.getBoolPref(directory.URI + ".bm-default", false);
        }
        return ret;
    },
    isBmDirectory: function(directory) {
        let id = this.getCharPref(directory.URI + ".bm-id", null);
        return id != null && !directory.URI.indexOf("bmdirectory://") == 0;
    },
    isBmList: function(directory) {
        if (!directory.isMailList) {
            return false;
        }
        let uri = directory.URI;
        let parentUri = uri.substring(0, uri.indexOf("sqlite") + 6);
        let parentDir = MailServices.ab.getDirectory(parentUri);
        return this.isBmDirectory(parentDir);
    },
    isBmReadOnlyAddressbook: function(directory) {
        let uri = directory.URI;
        return !this.getBoolPref(uri + ".bm-writable", false);
    },
    isBmReadOnlyList: function(directory) {
        let uri = directory.URI;
        let isLocal = bmUtils.getBoolPref(uri + ".bm-local", false);
        if (isLocal) {
            return true;
        }
        // "moz-jsdirectory://abook-5.sqlite/MailList1"
        let parentUri = uri.substring(0, uri.indexOf("sqlite") + 6);
        let parentDir = MailServices.ab.getDirectory(parentUri);
        return this.isBmReadOnlyAddressbook(parentDir);
    },
    isBmSharedAddressbok: function(aUserId, aOwnerId, aName, isDefault) {
	if (isDefault && (aName == "users" || aName == "public_contacts")) {
        return false;
	}
	if (aUserId != aOwnerId) {
        return true;
	}
	return false;
    },
    randomUUID: function() {
        let uuid = this._uuidGenerator.generateUUID();
        let uuidString = uuid.toString();
        return uuidString.substring(1, uuidString.length -1); //remove {}
    },
    _versions: null,
    _getVersions: function() {
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            if (self._versions) {
                resolve(self._versions);
            } else {
                let callback = function(aExt) {
                    self._versions = {
                        tbVersion: Components.classes["@mozilla.org/xre/app-info;1"]
                                        .getService(Components.interfaces.nsIXULAppInfo).version,
                        osString: Components.classes["@mozilla.org/xre/app-info;1"]
                                            .getService(Components.interfaces.nsIXULRuntime).OS,
                        extVersion: aExt.version
                    };
                    resolve(self._versions);
                };
                if (ChromeUtils.generateQI) {
				    //TB >= 65
				    AddonManager.getAddonByID("bm-connector-tb@blue-mind.net").then(callback);
			    } else {
				    AddonManager.getAddonByID("bm-connector-tb@blue-mind.net", callback);
			    }
            }
        });
       return prom;
    },
    getVersionString: function() {
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            let versions = self._getVersions();
            versions.then(function(aVersions) {
                resolve("tb: version[" + aVersions.tbVersion + "] OS["
                            + aVersions.osString + "]" + " extension: version["
                            + aVersions.extVersion + "]");
            }).catch(function(e) {
                Components.utils.reportError(e);
            });
        });
        return prom;
    },
    _origin: null,
    getOrigin: function() {
        let self = this;
        let prom = new Promise(function(resolve, reject) {
            if (self._origin) {
                resolve(self._origin);
            } else {
                let versions = self._getVersions();
                versions.then(function(aVersions) {
                    resolve("bm-connector-thunderbird-" + aVersions.extVersion
                            + " tbird:" + aVersions.tbVersion
                            + " os:" + aVersions.osString);
                });
            }
        });
        return prom;
    },
    convertToPlainText: function(aHtmlString) {
        return this._parserUtils.convertToPlainText(aHtmlString, Components.interfaces.nsIDocumentEncoder.OutputFormatted, 0);
    },
    overrideBM: function(object, methodName, callback) {
        object[methodName] = callback(object[methodName]);
    },
    _esChars: [ '\\', '+', '-', '&&', '||', '!', '(', ')', '{', '}', '[', ']', '^', '"', '~', '*', '?', ':' ],
    esEscape: function(term) {
        for (let i = 0; i < this._esChars.length; i++) {
            term = term.split(this._esChars[i]).join('\\' + this._esChars[i]);
        }
        return term;
    }
}

/**
 * @constructor
 *
 * @param {string} branch_name
 * @param {Function} callback must have the following arguments:
 *   branch, pref_leaf_name
 */

function BmPrefListener(branch_name, callback) {
    // Keeping a reference to the observed preference branch or it will get
    // garbage collected.
    let prefService = Components.classes["@mozilla.org/preferences-service;1"].getService(Components.interfaces.nsIPrefService);
    this._branch = prefService.getBranch(branch_name);
    if (Components.interfaces.nsIPrefBranch2) {
        this._branch.QueryInterface(Components.interfaces.nsIPrefBranch2);
    } else {
        //nsIPrefBranch2 merged in nsIPrefBranch
        this._branch.QueryInterface(Components.interfaces.nsIPrefBranch);
    }
    this._callback = callback;
}

BmPrefListener.prototype.observe = function(subject, topic, data) {
    if (topic == 'nsPref:changed') this._callback(this._branch, data);
};

/**
 * @param {boolean=} trigger if true triggers the registered function
 *   on registration, that is, when this method is called.
 */
BmPrefListener.prototype.register = function(trigger) {
    this._branch.addObserver('', this, false);
    if (trigger) {
	let self = this;
	this._branch.getChildList('', {}).forEach(function(pref_leaf_name) {
        self._callback(self._branch, pref_leaf_name);
	});
    }
};

BmPrefListener.prototype.unregister = function() {
    if (this._branch) this._branch.removeObserver('', this);
};

function HashMap(obj) {
    this._length = 0;
    this._items = {};
    for (let p in obj) {
        if (obj.hasOwnProperty(p)) {
            this._items[p] = obj[p];
            this._length++;
        }
    }

    this.put = function(key, value) {
        let previous = undefined;
        if (this.containsKey(key)) {
            previous = this._items[key];
        } else {
            this._length++;
        }
        this._items[key] = value;
        return previous;
    }

    this.get = function(key) {
        return this.containsKey(key) ? this._items[key] : undefined;
    }

    this.containsKey = function(key) {
        return this._items.hasOwnProperty(key);
    }
   
    this.remove = function(key) {
        if (this.containsKey(key)) {
            let previous = this._items[key];
            this._length--;
            delete this._items[key];
            return previous;
        }
        else {
            return undefined;
        }
    }

    this.keys = function() {
        let keys = [];
        for (let k in this._items) {
            if (this.containsKey(k)) {
                keys.push(k);
            }
        }
        return keys;
    }

    this.values = function() {
        let values = [];
        for (let k in this._items) {
            if (this.containsKey(k)) {
                values.push(this._items[k]);
            }
        }
        return values;
    }

    this.each = function(fn) {
        for (let k in this._items) {
            if (this.containsKey(k)) {
                fn(k, this._items[k]);
            }
        }
    }

    this.clear = function() {
        this._items = {}
        this._length = 0;
    }
}

console.trace("Loading bmUtils.jsm");