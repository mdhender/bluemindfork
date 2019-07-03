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

/*Detect local changes*/

Components.utils.import("resource://bm/bmUtils.jsm");

var gBMMonitor = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMMonitor: "),
    _listenedAb: new HashMap(),
    _isListening: false,
    _copiedOrMoved: new HashMap(),
    _abManager: Components.classes["@mozilla.org/abmanager;1"]
                        .getService().QueryInterface(Components.interfaces.nsIAbManager),
    _observerService: Components.classes["@mozilla.org/observer-service;1"]
                        .getService(Components.interfaces.nsIObserverService),
    startListening: function() {
        this._logger.info("Start Listening");
        this._listenedAb.clear();
        this._copiedOrMoved.clear();
        let it = this._abManager.directories;
        while (it.hasMoreElements()) {
            let directory = it.getNext();
            if (directory instanceof Components.interfaces.nsIAbDirectory) {
                let id = bmUtils.getCharPref(directory.URI + ".bm-id", null);
                if (id != null && !directory.URI.indexOf("bmdirectory://") == 0) {
                    this.listenDirectory(directory.URI, id);
                }
            }
        }
        this._abManager.addAddressBookListener(this, Components.interfaces.nsIAbListener.all);
        this._isListening = true;
    },
    listenDirectory: function(uri, id) {
        this._logger.info("Listening: [" + uri + "][" + id + "]");
        this._listenedAb.put(uri, id);
    },
    stopListening: function() {
        if (!this._isListening) return;
        try {
            this._abManager.removeAddressBookListener(this);
            this._isListening = false;
            this._logger.info("Stopped Listening");
        } catch(e) {
            //ok
        }
    },
    isListenedDirectory: function(directory) {
        this._logger.debug("directory [" + directory.URI + "]");
        if(directory.isMailList) {
            this._logger.debug("directory [" + directory.URI + "] is a Mailing List");
            return false;
        }
        return this._listenedAb.containsKey(directory.URI);
    },
    getAbDirectoryFromUri: function(aUri) {
        return this._abManager.getDirectory(aUri);
    },
    /*nsIAbListener*/
    onItemAdded: function(parentDir, item) {
        this._logger.debug("onItemAdded(parentDir:" + parentDir + ", item:" + item);
        
        let directory = parentDir.QueryInterface(Components.interfaces.nsIAbDirectory);
        let readonly = false;
        if (this.isListenedDirectory(directory)) {
            readonly = bmUtils.isBmReadOnlyAddressbook(directory);
        } else {
            return;
        }
        try {
            let card = item.QueryInterface(Components.interfaces.nsIAbCard);
            if (!card.isMailList) {
                if (readonly) {
                    this._logger.debug("addressbook is not writable");
                    let cardsToDel = Components.classes["@mozilla.org/array;1"].createInstance(Components.interfaces.nsIMutableArray);
                    cardsToDel.appendElement(item, false);
                    directory.deleteCards(cardsToDel);
                    let msg = new BMXPComObject();
                    msg.type = "card";
                    this._observerService.notifyObservers(msg, "bm-ab-observe", "readonly");
                    return;
                }
                this.notifyCardAdded(directory, card);
            } else {
                this._logger.debug("list card is added : ignored");
            }
        } catch(e) {
            // exception QueryInterface as Card
            try {
                let list = item.QueryInterface(Components.interfaces.nsIAbDirectory);
                if (list.isMailList) {
                    if (readonly) {
                        directory.deleteDirectory(list);
                        let msg = new BMXPComObject();
                        msg.type = "list";
                        this._observerService.notifyObservers(msg, "bm-ab-observe", "readonly");
                        return;
                    }
                    this.notifyListAdded(directory, list);
                } else {
                    this._logger.debug("directory is added : ignored");
                }
            } catch(e) {
                this._logger.error("ERROR onItemAdded:" + e);
            }
        }
    },
    onItemRemoved: function(parentDir, item) {
        this._logger.debug("onItemRemoved(parentDir:" + parentDir + ", item:" + item);
        
        let directory = parentDir.QueryInterface(Components.interfaces.nsIAbDirectory);
        if (directory.isMailList) {
            bmUtils.setBoolPref(directory.URI + ".bm-updated", true);
            this._logger.debug("=> list marked as updated");
            return;
        }
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        try {
            let card = item.QueryInterface(Components.interfaces.nsIAbCard);          
            if (this.isListenedDirectory(directory)) {
                if (card.isMailList) {
                    this._logger.debug("list card is removed : ignored");
                    return;
                }
                this.notifyCardRemoved(directory, card);
            }
        } catch (e) {
            // exception QueryInterface as Card
            try {
                let list = item.QueryInterface(Components.interfaces.nsIAbDirectory);
                if (list.isMailList) {
                    if (this.isListenedDirectory(directory)) {
                        this.notifyListRemoved(directory, list);
                    }
                } else {
                    if (this.isListenedDirectory(list)) {
                        this.notifyDirectoryDeleted(list);
                    }
                }
            } catch(e) {
                this._logger.error("ERROR onItemRemoved:" + e);
            }
        }
    },
    onItemPropertyChanged: function(item, property, oldValue, newValue) {
        this._logger.debug("onItemPropertyChanged(item:" + item + ", property:" + property
                    + ", oldValue:" + oldValue + ", newValue:" + newValue);
        try {
            let card = item.QueryInterface(Components.interfaces.nsIAbCard);
            if (card.isMailList) {
                this._logger.debug("list card is updated : ignored");
                return;
            }
            //no way to know directly witch directory card belong
            let uri = card.getProperty("bm-ab-uri", null);
            this._logger.debug("card bm-ab-uri:" + uri);
            //this._dumpCard(card);
            if (uri) {
                this.notifyCardModified(this.getAbDirectoryFromUri(uri), card);  
            } else {
                //no "bm-ab-uri" in added
            }
        } catch (e) {
            try {
                let list = item.QueryInterface(Components.interfaces.nsIAbDirectory);
                if (list.isMailList) {
                    //get URI of parent
                    let uri = "";
                    let abUri = list.URI.split("/");
                    if (abUri.length == 4 && abUri[0] == "moz-abmdbdirectory:" && abUri[3] != "") {
                        uri = abUri[0] + "/" + abUri[1] + "/" + abUri[2];
                        this._logger.debug("list parent uri:" + uri);
                    } else {
                        this._logger.debug("invalid list uri:" + list.URI);
                        return;
                    }
                    this.notifyListModified(this.getAbDirectoryFromUri(uri), list);
                } else {
                    this._logger.debug("directory is updated : ignored");
                }
            } catch (e) {
                this._logger.error("ERROR onItemPropertyChanged:" + e);
            }
        }
    },
    /*record local mod*/
    notifyCardAdded: function(directory, card) {
        this._logger.debug("card [" + card.displayName + "] added in [" + directory.dirName + "]");
        //this._dumpCard(card);
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let notes = card.getProperty("Notes", "not exist");
        let found = false;
        if (notes == "not exist" && bmService.listAddLocalMemberAckEnabled) {
            this._logger.debug("card added from list");
            for (let uri of this._listenedAb.keys()) {
                if (directory.URI == uri) continue;
                let dir = this.getAbDirectoryFromUri(uri);
                let original = dir.cardForEmailAddress(card.primaryEmail);
                if (original != null) {
                    found = true;
                    card.setProperty("bm-id", original.getProperty("bm-id", null));
                    card.setProperty("bm-local", "true");
                    this._logger.debug("=> original card found: marked as bm-local");
                    break;
                }
            }
        }
        if(!found) {
            let destId = bmUtils.randomUUID();
            let srcId = card.getProperty("bm-id", null);
            if (srcId) {
                let srcUri = card.getProperty("bm-ab-uri", null);
                if (srcUri) {
                    this._logger.debug("card [" + srcId + "] copied or moved from directory [" + srcUri + "]");
                    this._copiedOrMoved.put(destId, {oldId: srcId, srcUri: srcUri});
                }
            }
            this._logger.debug("new id:" + destId);
            card.setProperty("bm-id", destId);
            card.setProperty("bm-added", "true");
            card.setProperty("bm-local", "false");
            card.setProperty("bm-ab-uri", null);
            this._logger.debug("=> card marked as added");
        }
        directory.QueryInterface(Components.interfaces.nsIAbMDBDirectory);
        directory.database.editCard(card, false, null);
    },
    notifyListAdded: function(directory, list) {
        this._logger.debug("list [" + list.dirName + "] added in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let uri = list.URI;
        let newId = bmUtils.randomUUID();
        this._logger.debug("new id:" + newId);
        bmUtils.setCharPref(uri + ".bm-id", newId);
        bmUtils.setBoolPref(uri + ".bm-added", true);
        this._logger.debug("=> list marked as added");
    },
    notifyCardRemoved: function(directory, card) {
        this._logger.debug("card [" + card.displayName + "] removed from [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let id = card.getProperty("bm-id", null);
        this._logger.debug("id:" + id);
        if (!id) {
            //not a bm item
        } else {
            //bm item
            if (this._copiedOrMoved.containsKey(id)) {
                let oldInfos = this._copiedOrMoved.get(id);
                let srcDir = this.getAbDirectoryFromUri(oldInfos.srcUri);
                if (srcDir) {
                    this._logger.debug("card has moved and id changed => delete from src dir");
                    this._markCardDeleted(oldInfos.srcUri, oldInfos.oldId);
                }
                this._copiedOrMoved.remove(id);
            } else {
                let isAdded = card.getProperty("bm-added", "false");
                let isUpdated = card.getProperty("bm-updated", "false");
                this._logger.debug("isAdded:" + isAdded);
                this._logger.debug("isUpdated:" + isUpdated);
                if (isAdded == "true" || isUpdated == "true") {
                    //already in added or updated
                } else {
                    this._markCardDeleted(directory.URI, id);
                }
            }
        }
    },
    notifyListRemoved: function(directory, list) {
        this._logger.debug("list [" + list.dirName + "] removed from [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let uri = list.URI;
        let id = bmUtils.getCharPref(uri + ".bm-id", null);
        if (!id) {
            //not a bm item or in added
        } else {
            //bm item
            let isAdded = bmUtils.getBoolPref(uri + ".bm-added", false);
            let isUpdated = bmUtils.getBoolPref(uri + ".bm-updated", false);
            if (isAdded || isUpdated) {
                //already in added or updated
            } else {
                let del = bmUtils.getCharPref(directory.URI + ".deleted.lists", "");
                if (del.length > 0) {
                    del += "|";
                }
                del += id;
                bmUtils.setCharPref(directory.URI + ".deleted.lists", del);
                bmUtils.deletePrefBranch(uri);
                //remove local members
                let cardsToDel = Components.classes["@mozilla.org/array;1"].createInstance(Components.interfaces.nsIMutableArray);
                let cards = directory.getCardsFromProperty("bm-parent", id, false);
                while (cards.hasMoreElements()) {
                    let card = cards.getNext().QueryInterface(Components.interfaces.nsIAbCard);
                    if (card) cardsToDel.appendElement(card, false);
                }
                directory.deleteCards(cardsToDel);
                this._logger.debug("=> list marked as deleted");
            }
        }
    },
    notifyDirectoryDeleted: function(directory) {
        this._logger.debug("directory [" + directory.dirName + "] removed");
        let id = this._listenedAb.get(directory.URI);
        if (id != null && id.indexOf("temp-") == -1) {
            //is bm directory and not new
            let del = bmUtils.getCharPref("extensions.bm.folders.deleted", "");
            if (del.length > 0) {
                del += "|";
            }
            del += id;
            bmUtils.setCharPref("extensions.bm.folders.deleted", del);
            //remove bm prefs
            bmUtils.deletePrefBranch(directory.URI);
            this._logger.debug("=> directory marked as deleted");
        }
    },
    notifyCardModified: function(directory, card) {
        this._logger.debug("card [" + card.displayName + "] modified in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let id = card.getProperty("bm-id", null);
        if (!id) {
            //not a bm item nothing todo
        } else {
            let c = directory.getCardFromProperty("bm-id", id, false);
            //this._dumpCard(c);
            //bm item
            let isAdded = c.getProperty("bm-added", "false");
            let isUpdated = c.getProperty("bm-updated", "false");
            if (isAdded == "true" || isUpdated == "true") {
                //already in added or updated
            } else {
                c.setProperty("bm-updated", "true");
                this._logger.debug("=> card marked as updated");
                directory.QueryInterface(Components.interfaces.nsIAbMDBDirectory);
                directory.database.editCard(c, false, null);
            }
        }
    },
    notifyListModified: function(directory, list) {
        this._logger.debug("list [" + list.dirName + "] modified in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.error("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let uri = list.URI;
        let id = bmUtils.getCharPref(uri + ".bm-id", null);
        if (!id) {
            //not a bm item nothing todo
        } else {
            //bm item
            let isAdded = bmUtils.getBoolPref(uri + ".bm-added", false);
            let isUpdated = bmUtils.getBoolPref(uri + ".bm-updated", false);
            if (isAdded || isUpdated) {
                //already in added or updated
            } else {
                bmUtils.setBoolPref(uri + ".bm-updated", true);
                this._logger.debug("=> list marked as updated");
            }
        }
    },
    _markCardDeleted: function(aUri, aId) {
        let del = bmUtils.getCharPref(aUri + ".deleted.cards", "");
        if (del.length > 0) {
            del += "|";
        }
        del += aId;
        bmUtils.setCharPref(aUri + ".deleted.cards", del);
        this._logger.debug("=> card marked as deleted");
    },
    _dumpCard: function(aCard) {
        let it = aCard.properties;
        while (it.hasMoreElements()) {
            let property = it.getNext().QueryInterface(Components.interfaces.nsIProperty);
            this._logger.trace("[" + property.name + "] = " + property.value);
        }
    }
}
