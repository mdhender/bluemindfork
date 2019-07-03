import defaults from 'lodash.defaults';

import defaultFormat from './Formatter';

// Which type of tags should not be parsed
const SKIPTAGS = [
    'style',
    'script',
    'head',
    'iframe',
    'frame',
    'frameset'
];

const SKIPTYPES = [
    Node.ATTRIBUTE_NODE,
    Node.CDATA_SECTION_NODE,
    Node.ENTITY_REFERENCE_NODE,
    Node.ENTITY_NODE,
    Node.PROCESSING_INSTRUCTION_NODE,
    Node.COMMENT_NODE,
    Node.DOCUMENT_TYPE_NODE,
    Node.NOTATION_NODE
];

const SKIPATTRIBUTES = {
    'hidden': 'true',
    'aria-hidden': 'true'
};

const SKIPSTYLES = {
    'display': 'none',
    'visibility': 'hidden'
};

function htmlToText(html, options) {
    defaults(options, {
        tables: [],
        ignore: ['img'],
        noHref: false,
        headingToUpperCase: true,
        boldToUpperCarse: false,
        lineLength: 120
    });

    const dom = new DOMParser().parseFromString(html, 'text/html');

    return accept(dom, defaultFormat, options);
}


function adapt(element) {
    element.name = element.tagName && element.tagName.toLowerCase() || '';
    element.nodes = Array.from(element.childNodes);
    element.accept = accept;
    if (element.style && ['block', 'inline-block'].indexOf(element.style.display) > -1) {
        element.display = 'block';
    } else if (element.style && ['run-in', 'inline'].indexOf(element.style.display) > -1) {
        element.display = 'inline';
    } else if (element.name && display[element.name]) {
        element.display = display[element.name];
    } else {
        element.display = 'inline';
    }
    return element;
}

function skip(element, options) {
    if (options.ignore.indexOf(element.name) > -1 || SKIPTAGS.indexOf(element.name) > -1) {
        return true;
    }
    if (element.nodeType == Node.ELEMENT_NODE) {
        for (const attr in SKIPATTRIBUTES) {
            if (element.attributes[attr] && element.attributes[attr].value == SKIPATTRIBUTES[attr]) {
                return true;
            }
        }
        for (const style in SKIPSTYLES) {
            if (element.style[style] && element.style[style] == SKIPSTYLES[style]) {
                return true;
            }
        }
    }
    return false;
}


function accept(element, formatter, options) {
    if (SKIPTYPES.indexOf(element.nodeType) > -1) {
        return '';
    }
    const node = adapt(element);
    if (skip(node, options)) {
        return '';
    } else {
        return formatter.visit(node, formatter.context, options);
    }
}

const display = {
    address: 'block',
    article: 'block',
    blockquote: 'block',
    body: 'block',
    caption: 'block',
    center: 'block',
    content: 'block',
    datalist: 'block',
    details: 'block',
    dialog: 'block',
    dir: 'block',
    div: 'block',
    dl: 'block',
    dt: 'block',
    dd: 'block',
    fieldset: 'block',
    figure: 'block',
    footer: 'block',
    form: 'block',
    frame: 'block',
    frameset: 'block',
    h1: 'block',
    h2: 'block',
    h3: 'block',
    h4: 'block',
    h5: 'block',
    h6: 'block',
    header: 'block',
    hgroup: 'block',
    hr: 'block',
    html: 'block',
    iframe: 'block',
    legend: 'block',
    li: 'block',
    listing: 'block',
    main: 'block',
    marquee: 'block',
    menu: 'block',
    menuitem: 'block',
    nav: 'block',
    nobr: 'block',
    noscript: 'block',
    ol: 'block',
    optgroup: 'block',
    option: 'block',
    p: 'block',
    plaintext: 'block',
    pre: 'block',
    section: 'block',
    summary: 'block',
    table: 'block',
    tbody: 'block',
    tfoot: 'block',
    thead: 'block',
    title: 'block'
};

export const fromString = function (str) {
    return htmlToText(str, {});
};
export default { fromString };