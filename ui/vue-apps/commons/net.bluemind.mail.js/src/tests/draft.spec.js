import { MockI18NProvider } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";

import { create, MessageCreationModes, MessageHeader } from "~/src/message";
import {
    computeCcRecipients,
    computeToRecipients,
    computeSubject,
    findIdentityFromMailbox,
    addSeparator,
    computeIdentityForReplyOrForward
} from "~/src/draft";

ServiceLocator.register({ provide: "i18n", factory: () => MockI18NProvider });
const vueI18n = ServiceLocator.getProvider("i18n").get();

const previousMessageFrom = { address: "someone@vm40.net", dn: "Some One" };
const previousMessageTo = [
    { dn: "John Doe", address: "jdoe@vm40.net" },
    { dn: "Toto Matic", address: "tmatic@vm40.net" },
    { dn: "Georges Abitbol", address: "gabitbol@vm40.net" }
];
const previousMessageCc = [
    { dn: "Another John Doe", address: "anotherjdoe@vm40.net" },
    { dn: "Another Toto Matic", address: "anothertmatic@vm40.net" },
    { dn: "Another Georges Abitbol", address: "anothergabitbol@vm40.net" }
];

const previousMessage = {
    ...create(),
    ...{
        date: new Date(),
        from: previousMessageFrom,
        to: previousMessageTo,
        cc: previousMessageCc,
        remoteRef: { internalId: 10 },
        folderRef: { uid: "my-uid" }
    }
};

describe("Compute subject", () => {
    const message = { subject: "TrucTruc" };

    test("computeSubject for Reply", () => {
        checkBuildSubject(message, MessageCreationModes.REPLY, "Re: ");
    });

    test("computeSubject for ReplyAll", () => {
        checkBuildSubject(message, MessageCreationModes.REPLYALL, "Re: ");
    });

    test("computeSubject for Forward", () => {
        checkBuildSubject(message, MessageCreationModes.FORWARD, "Fw: ");
    });
});

function checkBuildSubject(message, creationMode, prefix) {
    const subject = computeSubject(creationMode, message);
    const expectedSubject = prefix + message.subject;
    expect(subject).toEqual(expectedSubject);

    // should not add the prefix again
    const subject2 = computeSubject(creationMode, message);
    expect(subject2).toEqual(expectedSubject);
}

describe("Compute reply / forward separators", () => {
    const messageContent = "messageContent";

    let itemsService = {};
    itemsService.uploadPart = jest.fn().mockReturnValue("2");

    vueI18n.t = jest.fn().mockImplementation((key, params) => {
        if (key === "mail.compose.reply.body") {
            return "On " + params.date + ", " + params.name + " wrote:";
        } else if (key === "mail.compose.forward.body") {
            return "---- Original Message ----";
        } else if (key === "mail.compose.forward.prev.message.info.from") {
            return "From";
        } else if (key === "mail.compose.forward.prev.message.info.to") {
            return "To";
        } else if (key === "mail.compose.forward.prev.message.info.date") {
            return "Date";
        } else if (key === "mail.compose.forward.prev.message.info.subject") {
            return "Subject";
        }
    });

    test("for Reply and ReplyAll with userPrefTextOnly", () => {
        const expectedContent =
            "\n<p>On " + previousMessage.date + ", Some One <someone@vm40.net> wrote:\n</p>\n\n> " + messageContent;

        let contentWithSeparator = addSeparator(
            messageContent,
            previousMessage,
            MessageCreationModes.REPLY,
            true,
            vueI18n
        );
        expect(contentWithSeparator).toEqual(expectedContent);
        contentWithSeparator = addSeparator(
            messageContent,
            previousMessage,
            MessageCreationModes.REPLY_ALL,
            true,
            vueI18n
        );
        expect(contentWithSeparator).toEqual(expectedContent);
    });

    test("for Forward with userPrefTextOnly", async () => {
        const expectedContent =
            '\n<p style="color: purple;">---- Original Message ----\nSubject: ' +
            previousMessage.subject +
            "\nTo: John Doe <jdoe@vm40.net>,Toto Matic <tmatic@vm40.net>,Georges Abitbol <gabitbol@vm40.net>\nDate: " +
            previousMessage.date +
            "\nFrom: Some One <someone@vm40.net>\n\n</p>messageContent";

        const contentWithSeparator = addSeparator(
            messageContent,
            previousMessage,
            MessageCreationModes.FORWARD,
            true,
            vueI18n
        );
        expect(contentWithSeparator).toEqual(expectedContent);
    });

    test("for Reply without userPrefTextOnly", async () => {
        const expectedContent =
            '<br><div id="data-bm-forward-separator"><p>On ' +
            previousMessage.date +
            `, Some One <someone@vm40.net> wrote:<br></p><blockquote style="margin-left: 1rem; padding-left: 1rem; border-left: 2px solid black;">` +
            messageContent +
            "</blockquote></div>";

        const contentWithSeparator = addSeparator(
            messageContent,
            previousMessage,
            MessageCreationModes.REPLY,
            false,
            vueI18n
        );
        expect(contentWithSeparator).toEqual(expectedContent);
    });
});

