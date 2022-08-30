import partition from "lodash.partition";
import PostalMime from "postal-mime";

const Cache = new Map();

export default {
    async parseBodyStructure({ bodyVersion, date, guid, ...options }, eml) {
        const body = { bodyVersion, date, guid, ...options };
        const message = Cache.has(guid) ? Cache.get(guid) : await new PostalMime().parse(eml);
        // Cache.set(guid, message);
        body.smartAttach = message.attachments.some(({ disposition }) => disposition === "attachment");
        body.structure = buildStructure(message);
        if (message.text) {
            body.preview = `[Decrypted message] ${message.text}`;
        } else if (message.html) {
            body.preview = `[Decrypted message] ${message.html.replaceAll(/<[^>]*>/g, "")}`;
        }
        body.preview = body.preview.replaceAll("\n", "").replaceAll("\n", "").substring(0, 120).trim();
        return body;
    }
};

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
                encoding: ""
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
