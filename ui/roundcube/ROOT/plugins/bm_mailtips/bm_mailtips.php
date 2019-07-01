<?php
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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

class bm_mailtips extends rcube_plugin { 

  private $rcmail;
  private $client;
  private $output;

  function init() {

    $this->rcmail = rcmail::get_instance();
    $this->client = new BM\MailTipClient($_SESSION['bm']['core'], $this->rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);

    $this->output = $this->rcmail->output;

    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->include_script('bm_mailtips.js');
    }
    $this->register_action('plugin.bm_mailtips.getmailtips', array($this, 'getMailTips'));
  }


  function getMailTips() {
    $from = get_input_value('_from', RCUBE_INPUT_POST, TRUE);
    $charset = isset($_POST['_charset']) ? $_POST['_charset'] : $this->output->get_charset();
    $recipients = $this->parseRecipients(get_input_value('_to', RCUBE_INPUT_POST, TRUE, $charset));
    $recipients = $this->parseRecipients(get_input_value('_cc', RCUBE_INPUT_POST, TRUE, $charset), 'CC', $recipients);
    $recipients = $this->parseRecipients(get_input_value('_bcc', RCUBE_INPUT_POST, TRUE, $charset), 'BCC', $recipients);
    $sender = $this->parseSender($from);
    $context = new BM\MessageContext();
    $context->fromIdentity = $sender;
    $context->recipients = $recipients;
    $context->messageClass = 'Mail';
    $context->subject = get_input_value('_subject', RCUBE_INPUT_POST, TRUE);
    $filter = new BM\MailTipFilter();
    $filter->mailTips = array();
    $filter->filterType = 'INCLUDE';
    $mailContext = new BM\MailTipContext();
    $mailContext->messageContext = $context;
    $mailContext->filter = $filter;
    $args= $this->rcmail->plugins->exec_hook('plugin.bm_mailtips.buildContext',array('from' => $from, 'context' => $mailContext));
    $mailContext = $args['context'];
    if ($mailContext->filter->filterType == 'INCLUDE' && count($mailContext->filter->mailTips) == 0) {
      $tips = array();
    } else {
      $tips = $this->client->getMailTips($mailContext);
    }

    $this->rcmail->plugins->exec_hook('plugin.bm_mailtips.mailtips',array('from' => $from, 'context' => $mailContext, 'tips' => $tips));
  }

  function parseRecipients($mailto, $type = 'TO', $list = array()) {
    // simplified email regexp, supporting quoted local part
    $email_regexp = '(\S+|("[^"]+"))@\S+';
  
    $delim = trim($this->rcmail->config->get('recipients_separator', ','));
    $regexp  = array("/[,;$delim]\s*[\r\n]+/", '/[\r\n]+/', "/[,;$delim]\s*\$/m", '/;/', '/(\S{1})(<'.$email_regexp.'>)/U');
    $replace = array($delim.' ', ', ', '', $delim, '\\1 \\2');
  
    // replace new lines and strip ending ', ', make address input more valid
    $mailto = trim(preg_replace($regexp, $replace, $mailto));
  
    $items = rcube_explode_quoted_string($delim, $mailto);
    $emails = []; 
    foreach($items as $email) {
      $email = trim($email);
      $recipient = new BM\Recipient;
      $recipient->addressType = 'SMTP';
      $recipient->recipientType = $type;
      if (preg_match('/^<'.$email_regexp.'>$/', $email)) {
        $recipient->email = rcube_idn_to_ascii(trim($email, '<>'));
      } else if (preg_match('/^'.$email_regexp.'$/', $email)) {
        $recipient->email = rcube_idn_to_ascii($email);
      } else if (preg_match('/<*'.$email_regexp.'>*$/', $email, $matches)) {
        $address = $matches[0];
        $name = trim(str_replace($address, '', $email));
        if ($name[0] == '"' && $name[count($name)-1] == '"') {
          $name = substr($name, 1, -1);
        }
        $recipient->name = stripcslashes($name);
        $recipient->email = rcube_idn_to_ascii(trim($address, '<>'));
      } else {
        continue;
      }
      if (!in_array($recipient->email, $emails)) {
        $list[] = $recipient;
        $emails[] = $recipient->email;
      }
    }

    return $list;
  }

    function parseSender($id) {
      $sender = new BM\SendingAs();
      $sender->sender = $_SESSION['bm_sso']['bmDefaultEmail'];
      $identity = $this->rcmail->user->get_identity($id);
      $sender->from = $identity['email'];
      return $sender;
    }
}
 


