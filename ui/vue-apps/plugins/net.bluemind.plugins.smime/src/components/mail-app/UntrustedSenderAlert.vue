<template>
    <div>
        <!-- {{ $t("mail.content.alert.images.blocked") }} -->
        <!-- FIXME i18n -->
        Le message a été signé par son expéditeur mais la signature n'a pas pu être validée.
        <bm-button variant="link" @click="displayMessage">Afficher quand même</bm-button>
    </div>
</template>

<script>
import { BmButton } from "@bluemind/styleguide";

export default {
    name: "UntrustedSenderAlert",
    components: { BmButton },
    methods: {
        displayMessage() {
            // TODO
        }
    }
};
</script>
