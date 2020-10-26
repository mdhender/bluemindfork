import { MockI18NProvider } from "@bluemind/test-utils";
import ServiceLocator from "@bluemind/inject";
import { MimeType } from "@bluemind/email";

import { create, MessageCreationModes, MessageHeader } from "../message";
import { buildRecipients, buildSubject, uploadInlineParts, sanitizeForCyrus } from "../draft";

import PlayWithInlinePartsByCapabilities from "../../store/messages/helpers/PlayWithInlinePartsByCapabilities";

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

describe("computeSubject", () => {
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
    const subject = buildSubject(creationMode, message);
    const expectedSubject = prefix + message.subject;
    expect(subject).toEqual(expectedSubject);

    // should not add the prefix again
    const subject2 = buildSubject(creationMode, message);
    expect(subject2).toEqual(expectedSubject);
}

describe("uploadInlineParts", () => {
    const messageContent = "messageContent";
    PlayWithInlinePartsByCapabilities.getTextFromStructure = jest.fn().mockReturnValue(messageContent);
    PlayWithInlinePartsByCapabilities.getHtmlFromStructure = jest
        .fn()
        .mockReturnValue({ html: messageContent, inlineImageParts: [] });

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

    test("for Reply and ReplyAll with userPrefTextOnly", async () => {
        const expectedContent =
            "<p>On " + previousMessage.date + ", Some One <someone@vm40.net> wrote:</p>\r\n\r\n> " + messageContent;

        const { partContentByMimeType } = await uploadInlineParts(
            MessageCreationModes.REPLY,
            previousMessage,
            itemsService,
            true,
            vueI18n
        );
        expect(partContentByMimeType[MimeType.TEXT_PLAIN]).toEqual(expectedContent);

        const result = await uploadInlineParts(
            MessageCreationModes.REPLY_ALL,
            previousMessage,
            itemsService,
            true,
            vueI18n
        );
        expect(result.partContentByMimeType[MimeType.TEXT_PLAIN]).toEqual(expectedContent);
    });

    test("for Forward with userPrefTextOnly", async () => {
        const expectedContent =
            '<p style="color: purple;">---- Original Message ----\r\nSubject: ' +
            previousMessage.subject +
            "\r\nTo: John Doe <jdoe@vm40.net>,Toto Matic <tmatic@vm40.net>,Georges Abitbol <gabitbol@vm40.net>\r\nDate: " +
            previousMessage.date +
            "\r\nFrom: Some One <someone@vm40.net>\r\n\r\n</p>messageContent";
        const { partContentByMimeType } = await uploadInlineParts(
            MessageCreationModes.FORWARD,
            previousMessage,
            itemsService,
            true,
            vueI18n
        );
        expect(partContentByMimeType[MimeType.TEXT_PLAIN]).toEqual(expectedContent);
    });

    test("for Reply without userPrefTextOnly", async () => {
        const expectedContent = sanitizeForCyrus(
            "<div data-bm-forward-separator><p>On " +
                previousMessage.date +
                `, Some One <someone@vm40.net> wrote:</p><br>
            <blockquote style="margin-left: 1rem; padding-left: 1rem; border-left: 2px solid black;">` +
                messageContent +
                "</blockquote></div>"
        );

        const { partContentByMimeType } = await uploadInlineParts(
            MessageCreationModes.REPLY,
            previousMessage,
            itemsService,
            false,
            vueI18n
        );

        expect(partContentByMimeType[MimeType.TEXT_HTML]).toEqual(expectedContent);
    });
});

describe("buildRecipients", () => {
    beforeEach(() => {
        previousMessage.headers = [];
    });

    const myEmail = "jdoe@vm40.net",
        myName = "John Doe";
    const otherRecipients = ["azerty@keyboard.com", "memory@ram.net", "pixel@lcd.org"];
    const otherRecipientsWithDn = otherRecipients.map(address => ({ address, dn: "" }));

    test("Reply and no header", () => {
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY, previousMessage, myEmail, myName);
        expect(to).toEqual([previousMessageFrom]);
        expect(cc).toEqual([]);
    });

    test("Reply and Mail-Followup-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_FOLLOWUP_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY, previousMessage, myEmail, myName);
        expect(to).toEqual([previousMessageFrom]);
        expect(cc).toEqual([]);
    });

    test("Reply and Mail-Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_REPLY_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY, previousMessage, myEmail, myName);
        expect(to).toEqual([{ address: "azerty@keyboard.com", dn: "" }]);
        expect(cc).toEqual([]);
    });

    test("Reply and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY, previousMessage, myEmail, myName);
        expect(to).toEqual([{ address: "azerty@keyboard.com", dn: "" }]);
        expect(cc).toEqual([]);
    });

    const previousToWithoutMe = previousMessageTo.filter(to => to.address !== myEmail);

    test("ReplyAll and no header", () => {
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY_ALL, previousMessage, myEmail, myName);
        expect(to).toEqual([previousMessageFrom].concat(previousToWithoutMe));
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Mail-Followup-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_FOLLOWUP_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY_ALL, previousMessage, myEmail, myName);
        expect(to).toEqual(otherRecipientsWithDn);
        expect(cc).toEqual([]);
    });

    test("ReplyAll and Mail-Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.MAIL_REPLY_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY_ALL, previousMessage, myEmail, myName);
        expect(to).toEqual(otherRecipientsWithDn);
        expect(cc).toEqual(previousMessageCc);
    });

    test("ReplyAll and Reply-To header", () => {
        previousMessage.headers = [{ name: MessageHeader.REPLY_TO, values: otherRecipients }];
        const { to, cc } = buildRecipients(MessageCreationModes.REPLY_ALL, previousMessage, myEmail, myName);
        expect(to).toEqual(otherRecipientsWithDn);
        expect(cc).toEqual(previousMessageCc);
    });

    test("Forward", () => {
        const { to, cc } = buildRecipients(MessageCreationModes.FORWARD, previousMessage, myEmail, myName);
        expect(to).toEqual([]);
        expect(cc).toEqual([]);
    });
});
