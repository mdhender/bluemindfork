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

Components.utils.import("resource://gre/modules/Services.jsm");
if (ChromeUtils.generateQI) {
    //TB >= 65
    Components.utils.import("resource:///modules/MailServices.jsm");
} else {
    Components.utils.import("resource:///modules/mailServices.js");
}
Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");

function load_bm_manifest() {
    let bindir = Components.classes["@mozilla.org/file/directory_service;1"]
                        .getService(Components.interfaces.nsIProperties)
                        .get("CurProcD", Components.interfaces.nsIFile);
    bindir.append("..");
    bindir.append("..");
    bindir.append("..");
    bindir.append("..");
    bindir.append("mozilla");
    bindir.append("extensions");
    bindir.append("bm-connector-tb");
    bindir.append("src");
    bindir.append("chrome.manifest");
    Components.manager.autoRegister(bindir);
}

load_bm_manifest();

// Ensure the profile directory is set up
do_get_profile();

// Import the required setup scripts.
load("../../../../../../../../mailnews/test/resources/abSetup.js");

var gBaseUrl = "http://192.168.61.1:8090";
