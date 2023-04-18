import LuceneQueryParser from "lucene";

function parseSearchPattern(pattern, keywords) {
    if (!pattern) {
        return {};
    }
    const result = {};
    const termsOnly = [];
    try {
        const rootNode = LuceneQueryParser.parse(pattern);
        const nodeFunction = node => {
            if (keywords.includes(node.field)) {
                const value = node.term ? node.term : LuceneQueryParser.toString(node).split(":")?.pop();
                result[node.field] = value;
            } else if (node.field) {
                if (node.field === "<implicit>") {
                    termsOnly.push(node.term);
                } else {
                    termsOnly.push(LuceneQueryParser.toString(node));
                }
            }
        };
        walkLuceneTree(rootNode, nodeFunction);

        result.contains = termsOnly.join(" ");
    } catch {
        result.contains = pattern;
    }
    return result;
}

const folderParser = (node, result) => {
    if (node.field && node.field === "in") {
        result.folder = node.term;
    }
    return undefined;
};
const deepParser = (node, result) => {
    if (node.field && node.field === "is") {
        result.deep = result.deep || node.term === "deep";
    }
    return undefined;
};
const parsers = [folderParser, deepParser];

function parseQuery(expression) {
    let result = {};
    if (expression) {
        const rootNode = LuceneQueryParser.parse(expression);
        if (rootNode) {
            result.pattern = rootNode.left.term;
            const nodeFunction = node => parsers.forEach(parser => parser(node, result));
            walkLuceneTree(rootNode, nodeFunction);
        }
    }
    return result;
}

function isSameSearch(previousPattern, pattern, previousFolderKey, folderKey, isDeep, previousIsDeep) {
    const isSamePattern = pattern === previousPattern;
    const isSameFolder = folderKey === previousFolderKey;
    const isSameDepth = !!isDeep === !!previousIsDeep;
    const isAllFoldersAgain = !folderKey && !previousFolderKey;
    return isSamePattern && isSameDepth && (isSameFolder || isAllFoldersAgain);
}

function walkLuceneTree(node, nodeFunction) {
    nodeFunction(node);
    if (node.left) {
        walkLuceneTree(node.left, nodeFunction);

        if (node.right) {
            walkLuceneTree(node.right, nodeFunction);
        }
    }
}

export const SearchHelper = { parseQuery, parseSearchPattern, isSameSearch };
export default SearchHelper;
