if (window.rcmail) {

  rcmail.addEventListener('init', function(evt) {

    rcmail.message_list.addEventListener('select', function(e) {
      var selected = e.get_selection();
      if (selected && selected.length == 1) {
        selected = selected[0];
        var msg = rcmail.env.messages[selected];
        if (msg && msg.flags && msg.flags.mbox) {
          var mbox = msg.flags.mbox;
          rcmail.select_folder(mbox, '', true);
          rcmail.env.mailbox = mbox;
        }
      } else if (selected.length > 0) {
        var mbox;
        for(var i = 0; i < selected.length; i++) {
          var msg = rcmail.env.messages[selected[i]];

          if (!msg || !msg.flags || !msg.flags.mbox) {
            return;
          }
          if (mbox && mbox != msg.flags.mbox) {
            mbox = null;
            break;
          }
          mbox = msg.flags.mbox;
        }
        rcmail.env.mailbox = mbox;
        rcmail.select_folder(mbox, '', true);
      }
    });

    $('#searchmenulink').remove();
    
    var input = $('#quicksearchbox');
    var form = input.parent();
    input.hide();

    bluemind.model.search.SearchTerm.DEFAULT_FORMAT = '<strong>%0</strong> <i>' + this.get_label('bm_search.contains') + '</i> "<span>%1</span>"';

    var f = '<strong>' + this.get_label('bm_search.to_desc') + '</strong> <i>' + this.get_label('bm_search.is') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('to', this.get_label('bm_search.to'), f));
    f = '"<span>%1</span> " <i>' + this.get_label('bm_search.is') + '</i> <strong>' + this.get_label('bm_search.cc_desc') + '</strong>';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('cc', this.get_label('bm_search.cc'), f));
    f = '<strong>' + this.get_label('bm_search.flag_desc') + '</strong> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('is', this.get_label('bm_search.flag'), f));

    var terms = [];
    f = '<strong>' + this.get_label('bm_search.content_desc') + '</strong> <i>' + this.get_label('bm_search.contains') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('content', this.get_label('bm_search.content'), f), true));
    f = '<strong>' + this.get_label('bm_search.all_desc') + '</strong> <i>' + this.get_label('bm_search.contains') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('_all', this.get_label('bm_search.all'), f)));
    f = '<strong>' + this.get_label('bm_search.subject') + '</strong> <i>' + this.get_label('bm_search.contains') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('subject', this.get_label('bm_search.subject'), f)));
    f = '<strong>' + this.get_label('bm_search.with_desc') + '</strong> <i>' + this.get_label('bm_search.by') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('with', this.get_label('bm_search.with'), f)));
    f = '<strong>' + this.get_label('bm_search.from_desc') + '</strong> <i>' + this.get_label('bm_search.by') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('from', this.get_label('bm_search.from'), f)));
    f = '<strong>' + this.get_label('bm_search.filename_desc') + '</strong> <i>' + this.get_label('bm_search.contains') + '</i> "<span>%1</span>"';
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('filename', this.get_label('bm_search.filename'), f)));

    var field = new bluemind.ui.SearchField(terms);
    
    terms = [];
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('in:$current', this.get_label('bm_search.currentFolder'))));
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('mailbox:$current', this.get_label('bm_search.myFolders'))));
    terms.push(bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('mailbox:$all', this.get_label('bm_search.allFolders'))));
    var s = terms[0];
    if (input.val() != '') { 
      var parts = input.val().split(';');
      var val = [];
      for(var i = 0; i < parts.length; i++) {
        var part = parts[i];
        if (i == 0) {
          if (part == 'in:$current') {
	    s = terms[0];
          } else if (part == 'mailbox:$current') {
            s = terms[1];
          } else if (part == 'mailbox:$all') {
            s = terms[2];
          }
        } else {
          val.push(parts[i].replace(/:\(([^)]*)\)/, ':$1'));
        } 
      }
	
    }



    var folder = new bluemind.ui.CartoucheBoxSearchItem(new bluemind.model.search.QueryPart('', s), terms);
    folder.setReadOnly(true);
    field.addValue(folder);
    f = '<strong>' + this.get_label('bm_search.date_desc') + '</strong> <i>' + this.get_label('bm_search.is') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('date', this.get_label('bm_search.date'), f));
    f = '<strong>' + this.get_label('bm_search.owner_id_desc') + '</strong> <i>' + this.get_label('bm_search.is') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('folders', this.get_label('bm_search.owner_id'), f));    
    f = '<strong>' + this.get_label('bm_search.uid_desc') + '</strong> <i>' + this.get_label('bm_search.is') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('uid', this.get_label('bm_search.uid'), f));    
    f = '<strong>' + this.get_label('bm_search.size_desc') + '</strong> <i>' + this.get_label('bm_search.higher') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('size', this.get_label('bm_search.size'), f));    
    f = '<strong>' + this.get_label('bm_search.in_desc') + '</strong> <i>' + this.get_label('bm_search.is') + '</i> "<span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('in', this.get_label('bm_search.in'), f));
    f = '<strong>' + this.get_label('bm_search.has_desc') + '</strong> <i> <span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('has', this.get_label('bm_search.has'), f));
    f = '<strong>' + this.get_label('bm_search.raw_desc') + '</strong> : <span>%1</span>"';
    bluemind.model.search.SearchTermFactory.getInstance().store(new bluemind.model.search.SearchTerm('raw', this.get_label('bm_search.raw'), f));
    
    field.render(form[0]);
    if (val) {
      for (var i = 0; i< val.length; i++) {
        field.addValue(val[i]);
      }
    } 
    input.val(field.getValue().join(' '));
    input.data('old', input.val());
    
    input.change(function(e) {
      field.lock = true;
      if (input.data('old') != input.val()) {
        if (input.val() == '') {
          field.reset();
        } else {
          //Should not happened.
        }
      }
      field.lock = false;
    });

    var search = $.throttle(function() {
      input.parent().submit();
    }, 1000);

    var changed = function(e) {
      var val = field.getValue();
      if (val.length > 1) {
        for(i = 1; i<val.length; i++) {
            // remove Elasticsearch special characters (except *)
            var criterion = val[i].replace(/.*:\(([^)]*)\)/, '$1');
            criterion = criterion.replace(/[\+\=\&\|\!\(\)\{\}\[\]\^\"\~\<\>\?\:\\\/]/g, '');
            val[i] = val[i].replace(/:\(([^)]*)\)/, ":(" + criterion + ")");
          }
          input.val(val.join(';'));
        if (input.data('old') != input.val()) {
          if (!field.lock) search();
        }
      } else if (val[0] == 'in:$current') {
        if (!field.lock) rcmail.command('reset-search','',$('#searchreset')[0]);
      }
      input.data('old', input.val());
    }

    field.addEventListener('change', changed);
    $('#searchreset').before('<div id="searchhelp"><a class="" title="Aide sur la recherche" href="#" onclick="">?</a></div>');
    var dialog = $(this.get_label('bm_search.help_content')).dialog({
      autoOpen: false,
      modal: true,
      resizable: false,
      closeOnEscape: true,
      title: this.get_label('bm_search.help_title'),
      width: 650
    });
    $('#searchhelp').click(function() {dialog.dialog('open')}); 
  }, rcmail);
}
