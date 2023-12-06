# CRP - Central Reverse Proxy

## Postfix

Support for Postfix maps resolution using Kafka CRP topics as source.

CRP Postfix configuration:

```
virtual_transport = error:mailbox does not exist
virtual_mailbox_domains = socketmap:inet:127.0.0.1:25252:domain
virtual_mailbox_maps = socketmap:inet:127.0.0.1:25252:mailbox
virtual_alias_maps = socketmap:inet:127.0.0.1:25252:alias
transport_maps = socketmap:inet:127.0.0.1:25252:transport
recipient_canonical_maps = socketmap:inet:127.0.0.1:25252:srsrecipient
smtpd_milters = inet:127.0.0.1:2500
non_smtpd_milters = inet:127.0.0.1:2500
```
