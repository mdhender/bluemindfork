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

/**
 * @fileoverview Contact synchronization service.
 */

goog.provide("net.bluemind.addressbook.sync.UnitaryAddressBookSync");

goog.require("goog.log");
goog.require("net.bluemind.addressbook.sync.AddressBookSyncClient");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");
goog.require("goog.async.Deferred");
/**
 * Synchronize addressbook data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.addressbook.sync.UnitaryAddressBookSync = function(ctx, containerUid) {
  goog.base(this, ctx, containerUid);
  this.logger = goog.log.getLogger('net.bluemind.addressbook.sync.UnitaryAddressBookSync');
};
goog.inherits(net.bluemind.addressbook.sync.UnitaryAddressBookSync, net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.addressbook.sync.UnitaryAddressBookSync.prototype.getClient = function(uid) {
  return new net.bluemind.addressbook.sync.AddressBookSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.addressbook.sync.UnitaryAddressBookSync.prototype.getName = function() {
  return 'AddressBook ('+this.containerUid+')';
};

/** @override */
net.bluemind.addressbook.sync.UnitaryAddressBookSync.prototype.getContainerService = function() {
  return this.ctx.service('addressbook').cs_;
};

/** @override */
net.bluemind.addressbook.sync.UnitaryAddressBookSync.prototype.getContainersService = function() {
  return this.ctx.service('addressbooks').css_;
};

/** @override */
net.bluemind.addressbook.sync.UnitaryAddressBookSync.prototype.adaptItem = function(item) {
  var i = item;
  i['name'] = i['displayName'];
  if (!i['name']){
	  //prevent npe
	  i['name'] = "";
  }
  i['order'] = bluemind.string.normalize(i['name'])
  i['tags'] = goog.array.map(i['value']['explanatory']['categories'], function(t) {
    return t['itemUid'];
});
  
  
  return i;
};