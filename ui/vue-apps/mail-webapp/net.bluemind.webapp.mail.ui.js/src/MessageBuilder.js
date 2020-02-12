import { html2text } from "@bluemind/html-utils";
import { mailText2Html, MimeType } from "@bluemind/email";
import injector from "@bluemind/inject";

/**
 * Build the text representing this message as a previous message.
 * @example TEXT
 * `On Tuesday 2019 01 01, John Doe wrote:
 * > Dear Jane,
 * >  I could not bear to see you with Tarzan anymore,
 * > it will kill me! Please come back!
 * ...`
 * @example HTML
 * `On Tuesday 2019 01 01, John Doe wrote:
 * <blockquote>
 * Dear Jane,
 * I could not bear to see you with Tarzan anymore,
 * it will kill me! Please come back!
 * ...
 * </blockquote>`
 */
export function previousMessageContent(action, parts, message, expected) {
    const vueI18n = injector.getProvider("i18n").get();
    const lineBreakSeparator = MimeType.equals(expected, MimeType.TEXT_PLAIN) ? "\n" : "<br>";

    const separator =
        action === message.actions.FORWARD
            ? buildSeparatorForForward(message, vueI18n, lineBreakSeparator)
            : buildSeparatorForReply(vueI18n, message);

    let previousMessage;
    parts.forEach(part => {
        if (MimeType.equals(part.mime, expected)) {
            previousMessage = part.content;
        } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
            previousMessage = mailText2Html(part.content);
        } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
            previousMessage = html2text(part.content);
        }
    });

    if (action === message.actions.REPLY || action === message.actions.REPLYALL) {
        previousMessage = adaptPreviousMessageForReply(expected, previousMessage);
    }

    return lineBreakSeparator + lineBreakSeparator + lineBreakSeparator + separator + previousMessage;
}

/** Compute the subject in function of the current action (like "Re: My Subject" when Reply). */
export function computeSubject(action, message) {
    const vueI18n = injector.getProvider("i18n").get();
    const subjectPrefix =
        action === message.actions.FORWARD
            ? vueI18n.t("mail.compose.forward.subject")
            : vueI18n.t("mail.compose.reply.subject");

    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== message.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + message.subject;
    }
    return message.subject;
}

/**
 *  A separator before the previous message (forward).
 */
function buildSeparatorForForward(message, vueI18n, lineBreakSeparator) {
    let previousMessageSeparator = vueI18n.t("mail.compose.forward.body") + lineBreakSeparator;
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.subject");
    previousMessageSeparator += ": " + message.subject + lineBreakSeparator;
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.to");
    previousMessageSeparator += ": " + message.to.map(to => nameAndAddress(to)) + lineBreakSeparator;
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.date");
    previousMessageSeparator += ": " + vueI18n.d(message.date, "full_date_time") + lineBreakSeparator;
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.from");
    previousMessageSeparator += ": " + nameAndAddress(message.from) + lineBreakSeparator + lineBreakSeparator;
    return previousMessageSeparator;
}

/**
 *  A separator before the previous message (reply).
 */
function buildSeparatorForReply(vueI18n, message) {
    return vueI18n.t("mail.compose.reply.body", {
        date: vueI18n.d(message.date, "full_date_time"),
        name: nameAndAddress(message.from)
    });
}

function adaptPreviousMessageForReply(expected, previousMessage) {
    if (MimeType.equals(expected, MimeType.TEXT_PLAIN)) {
        return (
            "\n\n" +
            previousMessage
                .split("\n")
                .map(line => "> " + line)
                .join("\n")
        );
    } else if (MimeType.equals(expected, MimeType.TEXT_HTML)) {
        return (
            `<br>
            <style>
                .reply {
                    margin-left: 1rem;
                    padding-left: 1rem;
                    border-left: 2px solid black;
                }
            </style>
            <blockquote class="reply">` +
            previousMessage +
            "</blockquote>"
        );
    }
}

/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient) {
    return recipient.dn ? recipient.dn + " <" + recipient.address + ">" : recipient.address;
}
