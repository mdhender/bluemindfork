goog.provide("net.bluemind.filehosting.ui.file.FileHostingPDFRenderer");

goog.require("net.bluemind.filehosting.ui.FileHostingFileRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingFileRenderer}
 */
net.bluemind.filehosting.ui.file.FileHostingPDFRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.filehosting.ui.file.FileHostingPDFRenderer,
    net.bluemind.filehosting.ui.FileHostingFileRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.file.FileHostingPDFRenderer);

/** @override */
net.bluemind.filehosting.ui.file.FileHostingPDFRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file-pdf-o') ];
