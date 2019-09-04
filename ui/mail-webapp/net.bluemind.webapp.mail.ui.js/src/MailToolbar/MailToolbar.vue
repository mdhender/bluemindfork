<template>
    <bm-button-toolbar 
        v-if="message || isMessageComposerDisplayed"
        key-nav
        class="mail-toolbar flex-nowrap h-100"
    >
        <mail-toolbar-compose-message v-if="isMessageComposerDisplayed" />
        <mail-toolbar-consult-message v-else />
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar }  from "@bluemind/styleguide";
import { mapGetters } from "vuex";
import MailToolbarComposeMessage from "./MailToolbarComposeMessage";
import MailToolbarConsultMessage from "./MailToolbarConsultMessage";

export default {
    name: "MailToolbar",
    components: {
        BmButtonToolbar,
        MailToolbarComposeMessage,
        MailToolbarConsultMessage
    },
    computed: {
        ...mapGetters("backend.mail/items", { message: "currentMessage" }),
        isMessageComposerDisplayed() {
            const routePath = this.$route.path;
            return routePath.endsWith("new") ||
                routePath.endsWith("reply") ||
                routePath.endsWith("replyAll") ||
                routePath.endsWith("forward");
        }
    }
};
</script>

<style lang="scss">
//TODO might move inside bluemind-styleguide
@import "~@bluemind/styleguide/css/variables";

.mail-toolbar .btn {
    flex-basis: 11em;
    flex-grow: 1;
    flex-shrink: 0;
    padding-top: 0;
    padding-bottom: 0;
    height: 100%;
    min-width: 5.5rem;
}

.mail-toolbar .btn > svg {
    font-size: 1.5em;
}

.mail-toolbar .btn:focus,
.mail-toolbar .btn.focus {
    box-shadow: none !important;
}

.mail-toolbar .btn:hover {
    background-color: $extra-light !important;
    border-color: $extra-light !important;
}
</style>