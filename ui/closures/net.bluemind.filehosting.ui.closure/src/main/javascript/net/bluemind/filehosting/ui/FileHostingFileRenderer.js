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
goog.provide("net.bluemind.filehosting.ui.FileHostingFileRenderer");
goog.provide("net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry");

goog.require("goog.string.path");
goog.require("net.bluemind.filehosting.ui.FileHostingItemRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingItemRenderer}
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer = function() {
  goog.base(this);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingFileRenderer, net.bluemind.filehosting.ui.FileHostingItemRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.FileHostingFileRenderer);

/** @override */
net.bluemind.filehosting.ui.FileHostingFileRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file-o') ];

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.CSS_CLASS = goog.getCssName('filehosting-file');

/** @override */
net.bluemind.filehosting.ui.FileHostingFileRenderer.prototype.getCssClass = function() {
  return net.bluemind.filehosting.ui.FileHostingFileRenderer.CSS_CLASS;
};

/**
 * Map of file extension to registry factory functions. The keys are file
 * extension. The values are function objects that return new instances of
 * {@link net.bluemind.filehosting.ui.FileHostingFileRenderer.registry} or one
 * of its subclasses.
 * 
 * @constructor
 * 
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry = function() {
};

/**
 * Returns an instance of default renderer.
 * 
 * @return {net.bluemind.filehosting.ui.FileHostingFileRenderer}
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDefaultRenderer = function() {
  return net.bluemind.filehosting.ui.FileHostingFileRenderer.getInstance()
};

/**
 * Returns the the decorator registered for the given extension or null
 * 
 * @param {string} ext extension name.
 * @return {net.bluemind.filehosting.ui.FileHostingFileRenderer?} Decorator
 *         instance.
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDecoratorByExtension = function(ext) {
  if (ext in net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.decorators_) {
    return net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.decorators_[ext];
  } else {
    return null;
  }
};

/**
 * Maps a extension name to a instance of
 * {@link net.bluemind.filehosting.ui.FileHostingFileRenderer} or a subclass,
 * suitable to decorate an element that has the specified extension.
 * 
 * @param {string} ext extension name.
 * @param {net.bluemind.filehosting.ui.FileHostingFileRenderer} decorator
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension = function(ext, decorator) {
  net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.decorators_[ext] = decorator;
};

/**
 * Returns an instance of decorator or default one based on an extension name.
 * 
 * @param {string} filename File name.
 * @return {net.bluemind.filehosting.ui.FileHostingFileRenderer?}
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDecorator = function(filename) {
  var ext = goog.string.path.extension(filename).toLocaleLowerCase();
  var decorator = net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDecoratorByExtension(ext);
  return decorator || net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDefaultRenderer();
};

/**
 * Resets the global decorator registry.
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.reset = function() {
  net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.decorators_ = {};
};

/**
 * Map of extension names to decorator instance. The keys are extension name
 * names.
 * 
 * @type {Object}
 * @private
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.decorators_ = {};
