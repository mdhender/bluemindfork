import { normalize } from "@bluemind/string";

export default { bind: updateHighlight, componentUpdated: updateHighlight };

/**
 * @param binding like "myPattern"
 *      or { pattern: "myPattern", text: "Provide current text if DOM element is re-used with different text content" }
 */
function updateHighlight(el, binding) {
    const { pattern, text, previousPattern, previousText } = parseBinding(binding);
    if (pattern !== previousPattern || text !== previousText) {
        unhighlight(el, text);
        if (pattern) {
            highlight(el, pattern);
        }
    }
}

function parseBinding(binding) {
    let { pattern, text } = binding.value || { pattern: "" };
    if (pattern === undefined) {
        pattern = binding.value;
    }
    const previousPattern = binding.oldValue?.pattern === undefined ? binding.oldValue : binding.oldValue.pattern;
    const previousText = binding.oldValue?.text;
    return { pattern, text, previousPattern, previousText };
}

function highlight(node, pattern) {
    findTextNodes(node).forEach(node => {
        const newContent = highlightMatchingContent(pattern, node.textContent);
        if (newContent !== node.textContent) {
            const newNode = createHighlightedElement(newContent, node.textContent);
            node.parentNode.insertBefore(newNode, node);
            node.textContent = "";
        }
    });
}
function findTextNodes(node) {
    const textNodes = [];
    walk(node, node => {
        if (node.nodeType === Node.TEXT_NODE) {
            textNodes.push(node);
        }
    });
    return textNodes;
}
export function highlightMatchingContent(matchPattern, text) {
    return text.replaceAll(createAccentInsensitiveRegex(matchPattern), "<mark>$1</mark>");
}

function createHighlightedElement(highlightedContent, text) {
    const highlightedNode = document.createElement("span");
    highlightedNode.setAttribute(highlightAttribute, text);
    highlightedNode.innerHTML = highlightedContent;

    return highlightedNode;
}

function createAccentInsensitiveRegex(searchTerm) {
    const regexString = [...normalize(searchTerm)].reduce(
        (regexString, char, i) => regexString + enhancedRegexpChar(searchTerm[i], char),
        ""
    );
    return new RegExp("(" + regexString + ")", "gi");
}

function enhancedRegexpChar(searchTerm, char) {
    const normalizableCharsMap = new Map([
        ["e", "[eéêè]"],
        ["i", "[iîï]"],
        ["a", "[aàâä]"],
        ["o", "[oô]"],
        ["c", "[cçĉ]"]
    ]);
    if (normalizableCharsMap.has(char)) {
        return normalizableCharsMap.get(char);
    }
    if (normalizableCharsMap.has(searchTerm)) {
        return normalizableCharsMap.get(searchTerm);
    }
    return char;
}

function unhighlight(node, initialText) {
    walk(node, node => {
        if (node.attributes && node.hasAttribute(highlightAttribute)) {
            node.nextSibling.textContent = initialText || node.getAttribute(highlightAttribute);
            node.remove();
        }
    });
}

function walk(node, fn) {
    fn(node);
    node.childNodes?.forEach(child => walk(child, fn));
}

const highlightAttribute = "data-highlight-directive-highlighted-node";
