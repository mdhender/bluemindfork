/* BEGIN LICENSE
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

goog.provide("net.bluemind.task.TaskApplication");

goog.require('net.bluemind.commons.ui.ApplicationViewHelper');
goog.require("net.bluemind.mvp.Application");
goog.require("net.bluemind.mvp.banner.BannerHandler");
goog.require("net.bluemind.mvp.logo.LogoHandler");
goog.require("net.bluemind.task.create.CreateHandler");
goog.require("net.bluemind.task.todolist.TodoListHandler");
goog.require("net.bluemind.task.todolists.TodoListsHandler");
goog.require("net.bluemind.task.filter.ContainerFilter");
goog.require("net.bluemind.task.filter.TodoListsFilter");
goog.require("net.bluemind.task.filter.VTodoFilter");
goog.require("net.bluemind.task.vtodo.consult.VTodoConsultHandler");
goog.require("net.bluemind.task.vtodo.edit.VTodoEditHandler");
goog.require("net.bluemind.todolist.api.TodoListsClient");
goog.require("net.bluemind.todolist.service.TodoListsService");
goog.require("net.bluemind.todolist.service.TodoListService");
goog.require("net.bluemind.folder.sync.FoldersSync");
goog.require("net.bluemind.todolist.sync.TodoListSync");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("net.bluemind.folder.service.FoldersService");
goog.require("net.bluemind.folder.service.FolderService");
goog.require("net.bluemind.folder.persistence.schema");
goog.require("net.bluemind.todolist.persistence.schema");
goog.require("net.bluemind.container.persistence.schema");
goog.require("net.bluemind.tag.service.TagService");
goog.require("net.bluemind.tag.sync.UnitaryTagSync");
goog.require("net.bluemind.todolist.service.TodolistsSyncManager");
/**
 * Calendar application
 * 
 * @constructor
 */
net.bluemind.task.TaskApplication = function() {
  var routes = [
      {
        path : '.*',
        handlers : [ net.bluemind.mvp.banner.BannerHandler, net.bluemind.task.create.CreateHandler,
            net.bluemind.task.todolists.TodoListsHandler, net.bluemind.task.todolist.TodoListHandler ]
      }, {
        path : '/vtodo/consult/',
        handlers : [ net.bluemind.task.vtodo.consult.VTodoConsultHandler ]
      }, {
        path : '/vtodo/edit/',
        handlers : [ net.bluemind.task.vtodo.edit.VTodoEditHandler ]
      }

  ];

  goog.base(this, 'task', '/task/', routes);
};
goog.inherits(net.bluemind.task.TaskApplication, net.bluemind.mvp.Application);

/**
 * Application bootstrap
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 */
net.bluemind.task.TaskApplication.prototype.bootstrap = function(ctx) {

  return goog.base(this, 'bootstrap', ctx).thenCatch(function(error) {
    window.alert('An error occured during application initialization: ' + error);
  }, this)
};


/** @override */
net.bluemind.task.TaskApplication.prototype.postBootstrap = function(ctx) {
  goog.base(this, 'postBootstrap', ctx);

  var sync = net.bluemind.sync.SyncEngine.getInstance();
  net.bluemind.tag.sync.UnitaryTagSync.registerAll(ctx, sync);
  ctx.service("todolists-sync-manager").refresh();

  net.bluemind.folder.sync.FoldersSync.register(ctx, sync);
  net.bluemind.tag.sync.UnitaryTagSync.registerAll(ctx, sync);

  sync.start(1);
  goog.log.info(this.logger,'Synchronization started');
  new net.bluemind.commons.ui.ApplicationViewHelper().afterBootstrap();


};

/** @override */
net.bluemind.task.TaskApplication.prototype.registerFilters = function(router) {
  goog.base(this, 'registerFilters', router);
  router.addFilter(new net.bluemind.task.filter.VTodoFilter());
  router.addFilter(new net.bluemind.task.filter.ContainerFilter());
  router.addFilter(new net.bluemind.task.filter.TodoListsFilter());
};

/**
 * Register clients in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 */
net.bluemind.task.TaskApplication.prototype.registerClients = function(ctx) {
  goog.base(this, 'registerClients', ctx);
  ctx.client('appContainerClient', net.bluemind.todolist.api.TodoListsClient);
};

/**
 * Register service in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 */
net.bluemind.task.TaskApplication.prototype.registerServices = function(ctx) {
  goog.base(this, 'registerServices', ctx);
  ctx.service("folders", net.bluemind.folder.service.FoldersService);
  ctx.service("todolists", net.bluemind.todolist.service.TodoListsService);
  ctx.service("todolist", net.bluemind.todolist.service.TodoListService);
  ctx.service("tags", net.bluemind.tag.service.TagService);
  ctx.service("todolists-sync-manager", net.bluemind.todolist.service.TodolistsSyncManager);
};

net.bluemind.task.TaskApplication.prototype.getDbSchemas = function(ctx) {
  var root = goog.base(this, 'getDbSchemas', ctx);
  return goog.array.concat(root, [ {
    name : 'tag',
    schema : net.bluemind.container.persistence.schema
  }, {
    name : 'folder',
    schema : net.bluemind.folder.persistence.schema
  }, {
    name : 'todolist',
    schema : net.bluemind.todolist.persistence.schema
  } ]);
};
