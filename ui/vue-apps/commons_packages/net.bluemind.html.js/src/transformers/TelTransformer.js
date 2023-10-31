import { findPhoneNumbersInText } from "libphonenumber-js";
import EmptyTransformer from "./EmptyTransformer";

/**
 * Transforms phone numbers to phone links.
 *
 * 'tel:' scheme was used in the late 1990s and documented in early 2000 with RFC 2806 (which was obsoleted by the more-thorough RFC 3966 in 2004)
 * and continues to be improved. Supporting tel: on the iPhone was not an arbitrary decision.
 * 'callto:' while supported by Skype, is not a standard and should be avoided unless specifically targeting Skype users.
 */

export default class {
    constructor(transformer, lang) {
        this.transformer = transformer || new EmptyTransformer();
        this.lang = lang;
    }

    transform(text) {
        return linkify(this.transformer.transform(text), this.lang);
    }
}

function linkify(text, userLang) {
    const node = document.createElement("div");
    node.innerHTML = text;

    if (node.childNodes.length === 0) {
        return text;
    }

    node.childNodes.forEach(child => {
        browseNode(child, userLang);
    });

    return node.innerHTML;
}

function browseNode(node, userLang) {
    if (node.nodeName === "#text") {
        const newNode = addPhoneLinks(node.textContent, userLang);
        node.parentNode.replaceChild(newNode, node);
    } else if (node.nodeName !== "A") {
        node.childNodes.forEach(child => browseNode(child, userLang));
    }
}

function addPhoneLinks(text, userLang) {
    let anyPhoneNumberFound = false;

    let lines = text.split("\n");
    lines = lines.map(line => {
        const tels = findPhoneNumbersInText(line, userLang.toUpperCase());
        let offset = 0;
        tels.forEach(tel => {
            anyPhoneNumberFound = true;
            const before = line.substring(0, tel.startsAt + offset);
            const after = line.substring(tel.endsAt + offset, line.length + offset);
            const link = `<a href="tel:${tel.number.number}">${line.substring(
                tel.startsAt + offset,
                tel.endsAt + offset
            )}</a>`;
            const newLine = `${before}${link}${after}`;
            offset += newLine.length - line.length;
            line = newLine;
        });
        return line;
    });

    if (!anyPhoneNumberFound) {
        return document.createTextNode(text);
    }

    const newText = lines.join("\n");
    const newNode = document.createElement("div");
    newNode.innerHTML = newText;
    return newNode;
}
