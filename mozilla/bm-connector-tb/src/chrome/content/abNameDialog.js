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

function BMCheckIsLocal() {
    if (gDirectory) {
        let id = bmUtils.getCharPref(gDirectory.URI + ".bm-id", null);
        if (id != null) {
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
        }
    }
}

setTimeout(BMCheckIsLocal, 100);