<template>
    <div>
        <!-- {{ $t("mail.content.alert.images.blocked") }} -->
        <!-- FIXME i18n -->
        Le message a été signé par son expéditeur mais la signature n'a pas pu être validée.
        <bm-button v-if="!isAlreadyDisplayed" variant="link" class="ml-3" @click="displayMessage">
            <!-- FIXME i18n -->
            Afficher quand même
        </bm-button>
    </div>
</template>

<script>
import { DISPLAY_UNTRUSTED } from "../../store/mutationTypes";
import { BmButton } from "@bluemind/styleguide";

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
        messageKey() {
            return this.alert.payload;
        },
        isAlreadyDisplayed() {
            return this.$store.state.smime.displayUntrusted.indexOf(this.messageKey) !== -1;
        }
    },
    methods: {
        displayMessage() {
            this.$store.commit("smime/" + DISPLAY_UNTRUSTED, this.messageKey);
        }
    }
};
</script>
