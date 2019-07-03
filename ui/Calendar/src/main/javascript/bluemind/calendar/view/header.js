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
 * @fileoverview Calendar month view.
 */

goog.provide('bluemind.calendar.view.Header');

goog.require('bluemind.calendar.ui.pending.Notification');
goog.require('bluemind.calendar.ui.widget.ConnectionStatus');
goog.require('bluemind.net.OnlineHandler');
goog.require('bluemind.ui.Dialer');
goog.require('goog.dom.classes');
goog.require('goog.dom.forms');
goog.require('goog.events.KeyCodes');
goog.require('goog.json');

/**
 * BlueMind Calendar header
 *
 * @param {boolean} hideBandal hide bandal.
 *
 * @constructor
 */
bluemind.calendar.view.Header = function(hideBandal) {
  this.manager_ = bluemind.manager;

  var data = {
    displayName: bluemind.me['displayName'],
    profile: bluemind.me['profile'],
    picture: bluemind.me['picture'],
    sid: bluemind.me['sid'],
    version: bluemind.me['fullStringVersion'],
    mailPerms: bluemind.me['mailPerms'],
    unreadMailCount: bluemind.me['unreadMailCount'],
    hideBandal: hideBandal,
    roles: bluemind.me['roles'],
    im: bluemind.me['im']
  };
  var header = soy.renderAsFragment(bluemind.calendar.template.header, data);
  goog.dom.appendChild(goog.dom.getElement('pageHeader'), header);

  var searchForm = goog.dom.getElement('search-form');
  goog.events.listen(searchForm, goog.events.EventType.SUBMIT, function(e) {
    e.stopPropagation();
    this.search();
  }, false, this);

  var searchField = goog.dom.getElement('pattern');
  searchField.placeholder = bluemind.calendar.template.i18n.search();

  var searchBtn = goog.dom.getElement('search-btn');
  goog.events.listen(searchBtn, goog.events.EventType.CLICK, function(e) {
    e.stopPropagation();
    this.search();
  }, false, this);

  bluemind.pendingNotification =
    new bluemind.calendar.ui.pending.Notification();
  bluemind.pendingNotification
    .render(goog.dom.getElement('notificationContainer'));

  this.cs_ = new bluemind.calendar.ui.widget.ConnectionStatus();
  this.cs_.render(goog.dom.getElement('connectionStatusContainer'));

 
  var dial = goog.dom.getElement('dial');
  if (dial) {
    var bandal = goog.dom.getElement('bandal');
    this.dialer_ = new bluemind.ui.Dialer();
    var url = new goog.Uri(bluemind.path + '/bmc');
    url.setParameterValue('service', 'ac');
    url.setParameterValue('method', 'phone');
    this.dialer_.url = url.toString();     
    this.dialer_.render(bandal);
    this.dialer_.setVisible(false);
    
    goog.events.listen(dial, goog.events.EventType.CLICK, function() {
      this.dialer_.toggleVisibility();
    }, false, this);
    goog.events.listen(this.dialer_, goog.ui.Component.EventType.ACTION, function(e) {
      bluemind.calendar.Controller.getInstance().call(e);
    }, false, this);        
  }

  var logoutLink = goog.dom.getElement('logoutLnk');
  if (!bluemind.net.OnlineHandler.getInstance().isOnline()) {
    goog.dom.forms.setDisabled(searchForm, true);
    var title = bluemind.calendar.template.i18n.onlineOnly();
    searchForm.title = title;
    logoutLink.title = title;
    logoutLink.style.color = '#666';
    logoutLink.href = 'javascript:void(0)';
    logoutLink.style.cursor = 'default';
  }
  goog.events.listen(bluemind.net.OnlineHandler.getInstance(), 'offline',
    function() {
    goog.dom.forms.setDisabled(searchForm, true);
    var title = bluemind.calendar.template.i18n.onlineOnly();
    searchForm.title = title;
    logoutLink.title = title;
    logoutLink.style.color = '#666';
    logoutLink.href = 'javascript:void(0)';
    logoutLink.style.cursor = 'default';
  });
  goog.events.listen(bluemind.net.OnlineHandler.getInstance(), 'online',
    function() {
    goog.dom.forms.setDisabled(searchForm, false);
    searchForm.title = '';
    logoutLink.title = '';
    logoutLink.style.color = '';
    logoutLink.href = 'bluemind_sso_logout';
    logoutLink.style.cursor = 'pointer';
  });

};

/**
 * Connection status widget 
 *
 * @type {bluemind.calendar.ui.widget.ConnectionStatus}
 * @private
 */
bluemind.calendar.view.Header.prototype.cs_;

/**
 * Dialer widget
 * @type {bluemind.ui.Dialer}
 * @private
 */
bluemind.calendar.view.Header.prototype.dialer_;

/**
 * Calendar manager
 *
 * @type {bluemind.calendar.Manager}
 * @private
 */
bluemind.calendar.view.Header.prototype.manager_;

/**
 * submit
 * @param {goog.events.KeyEvent} e key event.
 */
bluemind.calendar.view.Header.prototype.submit = function(e) {
  if (e.keyCode == goog.events.KeyCodes.ENTER) this.search();
};

/**
 * search.
 */
bluemind.calendar.view.Header.prototype.search = function() {
  bluemind.calendar.Controller.getInstance().search();
};
