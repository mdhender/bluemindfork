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
import { RecipientKind, SystemFlag } from "@bluemind/backend.mail.api";
import GetInlinePartsVisitor from "./GetInlinePartsVisitor";
import TreeWalker from "./TreeWalker";
import { EmailExtractor } from "@bluemind/email";
import injector from "@bluemind/inject";
import BackMailL10N from "@bluemind/backend.mail.l10n";
import { html2text } from "@bluemind/html-utils";

/**
 * Holds data and methods for displaying a mail message and respond to it.
 * 
 * @see net.bluemind.backend.mail.api.MailboxItem.java
 */
export default class Message {

    constructor(item) {
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

        fromMailboxItem(item, this);

        this.userSession = injector.getProvider('UserSession').get();

    }

    toMailboxItem(addrPart, sender, senderName) {
        return {
            body: {
                subject: this.subject,
                headers: this.headers,
                recipients: buildRecipients(sender, senderName, this),
                messageId: this.messageId,
                references: this.references,
                structure: {
                    mime: "text/plain",
                    address: addrPart
                }
            }
        };
    }

    /** 
     * Compute the inline parts keyed by capabilities.
     * 
     * @see GetInlinePartsVisitor
     */
    computeInlineParts() {
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(this.structure, visitor);
        walker.walk();
        return visitor.result();
    }

    /** Compute the subject in function of the current action (like "Re: My Subject" when Reply). */
    computeSubject(action) {
        let subjectPrefix;
        switch (action) {
            case this.actions.REPLY:
            case this.actions.REPLYALL:
                subjectPrefix = getLocalizedProperty(this.userSession, "mail.compose.reply.subject");
                break;
            case this.actions.FORWARD:
                subjectPrefix = getLocalizedProperty(this.userSession, "mail.compose.forward.subject");
                break;
            default:
                break;
        }

        // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
        if (subjectPrefix !== this.subject.substring(0, subjectPrefix.length)) {
            return subjectPrefix + this.subject;
        }
        return this.subject;
    }

