<template>
    <bm-icon-dropdown
        v-if="SMIME_AVAILABLE"
        split
        :icon="!hasEncryptionHeader ? 'lock-open' : hasEncryptError ? 'lock-slash' : 'lock'"
        class="encrypt-and-sign-button"
        :class="{ selected: hasEncryptionHeader, 'has-error': hasEncryptError }"
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
import { draftUtils, messageUtils } from "@bluemind/mail";
import { SMIME_AVAILABLE } from "../../store/getterTypes";
import EncryptSignMixin from "../../mixins/EncryptSignMixin";
import { hasEncryptionHeader, hasSignatureHeader, addHeaderValue } from "../../lib/helper";
import { CRYPTO_HEADERS, ENCRYPTED_HEADER_NAME, SIGNED_HEADER_NAME, SMIMEPrefKeys } from "../../lib/constants";

const { MessageCreationModes, messageKey, MessageStatus } = messageUtils;
const { isNewMessage } = draftUtils;

const encryptAlert = { name: "smime.encrypt_error", uid: "SMIME_ENCRYPT_ERROR", payload: null };
const encryptAlertOptions = {
    area: "composer-footer",
    icon: "lock-slash",
    renderer: "EncryptErrorAlert",
    dismissible: false
};
const signAlert = { name: "smime.sign_error", uid: "SMIME_SIGN_ERROR", payload: null };
const signAlertOptions = {
    area: "composer-footer",
    icon: "lock-slash",
    renderer: "SignErrorAlert",
    dismissible: false
};

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
        ...mapGetters("mail", { SMIME_AVAILABLE }),
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
        "$store.state.mail.smime.missingCertificates": {
            handler(missingCertificates) {
                if (this.hasEncryptionHeader && missingCertificates.length > 0) {
                    this.$store.commit("mail/SET_MESSAGES_STATUS", [
                        { key: this.message.key, status: MessageStatus.SAVE_ERROR }
                    ]);
                }
            }
        },
        hasEncryptError: {
            handler(hasError) {
                const error = this.$store.state.mail.smime.encryptError;
                const alert = { ...encryptAlert, payload: error };
                if (hasError) {
                    this.ERROR({ alert, options: encryptAlertOptions });
                } else {
                    this.REMOVE(encryptAlert);
                }
            },
            immediate: true
        },
        hasSignError: {
            handler(hasError) {
                const error = this.$store.state.mail.smime.signError;
                const alert = { ...signAlert, payload: error };
                if (hasError) {
                    this.ERROR({ alert, options: signAlertOptions });
                } else {
                    this.REMOVE(signAlert);
                }
            },
            immediate: true
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
