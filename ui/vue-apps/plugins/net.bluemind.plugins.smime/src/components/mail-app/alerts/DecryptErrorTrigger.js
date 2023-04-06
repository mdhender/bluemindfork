import { mapActions } from "vuex";
import { hasEncryptionHeader, isDecrypted } from "../../../lib/helper";

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            alert: { name: "smime.decrypt_error", uid: "SMIME_DECRYPT_ERROR" },
            options: { area: "right-panel", icon: "lock", renderer: "DecryptErrorAlert", dismissible: false }
        };
    },
    watch: {
        "message.headers": {
            handler() {
                if (hasEncryptionHeader(this.message.headers) && !isDecrypted(this.message.headers)) {
                    this.ERROR({ alert: this.alert, options: this.options });
                } else {
                    this.REMOVE(this.alert);
                }
            },
            immediate: true
        }
    },
    destroyed() {
        this.REMOVE(this.alert);
    },
    methods: {
        ...mapActions("alert", ["ERROR", "REMOVE"])
    },
    render() {
        return "";
    }
};
