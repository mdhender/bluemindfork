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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.calendar.toolbar.ToolbarPresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("net.bluemind.date.Date");
goog.require("goog.date.Interval");
goog.require("goog.Timer");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.calendar.toolbar.ToolbarView");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.calendar.PendingEventsMgmt");
/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.toolbar.ToolbarPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.toolbar.ToolbarView(ctx);
  this.registerDisposable(this.view_);
  this.handler.listen(this.view_.getChild('today'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("?date=" + new net.bluemind.date.Date().toIsoString(), true)
  });
  this.handler.listen(this.view_.getChild('pending'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("/pending/", true)
  });
  this.handler.listen(this.view_.getChild('day'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("/day/?range=day", true)
  });
  this.handler.listen(this.view_.getChild('week'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("/day/?range=week", true)
  });
  this.handler.listen(this.view_.getChild('month'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("/month/", true)
  });
  this.handler.listen(this.view_.getChild('list'), goog.ui.Component.EventType.ACTION, function() {
    this.ctx.helper('url').redirect("/list/", true)
  });
  this.handler.listen(this.view_.getChild('previous'), goog.ui.Component.EventType.ACTION, function() {
    var date = this.ctx.session.get('range').getStartDate();
    // BM-7850 Fix: business week prev period
    if (ctx.settings.get('showweekends') !== 'true' && ctx.session.get('range.size') == 5) {
      date.add(new goog.date.Interval(goog.date.Interval.DAYS, -7))
    } else { 
      date.add(new goog.date.Interval(goog.date.Interval.DAYS, -1))
    }
    this.ctx.helper('url').redirect("?date=" + date.toIsoString(), true)
  });
  this.handler.listen(this.view_.getChild('next'), goog.ui.Component.EventType.ACTION, function() {
    var date = this.ctx.session.get('range').getEndDate();
    // BM-7850 Fix: business week next period
    if (ctx.settings.get('showweekends') !== 'true' && ctx.session.get('range.size') == 5) {
      date = this.ctx.session.get('range').getStartDate();
      date.add(new goog.date.Interval(goog.date.Interval.DAYS, +7))
    }
    this.ctx.helper('url').redirect("?date=" + date.toIsoString(), true)
  });

  this.handler.listen(this.ctx.service("pendingEventsMgmt"), 'change', function() {
    this.updatePendingCount();
  });

};
goog.inherits(net.bluemind.calendar.toolbar.ToolbarPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.toolbar.ToolbarView}
 * @private
 */
net.bluemind.calendar.toolbar.ToolbarPresenter.prototype.view_;

/** @override */
net.bluemind.calendar.toolbar.ToolbarPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('content-body'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.toolbar.ToolbarPresenter.prototype.setup = function() {
  this.view_.setModel({
    view : this.ctx.session.get('view'),
    range : this.ctx.session.get('range')
  });
  this.updatePendingCount();
  return goog.Promise.resolve();
};

net.bluemind.calendar.toolbar.ToolbarPresenter.prototype.updatePendingCount = function() {
  return this.ctx.service("pendingEventsMgmt").retrievePendingActions().then(function(total) {
    this.view_.setPendingCount(total);
  }, null, this);
}

/** @override */
net.bluemind.calendar.toolbar.ToolbarPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};
