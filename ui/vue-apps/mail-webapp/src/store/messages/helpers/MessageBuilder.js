import { html2text, sanitizeHtml } from "@bluemind/html-utils";
import { EmailExtractor, mailText2Html, MimeType, PartsBuilder } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import {
    MessageCreationModes,
    MessageHeader,
    MessageReplyAttributeSeparator,
    MessageForwardAttributeSeparator
} from "../../../model/message";
import PartsHelper from "./PartsHelper";

export default {
    createDraftStructure: (
        { attachments, multipartAddresses },
        userPrefTextOnly,
        inlinePartAddresses,
        inlineImages = []
    ) => {
        let structure;
        const textPart = PartsBuilder.createTextPart(inlinePartAddresses[MimeType.TEXT_PLAIN][0]);

        if (userPrefTextOnly) {
            structure = textPart;
        } else {
            const htmlPart = PartsBuilder.createHtmlPart(inlinePartAddresses[MimeType.TEXT_HTML][0]);
            structure = PartsBuilder.createAlternativePart(textPart, htmlPart);
            structure = PartsBuilder.createInlineImageParts(
                structure,
                inlineImages,
                inlinePartAddresses[MimeType.IMAGE]
            );
        }
        structure = PartsBuilder.createAttachmentParts(attachments, structure);

        setMultipartAddresses(structure, multipartAddresses);

        return structure;
    },

    /**
     * Build the text representing this message as a previous message.
     * @example TEXT
     * `On Tuesday 2019 01 01, John Doe wrote:
     * > Dear Jane,
     * >  I could not bear to see you with Tarzan anymore,
     * > it will kill me! Please come back!
     * ...`
     */
    getTextFromStructure: async message => {
        const inlinePartsByCapabilities = message.inlinePartsByCapabilities,
            imapUid = message.remoteRef.imapUid,
            folderUid = message.folderRef.uid;
        let textPart = "";

        if (containsTextPlainAlternative(inlinePartsByCapabilities)) {
            const part = getTextPlainAlternative(inlinePartsByCapabilities);
            textPart = await PartsHelper.fetch(imapUid, folderUid, part, false);
        } else if (inlinePartsByCapabilities.length === 1) {
            for (const part of inlinePartsByCapabilities[0].parts) {
                if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
                    textPart += await PartsHelper.fetch(imapUid, folderUid, part, false);
                } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
                    textPart += html2text(await PartsHelper.fetch(imapUid, folderUid, part, false));
                }
            }
        } else {
            // FIXME support more structure
            console.error("Need to support more structure type..");
        }
        return textPart;
    },

    /**
     * Build the text representing this message as a previous message.
     * * @example HTML
     * `On Tuesday 2019 01 01, John Doe wrote:
     * <blockquote>
     * Dear Jane,
     * I could not bear to see you with Tarzan anymore,
     * it will kill me! Please come back!
     * ...
     * </blockquote>`
     */
    getHtmlFromStructure: async message => {
        const inlinePartsByCapabilities = message.inlinePartsByCapabilities,
            imapUid = message.remoteRef.imapUid,
            folderUid = message.folderRef.uid;
        let html = "",
            inlineImageParts = [];
        if (hasOnlyTextPlain(inlinePartsByCapabilities)) {
            const part = getTextPlainAlternative(inlinePartsByCapabilities);
            html = mailText2Html(await PartsHelper.fetch(imapUid, folderUid, part, false));
        } else {
            const byCapabilities =
                inlinePartsByCapabilities.length === 1
                    ? inlinePartsByCapabilities[0]
                    : inlinePartsByCapabilities.find(a => a.capabilities[0] === MimeType.TEXT_HTML);

            if (!byCapabilities) {
                // FIXME support more structure
                console.error("Need to support more structure type..");
                return { html, inlineImageParts };
            }

            for (const part of byCapabilities.parts) {
                if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
                    html += await PartsHelper.fetch(imapUid, folderUid, part, false); // FIXME: HTML needs to be sanitized here
                } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
                    html += mailText2Html(await PartsHelper.fetch(imapUid, folderUid, part, false));
                } else if (MimeType.isImage(part)) {
                    inlineImageParts.push(part);
                }
            }
        }
        html = sanitizeHtml(html);
        return { html, inlineImageParts };
    },

    sanitizeForCyrus(text) {
        return text.replace(/\r?\n/g, "\r\n");
    },

    /**
     * Compute the subject in function of creationMode (like "Re: My Subject" when replying).
     */
    addSubject: (message, creationMode, previousMessage) => {
        const vueI18n = inject("i18n");
        const subjectPrefix =
            creationMode === MessageCreationModes.FORWARD
                ? vueI18n.t("mail.compose.forward.subject")
                : vueI18n.t("mail.compose.reply.subject");
        // avoid subject prefix repetitions (like "Re: Re: Re: Re: My Subject")
        if (subjectPrefix !== previousMessage.subject.substring(0, subjectPrefix.length)) {
            return subjectPrefix + previousMessage.subject;
        }
        return previousMessage.subject;
    },

    addSeparator: (inlinePartContent, previousMessage, creationMode, expectedMimeType) => {
        let content = inlinePartContent;
        if (creationMode === MessageCreationModes.REPLY || creationMode === MessageCreationModes.REPLY_ALL) {
            content = adaptPreviousMessageForReply(expectedMimeType, inlinePartContent);
        }
        const lineBreakSeparator = expectedMimeType === MimeType.TEXT_PLAIN ? "\n" : "<br>";
        const separator =
            creationMode === MessageCreationModes.FORWARD
                ? buildSeparatorForForward(previousMessage, lineBreakSeparator)
                : buildSeparatorForReply(previousMessage);
        content = lineBreakSeparator + lineBreakSeparator + lineBreakSeparator + separator + content;

        if (expectedMimeType === MimeType.TEXT_HTML) {
            const attribute = MessageCreationModes.FORWARD
                ? MessageForwardAttributeSeparator
                : MessageReplyAttributeSeparator;
            content = "<div " + attribute + ">" + content + "</div>";
        }
        return content;
    },

    addRecipients: (message, creationMode, previousMessage, myEmail, myName) => {
        const isReplyAll = creationMode === MessageCreationModes.REPLY_ALL;

        const mailFollowUpTo = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_FOLLOWUP_TO);
        const mailReplyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.MAIL_REPLY_TO);
        const replyToHeader = previousMessage.headers.find(header => header.name === MessageHeader.REPLY_TO);

        if (isReplyAll && !mailFollowUpTo) {
            message.cc = previousMessage.cc.slice(0);
        }

        if (isReplyAll && mailFollowUpTo) {
            message.to = extractAddressesFromHeader(mailFollowUpTo, true);
        } else if (mailReplyToHeader) {
            message.to = extractAddressesFromHeader(mailReplyToHeader, isReplyAll);
        } else if (replyToHeader) {
            message.to = extractAddressesFromHeader(replyToHeader, isReplyAll);
        } else {
            // compute recipients from "From" or "To"
            let recipients = [previousMessage.from];
            if (isReplyAll) {
                // respond to sender and all recipients except myself
                recipients.push(...previousMessage.to);
                recipients = recipients.filter(r => r.address !== myEmail);
                // FIXME: avoid duplicates
                if (recipients.length === 0) {
                    // I was alone, respond to myself then
                    recipients = [{ address: myEmail, name: myName }];
                }
            } else if (recipients.map(r => r.address).includes(myEmail)) {
                // all recipients except myself
                recipients = previousMessage.to.filter(r => r.address !== myEmail);
                if (recipients.length === 0) {
                    // I was alone, respond to myself then
                    recipients = [{ address: myEmail, name: myName }];
                } else {
                    // respond to the first "not me" recipient only
                    recipients = [recipients[0]];
                }
            }
            message.to = recipients;
        }
    }
};

