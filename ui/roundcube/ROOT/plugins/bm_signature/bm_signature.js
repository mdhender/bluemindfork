rcmail.addEventListener('init', function(evt) {
  rcmail.addEventListener('plugin.bm_signature', rcmail.bm_signature_enable, rcmail); 
  rcmail.bm_signature = {
    signatures : {}
  }
  for(id in rcmail.env.signatures)
    rcmail.bm_signature.signatures[id] = rcmail.env.signatures[id];

  var exp = new Date();
  exp.setYear(exp.getFullYear() + 1);

  $('#signaturepreviewtoggle').parent().click(function() {
    var show = !$('#signaturepreview').is(':visible');
    rcmail.bm_signature_show_preview(show);
    bw.set_cookie('bm_signature_preview', show ? 'yes': 'no', exp);
  });
  $('#signaturepreviewtoggle').parent().css('cursor', 'pointer');
});


rcube_webmail.prototype.bm_signature_enable = function(args) {
  var identity = args['from'], enable = args['enable'], value = args['value'], uid = args['uid'], placeholder = args['placeholder'];
  if (enable) {
    var signature = this.env.signatures[identity];
  } else if (placeholder) {
    var label = this.get_label('bm_signature.signature_placeholder');
    var signature = {html: "--X-BM-SIGNATURE--", text:"--X-BM-SIGNATURE--"};
  } else {
    var signature = false;
  }
  if (identity == this.env.identity && this.env.signatures[identity] != signature) {
    if (!enable) {
      this.change_identity({
        selectedIndex: 0,
        options: [{value:'x-bm-signature-disabled'}]
      });
      this.env.signatures[identity] = signature;
      this.display_message(this.get_label('bm_signature.corporate_signature'), 'warning');
    } else {
      this.env.signatures[identity] = this.bm_signature.signatures[identity];
      this.display_message(this.get_label('bm_signature.no_corporate_signature'));
    }

    this.change_identity($("[name='_from']")[0]);
  }
  if (!value || value.html.trim() == '') {
    $('#signaturepreviewtoggle').parent().hide();
    this.bm_signature_show_preview(false);
    $('#signaturepreview').html('');
  } else {
    $('#signaturepreviewtoggle').parent().show();
    this.bm_signature_show_preview();
    $('#signaturepreview').html(value.html);
  }
}

rcube_webmail.prototype.bm_signature_show_preview = function(show) {
  if (show === undefined) {
    show = bw.get_cookie('bm_signature_preview') !== 'no';
  }
  if (show) {
    $('#signaturepreviewtoggle').removeClass('fa fa-caret-square-o-up');
    $('#signaturepreviewtoggle').addClass('fa fa-caret-square-o-down');
    $('#signaturepreview').show();
  } else {
    $('#signaturepreviewtoggle').removeClass('fa fa-caret-square-o-down');
    $('#signaturepreviewtoggle').addClass('fa fa-caret-square-o-up');
    $('#signaturepreview').hide();

  }
};
