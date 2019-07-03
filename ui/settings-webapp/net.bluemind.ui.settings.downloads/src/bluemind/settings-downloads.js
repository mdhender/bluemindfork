goog.require('net.bluemind.ui.settings.downloads.template');
goog.require('net.bluemind.ui.settings.downloads.SettingsDownloadPartProvider');


net.bluemind.calendar.navigation.NavigationView.MSG_ADD_CALENDAR = goog.getMsg('Add a calendar...');

/** @meaning settings.downloads.title */
var MSG_SECTION_TITLE = goog.getMsg("Downloads");

/** @meaning settings.connectors */
var MSG_TAB_CONNECTORS = goog.getMsg("Connectors");

window["gwtSettingsDownloadsMenusContributor"] = function() {
	
	var menus = {
			'sections' : [ {
				"parentId" : null,
				"contribution" : {
					"id" : "downloads",
					"name" : MSG_SECTION_TITLE,
					"priority" : 3,
					'sections' : [],
					'screens' : []
				}
			} ],
			'screens' : []
		};

	var contribution = {
		'contribute' : function() { return menus;}
	};

	return contribution;

};

window["gwtSettingsDownloadsScreensContributor"] = function() {

	return {
		'contribute' : function() {
			return [ {
				"contributedElementId" : "root",
				"contributedAttribute" : "childrens",
				"contribution" : {
					"id" : "downloads",
					"type" : "bm.Tabs",
					"tabs" : [
						 {'type':'bm.Tab' , 'title':MSG_TAB_CONNECTORS
						   , 'content': {'type':'bm.settings.Downloads'}
						 }]
					
				}
			} ];
		}
	};
};
