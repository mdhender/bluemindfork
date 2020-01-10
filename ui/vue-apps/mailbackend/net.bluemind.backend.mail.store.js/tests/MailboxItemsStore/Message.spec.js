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
import Message from "../../src/MailboxItemsStore/Message.js";
import mailboxItem from "./data/mailbox-item.json";

describe("Message", () => {
    test("constructor & toMailboxItem", () => {
        const message = new Message("key", mailboxItem);

        expect(message.messageId).toEqual(mailboxItem.value.body.messageId);
        expect(message.uid).toEqual(mailboxItem.uid);
        expect(message.flags.map(f => f.toLowerCase())).toEqual(mailboxItem.flags.map(f => f.toLowerCase()));

        const expectedItem = {
            body: {
                subject: mailboxItem.value.body.subject,
                headers: mailboxItem.value.body.headers,
                // FIXME we do not handle the displayed name (aka distinguished name) yet (except fo Originator)
                recipients: mailboxItem.value.body.recipients.map(r =>
                    r.kind == "Originator" ? r : { kind: r.kind, address: r.address, dn: "" }
                ),
                messageId: mailboxItem.value.body.messageId,
                references: mailboxItem.value.body.references,
                structure: {
                    mime: "text/plain",
                    address: "TEXT"
                }
            }
        };

        // FIXME for now message recipients are just addresses (when sending message)
        message.to = message.to.map(r => r.address);

        let structure = {
            mime: "text/plain",
            address: "TEXT"
        };
        let actualItem = message.toMailboxItem("jdoe@vm40.net", "John Doe", false, structure);

        expect(actualItem).toEqual(expectedItem);
    });
    test("computeRecipients for TO and Reply and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLY);
    });
    test("computeRecipients for TO and Reply and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLY);
    });
    test("computeRecipients for TO and Reply and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLY, { mailReplyTo: others });
    });
    test("computeRecipients for TO and Reply and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLY, { replyTo: others });
    });
    test("computeRecipients for TO and ReplyAll and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLYALL);
    });
    test("computeRecipients for TO and ReplyAll and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLYALL, {
            mailFollowupTo: others
        });
    });
    test("computeRecipients for TO and ReplyAll and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLYALL, { mailReplyTo: others });
    });
    test("computeRecipients for TO and ReplyAll and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.REPLYALL, { replyTo: others });
    });
    test("computeRecipients for TO and Forward and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.FORWARD);
    });
    test("computeRecipients for TO and Forward and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.FORWARD, {
            mailFollowupTo: others
        });
    });
    test("computeRecipients for TO and Forward and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.FORWARD, { mailReplyTo: others });
    });
    test("computeRecipients for TO and Forward and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.TO, message.actions.FORWARD, { replyTo: others });
    });
    test("computeRecipients for CC and Reply and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLY);
    });
    test("computeRecipients for CC and Reply and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLY);
    });
    test("computeRecipients for CC and Reply and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLY, { mailReplyTo: others });
    });
    test("computeRecipients for CC and Reply and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLY, { replyTo: others });
    });
    test("computeRecipients for CC and ReplyAll and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLYALL);
    });
    test("computeRecipients for CC and ReplyAll and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLYALL, {
            mailFollowupTo: others
        });
    });
    test("computeRecipients for CC and ReplyAll and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLYALL, { mailReplyTo: others });
    });
    test("computeRecipients for CC and ReplyAll and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.REPLYALL, { replyTo: others });
    });
    test("computeRecipients for CC and Forward and no header", () => {
        const message = new Message("key", mailboxItem);
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.FORWARD);
    });
    test("computeRecipients for CC and Forward and Mail-Followup-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Followup-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.FORWARD, {
            mailFollowupTo: others
        });
    });
    test("computeRecipients for CC and Forward and Mail-Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Mail-Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.FORWARD, { mailReplyTo: others });
    });
    test("computeRecipients for CC and Forward and Reply-To header", () => {
        const message = new Message("key", mailboxItem);
        const others = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
        message.headers = [{ name: "Reply-To", values: others }];
        checkComputeRecipients(message, message.recipientFields.CC, message.actions.FORWARD, { replyTo: others });
    });
});

function checkComputeRecipients(message, recipientField, action, headersInfo) {
    message.userSession = { defaultEmail: "jdoe@vm40.net" };

    message.from = { dn: "John Doe", address: "jdoe@vm40.net" };
    message.to = [
        { dn: "John Doe", address: "jdoe@vm40.net" },
        { dn: "Toto Matic", address: "tmatic@vm40.net" },
        { dn: "Georges Abitbol", address: "gabitbol@vm40.net" }
    ];
    message.cc = [
        { dn: "John Doe", address: "jdoe@vm40.net" },
        { dn: "Toto Matic", address: "tmatic@vm40.net" },
        { dn: "Georges Abitbol", address: "gabitbol@vm40.net" }
    ];

    const recipients = message.computeRecipients(recipientField, action);

    switch (recipientField) {
        case message.recipientFields.TO:
            switch (action) {
                case message.actions.REPLY:
                    if (message.headers.find(header => header.name === message.recipientHeaders.MAIL_REPLY_TO)) {
                        expect(recipients).toEqual([headersInfo.mailReplyTo[0]]);
                    } else if (message.headers.find(header => header.name === message.recipientHeaders.REPLY_TO)) {
                        expect(recipients).toEqual([headersInfo.replyTo[0]]);
                    } else {
                        expect(recipients).toEqual(["tmatic@vm40.net"]);
                    }
                    break;
                case message.actions.REPLYALL:
                    if (message.headers.find(header => header.name === message.recipientHeaders.MAIL_FOLLOWUP_TO)) {
                        expect(recipients).toEqual(headersInfo.mailFollowupTo);
                    } else if (message.headers.find(header => header.name === message.recipientHeaders.MAIL_REPLY_TO)) {
                        expect(recipients).toEqual(headersInfo.mailReplyTo);
                    } else if (message.headers.find(header => header.name === message.recipientHeaders.REPLY_TO)) {
                        expect(recipients).toEqual(headersInfo.replyTo);
                    } else {
                        expect(recipients).toEqual(["tmatic@vm40.net", "gabitbol@vm40.net"]);
                    }
                    break;
                case message.actions.FORWARD:
                    expect(recipients).toEqual([]);
                    break;
                default:
                    break;
            }
            break;
        case message.recipientFields.CC:
            switch (action) {
                case message.actions.REPLY:
                    expect(recipients).toEqual([]);
                    break;
                case message.actions.REPLYALL:
                    if (!message.headers.find(header => header.name === message.recipientHeaders.MAIL_FOLLOWUP_TO)) {
                        expect(recipients).toEqual(message.cc.map(cc => cc.address));
                    }
                    break;
                case message.actions.FORWARD:
                    expect(recipients).toEqual([]);
                    break;
                default:
                    break;
            }
            break;
        default:
            break;
    }
}
