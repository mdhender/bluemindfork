import cloneDeep from "lodash.clonedeep";
import { mapActions, mapState } from "vuex";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { messageUtils } from "@bluemind/mail";
import { DEBOUNCED_SAVE_MESSAGE, TOGGLE_DSN_REQUEST } from "~/actions";
import { MAX_MESSAGE_SIZE_EXCEEDED, RESET_COMPOSER, SET_MESSAGE_HEADERS } from "~/mutations";
import { IS_SENDER_SHOWN } from "~/getters";
import { ComposerFromMixin } from "~/mixins";
import { Flag } from "@bluemind/email";

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
        return { draggedFilesCount: -1, isSignatureInserted: false };
    },
    computed: {
        ...mapState("mail", ["messageCompose"]),
        isSenderShown() {
            return this.$store.getters["mail/" + IS_SENDER_SHOWN](this.$store.state.settings);
        },
        isDeliveryStatusRequested() {
            return this.message.flags.includes(Flag.BM_DSN);
        },
        isDispositionNotificationRequested() {
            return messageUtils.findDispositionNotificationHeaderIndex(this.message.headers) >= 0;
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
        },
        "message.from": {
            handler: function (value) {
                if (this.isDispositionNotificationRequested) {
                    const headers = [...this.message.headers];
                    messageUtils.setDispositionNotificationHeader(headers, value);
                    this.$store.commit("mail/" + SET_MESSAGE_HEADERS, { messageKey: this.message.key, headers });
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
        toggleSignature() {
            this.$refs.content.toggleSignature();
        },
        toggleDeliveryStatus() {
            this.$store.dispatch(`mail/${TOGGLE_DSN_REQUEST}`, this.message);
            this.$store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
                draft: this.message,
                messageCompose: cloneDeep(this.$store.state.mail.messageCompose),
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
        },
        toggleDispositionNotification() {
            const headers = [...this.message.headers];
            if (this.isDispositionNotificationRequested) {
                messageUtils.removeDispositionNotificationHeader(headers);
            } else {
                messageUtils.setDispositionNotificationHeader(headers, this.message.from);
            }
            this.$store.commit(`mail/${SET_MESSAGE_HEADERS}`, { messageKey: this.message.key, headers });
            this.$store.dispatch(`mail/${DEBOUNCED_SAVE_MESSAGE}`, {
                draft: this.message,
                messageCompose: cloneDeep(this.$store.state.mail.messageCompose),
                files: this.message.attachments.map(({ fileKey }) => this.$store.state.mail.files[fileKey])
            });
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
