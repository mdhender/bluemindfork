<template>
    <bm-icon-dropdown
        v-if="SMIME_AVAILABLE"
        split
        :icon="!hasEncryptionHeader ? 'lock-open' : hasEncryptError || hasInvalidIdentity ? 'lock-slash' : 'lock'"
        class="encrypt-and-sign-button"
        :class="{ selected: hasEncryptionHeader, 'has-error': hasEncryptError || hasInvalidIdentity }"
        variant="compact"
        size="lg"
        :title="title"
        right
        @click="toggleAll"
    >
        <bm-dropdown-item-toggle :checked="hasEncryptionHeader" @click="toggleEncryption">
            {{ $t("smime.mailapp.composer.encrypt") }}
        </bm-dropdown-item-toggle>

        <bm-dropdown-item-toggle :checked="hasSignatureHeader" @click="toggleSignature">
            {{ $t("smime.mailapp.composer.sign") }}
        </bm-dropdown-item-toggle>
    </bm-icon-dropdown>
</template>

<script>
import { mapActions, mapGetters } from "vuex";
import { BmDropdownItemToggle, BmIconDropdown } from "@bluemind/ui-components";
import { ERROR, REMOVE } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { draftUtils, mailTipUtils, messageUtils } from "@bluemind/mail";
import { SMIME_AVAILABLE } from "../../store/root-app/types";
import { RESET_MISSING_CERTIFICATES } from "../../store/mail/types";
import EncryptSignMixin from "../../mixins/EncryptSignMixin";
import { hasEncryptionHeader, hasSignatureHeader, addHeaderValue } from "../../lib/helper";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME, SMIMEPrefKeys } from "../../lib/constants";

const { MessageCreationModes, messageKey, MessageStatus } = messageUtils;
const { isNewMessage } = draftUtils;
const { getMailTipContext } = mailTipUtils;

const alert = { name: "smime", uid: "SMIME", payload: null };
const alertOptions = {
    area: "composer-footer",
    icon: "lock-slash",
    dismissible: false
};
const encryptAlertOptions = {
    ...alertOptions,
    renderer: "EncryptErrorAlert"
};
const signAlertOptions = {
    ...alertOptions,
    renderer: "SignErrorAlert"
};

const identityAlertOptions = {
    ...alertOptions,
    renderer: "InvalidIdentityAlert"
};

const alerts = {};

