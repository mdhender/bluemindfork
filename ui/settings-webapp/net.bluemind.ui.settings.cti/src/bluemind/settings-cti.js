goog.require('net.bluemind.ui.settings.cti.CalendarCtiPartProvider');
goog.require('net.bluemind.ui.settings.cti.MainCtiPartProvider');

goog.global['gwtSettingsCTIScreensContributor'] = function() {
  
  return {'contribute': function() {
    return [{
      'contributedElementId' : 'userGenralContainer',
      'contributedAttribute' : 'childrens',
      'contribution' : {
        'type' : 'net.bluemind.ui.settings.cti.MainCtiPartProvider',
        'roles': ['hasCTI']
      }
    },
    {
      'contributedElementId' : 'calendarGeneralContainer',
      'contributedAttribute' : 'childrens',
      'contribution' : {
        'type' : 'net.bluemind.ui.settings.cti.CalendarCtiPartProvider',
          'role' : 'hasCTI'
      }
    }];  
  }};
  
};
