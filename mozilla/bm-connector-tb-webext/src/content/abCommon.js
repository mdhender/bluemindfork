/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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

var { MailServices } = ChromeUtils.import("resource:///modules/MailServices.jsm");
var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");
var { bmService } = ChromeUtils.import("chrome://bm/content/modules/bmService.jsm");


function wrappBmDirectory(aDirectory) {
    let wrapped = Components.classes["@blue-mind.net/bmdirwrapper;1"].createInstance().wrappedJSObject;
    wrapped.mDirectory = aDirectory;
    wrapped.mReadOnly = true;
    return wrapped;
}

//override
function GetDirectoryFromURI(uri) {
    console.log("GetDirectoryFromURI(" + uri + ")");
    if (uri.startsWith("moz-abdirectory://")) {
        return null;
    }
    let directory = MailServices.ab.getDirectory(uri);
    let bm = false;
    let readOnly =  false;
    if (bmUtils.isBmList(directory)) {
        bm = true;
        readOnly = bmUtils.isBmReadOnlyList(directory);
    } else if (bmUtils.isBmDirectory(directory)) {
        bm = true;
        readOnly = bmUtils.isBmReadOnlyAddressbook(directory);
    }
    if (bm && !readOnly) {
        try {
            if (gEditCard) {
                let local = gEditCard.card.getProperty("bm-local", null);
                readOnly = local == "true";
            }
        } catch(e) {
            //gEditCard undef : not in edit card dialog
        }
    }
    if (bm && (readOnly || bmService.lockSync)) {
        console.log("use wrapped BM read only dir");
        return wrappBmDirectory(directory);
    } else {
        return directory;
    }
}