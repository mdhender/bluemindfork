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

/* Utils module */
var { AddonManager } = ChromeUtils.import("resource://gre/modules/AddonManager.jsm");
var { Downloads } = ChromeUtils.import("resource://gre/modules/Downloads.jsm");

this.EXPORTED_SYMBOLS = ["bmPhotos"];

let bmPhotos = {
    _photoManager: null,
    get photoManager() {
        if (!this._photoManager) {
            this._photoManager = {};
            let loader = Components.classes["@mozilla.org/moz/jssubscript-loader;1"]
                        .getService(Components.interfaces.mozIJSSubScriptLoader);
            loader.loadSubScript("chrome://messenger/content/addressbook/abCommon.js", this._photoManager);
            try {
                loader.loadSubScript("chrome://messenger/content/addressbook/abCardOverlay.js", this._photoManager);
            } catch(e) {
                //TB >= 60 abCard.js with photos functions cannot be loaded without window
                this._photoManager.removePhoto = function(aName) {
                    if (!aName) return false;
                    // Get the directory with all the photos
                    let file = this.getPhotosDir();
                    // Get the photo (throws an exception for invalid names)
                    try {
                      file.append(aName);
                      file.remove(false);
                      return true;
                    } catch (e) {
                        //
                    }
                    return false;
                };
            }
            this._photoManager.storePhoto = function(aUri) {
                //storePhoto overriden to use Downloads.jsm instead of standard Channel with witch photos are truncated sometimes 
                if (!aUri) return false;
                let file = this.getPhotosDir();
                file = this.makePhotoFile(file, "png");
                Downloads.fetch(aUri, file);
                return file;
            };
        }
        return this._photoManager;
    },
    removePhoto: function(aCard) {
        this.photoManager.removePhoto(aCard.getProperty("PhotoName", null));
        aCard.setProperty("PhotoType", "generic");
        aCard.setProperty("PhotoURI", "");
        aCard.setProperty("PhotoName", "");
    },
    setPhoto: function(aCard, aUri) {
        let file = this.photoManager.storePhoto(aUri);
        if (file) {
            this.photoManager.removePhoto(aCard.getProperty("PhotoName", null));
            aCard.setProperty("PhotoName", file.leafName);
            aCard.setProperty("PhotoType", "file");
            let photoUri = Components.classes["@mozilla.org/network/io-service;1"]
                                .getService(Components.interfaces.nsIIOService)
                                .newFileURI(file)
                                .spec;
            aCard.setProperty("PhotoURI", photoUri);
            aCard.setProperty("bm-PhotoURI", photoUri);
        }
    },
    getPhoto: function(aCard) {
        let photoName = aCard.getProperty("PhotoName", null);
        if (!photoName) return null;
        let file = this.photoManager.getPhotosDir();
        file.append(photoName);
    
        let inputStream = Components.classes["@mozilla.org/network/file-input-stream;1"]
                                    .createInstance(Components.interfaces.nsIFileInputStream);
        inputStream.init(file, 0o01, 0o0600, 0);
        let stream = Components.classes["@mozilla.org/binaryinputstream;1"]
                               .createInstance(Components.interfaces.nsIBinaryInputStream);
        stream.setInputStream(inputStream);
        return btoa(stream.readBytes(stream.available()));
    },
};
