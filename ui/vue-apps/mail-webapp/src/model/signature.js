const HTML_SIGNATURE_ID = "x-bm-signature";
const TEXT_SIGNATURE_PREFIX = "--\n";

export function isHtmlSignaturePresent(raw) {
    const fragment = htmlAsFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    return !!signature && !!signature.innerHTML;
}

function removeHtmlSignature(raw) {
    const fragment = htmlAsFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    return fragment.firstElementChild.innerHTML;
}

function addHtmlSignature(raw, signatureContent) {
    const fragment = htmlAsFragment(raw);
    let signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (!signature) {
        signature = document.createElement("p");
        signature.id = HTML_SIGNATURE_ID;
        fragment.firstElementChild.appendChild(document.createElement("br"));
        fragment.firstElementChild.appendChild(signature);
    }
    signature.innerHTML = signatureContent;
    return fragment.firstElementChild.innerHTML;
}

function htmlAsFragment(content) {
    const fragment = document.createDocumentFragment();
    fragment.appendChild(document.createElement("p"));
    fragment.firstElementChild.innerHTML = content;
    return fragment;
}

export function isTextSignaturePresent(raw, content) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return regexp.test(raw);
}

function removeTextSignature(raw, content) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "");
}
function addTextSignature(raw, content) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, TEXT_SIGNATURE_PREFIX + content);
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
