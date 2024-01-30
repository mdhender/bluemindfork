<template>
    <span class="private-event-not-sent-to-delegates-alert d-flex flex-wrap">
        {{ $t("alert.mail.private_event_not_sent_to_delegates.warning") }}
        <bm-read-more
            class="ml-3"
            href="https://doc.bluemind.net/release/5.0/guide_de_l_utilisateur/parametrer_le_compte_utilisateur#2-donner-les-droits-de-gestion-des-invitations"
        />
    </span>
</template>

<script>
import { BmReadMore } from "@bluemind/ui-components";

export default {
    name: "PrivateEventNotSentToDelegatesAlert",
    components: { BmReadMore },
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
