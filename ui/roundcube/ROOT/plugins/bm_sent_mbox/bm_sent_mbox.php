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
 * redirect mail in the send folder specified in the identity. 
 *
 *
 */

 
class bm_sent_mbox extends rcube_plugin {
  private $rcmail;
  private $hierarchy;

  public function init() {
    $this->rcmail = rcmail::get_instance();
    $this->hierarchy = new rcube\MyHierarchy();  
    $this->add_hook('message_compose_body', array($this, 'message_compose'));
  }

  // global hooks
  public function message_compose($args) {
    global $COMPOSE, $MESSAGE;
    if ($this->rcmail->config->get('reply_same_folder') && $args['mode'] ==  RCUBE_COMPOSE_REPLY) { 
      return;
    }
    $this->include_script('bm_sent_mbox.js');

    $mbox = array();
    $mbox['default'] = $COMPOSE['param']['sent_mbox'];
    $identities = $this->rcmail->user->list_identities();
    foreach ($identities as $identity) {
      if ($identity['sent'] != 'Sent' && $identity['owner']) {
        $mailbox = $this->hierarchy->getMailboxByUid($identity['owner']);
        $sent = (($mailbox && $mailbox->path) ? ($mailbox->path . $this->rcmail->storage->delimiter) : '') . $this->rcmail->config->get('sent_mbox', 'Sent');

        if ($sent && rcmail_check_sent_folder($sent, true)) {
          $mbox[$identity['identity_id']] = $sent;
        }
        if ($MESSAGE->compose['from'] == $identity['identity_id']) {
          $COMPOSE['param']['sent_mbox'] = $sent;
        }
      }
    }
    
    $this->rcmail->output->set_env('sent_mbox', $mbox);
  }

}
