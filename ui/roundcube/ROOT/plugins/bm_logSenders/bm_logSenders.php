<?php
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
*/
?>
<?php

/**
 * BlueMind log senders plug-in
 *
 * Log real login and identity used on mail sent
 *
 */

/*
 * 1. If token exist in session, use it to authenticate
 * 2. If no token exist or token not valid: redirect to SSO to get a valid ticket, then ckeck ticket against SSO:
 *   a. if valid ticket, get valid token
 *   b. if invalid ticket, redirect to SSO again
*/
class bm_logSenders extends rcube_plugin {
  public $task = 'mail';
  private $logFile = 'mailSent.log';

  public function init() {
    // global hooks
    $this->add_hook('message_sent', array($this, 'message_sent'));
  }

  // global hooks
  public function message_sent($args) {
    $logMessage = "User: ".$_SESSION['username']." send email ID: ".$args['headers']['Message-ID'].", using identity: ".$args['headers']['From'];

    if(isset($args['headers']['To']) && $args['headers']['To'] != '') {
      $logMessage .= ", to: ".str_replace("\r\n", "", $args['headers']['To']);
    }else {
      $logMessage .= ", to: -";
    }

    if(isset($args['headers']['Cc']) && $args['headers']['Cc'] != '') {
      $logMessage .= ", cc: ".str_replace("\r\n", "", $args['headers']['Cc']);
    }else {
      $logMessage .= ", cc: -";
    }

    if(isset($args['headers']['Bcc']) && $args['headers']['Bcc'] != '') {
      $logMessage .= ", bcc: ".str_replace("\r\n", "", $args['headers']['Bcc']);
    }else {
      $logMessage .= ", bcc: -";
    }

    write_log($this->logFile, $logMessage);
  }
}
