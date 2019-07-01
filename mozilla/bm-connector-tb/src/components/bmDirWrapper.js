/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");

function BmDirWrapper() {
    this.mDirectory = null;
    this.wrappedJSObject = this;
}

BmDirWrapper.prototype = {
    classDescription: "BmDirWrapper XPCOM Component",
    classID:          Components.ID("{bbfaef66-97eb-4856-bfd9-2b7e334f1ebc}"),
    contractID:       "@blue-mind.net/bmdirwrapper;1",
    QueryInterface: ChromeUtils.generateQI ?
        ChromeUtils.generateQI([Components.interfaces.nsIAbDirectory])
        : XPCOMUtils.generateQI([Components.interfaces.nsIAbDirectory]),
    /*nsIAbDirectory*/
    get propertiesChromeURI() {
        return this.mDirectory.propertiesChromeURI;
    },
    get dirName() {
        return this.mDirectory.dirName;
    },
    set dirName(value) {
        return (this.mDirectory.dirName = value);
    },
    get dirType() {
        return this.mDirectory.dirType;
    },
    get fileName() {
        return this.mDirectory.fileName;
    },
    get URI() {
        return this.mDirectory.URI;
    },
    get position() {
        return this.mDirectory.position;  
    },
    get lastModifiedDate() {
        return this.mDirectory.lastModifiedDate;
    },
    get isMailList() {
        return this.mDirectory.isMailList;
    },
    get childNodes() {
        return this.mDirectory.childNodes;
    },
    get childCards() {
        return this.mDirectory.childCards;
    },
    get isQuery() {
        return this.mDirectory.isQuery;
    },
    init: function(aURI) {
        return this.mDirectory.init(aURI);
    },
    deleteDirectory: function(directory) {
        return this.mDirectory.deleteDirectory(directory);  
    },
    hasCard: function(cards) {
        return this.mDirectory.hasCard(cards);
    },
    hasDirectory: function(dir) {
        return this.mDirectory.hasDirectory(dir);
    },
    addCard: function(card) {
        return this.mDirectory.addCard(card);
    },
    modifyCard: function(modifiedCard) {
        return this.mDirectory.modifyCard(modifiedCard);
    },
    deleteCards: function(aCards) {
        return this.mDirectory.deleteCards(aCards);
    },
    dropCard: function(card, needToCopyCard) {
        var res = this.mDirectory.dropCard(card, needToCopyCard);
        if (this.isMailList) {
            let uri = this.URI;
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
                }
            }
        }
        return res;
    },
    useForAutocomplete: function(aIdentityKey) {
        return this.mDirectory.useForAutocomplete(aIdentityKey);
    },
    get supportsMailingLists() {
        return this.mDirectory.supportsMailingLists;
    },
    get addressLists() {
        return this.mDirectory.addressLists;
    },
    set addressLists(value) {
        return (this.mDirectory.addressLists = value);
    },
    addMailList: function(list) {
        return this.mDirectory.addMailList(list);
    },
    get listNickName() {
        return this.mDirectory.listNickName;
    },
    set listNickName(value) {
        return (this.mDirectory.listNickName = value);
    },
    get description() {
        return this.mDirectory.description;
    },
    set description(value) {
        return (this.mDirectory.description = value);
    },
    editMailListToDatabase: function(listCard) {
        return this.mDirectory.editMailListToDatabase(listCard);
    },
    copyMailList: function(srcList) {
        return this.mDirectory.copyMailList(srcList);
    },
    createNewDirectory: function(aDirName, aURI, aType, aPrefName) {
        return this.mDirectory.createNewDirectory(aDirName, aURI, aType, aPrefName);
    },
    createDirectoryByURI: function(displayName, aURI) {
        return this.mDirectory.createDirectoryByURI(displayName, aURI);
    },
    get dirPrefId() {
        return this.mDirectory.dirPrefId;
    },
    set dirPrefId(value) {
        return (this.mDirectory.dirPrefId = value);
    },
    getIntValue: function(aName, aDefaultValue) {
        return this.mDirectory.getIntValue(aName, aDefaultValue);
    },
    getBoolValue: function(aName, aDefaultValue) {
        return this.mDirectory.getBoolValue(aName, aDefaultValue);
    },
    getStringValue: function(aName, aDefaultValue) {
        return this.mDirectory.getStringValue(aName, aDefaultValue);
    },
    getLocalizedStringValue: function(aName, aDefaultValue) {
        return this.mDirectory.getLocalizedStringValue(aName, aDefaultValue);
    },
    setIntValue: function(aName, aValue) {
        return this.mDirectory.setIntValue(aName, aValue);
    },
    setBoolValue: function(aName, aValue) {
        return this.mDirectory.setBoolValue(aName, aValue);
    },
    setStringValue: function(aName, aValue) {
        return this.mDirectory.setStringValue(aName, aValue);
    },
    setLocalizedStringValue: function(aName, aValue) {
        return this.mDirectory.setLocalizedStringValue(aName, aValue);
    },
    mReadOnly: true,
    get readOnly() {
        return bmService.lockSync || this.mReadOnly;
    }
};

var NSGetFactory = XPCOMUtils.generateNSGetFactory([BmDirWrapper]);
