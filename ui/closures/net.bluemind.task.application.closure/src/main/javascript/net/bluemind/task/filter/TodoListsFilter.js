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
goog.provide("net.bluemind.task.filter.TodoListsFilter");

goog.require("goog.array");
goog.require("net.bluemind.mvp.Filter");
goog.require("net.bluemind.task.Messages");
/**
 * 
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.task.filter.TodoListsFilter = function() {
  net.bluemind.mvp.Filter.call(this);
};
goog.inherits(net.bluemind.task.filter.TodoListsFilter, net.bluemind.mvp.Filter);

net.bluemind.task.filter.TodoListsFilter.prototype.priority = 49;

/** @override */
net.bluemind.task.filter.TodoListsFilter.prototype.filter = function(ctx) {
  return ctx.service('todolists').list().then(function(todolists) {
    ctx.session.set('todolists', todolists);
    var def = goog.array.find(todolists, function(tdl) {
      return (tdl['defaultContainer'] == true && tdl['owner'] == ctx.user['uid']);
    });

    if (def != null) {
      ctx.session.set('todolist.default', def['uid']);
    } // else not default container found
  }, null, this).thenCatch(function(e) {
    ctx.notifyError(net.bluemind.task.Messages.errorLoadingLists(e), e);
  }, this)
};