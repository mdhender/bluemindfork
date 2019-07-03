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

Components.utils.import("resource://bm/bmUtils.jsm");

function GetDirectoryFromURI(uri) {
    let directory = MailServices.ab.getDirectory(uri);
    let wrap =  false;
    let readOnly = false;
    if (bmUtils.isBmList(directory)) {
        wrap = true;
        readOnly = bmUtils.isBmReadOnlyList(directory);
    } else if (bmUtils.isBmDirectory(directory)) {
        wrap = true;
        readOnly = bmUtils.isBmReadOnlyAddressbook(directory);
    }
    if (wrap) {
        let myDirectory = Components.classes["@blue-mind.net/bmdirwrapper;1"].createInstance().wrappedJSObject;
        myDirectory.mDirectory = directory;
        myDirectory.mReadOnly = readOnly;
        return myDirectory;
    } else {
        return directory;
    }
}

RegisterLoadListener(function(aList, aDoc) {
    if (!aList.URI) return;
    let error = bmUtils.getCharPref(aList.URI + ".bm-error-code", "");
    if (error) {
        let errorLabel = aDoc.getElementById("bmError");
        errorLabel.setAttribute("value", error);
        let errorHbox = aDoc.getElementById("bmInError");
        errorHbox.setAttribute("hidden", false);
    }
});

RegisterSaveListener(function(aList, aDoc) {
    if (aList.URI) {
        bmUtils.setCharPref(aList.URI + ".bm-error-code", "");
        bmUtils.setCharPref(aList.URI + ".bm-error-message", "");
    }
    let errorHbox = aDoc.getElementById("bmInError");
    errorHbox.setAttribute("hidden", true);
});

