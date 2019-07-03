/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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

        this.loadFromItem(item);
    }

    loadFromItem(item) {
        const mailboxItem = item.value;
        this.subject = mailboxItem.body.subject;
        this.preview = mailboxItem.body.preview;
        this.from = mailboxItem.body.recipients.find(
            rcpt => rcpt.kind == RecipientKind.Originator
        );
        this.to = mailboxItem.body.recipients.filter(
            rcpt => rcpt.kind == RecipientKind.Primary
        );
        this.cc = mailboxItem.body.recipients.filter(
            rcpt => rcpt.kind == RecipientKind.CarbonCopy
        );
        this.bcc = mailboxItem.body.recipients.filter(
            rcpt => rcpt.kind == RecipientKind.BlindCarbonCopy
        );
        this.computeFormattedName();
        this.date = new Date(mailboxItem.body.date);
        this.structure = mailboxItem.body.structure;
        this.headers = mailboxItem.body.headers;
        this.messageId = mailboxItem.body.messageId;
        this.references = mailboxItem.body.references;
        this.flags = mailboxItem.systemFlags
            .concat(mailboxItem.otherFlags)
            .map(flag => flag.toLowerCase());
        this.states = [];
        this.uid = item.uid; // FIXME remove me
        this.ids = {
            uid: item.uid,
            imap: mailboxItem.imapUid,
            id: item.internalId
        };
        this.extend(item);
    }

    toMailboxItem(addrPart, sender, senderDomain) {
        return {
            body: {
                subject: this.subject,
                headers: this.headers,
                recipients: this.buildRecipients(sender, senderDomain),
                messageId: this.messageId,
                references: this.references,
                structure: {
                    mime: "text/plain",
                    address: addrPart
                }
            }
        };
    }

    buildRecipients(sender, senderDomain) {
        const primaries = this.buildRecipientsForKind(RecipientKind.Primary, this.to);
        const carbonCopies = this.buildRecipientsForKind(RecipientKind.CarbonCopy, this.cc);
        const blindCarbonCopies = this.buildRecipientsForKind(RecipientKind.BlindCarbonCopy, this.bcc);
        const originator = [{
            kind: RecipientKind.Originator,
            address: sender,
            dn: senderDomain
        }];

        return primaries.concat(carbonCopies).concat(blindCarbonCopies).concat(originator);
    }

    buildRecipientsForKind(kind, addresses) {
        return (addresses || []).map(address => {
            return {
                kind: kind,
                address: address,
                dn: address.split("@")[1]
            };
        });
    }

    computeFormattedName() {
        this.from.formattedName = this.from.dn || this.from.address;
        this.to.forEach(rcpt => rcpt.formattedName = rcpt.dn || rcpt.address);
        this.cc.forEach(rcpt => rcpt.formattedName = rcpt.dn || rcpt.address);
        this.bcc.forEach(rcpt => rcpt.formattedName = rcpt.dn || rcpt.address);
    }

    extend(item) {
        if (item.value.body.smartAttach) {
            this.states.push("has-attachment");
        }
        if (item.value.systemFlags.indexOf(SystemFlag.seen) === -1) {
            this.states.push("not-seen");
        }
    }

    getInlineParts(rootPart) {
        const visitor = new GetInlinePartsVisitor();
        const walker = new TreeWalker(rootPart, visitor);
        walker.walk();
        return visitor.result();
    }

    /** 
     * Compute the list of recipients depending on the action (reply, reply all...) and the kind of recipient field we
     *  want to fill (cc, to).
     */
    computeRecipients(targetField = this.recipientFields.TO, action = this.actions.REPLY) {
        switch (targetField) {
            case this.recipientFields.CC:
                return this.computeRecipientsCC(action);
            case this.recipientFields.TO:
                return this.computeRecipientsTO(action);
            default:
                return [];
        }
    }

    computeRecipientsCC(action) {
        if (action == this.actions.REPLYALL) {
            const mailFollowUpTo = this.headers.find(header => header.name === this.recipientHeaders.MAIL_FOLLOWUP_TO);
            if (!mailFollowUpTo) {
                return this.cc.map(cc => cc.address);
            }
        }
        return [];
    }

    computeRecipientsTO(action) {
        const isReplyAll = action == this.actions.REPLYALL;

        if (isReplyAll) {
            const mailFollowUpTo = this.header(this.recipientHeaders.MAIL_FOLLOWUP_TO);
            if (mailFollowUpTo) {
                return this.addressesFromHeader(mailFollowUpTo, true);
            }
        }

        const mailReplyToHeader = this.header(this.recipientHeaders.MAIL_REPLY_TO);
        if (mailReplyToHeader) {
            return this.addressesFromHeader(mailReplyToHeader, isReplyAll);
        }

        const replyToHeader = this.header(this.recipientHeaders.REPLY_TO);
        if (replyToHeader) {
            return this.addressesFromHeader(replyToHeader, isReplyAll);
        }

        // compute recipients from "From" or "To"
        let recipients = [this.from.address];
        const myEmail = injector.getProvider('UserSession').get().defaultEmail;
        if (isReplyAll) {
            // respond to sender and all recipients except myself
            recipients.push(...this.to.map(to => to.address));
            recipients = recipients.filter(address => address != myEmail);
            // avoid duplicates
            recipients = Array.from(new Set(recipients));
            if (recipients.length == 0) {
                // I was alone, respond to myself then
                recipients = [myEmail];
            }
        } else if (recipients.includes(myEmail)) {
            // all recipients except myself
            recipients = this.to.map(to => to.address).filter(address => address != myEmail);
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

    header(headerName) {
        return this.headers.find(header => header.name === headerName);
    }

    addressesFromHeader(header, isReplyAll) {
        if (isReplyAll) {
            return header.values.map(value => EmailExtractor.extractEmail(value));
        } else {
            return [EmailExtractor.extractEmail(header.values[0])];
        }
    }

}
