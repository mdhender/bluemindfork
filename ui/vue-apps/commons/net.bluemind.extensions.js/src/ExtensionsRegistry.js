import global from "@bluemind/global";
import { normalizeMap } from "./normalizeMap";

const sort = ({ priority: a }, { priority: b }) => (b || 0) - (a || 0);
const LoadingStatus = { LOADED: true, LOADING: false };

class ExtensionsRegistry {
    constructor() {
        this.extensions = new ExtensionsMap();
        // Extension auto loading on window... should we keep it ?
        if (self?.bmExtensions_) {
            this.load(self.bmExtensions_);
        }
    }
    load(extensions) {
        for (const point in extensions) {
            extensions[point].forEach(extension =>
                this.extensions.add(point, normalizeExtension(extension, extension.bundle))
            );
        }
    }
    register(point, origin, extension) {
        this.extensions.add(point, normalizeExtension(extension, origin));
    }
    get(point, property) {
        const extensions = this.extensions.get(point);
        if (property) {
            return extensions
                .map(extension => extension[property])
                .filter(Boolean)
                .sort(sort);
        }
        return extensions;
    }
    isLoaded(extension) {
        return extension.$loaded.status === LoadingStatus.LOADED;
    }
    clear() {
        this.extensions = new ExtensionsMap();
    }
}
function normalizeExtension(value, id) {
    const isAsync = Boolean(self.bundleResolve);
    const $loaded = { status: isAsync ? LoadingStatus.LOADING : LoadingStatus.LOADED };
    const extension = normalizeElement(value, { $id: id, $loaded });
    if (isAsync) {
        self.bundleResolve(id, () => ($loaded.status = LoadingStatus.LOADED));
    }
    return extension;
}

function normalizeElement(value, extra = {}) {
    if (value) {
        const body = normalizeBody(value);
        const { attributes, children } = normalizeProperties(value);
        const res = Object.assign(body || {}, attributes, extra);
        children.forEach(({ key, value }) => (res[key] = normalizeElement(value, extra)));
        return res;
    }
}

function normalizeBody(value) {
    if (value.body) {
        return value.body;
    } else if (!isPlainObject(value)) {
        return value;
    }
    return undefined;
}

function normalizeProperties(value) {
    const { body, children, ...attributes } = value;
    if (children || body) {
        return { children: normalizeMap(children), attributes };
    } else if (!isPlainObject(value)) {
        return { children: [], attributes: {} };
    } else {
        return normalizeMap(attributes).reduce(
            ({ children, attributes }, { key, value }) => {
                if (typeof value === "object") {
                    children.push({ key, value });
                } else {
                    attributes[key] = value;
                }
                return { children, attributes };
            },
            { children: [], attributes: {} }
        );
    }
}

class ExtensionsMap extends Map {
    get(key) {
        if (!this.has(key)) {
            this.set(key, []);
        }
        return super.get(key);
    }
    add(key, data) {
        this.get(key).push(data);
    }
}

function isPlainObject(object) {
    const isObject = object => Object.prototype.toString.call(object) === "[object Object]";
    return (
        isObject(object) &&
        object.constructor?.prototype?.hasOwnProperty?.call(object.constructor.prototype, "isPrototypeOf")
    );
}

export default global.$extensions || (global.$extensions = new ExtensionsRegistry());
