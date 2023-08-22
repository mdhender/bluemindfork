export const CUSTOM_PROPERTIES_NODE_ATTR = "data-bm-color-variables";

const CSS3_KEYWORD_COLORS = [
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
    "turquoise",
    "violet",
    "wheat",
    "whitesmoke",
    "yellowgreen",
    "rebeccapurple"
];

const SUPPORTED_CSS2_KEYWORD_COLORS = [
    "ActiveBorder",
    "ButtonText",
    "CaptionText",
    "GrayText",
    "Highlight",
    "HighlightText",
    "InfoBackground",
    "InfoText",
    "Menu",
    "MenuText",
    "Scrollbar",
    "Window",
    "WindowFrame",
    "WindowText"
];

const SUPPORTED_HTML_ATTRIBUTES = ["fill", "stroke", "text", "style"];

const DEPRECATED_HTML_ATTRIBUTES = [
    { name: "bgcolor", replacement: "backgroundColor" },
    { name: "color", replacement: "color" }
];

class ColorStringProcessor {
    #fn;
    #customPropertiesMap;
    regex;

    constructor(fn, customPropertiesMap) {
        this.#fn = fn;
        this.#customPropertiesMap = customPropertiesMap;
    }

    applyToString(str) {
        return str.replaceAll(this.regex, (...match) => {
            const name = "--" + this.colorCustomPropertyName(match);
            this.#customPropertiesMap.set(name, this.#fn(match[0].toLowerCase()));
            return `var(${name})`;
        });
    }
}
class ColorStringUnprocessor {
    regex;

    applyToString(str) {
        return str.replaceAll(this.regex, (...match) => this.colorValue(match));
    }
}

class HexColorStringProcessor extends ColorStringProcessor {
    constructor(fn, customPropertiesMap) {
        super(fn, customPropertiesMap);
        super.regex = /#([0-9a-f]+)/gi;
    }

    colorCustomPropertyName(match) {
        return `hex_${match[1].toLowerCase()}`;
    }
}
class HexColorStringUnprocessor extends ColorStringUnprocessor {
    constructor() {
        super();
        super.regex = /var\(--hex_([0-9a-f]+)\)/gi;
    }

    colorValue(match) {
        return `#${match[1]}`;
    }
}

const sep = {
    proc: "[, ]",
    unproc: "_"
};
const lastSep = {
    proc: "[,\\/]",
    unproc: "_"
};
const number = {
    proc: "[\\d.\\-\\+]+",
    unproc: "[\\d\\-m]+",
    replace(str) {
        return str.replaceAll("-", "m").replaceAll("+", "").replaceAll(".", "-");
    },
    unreplace(str) {
        return str.replaceAll("-", ".").replaceAll("m", "-");
    }
};
const numberArg = {
    proc: `\\s*(${number.proc}%?)\\s*`,
    unproc: `\\s*(${number.unproc}p?)\\s*`,
    replace(str) {
        return number.replace(str).replace("%", "p");
    },
    unreplace(str) {
        return number.unreplace(str).replace("p", "%");
    }
};
const percentArg = {
    proc: `\\s*(${number.proc}%)\\s*`,
    unproc: `\\s*(${number.unproc}p)\\s*`,
    ...numberArg
};
const angleArg = {
    proc: `\\s*(${number.proc}(?:%|deg)?)\\s*`,
    unproc: `\\s*(${number.unproc}[pd]?)\\s*`,
    replace(str) {
        return numberArg.replace(str).toLowerCase().replace("deg", "d");
    },
    unreplace(str) {
        return numberArg.unreplace(str).replace("d", "deg");
    }
};

const rgbArgs = {
    proc: `${numberArg.proc}${sep.proc}${numberArg.proc}${sep.proc}${numberArg.proc}(${lastSep.proc}${numberArg.proc})?`,
    unproc: `${numberArg.unproc}${sep.unproc}${numberArg.unproc}${sep.unproc}${numberArg.unproc}(${lastSep.unproc}${numberArg.unproc})?`
};

class RgbColorStringProcessor extends ColorStringProcessor {
    constructor(fn, customPropertiesMap) {
        super(fn, customPropertiesMap);
        super.regex = new RegExp(`(rgba?)\\(${rgbArgs.proc}\\)`, "gi");
    }

    colorCustomPropertyName(match) {
        let res = match[1];
        for (let i = 2; i <= 4; i++) {
            res += `_${numberArg.replace(match[i])}`;
        }
        if (match[6]) {
            res += `_${numberArg.replace(match[6])}`;
        }
        return res.toLowerCase();
    }
}
class RgbColorStringUnprocessor extends ColorStringUnprocessor {
    constructor() {
        super();
        super.regex = new RegExp(`var\\(--(rgba?)_${rgbArgs.unproc}\\)`, "gi");
    }

    colorValue(match) {
        let res = `${match[1]}(`;
        for (let i = 2; i <= 4; i++) {
            res += `${numberArg.unreplace(match[i])}${i < 4 ? ", " : ""}`;
        }
        if (match[6]) {
            res += `, ${numberArg.unreplace(match[6])}`;
        }
        res += ")";
        return res;
    }
}

const hslArgs = {
    proc: `${angleArg.proc}${sep.proc}${percentArg.proc}${sep.proc}${percentArg.proc}(${lastSep.proc}${numberArg.proc})?`,
    unproc: `${angleArg.unproc}${sep.unproc}${percentArg.unproc}${sep.unproc}${percentArg.unproc}(${lastSep.unproc}${numberArg.unproc})?`
};

