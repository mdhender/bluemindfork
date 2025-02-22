<?php
/*

 +-----------------------------------------------------------------------+
 | program/steps/utils/save_pref.inc                                     |
 |                                                                       |
 | This file is part of the Roundcube Webmail client                     |
 | Copyright (C) 2005-2013, The Roundcube Dev Team                       |
 |                                                                       |
 | Licensed under the GNU General Public License version 3 or            |
 | any later version with exceptions for skins & plugins.                |
 | See the README file for a full license statement.                     |
 |                                                                       |
 | PURPOSE:                                                              |
 |   Save preferences setting in database                                |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Aleksander Machniak <alec@alec.pl>                            |
 +-----------------------------------------------------------------------+

 $Id$

*/
$name     = get_input_value('_name', RCUBE_INPUT_POST);
$value    = get_input_value('_value', RCUBE_INPUT_POST);
$sessname = get_input_value('_session', RCUBE_INPUT_POST);
// Whitelisted preferences and session variables, others
// can be added by plugins
$whitelist = array(
    'preview_pane',
    'list_cols',
    'collapsed_folders',
    'collapsed_abooks',
);
$whitelist_sess = array(
    'list_attrib/columns',
);

$whitelist      = array_merge($whitelist, $RCMAIL->plugins->allowed_prefs);
$whitelist_sess = array_merge($whitelist_sess, $RCMAIL->plugins->allowed_session_prefs);

if (!in_array($name, $whitelist) || ($sessname && !in_array($sessname, $whitelist_sess))) {
    raise_error(array('code' => 500, 'type' => 'php',
        'file' => __FILE__, 'line' => __LINE__,
        'message' => sprintf("Hack attempt detected (user: %s)", $_SESSION['username'])),
        true, false);

    $OUTPUT->reset();
    $OUTPUT->send();
}

// save preference value
$RCMAIL->user->save_prefs(array($name => $value));

// update also session if requested
if ($sessname) {
    // Support multidimensional arrays...
    $vars = explode('/', $sessname);

    // ... up to 3 levels
    if (count($vars) == 1)
        $_SESSION[$vars[0]] = $value;
    else if (count($vars) == 2)
        $_SESSION[$vars[0]][$vars[1]] = $value;
    else if (count($vars) == 3)
        $_SESSION[$vars[0]][$vars[1]][$vars[2]] = $value;
}

$OUTPUT->reset();
$OUTPUT->send();

