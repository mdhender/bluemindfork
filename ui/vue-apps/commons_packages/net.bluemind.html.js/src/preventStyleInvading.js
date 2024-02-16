/**
 * WARNING: this is an internal const, it's exported just for testing purpose
 */
export const WRAPPER_CLASS = "bm-composer-content-wrapper";

export default function preventStyleInvading(html) {
    const tmpDoc = new DOMParser().parseFromString(html, "text/html");

    const styleRules = getStyleRules(tmpDoc);

    const rootDiv = tmpDoc.createElement("div");
    rootDiv.classList.add(WRAPPER_CLASS);
    rootDiv.innerHTML = tmpDoc.body.innerHTML;

    const rootDivStyleTags = rootDiv.getElementsByTagName("style");
    while (rootDivStyleTags.length > 0) {
        rootDivStyleTags.item(0).remove();
    }

    const styleNode = document.createElement("style");
    styleNode.innerHTML = styleRules;
    rootDiv.appendChild(styleNode);

    return rootDiv.outerHTML;
}

/**
 * WARNING: this is an internal method, it's exported just for testing purpose
 */
export function getStyleRules(doc) {
    let styleRules = "";
    const styleTags = getStyleSheets(doc);
    for (let tag of styleTags) {
        for (let rule of tag.cssRules) {
            if (rule.selectorText) {
                rule.selectorText = computeNewSelector(rule.selectorText);
                styleRules += "\n" + rule.cssText;
            }
        }
    }
    return styleRules;
}

function getStyleSheets(doc) {
    let styleSheets = doc.styleSheets;
    if (!styleSheets?.length) {
        styleSheets = [];
        const findStyleNode = nodes => nodes.find(c => c.tagName === "STYLE");
        const headStyleNode = findStyleNode([...doc.head.children]);
        const headStyleSheet = styleNodeToStyleSheet(headStyleNode);
        if (headStyleSheet) {
            styleSheets.push(headStyleSheet);
        }
        const bodyStyleNode = findStyleNode([...doc.body.children]);
        const bodyStyleSheet = styleNodeToStyleSheet(bodyStyleNode);
        if (bodyStyleSheet) {
            styleSheets.push(bodyStyleSheet);
        }
    }
    return styleSheets;
}

function styleNodeToStyleSheet(node) {
    const nodeContent = node?.textContent;
    if (nodeContent) {
        const cssStyleSheet = new CSSStyleSheet();
        if (cssStyleSheet.replaceSync) {
            cssStyleSheet.replaceSync(nodeContent);
        } else {
            try {
                cssStyleSheet.insertRule(nodeContent, 0);
            } catch {
                console.error("Invalid css rule: ", nodeContent);
            }
        }
        return cssStyleSheet;
    }
}

/**
 * WARNING: this is an internal method, it's exported just for testing purpose
 */
export function computeNewSelector(selectorText) {
    let selectors = selectorText.split(",");
    return selectors
        .map(selector => selector.trim().replace(/^([\s>+~]*(html|body)(\.[^\s>]*)?[\s]*)*/g, ""))
        .map(selector => "." + WRAPPER_CLASS + " " + selector)
        .join(",");
}
