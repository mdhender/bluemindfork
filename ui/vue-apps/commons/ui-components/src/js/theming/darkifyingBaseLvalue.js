import Color from "color";

export default function () {
    const propertyValue = getComputedStyle(document.body).getPropertyValue("--darkified-content-bg").trim();
    const color = new Color(propertyValue);
    return color.lab().l();
}
