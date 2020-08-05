<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap h-100">
        <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="back()">
            <bm-icon icon="arrow-back" size="2x" />
        </bm-button>
        <mail-toolbar-compose-message v-if="isMessageComposerDisplayed" />
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
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        ...mapGetters("mail-webapp/currentMessage", { currentMessage: "message" }),
        isMessageComposerDisplayed() {
            const routePath = this.$route.path;
            return (
                routePath.endsWith("new") ||
                routePath.endsWith("reply") ||
                routePath.endsWith("replyAll") ||
                routePath.endsWith("forward")
            );
        }
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
    padding-top: 0;
    padding-bottom: 0;
    height: 100%;
    min-width: 5.5rem;
    font-weight: $font-weight-normal;
    @media (max-width: map-get($grid-breakpoints, "lg")) {
        min-width: auto;
    }
}

.mail-toolbar .btn > svg {
    font-size: 1.5em;
}
</style>
