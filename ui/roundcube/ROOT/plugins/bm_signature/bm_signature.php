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

class bm_signature extends rcube_plugin { 

  const EXPIRATION_TIME = 600;
  private $rcmail;
  private $executed = false;

  function init() {
    if (!isset($_SESSION['bm']['signature'])) $_SESSION['bm']['signature'] = array();
    $this->rcmail = rcmail::get_instance();
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $this->add_texts('localization', true);      
      $this->include_script('bm_signature.js');
      $css = 'skins/bluemind/bm_signature.css';
      $this->include_stylesheet($css);
    }
    
    $this->add_hook('plugin.bm_mailtips.buildContext', array($this, 'addFilter'));
    $this->add_hook('plugin.bm_mailtips.mailtips', array($this, 'onMailTips'));
    $this->add_hook('template_object_composeoptionsheader', array($this, 'signaturePreviewHeader'));
    $this->add_hook('template_object_composeoptionsbody', array($this, 'signaturePreviewBody'));

  }

  function addFilter($args) {
    $context = $args['context'];
    $cache = $this->getCacheValue($context);
    if ($cache === NULL) {
      $context->filter->mailTips[] = 'Signature';
    } else {
      $from = $args['from'];
      $this->rcmail->output->command('plugin.bm_signature', array(
       'from' => $args['from'],
       'value' => $cache['value'],
       'enable' => $cache['enable'],
       'placeholder' => $cache['placeholder'],
       'fromCache' => 'true' 
      ));
      $this->executed = true;
    }
    return $args;
  }

  function onMailTips($args) {
    if ($this->executed) return $args;

    $tips = $args['tips'];
    $context = $args['context'];
    $from = $args['from'];
    $value = false;
    $enable = true;
    $placeholder = false;
    foreach($tips as $tip) {
      foreach($tip->matchingTips as $matchingTip) {
        if ($matchingTip->mailtipType == 'Signature') {
          $tipContent = json_decode($matchingTip->value);
          if (!$this->isDisclaimer($tipContent)) {
            $enable = false;
          }
          if ($tipContent->usePlaceholder) {
            $placeholder = true;
          }
          if (!$value) {
            $value = $tipContent;
          } else {
            $value->html .= "<div>$tipContent->html</div>";
            $value->text .= "\n" . $tipContent->text;
          }
        }
      }
    }

    $this->rcmail->output->command('plugin.bm_signature', array(
     'from' => $from,
     'value' => $value,
     'enable' => $enable,
     'placeholder' => $placeholder
    ));
    $this->setCacheValue($context, $value, $enable, $placeholder);
    return $args;
  }
  
  function getCacheKey($context) {
    $from = $context->messageContext->fromIdentity->from;
    $internal = true;
    foreach($context->messageContext->recipients as $r) {
      $left = strtolower(array_pop(explode('@', $r->email)));
      if (!in_array($left, $_SESSION['bm_sso']['domainAliases'])) {
        $internal = false;
        break;
      }
    }
    return $from . ':' . ($internal ? 'internal':'external');
  }

  function setCacheValue($context, $value=false, $enable=false, $placeholder=false) {
    $_SESSION['bm']['signature'][$this->getCacheKey($context)] = array(
      'value' => $value,
      'enable' => $enable,
      'placeholder' => $placeholder,
      'expiration' => time() + self::EXPIRATION_TIME
    );
  }

  function getCacheValue($context) {
    $key = $this->getCacheKey($context);
    if (!isset($_SESSION['bm']['signature'][$key]) || ($_SESSION['bm']['signature'][$key]['expiration'] < time())) {
      return null;
    }
    return $_SESSION['bm']['signature'][$key];
  }

  function isDisclaimer($tip) {
    return $tip->isDisclaimer;
  }

  function signaturePreviewHeader($args) {
    $out = html::label(
      array('style' => 'display:none;'), 
      rcube_label('bm_signature.signature_preview') . ' ' .
      html::a(
        array('href' => '#signature_preview', 'id' => 'signaturepreviewtoggle', 'class' => 'fa  fa-caret-square-o-up', 'nofaspan' => true), ''
      )
    );
    $args['content'] .= $out;
    return $args;
  }

  function signaturePreviewBody($args) {
    $out = html::div (
      array('style' => 'display:none;', 'id' => 'signaturepreview')
    );
    $args['content'] .= $out;
    return $args;
  }
}
