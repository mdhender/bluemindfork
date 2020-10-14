<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap h-100">
        <bm-button variant="inline-light" class="d-lg-none btn-sm mr-auto" @click="back()">
            <bm-icon icon="arrow-back" size="2x" />
        </bm-button>
        <mail-toolbar-compose-message
            v-if="MESSAGE_IS_LOADED(ACTIVE_MESSAGE) && ACTIVE_MESSAGE.composing"
            :message="ACTIVE_MESSAGE"
        />
        <mail-toolbar-selected-conversations
            v-else-if="currentConversationIsLoaded || SEVERAL_CONVERSATIONS_SELECTED"
        />
    </bm-button-toolbar>
</template>

<script>
import { mapGetters, mapMutations, mapState } from "vuex";

import { BmButton, BmIcon, BmButtonToolbar } from "@bluemind/styleguide";

import MailToolbarComposeMessage from "./MailToolbarComposeMessage";
import MailToolbarSelectedConversations from "./MailToolbarSelectedConversations";
import {
    ACTIVE_MESSAGE,
    CONVERSATION_IS_LOADED,
    CONVERSATION_METADATA,
    MESSAGE_IS_LOADED,
    SEVERAL_CONVERSATIONS_SELECTED
} from "~/getters";
import { UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "MailToolbar",
    components: {
        BmButton,
        BmButtonToolbar,
        BmIcon,
        MailToolbarComposeMessage,
        MailToolbarSelectedConversations
    },
    computed: {
        ...mapState("mail", { messages: ({ conversations }) => conversations.messages }),
        ...mapState("mail", { currentConversation: ({ conversations }) => conversations.currentConversation }),
        ...mapGetters("mail", {
            ACTIVE_MESSAGE,
            CONVERSATION_IS_LOADED,
            CONVERSATION_METADATA,
            MESSAGE_IS_LOADED,
            SEVERAL_CONVERSATIONS_SELECTED
        }),
        currentConversationIsLoaded() {
            return this.CONVERSATION_IS_LOADED(this.CONVERSATION_METADATA(this.currentConversation?.key));
        }
    },
    methods: {
        ...mapMutations("mail", { UNSET_CURRENT_CONVERSATION }),
        back() {
            this.UNSET_CURRENT_CONVERSATION();
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
