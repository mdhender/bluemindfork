function subscribe(folderId) {
  rcmail.http_post('plugin.bm_foldersharing.subscribe', '_folderId='+folderId);
  $('#fs-toolbar-yes').addClass('highlight');
  $('#fs-toolbar-no').removeClass('highlight');
}

function unsubscribe(folderId) {
  rcmail.http_post('plugin.bm_foldersharing.unsubscribe', '_folderId='+folderId);
  $('#fs-toolbar-yes').removeClass('highlight');
  $('#fs-toolbar-no').addClass('highlight');

}
