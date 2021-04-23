/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

function BMAbObserver() {}

BMAbObserver.prototype = {
    observe: function(subject, topic, data) {
        try {
            let msg;
            if (subject != null) {
                msg = subject.wrappedJSObject;
            }
            switch(data) {
                case "readonly":
                    bmUtils.promptService.alert(window, bmUtils.getLocalizedString("dialogs.title"),
                                                bmUtils.getLocalizedString("ab.readonly." + msg.type));
                    break;
                case "reload":
                    SelectFirstAddressBook();
                    break;
                case "refresh":
                    ChangeDirectoryByURI(getSelectedDirectoryURI());
                    break;
            }
        } catch (e) {
            Components.utils.reportError(e);
        }
    },
    register: function() {
        let obs = Components.classes["@mozilla.org/observer-service;1"]
                            .getService(Components.interfaces.nsIObserverService);
        obs.addObserver(this, "bm-ab-observe", false);
    },
    unregister: function() {
        let obs = Components.classes["@mozilla.org/observer-service;1"]
                            .getService(Components.interfaces.nsIObserverService);
        obs.removeObserver(this, "bm-ab-observe");
    }
}

var gBMAbOverlay = {
    _initDone: false,
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMAbOverlay: "),
    init: function() {
		if (!this._initDone) {
            console.log("INIT gBMAbOverlay");
            this._initDone = true;
            this._abObserver = new BMAbObserver();
            this._abObserver.register();
            this._onDropOrginal = abDirTreeObserver.onDrop;
            bmUtils.overrideBM(abDirTreeObserver, "onDrop", function(original) {
                return function(index, orientation, dataTransfer) {
                    gBMAbOverlay._logger.debug("myOnDrop");
                    
                    bmService.lockSync = true;
                    bmService.listAddLocalMemberAckEnabled = false;
                    let maxScriptRunTime = bmUtils.getIntPref("dom.max_script_run_time", 10);
                    let maxChromeScriptRunTime = bmUtils.getIntPref("dom.max_chrome_script_run_time", 20);
                    bmUtils.setIntPref("dom.max_script_run_time", 0);
                    bmUtils.setIntPref("dom.max_chrome_script_run_time", 0);
                    
                    try {
                        original.apply(this, arguments);
                    } catch(e) {
                        gBMAbOverlay._logger.error(e);
                    }
            
                    bmService.lockSync = false;
                    bmService.listAddLocalMemberAckEnabled = true;
                    bmUtils.setIntPref("dom.max_script_run_time", maxScriptRunTime);
                    bmUtils.setIntPref("dom.max_chrome_script_run_time", maxChromeScriptRunTime);
                }
            });
		}
    },
    onremove: function() {
        this._logger.info("Overlay removing");
        abDirTreeObserver.onDrop = this._onDropOrginal;
        abDirTreeItem.prototype.getProperties = abDirTreeItemGetPropertiesOriginal;
        this._abObserver.unregister();
    }
}

abDirTreeItemGetPropertiesOriginal = abDirTreeItem.prototype.getProperties;
abDirTreeItem.prototype.getProperties = function atv_getProps(aProps) {
    let id = bmUtils.getCharPref(this._directory.URI + ".bm-id", null);
	let properties = [];
    if (this._directory.isMailList)
        properties.push("IsMailList-true");
    if (this._directory.isRemote)
        properties.push("IsRemote-true");
    if (this._directory.isSecure)
        properties.push("IsSecure-true");
    if (id != null && !this._directory.isMailList
        && this._directory.URI.indexOf("bmdirectory://") != 0) {
        properties.push("IsBm-true");
        if (bmUtils.getBoolPref(this._directory.URI + ".bm-shared", false))
            properties.push("IsBmShared-true");
    }
	return properties.join(" ");
}

//override
function OnClickedCard(card)
{
	let errorLabel = document.getElementById("bmError");
	errorLabel.setAttribute("hidden", true);
	if (card) {
		DisplayCardViewPane(card);
		let error = null;
		if (card.isMailList) {
			error = bmUtils.getCharPref(card.UID + ".bm-error-message", null);
		} else {
			error = card.getProperty("bm-error-message", null);
		}
		if (error) {
			errorLabel.setAttribute("value", error);
			errorLabel.setAttribute("hidden", false);
		}
	} else {
		ClearCardViewPane();
	}
}
