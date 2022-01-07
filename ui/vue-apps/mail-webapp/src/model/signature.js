import { createDocumentFragment } from "@bluemind/html-utils";

const HTML_SIGNATURE_ID = "x-bm-signature";
const TEXT_SIGNATURE_PREFIX = "--\n";

export function isHtmlSignaturePresent(raw) {
    const fragment = createDocumentFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    return !!signature && !!signature.innerHTML;
}

function removeHtmlSignature(raw) {
    const fragment = createDocumentFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    return fragment.firstElementChild.innerHTML;
}

function addHtmlSignature(raw, signatureContent) {
    const fragment = createDocumentFragment(raw);
    let signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (!signature) {
        addSignatureToFragment(signatureContent, fragment);
        return fragment.firstElementChild.innerHTML;
    }
    return raw;
}

function addSignatureToFragment(signatureContent, fragment) {
    const signature = document.createElement("div");
    signature.id = HTML_SIGNATURE_ID;
    fragment.firstElementChild.appendChild(document.createElement("br"));
    fragment.firstElementChild.appendChild(signature);
    signature.innerHTML = signatureContent;
}

function replaceHtmlSignature(raw, signatureContent) {
    const fragment = createDocumentFragment(raw);
    let signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    addSignatureToFragment(signatureContent, fragment);
    return fragment.firstElementChild.innerHTML;
}

export function isTextSignaturePresent(raw, content) {
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return regexp.test(raw);
}

function removeTextSignature(raw, content) {
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "");
}
function addTextSignature(raw, content) {
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, TEXT_SIGNATURE_PREFIX + content);
}

function replaceTextSignature(raw, content) {
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "").replace(regexp, TEXT_SIGNATURE_PREFIX + content);
}

export function removeSignatureIds(content) {
    const regexp = new RegExp("id\\s*=\\s*['\"]\\s*" + HTML_SIGNATURE_ID + "\\s*['\"]", "g");
    return content.replace(regexp, "");
}

export function addSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? addTextSignature(content, signature) : addHtmlSignature(content, signature);
}

export function removeSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? removeTextSignature(content, signature) : removeHtmlSignature(content, signature);
}

export function replaceSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? replaceTextSignature(content, signature) : replaceHtmlSignature(content, signature);
}
