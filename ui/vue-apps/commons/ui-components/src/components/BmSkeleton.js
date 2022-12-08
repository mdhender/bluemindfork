import { BSkeleton } from "bootstrap-vue";

export default {
    name: "BmSkeleton",
    extends: BSkeleton,
    props: {
        animation: {
            type: String,
            default: "null"
        }
    }
};
