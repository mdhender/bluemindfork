#!/usr/bin/expect

log_user 0
spawn telnet 127.0.0.1 2400
expect {
    default {
        send_user "Failed to get LMTP banner\n"
        exit 2
    }
    "220" {}
}

send "HELO lmtp-check.tld\n"
expect {
    default {
        send_user "EHLO fail!\n"
        exit 2
    }
    "250" {}
}

send_user "EHLO succeed!\n"
exit 0
