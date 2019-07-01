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
goog.provide("net.bluemind.contact.filters.AddressBooksFilter");

goog.require("goog.array");
goog.require("net.bluemind.mvp.Filter");

/**
 * 
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.contact.filters.AddressBooksFilter = function() {
  net.bluemind.mvp.Filter.call(this);
};
goog.inherits(net.bluemind.contact.filters.AddressBooksFilter, net.bluemind.mvp.Filter);

net.bluemind.contact.filters.AddressBooksFilter.prototype.priority = 49;

/** @override */
net.bluemind.contact.filters.AddressBooksFilter.prototype.filter = function(ctx) {
  return ctx.service('addressbooks').list().then(function(addressbooks) {
    ctx.session.set('addressbooks', addressbooks);
    var def = goog.array.find(addressbooks, function(adb) {
      return (adb['defaultContainer'] == true && adb['owner'] == ctx.user['uid']);
    });
    
    if( def != null) {
      ctx.session.set('addressbook.default', def['uid']);
    } // else not default container found
  }, null, this).thenCatch(function(e) {
    ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(e), e);
  }, this);
};
