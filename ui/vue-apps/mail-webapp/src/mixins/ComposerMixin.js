import { mapActions, mapState } from "vuex";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { MAX_MESSAGE_SIZE_EXCEEDED, RESET_COMPOSER } from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import { ComposerFromMixin } from "~/mixins";

const maxMessageSizeExceededAlert = {
    alert: { name: "mail.DRAFT_EXCEEDS_MAX_MESSAGE_SIZE", uid: "DRAFT_EXCEEDS_MAX_MESSAGE_SIZE" },
    options: { area: "right-panel", renderer: "DraftExceedsMaxMessageSizeAlert" }
};

export default {
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    mixins: [ComposerFromMixin],
    data() {
        return { isSignatureInserted: false, draggedFilesCount: -1 };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        isSenderShown() {
            return this.$store.getters["mail/" + IS_SENDER_SHOWN](this.$store.state.settings);
        }
    },
    watch: {
        "messageCompose.maxMessageSizeExceeded": {
            handler(hasExceeded) {
                if (hasExceeded) {
                    this.ERROR(maxMessageSizeExceededAlert);
                } else {
                    this.REMOVE(maxMessageSizeExceededAlert.alert);
                }
            },
            immediate: true
        }
    },
    mounted() {
        this.$store.commit("mail/" + MAX_MESSAGE_SIZE_EXCEEDED, false);
        if (this.message.from) {
            this.setIdentity({ email: this.message.from.address, displayname: this.message.from.dn });
        }
    },
    destroyed() {
        this.$store.commit("mail/" + RESET_COMPOSER);
    },
    methods: {
        ...mapActions("alert", { ERROR, REMOVE }),
        async toggleSignature() {
            this.$refs.content.toggleSignature();
        },
        async checkAndRepairFrom() {
            const matchingIdentity = this.$store.state["root-app"].identities.find(
                i => i.email === this.message.from.address && i.displayname === this.message.from.dn
            );
            if (!matchingIdentity) {
                // eslint-disable-next-line no-console
                console.warn("identity changed because no identity matched message.from");
                const defaultIdentity = this.$store.getters["root-app/DEFAULT_IDENTITY"];
                await this.setFrom(defaultIdentity, this.message);
            }
        }
    }
};
