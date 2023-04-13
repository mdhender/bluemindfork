import { getDarkColor } from "roosterjs-color-utils";
import Color from "color";

export function darkifyCss(str, bgLvalue) {
    return searchAndReplaceColors(str, color => getDarkColor(color, bgLvalue));
}

export function darkifyHtml(el, bgLvalue) {
    applyDeep(el, el => darkifyElement(el, color => getDarkColor(color, bgLvalue)));
}

const KEYWORD_COLORS = [
    "black",
    "silver",
    "gray",
    "white",
    "maroon",
    "red",
    "purple",
    "fuchsia",
    "green",
    "lime",
    "olive",
    "yellow",
    "navy",
    "blue",
    "teal",
    "aqua",
    "orange",
    "aliceblue",
    "antiquewhite",
    "aquamarine",
    "azure",
    "beige",
    "bisque",
    "blanchedalmond",
    "blueviolet",
    "brown",
    "burlywood",
    "cadetblue",
    "chartreuse",
    "chocolate",
    "coral",
    "cornflowerblue",
    "cornsilk",
    "crimson",
    "cyan",
    "darkblue",
    "darkcyan",
    "darkgoldenrod",
    "darkgray",
    "darkgreen",
    "darkgrey",
    "darkkhaki",
    "darkmagenta",
    "darkolivegreen",
    "darkorange",
    "darkorchid",
    "darkred",
    "darksalmon",
    "darkseagreen",
    "darkslateblue",
    "darkslategray",
    "darkslategrey",
    "darkturquoise",
    "darkviolet",
    "deeppink",
    "deepskyblue",
    "dimgray",
    "dimgrey",
    "dodgerblue",
    "firebrick",
    "floralwhite",
    "forestgreen",
    "gainsboro",
    "ghostwhite",
    "gold",
    "goldenrod",
    "greenyellow",
    "grey",
    "honeydew",
    "hotpink",
    "indianred",
    "indigo",
    "ivory",
    "khaki",
    "lavender",
    "lavenderblush",
    "lawngreen",
    "lemonchiffon",
    "lightblue",
    "lightcoral",
    "lightcyan",
    "lightgoldenrodyellow",
    "lightgray",
    "lightgreen",
    "lightgrey",
    "lightpink",
    "lightsalmon",
    "lightseagreen",
    "lightskyblue",
    "lightslategray",
    "lightslategrey",
    "lightsteelblue",
    "lightyellow",
    "limegreen",
    "linen",
    "magenta",
    "mediumaquamarine",
    "mediumblue",
    "mediumorchid",
    "mediumpurple",
    "mediumseagreen",
    "mediumslateblue",
    "mediumspringgreen",
    "mediumturquoise",
    "mediumvioletred",
    "midnightblue",
    "mintcream",
    "mistyrose",
    "moccasin",
    "navajowhite",
    "oldlace",
    "olivedrab",
    "orangered",
    "orchid",
    "palegoldenrod",
    "palegreen",
    "paleturquoise",
    "palevioletred",
    "papayawhip",
    "peachpuff",
    "peru",
    "pink",
    "plum",
    "powderblue",
    "rosybrown",
    "royalblue",
    "saddlebrown",
    "salmon",
    "sandybrown",
    "seagreen",
    "seashell",
    "sienna",
    "skyblue",
    "slateblue",
    "slategray",
    "slategrey",
    "snow",
    "springgreen",
    "steelblue",
    "tan",
    "thistle",
    "tomato",
    "transparent",
    "turquoise",
    "violet",
    "wheat",
    "whitesmoke",
    "yellowgreen",
    "rebeccapurple"
];

function searchAndReplaceColors(str, fn) {
    const hex = "#\\w+";
    const matchingParens = "\\([^)(]*(?:\\([^)(]*(?:\\([^)(]*(?:\\([^)(]*\\)[^)(]*)*\\)[^)(]*)*\\)[^)(]*)*\\)"; // see https://stackoverflow.com/questions/546433/regular-expression-to-match-balanced-parentheses
    const hslRgb = `(hsl|rgb)a?${matchingParens}`;
    const keyword = `(?<![\\w\\-.#])(${KEYWORD_COLORS.join("|")})`;
    const re = new RegExp(`${hex}|${hslRgb}|${keyword}`, "g");
    return str.replaceAll(re, fn);
}

const SUPPORTED_CSS_PROPERTIES = [
    "accent-color",
    "background-color",
    "border-bottom-color",
    "border-left-color",
    "border-right-color",
    "border-top-color",
    "caret-color",
    "color",
    "column-rule-color",
    "outline-color",
    "text-decoration-color",
    "text-emphasis-color"
];

const SUPPORTED_HTML_ATTRIBUTES = ["bgcolor", "color", "text", "stroke", "fill"];

function hex(colorStr) {
    return new Color(colorStr).hex();
}

function darkifyElement(el, darkifyColor) {
    if (el.tagName === "STYLE") {
        el.textContent = darkifyCss(el.textContent);
        return;
    }
    for (const attr of el.attributes) {
        if (SUPPORTED_HTML_ATTRIBUTES.includes(attr.name)) {
            attr.value = hex(darkifyColor(attr.value));
        }
    }
    for (const key of SUPPORTED_CSS_PROPERTIES) {
        const value = el.style.getPropertyValue(key);
        if (!value) {
            continue;
        }
        el.style.setProperty(key, darkifyColor(value));
    }
}

function applyDeep(el, fn) {
    if (el.nodeType !== Node.ELEMENT_NODE) {
        return;
    }
    fn(el);
    for (const child of el.childNodes) {
        applyDeep(child, fn);
    }
}
