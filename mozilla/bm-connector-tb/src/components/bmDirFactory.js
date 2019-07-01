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

function BmDirFactory() {
    this.mDirectory = null;
    this.wrappedJSObject = this;
}

BmDirFactory.prototype = {
    classDescription: "BmDirFactory XPCOM Component",
    classID:          Components.ID("{e86a2bef-c9dd-4b33-85ae-14636d7bd077}"),
    contractID:       null,
    QueryInterface: ChromeUtils.generateQI ?
        ChromeUtils.generateQI([Components.interfaces.nsIAbDirFactory])
        : XPCOMUtils.generateQI([Components.interfaces.nsIAbDirFactory]),
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("bmDirFactory: "),
    /*nsIAbDirFactory*/
    getDirectories: function(aDirName, aURI, aPrefId) {
        this._logger.debug("getDirectories("+ aDirName + ", " + aURI + ", " + aPrefId + ")");
        let baseArray = Components.classes["@mozilla.org/array;1"]
                                  .createInstance(Components.interfaces.nsIMutableArray);
        let abManager = Components.classes["@mozilla.org/abmanager;1"]
                                  .getService(Components.interfaces.nsIAbManager);
        let directory = abManager.getDirectory(aURI);
        if (directory) {
            baseArray.appendElement(directory, false);
        } else {
             this._logger.debug("directory not found");
        }
        return baseArray.enumerate();
    },
    deleteDirectory: function(/*nsIAbDirectory*/ directory) {
        this._logger.debug("deleteDirectory");
    },
};

var NSGetFactory = XPCOMUtils.generateNSGetFactory([BmDirFactory]);
