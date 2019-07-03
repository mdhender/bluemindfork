<?php
/*
 +-----------------------------------------------------------------------+
 | steps/mail/search.inc                                                 |
 |                                                                       |
 | Search functions for rc webmail                                       |
 |                                                                       |
 | Licensed under the GNU General Public License version 3 or            |
 | any later version with exceptions for skins & plugins.                |
 | See the README file for a full license statement.                     |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Benjamin Smith <defitro@gmail.com>                            |
 |         Thomas Bruederli <roundcube@gmail.com>                        |
 +-----------------------------------------------------------------------+

 $Id$

*/

$REMOTE_REQUEST = TRUE;

// reset list_page and old search results
$RCMAIL->storage->set_page(1);
$RCMAIL->storage->set_search_set(NULL);
$_SESSION['page'] = 1;

// using encodeURI with javascript "should" give us
// a correctly encoded query string
$imap_charset = RCMAIL_CHARSET;

// get search string
$str     = get_input_value('_q', RCUBE_INPUT_GET, true);
$mbox    = get_input_value('_mbox', RCUBE_INPUT_GET, true);
$filter  = get_input_value('_filter', RCUBE_INPUT_GET);
$headers = get_input_value('_headers', RCUBE_INPUT_GET);
$subject = array();

$search_request = md5($mbox.$filter.$str);

$_SESSION['search_filter'] = $filter;

$search_str = $RCMAIL->storage->parseSearch($str, $filter, $headers);
$sort_column = rcmail_sort_column();

// execute IMAP search
if ($search_str)
  $RCMAIL->storage->search($mbox, $search_str, $imap_charset, $sort_column);

// save search results in session
if (!is_array($_SESSION['search']))
  $_SESSION['search'] = array();

if ($search_str) {
  $_SESSION['search'] = $RCMAIL->storage->get_search_set();
  $_SESSION['last_text_search'] = $str;
}
$_SESSION['search_request'] = $search_request;


// Get the headers
$result_h = $RCMAIL->storage->list_messages($mbox, 1, $sort_column, rcmail_sort_order());
$count = $RCMAIL->storage->count($mbox, $RCMAIL->storage->get_threading() ? 'THREADS' : 'ALL');

// Make sure we got the headers
if (!empty($result_h)) {
  rcmail_js_message_list($result_h);
  if ($search_str)
    $OUTPUT->show_message('searchsuccessful', 'confirmation', array('nr' => $RCMAIL->storage->count(NULL, 'ALL')));
}
// handle IMAP errors (e.g. #1486905)
else  if ($err_code = $RCMAIL->storage->get_error_code()) {
  rcmail_display_server_error();
}
else {
  $OUTPUT->show_message('searchnomatch', 'notice');
}

// update message count display
$OUTPUT->set_env('search_request', $search_str ? $search_request : '');
$OUTPUT->set_env('messagecount', $count);
$OUTPUT->set_env('pagecount', ceil($count/$RCMAIL->storage->get_pagesize()));
$OUTPUT->command('set_rowcount', rcmail_get_messagecount_text($count, 1), $mbox);
$OUTPUT->send();
