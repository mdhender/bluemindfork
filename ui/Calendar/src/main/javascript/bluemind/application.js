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

goog.provide('bm.cal.Application');

goog.require('bluemind.auth.AuthManager');
goog.require('bluemind.events.NotificationHandler');
goog.require('bluemind.events.LinkHandler');
goog.require('bluemind.nav.NavManager');
goog.require('bluemind.net.OnlineHandler');
goog.require('bluemind.ServiceProvider');
goog.require('bluemind.sync.SyncEngine');
goog.require('goog.dom');
goog.require('goog.events');
goog.require('goog.structs.Map');
goog.require('relief.cache.Cache');
goog.require('relief.rpc.RPCService');
goog.require('relief.utils');
goog.require('bluemind.calendar.urls');
goog.require('bluemind.cal.view.HeaderView');
goog.require('net.bluemind.tag.sync.TagSync');
goog.require('bluemind.calendar.CalendarSync');
/**
 * Contact application
 * 
 * @constructor
 */
bm.cal.Application = function() {

  this.cache_ = new relief.cache.Cache();

  this.rpc_ = new relief.rpc.RPCService(this.cache_, new goog.structs.Map({
    'X-BM-ApiKey' : bmcSessionInfos.sid
  }));

  this.auth_ = new bluemind.auth.AuthManager();

  var notification = new bluemind.events.NotificationHandler();

  var iframe = /** @type {!HTMLIFrameElement} */
  (goog.dom.getElement('history_frame'));
  var input = /** @type {!HTMLInputElement} */
  (goog.dom.getElement('history_input'));
  var content = /** @type {!Element} */
  (goog.dom.getElement('content-body'));
  this.sp_ = new bluemind.ServiceProvider(bluemind.calendar.urls, this.cache_,
      this.rpc_, this.auth_, notification, iframe, input, content);

  this.auth_.setServiceProvider(this.sp_);

  this.manager_ = new bluemind.calendar.Manager();
  this.init_();
};

/**
 * The application's cache.
 * 
 * @type {relief.cache.Cache}
 * @private
 */
bm.cal.Application.prototype.cache_;

/**
 * The app's service provider.
 * 
 * @type {!bluemind.ServiceProvider}
 * @private
 */
bm.cal.Application.prototype.sp_;

/**
 * The app's RPC Service, which is given our Cache instance.
 * 
 * @type {relief.rpc.RPCService}
 * @private
 */
bm.cal.Application.prototype.rpc_;

/**
 * This is the simplest AuthManager implementation possible
 * 
 * @type {bm.auth.Manager}
 * @private
 */
bm.cal.Application.prototype.auth_;

/**
 * Navigation manager, handle routing, history, dispatching.
 * 
 * @type {bluemind.nav.NavManager}
 * @private
 */
bm.cal.Application.prototype.nav_;

/**
 * Application header.
 * 
 * @type {bm.cal.presenter.HeaderPresenter}
 * @private
 */
bm.cal.Application.prototype.header_;

/**
 * Initialize application storage, timers, handlers...
 */
bm.cal.Application.prototype.init_ = function() {
  // First initialize storage
  bluemind.storage.StorageHelper.initStorage().addCallback(function() {
    // Initialize online status
    return bluemind.net.OnlineHandler.getInstance().init(this.sp_)
  }, this).addCallback(function(online) {
    // Get logged user
    return this.auth_.init();
  }, this).addCallback(
      function() {
        var user = this.auth_.getUser();
        bluemind.user = user;
        // Initialize data factories

        // Start synchronization
        bluemind.sync.SyncEngine.getInstance().registerService(
            new net.bluemind.tag.sync.TagSync(this.sp_, ''));

        bluemind.sync.SyncEngine.getInstance().registerService(
            new bluemind.calendar.CalendarSync(this.sp_, ''));

     
        // Start synchronization
        bluemind.sync.SyncEngine.getInstance().start();

        var fdow = this.auth_.getSettings().get('firstdayofweek') == 'monday' ? 0 : 6;
        var fwcod = 3;
        goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK = fdow;
        goog.i18n.DateTimeSymbols_en.FIRSTDAYOFWEEK = fdow;
        goog.i18n.DateTimeSymbols.FIRSTWEEKCUTOFFDAY = fwcod;
        goog.i18n.DateTimeSymbols_en.FIRSTWEEKCUTOFFDAY = fwcod;
        goog.date.Date.prototype.firstDayOfWeek_ = fdow;
        goog.date.Date.prototype.firstWeekCutOffDay_ = fwcod;
        this.manager_.setup();
        // Render application
        this.render_();

        // Initialize link handlers
      }, this);
};

/**
 * Start application navigation
 */
bm.cal.Application.prototype.render_ = function() {
  this.nav_ = new bluemind.nav.NavManager(this.sp_);
  var content, view;
  bluemind.manager = this.manager_;
  bluemind.manager.visibleCalendars_ = new goog.structs.Map();
 var body = soy.renderAsFragment(bluemind.calendar.template.body);
  goog.dom.appendChild(document.body, body);

  var tb = bluemind.ui.Toolbar.getInstance();
  tb.render(goog.dom.getElement('toolbar'));
  content = goog.dom.getElement('pageHeader');
 view = new bluemind.cal.view.HeaderView();
  view.setModel(this.auth_.getUser());
  view.render(content);
  
  var tabBar = new goog.ui.TabBar();
  tabBar.decorate(goog.dom.getElement('bm-selector-tab'));
  goog.events.listen(tabBar, goog.ui.Component.EventType.SELECT,
    function(e) { 
      bluemind.manager.switchTab(e.target.getElement().id);
   });

  tabBar.setSelectedTabIndex(0);
  this.manager_.switchTab('bm-selector-calendars');

  var settings = this.auth_.getSettings();
  
  var tzData =
    bluemind.timezone.Detector.getInstance().get(settings.get('timezone'));
  
  goog.global['timezone'] = goog.i18n.TimeZone.createTimeZone(tzData);
  this.manager_.setSettings(settings);
  bluemind.view = new bluemind.calendar.View(this.manager_);
  var dv = settings.get('defaultview');
  switch (dv) {
  case 'day':
    bluemind.view.day();
    break;
  case 'week':
    bluemind.view.week();
    break;
  case 'month':
    bluemind.view.month();
    break;
  case 'agenda':
    bluemind.view.agenda();
    break;
  default:
    bluemind.view.week();
    break;
}

  
};
