#!/usr/bin/expect

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

send_user "EHLO succeed!\n"
exit 0
