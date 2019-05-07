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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.calendar.navigation.NavigationPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("net.bluemind.calendar.ColorPalette");
goog.require("net.bluemind.calendar.navigation.NavigationView");
goog.require("net.bluemind.calendar.vtodo.TodolistsManager");
goog.require("net.bluemind.calendar.navigation.events.EventType");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.tag.service.TagService");
/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.navigation.NavigationPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.navigation.NavigationView(ctx);
  this.registerDisposable(this.view_);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
};
goog.inherits(net.bluemind.calendar.navigation.NavigationPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.todolists_;

/**
 * @type {net.bluemind.calendar.navigation.NavigationView}
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.view_;

/** @override */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('content-menu'));
  var t = new net.bluemind.tag.service.TagService(this.ctx);
  this.handler.listen(t, net.bluemind.container.service.ContainersService.EventType.CHANGE, this.loadTags_);

  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.DELETE_VIEW, this.deleteView_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.SAVE_VIEW, this.saveView_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.SHOW_MY_CALENDAR,
      this.showMyCalendar_);

  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.SHOW_VIEW, this.showView_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.ADD_CALENDAR, this.addCalendar_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.REMOVE_CALENDAR,
      this.removeCalendar_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.SHOW_CALENDAR, this.showCalendar_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.HIDE_CALENDAR, this.hideCalendar_);
  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.CHANGE_CALENDAR_COLOR,
      this.changeCalendarColor_);

  this.handler.listen(this.view_, net.bluemind.calendar.navigation.events.EventType.TOGGLE_TAG, this.toggleTag_);

  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.ONLINE,
      function() {
        this.loadCalendars_();
        this.setOnline_();
      });
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.OFFLINE,
      function() {
        this.loadCalendars_();
        this.setOffline_();
      });

  var todolists = this.ctx.service('todolists');
  var cals = this.ctx.service('calendars');
  var cal = this.ctx.service('calendar');
  this.handler.listen(cals, net.bluemind.container.service.ContainersService.EventType.CHANGE, this.loadCalendars_);
  this.handler.listen(cal, net.bluemind.container.service.ContainerService.EventType.CHANGE, function() {
    this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {
      var hasChanged = !goog.array.equals(cals, this.ctx.session.get('calendars'), function(calInDb, calInSession) {
        return (calInDb['uid'] == calInSession['uid'])
      })
      if (hasChanged) {
        this.loadCalendars_();
      }
    }, null, this);
  });
  this.handler.listen(todolists, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.loadCalendars_();
    this.todolists_.getTodolistsModelView().then(function(todolists) {
      this.view_.setTodolists(todolists);
    }, null, this);
  }, false, this);
  // FIXME reload is violent !
  this.handler.listen(this.ctx.service('calendarviews'),
      net.bluemind.container.service.ContainerService.EventType.CHANGE, function() {
        this.loadViews_();
      }, false, this);
  this.handler.listen(this.ctx.service('tags'), net.bluemind.container.service.ContainerService.EventType.CHANGE,
      function() {
        this.loadTags_();
      }, false, this);
  return goog.Promise.all([ this.loadTags_(), this.loadViews_() ]).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/** @override */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.setup = function() {
  return this.loadCalendars_().then(function() {
    return this.loadTags_();
  }, null, this).then(function() {
    return this.loadViews_();
  }, null, this).then(function() {
    return this.todolists_.getTodolistsModelView()
  }, null, this).then(function(todolists) {
    return this.view_.setTodolists(todolists);
  }, null, this);
};

