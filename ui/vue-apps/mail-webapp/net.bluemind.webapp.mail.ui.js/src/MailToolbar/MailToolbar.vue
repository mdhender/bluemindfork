<template>
    <bm-button-toolbar
        v-if="currentMessageKey || isMessageComposerDisplayed"
        key-nav
        class="mail-toolbar flex-nowrap h-100"
    >
        <bm-button variant="link" class="d-md-none btn-sm mr-auto" @click="back()">
            <bm-icon icon="arrow-back" size="2x" />
        </bm-button>
        <mail-toolbar-compose-message v-if="isMessageComposerDisplayed" />
        <mail-toolbar-consult-message v-else />
    </bm-button-toolbar>
</template>

<script>
import { BmButton, BmIcon, BmButtonToolbar } from "@bluemind/styleguide";
import { mapState } from "vuex";
import MailToolbarComposeMessage from "./MailToolbarComposeMessage";
import MailToolbarConsultMessage from "./MailToolbarConsultMessage";

export default {
    name: "MailToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailToolbarComposeMessage,
        MailToolbarConsultMessage
    },
    computed: {
        ...mapState("mail-webapp", ["currentMessageKey", "currentFolderKey"]),
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
            this.$router.push("/mail/" + this.currentFolderKey + "/");
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
