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

/**
 * Blue Mind ICS
 */
class bm_ics extends rcube_plugin {

  public $task = 'mail';
  private $rcmail;

  private $event_uid;
  private $rsvp_event_uids;
  private $rsvp;
  private $resource_id;
  private $other;
  private $shared;
  private $evtType;
  private $originator;

  function init() {
    $this->event_uid = null;
    $this->rsvp = null;
    $this->resource_id = null;
    $this->other = $_SESSION['imap_namespace']['other'][0][0];
    $this->shared = $_SESSION['imap_namespace']['shared'][0][0];
    $this->rcmail = rcmail::get_instance();
    $this->evtType = null;
    $this->originator = null;
    $this->add_hook('startup', array($this, 'startup'));
    $this->register_action('plugin.bm_ics.update', array($this, 'update'));
    $this->register_action('plugin.bm_ics.updateCounter', array($this, 'updateCounter'));
    $this->hierarchy = new rcube\MyHierarchy();    
  }

  private function getCalendarClient($container) {
    $rcmail = rcmail::get_instance();
    $calendarClient = new BM\CalendarClient($_SESSION['bm']['core'],
      $rcmail->decrypt($_SESSION['password']),
      $container);
    return $calendarClient;
  }

  private function getContainerManagement($container) {
    $rcmail = rcmail::get_instance();
    $cli = new BM\ContainerManagementClient($_SESSION['bm']['core'],
      $rcmail->decrypt($_SESSION['password']),
      $container);
    return $cli;
  }


  private function register() {    
    $this->add_hook('messages_list', array($this, 'message_list'));
    $this->include_stylesheet('skins/default/bm_ics.css');
    $this->add_texts('localization/', true);
    if ($this->rcmail->action == 'show' || $this->rcmail->action == 'preview') {
      $this->add_hook('message_load', array($this, 'message_load'));
      $this->add_hook('template_object_messagebody', array($this, 'template_object_messagebody'));
      $this->include_script('bm_ics.js');
    }
  }

  public function startup() {
    $this->register();
  }

  function message_load($p) {
    $this->rsvp_event_uids = array();
    $eventHeader = $p['object']->get_header('x-bm-event');
    
    if (!$eventHeader){
      $eventHeader = $p['object']->get_header('x-bm-event-countered');
      if ($eventHeader){
        $evtType = 'COUNTER';
        $this->resource_id = $p['object']->get_header('x-bm-resourcebooking');
        list($icsUid, $recurid, $originator) = $this->parseCounterEventHeader($eventHeader);
        $this->series = $this->getSeriesByIcsUid($icsUid, $recurid);
        $this->recurid = $recurid;
        $this->originator = $originator;
        $this->evtType = $evtType;
      }
    } else {
      $evtType = 'INVITE';
      $this->resource_id = $p['object']->get_header('x-bm-resourcebooking');
      list($icsUid, $recurid, $rsvp) = $this->parseEventHeader($eventHeader);
      $this->series = $this->getSeriesByIcsUid($icsUid, $recurid);
      $this->recurid = $recurid;
      $this->rsvp = $rsvp;
      $this->evtType = $evtType;
    }
    return $p;
  }

  function parseCounterEventHeader($header) {
    $header = is_array($header) ? $header[0]: $header;
    $values = explode(';', $header);
    $ics = trim(array_shift($values));
    if (count($values) > 1) {
      preg_match_all('/"([^"]+)"/', array_shift($values), $matches);
      $originator = $matches[0][0];
      $originator = str_replace("\"", "", $originator);
      preg_match_all('/"([^"]+)"/', array_shift($values), $matches);
      $recurid = $matches[0][0];
      $recurid = str_replace("\"", "", $recurid);
    } else {
      $originator = substr(trim(array_shift($values)), 12);
      $originator = str_replace("\"", "", $originator);
    }
    return array($ics, $recurid, $originator);
  }

