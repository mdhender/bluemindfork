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
 * Bluemind application banner.
 */

goog.provide('bluemind.ui.Banner');
goog.provide("net.bluemind.ui.banner.Banner");
goog.provide('net.bluemind.ui.banner.Banner.CSS_');
goog.provide('net.bluemind.ui.banner.Banner.StatusType_');

goog.require('bluemind.ui.BannerModel');
goog.require('bm.extensions.ExtensionsManager');
goog.require('net.bluemind.announcement.api.UserAnnouncementsClient');
goog.require('net.bluemind.net.OnlineHandler');
goog.require('net.bluemind.ui.banner.BannerWidget');
goog.require('net.bluemind.ui.banner.templates');
goog.require('net.bluemind.ui.Notification');
goog.require('net.bluemind.ui.BannerMessage');
goog.require('net.bluemind.sync.SyncEngine');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.classlist');
goog.require('goog.ui.Component');
goog.require('goog.ui.Dialog');
goog.require('goog.ui.Dialog.ButtonSet');
goog.require('goog.ui.Menu');
goog.require('goog.ui.MenuSeparator');
goog.require('goog.ui.MenuItem');
goog.require('goog.ui.MenuButton');
goog.require('goog.soy');

/**
 * Bluemind application banner
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.ui.banner.Banner = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  var menu = new goog.ui.Menu();
  menu.addChild(new goog.ui.MenuSeparator(), true);
  var child = new goog.ui.MenuItem(goog.soy.renderAsFragment(net.bluemind.ui.banner.templates.settings));
  child.setId('settings');
  menu.addChild(child, true);
  menu.addChild(new goog.ui.MenuSeparator(), true);
  child = new goog.ui.MenuItem(goog.soy.renderAsFragment(net.bluemind.ui.banner.templates.logout));
  child.setId('logout');
  menu.addChild(child, true);
  child = new goog.ui.MenuButton(undefined, menu);
  child.setId('user');
  child.setContent(goog.soy.renderAsFragment(net.bluemind.ui.banner.templates.anonymous));
  child.setRenderMenuAsSibling(true);
  this.addChild(child, true);
  goog.dom.classlist.set(child.getElement(), goog.getCssName('bm-user'));

  child = new goog.ui.Control(this.getDomHelper().createDom('div', goog.getCssName('bar')));
  child.addClassName(goog.getCssName('progress'));
  child.addClassName(goog.getCssName('loading'));
  child.setId('sync-status');
  this.addChild(child, true);

  child = new goog.ui.Component();
  child.setId('widgets');
  this.addChild(child, true);
  goog.dom.classlist.set(child.getElement(), 'bm-widgets');

  child = new goog.ui.Component();
  child.setId('logo');
  this.addChild(child, true);
  goog.dom.classlist.set(child.getElement(), "logo");

  child = new goog.ui.Component();
  child.setId('entries');
  this.addChild(child, true);
  goog.dom.classlist.set(child.getElement(), "applications");

  child = new net.bluemind.ui.Notification();
  this.addChild(child, true);

  child = new net.bluemind.ui.BannerMessage();
  this.addChild(child, true);

};
goog.inherits(net.bluemind.ui.banner.Banner, goog.ui.Component);

/**
 * Unread mail count
 * 
 * @type {number}
 * @private
 */
net.bluemind.ui.banner.Banner.prototype.mails_ = 0;

/**
 * @private {string}
 */
net.bluemind.ui.banner.Banner.prototype.logo_ = 'images/logo-bluemind.png';

/**
 * Pending event count
 * 
 * @type {number}
 * @private
 */
net.bluemind.ui.banner.Banner.prototype.events_ = 0;

