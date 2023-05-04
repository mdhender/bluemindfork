import LuceneQueryParser from "lucene";
import patternParsers from "./patternParsers";
import PATTERN_KEYWORDS from "./Keywords";

function parseSearchPattern(pattern) {
    if (!pattern) {
        return {};
    }
    const result = {};
    const termsOnly = [];
    try {
        const rootNode = LuceneQueryParser.parse(pattern);
        const nodeFunction = (node, exit) => {
            const field = node.field;
            if (exit) {
                return true;
            }

            if (Object.values(PATTERN_KEYWORDS).includes(field)) {
                let value = "";
                if (patternParsers[field]) {
                    value = patternParsers[field](node);
                } else {
                    value = patternParsers.default(node);
                }
                result[field] = value;
            } else if (field) {
                termsOnly.push(LuceneQueryParser.toString(node));
            }
            return !!field;
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
const queryParsers = [folderParser, deepParser];

function parseQuery(expression) {
    let result = {};
    if (expression) {
        const rootNode = LuceneQueryParser.parse(expression);
        if (rootNode) {
            result.pattern = rootNode.left.term;
            const nodeFunction = node => queryParsers.forEach(parser => parser(node, result));
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

function walkLuceneTree(node, nodeFunction, context) {
    context = nodeFunction(node, context);
    if (node.left) {
        walkLuceneTree(node.left, nodeFunction, context);

        if (node.right) {
            walkLuceneTree(node.right, nodeFunction, context);
        }
    }
}

export const SearchHelper = { parseQuery, parseSearchPattern, isSameSearch };
export default SearchHelper;
