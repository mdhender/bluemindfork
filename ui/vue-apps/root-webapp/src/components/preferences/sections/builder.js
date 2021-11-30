import sortedIndexBy from "lodash.sortedindexby";
import { RoleCondition, StoreFieldCondition } from "../conditions";

export function merge(existing, extension) {
    return mergeInternal(existing, normalize(extension));
}

const DEFAULT_PRIORITY = 1;

function mergeInternal(existings, additionals = [], mergeFn = mergeSection) {
    additionals.forEach(entry => {
        const index = existings.findIndex(({ id }) => entry.id === id);
        if (index >= 0) {
            entry = mergeFn(existings[index], entry);
        }
        existings.splice(index >= 0 ? index : existings.length, index >= 0 ? 1 : 0, entry);
    });
    return existings;
}

function mergeSection(existing, section) {
    const { categories, ...params } = section;
    mergeInternal(existing.categories, categories, mergeCategory);
    return { ...existing, ...params };
}

function mergeCategory(existing, category) {
    const { groups, ...params } = category;
    mergeInternal(existing.groups, groups, mergeGroup);
    return { ...existing, ...params };
}

function mergeGroup(existing, group) {
    const { fields, ...params } = group;
    mergeInternal(existing.fields, fields, mergeField);
    return { ...existing, ...params };
}

function mergeField(existing, field) {
    return { ...existing, ...field };
}

function normalize(data) {
    data = Array.isArray(data) ? data : [data];
    return data.map(toSection);
}

function toSection(entry) {
    const levels = entry.id.split(".");
    if (levels.length === 4) {
        entry = { fields: [{ ...entry, id: levels.pop() }] };
    }
    if (levels.length === 3) {
        entry = { groups: [{ ...entry, id: levels.pop() }] };
    }
    if (levels.length === 2) {
        entry = { categories: [{ ...entry, id: levels.pop() }] };
    }
    if (levels.length === 1) {
        entry = { ...entry, id: levels.pop() };
    }
    return entry;
}

export function sanitize(entries, sanitizer = sanitizeSection, prefix = []) {
    return (entries || [])
        .reduce((values, entry) => {
            const sanitized = sanitizer(entry, prefix);
            values.splice(sortedIndexBy(values, sanitized, "priority"), 0, sanitized);
            return values;
        }, [])
        .reverse();
}

function sanitizeSection(section, prefix) {
    let id = sanitizeId(section.id, prefix);
    return {
        ...section,
        priority: sanitizePriority(section.priority),
        visible: sanitizeCondition(section.visible, true),
        categories: sanitize(section.categories, sanitizeCategory, id.split(".")),
        id
    };
}

function sanitizeCategory(category, prefix) {
    let id = sanitizeId(category.id, prefix);
    return {
        ...category,
        priority: sanitizePriority(category.priority),
        visible: sanitizeCondition(category.visible, true),
        groups: sanitize(category.groups, sanitizeGroup, id.split(".")),
        id
    };
}
function sanitizeGroup(group, prefix) {
    let id = sanitizeId(group.id, prefix);
    return {
        ...group,
        priority: sanitizePriority(group.priority),
        visible: sanitizeCondition(group.visible, true),
        disabled: sanitizeCondition(group.disabled, false),
        fields: sanitize(group.fields, sanitizeField, id.split(".")),
        id
    };
}
function sanitizeField(field, prefix) {
    return {
        ...field,
        priority: sanitizePriority(field.priority),
        visible: sanitizeCondition(field.visible, true),
        disabled: sanitizeCondition(field.disabled, false),
        keywords: sanitizeKeywords(field.keywords),
        id: sanitizeId(field.id, prefix)
    };
}

function sanitizePriority(priority) {
    return isNaN(parseInt(priority)) ? DEFAULT_PRIORITY : parseInt(priority);
}
function sanitizeId(id, prefix) {
    return [...prefix, id.split(".").pop()].join(".");
}
function sanitizeKeywords(keywords) {
    if (typeof keywords === "string") {
        return [keywords];
    }
    if (!Array.isArray(keywords)) {
        return [];
    }
    return keywords;
}

function sanitizeCondition(value, fallback) {
    try {
        const type = typeof value === "object" ? value.constructor.name.toLowerCase() : typeof value;
        switch (type) {
            case "string":
                if (/^\w+$/.test(value)) {
                    return value.toLowerCase().trim() === "true";
                }
                return execute({ name: "Function", args: [value] });
            case "array":
                return execute({ name: "Function", args: value });
            case "object":
                return execute(value);
            case "boolean":
            case "number":
                return Boolean(value);
            case "function":
                return value;
            case "undefined":
            default:
                return fallback;
        }
    } catch {
        return fallback;
    }
}

const conditionFns = {
    RoleCondition,
    StoreFieldCondition,
    Function
};

function execute({ name, args = [] }) {
    let fn = name.split(".").reduce((fn, name) => fn[name], conditionFns);
    if (typeof fn === "function") {
        return fn.apply(null, args);
    }
    throw `Unknwon function name ${name}`;
}

export function reactive(entries, vm) {
    entries.forEach(entry => {
        for (let property in entry) {
            if (Array.isArray(entry[property])) {
                reactive(entry[property], vm);
            } else if (["visible", "disabled"].includes(property)) {
                computed(entry, property, vm);
            }
        }
    });
    return entries;
}

function computed(entry, property, vm) {
    const subject = entry[property];
    if (typeof subject === "function") {
        vm.$watch(subject, value => (entry[property] = value), { immediate: true });
    }
}
