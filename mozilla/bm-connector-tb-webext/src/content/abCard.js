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

function ShowProps() {
    console.log("ShowProps");
    let disp = document.getElementById("bmProps");
    let props = gEditCard.card.properties;
    console.log(props);
    let texts = "";
    while (props.hasMoreElements()) {
        let prop = props.getNext().QueryInterface(Components.interfaces.nsIProperty);
        texts += prop.name + "      : " + prop.value + "\r\n";
    }
    console.log("set value:" + texts);
    disp.value = texts;
}

RegisterLoadListener(function(aCard, aDoc) {
    let error = aCard.getProperty("bm-error-message", null);
    if (error) {
        let errorLabel = aDoc.getElementById("bmError");
        errorLabel.setAttribute("value", error);
        let errorHbox = aDoc.getElementById("bmInError");
        errorHbox.setAttribute("hidden", false);
    }
    bmUtils.session.editedCard = gEditCard.card;
});

RegisterSaveListener(function(aCard, aDoc) {
    aCard.setProperty("bm-error-code", null);
    aCard.setProperty("bm-error-message", null);
    let errorHbox = aDoc.getElementById("bmInError");
    errorHbox.setAttribute("hidden", true);
    bmUtils.session.editedCard = null;
    let observerService = Components.classes["@mozilla.org/observer-service;1"]
                        .getService(Components.interfaces.nsIObserverService);
    observerService.notifyObservers(null, "bm-ab-observe", "refresh");
});
