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
 * @fileoverview Application bootstrap.
 */

goog.provide('net.bluemind.calendar.CalendarApplication');

goog.require('goog.Promise');
goog.require('net.bluemind.authentication.service.AuthService');
goog.require('net.bluemind.authentication.api.AuthClient');
goog.require('net.bluemind.date.DateHelper');
goog.require('net.bluemind.calendar.api.CalendarAutocompleteClient');
goog.require('net.bluemind.calendar.api.PrintClient');
goog.require('net.bluemind.calendar.create.CreateHandler');
goog.require('net.bluemind.calendar.defaultview.DefaultViewHandler');
goog.require('net.bluemind.calendar.day.DayHandler');
goog.require('net.bluemind.calendar.filters.CalendarsFilter');
goog.require('net.bluemind.calendar.filters.DateFilter');
goog.require('net.bluemind.calendar.list.ListHandler');
goog.require('net.bluemind.calendar.list.PendingEventsHandler');
goog.require('net.bluemind.calendar.minical.MiniCalHandler');
goog.require('net.bluemind.calendar.month.MonthHandler');
goog.require('net.bluemind.calendar.navigation.NavigationHandler');
goog.require('net.bluemind.calendar.service.CalendarService');
goog.require('net.bluemind.calendar.service.CalendarsService');
goog.require('net.bluemind.calendar.searchform.SearchFormHandler');
goog.require('net.bluemind.calendar.search.SearchHandler');
goog.require('net.bluemind.calendar.sync.CalendarSync');
goog.require('net.bluemind.calendar.toolbar.ToolbarHandler');
goog.require('net.bluemind.calendar.vevent.VEventHandler');
goog.require('net.bluemind.core.container.api.ContainersClient');
goog.require('net.bluemind.i18n.DateTimeHelper');
goog.require('net.bluemind.mvp.Router');
goog.require('net.bluemind.mvp.ApplicationContext');
goog.require('net.bluemind.mvp.banner.BannerHandler');
goog.require('net.bluemind.mvp.logo.LogoHandler');
goog.require('net.bluemind.tag.service.TagService');
goog.require('net.bluemind.sync.SyncEngine');
goog.require('net.bluemind.mvp.helper.URLHelper');
goog.require('net.bluemind.timezone.TimeZoneHelper');
goog.require('relief.rpc.RPCService');
goog.require("net.bluemind.folder.sync.FoldersSync");
goog.require("net.bluemind.folder.service.FoldersService");
goog.require("goog.debug.Console");
goog.require("net.bluemind.mvp.Application");
goog.require("net.bluemind.calendar.sync.CalendarViewSync");
goog.require("net.bluemind.calendar.service.CalendarViewService");
goog.require("net.bluemind.folder.service.FoldersService");
goog.require("net.bluemind.folder.service.FolderService");
goog.require("net.bluemind.container.sync.ContainerSettingsSync");
goog.require("net.bluemind.folder.persistence.schema");
goog.require("net.bluemind.calendar.persistence.schema");
goog.require("net.bluemind.todolist.persistence.schema");
goog.require("net.bluemind.tag.sync.TagSync");
goog.require("net.bluemind.addressbook.persistence.schema");
goog.require("net.bluemind.addressbook.sync.AddressBookSync");
goog.require('net.bluemind.addressbook.service.AddressBookService');
goog.require('net.bluemind.addressbook.service.AddressBooksService');
goog.require('net.bluemind.calendar.tasks.TasksHandler');
goog.require('net.bluemind.todolist.sync.TodoListSync');
goog.require("net.bluemind.todolist.service.TodoListsService");
goog.require("net.bluemind.todolist.service.TodoListService");
goog.require("net.bluemind.calendar.CalendarsMgmt");
goog.require("net.bluemind.calendar.MetadataMgmt");
goog.require("net.bluemind.calendar.vtodo.consult.VTodoConsultHandler");
goog.require("net.bluemind.mvp.filter.HistoryFilter");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.container.persistence.schema");
goog.require("net.bluemind.container.persistence.options");
goog.require("net.bluemind.ui.banner.widget.UnseenEvents");
goog.require("net.bluemind.addressbook.service.AddressBooksSyncManager");
goog.require("net.bluemind.tag.sync.UnitaryTagSync");
goog.require("net.bluemind.todolist.service.TodolistsSyncManager");
goog.require("net.bluemind.calendar.service.CalendarsSyncManager");
goog.require("net.bluemind.calendar.PendingEventsMgmt");
goog.require("net.bluemind.resource.persistence.schema");
goog.require("net.bluemind.resource.sync.ResourcesSync");
goog.require("net.bluemind.resource.sync.UnitaryResourcesSync");
goog.require("net.bluemind.resource.sync.ResourcesClientSync");
goog.require('net.bluemind.resource.service.ResourcesService');