describe("compute To and Cc recipients when replying", () => {
    beforeEach(() => {
        previousMessage.headers = [];
    });

    const currentIdentity = { email: "jdoe@vm40.net", displayname: "John Doe" };
    const otherRecipients = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
    const otherRecipientsWithDn = otherRecipients.map(address => ({ address, dn: "" }));

    test("Reply and no header", () => {
        const to = computeToRecipients(MessageCreationModes.REPLY, previousMessage, currentIdentity);
        expect(to).toEqual([previousMessageFrom]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    test("Reply and Mail-Followup-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_FOLLOWUP_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY, previousMessage, currentIdentity);
        expect(to).toEqual([previousMessageFrom]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    test("Reply and Mail-Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY, previousMessage, currentIdentity);
        expect(to).toEqual([{ address: "azerty@keyboard.com", dn: "" }]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    test("Reply and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY, previousMessage, currentIdentity);
        expect(to).toEqual([{ address: "azerty@keyboard.com", dn: "" }]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    const previousToWithoutMe = previousMessageTo.filter(to => to.address !== currentIdentity.email);

    test("ReplyAll and no header", () => {
        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual([previousMessageFrom].concat(previousToWithoutMe));

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage);
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Mail-Followup-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_FOLLOWUP_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual(otherRecipientsWithDn);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage);
        expect(cc).toEqual([]);
    });

    test("ReplyAll and Mail-Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual(otherRecipientsWithDn);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage);
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual(otherRecipientsWithDn);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage);
        expect(cc).toEqual(previousMessageCc);
    });

    test("Remove 'From' from recipients", () => {
        previousMessage.to.push(currentIdentity);
        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to.findIndex(recipient => recipient.address === currentIdentity.email)).toBe(-1);
    });
});

describe("compute From when replying or forwarding", () => {
    const defaultId = { email: "default@mail.org", name: "default", isDefault: true, displayname: "default" };
    const other = { email: "contact@mail.org", name: "contacts", isDefault: false, displayname: "Contact" };
    const anotherAgain = { email: "rh@mail.org", name: "rh", isDefault: false, displayname: "RH" };
    const identities = [defaultId, other, anotherAgain];

    const currentMailbox = { address: "rh@mail.org" };

    test("only one identity", () => {
        const message = { to: [{ address: "contact@mail.org" }], cc: [] };
        const from = computeIdentityForReplyOrForward(message, identities, currentMailbox);
        expect(from.email).toBe("contact@mail.org");
    });

    test("priorize default identity if multiple found", () => {
        const message = {
            to: [{ address: "contact@mail.org" }, { address: "default@mail.org" }],
            cc: [{ address: "rh@mail.org" }]
        };
        const defaultIdentity = { email: "plop@plop.plop", displayname: "Plop Plop" };
        const from = computeIdentityForReplyOrForward(message, identities, currentMailbox, defaultIdentity);
        expect(from.email).toBe("plop@plop.plop");
    });

    test("priorize 'to' recipient if multiple found (and no default)", () => {
        const message = { to: [{ address: "contact@mail.org" }], cc: [{ address: "rh@mail.org" }] };
        const from = computeIdentityForReplyOrForward(message, identities, currentMailbox);
        expect(from.email).toBe("contact@mail.org");
    });

    test("if no identities found in recipients, take current mailbox one", () => {
        const message = { to: [], cc: [] };
        const from = computeIdentityForReplyOrForward(message, identities, currentMailbox);
        expect(from.email).toBe("rh@mail.org");
    });

    test("if no identities found in recipients, and mailbox dont match any identity, use default", () => {
        let message = { to: [], cc: [] };
        const defaultIdentity = { email: "plop@plop.plop", displayname: "Plop Plop" };
        let from = computeIdentityForReplyOrForward(message, identities, {}, defaultIdentity);
        expect(from.email).toBe("plop@plop.plop");

        message = { to: [{ address: "blabla@mail.com" }], cc: [{ address: "moreblabla@mail.org" }] };
        from = computeIdentityForReplyOrForward(
            message,
            identities,
            {
                address: "cantfindme@mail.org",
                dn: "Cant find me"
            },
            defaultIdentity
        );
        expect(from.email).toBe("plop@plop.plop");
    });
});

describe("find identity given a current mailbox, or fallback on default", () => {
    const defaultIdentity = { email: "default@mail.org", name: "default", isDefault: true, displayname: "default" };
    const other = { email: "contact@mail.org", name: "contacts", isDefault: false, displayname: "Contact" };

    const identities = [other, defaultIdentity];
    const currentMailbox = { dn: other.displayname, address: other.email };

    test("set identity according to currentMailbox", () => {
        const identity = findIdentityFromMailbox(currentMailbox, identities, defaultIdentity);
        expect(identity).toMatchObject(other);
    });

    test("set default identity if no identity matches currentMailbox", () => {
        const identity = findIdentityFromMailbox(
            { dn: "unknown", address: "un@known.org" },
            identities,
            defaultIdentity
        );
        expect(identity).toMatchObject(defaultIdentity);
    });
});
