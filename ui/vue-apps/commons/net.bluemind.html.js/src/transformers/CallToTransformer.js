import { findPhoneNumbersInText } from "libphonenumber-js";
import EmptyTransformer from "./EmptyTransformer";

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
        const newNode = addCallTo(node.textContent, userLang);
        node.parentNode.replaceChild(newNode, node);
    } else if (node.nodeName !== "A") {
        node.childNodes.forEach(child => browseNode(child, userLang));
    }
}

function addCallTo(text, userLang) {
    let anyPhoneNumberFound = false;

    let lines = text.split("\n");
    lines = lines.map(line => {
        const tels = findPhoneNumbersInText(line, userLang.toUpperCase());
        tels.forEach(tel => {
            anyPhoneNumberFound = true;
            const before = line.substring(0, tel.startsAt);
            const after = line.substring(tel.endsAt, line.length);
            const adaptedCallTo =
                '<a href="callto:' + //
                tel.number.number + //
                '">' + //
                line.substring(tel.startsAt, tel.endsAt) + //
                "</a>";
            line = before + adaptedCallTo + after;
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