  function parseEventHeader($header) {
    $header = is_array($header) ? $header[0]: $header;
    $values = explode(';', $header);
    $ics = trim(array_shift($values));
    if (count($values) > 1) {
      preg_match_all('/"([^"]+)"/', array_shift($values), $matches);
      $recurid = $matches[1][0];
    }
    $rsvp = strpos(array_shift($values), 'true') !== false;
    return array($ics, $recurid, $rsvp);
  }

  function getContainer() {
    $container = 'calendar:Default:'.$_SESSION['bm_sso']['bmUserId'];
    if ($this->resource_id != null) {
        $container = 'calendar:'.$this->resource_id;
    } else if ((strpos($_SESSION['mbox'], $this->other) === 0)) {
       $mailbox = $this->hierarchy->getMailboxByPath($_SESSION['mbox']);
       $container = 'calendar:Default:' . $mailbox->uid;
    }
    return $container;
  }

  function getSeriesByIcsUid($icsUid, $recurid){
    $container = $this->getContainer();  
    try {
      $series = $this->getCalendarClient($container)->getByIcsUid($icsUid);
    } catch (Exception $e) {
      return;
    }
    foreach($series as $vseries) {
      if ($vseries->value->main != null) {
        return $vseries;
      } elseif ($recurid != null && $vseries->value->occurrences[0]->recurid->iso8601 == $recurid) {
        return $vseries;
      }
      $default = $vseries; 
    }
    return $default;
  }

  function getVEvent($series, $recurid) {
    if ($recurid == null) {
      return $series->value->main;
    } else {
      foreach($series->value->occurrences as $vevent) {
        if ($vevent->recurid->iso8601 == $recurid) {
          return $vevent;
        }
      }
    }
  }

 function getCounter($series, $recurid) {
    foreach($series->value->counters as $counter) {
      if ($counter->originator->email == $this->originator){
        if (($recurid == null && $counter->counter->recurid == null) || ($recurid != null && $counter->counter->recurid != null && $counter->counter->recurid->iso8601 == $recurid)) {
          return $counter->counter;
        }
      }
    }
  }

  public function message_list($args) {
    $count = count($args['messages']);
    for ($i=0;$i<$count;$i++) {
      $header = $args['messages'][$i];
      $uid = $header->uid;
      if ($uid) {
        $invitation = ($header->get('x-bm-event') != null);
        $invitation = $invitation || ($header->get('x-bm-resourcebooking') != null);
        if ($invitation) {
          $header->list_flags['extra_flags']['invitation'] = $invitation;
          $args['messages'][$i]->list_cols['attachment'] = "<span class=\"fa fa-calendar\"></span>";
        } else {
          $counter = ($header->get('x-bm-event-countered') != null);
          if ($invitation) {
            $header->list_flags['extra_flags']['counter'] = $counter;
            $args['messages'][$i]->list_cols['attachment'] = "<span class=\"fa fa-calendar\"></span>";
          }
        }   
      }
    }
    return $args;
  }

  function template_object_messagebody($p){
    if ($this->rsvp && $this->evtType == 'INVITE' && !(strpos($_SESSION['mbox'], $this->shared) === 0)) {
      return $this->template_invite($p);
    } else if ($this->evtType == 'COUNTER' && !(strpos($_SESSION['mbox'], $this->shared) === 0)){
      return $this->template_counter($p);
    }
  }

