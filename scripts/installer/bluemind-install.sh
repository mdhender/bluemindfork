#!/bin/bash
#BEGIN LICENSE
#
# Copyright © Blue Mind SAS, 2012-2016
#
# This file is part of BlueMind. BlueMind is a messaging and collaborative
# solution.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of either the GNU Affero General Public License as
# published by the Free Software Foundation (version 3 of the License).
#
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
#
# See LICENSE.txt
#
#END LICENSE


BLUEMIND_INSTALLER=`ps -ocommand= -p $PPID | awk -F/ '{print $NF}' | awk '{print $1}'`
INSTALLER_DIR=`dirname $0`

pushd ${INSTALLER_DIR} > /dev/null 2>&1

source "include/path"
source "lang/output-common"
source "include/os-common"

ending() {
    if (($FAILURE)); then
        display_message "${txt_footer_msg}: ${BLUEMIND_INSTALLER_LOG}"
    fi
}
trap ending EXIT

sanity() {
    if [ ${EUID} -ne 0 ]; then
        display_error "${txt_running_user_error}"
        exit 1
    fi
    
    if [ ! -d ${PKGS} ]; then
        display_error "${txt_extracted_pkgs_not_found}"
        exit 1
    fi

    architecture=`uname -m`
    if [ "${architecture}" != "x86_64" ]; then
        display_error "${txt_invalid_architecture}"
        exit 1
    fi

    hostname=`hostname -s`
    if [[ ${hostname} =~ "." ]]; then
        display_error "${txt_deb_invalid_hostname}" ${hostname}
        exit 1
    fi
    
    hostname=`hostname -f`
    if [[ ! ${hostname} == *.* ]]; then
        display_error "${txt_deb_invalid_fqdn}" ${hostname}
        exit 1
    fi
    
    utf8=`locale -a|grep -i -m 1 en_us.utf8`
    if [ $? -ne 0 ]; then
        display_error "${txt_utf8_not_found}"
        exit 1
    else
        display_message "${txt_utf8_found}" ${utf8}
	    export LANG="en_US.UTF8"
	    export LC_ALL="en_US.UTF8"
    fi
}

sanity_install() {
    if [ -e ${INSTALL_PKGS_TO} ]; then
        display_error "${txt_upgrade_unavailable}"
        exit 1
    fi
    
    if [ -e /etc/sudoers ] \
        && which sudo > /dev/null 2>&1 \
        && ! su -l root -c "sudo echo '' > /dev/null"; then
        display_error "${txt_disable_sudo_requiretty}"
        exit 1
    fi

    # Check local package install (2 GiB)
    # Don't check INSTALL_PKGS_TO because df will crash if the
    # folder does not exists.
    display_message "${txt_diskspace_label}" "/var/spool"
    if ! check_diskfree "/var/spool" $((2 * 1024 * 1024)); then
      # Don't use whitespaces in the label
      display_error "${txt_diskspace_error}" "2GiB"  "/var/spool"
      exit 1
    fi
    # Check /usr (8 GiB)
    display_message "${txt_diskspace_label}" "/usr"
    if ! check_diskfree "/usr" $((8 * 1024 * 1024)); then
      display_error "${txt_diskspace_error}" "8GiB" "/usr"
      exit 1
    fi

    os_sanity_install
}

check_install() {
  display_message "${txt_check_intall}"
  
  count=0
  success=1

  ports="80 443 5432 8021 8080"
  while [ ${count} -lt 3 ] && [ ${success} != 0 ]; do
    count=$((count+1))

    sleep 5
    for i in ${ports}; do
      exec 3<>/dev/tcp/127.0.0.1/$i 2> /dev/null
      ret=$?

      if [ ${ret} -ne 0 ]; then
        success=${i}
        break;
      fi

      success=0
    done
  done

  if [ ${success} -ne 0 ]; then
    display_error "${txt_fail_to_connect}: ${success}"
    display_error "${txt_fail_to_install}"
    exit 1
  fi
}

install() {
    install_prepare_system
    install_local_repository
    if [ -z ${local_repo_only} ]; then
      install_bluemind-full
    fi
    
    check_install
    display_success
}

read_opts() {
  while getopts ":nr" opt; do
    case $opt in
      n)
        interactive="n"
        ;;
      r)
        local_repo_only="y"
        ;;
      \?)
        display_error "${txt_invalid_error}: -%s" ${OPTARG}
        display_message "\t-n: ${txt_non_interactive}"
        display_message "\t-r: ${txt_local_repo_only}"
        exit 1
        ;;
    esac
  done
}

read_opts $*

display_welcome

detect_os

sanity

sanity_install
install
