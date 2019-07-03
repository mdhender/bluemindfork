// callback for app-onload event
if (window.rcmail) {
  rcmail.addEventListener('init', function(evt) {
    var identities = $('#_from');
    var sent = $('select[name=_store_target]');
    if (sent.size() > 0 && identities.prop('tagName').toLowerCase() == 'select') {
      identities.bind('change', function(e) {
        var id = $(e.target).val();
        if (rcmail.env.sent_mbox) {
          if (rcmail.env.sent_mbox[id]) {
            sent.val(rcmail.env.sent_mbox[id]);
          } else {
            sent.val(rcmail.env.sent_mbox['default']);
          }
        }
      });
    }
  });
}

