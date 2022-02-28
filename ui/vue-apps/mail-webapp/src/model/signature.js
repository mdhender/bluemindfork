import { createDocumentFragment } from "@bluemind/html-utils";
// import { BmRichEditor } from "@bluemind/styleguide";

export function addSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? addTextSignature(content, signature) : addHtmlSignature(content, signature);
}

export function removeSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? removeTextSignature(content, signature) : removeHtmlSignature(content, signature);
}

export function removeSignatureAttr(content) {
    const patternToRemove = "<" + HTML_SIGNATURE_TAG + " " + HTML_SIGNATURE_ATTR + '="">';
    return content.replace(patternToRemove, "<div>");
}

export function replaceSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? replaceTextSignature(content, signature) : replaceHtmlSignature(content, signature);
}

export function isSignaturePresent(content, userPrefTextOnly) {
    return userPrefTextOnly ? isTextSignaturePresent(content) : isHtmlSignaturePresent(content);
}

export function isTextHtmlSignatureEmpty(signature) {
    if (signature) {
        const signatureNode = createDocumentFragment(signature).firstElementChild;
        return !signatureNode.textContent && !hasImage(signatureNode);
    }
    return true;
}

const HTML_SIGNATURE_ATTR = "data-bm-signature"; // must match same attr defined in RichEditor.Signature extension and in sanitizeHtml
const HTML_SIGNATURE_TAG = "div";
const HTML_SIGNATURE_SELECTOR = HTML_SIGNATURE_TAG + "[" + HTML_SIGNATURE_ATTR + "]";
const TEXT_SIGNATURE_PREFIX = "--\n";

function isHtmlSignaturePresent(raw) {
    const fragment = createDocumentFragment(raw);
    const signature = fragment.querySelector(HTML_SIGNATURE_SELECTOR);
    return !!signature;
}

function removeHtmlSignature(raw) {
    const fragment = createDocumentFragment(raw);
    const signature = fragment.querySelector(HTML_SIGNATURE_SELECTOR);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    return fragment.firstElementChild.innerHTML;
}

function addHtmlSignature(raw, signatureContent) {
    if (isTextHtmlSignatureEmpty(signatureContent)) {
        return raw;
    }
    const fragment = createDocumentFragment(raw);
    let signature = fragment.querySelector(HTML_SIGNATURE_SELECTOR);
    if (!signature) {
        addSignatureToFragment(signatureContent, fragment);
        return fragment.firstElementChild.innerHTML;
    }
    return raw;
}

function addSignatureToFragment(signatureContent, fragment) {
    const signature = document.createElement("div");
    signature.setAttribute(HTML_SIGNATURE_ATTR, "");
    fragment.firstElementChild.appendChild(signature);
    signature.innerHTML = signatureContent;
}

function replaceHtmlSignature(raw, signatureContent) {
    if (isTextHtmlSignatureEmpty(signatureContent)) {
        return raw;
    }
    const fragment = createDocumentFragment(raw);
    const signature = fragment.querySelector(HTML_SIGNATURE_SELECTOR);
    if (signature) {
        signature.parentElement.removeChild(signature);
    }
    addSignatureToFragment(signatureContent, fragment);
    return fragment.firstElementChild.innerHTML;
}

function isTextSignaturePresent(raw) {
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX, "mi");
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

function hasImage(node) {
    const xPaths = [".//img[@src!='']", ".//*[contains(@style,'background-image:')]"];
    return xPaths.some(
        xpath => node.ownerDocument.evaluate(xpath, node, null, XPathResult.BOOLEAN_TYPE, null)?.booleanValue
    );
}
