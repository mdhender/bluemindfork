<script>
import { BmButton } from "@bluemind/ui-components";
import { IS_SW_AVAILABLE, PKCS7_MIMES } from "../../lib/constants";
import { isDecrypted, isEncrypted, isSigned, isVerified } from "../../lib/helper";
import untrustedIllustration from "../../../assets/mail-app-untrusted.png";
import undecryptedIllustration from "../../../assets/mail-app-undecrypted.png";

export default {
    name: "SmimeBodyWrapper",
    components: { BmButton },
    props: {
        message: {
            type: Object,
            required: true
        },
        next: {
            type: Function,
            required: true
        }
    },
    data() {
        return {
            untrustedIllustration,
            undecryptedIllustration
        };
    },
    computed: {
        forceDisplay() {
            return this.$store.state.mail.smime.displayUntrusted.includes(this.message.key);
        },
        untrusted() {
            return isSigned(this.message.headers) && !isVerified(this.message.headers);
        },
        undecrypted() {
            return isEncrypted(this.message.headers) && !isDecrypted(this.message.headers);
        }
    },
    methods: {
        hasEncryptedPart(partsByCapabilities) {
            const isEncrypted = ({ mime }) => PKCS7_MIMES.includes(mime);
            return partsByCapabilities.some(({ parts }) => parts.some(isEncrypted));
        }
    },
    render(h) {
        const src = this.untrusted ? untrustedIllustration : undecryptedIllustration;
        const text = this.untrusted
            ? this.$t("common.whats_going_on")
            : this.$t("smime.mailapp.body_wrapper.cant_display");
        if (
            (!this.forceDisplay && this.untrusted) ||
            this.undecrypted ||
            (!IS_SW_AVAILABLE && this.hasEncryptedPart(this.message.inlinePartsByCapabilities))
        ) {
            const imgDiv = h("div", {}, [h("img", { attrs: { src } })]);
            //  FIXME doc urls: verification failure, decryption failure and missing SW
            const button = h(
                "bm-button",
                { props: { variant: "link" }, class: "mt-6", attrs: { target: "_blank" } },
                text
            );
            const children = [imgDiv, button];
            return h("div", { class: "smime-body-wrapper py-8" }, [children]);
        } else {
            return this.next();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";
.smime-body-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    background-color: $neutral-bg-lo1;
}
</style>
