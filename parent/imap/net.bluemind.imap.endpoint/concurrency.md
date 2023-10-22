# Concurrency with IMAP

- [Concurrency with IMAP](#concurrency-with-imap)
  - [IMAP 101](#imap-101)
    - [Sequences](#sequences)
    - [Flags](#flags)
    - [Expunge](#expunge)
  - [Pipelining](#pipelining)
    - [API calls and locking](#api-calls-and-locking)
      - [Caching](#caching)
  - [Multi Access](#multi-access)
    - [Example](#example)
    - [Implementation](#implementation)
      - [Permanent flags](#permanent-flags)
    - [Notes regarding IDLE command](#notes-regarding-idle-command)
      - [Implementation](#implementation-1)
    - [imaptest](#imaptest)
      - [Building locally](#building-locally)
      - [junit execution](#junit-execution)
      - [Emulate thunderbird activity](#emulate-thunderbird-activity)


## IMAP 101

IMAP loves to play with **sequences**. Sequences are the positions of messages in a mail folder. Messages in imap also have a UID (per-folder monotonically increasing uint32); its sql definition would be "serial primary key". Uid is easy to understand and use, sequences are brain-fucked and error prone.

Messages can hold flags. Most of them are boring labels similar to tags: seen, flagged (starred for a gmail user), answered. One of them is more fun: *deleted*, because its just a flag meaning "do not show me" for an IMAP client.

### Sequences

3 messages are delivered to an empty INBOX

```
email1, sequence 1, uid 1, flags ()
email2, sequence 2, uid 2, flags ()
email3, sequence 3, uid 3, flags ()
```

email2 is "removed" (more on that later), then we have:

```
email1, sequence 1, uid 1, flags ()
email3, sequence 2, uid 3, flags ()
```

email3 has sequence 2 because its the second one the mailbox.

### Flags

Our 2 flagged messages might look like this :


```
email1, sequence 1, uid 1, flags (\seen, $category42$)
email3, sequence 2, uid 3, flags ($TopSecret)
```

Then email1 is flagged as deleted :

```
email1, sequence 1, uid 1, flags (\seen, $category42$, \deleted)
email3, sequence 2, uid 3, flags ($TopSecret)
```

Deleted is just a flag, 2 emails still exist, email1 still has sequence 1. The 'expunge' might occur and mess things up.

### Expunge

Expunge is a command to "commit" the deleted flags and make the messages disappear.

Consider this folder state:
```
email1, sequence 1, uid 1, flags (\seen, $category42$)
email3, sequence 2, uid 3, flags ($TopSecret, \deleted)
```
Then the EXPUNGE command occurs:
```
C: TAG12 EXPUNGE
S: * 2 EXPUNGE
S: TAG12 Ok Completed
```

2 expunge means that the second message is gone (but its uid was 3).

The response MAY also look like :
```
C: TAG12 EXPUNGE
S: * 2 EXPUNGE
S: * 1 EXISTS
S: TAG12 Ok Completed
```

The "sequenceNumber EXPUNGE" is not really a response but is part of what we could call a "checkpoint" process. EXPUNGE drops the deleted messages which alters the positions of the messages in the folder and their count.

EXPUNGE is a command allowed to return checkpoint infos. Most command could return checkpoint infos except FETCH, STORE & SEARCH (UID FETCH, UID STORE and UID SEARCH can checkpoint).

## Pipelining

Multiple commands can be submitted without waiting for a response.
We parse them from a vertx event loop then drop them in the worker pool in ImapCommandHandler.

```java
vertxContext.executeBlocking(promise, boolean, asyncresult);
```
CAN and will reorder stuff because the ordering boolean does not work if you complete the promise from another thread.

### API calls and locking

Our IMAP endpoint can run multiple API calls to execute a command and can't really do that in an atomic way.

As IMAP rely on sequences, you can't really expect that the sequence 1 is still the same uid a few seconds later with concurrent access.

We use 2 marker interfaces to setup locking around sequences : ISequenceReader and ISequenceWriter. Command processors may implement one or the other. EXPUNGE, APPEND and COPY will implement ISequenceWriter as they create or remove stuff in the sequence.

ImapCommandHandler will look if the command processor implements Writer to grab a writeLock for the target folder uid. Read locks will be taken when a processor relies on consistent sequence numbers.

This provides a limited safety regarding modifications made from multiple imap connections and makes our processors a bit more atomic, but this is not enough as mutations could occur from another protocol.

#### Caching

As nothing on the database side tracks message positions, we have to remember the sequence/uid mapping when folders are selected. When a checkpoint capable command runs, we update our cache sequences (SelectedMessage[] inside the SelectedFolder class).

## Multi Access

### Example

Given 2 two connections:

```
C1: 1.1 SELECT INBOX
S1: * 12 EXISTS
S1: 1.1 Ok [read-write] completed

                                        C2: 2.1 SELECT INBOX
                                        S2: * 12 EXISTS
                                        S2: 2.1 Ok completed
C1: STORE 1 +flags.silent \deleted
C1: EXPUNGE
                                        C2: 2.2 APPEND INBOX ...
                                        S2: ...
                                        S2: 2.2 OK [APPENDUID 21 1234] Yeah
                                        C2: 2.3 FETCH 12 (uid flags)
```

C1 deletes and expunge the first sequence in the mailbox while C2 was doing nothing.
The C2 decides to APPEND a fresh message into inbox (the selected folder). Without further infos, C2 will assume that sequence 13 is 1234.

There are multi ways to manage that but the easiest is to given checkpoint infos as often as possible. APPEND is a synchronization point (aka not fetch, store or search), so it is allowed to return EXPUNGE, EXISTS and FLAGS tokens.

A valid set of responses COULD look like this (the RFCs always says "MAY" return EXPUNGE responses):

```
C1: EXPUNGE
S1: * 1 EXPUNGE
                                        C2: 2.2 APPEND INBOX ...
                                        S2: * 1 EXPUNGE
                                        S2: * 12 EXISTS
                                        S2: 2.2 OK [APPENDUID 21 1234]
                                        C2: 2.3 FETCH 12 (uid flags)
                                        S2: * 12 FETCH (UID 1234 FLAGS())
```

### Implementation

To implement this ISequenceWriter|Reader is combined with ISequenceCheckpoint and the SelectedMessage[] cached list of message. 

We compute, while holding read (or write) locks on the folder what the refreshed version of the SelectedFolder would look like. We can compute which UIDs are removed and provide "* seq EXPUNGE" responses. The we can output the new EXISTS count safely and replace the SelectedFolder with the live version.

#### Permanent flags

Per folder unique permanent flags (eg. $TopSecret) are also tracked in the same way. We use the IFlagsCheckpoint interface for commands that can return " * FLAGS (... $TopSecret) " before running. For exemple FETCH is allowed to IFlagsCheckpoint but not alter sequences. Before returning never-seen before flags, we need to checkpoint the flags that a STORE command from another connection might have created. 

### Notes regarding IDLE command

IDLE is some sort of long polling for the IMAP protocol. It is a complexing beast as it can checkpoint as many times as necessary.

When IDLE starts we echo the +idling continuation AND issue a checkpoint as thunderbird sometimes rely on that trigger an instant checkpoint.

#### Implementation

As IDLE is running for a long time it cannot grab a read lock for its whole execution as it would deadlock all writers.

For now we duplicate some of the locking logic of ImapCommandHandle when our registered consumer receives changes. IDLE response writing was moved to endpoint bundle instead of mailapi.driver to keeping all the locking stuff with all the processors.

### imaptest

#### Building locally

```shell
#!/bin/bash

base=$(dirname $0)
base=$(realpath $base)
rm -fr *.gz dovecot-latest imaptest-latest

wget http://dovecot.org/nightly/dovecot-latest.tar.gz
wget http://dovecot.org/nightly/imaptest/imaptest-latest.tar.gz

tar xf dovecot-latest.tar.gz && mv dovecot-0.0.0-* dovecot-latest
tar xf imaptest-latest.tar.gz && mv dovecot-*-imaptest-* imaptest-latest



pushd dovecot-latest
./configure --without-shared-libs && make -j16
popd

pushd imaptest-latest
./configure --with-dovecot=${base}/dovecot-latest && make -j16 && make install
popd
```

#### junit execution

FullPlanTests uses docker to exercise our code with imaptest directly. It runs the a 2min hammering session with 10 concurrent users playing with the same folder. This test works locally and on CI with this branch.

#### Emulate thunderbird activity

This one requires a configuration profile profile.conf. This profile is usable against imap-endpoint.product running in eclipse for quick testing.

```
# Port number to use for LMTP. The host is assumed to be the same as for IMAP.
lmtp_port = 2400
# Maximum number of concurrent LMTP connections.
lmtp_max_parallel_count = 10

# Total number of users used for the test. This is divided between user {}
# according to their count=n% settings.
total_user_count = 2
# Spread the initial connections at startup equally to this time period.
# This way there's not a huge connection spike at startup that overloads
# the server.
rampup_time = 4s

##
## Users
##

# User profiles describe how the users are expected to behave. There can be
# one or more user profiles.

user aggressive {
  # Username template format. %n expands to the user index number.
  username_format = test%n@devenv.blue
  # The first index number to use for users in this profile. Usually different
  # user profiles should either not overlap or overlap only partially (to
  # describe users who have different behaviors with different clients).
  #username_start_index = 1
  # It's possible to give the list of usernames from a file. Each line in the
  # file contains either "username" or "username:password". If password isn't
  # specified, the global password is used. This setting overrides
  # username_format and username_start_index settings.
  #userfile = 
  # Percentage of total_user_count to assign for this user profile.
  count = 100%

  # How long the IMAP connection is kept open before disconnecting.
  mail_session_length = 3 min

  # How often emails are delivered to INBOX
  mail_inbox_delivery_interval = 10s
  # How often emails are delivered to Spam
  mail_spam_delivery_interval = 5s

  # How quickly user acts on an incoming email. This is calculated from the
  # time the user's IMAP connection has seen the new message and FETCHed its
  # metadata. This may be a long time after the actual mail delivery in case
  # all users don't have active IMAP connections all the time.
  mail_action_delay = 2s
  # After the initial action, how quickly is the next action performed.
  mail_action_repeat_delay = 1s
  # Likelyhood of incoming mail being moved to Spam mailbox immediately when
  # noticed by the IMAP client. mail_action_delay won't affect this.
  mail_inbox_move_filter_percentage = 10

  # How often are outgoing mails sent. The mail is initially written to the
  # Drafts mailbox, and after mail_write_duration it's written to the Sent
  # mailbox and deleted from Drafts.
  mail_send_interval = 10s
  mail_write_duration = 5s

  # Below percentages describe the likelyhood of mail actions being performed
  # for incoming mails. The actions are performed in the given order and
  # multiple actions can be performed on the same mail.

  # Mail is marked as \Deleted and UID EXPUNGEd
  mail_inbox_delete_percentage = 5
  # Mail is moved to Spam
  mail_inbox_move_percentage = 5
  # Mail is replied to: APPEND via Drafts and Sent mailboxes and add
  # \Answered flag
  mail_inbox_reply_percentage = 50
}

user normal {
  username_format = test%n@devenv.blue
  #username_start_index = 500
  #userfile = 
  count = 0%

  mail_inbox_delivery_interval = 5 min
  mail_spam_delivery_interval = 3 min
  mail_action_delay = 3 min
  mail_action_repeat_delay = 10s
  mail_session_length = 20 min

  mail_send_interval = 10 min
  mail_write_duration = 2 min

  mail_inbox_reply_percentage = 50
  mail_inbox_delete_percentage = 5
  mail_inbox_move_percentage = 5
  mail_inbox_move_filter_percentage = 10
}

##
## Clients
##

# Client profiles describe how the emulated clients are expected to behave.

client Thunderbird {
  count = 100%
  connection_max_count = 5
  imap_idle = yes
  imap_fetch_immediate = UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (From To Cc Bcc Subject Date Message-ID Priority X-Priority References Newsgroups In-Reply-To Content-Type)]
  imap_fetch_manual = RFC822.SIZE BODY[]
  imap_status_interval = 5 min
}

client AppleMail {
  count = 0%
  connection_max_count = 5
  imap_idle = yes
  imap_fetch_immediate = INTERNALDATE UID RFC822.SIZE FLAGS BODY.PEEK[HEADER.FIELDS (date subject from to cc message-id in-reply-to references x-priority x-uniform-type-identifier x-universally-unique-identifier)] MODSEQ
  imap_fetch_manual = BODYSTRUCTURE BODY.PEEK[]
  imap_status_interval = 5 min
}

```

imaptest with a local build is then started like this:

```
#!/bin/bash

exec ./imaptest-latest/src/imaptest \
    pass=test \
    mbox=dovecot-crlf \
    host=127.0.0.1 port=1143 \
    profile=profile.conf \
    clients=2 secs=30
```

This profile should also run correctly without error.
