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
 * @fileoverview Utilities useful in testing.
 *
 * @author kyawtun@yathit.com (Kyaw Tun)
 */

goog.provide('ydn.testing');
goog.require('goog.log');
goog.require('goog.debug.Console');
goog.require('goog.debug.DebugWindow');
goog.require('goog.debug.DivConsole');
goog.require('goog.debug.FancyWindow');
goog.require('goog.log');
goog.require('goog.debug.Logger.Level');
goog.require('goog.math');


/**
 * Normal deviate function.
 * @see #randomNormal
 * @return {number} return a normally distributed pseudorandom numbers.
 */
ydn.testing.randn = function() {
  // base on jstat library
  var u, v, x, y, q, mat;

  do {
    u = Math.random();
    v = 1.7156 * (Math.random() - 0.5);
    x = u - 0.449871;
    y = Math.abs(v) + 0.386595;
    q = x * x + y * (0.19600 * y - 0.25472 * x);
  } while (q > 0.27597 && (q > 0.27846 || v * v > -4 * Math.log(u) * u * u));
  return v / u;
};


/**
 * Generate normally distributed random around given std and mean
 *
 * @param {number=} opt_mean mean value, default to 0.
 * @param {number=} opt_stdev standard deviation, default to 1.
 * @return {number} return a normally distributed pseudorandom numbers of given
 * standard deviation and mean.
 */
ydn.testing.randomNormal = function(opt_mean, opt_stdev) {
  var x = ydn.testing.randn();
  if (goog.isDef(opt_mean) || goog.isDef(opt_stdev)) {
    if (!goog.isDef(opt_mean)) {
      opt_mean = 0;
    }
    if (!goog.isDef(opt_stdev)) {
      opt_stdev = 1;
    }
    x = x * opt_stdev + opt_mean;
  }
  return x;
};


/**
 * Generate random string
 *
 * @param {number} len require length.
 * @param {string=} opt_charSet character set to be used.
 * @return {string} return random string.
 */
ydn.testing.randString = function(len, opt_charSet) {
  opt_charSet = opt_charSet ||
      'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
  var randomString = '';
  for (var i = 0; i < len; i++) {
    //var randomPoz = Math.floor(Math.random() * opt_charSet.length);
    // same as above, but much faster.
    var randomPoz = (Math.random() * opt_charSet.length) | 0;
    randomString += opt_charSet.substring(randomPoz, randomPoz + 1);
  }
  return randomString;
};


/**
 * Generate random id
 *
 * @param {number=} opt_len require length.
 * @return {string} return random id.
 */
ydn.testing.randId = function(opt_len) {
  opt_len = opt_len || 17;
  return ydn.testing.randString(opt_len, 'abcde0123456789');
};


/**
 * Generate random digit string
 *
 * @param {number=} opt_len require length.
 * @return {string} return a random digit string.
 */
ydn.testing.randDigits = function(opt_len) {
  opt_len = opt_len || 8;
  return ydn.testing.randString(opt_len, '0123456789');
};


/**
 * Generate random word with all small letter
 *
 * @param {number=} opt_mean word length mean.
 * @param {number=} opt_max word length maximun.
 * @return {string} random word.
 */
ydn.testing.randWord = function(opt_mean, opt_max) {
  opt_mean = opt_mean || 5;
  opt_max = opt_max || opt_mean * 3;
  var len = goog.math.clamp(ydn.testing.randomNormal(opt_mean, opt_mean - 1), 1,
      opt_max);
  return ydn.testing.randString(len, 'abcdefghijklmnopqrstuvwxyz');
};

/**
 * Returns a random integer greater than or equal to 0 and less than {@code a}.
 * @param {number} a  The upper bound for the random integer (exclusive).
 * @return {number} A random integer N such that 0 <= N < a.
 */
ydn.testing.randomInt = function(a) {
  return (Math.random() * a) | 0;
};


/**
 * Generate random sentence without full stop.
 * @param {Array.<string>=} keywords optional keywords to include.
 */
ydn.testing.randSentence = function(keywords) {

  var s = [];
  var char_set1 = 'zanatpungigorbagt  rnemnedek   mcdeefghijkllnpqrsuvz';
  var char_set2 = 'anotiuniieoneae hIAe   oa d nfneoiarioiroiei iueonoi';
  var nw = (Math.random() * 20) + 1;
  var n = char_set1.length;
  for (var i = 0; i < nw; i++) {
    var idx = (Math.random() * n) | 0;
    s[i] = char_set1.charAt(idx) + char_set2.charAt(idx);
  }
  if (s[0]) {
    s[0] = s[0].charAt(0).toUpperCase() + s[0].charAt(1);
  }

  var st = s.join('').trim();
  if (keywords) {
    var pos = st.lastIndexOf(' ');
    var cnt = 0;
    while (pos > 0 && cnt < 2) {
      var ky = keywords[ydn.testing.randomInt(keywords.length)];
      st = st.substr(0, pos) + ' ' + ky + st.substr(pos);
      pos = st.lastIndexOf(' ', pos - 1);
      cnt++;
    }
  }
  return st;
};


/**
 * Generate random paragraph.
 * @param {Array.<string>=} keywords optional keywords to include.
 * @return {string}
 */
ydn.testing.randParagraph = function(keywords) {
  var out = [];
  var ns = Math.random() * 15 + 1;
  for (var i = 0; i < ns; i++) {
    var w = [];
    var wn = Math.random() * 20 + 1;
    for (var j = 0; j < wn; j++) {
      w[j] = ydn.testing.randSentence(keywords);
    }
    var r = Math.random();
    var tr = r < 0.01 ? '!' : r < 0.05 ? '?' : '.';
    out[i] = w.join('').trim() + tr;
  }
  return out.join(' ');
};


/**
 * Generate random name
 *
 * @return {string} return a random name.
 */
ydn.testing.randName = function() {
  return ydn.testing.randString(1, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') +
      ydn.testing.randWord(3);
};


/**
 * Generate a random email
 *
 * @return {string} return a random email.
 */
ydn.testing.randEmail = function() {
  var id = ydn.testing.randWord();
  var dn = ydn.testing.randString((Math.random() * 7 + 1) | 0, 'gmail');
  return ydn.testing.randWord() + '@' + dn + '.com';
};


