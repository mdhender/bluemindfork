/* Bluemind default icons */
if (window.rcmail) {
  rcmail.addEventListener('init', function(evt) {
  $('#mailboxlist a[rel="Outbox"]').parent().addClass('sent');

  $("#mailboxlist a").filter(function() {
              return this.rel.match(/^.*\/Outbox$/);
                  }).parent().addClass('sent');

  $("#mailboxlist a").filter(function() {
              return this.rel.match(/^.*\/Sent$/);
                  }).parent().addClass('sent');

  $("#mailboxlist a").filter(function() {
              return this.rel.match(/^.*\/Junk$/);
                  }).parent().addClass('junk');

  $("#mailboxlist a").filter(function() {
              return this.rel.match(/^.*\/Drafts$/);
                  }).parent().addClass('drafts');

  $("#mailboxlist a").filter(function() {
              return this.rel.match(/^.*\/Trash$/);
                  }).parent().addClass('trash');
   
  })
}
