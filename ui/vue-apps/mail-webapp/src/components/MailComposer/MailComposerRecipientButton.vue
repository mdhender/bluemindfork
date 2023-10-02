<template>
    <bm-button
        v-key-nav-group:recipient-button
        class="mail-composer-recipient-button px-4"
        variant="text-accent"
        @click="
            $bvModal.show('recipient-picker');
            $emit('open-picker');
        "
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