  public function template_counter($p){
      $evt = $this->getVEvent($this->series, $this->recurid);
      $counter = $this->getCounter($this->series, $this->recurid);
      $recurid = urlencode($this->recurid);

      if ($evt == null){
        $p['content'] = "<div class='ics-toolbar-error'>
          ".$this->gettext('deletedEvent')."
        </div> 
        $p[content]";
        return $p;
      } 
      if ($counter == null){
        $p['content'] = "<div class='ics-toolbar-error'>
          ".$this->gettext('deletedCounter')."
        </div> 
        $p[content]";
        return $p;
      }
  
      $container = $this->getContainer();
      $id = $this->series->uid;  
      if ($this->resource_id != null) {
        $me = 'bm://'.$_SESSION['bm_sso']['bmDomain'].'/resources/'.$this->resource_id;
      } else {
        if ((strpos($_SESSION['mbox'], $this->other) === 0)) {
          $writable = $this->getContainerManagement($container)->getDescriptor()->writable;
          if (!$writable) {
            return; 
          }
          $mailbox = $this->hierarchy->getMailboxByPath($_SESSION['mbox']);
          $user = $mailbox->uid;
          $name = $mailbox->displayName;
        } else {
          $user = $_SESSION['bm_sso']['bmUserId'];
        }
        $me = 'bm://'.$_SESSION['bm_sso']['bmDomain'].'/users/'.$user;
      }

      $accept = "<a id='ics-toolbar-accepted' href='#' onClick='acceptCounter(\"$container\", \"$id\", \"$recurid\", \"$this->originator\"); return false'>".$this->gettext('accepted')."</a>";
      $decline = "<a id='ics-toolbar-declined' href='#' onClick='declineCounter(\"$container\", \"$id\", \"$recurid\", \"$this->originator\"); return false'>".$this->gettext('declined')."</a>";

      $dateformat = $_SESSION['bm']['settings']['date'] == 'yyyy-MM-dd' ? 'Y-m-d' : 'd/m/Y';
      $timeformat = $_SESSION['bm']['settings']['timeformat'] == 'HH:mm' ? 'H:i':'h:i a';
      $tz = $_SESSION['bm']['settings']['timezone'];

      $dtstart = new DateTime($tz);
      $dtstart->setTimestamp(date("U",strtotime($evt->dtstart->iso8601)));
      if ($evt->dtstart->precision == 'DateTime') {
        $dtstartStr = $dtstart->format("$dateformat, $timeformat");
      } else {
        $dtstartStr = $dtstart->format($dateformat);
      }

      $dtend = new DateTime($tz);
      $dtend->setTimestamp(date("U",strtotime($evt->dtend->iso8601)));
      $dtendStr = $dtend->format($dateformat);
      if ($evt->dtend->precision == 'DateTime') {
        $dtendStr = $dtend->format("$dateformat, $timeformat");
      } else {
        $dtendStr = $dtend->format($dateformat);
      }


      $dtstartCounter = new DateTime($tz);
      $dtstartCounter->setTimestamp(date("U",strtotime($counter->dtstart->iso8601)));
      if ($counter->dtstart->precision == 'DateTime') {
        $dtstartStrCounter = $dtstartCounter->format("$dateformat, $timeformat");
      } else {
        $dtstartStrCounter = $dtstartCounter->format($dateformat);
      }

      $dtendCounter = new DateTime($tz);
      $dtendCounter->setTimestamp(date("U",strtotime($counter->dtend->iso8601)));
      $dtendStrCounter = $dtendCounter->format($dateformat);
      if ($counter->dtend->precision == 'DateTime') {
        $dtendStrCounter = $dtendCounter->format("$dateformat, $timeformat");
      } else {
        $dtendStrCounter = $dtendCounter->format($dateformat);
      }

      $location = "";
      if ($evt->location) {
        $location = "<tr>
            <td class='header-title'>".$this->gettext('where')."</td>
            <td class='header'>".$evt->location."</td>
          </tr>";
      }
      if ($name) {
        $label = $this->gettext(array('name' => 'participationof', 'vars' => array('$name' => $name)));
      } else {
        $label = $this->gettext('participation');
      }
      $p['content'] = "<div class='ics-toolbar'>
        <table class='headers-table'>
          <tr>
            <td class='header-title'>".$this->gettext('title')."</td>
            <td class='header'>".$evt->summary."</td>
          </tr>
          <tr>
            <td class='header-title'>".$this->gettext('when')."</td>
            <td class='header'>$dtstartStr - $dtendStr, $tz</td>
          </tr>
          <tr>
            <td class='header-title'>".$this->gettext('proposition')."</td>
            <td class='header'>$dtstartStrCounter - $dtendStrCounter, $tz</td>
          </tr>
          $location
          <tr>
            <td class='header-title'>".$this->gettext('applyProposition')." </td>
            <td class='header'>$accept - $decline</td>
          </tr>
        </table>
      </div>
      $p[content]";

      return $p;
  }

