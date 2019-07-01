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

goog.provide("net.bluemind.task.filter.VTodoFilter");

goog.require("net.bluemind.mvp.Filter");

/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Filter}
 */
net.bluemind.task.filter.VTodoFilter = function() {
  net.bluemind.mvp.Filter.call(this);
};
goog.inherits(net.bluemind.task.filter.VTodoFilter, net.bluemind.mvp.Filter);

net.bluemind.task.filter.VTodoFilter.prototype.priority = 50;

net.bluemind.task.filter.VTodoFilter.prototype.filter = function(ctx) {
  var path = ctx.uri.getPath().toLowerCase();
  var check = false;
  if (goog.string.startsWith(path, '/vtodo')) {
    var container = ctx.params.get('container');
    var uid = ctx.params.get('uid');

    if (!container) {
      container = ctx.session.get('todolist.default');
    }

    var todolist = goog.array.find(ctx.session.get('todolists'), function(tdl) {
      return (tdl['uid'] == container);
    });

    if (!todolist) {
      ctx.helper('url').goTo('/');
    } else if (todolist['writable'] && !uid) {
      ctx.helper('url').redirect('/vtodo/edit/?container=' + container);
    } else if (!uid) {
      ctx.helper('url').redirect('/vtodo/edit/?container=' + ctx.session.get('todolist.default'));
    } else {
      return ctx.service('todolist').getItem(container, uid).then(function(vcard) {
        if (!vcard) {
          // FIXME: UID does not exist and container is not writable.
          // => Error ?
          // => replace container to default one ?
          // Anyway a message must be shown.
          ctx.helper('url').goTo('/');
          return;
        }
        if (todolist['writable']) {
          var url = '/vtodo/edit/?uid=' + uid + '&container=' + container;
        } else {
          var url = '/vtodo/consult/?uid=' + uid + '&container=' + container;
        }
        ctx.helper('url').redirect(url);
      });
    }
  }
};
