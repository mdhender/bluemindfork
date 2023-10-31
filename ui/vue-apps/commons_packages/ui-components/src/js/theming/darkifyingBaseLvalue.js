import Color from "color";
import { getCustomProperty } from "./customProperty";

export default function () {
    const propertyValue = getCustomProperty("darkified-content-bg", document.body);
    const color = new Color(propertyValue);
    return color.lab().l();
}
