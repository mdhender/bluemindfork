import { mapActions } from "vuex";
import { ENCRYPTED_HEADER_NAME } from "../../../lib/constants";
import { getHeaderValue, hasEncryptionHeader, isDecrypted } from "../../../lib/helper";

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        alert() {
            return { name: "smime.decrypt_error", uid: "SMIME_DECRYPT_ERROR_" + this.message.key, payload: null };
        },
        options() {
            return { area: "right-panel", icon: "lock-fill", renderer: "DecryptErrorAlert", dismissible: false };
        }
    },
    watch: {
        "message.headers": {
            handler() {
                if (hasEncryptionHeader(this.message.headers) && !isDecrypted(this.message.headers)) {
                    this.alert.payload = getHeaderValue(this.message.headers, ENCRYPTED_HEADER_NAME);
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
