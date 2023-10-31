export default { bind: toggleHighlight, componentUpdated: toggleHighlight, update: toggleHighlight };

function toggleHighlight(el, binding) {
    binding.value ? highlight(el, binding.value) : unhighlight(el);
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
    const normalizedSearchTerm = searchTerm.normalize("NFD").replace(/\p{Diacritic}/gu, "");
    const regexString = [...normalizedSearchTerm].reduce(
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

function unhighlight(node) {
    walk(node, node => {
        if (node.attributes && node.hasAttribute(highlightAttribute)) {
            node.nextSibling.textContent = node.getAttribute(highlightAttribute);
            node.remove();
        }
    });
}

function walk(node, fn) {
    fn(node);
    node.childNodes?.forEach(child => walk(child, fn));
}

const highlightAttribute = "data-highlight-directive-highlighted-node";
