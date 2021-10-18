export function normalizeMap(map) {
    if (!isValidMap(map)) {
        return [];
    }
    return Array.isArray(map)
        ? map.map(key => ({ key, value: key }))
        : Object.keys(map).map(key => ({ key, value: map[key] }));
}

function isValidMap(map) {
    return Array.isArray(map) || typeof map === "object";
}
