<?php

/**
 * ContextMenu
 *
 * Plugin to add a context menu to the message list
 *
 * @version @package_version@
 * @author Philip Weir
 */
class contextmenu extends rcube_plugin
{
	public $task = 'mail|addressbook';

	function init()
	{
		$rcmail = rcmail::get_instance();
		if ($rcmail->task == 'mail' && ($rcmail->action == '' || $rcmail->action == 'show'))
			$this->add_hook('render_mailboxlist', array($this, 'show_mailbox_menu'));
		elseif ($rcmail->task == 'mail' && $rcmail->action == 'compose')
			$this->add_hook('addressbooks_list', array($this, 'show_compose_menu'));
		elseif ($rcmail->task == 'addressbook' && $rcmail->action == '')
			$this->add_hook('addressbooks_list', array($this, 'show_addressbook_menu'));

		$this->register_action('plugin.contextmenu.messagecount', array($this, 'messagecount'));
		$this->register_action('plugin.contextmenu.readfolder', array($this, 'readfolder'));
	}

	public function messagecount()
	{
		$mbox = get_input_value('_mbox', RCUBE_INPUT_GET);
		$this->api->output->set_env('messagecount', rcmail::get_instance()->storage->count($mbox));
		$this->api->output->send();
	}

	public function readfolder()
	{
		$storage = rcmail::get_instance()->storage;
		$cbox = get_input_value('_cur', RCUBE_INPUT_POST);
		$mbox = get_input_value('_mbox', RCUBE_INPUT_POST);
		$oact = get_input_value('_oact', RCUBE_INPUT_POST);

		$uids = $storage->search_once($mbox, 'ALL UNSEEN', true);

		if ($uids->is_empty())
			return false;

		if ($cbox == $mbox && $oact == '') {
			$this->api->output->command('toggle_read_status', 'read', $uids->get());
			$this->api->output->send();
		} else {
			$storage->set_flag($uids->get(), 'SEEN', $mbox);
			rcmail_send_unread_count($mbox, true);
		}
	}

	public function show_mailbox_menu($args)
	{
		$rcmail = rcmail::get_instance();
		$this->add_texts('localization/');
		$rcmail->output->add_label('nomessagesfound');
		$this->include_script('jquery.contextmenu.min.js');
		$this->include_script('contextmenu.js');

		$this->include_stylesheet($this->local_skin_path() . '/contextmenu.css');
		if ($rcmail->output->browser->ie && $rcmail->output->browser->ver == 6)
			$this->include_stylesheet($this->local_skin_path() . '/ie6hacks.css');

		$out = '';

		// message list menu
		if ($rcmail->action == '') {
			$li = '';

			$li .= html::tag('li', array('class' => 'conmentitle'), html::span(null, Q($this->gettext('markmessages'))));
			$li .= html::tag('li', array('class' => 'markmessage'), html::a(array('href' => "#read", 'class' => 'active'), html::span(array('class' => 'fa fa-circle-o'), Q($this->gettext('markread')))));
			$li .= html::tag('li', array('class' => 'markmessage'), html::a(array('href' => "#unread", 'class' => 'active'), html::span(array('class' => 'fa fa-circle'), Q($this->gettext('markunread')))));
			$li .= html::tag('li', array('class' => 'markmessage'), html::a(array('href' => "#flagged", 'class' => 'active'), html::span(array('class' => 'fa fa-flag'), Q($this->gettext('markflagged')))));
			$li .= html::tag('li', array('class' => 'markmessage'), html::a(array('href' => "#unflagged", 'class' => 'active'), html::span(array('class' => 'fa fa-flag-o'), Q($this->gettext('markunflagged')))));
			$li .= html::tag('li', array('class' => 'topreply'), html::a(array('href' => "#reply", 'class' => 'active'), html::span(array('class' => 'fa fa-reply'), Q($this->gettext('replytomessage')))));

			$lis = '';
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#reply-all", 'class' => 'active'), html::span(array('class' => 'fa fa-reply-all'), Q($this->gettext('replytoallmessage')))));
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#reply-list", 'class' => 'active'), html::span(array('class' => 'fa fa-reply-all'), Q($this->gettext('replylist')))));
			$li .= html::tag('li', array('class' => 'submenu replyacts'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('reply'))) . html::tag('ul', array('class' => 'popupmenu toolbarmenu replyacts'), $lis));

			$lis = '';
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#forward", 'class' => 'active'), html::span(array('class' => 'fa fa-long-arrow-right'), Q($this->gettext('forwardinline')))));
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#forward-attachment", 'class' => 'active'), html::span(array('class' => 'fa fa-long-arrow-right'), Q($this->gettext('forwardattachment')))));
			$li .= html::tag('li', array('class' => 'submenu forwardacts'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('forward'))) . html::tag('ul', array('class' => 'popupmenu toolbarmenu forwardacts'), $lis));

