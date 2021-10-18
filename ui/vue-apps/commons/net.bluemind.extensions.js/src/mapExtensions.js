import { normalizeMap } from "./normalizeMap";

const sort = ({ priority: a }, { priority: b }) => (b || 0) - (a || 0);

export function mapExtensions(extension, data) {
    const extensions = window.bmExtensions_[extension] || [];
    return normalizeMap(data).reduce((computed, { key, value }) => {
        return {
            ...computed,
            [key]: extensions.map(getExtensionValue.bind(null, value)).filter(Boolean).sort(sort)
        };
    }, {});
}

function getExtensionValue(property, extension) {
    let value = typeof property === "function" ? property.call(this, extension) : normalizeValue(extension[property]);
    if (value) {
        value.$id = extension.bundle;
        return value;
    }
}

function normalizeValue(value) {
    if (value) {
        const { body, children, ...properties } = value;
        const res = Object.assign(body || {}, properties);
        normalizeMap(children).forEach(({ key, value }) => (res[key] = normalizeValue(value)));
        return res;
    }
}