/** @override */
net.bluemind.ui.banner.Banner.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();

  if (!goog.dom.classlist.contains(el, goog.getCssName('banner'))) {
    goog.dom.classlist.add(el, goog.getCssName('banner'));
  }

  var manager = bm.extensions.ExtensionsManager.getInstance();
  var w = manager.getExtensionPoint('net.bluemind.ui.commons.banner.pendingActions');
  goog.array.forEach(w.getExtensions(), function(ext) {
    ext.resolve().then(function() {
      var p = ext.data('module-pending-actions');
      var module = p['module'];
      goog.global[p['jsfunc']].apply(null, [ goog.bind(this.updatePendingActions_, this, module) ]);
    }, null, this);

  }, this);
  var x = manager.getExtensionPoint('net.bluemind.mvp.logo');
  var entries = goog.array.map(x.getExtensions(), function(ext) {
    return ext.data('logo');
  });

  goog.array.sort(entries, function(a, b) {
    return (goog.string.parseInt(b["priority"]) || 0) - (goog.string.parseInt(a["priority"]) || 0);
  });

  if (entries.length > 0) {
    this.logo_ = entries[0]['image'];
  }
};

/** @override */
net.bluemind.ui.banner.Banner.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('user'), goog.ui.Component.EventType.ACTION, function(e) {
    if (e.target.getId() == 'logout') {
      goog.global['location']['href'] = 'bluemind_sso_logout';
    }

    if (e.target.getId() == 'settings') {
      goog.global['location']['href'] = "/settings/index.html#" + this.getModel()['selectedEntry'];
    }
  });
  this.getHandler().listen(net.bluemind.net.OnlineHandler.getInstance(), 'online', this.setOnline_);
  this.getHandler().listen(net.bluemind.net.OnlineHandler.getInstance(), 'offline', this.setOffline_);
  this.getHandler().listen(net.bluemind.sync.SyncEngine.getInstance(), 'start', this.setSyncing_);
  this.getHandler().listen(net.bluemind.sync.SyncEngine.getInstance(), 'stop', this.detectStatus_);
  this.detectStatus_();
};

/** @override */
net.bluemind.ui.banner.Banner.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);

  if (model['entries']) {
    this.drawEntries();
  }

  if (model['user']) {
    this.getChild('user').setContent(goog.soy.renderAsFragment(net.bluemind.ui.banner.templates.userInfo, {
      user : {
        uid : model['user']['uid'],
        domainUid : model['user']['domainUid'],
        displayName : model['user']['displayName']
      }
    }));
    this.getChild('user').invalidateMenuSize();
    this.displayAnnouncements_();
  }

  if (model['widgets']) {
    var widgetsComp = this.getChild('widgets');

    var el = widgetsComp.getElement();

    // widgets
    goog.array.forEach(model['widgets'], function(ext) {
      var wWrapper = new net.bluemind.ui.banner.BannerWidget(ext);
      this.getChild('widgets').addChild(wWrapper, true);
    }, this);

  }
  var frag = goog.soy.renderAsFragment(net.bluemind.ui.banner.templates.logo, {
    src : this.logo_,
    version : goog.global['bmcSessionInfos']['bmVersion'],
    brandVersion : goog.global['bmcSessionInfos']['bmBrandVersion']
  });

  this.getDomHelper().appendChild(this.getChild('logo').getElement(), frag);
  this.getChild('user').getMenu().getChild('settings').getElementByClass(goog.getCssName('settings-link')).href = "/settings/index.html#"
      + this.getModel()['selectedEntry'];
}

/**
 * Disable/enable components when offline/online
 */
net.bluemind.ui.banner.Banner.prototype.online_ = function() {
  var online = net.bluemind.net.OnlineHandler.getInstance().isOnline();
  if (!online) {
    this.getChild('user').getMenu().getChild('settings').setEnabled(false);
    this.getChild('user').getMenu().getChild('logout').setEnabled(false);
  } else {
    this.getChild('user').getMenu().getChild('logout').setEnabled(true);
    this.getChild('user').getMenu().getChild('settings').setEnabled(true);
  }
};

net.bluemind.ui.banner.Banner.prototype.drawEntries = function() {

  var online = net.bluemind.net.OnlineHandler.getInstance().isOnline();
  var entriesComp = this.getChild('entries');
  entriesComp.removeChildren(true);
  goog.array.forEach(this.getModel()['entries'], function(entry) {
    if (!online && entry['offline'] != "true") {
      return;
    }
    var comp = new goog.ui.Component();
    entriesComp.addChild(comp, true);
    var selected = (entry['root'] == this.getModel()['selectedEntry']);
    comp.getElement().innerHTML = net.bluemind.ui.banner.templates.bannerEntry({
      entry : entry,
      selected : selected
    });
    goog.dom.classlist.enable(comp.getElement(), goog.getCssName('selected'), selected)
  }, this);

}

