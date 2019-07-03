goog.provide("net.bluemind.filehosting.ui.file.FileHostingImageRenderer");

goog.require("net.bluemind.filehosting.ui.FileHostingFileRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingFileRenderer}
 */
net.bluemind.filehosting.ui.file.FileHostingImageRenderer = function() {
  goog.base(this);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.filehosting.ui.file.FileHostingImageRenderer,
    net.bluemind.filehosting.ui.FileHostingFileRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.file.FileHostingImageRenderer);

/** @override */
net.bluemind.filehosting.ui.file.FileHostingImageRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file-image-o') ];
