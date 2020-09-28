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

import GetAttachmentPartsVisitor from "../../../store/messages/helpers/GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "../../../store/messages/helpers/GetInlinePartsVisitor";
import TreeWalker from "../../../store/messages/helpers/TreeWalker";
import { MessageHeader } from "../../../model/message";

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

    /**
     * Compute parts (inline and attachment)
     *
     * @see GetInlinePartsVisitor
     * @see GetAttachmentPartsVisitor
     *
     */
    // DELETE ME once selectMessage is migrate in new store
    computeParts() {
        const inlineVisitor = new GetInlinePartsVisitor();
        const attachmentVisitor = new GetAttachmentPartsVisitor();
        const walker = new TreeWalker(this.structure, [inlineVisitor, attachmentVisitor]);
        walker.walk();
        return {
            inlines: inlineVisitor.result(),
            attachments: attachmentVisitor.result()
        };
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
    // FIXME: move ics object computation into EventHelper
    message.ics = {
        isEmpty: !mailboxItem.body.headers.map(header => header.name).includes(MessageHeader.X_BM_EVENT)
    };

    if (!message.ics.isEmpty) {
        const icsHeaderValue = mailboxItem.body.headers.find(header => header.name === MessageHeader.X_BM_EVENT)
            .values[0];
        message.ics.needsReply = icsHeaderValue.includes('rsvp="true"') || icsHeaderValue.includes("rsvp='true'");
        const semiColonIndex = icsHeaderValue.indexOf(";");
        message.ics.eventUid = semiColonIndex === -1 ? icsHeaderValue : icsHeaderValue.substring(0, semiColonIndex);
    }

    if (!message.ics.isEmpty) {
        message.states.push("is-ics");
    }
    if (mailboxItem.body.smartAttach) {
        message.states.push("has-attachment");
    }
    if (message.flags.find(mailboxItemFlag => mailboxItemFlag === Flag.SEEN) === undefined) {
        message.states.push("not-seen");
    }
}
