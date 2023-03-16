import { darkifyHtml } from "../js/theming/darkify";
import themeColorLvalue from "../js/theming/themeColorLvalue";

function enabled(binding) {
    if (binding.value.enabled === undefined) {
        return true; // default value
    }
    return binding.value.enabled;
}

function bgColorName(binding) {
    if (binding.value.bgColorName === undefined) {
        return "surface"; // default value
    }
    return binding.value.bgColorName;
}

export default {
    bind(el, binding) {
        if (enabled(binding)) {
            darkifyHtml(el, themeColorLvalue(bgColorName(binding)));
        }
    }
};
