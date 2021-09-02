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

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

Services.scriptloader.loadSubScript("chrome://bm/content/notifyTools.js", null, "UTF-8");

var gBMMonitor = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMMonitor: "),
    _listenedAb: new HashMap(),
    _isListening: false,
    _copiedOrMoved: new HashMap(),
    _abManager: Components.classes["@mozilla.org/abmanager;1"]
                        .getService().QueryInterface(Components.interfaces.nsIAbManager),
    _observerService: Components.classes["@mozilla.org/observer-service;1"]
                        .getService(Components.interfaces.nsIObserverService),
    setupListeners: function() {
        let self = this;
        notifyTools.registerListener((data) => {
            switch (data.command) {
                case "onContactCreated":
                    let dirAdd = self.getAbDirectoryFromUid(data.contact.parentId);
                    let cardAdd = dirAdd.getCardFromProperty("bmNewId", data.contact.id, false);
                    self.notifyCardAdded(dirAdd, cardAdd);
                    break;
                case "onContactUpdated":
                    let dirUp = self.getAbDirectoryFromUid(data.contact.parentId);
                    let cardUp = dirUp.getCardFromProperty("bm-id", data.contact.id, false);
                    if (cardUp) {
                        self.notifyCardModified(dirUp, cardUp);
                    }
                    break;
                case "onContactDeleted":
                    let dirDel = self.getAbDirectoryFromUid(data.contact.parentId);
                    self.notifyCardRemoved(dirDel, data.contact.id);
                    break;
                case "onListCreated":
                    let lDirAdd = self.getAbDirectoryFromUid(data.list.parentId);
                    let listAdd = self._getListById(lDirAdd, data.list.id);
                    self.notifyListAdded(lDirAdd, listAdd);
                    break;
                case "onListUpdated":
                    let lDirUp = self.getAbDirectoryFromUid(data.list.parentId);
                    let listUp = self._getListById(lDirUp, data.list.id);
                    self.notifyListModified(lDirUp, listUp);
                    break;
                case "onListDeleted":
                    let lDirDel = self.getAbDirectoryFromUid(data.list.parentId);
                    self.notifyListRemoved(lDirDel, data.list.id);
                    break;
            }
        });
        notifyTools.enable();
    },
    startListening: function() {
        this._logger.info("Start Listening");
        this._listenedAb.clear();
        this._copiedOrMoved.clear();
        let it = this._abManager.directories;
        for (let directory of it) {
            if (directory instanceof Components.interfaces.nsIAbDirectory) {
                console.log("listen dir:" + directory.URI + " UID:" + directory.UID);
                let id = bmUtils.getCharPref(directory.URI + ".bm-id", null);
                if (id != null && !directory.URI.indexOf("bmdirectory://") == 0) {
                    this.listenDirectory(directory.UID, id);
                }
            }
        }
        //this._abManager.addAddressBookListener(this, Components.interfaces.nsIAbListener.all);
        this._isListening = true;
        notifyTools.notifyBackground({command: "startAbListening"});
    },
    listenDirectory: function(uid, bmId) {
        this._logger.info("Listening: [" + uid + "][" + bmId + "]");
        this._listenedAb.put(uid, bmId);
    },
    stopListening: function() {
        if (!this._isListening) return;
        try {
            //this._abManager.removeAddressBookListener(this);
            this._isListening = false;
            this._logger.info("Stopped Listening");
            notifyTools.notifyBackground({command: "stopAbListening"});
        } catch(e) {
            //ok
        }
    },
    getAbDirectoryFromUri: function(aUri) {
        return this._abManager.getDirectory(aUri);
    },
    getAbDirectoryFromUid: function(aUid) {
        return this._abManager.getDirectoryFromUID(aUid);
    },
    /*record local mod*/
    notifyCardAdded: function(directory, card) {
        this._logger.debug("card [" + card.displayName + "] added in [" + directory.dirName + "]");
        //this._dumpCard(card);
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            let cardsToDel = [];
            cardsToDel.push(card);
            directory.deleteCards(cardsToDel);
            let msg = new BMXPComObject();
            msg.type = "card";
            this._observerService.notifyObservers(msg, "bm-ab-observe", "readonly");
            return;
        }

        let manual = card.getProperty("bm-created-from-dialog", false);
        card.setProperty("bm-created-from-dialog", null);
        let found = false;
        if (!manual && bmService.listAddLocalMemberAckEnabled) {
            this._logger.debug("card added from list");
            for (let uid of this._listenedAb.keys()) {
                if (directory.UID == uid) continue;
                let dir = this.getAbDirectoryFromUid(uid);
                let original = dir.cardForEmailAddress(card.primaryEmail);
                if (original != null) {
                    found = true;
                    card.setProperty("bm-id", original.getProperty("bm-id", null));
                    card.setProperty("bm-local", "true");
                    this._logger.debug("=> original card found in [" + dir.dirName + "] : marked as bm-local");
                    break;
                }
            }
        }
        if(!found) {
            let destId = card.getProperty("bmNewId", null);
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
        this._saveChanges(directory, card);
    },
    notifyListAdded: function(directory, list) {
        this._logger.debug("list [" + list.dirName + "] added in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            directory.deleteDirectory(list);
            let msg = new BMXPComObject();
            msg.type = "list";
            this._observerService.notifyObservers(msg, "bm-ab-observe", "readonly");
            return;
        }
        let pref = list.UID;
        let newId = list.UID;
        this._logger.debug("new id:" + newId);
        bmUtils.setCharPref(pref + ".bm-id", newId);
        bmUtils.setBoolPref(pref + ".bm-added", true);
        this._logger.debug("=> list marked as added");
    },
    notifyCardRemoved: function(directory, id) {
        this._logger.debug("card [" + id + "] removed from [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        if (this._copiedOrMoved.containsKey(id)) {
            let oldInfos = this._copiedOrMoved.get(id);
            let srcDir = this.getAbDirectoryFromUri(oldInfos.srcUri);
            if (srcDir) {
                this._logger.debug("card has moved and id changed => delete from src dir");
                this._markCardDeleted(oldInfos.srcUri, oldInfos.oldId);
            }
            this._copiedOrMoved.remove(id);
        } else {
            this._markCardDeleted(directory.URI, id);
        }
    },
    notifyListRemoved: function(directory, uid) {
        this._logger.debug("list [" + uid + "] removed from [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let pref = uid;
        let id = bmUtils.getCharPref(pref + ".bm-id", null);
        if (!id) {
            //not a bm item or in added
        } else {
            //bm item
            let del = bmUtils.getCharPref(directory.URI + ".deleted.lists", "");
            if (del.length > 0) {
                del += "|";
            }
            del += id;
            bmUtils.setCharPref(directory.URI + ".deleted.lists", del);
            bmUtils.deletePrefBranch(pref);
            //remove local members
            let cardsToDel = [];
            let cards = directory.getCardsFromProperty("bm-parent", id, false);
            for (let c of cards) {
                let card = c.QueryInterface(Components.interfaces.nsIAbCard);
                if (card) cardsToDel.push(card);
            }
            directory.deleteCards(cardsToDel);
            this._logger.debug("=> list marked as deleted");
        }
    },
    notifyCardModified: function(directory, card) {
        this._logger.debug("card [" + card.displayName + "] modified in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let isAdded = card.getProperty("bm-added", "false");
        let isUpdated = card.getProperty("bm-updated", "false");
        if (isAdded == "true" || isUpdated == "true") {
            //already in added or updated
        } else {
            card.setProperty("bm-updated", "true");
            this._logger.debug("=> card marked as updated");
            this._saveChanges(directory, card);
        }
    },
    notifyListModified: function(directory, list) {
        this._logger.debug("list [" + list.dirName + "] modified in [" + directory.dirName + "]");
        if (bmUtils.isBmDirectory(directory) && bmUtils.isBmReadOnlyAddressbook(directory)) {
            this._logger.debug("ERROR directory[" + directory.dirName + "] is not writable -> do not store change");
            return;
        }
        let pref = list.UID;
        let id = bmUtils.getCharPref(pref + ".bm-id", null);
        if (!id) {
            //not a bm item nothing todo
        } else {
            //bm item
            let isAdded = bmUtils.getBoolPref(pref + ".bm-added", false);
            let isUpdated = bmUtils.getBoolPref(pref + ".bm-updated", false);
            if (isAdded || isUpdated) {
                //already in added or updated
            } else {
                bmUtils.setBoolPref(pref + ".bm-updated", true);
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
        for (let p of it) {
            let property = p.QueryInterface(Components.interfaces.nsIProperty);
            this._logger.trace("[" + property.name + "] = " + property.value);
        }
    },
    _saveChanges: function(directory, card) {
        if (directory.wrappedJSObject.saveCardProperties) {
            directory.wrappedJSObject.saveCardProperties(card);
        } else {
            //TB 78
            directory.wrappedJSObject._saveCardProperties(card);
        }
    },
    _getListById: function(directory, id) {
        let it = directory.childNodes;
        for (let l of it) {
            let list = l.QueryInterface(Components.interfaces.nsIAbDirectory);
            if (list.UID == id) {
                return list;
            }
        }
        return null;
    },
}

gBMMonitor.setupListeners();
