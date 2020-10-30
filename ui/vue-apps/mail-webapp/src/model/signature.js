const HTML_SIGNATURE_ID = "x-bm-signature";
const TEXT_SIGNATURE_PREFIX = "--\n";

export function isHtmlSignaturePresent(raw) {
    const fragment = htmlAsFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    return !!signature && !!signature.innerHTML;
}

export function removeHtmlSignature(raw) {
    const fragment = htmlAsFragment(raw);
    const signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    return fragment.firstElementChild.innerHTML;
}

export function addHtmlSignature(raw, signatureContent) {
    const fragment = htmlAsFragment(raw);
    let signature = fragment.getElementById(HTML_SIGNATURE_ID);
    if (!signature) {
        signature = document.createElement("p");
        signature.id = HTML_SIGNATURE_ID;
        fragment.firstElementChild.appendChild(signature);
    }
    signature.innerHTML = signatureContent;
    fragment.firstElementChild.insertBefore(document.createElement("br"), signature);
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

export function removeTextSignature(raw, content) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "");
}
export function addTextSignature(raw, content) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, TEXT_SIGNATURE_PREFIX + content);
}

export function removeSignatureIds(content) {
    const regexp = new RegExp("id\\s*=\\s*['\"]\\s*" + HTML_SIGNATURE_ID + "\\s*['\"]", "g");
    return content.replace(regexp, "");
}
