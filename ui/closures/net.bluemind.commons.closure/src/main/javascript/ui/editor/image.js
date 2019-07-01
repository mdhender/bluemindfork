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
 * @fileoverview A utility class for managing editable images.
 */

goog.provide('bluemind.ui.editor.Image');

goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.NodeType');
goog.require('goog.dom.Range');
goog.require('goog.dom.TagName');
goog.require('goog.editor.BrowserFeature');
goog.require('goog.editor.Command');
goog.require('goog.editor.node');
goog.require('goog.editor.range');
goog.require('goog.fs.FileReader');
goog.require('goog.net.IframeIo');
goog.require('goog.string');
goog.require('goog.string.Unicode');
goog.require('goog.uri.utils');
goog.require('goog.userAgent');
goog.require('goog.userAgent.product');
goog.require('goog.userAgent.product.isVersion');
goog.require('goog.events.EventTarget');
goog.require('goog.events.EventType');



/**
 * Wrap an editable image.
 * @param {goog.dom.DomHelper} domHelper The dom helper to be used to
 *     create the image.
 * @param {HTMLImageElement=} opt_img The img element.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
bluemind.ui.editor.Image = function(domHelper, opt_img) {
  goog.base(this);
  this.image_ = opt_img || /** @type {HTMLImageElement} */ (domHelper.createDom(goog.dom.TagName.IMG));

  this.isNew_ = !opt_img;

};
goog.inherits(bluemind.ui.editor.Image, goog.events.EventTarget);

/**
 * The link DOM element.
 * @type {HTMLImageElement}
 * @private
 */
bluemind.ui.editor.Image.prototype.image_;

/**
 * Whether this image represents a link just added to the document.
 * @type {boolean}
 * @private
 */
bluemind.ui.editor.Image.prototype.isNew_;

/**
 * Loading image data url
 * @type {string}
 */
bluemind.ui.editor.Image.LOADING = 'data:image/gif;base64,R0lGODlhEAAQAPIAAP///wAAAMLCwkJCQgAAAGJiYoKCgpKSkiH+GkNyZWF0ZWQgd2l0aCBhamF4bG9hZC5pbmZvACH5BAAKAAAAIf8LTkVUU0NBUEUyLjADAQAAACwAAAAAEAAQAAADMwi63P4wyklrE2MIOggZnAdOmGYJRbExwroUmcG2LmDEwnHQLVsYOd2mBzkYDAdKa+dIAAAh+QQACgABACwAAAAAEAAQAAADNAi63P5OjCEgG4QMu7DmikRxQlFUYDEZIGBMRVsaqHwctXXf7WEYB4Ag1xjihkMZsiUkKhIAIfkEAAoAAgAsAAAAABAAEAAAAzYIujIjK8pByJDMlFYvBoVjHA70GU7xSUJhmKtwHPAKzLO9HMaoKwJZ7Rf8AYPDDzKpZBqfvwQAIfkEAAoAAwAsAAAAABAAEAAAAzMIumIlK8oyhpHsnFZfhYumCYUhDAQxRIdhHBGqRoKw0R8DYlJd8z0fMDgsGo/IpHI5TAAAIfkEAAoABAAsAAAAABAAEAAAAzIIunInK0rnZBTwGPNMgQwmdsNgXGJUlIWEuR5oWUIpz8pAEAMe6TwfwyYsGo/IpFKSAAAh+QQACgAFACwAAAAAEAAQAAADMwi6IMKQORfjdOe82p4wGccc4CEuQradylesojEMBgsUc2G7sDX3lQGBMLAJibufbSlKAAAh+QQACgAGACwAAAAAEAAQAAADMgi63P7wCRHZnFVdmgHu2nFwlWCI3WGc3TSWhUFGxTAUkGCbtgENBMJAEJsxgMLWzpEAACH5BAAKAAcALAAAAAAQABAAAAMyCLrc/jDKSatlQtScKdceCAjDII7HcQ4EMTCpyrCuUBjCYRgHVtqlAiB1YhiCnlsRkAAAOwAAAAAAAAAAAA==';
/**
 * @return {HTMLImageElement} The image element.
 */