/**
 *  A separator before the previous message (forward).
 */
function buildSeparatorForForward(message, lineBreakSeparator) {
    const vueI18n = inject("i18n");
    let separator = vueI18n.t("mail.compose.forward.body") + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.subject");
    separator += ": " + message.subject + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.to");
    separator += ": " + message.to.map(to => nameAndAddress(to)) + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.date");
    separator += ": " + vueI18n.d(message.date, "full_date_time") + lineBreakSeparator;
    separator += vueI18n.t("mail.compose.forward.prev.message.info.from");
    separator += ": " + nameAndAddress(message.from) + lineBreakSeparator + lineBreakSeparator;
    return separator;
}

/**
 *  A separator before the previous message (reply).
 */
function buildSeparatorForReply(message) {
    const vueI18n = inject("i18n");
    return vueI18n.t("mail.compose.reply.body", {
        date: vueI18n.d(message.date, "full_date_time"),
        name: nameAndAddress(message.from)
    });
}

function adaptPreviousMessageForReply(expectedMimeType, content) {
    if (MimeType.equals(expectedMimeType, MimeType.TEXT_PLAIN)) {
        return (
            "\n\n" +
            content
                .split("\n")
                .map(line => "> " + line)
                .join("\n")
        );
    } else if (MimeType.equals(expectedMimeType, MimeType.TEXT_HTML)) {
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
            content +
            "</blockquote>"
        );
    }
}

