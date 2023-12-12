import { MimeType } from "@bluemind/email";
import { ISignature } from "./signature.interface";

const WRAPPER_TAG = "div";
const CORPORATE_SIGNATURE_CLASSNAME = "bm-corporate-signature";

export function wrapCorporateSignature(html: string) {
    const wrapper = document.createElement(WRAPPER_TAG);
    wrapper.setAttribute("class", CORPORATE_SIGNATURE_CLASSNAME);
    wrapper.innerHTML = html;
    return wrapper;
}
const DISCLAIMER_CLASSNAME = "bm-disclaimer";

export const CORPORATE_SIGNATURE_PLACEHOLDER = "--X-BM-SIGNATURE--";
export const CORPORATE_SIGNATURE_SELECTOR = WRAPPER_TAG + '[class~="' + CORPORATE_SIGNATURE_CLASSNAME + '"]';
export const DISCLAIMER_SELECTOR = WRAPPER_TAG + '[class~="' + DISCLAIMER_CLASSNAME + '"]';

export function remove(
    content: string,
    { corporateSignature, disclaimer }: { corporateSignature: ISignature; disclaimer: Nullable<ISignature> }
) {
    let html = content;

    if (corporateSignature || disclaimer) {
        const htlmDoc = new DOMParser().parseFromString(html, MimeType.TEXT_HTML);
        if (corporateSignature) {
            const element = htlmDoc.querySelector(CORPORATE_SIGNATURE_SELECTOR);
            if (element) {
                if (corporateSignature.usePlaceholder) {
                    element.replaceWith(CORPORATE_SIGNATURE_PLACEHOLDER);
                    html = htlmDoc.body.innerHTML;
                } else {
                    element.remove();
                    html = htlmDoc.body.innerHTML;
                }
            }
        }
        if (disclaimer) {
            const element = htlmDoc.querySelector(DISCLAIMER_SELECTOR);
            if (element) {
                element.remove();
                html = htlmDoc.body.innerHTML;
            }
        }
    }
    return html;
}

type Nullable<T> = T | null;
