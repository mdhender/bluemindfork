import { MockI18NProvider } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";

jest.mock("postal-mime", () => ({ TextEncoder: jest.fn() }));

import { create, MessageCreationModes, MessageHeader } from "../message";
import {
    computeCcRecipients,
    computeToRecipients,
    computeSubject,
    findIdentityFromMailbox,
    quotePreviousMessage,
    computeIdentityForReplyOrForward
} from "../draft";

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
        date: new Date("2012-12-12"),
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

describe("Compute reply / forward quoted previous message", () => {
    const messageContent = "My message content.";

    let itemsService = {};
    itemsService.uploadPart = jest.fn().mockReturnValue("2");

    vueI18n.t = jest.fn().mockImplementation((key, params) => {
        if (key === "mail.compose.reply.body") {
            return "On " + params.date + ", " + params.name + " wrote:";
        } else if (key === "mail.compose.forward.prev.message.info.title") {
            return "---- Original Message ----";
        } else if (key === "mail.compose.forward.prev.message.info.from") {
            return "From";
        } else if (key === "common.to") {
            return "To";
        } else if (key === "common.cc") {
            return "Cc";
        } else if (key === "mail.compose.forward.prev.message.info.date") {
            return "Date";
        } else if (key === "mail.compose.forward.prev.message.info.subject") {
            return "Subject";
        }
    });

    test.each`
        userPrefTextOnly
        ${true}
        ${false}
    `("for Reply and ReplyAll with userPrefTextOnly $userPrefTextOnly", ({ userPrefTextOnly }) => {
        let contentWithSeparator = quotePreviousMessage(
            messageContent,
            previousMessage,
            MessageCreationModes.REPLY,
            userPrefTextOnly,
            vueI18n
        );
        expect(contentWithSeparator).toMatchSnapshot();
        contentWithSeparator = quotePreviousMessage(
            messageContent,
            previousMessage,
            MessageCreationModes.REPLY_ALL,
            userPrefTextOnly,
            vueI18n
        );
        expect(contentWithSeparator).toMatchSnapshot();
    });

    test.each`
        userPrefTextOnly
        ${true}
        ${false}
    `("for Forward with userPrefTextOnly $userPrefTextOnly", ({ userPrefTextOnly }) => {
        const contentWithSeparator = quotePreviousMessage(
            messageContent,
            previousMessage,
            MessageCreationModes.FORWARD,
            userPrefTextOnly,
            vueI18n
        );
        expect(contentWithSeparator).toMatchSnapshot();
    });
});

describe("compute To and Cc recipients when replying", () => {
    beforeEach(() => {
        previousMessage.headers = [];
    });

    const currentIdentity = { email: "jdoe@vm40.net", displayname: "John Doe" };
    const otherRecipients = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
    const otherRecipientsWithDn = otherRecipients.map(address => ({ address, dn: undefined }));

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
        expect(to).toEqual([{ address: "azerty@keyboard.com" }]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    test("Reply and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY, previousMessage, currentIdentity);
        expect(to).toEqual([{ address: "azerty@keyboard.com" }]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY, previousMessage);
        expect(cc).toEqual([]);
    });

    const previousToWithoutMe = previousMessageTo.filter(to => to.address !== currentIdentity.email);

    test("ReplyAll and no header", () => {
        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual([previousMessageFrom].concat(previousToWithoutMe));

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Mail-Followup-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_FOLLOWUP_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual(otherRecipientsWithDn);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(cc).toEqual([]);
    });

    test("ReplyAll and Mail-Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_REPLY_TO, values: otherRecipients }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual([...otherRecipientsWithDn, ...previousMessage.to]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: [otherRecipients[0]] }];

        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to).toEqual([otherRecipientsWithDn[0], ...previousMessage.to]);

        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(cc).toEqual(previousMessageCc);
    });

    test("Remove sender from TO recipients", () => {
        previousMessage.to.push(currentIdentity);
        const to = computeToRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(to.findIndex(recipient => recipient.address === currentIdentity.email)).toBe(-1);
    });

    test("Remove sender from CC recipients", () => {
        previousMessage.to.push(currentIdentity);
        const cc = computeCcRecipients(MessageCreationModes.REPLY_ALL, previousMessage, currentIdentity);
        expect(cc.findIndex(recipient => recipient.address === currentIdentity.email)).toBe(-1);
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