  public function template_invite($p){
    $evt = $this->getVEvent($this->series, $this->recurid);
      $recurid = urlencode($this->recurid);
      if ($evt != null) {
        $container = $this->getContainer();
        $id = $this->series->uid;  
        if ($this->resource_id != null) {
          $me = 'bm://'.$_SESSION['bm_sso']['bmDomain'].'/resources/'.$this->resource_id;
        } else {
          if ((strpos($_SESSION['mbox'], $this->other) === 0)) {
            $writable = $this->getContainerManagement($container)->getDescriptor()->writable;
            if (!$writable) {
              return; 
            }
            $mailbox = $this->hierarchy->getMailboxByPath($_SESSION['mbox']);
            $user = $mailbox->uid;
            $name = $mailbox->displayName;
          } else {
            $user = $_SESSION['bm_sso']['bmUserId'];
          }
          $me = 'bm://'.$_SESSION['bm_sso']['bmDomain'].'/users/'.$user;
        }

        $part = null;
        foreach($evt->attendees as $attendee) {
          if ($me == $attendee->dir) {
            $part = $attendee->partStatus;
          }
        }
        if (!$part) {
          return;
        }

        $acceptedCss = "";
        $tentativeCss = "";
        $declinedCss = "";
        if($part == "Accepted") {
          $acceptedCss = "class='highlight'";
        } else if ($part == "Tentative") {
          $tentativeCss = "class='highlight'"; 
        } elseif ($part == "Declined") {
          $declinedCss = "class='highlight'";
        }

   

        $accept = "<a id='ics-toolbar-accepted' $acceptedCss href='#' onClick='accept(\"$container\", \"$id\", \"$recurid\", \"$me\"); return false'>".$this->gettext('accepted')."</a>";
        $tentative = "<a id='ics-toolbar-tentative' $tentativeCss href='#' onClick='tentative(\"$container\",\"$id\", \"$recurid\", \"$me\"); return false'>".$this->gettext('tentative')."</a>";
        $decline = "<a id='ics-toolbar-declined' $declinedCss href='#' onClick='decline(\"$container\", \"$id\", \"$recurid\", \"$me\"); return false'>".$this->gettext('declined')."</a>";

        $dateformat = $_SESSION['bm']['settings']['date'] == 'yyyy-MM-dd' ? 'Y-m-d' : 'd/m/Y';
        $timeformat = $_SESSION['bm']['settings']['timeformat'] == 'HH:mm' ? 'H:i':'h:i a';
        $tz = $_SESSION['bm']['settings']['timezone'];

        $dtstart = new DateTime($tz);
        $dtstart->setTimestamp(date("U",strtotime($evt->dtstart->iso8601)));
        if ($evt->dtstart->precision == 'DateTime') {
          $dtstartStr = $dtstart->format("$dateformat, $timeformat");
        } else {
          $dtstartStr = $dtstart->format($dateformat);
        }

        $dtend = new DateTime($tz);
        $dtend->setTimestamp(date("U",strtotime($evt->dtend->iso8601)));
        $dtendStr = $dtend->format($dateformat);
        if ($evt->dtend->precision == 'DateTime') {
          $dtendStr = $dtend->format("$dateformat, $timeformat");
        } else {
          $dtendStr = $dtend->format($dateformat);
        }

        $location = "";
        if ($evt->location) {
          $location = "<tr>
              <td class='header-title'>".$this->gettext('where')."</td>
              <td class='header'>".$evt->location."</td>
            </tr>";
        }
        if ($name) {
          $label = $this->gettext(array('name' => 'participationof', 'vars' => array('$name' => $name)));
        } else {
          $label = $this->gettext('participation');
        }
        $p['content'] = "<div class='ics-toolbar'>
          <table class='headers-table'>
            <tr>
              <td class='header-title'>".$this->gettext('title')."</td>
              <td class='header'>".$evt->summary."</td>
            </tr>
            <tr>
              <td class='header-title'>".$this->gettext('when')."</td>
              <td class='header'>$dtstartStr - $dtendStr, $tz</td>
            </tr>
            $location
            <tr>
              <td class='header-title'>".$label." </td>
              <td class='header'>$accept - $tentative - $decline</td>
            </tr>
          </table>
        </div>
        $p[content]";

        return $p;
      } else {
        $p['content'] = "<div class='ics-toolbar-error'>
          ".$this->gettext('deletedEvent')."
        </div> 
        $p[content]";
        return $p;
      }
  }

