goog.provide('net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider');
goog.require('goog.soy');
goog.require('goog.dom');
goog.require('bm.extensions.ExtensionsManager');
goog.require('bm.extensions.ExtensionPoint');
goog.require('bm.extensions.Extension');
goog.require('net.bluemind.ui.settings.downloads.template');
/**
 * @constructor
 * @export
 */
net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider = function(model) {
  var ep = bm.extensions.ExtensionsManager.getInstance().getExtensionPoint(
      "net.bluemind.ui.settings.downloads");
  
  if( model) {
	  this['id'] = model['id'];
	  this['type'] = model['type'];
  }
  
  var roles = goog.global['bmcSessionInfos']['roles'].split(',');
  this.exts_ = goog.array.filter(goog.array.map(ep.getExtensions(), function(e) {
	var data = e.data("download");
	if (data['role'] == null || goog.array.contains(roles, data['role'])){
	  return data;	
	}
  }), function(e) {
	 return e != null; 
  });
}

/**
 * @export
 */
net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.attach = function(
    parent) {
  var el = goog.soy.renderAsElement(
      net.bluemind.ui.settings.downloads.template.downloads, {
        extensions : this.exts_,
        section : "downloads"
      });
  parent.appendChild(el);
}

/**
 * @export
 */
net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.detach = function() {

}

/**
 * @export
 */
net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.loadModel = function(
    model) {
}

/**
 * @export
 */
net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.saveModel = function(
    model) {
}

goog.exportSymbol(
    'bm.settings.Downloads',
    net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider);
goog
    .exportProperty(
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype,'attach',
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.attach);
goog
    .exportProperty(
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype,'detach',
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.detach);
goog
    .exportProperty(
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype,'loadModel',
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.loadModel);
goog
    .exportProperty(
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype,'saveModel',
        net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider.prototype.saveModel);
