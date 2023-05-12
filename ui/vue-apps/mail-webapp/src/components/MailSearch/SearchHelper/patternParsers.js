import LuceneQueryParser from "lucene";
import PATTERN_KEYWORDS from "./Keywords";

export default function parse(pattern, keyword) {
    try {
        const node = LuceneQueryParser.parse(pattern).left;
        switch (keyword) {
            case PATTERN_KEYWORDS.DATE:
                return dateParser(node);
            case PATTERN_KEYWORDS.SIZE:
                return sizeParser(node);
            default:
                return defaultParser(node);
        }
    } catch {
        return null;
    }
}

function dateParser(node) {
    let { min, max } = rangeParser(node);
    if (!min && !max && toIsoDate(node.term)) {
        min = node.term;
        max = node.term;
    }
    return { min: toIsoDate(min), max: toIsoDate(max) };
}

function sizeParser(node) {
    const { min, max } = rangeParser(node);
    return {
        min: +min > 0 ? +min : null,
        max: +max > 0 ? +max : null
    };
}
function rangeParser(node) {
    let min, max;
    if (node.term_min) {
        min = node.term_min;
        max = node.term_max;
    } else if (node.term.length > 1 && (node.term.startsWith(">") || node.term.startsWith("<"))) {
        if (node.term[0] === ">") {
            min = node.term.substring(1);
        } else {
            max = node.term.substring(1);
        }
    }
    return { min, max };
}

function defaultParser(node) {
    return node?.term ? node.term : LuceneQueryParser.toString(node.right);
}

function toIsoDate(str) {
    try {
        return new Date(str).toISOString().substring(0, 10);
    } catch {
        return null;
    }
}
