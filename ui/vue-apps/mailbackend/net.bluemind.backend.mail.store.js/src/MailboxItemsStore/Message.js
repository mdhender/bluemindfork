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
import { EmailExtractor, Flag } from "@bluemind/email";
import { RecipientKind } from "@bluemind/backend.mail.api";
import GetAttachmentPartsVisitor from "./GetAttachmentPartsVisitor";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import injector from "@bluemind/inject";
import TreeWalker from "./TreeWalker";

/**
 * Holds data and methods for displaying a mail message and respond to it.
 *
 * @see net.bluemind.backend.mail.api.MailboxItem.java
 */
export default class Message {
    constructor(key, item) {
        this.actions = {
            REPLY: "reply",
            REPLYALL: "replyAll",
            FORWARD: "forward"
        };

        this.recipientFields = {
            TO: "to",
            CC: "cc"
        };

        this.recipientHeaders = {
            MAIL_FOLLOWUP_TO: "Mail-Followup-To",
            MAIL_REPLY_TO: "Mail-Reply-To",
            REPLY_TO: "Reply-To"
        };

        if (item.value) {
            fromMailboxItem(item, this);
        } else {
            Object.assign(this, item);
        }
        this.key = key;
        //FIXME : 1 - The user session should not be a requirement here!
        //FIXME : 2 - Storing the whole user session in a serialized object is a bad idea...
        this.userSession = injector.getProvider("UserSession").get();
    }

    toMailboxItem(sender, senderName, isSeen, structure) {
        let mailboxItem = {
            body: {
                subject: this.subject,
                headers: this.headers,
                recipients: buildRecipients(sender, senderName, this),
                messageId: this.messageId,
                references: this.references,
                structure
            },
            flags: isSeen ? [Flag.SEEN] : []
        };
        return mailboxItem;
    }

    /**
     * Compute parts (inline and attachment)
     *
     * @see GetInlinePartsVisitor
     * @see GetAttachmentPartsVisitor
     *
     */
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

    /**
     * Compute the list of recipients depending on the action (reply, reply all...) and the kind of recipient field we
     *  want to fill (cc, to).
     */
    computeRecipients(targetField = this.recipientFields.TO, action = this.actions.REPLY) {
        switch (targetField) {
            case this.recipientFields.CC:
                return computeRecipientsCC(action, this);
            case this.recipientFields.TO:
                return computeRecipientsTO(action, this);
            default:
                return [];
        }
    }

    isEmpty() {
        return (
            (!this.to || !this.to.length) &&
            (!this.cc || !this.cc.length) &&
            (!this.bcc || !this.bcc.length) &&
            !this.subject &&
            isEmptyContent(this.content)
        );
    }

    hasRecipient() {
        return (this.to && this.to.length) || (this.cc && this.cc.length) || (this.bcc && this.bcc.length);
    }
}

function isEmptyContent(content) {
    const consideredAsEmptyRegex = /^<div>(<br>)*<\/div>$/;
    return !content || consideredAsEmptyRegex.test(content);
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
        isEmpty: !mailboxItem.body.headers.map(header => header.name).includes("X-BM-Event")
    };

    if (!message.ics.isEmpty) {
        const icsHeaderValue = mailboxItem.body.headers.find(header => header.name === "X-BM-Event").values[0];
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

function buildRecipients(sender, senderName, message) {
    const primaries = buildRecipientsForKind(RecipientKind.Primary, message.to);
    const carbonCopies = buildRecipientsForKind(RecipientKind.CarbonCopy, message.cc);
    const blindCarbonCopies = buildRecipientsForKind(RecipientKind.BlindCarbonCopy, message.bcc);
    const originator = [
        {
            kind: RecipientKind.Originator,
            address: sender,
            dn: senderName
        }
    ];

    return primaries
        .concat(carbonCopies)
        .concat(blindCarbonCopies)
        .concat(originator);
}

function buildRecipientsForKind(kind, addresses) {
    return (addresses || []).map(address => {
        return {
            kind: kind,
            address: address,
            dn: "" // FIXME should provide the displayed name here
        };
    });
}

/**
 * Compute the list of recipients depending on the action (reply, reply all...) and the 'Cc' recipient field.
 */
function computeRecipientsCC(action, message) {
    if (action === message.actions.FORWARD) {
        return [];
    }

    if (action === message.actions.REPLYALL) {
        const mailFollowUpTo = message.headers.find(
            header => header.name === message.recipientHeaders.MAIL_FOLLOWUP_TO
        );
        if (!mailFollowUpTo) {
            return message.cc.map(cc => cc.address);
        }
    }
    return [];
}

/**
 * Compute the list of recipients depending on the action (reply, reply all...) and the 'To' recipient field.
 */
function computeRecipientsTO(action, message) {
    if (action === message.actions.FORWARD) {
        return [];
    }

    const isReplyAll = action === message.actions.REPLYALL;

    if (isReplyAll) {
        const mailFollowUpTo = header(message.recipientHeaders.MAIL_FOLLOWUP_TO, message);
        if (mailFollowUpTo) {
            return addressesFromHeader(mailFollowUpTo, true);
        }
    }

    const mailReplyToHeader = header(message.recipientHeaders.MAIL_REPLY_TO, message);
    if (mailReplyToHeader) {
        return addressesFromHeader(mailReplyToHeader, isReplyAll);
    }

    const replyToHeader = header(message.recipientHeaders.REPLY_TO, message);
    if (replyToHeader) {
        return addressesFromHeader(replyToHeader, isReplyAll);
    }

    // compute recipients from "From" or "To"
    let recipients = [message.from.address];
    const myEmail = message.userSession.defaultEmail;
    if (isReplyAll) {
        // respond to sender and all recipients except myself
        recipients.push(...message.to.map(to => to.address));
        recipients = recipients.filter(address => address !== myEmail);
        // avoid duplicates
        recipients = Array.from(new Set(recipients));
        if (recipients.length === 0) {
            // I was alone, respond to myself then
            recipients = [myEmail];
        }
    } else if (recipients.includes(myEmail)) {
        // all recipients except myself
        recipients = message.to.map(to => to.address).filter(address => address !== myEmail);
        if (recipients.length === 0) {
            // I was alone, respond to myself then
            recipients = [myEmail];
        } else {
            // respond to the first "not me" recipient only
            recipients = [recipients[0]];
        }
    }

    return recipients;
}

function header(headerName, message) {
    return message.headers.find(header => header.name === headerName);
}

function addressesFromHeader(header, isReplyAll) {
    if (isReplyAll) {
        return header.values.map(value => EmailExtractor.extractEmail(value));
    } else {
        return [EmailExtractor.extractEmail(header.values[0])];
    }
}
