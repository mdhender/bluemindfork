/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2018
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

//goog.provide("net.bluemind.ui.banner.logout.BannerLogout");

goog.require("goog.Timer");
goog.require("goog.storage.ExpiringStorage");
goog.require("goog.storage.mechanism.HTML5LocalStorage");

goog.require("net.bluemind.concurrent.CrossWindowLock");

goog.global['bundleResolve']('net.bluemind.restclient.closure', function() {
    var TICK = 240000;
    var PREFIX = "LOGOUT-"
    var timer = new goog.Timer(TICK);
    var lock = new net.bluemind.concurrent.CrossWindowLock("AuthPing");
    var storage = new goog.storage.ExpiringStorage(new goog.storage.mechanism.HTML5LocalStorage());

    timer.addEventListener(goog.Timer.TICK, function() {
        var sid = goog.global['bmcSessionInfos']['sid'];
        if (storage.get(PREFIX + sid)) {
            logout();
        } else if (lock.tryLock(TICK)) {
            goog.global['restClient'].sendMessage({
                "method" : "GET",
                "path" : "/api/auth/ping",
                "headers" : {
                    "X-BM-ApiKey" : sid
                },
                "params" : {}
            }, function(res) {
                if (res["statusCode"] == 401) {
                    storage.set(PREFIX + "sid", "true", goog.now() + TICK * 2);
                    logout();
                }
            });
        }
    });
    timer.start(); 
});

function logout() {
    var redirectURL = goog.global['location']['protocol'] + "//" + goog.global['location']['host'];
    redirectURL += goog.global['location']['pathname'].substring(0, window.location.pathname.indexOf('/', 1) + 1);
    goog.global['location'].assign(redirectURL);
}
