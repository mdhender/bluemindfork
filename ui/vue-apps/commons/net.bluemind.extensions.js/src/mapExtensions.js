import { normalizeMap } from "./normalizeMap";

const sort = (a, b) => (a.order ? (b.order ? a.order - b.order : 1) : b.order ? -1 : 0);

export function mapExtensions(extension, data) {
    const res = {};
    const extensions = window.bmExtensions_[extension];
    if (extensions && extensions.length > 0) {
        normalizeMap(data).forEach(({ key, val }) => {
            res[key] = extensions
                .map(extension => {
                    let value = typeof val === "function" ? val.call(this, extension) : normalizeValue(extension[val]);
                    if (value) {
                        value.$id = extension.bundle;
                        return value;
                    }
                })
                .filter(Boolean)
                .sort(sort);
        });
    }
    return res;
}

function normalizeValue(value) {
    if (value) {
        const { body, children, ...properties } = value;
        const res = Object.assign(body || {}, properties);
        normalizeMap(children).forEach(({ key, val }) => (res[key] = normalizeValue(val)));
        return res;
    }
}