/**
 * Calendar application
 * 
 * @constructor
 * @extends {net.bluemind.mvp.Application}
 */
net.bluemind.calendar.CalendarApplication = function() {
  var leftMenu = [net.bluemind.calendar.create.CreateHandler, net.bluemind.calendar.searchform.SearchFormHandler,
                  net.bluemind.calendar.minical.MiniCalHandler, net.bluemind.calendar.navigation.NavigationHandler, 
                  net.bluemind.calendar.toolbar.ToolbarHandler]
  var routes = [
      {
        path : '.*',
        handlers : [ net.bluemind.mvp.banner.BannerHandler ]
      },
      {
        path : '',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.defaultview.DefaultViewHandler, 
            net.bluemind.calendar.tasks.TasksHandler ])
      },
      {
        path : '/$',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.defaultview.DefaultViewHandler,
            net.bluemind.calendar.tasks.TasksHandler ])
      },
      {
        path : '/day/',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.day.DayHandler, 
            net.bluemind.calendar.tasks.TasksHandler ])
      },
      {
        path : '/month/',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.month.MonthHandler,
                    net.bluemind.calendar.tasks.TasksHandler ])
      },
      {
        path : '/list/',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.list.ListHandler, 
                     net.bluemind.calendar.tasks.TasksHandler ])
      },
      {
        path : '/pending/',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.list.PendingEventsHandler ])
      },
      {
        path : '/search/',
        handlers : goog.array.concat(leftMenu, [ net.bluemind.calendar.search.SearchHandler ])
      }, {
        path : '/vevent/',
        handlers : [net.bluemind.calendar.vevent.VEventHandler ]
      }, {
        path : '/vtodo/consult',
        handlers : [net.bluemind.calendar.vtodo.consult.VTodoConsultHandler ]
      }

  ];

  goog.base(this, 'cal', '/cal/', routes);
};
goog.inherits(net.bluemind.calendar.CalendarApplication, net.bluemind.mvp.Application);

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.bootstrap = function(ctx) {

  return goog.base(this, 'bootstrap', ctx).then(function() {
    return ctx.service('auth').get('calendar.calendars');
  }, null, this).then(function(uids) {
    if (!uids || uids.length == 0) {
      return ctx.service('calendars').list();
    } else {
      return ctx.service('calendars').listByUids(uids);
    }
  }, null, this).then(function(calendars) {
    calendars = goog.array.filter(calendars, function(calendar) {
      return goog.isDefAndNotNull(calendar);
    });
    if (calendars.length == 0) {
      throw 'No valid calendar found';
    }
    return calendars;
  }, null, this).thenCatch(function(e) {
    goog.log.error(this.logger, "Failed to load calendars. Fallback to default view.", e);
    goog.log.info(this.logger, 'initializing folders...');
    return this.initializeFolders_(ctx);
  }, this).then(function(calendars) {
    return ctx.service('calendarsMgmt').setCalendars(calendars);
  }).then(function() {
    return ctx.service('calendarviews').getView("default");
  }).then(function(view) {
    if (!view && ctx.online) {
      return ctx.service('calendarviews').getViewRemote("default");      
    }
    return view;
  }).then(function(view) {
    if (view && view.value) {
      return ctx.session.set('defaultview', view.value.type);
    }
  }, null, this).then(function() {
    this.setEnvironnement_(ctx);
  }, null, this).thenCatch(function(error) {
    goog.log.error(this.logger, error.toString(), error);
    ctx.notifyError("startup error", error);
  }, this)
};

/** 
 * Set environnement variables
 * @private
 */
net.bluemind.calendar.CalendarApplication.prototype.setEnvironnement_ = function(ctx) {
  // TODO: Crappy
  // FIXME is crappy ?
  var fdow = ctx.settings.get('day_weekstart') == 'monday' ? 0 : 6;
  var fwcod = 3;
  var was = goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK;
  goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK = fdow;
  goog.i18n.DateTimeSymbols_en.FIRSTDAYOFWEEK = fdow;
  goog.i18n.DateTimeSymbols.FIRSTWEEKCUTOFFDAY = fwcod;
  goog.i18n.DateTimeSymbols_en.FIRSTWEEKCUTOFFDAY = fwcod;
  goog.date.Date.prototype.firstDayOfWeek_ = fdow;
  goog.date.Date.prototype.firstWeekCutOffDay_ = fwcod;
}

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.postBootstrap = function(ctx) {
  goog.base(this, 'postBootstrap', ctx);

  var sync = net.bluemind.sync.SyncEngine.getInstance();
  var settings = new net.bluemind.container.sync.ContainerSettingsSync(ctx);
  var calView = new net.bluemind.calendar.sync.CalendarViewSync(ctx);

  net.bluemind.tag.sync.UnitaryTagSync.registerAll(ctx, sync);

  ctx.service("todolists-sync-manager").refresh();

  sync.registerService(calView).registerService(settings);

  net.bluemind.folder.sync.FoldersSync.register(ctx, sync);

  ctx.service("addressbooks-sync-manager").refreshBooks();
  ctx.service("calendars-sync-manager").refresh();

  sync.start(1);
  goog.log.info(this.logger,'Synchronization started');

};

