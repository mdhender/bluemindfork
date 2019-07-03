goog.provide("net.bluemind.webmail.banner.WebmailBanner");

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
 * @fileoverview Calendar synchronization service.
 */

goog.require("goog.log");
goog.require('net.bluemind.mailbox.api.MailboxesClient');
goog.require("net.bluemind.container.service.ContainerObserver");
goog.require("net.bluemind.container.service.ContainersObserver");
goog.require("goog.async.Throttle");
goog.require("goog.Timer");
goog.require("goog.math");

net.bluemind.webmail.banner.WebmailBanner.retrievePendingActions = function() {
  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid']
  }));

  var domain = goog.global['bmcSessionInfos']['domain'];

  var mailboxClient = new net.bluemind.mailbox.api.MailboxesClient(rpc, '', domain);
  mailboxClient.getUnreadMessagesCount().then(function(count){
    net.bluemind.webmail.banner.WebmailBanner.notifyListener(count);
  });
}

net.bluemind.webmail.banner.WebmailBanner.notifyListener = function(pendingActions) {
  if( net.bluemind.webmail.banner.WebmailBanner.listener) {
    net.bluemind.webmail.banner.WebmailBanner.listener.apply(null, [pendingActions]);
  }
}

net.bluemind.webmail.banner.WebmailBanner.listener = null;

goog.global['webmailPendingActions'] = function(reciever) {
  if (net.bluemind.webmail.banner.WebmailBanner.listener) {
    net.bluemind.webmail.banner.WebmailBanner.listener = reciever;
    net.bluemind.webmail.banner.WebmailBanner.retrievePendingActions();
    console.error('Webmail pending listenr is already registered. It should not be called more than one time!')
  } else {
    // throttle at 1 call each 5 sec
    var throttle = new goog.async.Throttle( net.bluemind.webmail.banner.WebmailBanner.retrievePendingActions, 5000);
    var roles = goog.global['bmcSessionInfos']['roles'].split(',');
    roles = goog.array.map(roles, goog.string.trim);
    if( !goog.array.contains(roles, 'hasMail')) {
      console.log("user doesnt have 'hasMail' role, do not retrieve pending actions");
      return;
    }
    net.bluemind.webmail.banner.WebmailBanner.listener = reciever;
    net.bluemind.webmail.banner.WebmailBanner.retrievePendingActions();
    var handler = new goog.events.EventHandler(this);
    var obs = new net.bluemind.container.service.ContainersObserver();
    handler.listen(obs, net.bluemind.container.service.ContainersObserver.EventType.CHANGE, function() {
      goog.Timer.callOnce( function() {
        throttle.fire();
      }, goog.math.randomInt(5000));
    });
    obs.observerContainers('mailbox', [goog.global['bmcSessionInfos']['userId'] ]);
  }
}

goog.global['bundleResolve']('net.bluemind.restlclient.closure',function() {
});
