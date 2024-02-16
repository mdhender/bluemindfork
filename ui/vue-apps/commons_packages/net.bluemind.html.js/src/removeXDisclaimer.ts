export function removeXDisclaimer(content: string) {
    return createHtmlFragment(content).remove(" > div[class^=bm-composer] > div[class^=x-disclaimer]").innerHtml();
}

function createHtmlFragment<T extends string>(content: T) {
    const ROOT_ELEMENT = "root";

    const htmlFragment = document.createRange().createContextualFragment(createHtmlElement(ROOT_ELEMENT, content));

    return {
        remove(selector: string) {
            removeMatchingSelectors(htmlFragment, ROOT_ELEMENT + selector);
            return this;
        },
        innerHtml() {
            return extractInnerHtml(htmlFragment, ROOT_ELEMENT);
        }
    };
}

function removeMatchingSelectors(fragment: DocumentFragment, selector: string) {
    const nodesToRemove = fragment.querySelectorAll(selector);

    for (const node of nodesToRemove) {
        node.remove();
    }
}

function extractInnerHtml(fragment: DocumentFragment, selector: string) {
    return fragment?.querySelector(selector)?.innerHTML;
}

function createHtmlElement(tagName: string, content: string) {
    return `<${tagName}>${content}</${tagName}>`;
}