  public function updateCounter() {
    $uid = get_input_value('_uid', RCUBE_INPUT_POST);
    $recurid = get_input_value('_recurid', RCUBE_INPUT_POST);
    $container = get_input_value('_container', RCUBE_INPUT_POST);
    $status = get_input_value('_status', RCUBE_INPUT_POST);
    $this->originator = get_input_value('_originator', RCUBE_INPUT_POST);

    $cli = $this->getCalendarClient($container);

    $series = $cli->getComplete($uid);
    $evt = $this->getVEvent($series, $recurid);
    $counter = $this->getCounter($series, $recurid);  
    
    if ($status == 'ACCEPTED'){
      $evt->dtstart = $counter->dtstart;
      $evt->dtend = $counter->dtend;
      $series->value->counters = array();
      if ($series->value->main != null && $series->value->main->rrule != null && $recurid == null){
        $series->value->main->exdate = array();
        $series->value->occurrences = array();
      }
      $series->value->counters = array();
      foreach($evt->attendees as $attendee) {
          $attendee->partStatus = 'NeedsAction';
          $attendee->rsvp = true;
      }  
    } else {
      $i = 0;
      foreach($series->value->counters as $c) {
        if ($c->originator->email == $this->originator){
          if (($recurid == null && $c->counter->recurid == null) || ($recurid != null && $c->counter->recurid != null && $c->counter->recurid->iso8601 == $recurid)) {
            array_splice($series->value->counters, $i, 1);
            break;
          }
          $i++;
        }
      }
    }
    $serializable = new BMSerializableObject((array) $series->value);
    $cli->update($uid, $serializable, 'true');
    if ($status == 'ACCEPTED'){
      $this->rcmail->output->command('display_message', $this->gettext('propositionApplied'), 'confirmation');
    } else {
      $this->rcmail->output->command('display_message', $this->gettext('propositionRefused'), 'confirmation');
    }
    $this->rcmail->output->send();  
  }

  public function update() {
    $uid = get_input_value('_uid', RCUBE_INPUT_POST);
    $recurid = get_input_value('_recurid', RCUBE_INPUT_POST);
    $me = get_input_value('_me', RCUBE_INPUT_POST);
    $part = get_input_value('_part', RCUBE_INPUT_POST);
    $container = get_input_value('_container', RCUBE_INPUT_POST);

    $cli = $this->getCalendarClient($container);

    $series = $cli->getComplete($uid);
    $evt = $this->getVEvent($series, $recurid);

    foreach($evt->attendees as $attendee) {
      if ($me == $attendee->dir) {
        $attendee->partStatus = $part;
        $attendee->responseComment = '';
        break;
      }
    }
    $serializable = new BMSerializableObject((array) $series->value);
    $cli->update($uid, $serializable, 'true');

    $this->rcmail->output->command('display_message', $this->gettext('confirmation'), 'confirmation');
    $this->rcmail->output->send();
  }
}

