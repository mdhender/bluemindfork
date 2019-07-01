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

goog.provide("net.bluemind.filehosting.ui.FileHostingFile");

goog.require("net.bluemind.filehosting.ui.FileHostingItem");
goog.require("net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry");
goog.require("net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer");
goog.require("net.bluemind.filehosting.ui.file.FileHostingImageRenderer");
goog.require("net.bluemind.filehosting.ui.file.FileHostingPDFRenderer");
goog.require("net.bluemind.filehosting.ui.file.FileHostingVideoRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent=} opt_content
 * @param {string=} opt_path
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.filehosting.ui.FileHostingItem}
 */
net.bluemind.filehosting.ui.FileHostingFile = function(opt_content, opt_path, opt_renderer, opt_domHelper) {
  var renderer = opt_renderer;
  if (opt_path && !renderer) {
    renderer = net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.getDecorator(opt_path);
  }
  goog.base(this, opt_content, opt_path, renderer, opt_domHelper);
  this.setSupportedState(goog.ui.Component.State.SELECTED, true);
};
goog.inherits(net.bluemind.filehosting.ui.FileHostingFile, net.bluemind.filehosting.ui.FileHostingItem);

/*
 * Register this control so it can be created from markup.
 */
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('jpg',
    net.bluemind.filehosting.ui.file.FileHostingImageRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('jpeg',
    net.bluemind.filehosting.ui.file.FileHostingImageRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('png',
    net.bluemind.filehosting.ui.file.FileHostingImageRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('gif',
    net.bluemind.filehosting.ui.file.FileHostingImageRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('zip',
    net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('tar',
    net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('rar',
    net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('gz',
    net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('bz2',
    net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('pdf',
    net.bluemind.filehosting.ui.file.FileHostingPDFRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('avi',
    net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('mkv',
    net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('mp4',
    net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('mov',
    net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.getInstance());
net.bluemind.filehosting.ui.FileHostingFileRenderer.Registry.setDecoratorByExtension('wmv',
    net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.getInstance());