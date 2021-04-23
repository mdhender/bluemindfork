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

var { bmUtils, HashMap, BMXPComObject, BmPrefListener, BMError } = ChromeUtils.import("chrome://bm/content/modules/bmUtils.jsm");

RegisterLoadListener(function(aList, aDoc) {
    if (!aList.UID) return;
    let error = bmUtils.getCharPref(aList.UID + ".bm-error-code", "");
    if (error) {
        let errorLabel = aDoc.getElementById("bmError");
        errorLabel.setAttribute("value", error);
        let errorHbox = aDoc.getElementById("bmInError");
        errorHbox.setAttribute("hidden", false);
    }
});

RegisterSaveListener(function(aList, aDoc) {
    if (aList.UID) {
        bmUtils.setCharPref(aList.UID + ".bm-error-code", "");
        bmUtils.setCharPref(aList.UID + ".bm-error-message", "");
    }
    let errorHbox = aDoc.getElementById("bmInError");
    errorHbox.setAttribute("hidden", true);
});

