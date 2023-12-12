import { MailTipTypes } from "../mailTip";
import * as CorporateSignature from "./corporateSignature";

export function removeSignature(content, userPrefTextOnly, signature) {
    return userPrefTextOnly ? removeTextSignature(content, signature) : removeHtmlSignature(content);
}

function removeTextSignature(raw, content) {
    const TEXT_SIGNATURE_PREFIX = "--\n";
    // FIXME does not work if 'content' contains regex special characters like '('
    const regexp = new RegExp("^" + TEXT_SIGNATURE_PREFIX + content, "mi");
    return raw.replace(regexp, "");
}
const WRAPPER_TAG = "div";
const PERSONAL_SIGNATURE_ATTR = "data-bm-signature"; // must match same attr defined in sanitizeHtml

function removeHtmlSignature(raw) {
    const removeSignatureRegex = new RegExp(
        "<" + WRAPPER_TAG + " " + PERSONAL_SIGNATURE_ATTR + '=\\".*?\\">.*?</' + WRAPPER_TAG + ">"
    );
    return raw.replace(removeSignatureRegex, "");
}

export const PERSONAL_SIGNATURE_SELECTOR = (id = "default") =>
    WRAPPER_TAG + "[" + PERSONAL_SIGNATURE_ATTR + '="' + id + '"]';

export function removeSignatureAttr(content) {
    const removeRegex = new RegExp("<" + WRAPPER_TAG + " " + PERSONAL_SIGNATURE_ATTR + '=\\".*?\\">');
    return content.replace(removeRegex, "<" + WRAPPER_TAG + ">");
}

export function wrapPersonalSignature({ html, id }) {
    const wrapper = document.createElement(WRAPPER_TAG);
    wrapper.setAttribute(PERSONAL_SIGNATURE_ATTR, id);
    wrapper.innerHTML = html;
    return wrapper;
}

const CORPORATE_SIGNATURE_CLASSNAME = "bm-corporate-signature";
const DISCLAIMER_CLASSNAME = "bm-disclaimer";

export function wrapCorporateSignature(html) {
    const wrapper = document.createElement(WRAPPER_TAG);
    wrapper.setAttribute("class", CORPORATE_SIGNATURE_CLASSNAME);
    wrapper.innerHTML = html;
    return wrapper;
}
export function wrapDisclaimer(html) {
    const wrapper = document.createElement(WRAPPER_TAG);
    wrapper.setAttribute("class", DISCLAIMER_CLASSNAME);
    wrapper.innerHTML = html;
    return wrapper;
}

export const CORPORATE_SIGNATURE_PLACEHOLDER = "--X-BM-SIGNATURE--";
export const CORPORATE_SIGNATURE_SELECTOR = WRAPPER_TAG + '[class~="' + CORPORATE_SIGNATURE_CLASSNAME + '"]';
export const DISCLAIMER_SELECTOR = WRAPPER_TAG + '[class~="' + DISCLAIMER_CLASSNAME + '"]';

export function removeCorporateSignatureContent(content, { corporateSignature, disclaimer }) {
    return CorporateSignature.remove(content, { corporateSignature, disclaimer });
}

export function isDisclaimer(mailTip) {
    if (mailTip.mailtipType === MailTipTypes.SIGNATURE) {
        return JSON.parse(mailTip.value).isDisclaimer;
    }
    return false;
}

export function isCorporateSignature(mailTip) {
    if (mailTip.mailtipType === MailTipTypes.SIGNATURE) {
        return !JSON.parse(mailTip.value).isDisclaimer;
    }
    return false;
}

/** Remove leading and trailing empty lines. */
function trimSignature(signature) {
    if (!signature?.length) {
        return;
    }

    let root = new DOMParser().parseFromString(signature, "text/html")?.body;

    while (root.childElementCount === 1 && root.children[0].hasChildNodes()) {
        root = root.children[0];
    }

    const lines = root && Array.from(root.childNodes); // result OF ARRAY.FROM is ALWAY TRUE IN NEXT LINE IF (???)
    if (lines) {
        for (const line of lines) {
            if (isSignatureLineEmpty(line)) {
                root.removeChild(line);
            } else {
                break;
            }
        }
        if (root.hasChildNodes()) {
            for (const line of lines.reverse()) {
                if (isSignatureLineEmpty(line)) {
                    root.removeChild(line);
                } else {
                    break;
                }
            }
        }
        return root.innerHTML;
    }
    return signature;
}

function isSignatureLineEmpty(node) {
    if (!node || node.nodeType !== Node.ELEMENT_NODE) {
        return true;
    }
    return !node.textContent?.trim() && !containsImage(node);
}

function containsImage(node) {
    return (
        (node.tagName === "IMG" && node.getAttribute("src")) ||
        node.querySelector("img[src]:not([src=''])") ||
        node.style.getPropertyValue("background-image") ||
        node.querySelector("[style*='background-image']")?.style.getPropertyValue("background-image")
    );
}

export default {
    CORPORATE_SIGNATURE_PLACEHOLDER,
    CORPORATE_SIGNATURE_SELECTOR,
    DISCLAIMER_SELECTOR,
    isCorporateSignature,
    isDisclaimer,
    isSignatureLineEmpty,
    PERSONAL_SIGNATURE_SELECTOR,
    removeCorporateSignatureContent,
    removeSignature,
    removeSignatureAttr,
    trimSignature,
    wrapCorporateSignature,
    wrapDisclaimer,
    wrapPersonalSignature
};
