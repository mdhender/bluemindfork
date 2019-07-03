goog.provide("net.bluemind.commons.ui.ApplicationViewHelper");

goog.require("goog.dom");

net.bluemind.commons.ui.ApplicationViewHelper = function() {
}

net.bluemind.commons.ui.ApplicationViewHelper.prototype.afterBootstrap = function(){
	goog.dom.setProperties(goog.dom.getElement('main'), {
        'style':'background-image: url(\'images/watermark.png\')'
    });
}