class HslColorStringProcessor extends ColorStringProcessor {
    constructor(fn, customPropertiesMap) {
        super(fn, customPropertiesMap);
        super.regex = new RegExp(`(hsla?)\\(${hslArgs.proc}\\)`, "gi");
    }

    colorCustomPropertyName(match) {
        let res = match[1];
        res += `_${angleArg.replace(match[2])}`;
        res += `_${percentArg.replace(match[3])}_${percentArg.replace(match[4])}`;
        if (match[6]) {
            res += `_${numberArg.replace(match[6])}`;
        }
        return res.toLowerCase();
    }
}
class HslColorStringUnprocessor extends ColorStringUnprocessor {
    constructor() {
        super();
        super.regex = new RegExp(`var\\(--(hsla?)_${hslArgs.unproc}\\)`, "gi");
    }

    colorValue(match) {
        let res = `${match[1]}(`;
        res += `${angleArg.unreplace(match[2])}`;
        res += `, ${percentArg.unreplace(match[3])}, ${percentArg.unreplace(match[4])}`;
        if (match[6]) {
            res += `, ${numberArg.unreplace(match[6])}`;
        }
        res += ")";
        return res;
    }
}

const keyword = {
    proc: keywords => `(?<![\\w\\-.#])(${keywords.join("|")})\\b`,
    unproc: keywords => `var\\(--color_(${keywords.join("|")})\\)`
};

class Css3KeywordColorStringProcessor extends ColorStringProcessor {
    constructor(fn, customPropertiesMap) {
        super(fn, customPropertiesMap);
        super.regex = new RegExp(keyword.proc(CSS3_KEYWORD_COLORS), "gi");
    }

    colorCustomPropertyName(match) {
        return `color_${match[0].toLowerCase()}`;
    }
}
class Css3KeywordColorStringUnprocessor extends ColorStringUnprocessor {
    constructor() {
        super();
        super.regex = new RegExp(keyword.unproc(CSS3_KEYWORD_COLORS), "gi");
    }

    colorValue(match) {
        return match[1];
    }
}

class Css2KeywordColorStringProcessor extends ColorStringProcessor {
    constructor(customPropertiesMap) {
        super(c => this.getFallbackColor(c), customPropertiesMap);
        super.regex = new RegExp(keyword.proc(SUPPORTED_CSS2_KEYWORD_COLORS), "gi");
    }

    colorCustomPropertyName(match) {
        return `color_${match[0].toLowerCase()}`;
    }

    getFallbackColor(color) {
        switch (color.toLowerCase()) {
            case "activeborder":
                return "var(--neutral-fg)";
            case "buttontext":
                return "var(--neutral-fg)";
            case "captiontext":
                return "var(--neutral-fg-lo1)";
            case "graytext":
                return "var(--neutral-fg-lo2)";
            case "highlight":
                return "var(--fill-secondary-bg)";
            case "highlighttext":
                return "var(--fill-secondary-fg)";
            case "menu":
                return "var(--surface-hi1)";
            case "menutext":
                return "var(--neutral-fg)";
            case "window":
            case "windowframe":
                return "var(--surface)";
            case "windowtext":
                return "var(--neutral-fg-hi1)";
        }
        return "var(--neutral-fg-hi1)";
    }
}
class Css2KeywordColorStringUnprocessor extends ColorStringUnprocessor {
    constructor() {
        super();
        super.regex = new RegExp(keyword.unproc(SUPPORTED_CSS2_KEYWORD_COLORS), "gi");
    }

    colorValue(match) {
        return match[1];
    }
}

class ColorMultipleProcessor {
    processors = [];

    constructor(...processors) {
        this.processors = [...processors];
    }

    applyToString(str) {
        this.processors.forEach(processor => {
            str = processor.applyToString(str);
        });
        return str;
    }

    applyToHtmlElement(el) {
        if (el.tagName === "STYLE" && !el.hasAttribute(CUSTOM_PROPERTIES_NODE_ATTR)) {
            el.textContent = this.applyToString(el.textContent);
            return;
        }
        for (const attr of el.attributes) {
            if (SUPPORTED_HTML_ATTRIBUTES.includes(attr.name)) {
                attr.value = this.applyToString(attr.value);
            } else {
                const found = DEPRECATED_HTML_ATTRIBUTES.find(entry => entry.name === attr.name);
                if (found) {
                    el.style[found.replacement] = this.applyToString(attr.value);
                }
            }
        }
    }

    applyToHtmlTree(tree) {
        switch (tree.nodeType) {
            case Node.ELEMENT_NODE:
                this.applyToHtmlElement(tree);
            // fallthrough
            case Node.DOCUMENT_NODE:
            case Node.DOCUMENT_FRAGMENT_NODE:
                tree.childNodes.forEach(child => this.applyToHtmlTree(child));
        }
    }
}

export class ColorProcessor extends ColorMultipleProcessor {
    constructor(fn, customPropertiesMap) {
        super(
            new HexColorStringProcessor(fn, customPropertiesMap),
            new RgbColorStringProcessor(fn, customPropertiesMap),
            new HslColorStringProcessor(fn, customPropertiesMap),
            new Css3KeywordColorStringProcessor(fn, customPropertiesMap),
            new Css2KeywordColorStringProcessor(customPropertiesMap)
        );
    }
}
export class ColorUnprocessor extends ColorMultipleProcessor {
    constructor() {
        super(
            new HexColorStringUnprocessor(),
            new RgbColorStringUnprocessor(),
            new HslColorStringUnprocessor(),
            new Css3KeywordColorStringUnprocessor(),
            new Css2KeywordColorStringUnprocessor()
        );
    }
}
