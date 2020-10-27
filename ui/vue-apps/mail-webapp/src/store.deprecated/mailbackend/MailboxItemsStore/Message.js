/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2019
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";
import { Flag } from "@bluemind/email";

/**
 * Holds data and methods for displaying a mail message and respond to it.
 *
 * @see net.bluemind.backend.mail.api.MailboxItem.java
 */
export default class Message {
    constructor(key, item) {
        if (item.value) {
            fromMailboxItem(item, this);
        } else {
            Object.assign(this, item);
        }
        this.key = key;
    }
}

/** Initialize the given message using the MailboxItem 'item'.  */
function fromMailboxItem(item, message) {
    const mailboxItem = item.value;
    message.subject = mailboxItem.body.subject;
    message.preview = mailboxItem.body.preview;
    message.from = mailboxItem.body.recipients.find(rcpt => rcpt.kind === RecipientKind.Originator);
    message.to = mailboxItem.body.recipients.filter(rcpt => rcpt.kind === RecipientKind.Primary);
    message.cc = mailboxItem.body.recipients.filter(rcpt => rcpt.kind === RecipientKind.CarbonCopy);
    message.bcc = mailboxItem.body.recipients.filter(rcpt => rcpt.kind === RecipientKind.BlindCarbonCopy);
    message.date = new Date(mailboxItem.body.date);
    message.structure = mailboxItem.body.structure;
    message.headers = mailboxItem.body.headers;
    message.messageId = mailboxItem.body.messageId;
    message.references = mailboxItem.body.references;
    message.flags = mailboxItem.flags || [];
    message.states = [];
    message.uid = item.uid;
    message.id = item.internalId;
    message.imapUid = mailboxItem.imapUid;

    if (mailboxItem.body.smartAttach) {
        message.states.push("has-attachment");
    }
    if (message.flags.find(mailboxItemFlag => mailboxItemFlag === Flag.SEEN) === undefined) {
        message.states.push("not-seen");
    }
}
