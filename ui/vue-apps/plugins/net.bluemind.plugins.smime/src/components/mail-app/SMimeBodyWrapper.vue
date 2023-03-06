<script>
import { MimeType } from "@bluemind/email";
import { BmButton } from "@bluemind/ui-components";
import { isDecrypted, getHeaderValue, hasEncryptionHeader, hasSignatureHeader, isVerified } from "../../lib/helper";
import { ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME } from "../../lib/constants";
import DocLinkMixin from "../../mixins/DocLinkMixin";
import untrustedIllustration from "../../../assets/mail-app-untrusted.png";
import undecryptedIllustration from "../../../assets/mail-app-undecrypted.png";

export default {
    name: "SMimeBodyWrapper",
    components: { BmButton },
    mixins: [DocLinkMixin],
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
        isPreview() {
            return this.$store.state.mail.preview.messageKey !== null;
        },
        forceDisplay() {
            return this.$store.state.mail.smime.displayUntrusted.includes(this.message.key);
        },
        untrusted() {
            return hasSignatureHeader(this.message.headers) && !isVerified(this.message.headers);
        },
        isEncrypted() {
            return (
                hasEncryptionHeader(this.message.headers) ||
                this.hasEncryptedPart(this.message.inlinePartsByCapabilities)
            );
        },
        undecrypted() {
            return this.isEncrypted && !isDecrypted(this.message.headers);
        }
    },
    methods: {
        hasEncryptedPart(partsByCapabilities) {
            return partsByCapabilities.some(({ parts }) => parts.some(MimeType.isPkcs7));
        }
    },
    render(h) {
        const src = this.untrusted ? untrustedIllustration : undecryptedIllustration;
        const text = this.untrusted
            ? this.$t("common.whats_going_on")
            : this.$t("smime.mailapp.body_wrapper.cant_display");
        if ((!this.forceDisplay && this.untrusted) || this.undecrypted) {
            const imgDiv = h("div", {}, [h("img", { attrs: { src } })]);
            const headerName = this.untrusted ? SIGNED_HEADER_NAME : ENCRYPTED_HEADER_NAME;
            const href = this.isPreview
                ? this.noSmimeOnPreviewLink
                : this.linkFromCodeOrHeader(getHeaderValue(this.message.headers, headerName), headerName);
            const button = h(
                "bm-button",
                { props: { variant: "link" }, class: "mt-6", attrs: { href, target: "_blank" } },
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
