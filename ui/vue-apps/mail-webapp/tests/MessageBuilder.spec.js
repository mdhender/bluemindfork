import { computeSubject, previousMessageContent } from "../src/MessageBuilder";
import { MimeType } from "@bluemind/email";
import mailboxItem from "./data/mailbox-item.json";
jest.mock("@bluemind/inject", () => {
    return {
        getProvider() {
            return {
                get: val => {
                    if (val === "Environment") {
                        return { firstDayOfWeek: 1 };
                    } else {
                        return {
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
                            d: key => {
                                return key;
                            }
                        };
                    }
                }
            };
        }
    };
});
import Message from "../src/store.deprecated/mailbackend/MailboxItemsStore/Message";

describe("MessageBuilder", () => {
    let message = null;
    let parts;
    const additionnalTextPart = {
        mime: "text/plain",
        content: "example text plain",
        uid: "59."
    };

    beforeEach(() => {
        message = new Message("key", mailboxItem);
        message.userSession = { lang: "en" };
        parts = [
            {
                mime: "text/html",
                content:
                    '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"><html><body><p><span' +
                    ' style="font-family: Arial; font-size: 12px;">messageContent</span></p></body></html>',
                uid: "59."
            }
        ];
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
        const previousMessage = previousMessageContent(message.actions.REPLY, parts, message, MimeType.TEXT_PLAIN);
        let expectedPreviousMessage =
            "\n\n\nOn " + message.date + ", John Doe <jdoe@vm40.net> wrote:\n\n> messageContent\n> ";

        expect(previousMessage).toEqual(expectedPreviousMessage);
    });

    test("previousMessageContent for ReplyAll", () => {
        const previousMessage = previousMessageContent(message.actions.REPLYALL, parts, message, MimeType.TEXT_PLAIN);
        let expectedPreviousMessage =
            "\n\n\nOn " + message.date + ", John Doe <jdoe@vm40.net> wrote:\n\n> messageContent\n> ";

        expect(previousMessage).toEqual(expectedPreviousMessage);
    });

    test("previousMessageContent for Forward", () => {
        const previousMessage = previousMessageContent(message.actions.FORWARD, parts, message, MimeType.TEXT_PLAIN);
        let expectedPreviousMessage =
            "\n\n\n---- Original Message ----\nSubject: " +
            message.subject +
            "\nTo: John Doe <jdoe@vm40.net>\nDate: " +
            message.date +
            "\nFrom: John Doe <jdoe@vm40.net>\n\nmessageContent\n";

        expect(previousMessage).toEqual(expectedPreviousMessage);
    });

    test("compute previousMessageContent (multiple inline parts - TEXT expected)", () => {
        parts.push(additionnalTextPart);

        const previousMessage = previousMessageContent(message.actions.REPLY, parts, message, MimeType.TEXT_PLAIN);
        let expectedPreviousMessage =
            "\n\n\nOn " + message.date + ", John Doe <jdoe@vm40.net> wrote:\n\n> messageContent\n> example text plain";

        expect(previousMessage).toEqual(expectedPreviousMessage);
    });

    test("compute previousMessageContent (multiple inline parts - HTML expected)", () => {
        parts.push(additionnalTextPart);
        const previousMessage = previousMessageContent(message.actions.REPLY, parts, message, MimeType.TEXT_HTML);
        let expectedPreviousMessage =
            "<br><br><br>On " +
            message.date +
            `, John Doe <jdoe@vm40.net> wrote:<br>
            <style>
                .reply {
                    margin-left: 1rem;
                    padding-left: 1rem;
                    border-left: 2px solid black;
                }
            </style>
            <blockquote class="reply">` +
            parts[0].content +
            "<pre>example text plain</pre></blockquote>";

        expect(previousMessage).toEqual(expectedPreviousMessage);
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
