#!/usr/bin/expect

set username [exec echo -n "admin0" | base64]
set password [exec cat /etc/bm/bm-core.tok | base64]

log_user 0
spawn telnet 127.0.0.1 25
expect {
    default {
        send_user "Failed to get SMTP banner\n"
        exit 2
    }
    "220" {}
}

send "ehlo smtp-check.tld\n"
expect {
    default {
        send_user "EHLO fail!\n"
        exit 2
    }
    "250" {}
}

send "AUTH LOGIN\n"
expect {
    default {
        send_user "AUTH fail!\n"
        exit 2
    }
    "334" {}
}

send "$username\n"
expect {
    default {
        send_user "Sending username fail!\n"
        exit 2
    }
    "334" {}
}

send "$password\n"
expect {
    default {
        send_user "SMTP authentication fail!\n"
        exit 2
    }
    "535" {
        send_user "SMTP authentication fail!\n"
        exit 2
    }
    "235" {
        send_user "SMTP authentication success\n"
        exit 0
    }
}
