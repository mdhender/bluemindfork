<script setup>
import { DefaultAlert } from "@bluemind/alert.store";
import { BmReadMore } from "@bluemind/ui-components";
const props = defineProps({
    alert: {
        type: Object,
        required: true
    }
});
</script>

<template>
    <span class="notify-organizer-alert">
        <i18n path="alert.mail.notify_organizer.info">
            <template #organizer>
                <span class="bold">{{ alert.payload }}</span>
            </template>
        </i18n>
        <!-- FIXME doc -->
        <bm-read-more
            href="https://doc.bluemind.net/release/5.1/guide_de_l_utilisateur/la_messagerie/envoyer_des_messages/transferer_une_invitation#fwd-invitation"
        />
    </span>
</template>
