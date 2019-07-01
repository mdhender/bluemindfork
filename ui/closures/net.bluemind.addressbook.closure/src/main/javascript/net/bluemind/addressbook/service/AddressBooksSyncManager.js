/*
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

goog.provide("net.bluemind.addressbook.service.AddressBooksSyncManager");

goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.addressbook.api.AddressBooksClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.addressbook.sync.UnitaryAddressBookSync");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.addressbook.service.AddressBooksSyncManager = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.containersSyncByUid = new goog.structs.Map();
  this.handler = new goog.events.EventHandler(this);
  this.handler.listen(this.ctx.service('addressbooks'),
      net.bluemind.container.service.ContainersService.EventType.CHANGE, this.refreshBooks);

  this.handler.listen(this.ctx.service('containersObserver'),
      net.bluemind.container.service.ContainersObserver.EventType.CHANGE, function(e) {
        if (e.containerType == 'addressbook') {
          var s = this.containersSyncByUid.get(e.container);
          if (s) {
            s.needSync();
          } else {
            console.log("warn, no syncservice for addressbook " + e.container);
          }
        }
      });

};
goog.inherits(net.bluemind.addressbook.service.AddressBooksSyncManager, goog.events.EventTarget);

net.bluemind.addressbook.service.AddressBooksSyncManager.prototype.refreshBooks = function() {
  if (!this.ctx.service('addressbooks').isLocal()) return;
  this.ctx.service('addressbooks').list().then(function(addressbooks) {
    goog.array.forEach(addressbooks, function(ab) {
      if (!this.containersSyncByUid.containsKey(ab['uid']) && ab['offlineSync']) {
        var sync = new net.bluemind.addressbook.sync.UnitaryAddressBookSync(this.ctx, ab['uid']);
        net.bluemind.sync.SyncEngine.getInstance().registerService(sync);
        this.containersSyncByUid.set(ab['uid'], sync);
        sync.needSync();
      }
    }, this);

    this.containersSyncByUid.forEach(function(syncKey, uid) {
      var addressbook = goog.array.find(addressbooks, function(addressbook) {
        return addressbook['uid'] == uid;
      });

      if (!addressbook || !addressbook['offlineSync']) {
        this.containersSyncByUid.remove(uid);
        net.bluemind.sync.SyncEngine.getInstance().unregisterService(syncKey);
      }
    }, this);
  }, null, this);
}