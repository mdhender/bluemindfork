/** @format */

goog.provide("net.bluemind.mvp.helper.URLHelper");

goog.require("goog.Uri");

/**
 * Helper to manage closure application URL.
 *
 * @param {net.bluemind.mvp.Router} router Application router.
 * @constructor
 *
 */
net.bluemind.mvp.helper.URLHelper = function(router) {
    this.router_ = router;
};

/**
 * @type {net.bluemind.mvp.Router}
 * @private
 */
net.bluemind.mvp.helper.URLHelper.prototype.router_;

/**
 * Reload current url
 *
 */
net.bluemind.mvp.helper.URLHelper.prototype.reload = function() {
    var uri = this.current();
    uri.getQueryData().set("refresh", goog.now());
    this.router_.setURL(uri);
};

/**
 * Replace part of the url but current params
 *
 * @param {string|goog.Uri} redirect Uri to redirect to.
 * @param {goog.Uri=} opt_current Current uri.
 */
net.bluemind.mvp.helper.URLHelper.prototype.merge_ = function(redirect, opt_current) {
    if (!opt_current) {
        opt_current = this.current();
    }
    if (goog.isString(redirect)) {
        redirect = new goog.Uri(redirect);
    }
    if (redirect.hasPath()) {
        opt_current.setPath(redirect.getPath());
    }
    if (redirect.hasQuery()) {
        var query = redirect.getQueryData();
        goog.array.forEach(query.getKeys(), function(key) {
            opt_current.getQueryData().setValues(key, query.getValues(key));
        });
    }
    return opt_current;
};

/**
 * Replace current url keeping current params / path if not overriden in the
 * given url. If opt_navigation is not set this will not fire a route change
 * event, so it might be used mainly in filters.
 *
 * @param {string|goog.Uri} redirect Uri to redirect to.
 */
net.bluemind.mvp.helper.URLHelper.prototype.redirect = function(redirect, opt_navigation) {
    var uri = this.merge_(redirect).toString();
    if (opt_navigation) {
        this.router_.setURL(uri);
    } else {
        this.router_.modifyURL(uri);
    }
};

/**
 * Go to given url
 *
 * @param {string|goog.Uri} uri Uri to go to
 * @param {string|Array.<string>=} opt_keep Optional list of params to keep
 */
net.bluemind.mvp.helper.URLHelper.prototype.goTo = function(uri, opt_keep) {
    if (goog.isString(uri)) {
        uri = new goog.Uri(uri);
    }
    uri.getQueryData().set("refresh", goog.now());
    if (goog.isDefAndNotNull(opt_keep)) {
        if (!goog.isArray(opt_keep)) {
            opt_keep = [opt_keep];
        }
        var current = this.current();
        current.getQueryData().filterKeys(opt_keep);
        uri = this.merge_(uri, current);
    }
    this.router_.setURL(uri);
};

/**
 * Current URI
 *
 * @return {goog.Uri} Current uri
 */
net.bluemind.mvp.helper.URLHelper.prototype.current = function() {
    return this.router_.getURL();
};

/**
 * Go to previous uri of history. You should activate HistoryFilter to use this
 * feature.
 *
 * @param {Array.<goog.Uri>=} opt_history History
 * @param {goog.Uri=} opt_fallback If no history then fallback url.
 */
net.bluemind.mvp.helper.URLHelper.prototype.back = function(opt_history, opt_fallback) {
    var fallback = opt_fallback || "/";
    if (opt_history) {
        if (opt_history.length > 1) {
            this.goTo(opt_history[opt_history.length - 2]);
        } else {
            this.goTo(fallback);
        }
    } else {
        goog.global.history.back();
    }
};
