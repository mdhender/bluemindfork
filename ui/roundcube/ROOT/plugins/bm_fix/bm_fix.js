
if (window.rcmail) {
  rcmail.addEventListener('init', function() {
    window.bluemind = window.bluemind || {};

    // Add some translated title
    $("#rcmstatus span").attr('title', rcmail.labels["unread"]);
    $("#rcmflag span").attr('title', rcmail.labels["flagged"]);
    if (rcmail.env.action == 'edit-folder' && rcmail.gui_objects.editform) {
      if (!rcmail.env.trash_mailbox) rcmail.env.trash_mailbox = 'Trash';
      if (!rcmail.env.junk_mailbox) rcmail.env.trash_mailbox = 'Junk';
      parent.rcmail.enable_command('purge', rcmail.purge_mailbox_test(parent.rcmail.env.mailbox));
    }
  });
  rcmail.addEventListener('responsebeforecheck-recent', function(evt) {
    if (rcmail.message_list) {
      bluemind.messageListScrollTop =  rcmail.message_list.frame.scrollTop;
    }
  });
  rcmail.addEventListener('responseaftercheck-recent', function(evt) {
    if (rcmail.message_list && bluemind.messageListScrollTop ) {
      rcmail.message_list.frame.scrollTop = bluemind.messageListScrollTop;
    }
     bluemind.messageListScrollTop = false;
  });
  rcmail.addEventListener('selectfolder', function(e) {
    var mailbox = e.folder;
    if (mailbox && (mailbox == rcmail.env.trash_mailbox || mailbox == rcmail.env.junk_mailbox
				|| mailbox.match('^(.*' + RegExp.escape(rcmail.env.delimiter) + ')?' + RegExp.escape(rcmail.env.trash_mailbox) + '(' + RegExp.escape(rcmail.env.delimiter) + '.*)?$')
				|| mailbox.match('^(.*' + RegExp.escape(rcmail.env.delimiter) + ')?' + RegExp.escape(rcmail.env.junk_mailbox) + '(' + RegExp.escape(rcmail.env.delimiter) + '.*)?$'))) {
    	rcmail.enable_command('purge', true);
    } else {
    	rcmail.enable_command('purge', false);
    }
  });
  rcmail.purge_mailbox_test = function(mailbox)
  {
    if (!mailbox) mailbox = rcmail.env.mailbox;
    return (rcmail.env.messagecount && (mailbox == rcmail.env.trash_mailbox || mailbox == rcmail.env.junk_mailbox
      || mailbox.match('^(.*' + RegExp.escape(rcmail.env.delimiter) + ')?' + RegExp.escape(rcmail.env.trash_mailbox) + '(' + RegExp.escape(rcmail.env.delimiter) + '.*)?$')
      || mailbox.match('^(.*' + RegExp.escape(rcmail.env.delimiter) + ')?' + RegExp.escape(rcmail.env.junk_mailbox) + '(' + RegExp.escape(rcmail.env.delimiter) + '.*)?$')));
  };

}

(function($) {

$.extend({
  throttle : function(fn, timeout) {
    var timer, args, invoke;
    return function() {
      args = arguments;
      invoke = true;
      timer || (function() {
        if(invoke) {
          fn.apply(this, args);
          invoke = false;
          timer = setTimeout(arguments.callee, timeout);
        } else {
          timer = null;
        }
      })();

    };

  }

});

})(jQuery);

