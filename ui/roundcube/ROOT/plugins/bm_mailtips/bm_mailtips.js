rcmail.addEventListener('init', function(evt) {
  rcmail.bm_mailtips = {}
  var id;
  id = setInterval(function() {
    if (rcmail.env.identity) {
      rcmail.bm_mailtips_get_mailtips();
      clearInterval(id);
    }
  }, 300);
  rcmail.addEventListener('autocomplete_insert', rcmail.bm_mailtips_get_mailtips);   
  $("[name='_to'], [name='_cc'], [name='_bcc'], [name='_subject']").blur(rcmail.bm_mailtips_get_mailtips);
  $("[name='_from']").change(rcmail.bm_mailtips_get_mailtips);
});

rcube_webmail.prototype.bm_mailtips_get_mailtips = function() {
  var to = $("[name='_to']").val(), _to = [],
      cc = $("[name='_cc']").val(), _cc = [],
      bcc = $("[name='_bcc']").val(), _bcc = [],
      input_from = $("[name='_from']"),
      input_subject = $("[name='_subject']");

  to.replace(/[\s,;]+$/, '').split(',').forEach(function(rcpt) {
    rcpt = rcpt.trim();
    if (rcube_check_email(rcpt, true)) {
      _to.push(rcpt);
    }
  });
  cc.replace(/[\s,;]+$/, '').split(',').forEach(function(rcpt) {
    rcpt = rcpt.trim();
    if (rcube_check_email(rcpt, true)) {
      _cc.push(rcpt);
    }
  });
  bcc.replace(/[\s,;]+$/, '').split(',').forEach(function(rcpt) {
    rcpt = rcpt.trim();
    if (rcube_check_email(rcpt, true)) {
      _bcc.push(rcpt);
    }
  });  
  if ((_to.length + _cc.length + _bcc.length) == 0) {
    return false;
  }
  var query = '_from=' + encodeURIComponent(input_from.val()) + '&_subject=' + encodeURIComponent(input_subject.val()) + '&_to=' + encodeURIComponent(_to.join(', ')) + '&_cc=' + encodeURIComponent(_cc.join(', ') + '&_bcc=' + encodeURIComponent(_bcc.join(', '));
  if (query != rcmail.bm_mailtips.query) {
    rcmail.bm_mailtips.query = query;
    rcmail.http_post('plugin.bm_mailtips.getmailtips', query);
  }
};
