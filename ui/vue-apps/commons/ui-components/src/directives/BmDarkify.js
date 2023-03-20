import { darkifyHtml } from "../js/theming/darkify";
import darkifyingBaseLvalue from "../js/theming/darkifyingBaseLvalue";

function enabled(binding) {
    if (binding.value === undefined) {
        return true; // default value
    }
    return binding.value;
}

export default {
    bind(el, binding) {
        if (enabled(binding)) {
            darkifyHtml(el, darkifyingBaseLvalue());
        }
    }
};
