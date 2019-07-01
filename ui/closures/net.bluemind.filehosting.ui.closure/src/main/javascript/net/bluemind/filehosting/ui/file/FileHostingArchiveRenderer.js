goog.provide("net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer");

goog.require("net.bluemind.filehosting.ui.FileHostingFileRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingFileRenderer}
 */
net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer,
    net.bluemind.filehosting.ui.FileHostingFileRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer);

/** @override */
net.bluemind.filehosting.ui.file.FileHostingArchiveRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file-archive-o') ];
