const MESSAGE_QUERY_PARTS = {
    mailbox: "m",
    folder: "d",
    search: "s",
    filter: "f"
};

export default {
    parse(path) {
        return pathToMessageQuery(MESSAGE_QUERY_PARTS, path);
    },
    build(path, params) {
        const old = pathToMessageQuery(MESSAGE_QUERY_PARTS, path);
        return messageQueryToPath(MESSAGE_QUERY_PARTS, Object.assign(old, params));
    }
};

function pathToMessageQuery(parts, path) {
    const patterns = Object.keys(parts).map(partToRegexp(parts));
    var regexp = new RegExp("^" + patterns.join("") + "$");
    if (regexp.test(path)) {
        return extractGroups(regexp.exec(path), Object.keys(parts));
    } else {
        return {};
    }
}

function extractGroups(matches, groups) {
    const result = {};
    groups.forEach((key, index) => (result[key] = matches[(index + 1) * 2]));
    return result;
}

function partToRegexp(parts) {
    return part => "(\\/?\\." + parts[part] + "\\/(.*?))?";
}

function messageQueryToPath(parts, messageQuery) {
    return Object.keys(parts)
        .map(part => messageQuery[part] && "." + parts[part] + "/" + messageQuery[part])
        .filter(Boolean)
        .join("/");
}
