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
Components.utils.import("resource://bm/bmService.jsm");
Components.utils.import("resource://bm/core2/BMAuthService.jsm");

var gBMCalPrefsCalendars = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                    .getLogger("gBMCalPrefsCalendars: "),
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _calendars: new HashMap(),
    _added: new HashMap(),
    _removed: new HashMap(),
    _changed: false,
    init: function() {
        if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
            this._logger.error("cannot login");
        } else {
            this._listCalendars();
        }
    },
    hasChanged: function() {
        return this._changed;
    },
    getCalendars: function() {
        return this._calendars;
    },
    getAdded: function() {
        return this._added;
    },
    getRemoved: function() {
        return this._removed;
    },
    getUpdated: function() {
        let changed = [];
        let rows = document.getElementById("calendarsRows").children;
        for (let i = 0; i < rows.length; i++) {
            let row = rows[i];
            let id = row.getAttribute("id").replace("calendarsRow-", "");
            let ori = this._calendars.get(id);
            if (!ori) continue;
            let newName = document.getElementById("calendarsRow-textbox-" + id);
            if (!newName) continue;
            newName = newName.value;
            if (newName != ori.name) {
                ori.name = newName;
                changed.push(ori);
            }
        }
        return changed;
    },
    _listCalendars: function() {
        let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
        let self = this;
        result.then(function(logged) {
            self._authKey = logged.authKey;
            let user = logged.authUser;
            let client = new ContainersClient(self._srv.value, self._authKey);
            return client.all({type:"calendar", owner: user.uid});
        }).then(function(list) {
            for (let item of list) {
                self._addItem(item);
            }
        }).catch(function(err) {
            self._logger.error(err);
        });
    },
    addNewCalendar: function() {
        let name = document.getElementById("newCalendar").value;
        if (name) {
            //TODO check if a cal with same name already exist
            document.getElementById("newCalendar").value = "";
            let item = {};
            item.uid = "calendar:" + bmUtils.randomUUID();
            item.name = name;
            this._addItem(item);
            this._added.put(item.uid);
            this._changed = true;
        }
    },
    _addItem: function(item) {
        let row = this._createRow(item);
        let rows = document.getElementById("calendarsRows");
        rows.appendChild(row);
        this._calendars.put(item.uid, item);
    },
    _createRow: function(item) {
        let id = item.uid
        let row = document.createElement("row");
        row.setAttribute("id", "calendarsRow-" + id);
    
        let name;
        if (item.defaultContainer && item.writable) {
            name = document.createElement("label");
        } else {
            name = document.createElement("textbox");
            name.setAttribute("id", "calendarsRow-textbox-" + id);
            name.setAttribute("min", "1");
            name.setAttribute("style", "max-width: 25em;");
        }
        name.setAttribute("value", item.name);
        
        let spacer = document.createElement("spacer");
        spacer.setAttribute("flex", "1");
        
        let del = document.createElement("button");
        del.setAttribute("class", "bm-delete-button");
        if (item.defaultContainer && item.writable) {
            del.setAttribute("disabled", "true");
        } else {
            del.setAttribute("onclick", "gBMCalPrefsCalendars.onDelete('" + id + "');");
        }
    
        row.appendChild(name);
        row.appendChild(spacer);
        row.appendChild(del);
    
        return row;
    },
    onDelete: function(id) {
        this._logger.debug("onDelete(" + id + ")");
        let rows = document.getElementById("calendarsRows");
        let row = document.getElementById("calendarsRow-" + id);
        rows.removeChild(row);
        this._added.remove(id);
        this._removed.put(id, null);
        this._changed = true;
    }
};
