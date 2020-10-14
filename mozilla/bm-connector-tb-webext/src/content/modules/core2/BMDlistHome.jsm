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

/* */

this.EXPORTED_SYMBOLS = ["BMDlistHome", "BMDlist"];

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

function BMDlistHome() {
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService()
                            .wrappedJSObject.getLogger("BMDlistHome: ");
}

BMDlistHome.prototype.asEntry = function(dlist, directory) {
	let entry = {};
	entry['id'] = dlist.getId();
    entry['extId'] = dlist.getExtId();
	entry['name'] = dlist.getLabel();
	entry['folder'] = dlist.getFolder();
	let card = {
		'identification' : {},
		'explanatory' : {},
		'organizational' : {
			'member' : []
		},
		'communications' : {},
		'deliveryAddressing' : []
	};

	entry['value'] = card;
	card.kind = 'group';
	card.identification.formatedName = {
		'value' : dlist.getLabel()
	};
    card.explanatory.note = dlist.getDescription();
    
	dlist.getMembers(directory).forEach(function(member) {
		let m = {
            'itemUid': member.id,
            'containerUid' : + member.fid,
            'commonName': member.displayName,
            'mailto': member.email
		};
		card.organizational.member.push(m);
	});
	return entry;
};

BMDlistHome.prototype.fillDlistFromEntry = function(entry, /*BMDlist*/ dlist) {
    this._logger.debug("fillDlistFromEntry");
    dlist.setId(entry['id']);
	dlist.setExtId(entry['extId']);
	dlist.setFolder(entry['folder']);

	let card = entry['value'];
	dlist.setLabel(card.identification.formatedName.value);
    dlist.setDescription(card.explanatory.note);
	return dlist;
};

function BMDlist(/*nsIAbDirectory*/ aList) {
    this._list = aList;
    this._logger = Components.classes["@blue-mind.net/logger;1"].getService()
                            .wrappedJSObject.getLogger("BMDlist: ");
}

BMDlist.prototype = {
    getId: function() {
        bmUtils.getCharPref(this._list.URI + ".bm-id", null);
    },
    setId: function(value) {
        bmUtils.setCharPref(this._list.URI + ".bm-id", value);
    },
    getExtId: function() {
        bmUtils.getCharPref(this._list.URI + ".bm-extId", null);
    },
    setExtId: function(value) {
        bmUtils.setCharPref(this._list.URI + ".bm-extId", value ? value: "");
    },
    getFolder: function() {
        bmUtils.getCharPref(this._list.URI + ".bm-folder", null);
    },
    setFolder: function(value) {
        bmUtils.setCharPref(this._list.URI + ".bm-folder", value ? value: "");
    },
    getLabel: function() {
        return this._list.dirName;
    },
    setLabel: function(value) {
        this._list.dirName = value;
    },
    getDescription: function() {
        return this._list.description;
    },
    setDescription: function(value) {
        this._list.description = value ? value: "";
    },
    getMembers: function(directory) {
        let members = [];
        let it = this._list.childCards;
        while (it.hasMoreElements()) {
            let card = it.getNext().QueryInterface(Components.interfaces.nsIAbCard);
            let id = card.getProperty("bm-id", null);
            let fid = bmUtils.getCharPref(directory.URI + ".bm-id", null);
            if (id && fid && card.primaryEmail) {
                members.push({
                    id: id,
                    fid: fid,
                    displayName: card.displayName,
                    email: card.primaryEmail
                });
            }
        }
        let extras = JSON.parse(bmUtils.getCharPref(this._list.URI + ".bm-extras-members", "[]"));
        extras.forEach(function(extra) {
            members.push({
                id: extra.itemUid,
                fid: extra.containerUid,
                displayName: extra.commonName,
                email: extra.mailto 
            });
        });
        return members;
    },
    setMembers: function(directory, value) {
        //remove existing members
        let cardsToDel = [];
        let it = this._list.childCards;
        while (it.hasMoreElements()) {
            let card = it.getNext().QueryInterface(Components.interfaces.nsIAbCard);
            cardsToDel.push(card);
        }
        this._list.deleteCards(cardsToDel);
        //delete local members
        this.deleteLocalMembers(directory);
        //set members
        let extras = [];
        for (let entry of value) {
            if(!entry.mailto) {
                this._logger.debug(" -> store member with no email: " + entry.commonName);
                extras.push(entry);
                continue;
            }
            let card = directory.getCardFromProperty("bm-id", entry.itemUid, false);
            if (!card) {
                this._logger.debug(" card not found in directory -> create local card for thunderbird");
                card = Components.classes["@mozilla.org/addressbook/cardproperty;1"]
                                    .createInstance(Components.interfaces.nsIAbCard);
                card.displayName = entry.commonName;
                card.primaryEmail = entry.mailto;
                card.setProperty("bm-id", entry.itemUid);
                card.setProperty("bm-local", "true");
                card.setProperty("bm-folder", bmUtils.getCharPref(directory.URI + ".bm-id", null));
                card.setProperty("bm-parent", this.getId());
                card.setProperty("bm-parent-name", directory.dirName);
                directory.addCard(card);
            }
            this._list.addCard(card);
        }
        bmUtils.setCharPref(this._list.URI + ".bm-extras-members", JSON.stringify(extras));
    },
    deleteLocalMembers: function(directory) {
        let cardsToDel = [];
        let cards = directory.getCardsFromProperty("bm-parent", this.getId(), false);
        if (cards) {
            while (cards.hasMoreElements()) {
                let card = cards.getNext().QueryInterface(Components.interfaces.nsIAbCard);
                if (card) cardsToDel.push(card);
            }
            directory.deleteCards(cardsToDel);
        }
    }
}