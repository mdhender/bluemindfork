// Copyright 2012 YDN Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS-IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview Lean social gadget.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */


goog.provide('ydn.ui.Social');
goog.require('goog.soy');
goog.require('templ.ydn.ui');



/**
 * Create a lean social gadget.
 * @param {ydn.ui.Social.Type=} opt_intent Intent of the message. Default to
 * 'share this'.
 * @constructor
 * @struct
 */
ydn.ui.Social = function(opt_intent) {
  /**
   * @type {Array.<ydn.ui.Social.Data>}
   */
  this.data = [];
  /**
   * @final
   * @type {ydn.ui.Social.Type}
   */
  this.intent = opt_intent || ydn.ui.Social.Type.SHARE_THIS;
};


/**
 * @enum {number}
 */
ydn.ui.Social.Type = {
  SHARE_THIS: 0,
  FOLLOW_ME: 1
};


/**
 * @typedef {{
 *   name: string,
 *   href: string,
 *   content: string
 * }}
 */
ydn.ui.Social.Data;


/**
 * Set twitter.
 * {@link https://dev.twitter.com/docs/intents}
 * @param {string} url
 * @param {string} text Pre-prepared, properly UTF-8 & percent-encoded Tweet
 * body text.
 * @param {string} via A screen name to associate with the Tweet.
 * @param {string=} opt_hashtags Add context to the pre-prepared status update
 * by appending #hashtags. Omit the "#" symbol and separate multiple hashtags
 * with commas.
 */
ydn.ui.Social.prototype.twitter = function(url, text, via, opt_hashtags) {
  goog.asserts.assert(this.intent == ydn.ui.Social.Type.SHARE_THIS);
  var uri = new goog.Uri('https://twitter.com/intent/tweet');
  uri.setParameterValue('text', text);
  uri.setParameterValue('url', url);
  uri.setParameterValue('via', via);
  if (opt_hashtags) {
    uri.setParameterValue('hashtags', opt_hashtags);
  }
  this.data.push({
    name: 'twitter',
    content: 'Twitter',
    href: uri.toString()
  });
};


/**
 * Follow me on twitter.
 * @param {string} screen_name for example 'yathit'
 */
ydn.ui.Social.prototype.followTwitter = function(screen_name) {
  goog.asserts.assert(this.intent == ydn.ui.Social.Type.FOLLOW_ME);
  var uri = new goog.Uri('https://twitter.com/intent/user');
  uri.setParameterValue('screen_name', screen_name);
  this.data.push({
    name: 'twitter',
    content: 'Twitter',
    href: uri.toString()
  });
};


/**
 * Set Hacker News.
 * @param {string} u submit url
 * @param {string} t title
 */
ydn.ui.Social.prototype.hackerNews = function(u, t) {
  goog.asserts.assert(this.intent == ydn.ui.Social.Type.SHARE_THIS);
  var uri = new goog.Uri('http://news.ycombinator.com/submitlink');
  uri.setParameterValue('u', u);
  uri.setParameterValue('t', t);
  this.data.push({
    name: 'hackernews',
    content: 'HN',
    href: uri.toString()
  });
};


/**
 * Set google plus.
 * @link https://developers.google.com/+/web/share/
 * @param {string} url The URL of the page to share.
 * @param {string=} opt_hl The language code for the locale to use on the
 * Google+ sharing page.
 */
ydn.ui.Social.prototype.googlePlus = function(url, opt_hl) {
  // can use for both share and follow
  var uri = new goog.Uri('https://plus.google.com/share');
  uri.setParameterValue('url', url);
  if (opt_hl) {
    uri.setParameterValue('hl', opt_hl);
  }
  this.data.push({
    name: 'googleplus',
    content: 'Google+',
    href: uri.toString()
  });
};


/**
 * Set Reddit.
 * @link http://www.reddit.com/wiki/submitting
 * @param {string} url url to submit.
 */
ydn.ui.Social.prototype.reddit = function(url) {
  goog.asserts.assert(this.intent == ydn.ui.Social.Type.SHARE_THIS);
  var uri = new goog.Uri('http://www.reddit.com/' + url);
  this.data.push({
    name: 'reddit',
    content: 'Reddit',
    href: uri.toString()
  });
};


/**
 * @return {!Element}
 */
ydn.ui.Social.prototype.render = function() {
  var data = {
    data: this.data
  };
  if (this.intent == ydn.ui.Social.Type.FOLLOW_ME) {
    return goog.soy.renderAsElement(templ.ydn.ui.followMe, data);
  } else {
    return goog.soy.renderAsElement(templ.ydn.ui.social, data);
  }
};
