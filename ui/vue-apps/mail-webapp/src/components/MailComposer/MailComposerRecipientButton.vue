<template>
    <bm-button
        v-key-nav-group:recipient-button
        class="mail-composer-recipient-button mr-3"
        variant="text-accent"
        @click="$bvModal.show('recipient-picker')"
    >
        {{ recipientType }}
    </bm-button>
</template>

<script>
import { BmButton, KeyNavGroup } from "@bluemind/ui-components";

export default {
    name: "MailComposerRecipientButton",
    components: { BmButton },
    directives: { KeyNavGroup },
    props: {
        recipientType: { type: String, required: true }
    }
};
</script>

<style lang="scss">
.mail-composer-recipient-button {
    flex: none;
}
</style>
