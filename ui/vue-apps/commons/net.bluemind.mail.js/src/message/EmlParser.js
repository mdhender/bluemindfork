import partition from "lodash.partition";
import PostalMime from "postal-mime";
import { MessageBodyRecipientKind as RecipientKind } from "@bluemind/backend.mail.api";

const Cache = new Map();
const CACHE_MAX = 20;

export default {
    async parseBodyStructure({ bodyVersion, date, guid, ...options }, eml) {
        const body = { bodyVersion, date, guid, ...options, size: eml.size };
        const uid = uniqueId(eml);
        const message = await cachedParse(eml, uid);
        return build(body, message).body;
    },
    async parseEml(eml) {
        const uid = await uniqueId(eml);
        const message = await cachedParse(eml, uid);
        const { body, partsData } = build({ bodyVersion: 0, date: message.date, size: eml.size }, message);
        return { body, partsData, uid };
    }
};

async function uniqueId(eml) {
    const arrayBuffer = await eml.arrayBuffer();
    const sha = await crypto.subtle.digest("SHA-256", arrayBuffer);
    return window.btoa(String.fromCharCode(...new Uint8Array(sha)));
}

async function cachedParse(eml, uid) {
    let message = Cache.get(uid);
    if (!message) {
        message = await new PostalMime().parse(eml);
        Cache.set(uid, message);
        if (Cache.size > CACHE_MAX) {
            const firstEntryKey = Cache.keys().next()?.value;
            Cache.delete(firstEntryKey);
        }
    }
    return message;
}

function build(body, message) {
    body.smartAttach = message.attachments.some(({ disposition }) => disposition === "attachment");
    const rootPart = buildStructure(message);
    const partsData = {};
    const visit = part => {
        if (part.content) {
            partsData[part.address] = part.content;
            delete part.content;
        }
        part.children?.forEach(visit);
    };
    visit(rootPart);
    body.structure = rootPart;

    if (message.text) {
        body.preview = message.text;
    } else if (message.html) {
        body.preview = message.html.replaceAll(/<[^>]*>/g, "");
    }
    body.preview = body.preview.replaceAll("\n", "").replaceAll("\n", "").substring(0, 120).trim();

    const from = { kind: RecipientKind.Originator, dn: message.from.name, address: message.from.address };
    const toArray = message.to?.map(to => ({ kind: RecipientKind.Primary, dn: to.name, address: to.address })) || [];
    const ccArray = message.cc?.map(cc => ({ kind: RecipientKind.CarbonCopy, dn: cc.name, address: cc.address })) || [];
    const bccArray =
        message.bcc?.map(bcc => ({
            kind: RecipientKind.BlindCarbonCopy,
            dn: bcc.name,
            address: bcc.address
        })) || [];
    body.recipients = [from, ...toArray, ...ccArray, ...bccArray];

    body.date = message.date;
    body.headers = message.headers.map(({ key, value }) => {
        let json;
        try {
            json = JSON.parse(value);
        } catch {
            json = null;
        }
        return { name: key, values: json ? [value] : value.split(/,\s*/) };
    });
    body.messageId = message.messageId;
    body.subject = message.subject;

    return { body, partsData };
}

function buildStructure({ html, text, attachments }) {
    const [related, files] = partition(attachments, "related");
    let textPart = DefaultPartBuilder.build({ content: text, mimeType: "text/plain" });
    let htmlPart = DefaultPartBuilder.build({ content: html, mimeType: "text/html" });
    const relatedParts = related.map(attachment => DefaultPartBuilder.build(attachment));
    htmlPart = RelatedPartBuilder.build({ main: htmlPart, related: relatedParts });
    const content = DefaultMultiPartBuilder.build({ subType: "alternative", parts: [textPart, htmlPart] });
    const fileParts = files.map(attachment => DefaultPartBuilder.build(attachment));
    const structure = DefaultMultiPartBuilder.build({ subType: "mixed", parts: [content, ...fileParts] });
    return structure;
}

const DefaultPartBuilder = {
    build({ mimeType: mime, content, disposition, filename, contentId }) {
        if (content) {
            const part = {
                address: "1",
                dispositionType: disposition?.toUpperCase() || "INLINE",
                mime,
                size: content instanceof ArrayBuffer ? content.byteLength : content.length,
                encoding: "",
                content
            };
            if (contentId) {
                part.contentId = contentId;
            }
            if (filename) {
                part.fileName = filename;
            }
            return part;
        }
    }
};

const RelatedPartBuilder = {
    build({ main, related }) {
        related = related.filter(Boolean);
        if (related.length > 0 && main) {
            return {
                mime: "multipart/related",
                address: "TEXT",
                size: 0,
                children: adopt([main, ...related])
            };
        }
        return main;
    }
};

const DefaultMultiPartBuilder = {
    build({ parts, subType }) {
        parts = parts.filter(Boolean);
        if (parts.length > 1) {
            return {
                mime: `multipart/${subType}`,
                address: "TEXT",
                size: 0,
                children: adopt(parts)
            };
        }
        return parts[0];
    }
};

function adopt(parts, prefix = "") {
    return parts.map((part, index) => {
        part.address = `${prefix}${index + 1}`;
        if (part.children) {
            part.children = adopt(part.children, `${part.address}.`);
        }
        return part;
    });
}
