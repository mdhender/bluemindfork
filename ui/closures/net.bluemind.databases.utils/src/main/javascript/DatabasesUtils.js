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
 * @fileoverview
 * 
 * Application bootstrap.
 */

goog.provide("net.bluemind.databases.DatabasesUtils");
goog.require("net.bluemind.container.persistance.schema");
goog.require("net.bluemind.folder.persistance.schema");
goog.require("net.bluemind.addressbook.persistance.schema");
goog.require("net.bluemind.todolist.persistance.schema");
goog.require("net.bluemind.calendar.persistance.schema");
goog.require("net.bluemind.authentication.schema");
goog.require("net.bluemind.mvp.ApplicationContext");
goog.require("net.bluemind.persistance.DatabaseService");
goog.require('net.bluemind.mvp.Application');
goog.require('net.bluemind.mvp.ApplicationContext');
/**
 * @constructor
 */
net.bluemind.databases.DatabasesUtils = function() {

  var ctx = new net.bluemind.mvp.ApplicationContext();
  ctx.version = goog.global['applicationVersion'] || null;
  ctx.privacy = (new goog.net.Cookies(document).get('BMPRIVACY')) != 'false';
  ctx.databaseAvailable = true;
  this.ctx = ctx;

};

/**
 */
net.bluemind.databases.DatabasesUtils.prototype.reset = function() {
  var service = new net.bluemind.persistance.DatabaseService(this.ctx);

  return service.initialize().then(function() {
    return service.regsiterSchemas([ {
      name : 'context',
      schema : net.bluemind.authentication.schema,
      options : null
    }, {
      name : 'tag',
      schema : net.bluemind.container.persistance.schema
    }, {
      name : 'folder',
      schema : net.bluemind.folder.persistance.schema
    }, {
      name : 'contact',
      schema : net.bluemind.addressbook.persistance.schema
    }, {
      name : 'calendarview',
      schema : net.bluemind.container.persistance.schema
    }, {
      name : 'calendar',
      schema : net.bluemind.calendar.persistance.schema
    }, {
      name : 'todolist',
      schema : net.bluemind.todolist.persistance.schema
    }, {
      name : 'auth',
      schema : net.bluemind.authentication.schema
    } ]);
  }, null, this).then(function() {
    service.clearAll();
  });
};
