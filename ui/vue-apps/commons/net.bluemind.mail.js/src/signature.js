export function removeSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? removeTextSignature(content, signature) : removeHtmlSignature(content);
}

export function removeSignatureAttr(content) {
    const removeRegex = new RegExp("<" + SIGNATURE_WRAPPER_TAG + " " + PERSONAL_SIGNATURE_ATTR + '=\\".*?\\">');
    return content.replace(removeRegex, "<" + SIGNATURE_WRAPPER_TAG + ">");
}

const SIGNATURE_WRAPPER_TAG = "div";
const PERSONAL_SIGNATURE_ATTR = "data-bm-signature"; // must match same attr defined in sanitizeHtml
const CORPORATE_SIGNATURE_CLASSNAME = "bm-corporate-signature";

export const PERSONAL_SIGNATURE_SELECTOR = id =>
    SIGNATURE_WRAPPER_TAG + "[" + PERSONAL_SIGNATURE_ATTR + '="' + id + '"]';
export const CORPORATE_SIGNATURE_PLACEHOLDER = "--X-BM-SIGNATURE--";
export const CORPORATE_SIGNATURE_SELECTOR = SIGNATURE_WRAPPER_TAG + '[class="' + CORPORATE_SIGNATURE_CLASSNAME + '"]';

export function wrapPersonalSignature({ html, id }) {
    const wrapper = document.createElement(SIGNATURE_WRAPPER_TAG);
    wrapper.setAttribute(PERSONAL_SIGNATURE_ATTR, id);
    wrapper.innerHTML = html;
    return wrapper;
}

export function wrapCorporateSignature(html) {
    const wrapper = document.createElement(SIGNATURE_WRAPPER_TAG);
    wrapper.setAttribute("class", CORPORATE_SIGNATURE_CLASSNAME);
    wrapper.innerHTML = html;
    return wrapper;
}

export function isDisclaimer(mailTip) {
    if (mailTip.mailtipType === "Signature") {
        return JSON.parse(mailTip.value).isDisclaimer;
    }
    return false;
}

export function isCorporateSignature(mailTip) {
    if (mailTip.mailtipType === "Signature") {
        return !JSON.parse(mailTip.value).isDisclaimer;
    }
    return false;
}

function removeHtmlSignature(raw) {
    const removeSignatureRegex = new RegExp(
        "<" + SIGNATURE_WRAPPER_TAG + " " + PERSONAL_SIGNATURE_ATTR + '=\\".*?\\">.*?</' + SIGNATURE_WRAPPER_TAG + ">"
    );
    return raw.replace(removeSignatureRegex, "");
}

const TEXT_SIGNATURE_PREFIX = "--\n";
function removeTextSignature(raw, content) {
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "");
}

function isSignatureLineEmpty(node) {
    // no text or no images
    return (
        !node ||
        (!node.innerText?.trim() &&
            (node.nodeType !== Node.ELEMENT_NODE ||
                (!node.querySelector("img[src]:not([src=''])") &&
                    !node.style.getPropertyValue("background-image") &&
                    !node.querySelector("[style*='background-image']")?.style.getPropertyValue("background-image"))))
    );
}

/** Remove leading and trailing empty lines. */
function trimSignature(signature) {
    if (signature?.length) {
        const body = new DOMParser().parseFromString(signature, "text/html")?.body;
        const lines = body && Array.from(body.childNodes);
        if (lines) {
            for (const line of lines) {
                if (isSignatureLineEmpty(line)) {
                    body.removeChild(line);
                } else {
                    break;
                }
            }
            if (body.hasChildNodes()) {
                for (const line of lines.reverse()) {
                    if (isSignatureLineEmpty(line)) {
                        body.removeChild(line);
                    } else {
                        break;
                    }
                }
            }
            return body.innerHTML;
        }
        return signature;
    }
}

export default {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    isCorporateSignature,
    isDisclaimer,
    PERSONAL_SIGNATURE_SELECTOR,
    removeSignature,
    removeSignatureAttr,
    trimSignature,
    wrapCorporateSignature,
    wrapPersonalSignature
};
