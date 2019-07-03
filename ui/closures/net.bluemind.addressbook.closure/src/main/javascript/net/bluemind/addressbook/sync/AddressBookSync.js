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

goog.provide("net.bluemind.addressbook.sync.AddressBookSync");

goog.require("goog.log");
goog.require("net.bluemind.addressbook.sync.AddressBookSyncClient");
goog.require("net.bluemind.container.sync.ContainerSync");
goog.require("goog.async.Deferred");
/**
 * Synchronize addressbook data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.addressbook.sync.AddressBookSync = function(ctx) {
  goog.base(this, ctx);
  this.logger = goog.log.getLogger('net.bluemind.addressbook.sync.AddressBookSync');
  this.type = 'addressbook';
};
goog.inherits(net.bluemind.addressbook.sync.AddressBookSync, net.bluemind.container.sync.ContainerSync);

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.getClient = function(uid) {
  return new net.bluemind.addressbook.sync.AddressBookSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.getName = function() {
  return 'AddressBook';
};

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.getContainerService = function() {
  return this.ctx.service('addressbook').cs_;
};

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.getContainersService = function() {
  return this.ctx.service('addressbooks').css_;
};

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.containersToSyncList = function() {
  return goog.async.Deferred.fromPromise(this.ctx.service('addressbooks').list());
}

/** @override */
net.bluemind.addressbook.sync.AddressBookSync.prototype.adaptItem = function(item) {
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