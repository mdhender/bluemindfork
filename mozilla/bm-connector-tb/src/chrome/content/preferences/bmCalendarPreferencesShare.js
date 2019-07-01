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

var gBMCalPrefsShare = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                    .getLogger("gBMCalPrefsShare: "),
    _onError: function(aError) {
	this._logger.error(aError);
	this._finalCallback();
    },
    _acls: new HashMap(),
    _accessLabel: "",
    _readLabel: "",
    _writeLabel: "",
    _adminLabel: "",
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _domainUid: null,
    _selected : null,
    init: function() {
      this._accessLabel = bmUtils.getLocalizedString("calprefs.tabshare.access");
      this._readLabel = bmUtils.getLocalizedString("calprefs.tabshare.read");
      this._writeLabel = bmUtils.getLocalizedString("calprefs.tabshare.write");
      this._adminLabel = bmUtils.getLocalizedString("calprefs.tabshare.admin");

      let self = this;
      let textbox = document.getElementById("searchShare");

      textbox.popup.onPopupClick = function(evt) {
          var controller = this.view.QueryInterface(Components.interfaces.nsIAutoCompleteController);
          if (this.selectedIndex != -1) {
              let item = {
                "subject" : controller.getFinalCompleteValueAt(this.selectedIndex),
                "verb": "Invitation"
              };
              this.input.textValue = "";
              this.selectedIndex = -1;
              this.input.popupOpen = false;
              self._addItem(item);
          } else {
              controller.handleEnter(true);
          }
      };

      if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
        this._logger.error("cannot login");
      }
    },
    calendarSelect: function(calendarUid) {
      this.selected_ = calendarUid;
      let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
      let self = this;
      result.then(function(logged) {
        self._authKey = logged.authKey;
        self._user = logged.authUser;
        self._domainUid = logged.authUser.domainUid;
        let client = new ContainerManagementClient(self._srv.value, self._authKey, calendarUid);
        return client.getAccessControlList();
      }).then(function(list) {
        document.getElementById('searchShare').disabled = false;
        for (let item of list) {
          if (item.subject == self._domainUid) {
            document.getElementById('publicShare').checked = true;
            let selectPublicAcl = document.getElementById('publicAcl');
            selectPublicAcl.disabled = false;
            selectPublicAcl.value = item.verb;
          } else {
            self._addItem(item);
          }
        }
      }).catch(function(err) {
        self._logger.error(err);
      });
    },

    getSelected: function() {
      return this.selected_;
    },

    getValues: function() {
      let ret = new Array();

      if (document.getElementById('publicShare').checked) {
        ret.push({"subject" : this._domainUid, "verb" : document.getElementById("publicAcl").value});
      }

      for (let subject of this._acls.keys()) {
        ret.push({"subject" : subject, "verb" : document.getElementById("acl-"+subject).value});
      }

      return ret;
    },

    _addItem: function(item) {
      if (this._acls.containsKey(item.subject)) return;
      let client = new DirectoryClient(this._srv.value, this._authKey, this._domainUid);
      let result = client.findByEntryUid(item.subject);
      let self = this;
      result.then(function(entry) {
        let row = self._createRow(entry, item.verb);
        let rows = document.getElementById("shareRows");
        rows.insertBefore(row, rows.firstChild);
        self._acls.put(item.subject, row);
      }).catch(function(err) {
        self._logger.error(err);
      });
    },

    _createRow: function(e, verb) {
	let id = e.entryUid;
	let row = document.createElement("row");
	row.setAttribute("id", "row-" + id);

	let name = document.createElement("label");
	name.setAttribute("value", e.displayName);
	
	let acl = document.createElement("menulist");
	acl.setAttribute("id", "acl-" + id);

	let popup = document.createElement("menupopup");
	let item = document.createElement("menuitem");
	item.setAttribute("value", "Invitation");
	item.setAttribute("label", this._accessLabel);
	popup.appendChild(item);
	item = document.createElement("menuitem");
	item.setAttribute("value", "Read");
	item.setAttribute("label", this._readLabel);
	popup.appendChild(item);
	item = document.createElement("menuitem");
	item.setAttribute("value", "Write");
	item.setAttribute("label", this._writeLabel);
	popup.appendChild(item);
	item = document.createElement("menuitem");
	item.setAttribute("value", "All");
	item.setAttribute("label", this._adminLabel);
	popup.appendChild(item);
	acl.appendChild(popup);

	acl.setAttribute("value", verb);
	acl.setAttribute("sizetopopup", "always");
	
	let spacer = document.createElement("spacer");
	let del = document.createElement("button");
	del.setAttribute("class", "bm-delete-button");
	del.setAttribute("onclick", "gBMCalPrefsShare.onDelete('" + id + "');");

	row.appendChild(name);
	row.appendChild(acl);
	row.appendChild(spacer);
	row.appendChild(del);

	return row;
    },
    onDelete: function(aId) {
	this._logger.debug("onDelete(" + aId + ")");
	let rows = document.getElementById("shareRows");
	let row = document.getElementById("row-" + aId);
	rows.removeChild(row);
	this._acls.remove(aId);
    }
};
