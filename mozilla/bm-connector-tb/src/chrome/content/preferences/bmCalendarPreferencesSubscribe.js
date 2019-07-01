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
Components.utils.import("resource://bm/core2/BMFolderHome.jsm");

var gBMCalPrefsSubscribe = {
    _logger: Components.classes["@blue-mind.net/logger;1"].getService().wrappedJSObject
                    .getLogger("gBMCalPrefsSubscribe: "),
    _login: {},
    _pwd: {},
    _srv: {},
    _authKey: null,
    _user: null,
    _subs: new HashMap(),
    _unsubs: new HashMap(),
    _changed: false,
    init: function() {
      this._accessLabel = bmUtils.getLocalizedString("calprefs.tabshare.access");
      this._readLabel = bmUtils.getLocalizedString("calprefs.tabshare.read");
      this._writeLabel = bmUtils.getLocalizedString("calprefs.tabshare.write");
      this._adminLabel = bmUtils.getLocalizedString("calprefs.tabshare.admin");

      let self = this;
      let textbox = document.getElementById("searchSubcription");

      textbox.popup.onPopupClick = function(evt) {
          var controller = this.view.QueryInterface(Components.interfaces.nsIAutoCompleteController);
          if (this.selectedIndex != -1) {
              let item = {
                "uid" : controller.getFinalCompleteValueAt(this.selectedIndex),
                "name": controller.getValueAt(this.selectedIndex)
              };
              this.input.textValue = "";
              this.selectedIndex = -1;
              this.input.popupOpen = false;
              self._addItem(item);
              self._changed = true;
          } else {
              controller.handleEnter(true);
          }
      };

      if (!bmUtils.getSettings(this._login, this._pwd, this._srv, true)) {
        this._logger.error("cannot login");
      }

      this._listSubscription();
    },
    hasChanged: function() {
      return this._changed;
    },
    getSubscription: function() {
      return this._subs;
    },
    getUnsubscription: function() {
      return this._unsubs;
    },
    _listSubscription: function() {
      let result = BMAuthService.login(this._srv.value, this._login.value, this._pwd.value);
      let self = this;
      let descriptors = [];
      result.then(function(logged) {
        self._authKey = logged.authKey;
        let user = logged.authUser;
        let client = new UserSubscriptionClient(self._srv.value, self._authKey, user.domainUid);
        return client.listSubscriptions(user.uid, "calendar");
      }).then(function(subs) {
        let containers = new ContainersClient(self._srv.value, self._authKey);
        //get container descriptors one by one
        return subs.reduce(function(sequence, sub) {
            return sequence.then(function() {
                return containers.get(sub.containerUid);
            }).then(function(descriptor) {
                descriptors.push(descriptor);
            }, function(rej) {
                self._logger.error(rej);
                //skip not found subscribed container
            });
        }, Promise.resolve());

      }).then(function() {
        for (let item of descriptors) {
            if (BMFolderHome.isReadable(item.verbs)) {
              self._addItem(item);
            }
        }
      }).catch(function(err) {
        self._logger.error(err);
      });
    },
    _addItem: function(item) {
      if (this._subs.containsKey(item.uid)) return;
      let row = this._createRow(item);
      let rows = document.getElementById("subsciptionRows");
      rows.insertBefore(row, rows.firstChild);
      this._subs.put(item.uid, {
        containerUid: item.uid,
        offlineSync: item.offlineSync
      });
      this._unsubs.remove(item.uid);
    },
    _createRow: function(item) {
      let id = item.uid
      let row = document.createElement("row");
      row.setAttribute("id", "subcriptionRow-" + id);

      let name = document.createElement("label");
      name.setAttribute("value", item.name);
      
      let spacer = document.createElement("spacer");
      let del = document.createElement("button");
      del.setAttribute("class", "bm-delete-button");
      del.setAttribute("onclick", "gBMCalPrefsSubscribe.onDelete('" + id + "');");

      row.appendChild(name);
      row.appendChild(spacer);
      row.appendChild(del);

      return row;
    },
    onDelete: function(id) {
      this._logger.debug("onDelete(" + id + ")");
      let rows = document.getElementById("subsciptionRows");
      let row = document.getElementById("subcriptionRow-" + id);
      rows.removeChild(row);
      this._subs.remove(id);
      this._unsubs.put(id, null);
      this._changed = true;
    }
};
