/* eslint-disable object-shorthand */

// Get various parts of the WebExtension framework that we need.
var { ExtensionCommon } = ChromeUtils.import("resource://gre/modules/ExtensionCommon.jsm");
var { Services } = ChromeUtils.import("resource://gre/modules/Services.jsm");

var BmDirWrapperApi = class extends ExtensionCommon.ExtensionAPI {
  getAPI(context) {
    return {
      BmDirWrapperApi: {
        init: async function () {

          var { XPCOMUtils } = ChromeUtils.import("resource://gre/modules/XPCOMUtils.jsm");
          var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

          //Components.utils.import("resource://bm/bmService.jsm");

          let classID = Components.ID("{bbfaef66-97eb-4856-bfd9-2b7e334f1ebc}");
          let contractID = "@blue-mind.net/bmdirwrapper;1";

          function BmDirWrapper() {
            this.mDirectory = null;
            this.wrappedJSObject = this;
          }

          BmDirWrapper.prototype = {
            classDescription: "BmDirWrapper XPCOM Component",
            classID: classID,
            contractID: contractID,
            QueryInterface: ChromeUtils.generateQI([Components.interfaces.nsIAbDirectory]),
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
            init: function (aURI) {
              return this.mDirectory.init(aURI);
            },
            deleteDirectory: function (directory) {
              return this.mDirectory.deleteDirectory(directory);
            },
            hasCard: function (cards) {
              return this.mDirectory.hasCard(cards);
            },
            hasDirectory: function (dir) {
              return this.mDirectory.hasDirectory(dir);
            },
            addCard: function (card) {
              return this.mDirectory.addCard(card);
            },
            modifyCard: function (modifiedCard) {
              return this.mDirectory.modifyCard(modifiedCard);
            },
            deleteCards: function (aCards) {
              return this.mDirectory.deleteCards(aCards);
            },
            dropCard: function (card, needToCopyCard) {
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
            useForAutocomplete: function (aIdentityKey) {
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
            addMailList: function (list) {
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
            editMailListToDatabase: function (listCard) {
              return this.mDirectory.editMailListToDatabase(listCard);
            },
            copyMailList: function (srcList) {
              return this.mDirectory.copyMailList(srcList);
            },
            createNewDirectory: function (aDirName, aURI, aType, aPrefName) {
              return this.mDirectory.createNewDirectory(aDirName, aURI, aType, aPrefName);
            },
            createDirectoryByURI: function (displayName, aURI) {
              return this.mDirectory.createDirectoryByURI(displayName, aURI);
            },
            get dirPrefId() {
              return this.mDirectory.dirPrefId;
            },
            set dirPrefId(value) {
              return (this.mDirectory.dirPrefId = value);
            },
            getIntValue: function (aName, aDefaultValue) {
              return this.mDirectory.getIntValue(aName, aDefaultValue);
            },
            getBoolValue: function (aName, aDefaultValue) {
              return this.mDirectory.getBoolValue(aName, aDefaultValue);
            },
            getStringValue: function (aName, aDefaultValue) {
              return this.mDirectory.getStringValue(aName, aDefaultValue);
            },
            getLocalizedStringValue: function (aName, aDefaultValue) {
              return this.mDirectory.getLocalizedStringValue(aName, aDefaultValue);
            },
            setIntValue: function (aName, aValue) {
              return this.mDirectory.setIntValue(aName, aValue);
            },
            setBoolValue: function (aName, aValue) {
              return this.mDirectory.setBoolValue(aName, aValue);
            },
            setStringValue: function (aName, aValue) {
              return this.mDirectory.setStringValue(aName, aValue);
            },
            setLocalizedStringValue: function (aName, aValue) {
              return this.mDirectory.setLocalizedStringValue(aName, aValue);
            },
            mReadOnly: true,
            get readOnly() {
              return /*bmService.lockSync ||*/ this.mReadOnly;
            }
          };

          console.trace("Register component");

          let factory = XPCOMUtils.generateNSGetFactory([BmDirWrapper])(classID);
          // WARNING: this assumes that Thunderbird is already running, as
          // Components.manager.registerFactory will be unavailable for a few
          // milliseconds after startup.
          Components.manager.registerFactory(classID, "BmDirWrapper", contractID,
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