/** @override */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.loadCalendars_ = function() {
  return this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {
    return goog.array.map(cals, this.calendarToMV_, this);
  }, null, this).then(function(calendars) {
    this.view_.setCalendars(calendars);
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/**
 * Build calendar model for view
 * 
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.calendarToMV_ = function(calendar) {

  var owner = null;
  if (calendar['owner'] && calendar['owner'] != this.ctx.user['uid']
      && calendar['ownerDisplayname'] != calendar['name']) {
    owner = calendar['ownerDisplayname'];
  }

  var visible = calendar['metadata']['visible'];
  if (!this.ctx.online && !calendar['offlineSync']) {
    visible = false;
  }

  return {
    uid : calendar['uid'],
    type : 'calendar',
    name : calendar['name'],
    show : true,
    photo : calendar['photo'],
    ownerDisplayname : owner,
    states : {
      visible : visible,
      offlineSync : calendar['offlineSync']
    },
    color : {
      background : calendar['metadata']['color'],
      foreground : net.bluemind.calendar.ColorPalette.textColor(calendar['metadata']['color'], -0.3)
    },
    settings : calendar['settings'],
    verbs : calendar['verbs']
  }
};

/**
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.loadTags_ = function() {
  return this.ctx.service('tags').getTags().then(function(tags) {
    var mv = goog.array.map(tags, function(tag) {
      return {
        uid : tag['itemUid'],
        name : tag['label'],
        color : tag['color']
      }
    });

    goog.array.sortObjectsByKey(mv, 'name', goog.string.caseInsensitiveCompare);
    this.view_.setTags(mv);
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this)
};

/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.loadViews_ = function() {

  return this.ctx.service('calendarviews').getViews().then(function(views) {
    var vviews = goog.array.map(views, function(view) {
      return {
        uid : view['uid'],
        label : view['value']['label'],
        isDefault : view['value']['isDefault']
      };
    });
    this.view_.setViews(vviews);
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.setOnline_ = function() {
  this.view_.getChild('view-selector').getChild('menu-view').setEnabled(true);
  this.view_.getChild('view-selector').getChild('menu-view').getElement().title = '';
};
/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.setOffline_ = function() {
  this.view_.getChild('view-selector').getChild('menu-view').setEnabled(false);
  this.view_.getChild('view-selector').getChild('menu-view').getElement().title = '';
};

/**
 * @param {goog.event.Event} e Add calendar event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.addCalendar_ = function(e) {
  this.ctx.service('folders').getFoldersRemote(null, e.calendars).then(function(folders) {
    return this.ctx.service('calendarsMgmt').addCalendars(folders);
  }, null, this).then(function() {
    this.ctx.helper('url').reload();
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/**
 * @param {goog.event.Event} e Remove calendar event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.removeCalendar_ = function(e) {
  var calendar = e.calendar;
  this.ctx.service('calendarsMgmt').removeCalendar(calendar['uid']).then(function() {
    this.setCalendarVisible({
      uid : calendar['uid']
    }, null);
  }, null, this);
};

/**
 * @param {goog.event.Event} e Remove calendar event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.showCalendar_ = function(e) {
  var calendar = e.calendar;
  this.setCalendarVisible(calendar, true);

};

/**
 * @param {goog.event.Event} e Remove calendar event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.hideCalendar_ = function(e) {
  var calendar = e.calendar;
  this.setCalendarVisible(calendar, false);
};

net.bluemind.calendar.navigation.NavigationPresenter.prototype.setCalendarVisible = function(calendar, visible) {
  this.ctx.service('calendarsMgmt').setVisibility(calendar.uid, visible).then(function() {
    this.ctx.helper('url').reload();
  }, null, this)
};

/**
 * @param {goog.event.Event} e Change calendar color event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.changeCalendarColor_ = function(e) {
  var calendar = e.calendar;
  var color = e.color;
  return this.ctx.service('calendarsMgmt').setColor(calendar.uid, color).then(function() {
    this.ctx.helper('url').reload();
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorUpdateSettings(error), error);
  }, this);
};

/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.showMyCalendar_ = function() {
  return this.ctx.service('folders').getFolders('calendar') //
  .then(function(folders) {
    return this.ctx.service('calendarsMgmt').setCalendars(folders);
  }, null, this).then(function() {
    this.ctx.helper('url').reload();
  }, null, this);
};

/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.showView_ = function(e) {
  var lview = null;
  this.ctx.service('calendarviews').getView(e.viewUid).then(function(view) {
    lview = view;
    this.view_.setSelected(view);
    return this.ctx.service('folders').getFoldersRemote(null, view['value']['calendars']);
  }, null, this).then(function(folders) {
    return this.ctx.service('calendarsMgmt').setCalendars(folders);
  }, function(e) {
    var validFolders = [];
    var futures = goog.array.map(lview['value']['calendars'], function(c) {
      return this.ctx.service('folders').getFoldersRemote(null, new Array(c)).then(function(f) {
        validFolders.push(f);
      }, function(error) {
      }, this);
    }, this);
    return goog.Promise.all(futures).then(function(e) {
      return this.ctx.service('calendarsMgmt').setCalendars(validFolders);
    }, null, this);
  }, this).then(function() {
    if (this.ctx.online) {
      var viewType = lview['value']['type'];
      if (viewType == 'DAY') {
        this.ctx.helper('url').redirect("/day/?range=day&refresh=" + goog.now(), true);
      } else if (viewType == 'WEEK') {
        this.ctx.helper('url').redirect("/day/?range=week&refresh=" + goog.now(), true);
      } else if (viewType == 'MONTH') {
        this.ctx.helper('url').redirect("/month/?refresh=" + goog.now(), true);
      } else if (viewType == 'LIST') {
        this.ctx.helper('url').redirect("/list/?refresh=" + goog.now(), true);
      } else {
        this.ctx.helper('url').reload();
      }
    }
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
}
/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.saveView_ = function(e) {
  var cview = this.ctx.session.get('view');
  var type = 'WEEK';
  if (cview == 'list') {
    type = 'LIST';
  } else if (cview == 'month') {
    type = 'MONTH';
  } else if (cview == 'day') {
    type = 'DAY';
  } else {
    type = 'WEEK';
  }
  var view = {
    'label' : e.label,
    'type' : type,
    'calendars' : []
  };

  return this.ctx.service('calendarsMgmt').list('calendar').then(function(calendars) {
    view['calendars'] = goog.array.map(calendars, function(cal) {
      return cal['uid'];
    });
    if (e.uid) {
      this.ctx.service('calendarviews').updateView(e.uid, view).then(function() {
        this.loadViews_();
      }, null, this);

    } else {
      this.ctx.service('calendarviews').createView(net.bluemind.mvp.UID.generate(), view).then(function() {

        this.loadViews_();
      }, null, this);
    }

  }, null, this).then(function() {
    if (e.uid) {
      this.ctx.notifyInfo(net.bluemind.calendar.Messages.successViewUpdate());
    } else {
      this.ctx.notifyInfo(net.bluemind.calendar.Messages.successViewCreate());
    }
  }, function(error) {
    if (e.uid) {
      this.ctx.notifyError(net.bluemind.calendar.Messages.errorViewUpdate(error), error);
    } else {
      this.ctx.notifyError(net.bluemind.calendar.Messages.errorViewCreate(error), error);
    }
  }, this);
};

/**
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.deleteView_ = function(e) {
  this.ctx.service('calendarviews').deleteView(e.uid).then(function() {
    this.loadViews_();
  }, null, this).then(function() {
    this.ctx.notifyInfo(net.bluemind.calendar.Messages.successViewDelete());
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorViewDelete(error));
  }, this);
};

/**
 * @param {goog.event.Event} e Remove calendar event
 * @private
 */
net.bluemind.calendar.navigation.NavigationPresenter.prototype.toggleTag_ = function(e) {
  var selectedTag = this.ctx.session.get('selected-tag') || [];
  selectedTag = goog.array.clone(selectedTag);
  if (goog.array.contains(selectedTag, e.tag.uid)) {
    goog.array.remove(selectedTag, e.tag.uid);
  } else {
    goog.array.insert(selectedTag, e.tag.uid);
  }

  this.ctx.session.set('selected-tag', selectedTag);
  this.ctx.helper('url').reload();
};
