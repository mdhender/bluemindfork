<template>
    <div class="untrusted-sender-alert">
        {{ $tc("alert.smime.untrusted", untrustedMessageKeys.length) }}
        <bm-button v-if="!areAlreadyDisplayed" variant="link" class="ml-4" @click="displayMessages">
            {{ $t("common.display_anyway") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/ui-components";
import { hasSignatureHeader, isVerified } from "../../../lib/helper";
import { DISPLAY_UNTRUSTED } from "../../../store/mail/types";

export default {
    name: "UntrustedSenderAlert",
    components: { BmButton },
    props: {
        alert: {
            type: Object,
            required: true
        }
    },
    computed: {
        untrustedMessageKeys() {
            const conv = this.$store.getters["mail/CURRENT_CONVERSATION_METADATA"];
            const draftsFolderKey = this.$store.getters["mail/MY_DRAFTS"].key;
            const messageKeys = this.$store.getters["mail/CONVERSATION_MESSAGE_BY_KEY"](conv.key)
                .filter(
                    message =>
                        message.folderRef.key !== draftsFolderKey &&
                        hasSignatureHeader(message.headers) &&
                        !isVerified(message.headers)
                )
                .map(({ key }) => key);
            return messageKeys;
        },
        areAlreadyDisplayed() {
            return this.untrustedMessageKeys.every(key => this.$store.state.mail.smime.displayUntrusted.includes(key));
        }
    },
    methods: {
        displayMessages() {
            this.$store.commit("mail/" + DISPLAY_UNTRUSTED, this.untrustedMessageKeys);
        }
    }
};
</script>

<style lang="scss">
.untrusted-sender-alert {
    display: flex;
    align-items: center;
}
</style>