bluemind.ui.editor.Image.prototype.getImage = function() {
  return this.image_;
};

/**
 * @param {HTMLImageElement} img The image element.
 */
bluemind.ui.editor.Image.prototype.setImage = function(img) {
  this.image_ = img;
};


/**
 * @return {boolean} Whether the link is new.
 */
bluemind.ui.editor.Image.prototype.isNew = function() {
  return this.isNew_;
};


/**
 * Set the src without affecting the isNew() status of the image.
 * @param {string} url A URL.
 */
bluemind.ui.editor.Image.prototype.initializeSrc = function(url) {
  this.getImage().src = url;
};


/**
 * Removes the image.  Note that this
 * object will no longer be usable/useful after this call.
 */
bluemind.ui.editor.Image.prototype.removeImage = function() {
  goog.dom.removeNode(this.image_);
  this.image_ = null;
};


/**
 * Set loading state.
 * @param {boolean} state Loading state.
 */
bluemind.ui.editor.Image.prototype.loading = function(state) {
  this.setSrc(bluemind.ui.editor.Image.LOADING);
};

/**
 * Change the image.
 * @param {string} src A new image source.
 */
bluemind.ui.editor.Image.prototype.setSrc = function(src) {
  var image = this.getImage();
  image.src = src;
  this.dispatchEvent(goog.events.EventType.CHANGE);
};


/**
 * Places the cursor to the right of the image.
 * Note that this is different from goog.editor.range's placeCursorNextTo
 * in that it specifically handles the placement of a cursor in browsers
 * that trap you in links, by adding a space when necessary and placing the
 * cursor after that space.
 */
bluemind.ui.editor.Image.prototype.placeCursorRightOf = function() {
  var image = this.getImage();
  if (image.parentNode) {
    goog.editor.range.placeCursorNextTo(image, false);
  }
};


/**
 * Initialize a new image.
 * @param {goog.dom.DomHelper} domHelper The dom helper to be used to
 *     create the image.
 * @param {string} url The initial URL.
 * @return {bluemind.ui.editor.Image} The link.
 */
bluemind.ui.editor.Image.createNewImage = function(domHelper, url) {
  var img = new bluemind.ui.editor.Image(domHelper);
  img.initializeSrc(url);

  return img;
};

/**
 * Determines whether or not a url is an inline image code.
 * @param {string} url A url.
 * @return {boolean} Whether the url is an inline image.
 */
  bluemind.ui.editor.Image.isInline = function(url) {
  return !!url && goog.string.startsWith(url, 'data:');
};


/**
 * Determines whether or not a url is an email link.
 * @param {string} url A url.
 * @return {boolean} Whether the url is a mailto link.
 */
bluemind.ui.editor.Image.isInternal = function(url) {
  return !!url && (goog.uri.utils.getDomain(url) == null) && goog.string.contains(url, 'fetchDocument');
};


/**
 * Transform an image into a data-url string.
 * @param {HTMLInputElement} input The file input.
 * @param {?string} opt_fallback Fallback url used when browser does not support
 *   the File API.
 * @return {goog.async.Deferred} The callback parameter is the data-url.
 */
bluemind.ui.editor.Image.toDataURL = function(input, opt_fallback) {
  if (goog.userAgent.product.IE && !goog.userAgent.product.isVersion('10')) {
    var ret = new goog.async.Deferred();
    if (opt_fallback) {
      var form = input.form;
      var uri = opt_fallback;
      var io = new goog.net.IframeIo();
      goog.events.listenOnce(io, goog.net.EventType.SUCCESS, function(e) {
        var io = e.target;
        ret.callback(goog.string.trim(io.getResponseText()));
      });
      io.sendFromForm(form, uri);
    }
    return ret;
  } else {
    return goog.fs.FileReader.readAsDataUrl(input.files[0]);
  }
};

/**
 * Check if the URL is bigger than 10ko
 * @param {string} url image url.
 * @return {boolean} false if the string is bigger than 10ko
 */
bluemind.ui.editor.Image.checkUrlSize = function(url) {
  if (url.length > 1024 * 1024 * 10) {
    return false;
  }
  return true;
};
