import { BModal } from "bootstrap-vue";

export default {
    name: "BmModal",
    extends: BModal,
    props: {
        okVariant: {
            type: String,
            default: "fill-accent"
        },
        cancelVariant: {
            type: String,
            default: "text"
        },
        scrollable: {
            type: Boolean,
            default: true
        }
    }
};