/** 
 * Initialize calendars
 * @private
 * @return {goog.Thenable.<Array.Object<String, *>>} 
 */
net.bluemind.calendar.CalendarApplication.prototype.initializeFolders_ = function(ctx) {
  var calendars = [];
  return ctx.service('calendarviews').getViewRemote("default").then(function(view) {
    return ctx.service('folders').getFoldersRemote(null, view['value']['calendars']);
 });
}

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.registerFilters = function(router) {
  goog.base(this, 'registerFilters', router);
  router.addFilter(new net.bluemind.calendar.filters.DateFilter());
  router.addFilter(new net.bluemind.calendar.filters.CalendarsFilter());
  router.addFilter(new net.bluemind.mvp.filter.HistoryFilter([ 'refresh' ]));
};

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.registerClients = function(ctx) {
  goog.base(this, 'registerClients', ctx);
  ctx.client('calendar-autocomplete', net.bluemind.calendar.api.CalendarAutocompleteClient);
  ctx.client('calendar', net.bluemind.calendar.api.CalendarClient);
  ctx.client('print', net.bluemind.calendar.api.PrintClient);
  ctx.client('calendar-view', net.bluemind.calendar.api.CalendarViewClient);
};

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.registerServices = function(ctx) {
  goog.base(this, 'registerServices', ctx);
  ctx.service("folders", net.bluemind.folder.service.FoldersService);
  ctx.service("addressbooks", net.bluemind.addressbook.service.AddressBooksService);
  ctx.service("addressbook", net.bluemind.addressbook.service.AddressBookService);
  ctx.service("calendar", net.bluemind.calendar.service.CalendarService);
  ctx.service("calendars", net.bluemind.calendar.service.CalendarsService);
  ctx.service("tags", net.bluemind.tag.service.TagService);
  ctx.service("calendarviews", net.bluemind.calendar.service.CalendarViewService);
  ctx.service("todolists", net.bluemind.todolist.service.TodoListsService);
  ctx.service("todolist", net.bluemind.todolist.service.TodoListService);
  ctx.service("calendarsMgmt", net.bluemind.calendar.CalendarsMgmt);
  ctx.service("calendars-sync-manager", net.bluemind.calendar.service.CalendarsSyncManager);
  ctx.service("addressbooks-sync-manager", net.bluemind.addressbook.service.AddressBooksSyncManager);
  ctx.service("todolists-sync-manager", net.bluemind.todolist.service.TodolistsSyncManager);
  ctx.service("metadataMgmt", net.bluemind.calendar.MetadataMgmt);
  ctx.service("pendingEventsMgmt", net.bluemind.calendar.PendingEventsMgmt);
  ctx.service("resources", net.bluemind.resource.service.ResourcesService);
};

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.registerHelpers = function(ctx) {
  goog.base(this, 'registerHelpers', ctx);
  var helper = new net.bluemind.mvp.helper.URLHelper();
  ctx.helper('url', helper);
};

/** @override */
net.bluemind.calendar.CalendarApplication.prototype.getDbSchemas = function(ctx) {
	var root = goog.base(this, 'getDbSchemas', ctx);
	return goog.array.concat(root, [ {
		name : 'tag',
		schema : net.bluemind.container.persistence.schema,
		options : net.bluemind.container.persistence.options
	}, {
		name : 'folder',
		schema : net.bluemind.folder.persistence.schema
	}, {
		name : 'contact',
		schema : net.bluemind.addressbook.persistence.schema
	}, {
		name : 'calendarview',
		schema : net.bluemind.container.persistence.schema
	}, {
		name : 'calendar',
		schema : net.bluemind.calendar.persistence.schema
	}, {
		name : 'todolist',
		schema : net.bluemind.todolist.persistence.schema
	}, {
		name : 'resources',
		schema : net.bluemind.resource.persistence.schema
	} ]);
};