			//$rcmail = get_instance();
			//if (!$rcmail->config->get('flag_for_deletion', false) && $rcmail->config->get('trash_mbox') && $_SESSION['mbox'] != $rcmail->config->get('trash_mbox'))
			//	$li .= html::tag('li', array('class' => 'delete separator_below'), html::a(array('href' => "#delete", 'id' => 'rcm_delete', 'class' => 'active'), html::span(null, Q($this->gettext('movemessagetotrash')))));
			//else
			$li .= html::tag('li', array('class' => 'separator_below'), html::a(array('href' => "#delete", 'id' => 'rcm_delete', 'class' => 'active'), html::span(array('class' => 'fa fa-trash'), Q($this->gettext('movemessagetotrash')))));

			$li .= html::tag('li', array('class' => 'submenu moveto'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('moveto'))) . $this->_gen_folder_list($args['list'], '#moveto'));
			$li .= html::tag('li', array('class' => 'submenu copy'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('copyto'))) . $this->_gen_folder_list($args['list'], '#copy'));

			$lis = '';
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#print", 'class' => 'active'), html::span(array('class' => 'fa fa-print'), Q($this->gettext('printmessage')))));
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#download", 'class' => 'active'), html::span(array('class' => 'fa fa-download'), Q($this->gettext('emlsave')))));
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#edit", 'class' => 'active'), html::span(array('class' => 'fa fa-pencil'), Q($this->gettext('editasnew')))));
			$lis .= html::tag('li', array('class' => 'separator_below'), html::a(array('href' => "#viewsource", 'class' => 'active'), html::span(array('class' => 'fa fa-code'), Q($this->gettext('viewsource')))));
			$lis .= html::tag('li', array('class' => ''), html::a(array('href' => "#open", 'id' => 'rcm_open', 'class' => 'active'), html::span(array('class' => 'fa fa-external-link-square'), Q($this->gettext('openinextwin')))));
			$li .= html::tag('li', array('class' => 'submenu moreacts'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('moreactions'))) . html::tag('ul', array('class' => 'popupmenu toolbarmenu moreacts'), $lis));
			$out .= html::tag('ul', array('id' => 'rcmContextMenu', 'class' => 'rcmcontextmenu popupmenu toolbarmenu'), $li);
		}

		// folder list menu
		$li = '';

		$li .= html::tag('li', array('class' => 'readfolder separator_below'), html::a(array('href' => "#readfolder", 'class' => 'active'), html::span(array('class' => 'fa fa-circle-o'), Q($this->gettext('markreadfolder')))));

		$li .= html::tag('li', array('class' => 'expunge'), html::a(array('href' => "#expunge", 'class' => 'active'), html::span(null, Q($this->gettext('compact')))));
		$li .= html::tag('li', array('class' => 'purge separator_below'), html::a(array('href' => "#purge", 'class' => 'active'), html::span(null, Q($this->gettext('empty')))));

		$li .= html::tag('li', array('class' => 'collapseall'), html::a(array('href' => "#collapseall", 'class' => 'active'), html::span(array('class' => 'fa fa-minus-square-o'), Q($this->gettext('collapseall')))));
		$li .= html::tag('li', array('class' => 'expandall separator_below'), html::a(array('href' => "#expandall", 'class' => 'active'), html::span(array('class' => 'fa fa-minus-square-o'), Q($this->gettext('expandall')))));

		$li .= html::tag('li', array('class' => 'openfolder'), html::a(array('href' => "#openfolder", 'id' => 'rcm_openfolder', 'class' => 'active'), html::span(array('class' => 'fa fa-external-link'), Q($this->gettext('openinextwin')))));

		$out .= html::tag('ul', array('id' => 'rcmFolderMenu', 'class' => 'rcmcontextmenu popupmenu toolbarmenu'), $li);

		$this->api->output->add_footer(html::div(null , $out));

		if ($rcmail->action == 'show')
			$this->api->output->set_env('delimiter', $rcmail->storage->get_hierarchy_delimiter());

		// remove hook to prevent double execution
		$this->remove_hook('render_mailboxlist', array($this, 'show_mailbox_menu'));
	}

	public function show_addressbook_menu($args)
	{
		$rcmail = rcmail::get_instance();
		$this->add_texts('localization/');
		$this->include_script('jquery.contextmenu.min.js');
		$this->include_stylesheet($this->local_skin_path() . '/contextmenu.css');
		$this->include_script('contextmenu.js');
		$out = '';

		// contact list menu
		$li = '';

		$li .= html::tag('li', array('class' => 'composeto separator_below'), html::a(array('href' => "#compose", 'class' => 'active'), html::span(null, Q($this->gettext('composeto')))));

		$li .= html::tag('li', array('class' => 'editcontact'), html::a(array('href' => "#edit", 'class' => 'active'), html::span(null, Q($this->gettext('editcontact')))));
		$li .= html::tag('li', array('class' => 'deletecontact'), html::a(array('href' => "#delete", 'class' => 'active'), html::span(null, Q($this->gettext('deletecontact')))));
		$li .= html::tag('li', array('class' => 'removefromgroup'), html::a(array('href' => "#group-remove-selected", 'class' => 'active'), html::span(null, Q($this->gettext('groupremoveselected')))));

		if ($lis = $this->_gen_addressbooks_list($args['sources'], '#copy'))
			$li .= html::tag('li', array('class' => 'submenu separator_above'), html::span(null, Q($this->gettext('copyto'))) . $lis);

		$lis = '';
		$lis .= html::tag('li', array('class' => 'exportall'), html::a(array('href' => "#export", 'class' => 'active'), html::span(null, Q($this->gettext('exportall')))));
		$lis .= html::tag('li', array('class' => 'exportsel'), html::a(array('href' => "#export-selected", 'class' => 'active'), html::span(null, Q($this->gettext('exportsel')))));
		$li .= html::tag('li', array('class' => 'submenu exportacts'), html::span(null, html::span('fa fa-caret-right expand', '&nbsp;') . Q($this->gettext('export'))) . html::tag('ul', array('class' => 'popupmenu toolbarmenu exportacts'), $lis));

		$out .= html::tag('ul', array('id' => 'rcmAddressMenu', 'class' => 'rcmcontextmenu popupmenu toolbarmenu'), $li);

		// contact group menu
		$li = '';

		$li .= html::tag('li', array('class' => 'groupcreate'), html::a(array('href' => "#group-create", 'class' => 'active'), html::span(null, Q($this->gettext('newcontactgroup')))));
		$li .= html::tag('li', array('class' => 'grouprename'), html::a(array('href' => "#group-rename", 'class' => 'active'), html::span(null, Q($this->gettext('grouprename')))));
		$li .= html::tag('li', array('class' => 'groupdelete'), html::a(array('href' => "#group-delete", 'class' => 'active'), html::span(null, Q($this->gettext('groupdelete')))));

		$out .= html::tag('ul', array('id' => 'rcmGroupMenu', 'class' => 'rcmcontextmenu popupmenu toolbarmenu'), $li);

		$this->api->output->add_footer(html::div(null , $out));

		// remove hook to prevent double execution
		$this->remove_hook('addressbooks_list', array($this, 'show_addressbook_menu'));
	}

	public function show_compose_menu($args)
	{
		$rcmail = rcmail::get_instance();
		$this->add_texts('localization/');
		$this->include_script('jquery.contextmenu.min.js');
		$this->include_stylesheet($this->local_skin_path() . '/contextmenu.css');
		$this->include_script('contextmenu.js');
		$out = '';

		// contact list menu
		$li = '';

		$li .= html::tag('li', array('class' => 'compseaddto'), html::a(array('href' => "#add-recipient-to", 'class' => 'active'), html::span(null, Q($this->gettext('addcontactto')))));
		$li .= html::tag('li', array('class' => 'compseaddcc'), html::a(array('href' => "#add-recipient-cc", 'class' => 'active'), html::span(null, Q($this->gettext('addcontactcc')))));
		$li .= html::tag('li', array('class' => 'compseaddbcc'), html::a(array('href' => "#add-recipient-bcc", 'class' => 'active'), html::span(null, Q($this->gettext('addcontactbcc')))));

		$out .= html::tag('ul', array('id' => 'rcmComposeMenu', 'class' => 'rcmcontextmenu popupmenu toolbarmenu'), $li);

		$this->api->output->add_footer(html::div(null , $out));

		// remove hook to prevent double execution
		$this->remove_hook('addressbooks_list', array($this, 'show_compose_menu'));
	}

	// based on rcmail->render_folder_tree_html()
	private function _gen_folder_list($arrFolders, $command)
	{
		if (!$this->folderList) {
			$this->template = html::tag('li', array(), 
				html::a(
					array('href' => '%1$s', 
						'onclick' => "rcm_set_dest_folder('%2\$s')",
						'class' => 'active', 
						'style' => 'padding-left: %3$spx',
						'nofaspan' => true),
					html::span(
						array(
							'class' => 'fa %4$s',
							'nofaspan' => true
						), '' 
					).'%5$s'
				)
			);
			$this->classes = array('inbox' => 'fa-envelope', 'drafts'=> 'fa-pencil', 'sent' => 'fa-paper-plane', 'outbox' => 'fa-paper-plane-o',  'junk' => 'fa-fire', 'trash' => 'fa-trash');
			$folderTotal = 0;
			$this->folderList = $this->generate_tree_node($arrFolders, 0, $folderTotal);
			if ($folderTotal > 10) {
				$this->folderList = html::tag('ul', array('class' => 'popupmenu toolbarmenu folders scrollmenu'), $this->folderList);
			} else {
				$this->folderList = html::tag('ul', array('class' => 'popupmenu toolbarmenu folders'), $this->folderList);
			}
		}


	 	return sprintf($this->folderList, $command);
	}

	private function generate_tree_node($arrFolders, $nestLevel = 0, &$folderTotal = 0) {

		$out = '';
		foreach ($arrFolders as $key => $folder) {
			if ($folder['class']) {
				$foldername = rcube_label($folder['class']);
			  } else {
				$foldername = $folder['name'];
			}

			// make folder name safe for ids and class names
			if ($folder['virtual']) {
				$class = 'fa-inbox';
			} elseif (!empty($folder['folders'])) {
				$class = 'fa-folder-open';
			} else {
				$class = $folder['class'] ? $this->classes[$folder['class']] : 'fa-folder';
			}


			// $out .= sprintf($this->template, '%1$s', JQ($folder['id']), $nestLevel*16, $class, Q($foldername));

			$out .= sprintf($this->template, '%1$s', addslashes($folder['id']), $nestLevel*16, $class, ($foldername));

			if (!empty($folder['folders']))
				$out .= $this->generate_tree_node($folder['folders'], $nestLevel+1, $folderTotal);

			$folderTotal++;
		}


		return $out;
	}

	// based on rcmail_directory_list()
	private function _gen_addressbooks_list($arrBooks, $command, $nestLevel = 0, &$folderTotal = 0)
	{
		$rcmail = rcmail::get_instance();
		$groupTotal = 0;
		$maxlength = 35;
		$maxlength_grp = 33;
		$out = '';
		// address books
		foreach ($arrBooks as $j => $source) {
			$title = null;
			$id = $source['id'] ? $source['id'] : $j;
			$bookname = !empty($source['name']) ? Q($source['name']) : Q($id);

			// shorten the address book name to a given length
			if ($maxlength && $maxlength > 1) {
				$bname = abbreviate_string($bookname, $maxlength);

				if ($bname != $bookname)
					$title = $bookname;

				$bookname = $bname;
			}

			if ($source['readonly'])
				$out .= html::tag('li', array('id' => 'rcm_contextaddr_' . $id, 'class' => 'addressbook disabled'), html::a(array('href' => $command, 'id' => 'rcm_contextgrps_'. JQ($id), 'onclick' => "rcm_set_dest_book('" . JQ($id) ."', '" . JQ($id) ."', null)", 'class' => 'active', 'title' => $title, 'style' => 'padding-left: 0'), html::span(null, Q($bookname))));
			else
				$out .= html::tag('li', array('id' => 'rcm_contextaddr_' . $id, 'class' => 'addressbook'), html::a(array('href' => $command, 'id' => 'rcm_contextgrps_'. JQ($id), 'onclick' => "rcm_set_dest_book('" . JQ($id) ."', '" . JQ($id) ."', null)", 'class' => 'active', 'title' => $title, 'style' => 'padding-left: 0'), html::span(null, Q($bookname))));

			// contact groups
			if ($source['groups']) {
				$groups = $rcmail->get_address_book($source['id'])->list_groups();
				foreach ($groups as $group) {
					$title = null;
					$gid = 'G' . html_identifier($id . $group['ID']);
					$groupname = !empty($group['name']) ? Q($group['name']) : Q($gid);

					// shorten the address book name to a given length
					if ($maxlength_grp && $maxlength_grp > 1) {
						$gname = abbreviate_string($groupname, $maxlength_grp);

						if ($gname != $groupname)
							$title = $groupname;

						$groupname = $gname;
					}

					if ($source['readonly'])
						$out .= html::tag('li', array('class' => 'contactgroup disabled'), html::a(array('href' => $command, 'id' => 'rcm_contextgrps_'. JQ($gid), 'onclick' => "rcm_set_dest_book('" . JQ($gid) . "', '" . JQ($id) . "', '" . JQ($group['ID']) ."')", 'class' => 'active', 'title' => $title, 'style' => 'padding-left: 16px'), html::span(null, Q($groupname))));
					else
						$out .= html::tag('li', array('class' => 'contactgroup'), html::a(array('href' => $command, 'id' => 'rcm_contextgrps_'. JQ($gid), 'onclick' => "rcm_set_dest_book('" . JQ($gid) . "', '" . JQ($id) . "', '" . JQ($group['ID']) ."')", 'class' => 'active', 'title' => $title, 'style' => 'padding-left: 16px'), html::span(null, Q($groupname))));

					$groupTotal++;
				}
			}

			$groupTotal++;
		}

		if ($groupTotal > 10) {
			$out = html::tag('ul', array('id' => 'rcm_contextgrps', 'class' => 'popupmenu toolbarmenu folders scrollmenu'), $out);
		}
		else {
			$out = html::tag('ul', array('id' => 'rcm_contextgrps', 'class' => 'popupmenu toolbarmenu folders'), $out);
		}

		return $out;
	}
}

?>