net.bluemind.ui.banner.Banner.prototype.displayAnnouncements_ = function() {
  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid'],
    'Accept' : 'application/json'
  }));
  var client = new net.bluemind.announcement.api.UserAnnouncementsClient(rpc, '',
      goog.global['bmcSessionInfos']['userId']);
  client.get().then(function(res) {
    goog.array.forEach(res, function(message) {
      var doc = goog.dom.getDocument();
      var evt = doc.createEvent('Event');// new
      evt.initEvent('ui-banner-message', true, true);
      evt['detail'] = {
        'type' : message['kind'].toLowerCase(),
        'message' : message['message'],
        'closeable' : message['closeable'],
        'link' : message['link']
      };
      doc.dispatchEvent(evt);
    });
  });

}

net.bluemind.ui.banner.Banner.prototype.updatePendingActions_ = function(module, count) {
  var model = this.getModel();
  if (!model || !model['entries']) {
    return;
  }
  var entry = goog.array.find(model['entries'], function(e) {
    return e['root'] == module;
  });

  if (entry) {
    entry.pendingActions = count;
    this.drawEntries();
  }
}

/**
 * Detect the status and set it
 */
net.bluemind.ui.banner.Banner.prototype.detectStatus_ = function() {
  if (net.bluemind.net.OnlineHandler.getInstance().isOnline()) {
    this.setOnline_();
  } else {
    this.setOffline_();
  }
  this.drawEntries();
};

/**
 * Set offline status
 */
net.bluemind.ui.banner.Banner.prototype.setOffline_ = function() {
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ONLINE, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.OFFLINE, true);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ERROR, false);
  this.getChild('user').getMenu().getChild('settings').setEnabled(false);
  this.getChild('user').getMenu().getChild('logout').setEnabled(false);
  this.getChild('sync-status').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, false);
  /** @meaning banner.offlineCaption */
  var MSG_ONLINE_STATUS = goog.getMsg('Offline');
  this.getChild('user').getElementByClass(goog.getCssName('status-caption')).innerHTML = MSG_ONLINE_STATUS;
  this.drawEntries();

};

/**
 * Set offline status
 */
net.bluemind.ui.banner.Banner.prototype.setOnline_ = function() {
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ONLINE, true);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.OFFLINE, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ERROR, false);
  this.getChild('user').getMenu().getChild('settings').setEnabled(true);
  this.getChild('user').getMenu().getChild('logout').setEnabled(true);
  this.getChild('sync-status').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, false);
  /** @meaning banner.onlineCaption */
  var MSG_OFFLINE_STATUS = goog.getMsg('Online');
  this.getChild('user').getElementByClass(goog.getCssName('status-caption')).innerHTML = MSG_OFFLINE_STATUS;
  this.drawEntries();
};

/**
 * Set offline status
 */
net.bluemind.ui.banner.Banner.prototype.setSyncing_ = function() {
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ONLINE, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.OFFLINE, false);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, true);
  this.getChild('user').enableClassName(net.bluemind.ui.banner.Banner.CSS_.ERROR, false);
  this.getChild('sync-status').enableClassName(net.bluemind.ui.banner.Banner.CSS_.SYNCING, true);
  /** @meaning banner.synchronisationCaption */
  var MSG_SYNCING_STATUS = goog.getMsg('Syncing...');
  this.getChild('user').getElementByClass(goog.getCssName('status-caption')).innerHTML = MSG_SYNCING_STATUS;
};

/**
 * 
 * @enum {string}
 */
net.bluemind.ui.banner.Banner.StatusType_ = {
  OFFLINE : 'off',
  ONLINE : 'on',
  SYNCING : 's',
  ERROR : 'e'
};

/**
 * @enum {string}
 */
net.bluemind.ui.banner.Banner.CSS_ = {
  OFFLINE : goog.getCssName('offline'),
  ONLINE : goog.getCssName('online'),
  SYNCING : goog.getCssName('syncing'),
  ERROR : goog.getCssName('error')
};

bluemind.ui.Banner = net.bluemind.ui.banner.Banner
