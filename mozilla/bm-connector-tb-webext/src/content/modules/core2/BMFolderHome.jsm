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

this.EXPORTED_SYMBOLS = ["BMFolderHome", "BMFolder"];

var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var { Http } = ChromeUtils.import("resource://gre/modules/Http.jsm");
var { MailServices } = ChromeUtils.import("resource:///modules/MailServices.jsm");

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { bmPhotos } = ChromeUtils.import("chrome://bm/content/modules/bmPhotos.jsm");
var { BMContactHome, BMContact } = ChromeUtils.import("chrome://bm/content/modules/core2/BMContactHome.jsm");
var { BMDlistHome, BMDlist } = ChromeUtils.import("chrome://bm/content/modules/core2/BMDlistHome.jsm");

const historyDir = "jsaddrbook://history.sqlite";
const prevHistoryDir = "moz-abmdbdirectory://history.mab";

let disablePhotos = Components.classes["@mozilla.org/embedcomp/prompt-service;1"] ? false : true;
if (disablePhotos) {
    //TB 115 photo sync not working and causing phantom contact create/update
    console.warn("photo sync disabled on thunderbird 115 until fix");
}

let BMFolderHome = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                        .getLogger("BMFolderHome: "),
    _contactHome: new BMContactHome(),
    _dlistHome: new BMDlistHome(),
    _user: null, /*BMApi AuthUser*/
    _notify: {},
    init: function() {
        Services.scriptloader.loadSubScript("chrome://bm/content/notifyTools.js", this._notify, "UTF-8");
    },
    getFolders: function() {
        return Promise.resolve(this._getDirectories().map(this._directoryToFolder));
    },
    syncFolders: function(aFolders, aUser) {
        this._user = aUser;
        let directories = this._getDirectories();
        let directoriesById = new HashMap();
        for (let directory of directories) {
            let id = bmUtils.getCharPref(directory.URI + ".bm-id", null);
            directoriesById.put(id, directory);
        }
        let searchBooks = [];
        let isBmDomainAbRemote = false;
        for (let folder of aFolders) {
            this._logger.debug("on folder: " + folder.name);
            if (!folder.isReadable) continue;
            let isCollected = folder.id.indexOf("book:CollectedContacts") == 0 && folder.owner == this._user.uid;
            let isBmDomainAb = folder.isDefault && !folder.isWritable && folder.id == "addressbook_" + this._user.domainUid;
            let isSharedAb = folder.owner != this._user.uid && !isBmDomainAb;
            let dir = directoriesById.get(folder.id);
            let dirName = bmUtils.getBmAddressbookName(folder.name, folder.isDefault);
            if (isSharedAb) {
                dirName += " (" + folder.ownerDisplayname + ")";
            }
            let uri;
            if (dir) {
                this._logger.debug(" -> already exist");
                if (!isCollected) dir.dirName = dirName;
                uri = dir.URI;
                if (!folder.isSync) {
                    this._logger.debug(" sync disabled: remove local, use remote search ab");
                    searchBooks.push({id: folder.id, name: dirName});
                    if (isBmDomainAb) {
                        isBmDomainAbRemote = true;
                    }
                } else {
                    directoriesById.remove(folder.id);
                }
            } else {
                this._logger.debug(" -> create folder");
                if (isCollected && folder.isSync) {
                    uri = historyDir;
                } else {
                    if (folder.isSync) {
                        uri = this._createLocalDir(dirName);
                    } else {
                        this._logger.debug(" sync disabled: use remote search ab");
                        searchBooks.push({id: folder.id, name: dirName});
                        if (isBmDomainAb) {
                            isBmDomainAbRemote = true;
                        }
                    }
                }
            }
            bmUtils.setCharPref(uri + ".bm-id", folder.id);
            bmUtils.setBoolPref(uri + ".bm-default", folder.isDefault);
            bmUtils.setBoolPref(uri + ".bm-writable", folder.isWritable);
            bmUtils.setCharPref(uri + ".bm-owner", folder.owner);
            bmUtils.setCharPref(uri + ".bm-ownerDisplayName", folder.ownerDisplayname ? folder.ownerDisplayname : "");
            bmUtils.setBoolPref(uri + ".bm-domain-ab", isBmDomainAb);
            bmUtils.setBoolPref(uri + ".bm-shared", isSharedAb);
        }
        for (let id of directoriesById.keys()) {
            let toRemove = directoriesById.get(id);
            let uri = toRemove.URI;
            this._logger.debug("remove directory: " + toRemove.dirName);
            MailServices.ab.deleteAddressBook(uri);
            bmUtils.deletePrefBranch(uri);
        }
        bmUtils.setBoolPref("bm-domain-ab-remote", isBmDomainAbRemote);
        this._notify.notifyTools.notifyBackground({command: "setupSearchBooks", books: searchBooks});
        return Promise.resolve();
    },
    getLocalChangeSet: function(aFolder) {
        let directory = this._getDirectoryById(aFolder.id);
        this._logger.debug("get local changes in folder:" + directory.dirName);
    
        let changes = [];
        if (!bmUtils.getBoolPref(directory.URI + ".bm-writable", false)) {
            this._logger.debug("readonly: skip");
            return Promise.resolve(changes);
        }
        
        let removed = bmUtils.getCharPref(directory.URI + ".deleted.cards", "");
        removed = removed.split("|");
        if (!removed[0] == "") {
            for (let rmed of removed) {
                changes.push({
                    type: "removed",
                    itemId: rmed
                });
                this._logger.debug("to remove:" + rmed);
            }
        }
        
        let added = directory.getCardsFromProperty("bm-added", "true", false);
        for (let c of added) {
            let card = c.QueryInterface(Components.interfaces.nsIAbCard);
            let error = card.getProperty("bm-error-code", null);
            if (error) {
                this._logger.info("skip added:" + card.displayName + " with error:" + error);
                continue;
            }
            let id = card.getProperty("bm-id", null);
            changes.push({
                type: "added",
                itemId: id,
            });
            this._logger.debug("to add:" + card.displayName);
        }
        
        let updated = directory.getCardsFromProperty("bm-updated", "true", false);
        for (let c of updated) {
            let card = c.QueryInterface(Components.interfaces.nsIAbCard);
            let error = card.getProperty("bm-error-code", null);
            if (error) {
                this._logger.info("skip updated:" + card.displayName + " with error:" + error);
                continue;
            }
            let id = card.getProperty("bm-id", null);
            changes.push({
                type: "updated",
                itemId: id,
            });
            this._logger.debug("to update:" + card.displayName);
        }
        
        removed = bmUtils.getCharPref(directory.URI + ".deleted.lists", "");
        removed = removed.split("|");
        if (!removed[0] == "") {
            for (let rmed of removed) {
                changes.push({
                    type: "removed",
                    itemId: rmed
                });
                this._logger.debug("to remove:" + rmed);
            }
        }
        
        let it = directory.childNodes;
        for (let d of it) {
            let dir = d.QueryInterface(Components.interfaces.nsIAbDirectory);
            this._logger.debug("get local changes for list:" + dir.dirName);
            let pref = dir.UID;
            let id = bmUtils.getCharPref(pref + ".bm-id", null);
            if (id == null) continue;
            let isAdded = bmUtils.getBoolPref(pref + ".bm-added", false);
            let isUpdated = bmUtils.getBoolPref(pref + ".bm-updated", false);
            if (!isAdded && !isUpdated) continue;
            if (isAdded) {
                changes.push({
                    type: "added",
                    itemId: id,
                    kind: "group"
                });
                this._logger.debug("to add:" + id);
            } else if (isUpdated) {
                changes.push({
                    type: "updated",
                    itemId: id,
                    kind: "group"
                });
                this._logger.debug("to update:" + id);
            }
        }
        
        return Promise.resolve(changes);
    },
    getLocalPhotos: function(aFolder, aChanges) {
        if (disablePhotos) {
            return Promise.resolve([]);
        }
        let directory = this._getDirectoryById(aFolder.id);
        let photos = [];
        for (let chg of aChanges) {
            if (chg.type == "add" && chg.change.value.kind == "individual"
                && chg.change.value.identification.photo ) {
                let add = chg.change;
                let card = directory.getCardFromProperty("bm-id", add.uid, false);
                this._logger.debug("send photo for created contact:" + add.uid);
                photos.push({
                    data: bmPhotos.getPhoto(card),
                    contactId: add.uid
                });
            } else if (chg.type == "modify" && chg.change.value.kind == "individual") {
                let mod = chg.change;
                let card = directory.getCardFromProperty("bm-id", mod.uid, false);
                let oldPhoto = card.getProperty("bm-PhotoName", null);
                let newPhoto = card.getProperty("PhotoName", null);
                if (oldPhoto != newPhoto) {
                    this._logger.debug("change photo for updated contact:" + mod.uid);
                    photos.push({
                        data: bmPhotos.getPhoto(card),
                        contactId: mod.uid
                    });
                }
            }
        }
        return Promise.resolve(photos);
    },
    getFolder: function(aFolderId) {
        let directory = this._getDirectoryById(aFolderId);
        if (directory) {
            return Promise.resolve(this._directoryToFolder(directory));
        } else {
            return Promise.resolve(null);//FIXME reject ?
        }
    },
    getFolderSyncVersion: function(aFolder) {
        let directory = this._getDirectoryById(aFolder.id);
        if (directory) {
            return Promise.resolve(bmUtils.getCharPref(directory.URI + ".bm-syncVersion", 0));
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    setFolderSyncVersion: function(aFolder, aSyncVersion) {
        let directory = this._getDirectoryById(aFolder.id);
        if (directory) {
            bmUtils.setCharPref(directory.URI + ".bm-syncVersion", aSyncVersion);
            return Promise.resolve();
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    syncFolderEntries: function(aFolder, aChangedValues, aDeletedItems, aChangesetVersion) {
        let directory = this._getDirectoryById(aFolder.id);
        if (directory) {
            let upDlists = [];
            for (let entry of aChangedValues) {
                if (entry['value'].kind == "group") {
                    upDlists.push(entry);
                    continue;
                }
                let card = directory.getCardFromProperty("bm-id", entry.id, false);
                let toCreate = false;
                if (!card) {
                    card = Components.classes["@mozilla.org/addressbook/cardproperty;1"]
                                .createInstance(Components.interfaces.nsIAbCard);
                    toCreate = true;
                }
                card.setProperty("bm-ab-uri", directory.URI);
                card.setProperty("bm-added", "false");
                card.setProperty("bm-updated", "false");
                card.setProperty("bm-folder", aFolder.id);
                card.setProperty("PreferDisplayName", true);
                this._entryToCard(entry, card);
                this._setPhoto(entry, aFolder.id, card);
                if (toCreate) {
                    card.UID = entry.id;
                    directory.addCard(card);
                } else {
                    directory.modifyCard(card);
                }
            }
            
            for (let entry of upDlists) {
                let list = this._getListById(directory, entry.id);
                if (!list) {
                    list = Components.classes["@mozilla.org/addressbook/directoryproperty;1"].createInstance()
                                    .QueryInterface(Components.interfaces.nsIAbDirectory);
                    list.isMailList = true;
                    list.dirName = entry.value.identification.formatedName.value;
                    list = directory.addMailList(list);
                }
                bmUtils.setBoolPref(list.UID + ".bm-added", false);
                bmUtils.setBoolPref(list.UID + ".bm-updated", false);
                let dlist = this._entryToDlist(entry, list);
                dlist.setMembers(directory, entry.value.organizational.member);
                list.editMailListToDatabase(null);
            }
            
            let cardsToDel = [];
            for (let del of aDeletedItems) {
                let card = directory.getCardFromProperty("bm-id", del, false);
                if (card) {
                    cardsToDel.push(card);
                } else {
                    let list = this._getListById(directory, del);
                    if (list) {
                        let l = new BMDlist(list);
                        l.deleteLocalMembers(directory, del);
                        let pref = list.UID;
                        directory.deleteDirectory(list);
                        bmUtils.deletePrefBranch(pref);
                    }
                }
            }
            directory.deleteCards(cardsToDel);
            bmUtils.setCharPref(directory.URI + ".deleted.cards", "");
            bmUtils.setCharPref(directory.URI + ".deleted.lists", "");
            bmUtils.setCharPref(directory.URI + ".bm-syncVersion", aChangesetVersion);
            return Promise.resolve("ok");
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    getEntry: function(aFolder, aEntryId, aKind) {
        let directory = this._getDirectoryById(aFolder.id);
        if (directory) {
            if (aKind == "group") {
                let list = this._getListById(directory, aEntryId);
                if (list) {
                    return Promise.resolve(this._listToEntry(list, directory));
                } else {
                    return Promise.reject(new Error("Entry [" + aEntryId + "] not exist in folder [" + aFolder.name +"]"));
                }
            } else {
                let card = directory.getCardFromProperty("bm-id", aEntryId, false);
                if (card) {
                    return Promise.resolve(this._cardToEntry(card));
                } else {
                    return Promise.reject(new Error("Entry [" + aEntryId + "] not exist in folder [" + aFolder.name +"]"));
                }
            }
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    setErrors: function(aFolder, aErrors) {
        let directory = this._getDirectoryById(aFolder.id);
        this._logger.debug("setErrors:" + aFolder.name + ", "+ aErrors.length);
        if (directory) {
            for (let error of aErrors) {
                let card = directory.getCardFromProperty("bm-id", error.uid, false);
                if (card) {
                    card.setProperty("bm-error-code", error.errorCode);
                    card.setProperty("bm-error-message", error.message);
                    directory.modifyCard(card);
                } else {
                    let list = this._getListById(directory, error.uid);
                    if (list) {
                        bmUtils.setCharPref(list.UID + ".bm-error-code", error.errorCode);
                        bmUtils.setCharPref(list.UID + ".bm-error-message", error.message);
                    }
                }
            }
            return Promise.resolve();
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    ackChanges: function(aFolder, aUids) {
        let directory = this._getDirectoryById(aFolder.id);
        this._logger.debug("ackChanges:" + aFolder.name + ", "+ aUids.length);
        if (directory) {
            for (let uid of aUids) {
                let card = directory.getCardFromProperty("bm-id", uid, false);
                if (card) {
                    card.setProperty("bm-added", "false");
                    card.setProperty("bm-updated", "false");
                    directory.modifyCard(card);
                } else {
                    let list = this._getListById(directory, uid);
                    if (list) {
                        bmUtils.setBoolPref(list.UID + ".bm-added", false);
                        bmUtils.setBoolPref(list.UID + ".bm-updated", false);
                    }
                }
            }
            return Promise.resolve();
        } else {
            return Promise.reject(new Error("Folder [" + aFolder.name + "] not exist"));
        }
    },
    isReadable: function(aVerbs) {
        return aVerbs.indexOf("All") != -1
            || aVerbs.indexOf("Write") != -1
            || aVerbs.indexOf("Read") != -1;
    },
    _isRemoteDir: function(aUri) {
        return aUri.indexOf("bmdirectory://") == 0;
    },
    _getDirectoryById: function(aId) {
        let it = MailServices.ab.directories;
        for (let directory of it) {
            if (directory instanceof Components.interfaces.nsIAbDirectory) {
                let id = bmUtils.getCharPref(directory.URI + ".bm-id", null);
                //this._logger.debug("_getDirectoryById: " + directory.dirName + " uri: " + directory.URI + " id: " + id);
                if (id && id == aId) {
                    this._logger.debug("found");
                    return directory;
                }
            }
        }
        return null;
    },
    _getDirectories: function() {
        let ret = [];
        let it = MailServices.ab.directories;
        for (let directory of it) {
            if (directory instanceof Components.interfaces.nsIAbDirectory) {
                let id = bmUtils.getCharPref(directory.URI + ".bm-id", null);
                //this._logger.debug("_getDirectories: " + directory.dirName + " uri: " + directory.URI + " id: " + id);
                if (id) {
                    ret.push(directory);
                }
            }
        }
        return ret;
    },
    _createLocalDir: function(aDirName) {
        this._logger.debug("createLocalDir:" + aDirName);
        let pref = MailServices.ab.newAddressBook(aDirName, "", 101);
        let uri = "jsaddrbook://" + bmUtils.getCharPref(pref + ".filename");
        return uri;
    },
    _createRemoteDir: function(aDirName, aId) {
        this._logger.debug("_createRemoteDir:" + aDirName + ",id:" + aId);
        let pref = "ldap_2.servers." + aId.split(".").join("$$");
        let uri = "bmdirectory://" + pref;
        MailServices.ab.newAddressBook(aDirName, uri, 0, pref);
        return uri;
    },
    _getListById: function(aParent, aId) {
        let it = aParent.childNodes;
        for (let l of it) {
            let list = l.QueryInterface(Components.interfaces.nsIAbDirectory);
            let id = bmUtils.getCharPref(list.UID + ".bm-id", null);
            if (id && id == aId) {
                return list;
            }
        }
        return null;
    },
    _directoryToFolder: function(/*nsIAbDirectory*/ aDirectory) {
        let folder = new BMFolder();
        folder.id = bmUtils.getCharPref(aDirectory.URI + ".bm-id", null);
        folder.name = aDirectory.dirName;
        return folder;
    },
    _cardToEntry: function(/*nIAbCard*/ aCard) {
        let contact = new BMContact(aCard);
        return this._contactHome.asEntry(contact);
    },
    _entryToCard: function(aEntry, /*nsIAbCard*/ aCard) {
        let contact = new BMContact(aCard);
        this._contactHome.fillContactFromEntry(aEntry, contact);
        contact.beforeSave();
    },
    _listToEntry: function(/*nsIAbDirectory*/ aList, /*nsIAbDirectory*/ parent) {
        let list = new BMDlist(aList);
        return this._dlistHome.asEntry(list, parent);
    },
    _entryToDlist: function(aEntry, /*nsIAbDirectory*/ aList) {
        let list = new BMDlist(aList);
        return this._dlistHome.fillDlistFromEntry(aEntry, list);
    },
    _setPhoto: function(aEntry, aContaineId, /*nsIAbCard*/ aCard) {
        if (disablePhotos) return;
        if (aEntry.value.identification.photo) {
            let photoUri = bmUtils.session.baseUrl + "/api/addressbooks/" + aContaineId + "/" + aEntry.id + "/photo";
            this._logger.debug("get photo: " + photoUri);
            bmPhotos.setPhoto(aCard, photoUri);
        } else {
            bmPhotos.removePhoto(aCard);
        }
    }
}

function BMFolder() {
}

BMFolder.prototype = {
    mId: null,
    get id() {
        return this.mId;
    },
    set id(value) {
        return (this.mId = value);
    },
    mName: null,
    get name() {
        return this.mName;
    },
    set name(value) {
        return (this.mName = value);
    }
}

BMFolderHome.init();