/** @return like "John Doe <jdoe@bluemind.net>" */
function nameAndAddress(recipient) {
    return recipient.name ? recipient.name + " <" + recipient.address + ">" : recipient.address;
}

function setMultipartAddresses(structure, multipartAddresses) {
    let alternativePart;
    if (MimeType.isMultipart(structure)) {
        structure.address = "TEXT";
    }

    if (structure.mime === MimeType.MULTIPART_MIXED) {
        alternativePart = structure.children[0];
        alternativePart.address = multipartAddresses[MimeType.MULTIPART_ALTERNATIVE] || "1";
    } else {
        alternativePart = structure;
    }

    if (alternativePart.children && alternativePart.children[1].mime === MimeType.MULTIPART_RELATED) {
        alternativePart.children[1].address = multipartAddresses[MimeType.MULTIPART_RELATED] || "2";
    }
}

function containsTextPlainAlternative(inlinePartsByCapabilities) {
    if (inlinePartsByCapabilities.length > 1) {
        const textPart = inlinePartsByCapabilities.find(
            inlinesByCapability =>
                inlinesByCapability.capabilities.length === 1 &&
                inlinesByCapability.capabilities[0] === MimeType.TEXT_PLAIN
        );
        return !!textPart;
    } else {
        return (
            inlinePartsByCapabilities[0].parts.length === 1 &&
            inlinePartsByCapabilities[0].parts[0].mime === MimeType.TEXT_PLAIN
        );
    }
}

function getTextPlainAlternative(inlinePartsByCapabilities) {
    if (inlinePartsByCapabilities.length > 1) {
        return inlinePartsByCapabilities.find(
            inlinesByCapability =>
                inlinesByCapability.capabilities.length === 1 &&
                inlinesByCapability.capabilities[0] === MimeType.TEXT_PLAIN
        ).parts[0];
    } else {
        return inlinePartsByCapabilities[0].parts[0];
    }
}

function hasOnlyTextPlain(inlinePartsByCapabilities) {
    if (
        inlinePartsByCapabilities.length === 1 &&
        inlinePartsByCapabilities[0].parts.length === 1 &&
        inlinePartsByCapabilities[0].parts[0].mime === MimeType.TEXT_PLAIN
    ) {
        return true;
    }
    const htmlPart = inlinePartsByCapabilities.find(byCapabilities =>
        byCapabilities.capabilities.includes(MimeType.TEXT_HTML)
    );
    if (htmlPart) {
        return false;
    }
    const textPart = inlinePartsByCapabilities.find(byCapabilities =>
        byCapabilities.capabilities.includes(MimeType.TEXT_PLAIN)
    );
    return !!textPart;
}

function extractAddressesFromHeader(header, isReplyAll) {
    if (isReplyAll) {
        return header.values.map(value => EmailExtractor.extractEmail(value));
    } else {
        return [{ address: EmailExtractor.extractEmail(header.values[0]), name: "" }];
    }
}
