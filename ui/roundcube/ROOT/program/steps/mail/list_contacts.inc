<?php

/*
 +-----------------------------------------------------------------------+
 | program/steps/mail/list_contacts.inc                                  |
 |                                                                       |
 | This file is part of the Roundcube Webmail client                     |
 | Copyright (C) 2012, The Roundcube Dev Team                            |
 |                                                                       |
 | Licensed under the GNU General Public License version 3 or            |
 | any later version with exceptions for skins & plugins.                |
 | See the README file for a full license statement.                     |
 |                                                                       |
 | PURPOSE:                                                              |
 |   Send contacts list to client (as remote response)                   |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Thomas Bruederli <roundcube@gmail.com>                        |
 +-----------------------------------------------------------------------+

 $Id$

*/

$jsenv = array();
$source = get_input_value('_source', RCUBE_INPUT_GPC);
$CONTACTS = $RCMAIL->get_address_book($source);
$PAGE_SIZE = $RCMAIL->config->get('addressbook_pagesize', $RCMAIL->config->get('pagesize', 50));
$PAGE = max(1, intval($_GET['_page']));
if ($CONTACTS && $CONTACTS->ready) {
    // set list properties
    $CONTACTS->set_pagesize($PAGE_SIZE);
    $CONTACTS->set_page($PAGE);

    // list groups of this source (on page one)
    $groups = array();
    if ($CONTACTS->groups) {
        $groups = $CONTACTS->list_groups();
        while ($group = $groups->next()) {
            $CONTACTS->reset();
            $CONTACTS->set_group($group['ID']);
            $group_prop = $CONTACTS->get_group($group['ID']);

            // group (distribution list) with email address(es)
            if ($group_prop['email']) {
                foreach ((array)$group_prop['email'] as $email) {
                    $row_id = 'G'.$group['ID'];
                    $jsresult[$row_id] = format_email_recipient($email, $group['name']);
                    $OUTPUT->command('add_contact_row', $row_id, array(
                        'contactgroup' => html::span(array('class' => 'fa fa-users', 'title' => $email), Q($group['name']))), 'group');
                }
            }
            // show group
            else {
                $row_id = 'E'.$group['ID'];
                $jsresult[$row_id] = $group['name'];
                $OUTPUT->command('add_contact_row', $row_id, array(
                    'contactgroup' => html::span(array('class' => 'fa fa-users'), Q($group['name']))), 'group');
            }
        }
    }

    // get contacts for this user
    $contactPage = $PAGE - floor($groups->count / $PAGE_SIZE); 
    $missing = $PAGE_SIZE - count($groups->records);
    if ($missing > 0 && $contactPage > 0) {
        $CONTACTS->set_pagesize($missing);
        $CONTACTS->set_page($contactPage);
        $CONTACTS->set_group(0);
        $result = $CONTACTS->list_records(array('name', 'firstname', 'surname', 'email'));
        if (!$result->count && $result->searchonly) {
            $OUTPUT->show_message('contactsearchonly', 'notice');
        }
        else if (!empty($result) && $result->count > 0) {
            // create javascript list
            while ($row = $result->next()) {
                $name = rcube_addressbook::compose_list_name($row);

                // add record for every email address of the contact
                foreach ($CONTACTS->get_col_values('email', $row, true) as $i => $email) {
                    $row_id = $row['ID'].$i;
                    $jsresult[$row_id] = format_email_recipient($email, $name);
                    $OUTPUT->command('add_contact_row', $row_id, array(
                        'contact' => html::span(array('class' => 'fa fa-user', 'title' => $email), Q($name ? $name : $email))), 'person');
                        break;
                }
            }
        }
    }
}

// update env
$OUTPUT->set_env('contactdata', $jsresult);
$OUTPUT->set_env('pagecount', ceil(($result->count + $groups->count) / $PAGE_SIZE));
$OUTPUT->command('set_page_buttons');

// send response
$OUTPUT->send();