export default {
    name: "EncryptAndSignButton",
    components: { BmDropdownItemToggle, BmIconDropdown },
    mixins: [EncryptSignMixin],
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("smime", { SMIME_AVAILABLE }),
        title() {
            return this.hasEncryptionHeader
                ? this.$t("smime.mailapp.composer.stop_encrypt_and_sign")
                : this.$t("smime.mailapp.composer.encrypt_and_sign");
        },
        hasEncryptionHeader() {
            return hasEncryptionHeader(this.message.headers);
        },
        hasSignatureHeader() {
            return hasSignatureHeader(this.message.headers);
        },
        hasEncryptError() {
            return (
                this.hasEncryptionHeader &&
                (!this.SMIME_AVAILABLE ||
                    this.$store.state.mail.smime.missingCertificates.length > 0 ||
                    this.$store.state.mail.smime.encryptError)
            );
        },
        hasSignError() {
            return this.hasSignatureHeader && !!this.$store.state.mail.smime.signError;
        },
        hasInvalidIdentity() {
            return (
                (this.hasEncryptionHeader || this.hasSignatureHeader) &&
                this.message.from.address !== inject("UserSession").defaultEmail
            );
        },
        haveMissingCertificates() {
            return (
                (this.hasEncryptionHeader || this.hasSignatureHeader) &&
                this.$store.state.mail.smime.missingCertificates.length > 0
            );
        },
        isInvalid() {
            return this.hasInvalidIdentity || this.haveMissingCertificates;
        }
    },
    watch: {
        "$route.query.action": {
            handler(action) {
                if (this.SMIME_AVAILABLE) {
                    if (this.$store.state.settings[SMIMEPrefKeys.SIGNATURE] === "true") {
                        const headers = addHeaderValue(this.message.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.TO_DO);
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    }
                    if (this.$store.state.settings[SMIMEPrefKeys.ENCRYPTION] === "true") {
                        const headers = addHeaderValue(
                            this.message.headers,
                            ENCRYPTED_HEADER_NAME,
                            CRYPTO_HEADERS.TO_DO
                        );
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    } else if (!isNewMessage(this.message) && this.hasEncryptionHeader) {
                        const headers = addHeaderValue(
                            this.message.headers,
                            ENCRYPTED_HEADER_NAME,
                            CRYPTO_HEADERS.TO_DO
                        );
                        this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                    } else if (action && action !== MessageCreationModes.FORWARD_AS_EML) {
                        const key = messageKey(...this.$route.query.message.split(":").reverse());
                        const previous = this.$store.state.mail.conversations.messages[key];

                        if (previous && hasEncryptionHeader(previous.headers)) {
                            const headers = addHeaderValue(
                                this.message.headers,
                                ENCRYPTED_HEADER_NAME,
                                CRYPTO_HEADERS.TO_DO
                            );
                            this.$store.commit("mail/SET_MESSAGE_HEADERS", { messageKey: this.message.key, headers });
                        }
                    }
                }
            },
            immediate: true
        },
        hasEncryptError: {
            handler(hasError) {
                const priority = 2;
                const newAlert = { ...alert, payload: this.message };
                this.handleError(hasError, newAlert, encryptAlertOptions, priority);
            },
            immediate: true
        },
        hasSignError: {
            handler(hasError) {
                const priority = 1;
                const newAlert = { ...alert, payload: this.message };
                this.handleError(hasError, newAlert, signAlertOptions, priority);
            },
            immediate: true
        },
        hasInvalidIdentity: {
            handler(isInvalid) {
                const priority = 3;
                const newAlert = { ...alert, payload: this.message };
                this.handleError(isInvalid, newAlert, identityAlertOptions, priority);
            },
            immediate: true
        },
        isInvalid: {
            handler(isInvalid) {
                const status = isInvalid ? MessageStatus.INVALID : MessageStatus.IDLE;
                this.$store.commit("mail/SET_MESSAGES_STATUS", [{ key: this.message.key, status }]);
            },
            immediate: true
        },
        async hasEncryptionHeader() {
            if (this.hasEncryptionHeader) {
                await this.$execute("get-mail-tips", {
                    context: getMailTipContext(this.message),
                    message: this.message
                });
            } else {
                this.$store.commit("mail/" + RESET_MISSING_CERTIFICATES);
            }
        }
    },
    methods: {
        ...mapActions("alert", { ERROR, REMOVE }),
        async toggleAll() {
            if (this.hasEncryptionHeader) {
                this.stopSignature(this.message);
                this.stopEncryption(this.message);
            } else {
                this.startSignature(this.message);
                this.startEncryption(this.message);
            }
        },
        toggleEncryption() {
            if (this.hasEncryptionHeader) {
                this.stopEncryption(this.message);
            } else {
                this.startEncryption(this.message);
            }
        },
        toggleSignature() {
            if (this.hasSignatureHeader) {
                this.stopSignature(this.message);
            } else {
                this.startSignature(this.message);
            }
        },
        handleError(condition, alert, options, priority) {
            if (condition) {
                alerts[priority] = { alert, options };
            } else {
                delete alerts[priority];
            }
            this.displayAlertByPriority();
        },
        displayAlertByPriority() {
            const priorities = Object.keys(alerts);
            if (priorities.length === 0) {
                this.REMOVE(alert);
            } else {
                const max = Math.max(...priorities);
                this.ERROR(alerts[max]);
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";

.encrypt-and-sign-button {
    &.selected:not(.has-error) .btn {
        color: $primary-fg-hi1;
    }
    &.has-error.selected .btn {
        color: $danger-fg-hi1;
    }
}
</style>
