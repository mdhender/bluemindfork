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

goog.require('net.bluemind.ui.cti.Dialer');
goog.require('net.bluemin.ui.banner.template');
goog.require('goog.events.EventHandler');
goog.require('net.bluemind.cti.api.ComputerTelephonyIntegrationClient');
goog.require('relief.rpc.RPCService');
goog.require('relief.cache.Cache');

function CTIWidgetCreator() {

  var handler = new goog.events.EventHandler();
  var el = goog.soy.renderAsElement(net.bluemin.ui.banner.template.widget);

  handler.listen(el, goog.events.EventType.CLICK, function() {
    dialer.toggleVisibility();
  });

  var dialer = new net.bluemind.ui.cti.Dialer(el);
  dialer.setId('dialer');
  dialer.render();

  var rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid'],
    'Accept' : 'application/json'
  }));
  var client = new net.bluemind.cti.api.ComputerTelephonyIntegrationClient(rpc, '',
      goog.global['bmcSessionInfos']['domain'], goog.global['bmcSessionInfos']['userId']);

  handler.listen(dialer, goog.ui.Component.EventType.ACTION, function(e) {
    client.dial(e.number);
  });

  return el;
}

goog.global['CTIWidgetCreator'] = CTIWidgetCreator;
