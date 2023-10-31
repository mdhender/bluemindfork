import { getDarkColor } from "roosterjs-color-utils";
import { ColorProcessor, ColorUnprocessor } from "./colorProcessing";

export function getDarkifiedCss(str, bgLvalue, customPropertiesMap) {
    const proc = new ColorProcessor(color => getDarkColor(color, bgLvalue), customPropertiesMap);
    return proc.applyToString(str);
}

export function getUndarkifiedCss(str) {
    const proc = new ColorUnprocessor();
    return proc.applyToString(str);
}

export function darkifyHtml(node, bgLvalue, customPropertiesMap) {
    const proc = new ColorProcessor(color => getDarkColor(color, bgLvalue), customPropertiesMap);
    proc.applyToHtmlTree(node);
}

export function undarkifyHtml(node) {
    const proc = new ColorUnprocessor();
    return proc.applyToHtmlTree(node);
}
