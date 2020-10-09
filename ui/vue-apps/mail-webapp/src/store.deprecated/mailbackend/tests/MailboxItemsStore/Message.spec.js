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
import Message from "../../MailboxItemsStore/Message.js";
import mailboxItem from "./data/mailbox-item.json";

//FIXME: move me in MessageAdaptor test
describe.skip("Message", () => {
    test("constructor & toMailboxItem", () => {
        const message = new Message("key", mailboxItem);

        expect(message.messageId).toEqual(mailboxItem.value.body.messageId);
        expect(message.uid).toEqual(mailboxItem.uid);
        message.flags.forEach((messageFlag, index) => expect(messageFlag).toEqual(mailboxItem.value.flags[index]));

        const expectedItem = {
            body: {
                subject: mailboxItem.value.body.subject,
                headers: mailboxItem.value.body.headers,
                recipients: mailboxItem.value.body.recipients.map(r =>
                    r.kind === "Originator" ? r : { kind: r.kind, address: r.address, dn: "" }
                ),
                messageId: mailboxItem.value.body.messageId,
                references: mailboxItem.value.body.references,
                structure: {
                    mime: "text/plain",
                    address: "TEXT"
                }
            },
            flags: []
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
});
