import { html2text } from "@bluemind/html-utils";
import injector from "@bluemind/inject";

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
export function previousMessageContent(action, parts, message) {
    let previousMessage = "";
    parts.forEach(part => {
        if (part.mime === "text/html") {
            previousMessage += html2text(part.content);
        } else if (part.mime === "text/plain") {
            previousMessage += part.content;
        }
    });

    let previousMessageSeparator = "";
    const vueI18n = injector.getProvider("i18n").get();

    switch (action) {
        case message.actions.REPLY:
        case message.actions.REPLYALL:
            previousMessage = previousMessage
                .split("\n")
                .map(line => "> " + line)
                .join("\n");
            previousMessageSeparator = vueI18n.t("mail.compose.reply.body", {
                date: message.date,
                name: nameAndAddress(message.from)
            });
            break;
        case message.actions.FORWARD:
            previousMessageSeparator = buildPreviousMessageSeparatorForForward(message, vueI18n);
            break;
        default:
            break;
    }

    return previousMessageSeparator + "\n\n" + previousMessage;
}

/** Compute the subject in function of the current action (like "Re: My Subject" when Reply). */
export function computeSubject(action, message) {
    let subjectPrefix;
    const vueI18n = injector.getProvider("i18n").get();
    switch (action) {
        case message.actions.REPLY:
        case message.actions.REPLYALL:
            subjectPrefix = vueI18n.t("mail.compose.reply.subject");
            break;
        case message.actions.FORWARD:
            subjectPrefix = vueI18n.t("mail.compose.forward.subject");
            break;
        default:
            break;
    }

    // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
    if (subjectPrefix !== message.subject.substring(0, subjectPrefix.length)) {
        return subjectPrefix + message.subject;
    }
    return message.subject;
}

/** A separator before the previous message containing basic info. */
function buildPreviousMessageSeparatorForForward(message, vueI18n) {
    let previousMessageSeparator = vueI18n.t("mail.compose.forward.body");
    previousMessageSeparator += "\n";
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.subject");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.subject;
    previousMessageSeparator += "\n";
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.to");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.to.map(to => nameAndAddress(to));
    previousMessageSeparator += "\n";
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.date");
    previousMessageSeparator += ": ";
    previousMessageSeparator += message.date;
    previousMessageSeparator += "\n";
    previousMessageSeparator += vueI18n.t("mail.compose.forward.prev.message.info.from");
    previousMessageSeparator += ": ";
    previousMessageSeparator += nameAndAddress(message.from);
    return previousMessageSeparator;
}


/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient) {
    return recipient.dn ? recipient.dn + " <" + recipient.address + ">" : recipient.address;
}