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

/*Extend create Addressbook dialog*/

Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");

var gBMAbNameDlgLogger = Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMabNameDlg: ");

function abNameOKButton() {
    let isLocal = document.getElementById("local").getAttribute("checked") == "false";
    var newName = gNameInput.value.trim();
    if (!isLocal) {
        newName = bmUtils.getBmAddressbookName(newName, false);
    }
    if (gDirectory) {
        gDirectory.dirName = newName;
        gBMAbNameDlgLogger.debug("directory [" + newName + "] updated");
        if (!isLocal) {
            if (!bmUtils.getBoolPref(gDirectory.URI + ".bm-added", false)) {
                bmUtils.setBoolPref(gDirectory.URI + ".bm-updated", true);
                gBMAbNameDlgLogger.debug("=> directory marked as updated");
            }
        }
    } else {
        let pref = Components.classes["@mozilla.org/abmanager;1"]
            .getService(Components.interfaces.nsIAbManager)
            .newAddressBook(newName, "", 2);
        gBMAbNameDlgLogger.debug("directory [" + newName + "] added");
        if (!isLocal) {
            //calculate uri
            let uri = "moz-abmdbdirectory://" + bmUtils.getCharPref(pref + ".filename");
            //set temp bm-id
            let tempId = "temp-" + bmUtils.randomUUID();
            bmUtils.setCharPref(uri + ".bm-id", tempId);
            bmUtils.setBoolPref(uri + ".bm-added", true);
            bmUtils.setBoolPref(uri + ".bm-writable", true);
            bmUtils.setBoolPref(uri + ".bm-manageable", true);
            bmUtils.setBoolPref(uri + ".bm-isDefault", false);
            gBMAbNameDlgLogger.debug("=> directory marked as added");
            bmService.monitor.listenDirectory(uri, tempId);
        }
    }
    
    return true;
}

function BMCheckIsLocal() {
    if (gDirectory) {
        let id = bmUtils.getCharPref(gDirectory.URI + ".bm-id", null);
        let checkbox = document.getElementById("local");
        if (id != null) {
            checkbox.setAttribute("checked", "true");
            let isDefault = bmUtils.getBoolPref(gDirectory.URI + ".bm-default", false);
            let isManageable = bmUtils.getBoolPref(gDirectory.URI + ".bm-manageable", false);
            if (isDefault || !isManageable) {
                gNameInput.readOnly = true;
                document.documentElement.buttons = "accept";
                document.documentElement.removeAttribute("ondialogaccept");
            }
            let ownedBy = bmUtils.getLocalizedString("abname.ownedby") + ": "
                            +  bmUtils.getCharPref(gDirectory.URI + ".bm-ownerDisplayName", "");
            document.getElementById("sharedBy").setAttribute("value", ownedBy);
        } else {
            checkbox.setAttribute("checked", "false");
        }
        checkbox.setAttribute("disabled", "true");
    }
}

setTimeout(BMCheckIsLocal, 100);