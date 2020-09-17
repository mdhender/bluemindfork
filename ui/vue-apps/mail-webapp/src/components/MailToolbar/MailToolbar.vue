<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap h-100">
        <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="back()">
            <bm-icon icon="arrow-back" size="2x" />
        </bm-button>
        <mail-toolbar-compose-message v-if="currentMessageKey && messages[currentMessageKey].composing" />
        <mail-toolbar-selected-messages v-else-if="currentMessage || selectedMessageKeys.length > 1" />
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmIcon, BmButtonToolbar } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import MailToolbarComposeMessage from "./MailToolbarComposeMessage";
import MailToolbarSelectedMessages from "./MailToolbarSelectedMessages";

export default {
    name: "MailToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailToolbarComposeMessage,
        MailToolbarSelectedMessages
    },
    computed: {
        ...mapState("mail", ["messages"]),
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" })
    },
    methods: {
        back() {
            this.$router.navigate("v:mail:home");
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-toolbar {
    @media (max-width: map-get($grid-breakpoints, "lg")) {
        justify-content: end;
    }
}

.mail-toolbar .btn {
    padding: 0 $sp-1;
    height: 100%;
    font-weight: $font-weight-normal;
}

.mail-toolbar .bm-dropdown,
.mail-toolbar .btn {
    @media (min-width: map-get($grid-breakpoints, "xl")) {
        min-width: 5.5rem;
    }
}

.mail-toolbar .btn > svg {
    font-size: 1.5em;
}
</style>
