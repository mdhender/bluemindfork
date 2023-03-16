import Color from "color";

export default function (themeColorName) {
    const propertyValue = getComputedStyle(document.body)
        .getPropertyValue("--" + themeColorName)
        .trim();
    const color = new Color(propertyValue);
    return color.lab().l();
}
