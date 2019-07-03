goog.provide("net.bluemind.filehosting.ui.file.FileHostingVideoRenderer");

goog.require("net.bluemind.filehosting.ui.FileHostingFileRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingFileRenderer}
 */
net.bluemind.filehosting.ui.file.FileHostingVideoRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.filehosting.ui.file.FileHostingVideoRenderer,
    net.bluemind.filehosting.ui.FileHostingFileRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.file.FileHostingVideoRenderer);

/** @override */
net.bluemind.filehosting.ui.file.FileHostingVideoRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file-video-o') ];
