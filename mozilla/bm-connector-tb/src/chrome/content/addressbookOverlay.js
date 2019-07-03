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

/* Addressbook overlay */
Components.utils.import("resource://bm/bmUtils.jsm");
Components.utils.import("resource://bm/bmService.jsm");

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
    syncObserver : new BMSyncObserver(),
    abObserver : new BMAbObserver(),
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject.getLogger("gBMAbOverlay: "),
    doSync: function() {
        if (!Services.io.offline) {
            if (bmUtils.getSettings({}, {}, {}, true, window)) {
            bmService.doSync(false);
            } else {
            gBMAbOverlay._logger.info("Cancel sync: not enouth settings");
            }
        } else {
            gBMAbOverlay._logger.info("Cancel sync: offline");
        }
    }
}

window.addEventListener("load", function(e){
  installButton("ab-bar2", "bm-button-sync", "search-container");
  gBMAbOverlay.syncObserver.register(window);
  gBMAbOverlay.abObserver.register();
  // overload abDirTreeObserver.onDrop to support big d&d
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
  disableBtnWhenOffline('bm-button-sync', Services.io.offline);
}, false);

window.addEventListener("unload", function(e){
  gBMAbOverlay.syncObserver.unregister();
  gBMAbOverlay.abObserver.unregister();
}, false);

function installButton(toolbarId, id, beforeId) {  
  if (!document.getElementById(id)) {  
    let toolbar = document.getElementById(toolbarId);  

    let before = null;  
    if (beforeId) {  
        let elem = document.getElementById(beforeId);  
        if (elem && elem.parentNode == toolbar)  
            before = elem;  
    }  

    toolbar.insertItem(id, before);  
    toolbar.setAttribute("currentset", toolbar.currentSet);  
    if (document.persist) {
        document.persist(toolbar.id, "currentset");
    }
  }
}  

window.addEventListener("offline", function(e) {
  disableBtnWhenOffline('bm-button-sync', true);
}, false);

window.addEventListener("online", function(e) {
  disableBtnWhenOffline('bm-button-sync', false);
}, false);

function disableBtnWhenOffline(btnId, disable) {
  let btn = document.getElementById(btnId);
  if (btn) {
    btn.setAttribute('disabled', disable? 'true' : 'false');
  }
}

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

abDirTreeItem.prototype.getProperties = function atv_getProps(aProps) {
    let id = bmUtils.getCharPref(this._directory.URI + ".bm-id", null);
    if (aProps && aProps.AppendElement != undefined) {
	let atomSvc = Components.classes["@mozilla.org/atom-service;1"]
			.getService(Components.interfaces.nsIAtomService);
    if (this._directory.isMailList)
        aProps.AppendElement(atomSvc.getAtom("IsMailList-true"));
    if (this._directory.isRemote)
        aProps.AppendElement(atomSvc.getAtom("IsRemote-true"));
    if (this._directory.isSecure)
        aProps.AppendElement(atomSvc.getAtom("IsSecure-true"));
	if (id != null && !this._directory.isMailList
		&& this._directory.URI.indexOf("bmdirectory://") != 0) {
            aProps.AppendElement(atomSvc.getAtom("IsBm-true"));
            if (bmUtils.getBoolPref(this._directory.URI + ".bm-shared", false))
                aProps.AppendElement(atomSvc.getAtom("IsBmShared-true"));
	}
	return null;
    } else {
	//TB 24
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
			error = bmUtils.getCharPref(card.mailListURI + ".bm-error-message", null);
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