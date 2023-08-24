<script>
import { mapMutations } from "vuex";
import { BmIcon } from "@bluemind/ui-components";
import { GET_FH_FILE } from "../store/types/mutations";

export default {
    name: "PreviewInvalid",
    components: { BmIcon },
    props: {
        next: {
            type: Function,
            required: true
        },
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        isFhExpiredFile() {
            const file = this.GET_FH_FILE(this.file);
            return file && file.expirationDate < Date.now();
        }
    },
    methods: {
        ...mapMutations("mail", { GET_FH_FILE })
    },
    render(h) {
        if (this.isFhExpiredFile) {
            const icon = h("div", [h("bm-icon", { props: { icon: "cloud-exclamation", size: "4xl" } })]);
            const text = h("span", { class: "text" }, this.$t("mail.preview.nopreview.invalid"));
            return h("div", { class: "no-preview" }, [icon, text]);
        }
        return this.next();
    }
};
</script>
