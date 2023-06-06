export function getCustomProperty(name, element = document.documentElement) {
    return getComputedStyle(element)
        .getPropertyValue("--" + name)
        .trim();
}

export function setCustomProperty(name, value, element = document.documentElement) {
    element.style.setProperty("--" + name, value);
}
