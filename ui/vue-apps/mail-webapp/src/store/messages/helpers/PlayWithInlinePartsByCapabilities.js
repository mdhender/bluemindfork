import { html2text } from "@bluemind/html-utils";
import { mailText2Html, MimeType } from "@bluemind/email";
import { inject } from "@bluemind/inject";

import { fetch } from "../../../model/message";

/**
 * FIXME
 * Most of this code is hard to understand for a same reason :
 *    adapted structure (see computeParts in MessageAdaptor) has been done for MailViewer purpose.
 *    And we try here to use it for composer which has not the same needs
 *
 * Soluce : structure must not be adapted in state but in MailComposer and MailViewer according to their specific needs.
 * Steps :
 *      - move computeParts in MailViewer
 *      - list MailComposer needs and produce an adapted structure in MailComposer
 */

export default {
    getTextFromStructure: async message => {
        const inlinePartsByCapabilities = message.inlinePartsByCapabilities,
            imapUid = message.remoteRef.imapUid,
            folderUid = message.folderRef.uid;
        const service = inject("MailboxItemsPersistence", folderUid);
        let textPart = "";

        if (containsTextPlainAlternative(inlinePartsByCapabilities)) {
            const part = getTextPlainAlternative(inlinePartsByCapabilities);
            textPart = await fetch(imapUid, service, part, false);
        } else if (inlinePartsByCapabilities.length === 1) {
            for (const part of inlinePartsByCapabilities[0].parts) {
                if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
                    textPart += await fetch(imapUid, service, part, false);
                } else if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
                    textPart += html2text(await fetch(imapUid, service, part, false));
                }
            }
        } else {
            // FIXME support more structure
            console.error("Need to support more structure type..");
        }
        return textPart;
    },

    getHtmlFromStructure: async message => {
        const inlinePartsByCapabilities = message.inlinePartsByCapabilities,
            imapUid = message.remoteRef.imapUid,
            folderUid = message.folderRef.uid;
        const service = inject("MailboxItemsPersistence", folderUid);
        let html = "",
            inlineImageParts = [],
            inlineImagePartsContent = [];
        if (hasOnlyTextPlain(inlinePartsByCapabilities)) {
            const part = getTextPlainAlternative(inlinePartsByCapabilities);
            html = mailText2Html(await fetch(imapUid, service, part, false));
        } else {
            const byCapabilities =
                inlinePartsByCapabilities.length === 1
                    ? inlinePartsByCapabilities[0]
                    : inlinePartsByCapabilities.find(a => a.capabilities[0] === MimeType.TEXT_HTML);

            if (!byCapabilities) {
                // FIXME support more structure
                console.error("Need to support more structure type..");
                return { html, inlineImageParts, inlineImagePartsContent };
            }

            for (const part of byCapabilities.parts) {
                const partContent = await fetch(imapUid, service, part, false);
                if (MimeType.equals(part.mime, MimeType.TEXT_HTML)) {
                    html += partContent;
                } else if (MimeType.equals(part.mime, MimeType.TEXT_PLAIN)) {
                    html += mailText2Html(partContent);
                } else if (MimeType.isImage(part)) {
                    inlineImageParts.push(part);
                    inlineImagePartsContent.push(partContent);
                }
            }
        }
        return { html, inlineImageParts, inlineImagePartsContent };
    }
};

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
