import { BDropdown } from "bootstrap-vue";

export const BvDropdown = {
    name: "BvDropdown",
    extends: BDropdown,
    methods: {
        rootCloseListener() {
            // do nothing, overrides default behavior of autoclosing when another dropdown is opened.
        }
    }
};
