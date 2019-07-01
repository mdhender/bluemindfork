/**
 * compose_newwindow - Compose(Reply/Forward) in a New Window
 *
 * @version 3.00 (20110822)
 * @author Karl McMurdo (user xrxca on roundcubeforum.net)
 * @url http://github.com/xrxca/cnw
 * @copyright (c) 2010-2011 Karl McMurdo
 *
 */ 

// The following updates the links in the ContextMenu Plugin
$(document).ready(function(){

  // If this is a child window and the parent is still around close it.
  if (self.window.name == 'rc_compose_child') {                         
    if (window.opener) {
      try {
        if(window.opener.rcmail) {
          var l = '' + window.location;
          window.opener.focus();
          if ( l.indexOf('&_refresh=1') == -1 ) {
            window.opener.location = l;
          }
          window.opener.rcmail.command('checkmail','');
        }
      } catch(err) {
      }
    } else {
      window.name = 'rc_new_parent';
    }
    window.close();
  }     

  if ( rcmail.contextmenu_command_handlers ) {
    rcmail.contextmenu_command_handlers['reply'] = function(){replynewwindow('context');};
    rcmail.contextmenu_command_handlers['reply-all'] = function(){replyallnewwindow('context');};
    rcmail.contextmenu_command_handlers['forward'] = function(){forwardnewwindow('context');};
    rcmail.contextmenu_command_handlers['forward-attachment'] = function(){forwardattachmentnewwindow('context');};
    rcmail.contextmenu_command_handlers['compose'] = function(){abookcomposenewwindow('context');};
  }
});
