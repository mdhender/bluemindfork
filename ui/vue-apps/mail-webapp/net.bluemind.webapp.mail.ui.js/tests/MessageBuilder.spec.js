import { computeSubject, previousMessageContent } from "../src/MessageBuilder";
import { MimeType } from "@bluemind/email";
import { Message } from "@bluemind/backend.mail.store";
import mailboxItem from "./data/mailbox-item.json";
import injector from "@bluemind/inject";

jest.mock("@bluemind/inject");
injector.getProvider.mockReturnValue({
    get: jest.fn().mockReturnValue({
        t: (key, params) => {
            if (key === "mail.compose.reply.subject") {
                return "Re: ";
            } else if (key === "mail.compose.forward.subject") {
                return "Fw: ";
            } else if (key === "mail.compose.reply.body") {
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
        },
        d: (key) => {
            return key;
        }
    })
});

describe("MessageBuilder", () => {
    let message = null;

    beforeEach(() => {
        message = new Message("key", mailboxItem);
    });

    test("computeSubject for Reply", () => {
        checkComputeSubject(message, message.actions.REPLY, "Re: ");
    });
    test("computeSubject for ReplyAll", () => {
        checkComputeSubject(message, message.actions.REPLYALL, "Re: ");
    });
    test("computeSubject for Forward", () => {
        checkComputeSubject(message, message.actions.FORWARD, "Fw: ");
    });
    test("previousMessageContent for Reply", () => {
        checkComputePreviousMessage(message, message.actions.REPLY);
    });
    test("previousMessageContent for ReplyAll", () => {
        checkComputePreviousMessage(message, message.actions.REPLYALL);
    });
    test("previousMessageContent for Forward", () => {
        checkComputePreviousMessage(message, message.actions.FORWARD);
    });
});

function checkComputeSubject(message, action, prefix) {
    message.userSession = { lang: "en" };
    const subject = computeSubject(action, message);
    const expectedSubject = prefix + mailboxItem.value.body.subject;
    expect(subject).toEqual(expectedSubject);

    // should not add the prefix again
    message.subject = expectedSubject;
    const subject2 = computeSubject(action, message);
    expect(subject2).toEqual(expectedSubject);
}

function checkComputePreviousMessage(message, action) {
    message.userSession = { lang: "en" };
    const parts = [
        {
            mime: "text/html",
            content:
                '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"><html><body><p><span' +
                ' style="font-family: Arial; font-size: 12px;">messageContent</span></p></body></html>',
            uid: "59."
        }
    ];
    const previousMessage = previousMessageContent(action, parts, message, MimeType.TEXT_PLAIN);

    let expectedPreviousMessage;
    switch (action) {
        case message.actions.REPLY:
        case message.actions.REPLYALL:
            expectedPreviousMessage =
                "\n\n\nOn " + message.date + ", John Doe <jdoe@vm40.net> wrote:\n\n> messageContent\n> ";
            break;
        case message.actions.FORWARD:
            expectedPreviousMessage =
                "\n\n\n---- Original Message ----\nSubject: " +
                message.subject +
                "\nTo: John Doe <jdoe@vm40.net>\nDate: " +
                message.date +
                "\nFrom: John Doe <jdoe@vm40.net>\n\nmessageContent\n";
            break;
        default:
            break;
    }
    expect(previousMessage).toEqual(expectedPreviousMessage);
}