    /** 
     * Build the text representing this message as a previous message.
     * Like:
     * @example
     * `On Tuesday 2019 01 01, John Doe wrote:
     * > Dear Jane,
     * >  I could not bear to see you with Tarzan anymore,
     * > it will kill me! Please come back!
     * ...`
     */
    previousMessageContent(action, parts) {
        let previousMessage = "";
        parts.forEach(part => {
            if (part.mime === "text/html") {
                previousMessage += html2text.fromString(part.content);
            } else if (part.mime === "text/plain") {
                previousMessage += part.content;
            }
        });

        let previousMessageSeparator = "";

        switch (action) {
            case this.actions.REPLY:
            case this.actions.REPLYALL:
                previousMessage = previousMessage.split("\n").map(line => "> " + line).join("\n");
                previousMessageSeparator = getLocalizedProperty(this.userSession, "mail.compose.reply.body",
                    { date: this.date, name: nameAndAddress(this.from) });
                break;
            case this.actions.FORWARD:
                previousMessageSeparator = buildPreviousMessageSeparatorForForward(this);
                break;
            default:
                break;
        }

        return previousMessageSeparator + "\n\n" + previousMessage;
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
}

/** Initialize the given message using the MailboxItem 'item'.  */
function fromMailboxItem(item, message) {
    const mailboxItem = item.value;
    message.subject = mailboxItem.body.subject;
    message.preview = mailboxItem.body.preview;
    message.from = mailboxItem.body.recipients.find(
        rcpt => rcpt.kind == RecipientKind.Originator
    );
    message.to = mailboxItem.body.recipients.filter(
        rcpt => rcpt.kind == RecipientKind.Primary
    );
    message.cc = mailboxItem.body.recipients.filter(
        rcpt => rcpt.kind == RecipientKind.CarbonCopy
    );
    message.bcc = mailboxItem.body.recipients.filter(
        rcpt => rcpt.kind == RecipientKind.BlindCarbonCopy
    );
    message.date = new Date(mailboxItem.body.date);
    message.structure = mailboxItem.body.structure;
    message.headers = mailboxItem.body.headers;
    message.messageId = mailboxItem.body.messageId;
    message.references = mailboxItem.body.references;
    message.flags = mailboxItem.systemFlags
        .concat(mailboxItem.otherFlags)
        .map(flag => flag.toLowerCase());
    message.states = [];
    message.uid = item.uid; // FIXME remove me
    message.ids = {
        uid: item.uid,
        imap: mailboxItem.imapUid,
        id: item.internalId
    };

    if (item.value.body.smartAttach) {
        message.states.push("has-attachment");
    }
    if (item.value.systemFlags.indexOf(SystemFlag.seen) === -1) {
        message.states.push("not-seen");
    }
}

function buildRecipients(sender, senderName, message) {
    const primaries = buildRecipientsForKind(RecipientKind.Primary, message.to);
    const carbonCopies = buildRecipientsForKind(RecipientKind.CarbonCopy, message.cc);
    const blindCarbonCopies = buildRecipientsForKind(RecipientKind.BlindCarbonCopy, message.bcc);
    const originator = [{
        kind: RecipientKind.Originator,
        address: sender,
        dn: senderName
    }];

    return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
}

function buildRecipientsForKind(kind, addresses) {
    return (addresses || []).map(address => {
        return {
            kind: kind,
            address: address.address,
            dn: address.dn
        };
    });
}

/** 
 * Compute the list of recipients depending on the action (reply, reply all...) and the 'Cc' recipient field.
 */
function computeRecipientsCC(action, message) {
    if (action == message.actions.FORWARD) {
        return [];
    }

    if (action == message.actions.REPLYALL) {
        const mailFollowUpTo =
            message.headers.find(header => header.name === message.recipientHeaders.MAIL_FOLLOWUP_TO);
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
    if (action == message.actions.FORWARD) {
        return [];
    }

    const isReplyAll = action == message.actions.REPLYALL;

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
        recipients = recipients.filter(address => address != myEmail);
        // avoid duplicates
        recipients = Array.from(new Set(recipients));
        if (recipients.length == 0) {
            // I was alone, respond to myself then
            recipients = [myEmail];
        }
    } else if (recipients.includes(myEmail)) {
        // all recipients except myself
        recipients = message.to.map(to => to.address).filter(address => address != myEmail);
        if (recipients.length == 0) {
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

/**
 * Return the property value localized to the current user.
 * @param {*} userSession the current user session object
 * @param {string} propertyKey the key of the property
 * @param {*} namedParameters optional parameters like: { "date": "2019-09-01", "weather": "sunny" }
 */
function getLocalizedProperty(userSession, propertyKey, namedParameters) {
    // FIXME should use a common tool to translate messages (see '@bluemind/webapp.mail.l10n')
    let property = BackMailL10N[userSession.lang][propertyKey];
    if (namedParameters) {
        namedParameters = new Map(Object.entries(namedParameters));
        namedParameters.forEach((value, key) => property = property.replace(new RegExp("\\{" + key + "\\}"), value));
    }
    return property;
}

/** A separator before the previous message containing basic info. */
function buildPreviousMessageSeparatorForForward(message) {
    let previousMessageSeparator = getLocalizedProperty(message.userSession, "mail.compose.forward.body");
    previousMessageSeparator += "\n";
    previousMessageSeparator += getLocalizedProperty(message.userSession,
        "mail.compose.forward.prev.message.info.subject");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.subject;
    previousMessageSeparator += "\n";
    previousMessageSeparator += getLocalizedProperty(message.userSession,
        "mail.compose.forward.prev.message.info.to");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.to.map(to => nameAndAddress(to));
    previousMessageSeparator += "\n";
    previousMessageSeparator += getLocalizedProperty(message.userSession,
        "mail.compose.forward.prev.message.info.date");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.date;
    previousMessageSeparator += "\n";
    previousMessageSeparator += getLocalizedProperty(message.userSession,
        "mail.compose.forward.prev.message.info.from");
    previousMessageSeparator += ": ";
    previousMessageSeparator += nameAndAddress(message.from);
    return previousMessageSeparator;
}

/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient) {
    return recipient.dn ? recipient.dn + " <" + recipient.address + ">" : recipient.address;
}
