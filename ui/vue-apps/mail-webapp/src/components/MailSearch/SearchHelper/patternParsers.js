import LuceneQueryParser from "lucene";
import PATTERN_KEYWORDS from "./Keywords";
import LuceneTreeWalker from "./LuceneTreeWalker";

export default function parse(pattern, keyword) {
    try {
        const node = LuceneQueryParser.parse(pattern).left;
        switch (keyword) {
            case PATTERN_KEYWORDS.DATE:
                return dateParser(node);
            case PATTERN_KEYWORDS.SIZE:
                return sizeParser(node);
            case PATTERN_KEYWORDS.TO:
            case PATTERN_KEYWORDS.FROM:
            case PATTERN_KEYWORDS.CC:
            case PATTERN_KEYWORDS.WITH:
                return addressesParser(node);
            default:
                return defaultParser(node);
        }
    } catch {
        return null;
    }
}

function addressesParser(node) {
    let addresses = [];
    if (node.term) {
        addresses.push(node.term);
    } else {
        const nodeFunction = node => {
            if (node.term) {
                addresses.push(node.term);
            }
        };
        LuceneTreeWalker.walk(node, nodeFunction);
    }
    return addresses;
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

const INCLUDE_RANGE_LIMITS = {
    NONE: "none",
    LEFT: "left",
    RIGHT: "right",
    BOTH: "both"
};
function rangeParser(node) {
    let min, max;
    if (node.term_min || node.term_max) {
        switch (node.inclusive) {
            case INCLUDE_RANGE_LIMITS.LEFT:
                min = node.term_min;
                max = addDays(node.term_max, -1);
                break;
            case INCLUDE_RANGE_LIMITS.RIGHT:
                min = addDays(node.term_min, 1);
                max = node.term_max;
                break;
            case INCLUDE_RANGE_LIMITS.NONE:
                min = addDays(node.term_min, 1);
                max = addDays(node.term_max, -1);
                break;
            default:
                min = node.term_min;
                max = node.term_max;
        }
    } else if (node.term.length > 1 && (node.term.startsWith(">") || node.term.startsWith("<"))) {
        if (node.term[0] === ">") {
            min = node.term.substring(1);
        } else {
            max = node.term.substring(1);
        }
    }
    return { min: min || null, max: max || null };
}
function addDays(str, days) {
    const date = new Date(str);
    if (date instanceof Date && !isNaN(date)) {
        date.setDate(date.getDate() + days);
        return toIsoDate(date);
    }
}

function defaultParser(node) {
    return node?.term ? node.term : LuceneQueryParser.toString(node.right);
}

function toIsoDate(str) {
    if (!str) {
        return null;
    }
    try {
        return new Date(str).toISOString().substring(0, 10);
    } catch {
        return null;
    }
}
