import repeat from 'lodash.repeat';
import startsWith from 'lodash.startswith';

export function visit(node, context, options) {
    if (!context) context = defaultContext();
    let result = '';
    if (node.nodeType === Node.ELEMENT_NODE) {
        const fn = mapping[node.name] || mapping[node.display] || visitChildren;
        result = fn(node, context, options);
    } else if (node.nodeType === Node.TEXT_NODE) {
        result = text(node.data, context, options);
    } else {
        result = visitChildren(node, context, options);
    }

    return result;
}

function visitChildren(node, context, options) {
    let result = '';
    node.nodes.forEach(function (child) {
        result += node.accept(child, {
            visit,
            context
        }, options);

    });

    return result;
}

function sp(context) {
    if (!context.clear && context.spacer !== '\n') {
        context.spacer = ' ';
    }
}

function nl(context) {
    if (!context.clear) {
        context.spacer = '\n';
    }
}

function spacer(context, force) {
    if (!force && (!context.spacer || context.clear)) return '';
    if (context.spacer === '\n') {
        context.clear = true;
    }
    const val = context.spacer;
    context.spacer = '';
    return val;
}


function text(txt, context) {
    let content = txt;
    if (!context.pre) {
        if (/^\s/.test(txt)) sp(context);
        content = content.replace(/\s+/g, ' ').trim();
    }
    if (content.length > 0) {
        content = spacer(context) + content;
        context.clear = false;
    }
    if (!context.pre && /\s$/.test(txt)) {
        sp(context);
    }
    return content;
}

function ignore() {
    return '';
}

function pushContext(context, key, value) {
    if (!context._shadow[key]) context._shadow[key] = [];
    context._shadow[key].push(context[key]);
    context[key] = value;
}
function popContext(context, key) {
    context[key] = context._shadow[key].pop();
}

function inline(node, context, options) {
    return visitChildren(node, context, options);
}

function block(node, context, options) {
    nl(context);
    const result = visitChildren(node, context, options);
    nl(context);
    return result;
}

function a(node, context, options) {
    // Always get the anchor text
    let content = visitChildren(node, context, options);
    let href = node.href;
    if (!options.noHref && href && !startsWith(node.href, 'tel:')) {
        // Get the href, if present
        href.replace(/^mailto:/, '');
        if (href) {
            if (context.base && href.indexOf('/') === 0) {
                href = options.linkHrefBaseUrl + href;
            }
            if (href !== content.replace('\n', '')) {
                content += text(' [' + href + ']', context, options);
            }
        }
    }
    return content;
}


function base(node, context) {
    context.url = node.src;
}

function br(node, context) {
    const content = spacer(context, true);
    nl(context);
    return content;
}

function dd() {
    // TODO
}

function heading(node, context, options) {
    const result = block(node, context, options);
    if (options.headingToUpperCase) {
        return result.toUpperCase();
    }
    return result;
}

function img(node) {
    let result = '';
    if (node.alt) {
        result += node.alt;
        if (node.src) {
            result += ' ';
        }
    }
    if (node.src) {
        result += '[' + node.src + ']';
    }
    return (result);
}

function p(node, context, options) {
    return block(node, context, options) + '\n';
}

function pre(node, context, options) {
    pushContext(context, 'pre', true);
    const content = block(node, context, options);
    popContext(context, 'pre');
    return content;
}

function hr(node, context, options) {
    nl(context);
    const content = text(repeat('-', options.lineLength, context, options));
    nl(context);
    return content;
}

function li(node, context, options) {
    nl(context);
    pushContext(context, 'pre', true);
    const prefix = text(context.prefix, context, options);
    popContext(context, 'pre');
    let content = visitChildren(node, context, options);
    content = content.replace(/\n/g, '\n' + repeat(' ', context.prefix.length));
    nl(context);
    return prefix + content;
}

function ul(node, context, options) {
    pushContext(context, 'prefix', ' * ');
    const content = block(node, context, options);
    popContext(context, 'prefix');
    return content;
}

function ol(node, context) {
    context.prefix += ' % ';
    context.count.push((node.type || '1').charCodeAt(0) + (node.start || 1) - 1);
    const content = visitChildren();
    context.prefix = context.prefix.substring(context.prefix.length - 3, 3);
    context.count.pop();
    return content + '\n';
}


function formatAsTable(node, options) {
    if (options.tables === true) return true;
    var classes = options.tables.filter(t => startsWith(t, '.')).map(t => t.substr(1));
    var ids = options.tables.filter(t => startsWith(t, '#')).map(t => t.substr(1));
    return classes.indexOf(node.className) > -1 || ids.indexOf(node.id) > -1;
}

function strong(node, context, options) {
    const result = inline(node, context, options);
    if (options.boldToUpperCase) {
        return result.toUpperCase();
    }
    return result;
}

function table(node, context, options) {
    if (!formatAsTable(node, options)) {
        return block(node, context, options);
    }
    //TODO..
    return block(node, context, options);
}

function td(node, context, options) {
    if (!context.table) {
        sp(context);
        return inline(node, context, options);
    }
}

function th(node, context, options) {
    if (options.headingToUpperCase) {
        return td(node, context, options).toUpperCase();
    } else {
        return td(node, context, options);
    }
}

function tr(node, context, options) {
    if (!context.table) {
        return block(node, context, options);
    }
}


const mapping = {
    inline,
    block,
    a: a,
    applet: ignore,
    area: ignore,
    audio: ignore,
    base: base,
    b: strong,
    br: br,
    button: ignore,
    canvas: ignore,
    col: ignore,
    colgroup: ignore,
    command: ignore,
    datalist: ignore,
    dd: dd,
    h1: heading,
    h2: heading,
    h3: heading,
    h4: heading,
    h5: heading,
    h6: heading,
    head: ignore,
    hr: hr,
    image: img,
    img: img,
    isindex: ignore,
    keygen: ignore,
    li: li,
    link: ignore,
    listing: pre,
    map: ignore,
    menu: ignore,
    menuitem: ignore,
    meta: ignore,
    multicol: ignore,
    nobr: pre,
    noscript: ignore,
    object: ignore,
    ol: ol,
    optgroup: ignore,
    option: ignore,
    p: p,
    param: ignore,
    plaintext: pre,
    pre: pre,
    script: ignore,
    select: ignore,
    source: ignore,
    spacer: ignore,
    strong: strong,
    style: ignore,
    table: table,
    td: td,
    template: ignore,
    textarea: ignore,
    th: th,
    tr: tr,
    title: heading,
    ul: ul
};

function defaultContext() {
    return {
        count: [],
        clear: true,
        space: '',
        pre: false,
        prefix: '',
        table: false,
        spacer: '',
        _shadow: {}
    };
}

export default { visit };