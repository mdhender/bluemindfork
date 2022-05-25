import ExtensionsRegistry from "./ExtensionsRegistry";
import { normalizeMap } from "./normalizeMap";

export function mapExtensions(extension, data) {
    return normalizeMap(data).reduce((computed, { key, value }) => {
        return {
            ...computed,
            [key]: getExtensionValue(value, extension)
        };
    }, {});
}

function getExtensionValue(property, extension) {
    if (typeof property === "function") {
        return ExtensionsRegistry.get(extension).map(property);
    } else {
        return ExtensionsRegistry.get(extension, property);
    }
}
