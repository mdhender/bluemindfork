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

goog.provide("net.bluemind.contact.ContactApplication");

goog.require('net.bluemind.commons.ui.ApplicationViewHelper');
goog.require('net.bluemind.addressbook.service.AddressBookService');
goog.require('net.bluemind.addressbook.service.AddressBooksService');
goog.require('net.bluemind.addressbook.sync.AddressBookSync');
goog.require("net.bluemind.contact.addressbooks.AddressBooksHandler");
goog.require("net.bluemind.contact.create.CreateHandler");
goog.require("net.bluemind.contact.filters.VCardFilter");
goog.require("net.bluemind.contact.filters.AddressBooksFilter");
goog.require("net.bluemind.contact.group.consult.GroupConsultHandler");
goog.require("net.bluemind.contact.group.edit.GroupEditHandler");
goog.require("net.bluemind.contact.individual.consult.IndividualConsultHandler");
goog.require("net.bluemind.contact.individual.edit.IndividualEditHandler");
goog.require("net.bluemind.contact.search.SearchHandler");
goog.require("net.bluemind.contact.vcards.VCardsHandler");
goog.require("net.bluemind.mvp.Application");
goog.require("net.bluemind.mvp.banner.BannerHandler");
goog.require("net.bluemind.mvp.logo.LogoHandler");
goog.require("net.bluemind.folder.sync.FoldersSync");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.folder.service.FoldersService");
goog.require("net.bluemind.folder.service.FolderService");
goog.require("net.bluemind.folder.persistance.schema");
goog.require("net.bluemind.tag.service.TagService");
goog.require("net.bluemind.tag.sync.TagSync");
goog.require("net.bluemind.addressbook.persistance.schema");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.addressbook.service.AddressBooksSyncManager");
goog.require("net.bluemind.tag.sync.UnitaryTagSync");
goog.require("net.bluemind.container.persistance.schema");

/**
 * @constructor
 * @param {String} application
 * @param {String} base
 * @param {*} routes
 * @extends {net.bluemind.mvp.Application}
 */
net.bluemind.contact.ContactApplication = function() {
  var routes = [
      {
        path : '.*',
        handlers : [ net.bluemind.mvp.banner.BannerHandler, net.bluemind.contact.create.CreateHandler,
            net.bluemind.contact.search.SearchHandler, net.bluemind.contact.addressbooks.AddressBooksHandler,
            net.bluemind.contact.vcards.VCardsHandler ]
      }, {
        path : '',
        handlers : []
      }, {
        path : '/$',
        handlers : []
      }, {
        path : '/individual/consult/',
        handlers : [ net.bluemind.contact.individual.consult.IndividualConsultHandler ]
      }, {
        path : '/individual/edit/',
        handlers : [ net.bluemind.contact.individual.edit.IndividualEditHandler ]
      }, {
        path : '/group/consult/',
        handlers : [ net.bluemind.contact.group.consult.GroupConsultHandler ]
      }, {
        path : '/group/edit/',
        handlers : [ net.bluemind.contact.group.edit.GroupEditHandler ]
      }

  ];
  goog.base(this, 'contact', '/contact/', routes);
}
goog.inherits(net.bluemind.contact.ContactApplication, net.bluemind.mvp.Application);

/** @override */
net.bluemind.contact.ContactApplication.prototype.registerFilters = function(router) {
  goog.base(this, 'registerFilters', router);
  router.addFilter(new net.bluemind.contact.filters.AddressBooksFilter());
  router.addFilter(new net.bluemind.contact.filters.VCardFilter());
};

/** @override */
net.bluemind.contact.ContactApplication.prototype.registerServices = function(ctx) {
  goog.base(this, 'registerServices', ctx);
  ctx.service("folders", net.bluemind.folder.service.FoldersService);
  ctx.service("addressbooks", net.bluemind.addressbook.service.AddressBooksService);
  ctx.service("addressbook", net.bluemind.addressbook.service.AddressBookService);
  ctx.service("tags", net.bluemind.tag.service.TagService);
  ctx.service("addressbooks-sync-manager", net.bluemind.addressbook.service.AddressBooksSyncManager);

};

/** @override */
net.bluemind.contact.ContactApplication.prototype.bootstrap = function(ctx) {
  return goog.base(this, 'bootstrap', ctx).thenCatch(function(error) {
    goog.log.error(this.logger, error.toString(), error);
    ctx.notifyError("startup error", error);
  }, this);
};

net.bluemind.contact.ContactApplication.prototype.getDbSchemas = function(ctx) {
  var root = goog.base(this, 'getDbSchemas', ctx);
  return goog.array.concat(root, [ {
    name : 'tag',
    schema : net.bluemind.container.persistance.schema
  }, {
    name : 'folder',
    schema : net.bluemind.folder.persistance.schema
  }, {
    name : 'contact',
    schema : net.bluemind.addressbook.persistance.schema
  } ]);
};


/** @override */
net.bluemind.contact.ContactApplication.prototype.postBootstrap = function(ctx) {
  goog.base(this, 'postBootstrap', ctx);

  var sync = net.bluemind.sync.SyncEngine.getInstance();
  net.bluemind.tag.sync.UnitaryTagSync.registerAll(ctx, sync);
  net.bluemind.folder.sync.FoldersSync.register(ctx, sync);

  ctx.service("addressbooks-sync-manager").refreshBooks();
  sync.start(1);
  goog.log.info(this.logger,'Synchronization started');
  new net.bluemind.commons.ui.ApplicationViewHelper().afterBootstrap();

};
