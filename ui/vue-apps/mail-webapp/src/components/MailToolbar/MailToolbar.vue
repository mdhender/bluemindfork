<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap h-100">
        <bm-button variant="inline-on-fill-primary" class="d-lg-none mr-auto" @click="back()">
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
    CURRENT_CONVERSATION_METADATA,
    MESSAGE_IS_LOADED,
    SELECTION_IS_EMPTY,
    SEVERAL_CONVERSATIONS_SELECTED
} from "~/getters";
import { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";

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
        ...mapGetters("mail", {
            ACTIVE_MESSAGE,
            CONVERSATION_IS_LOADED,
            CURRENT_CONVERSATION_METADATA,
            MESSAGE_IS_LOADED,
            SELECTION_IS_EMPTY,
            SEVERAL_CONVERSATIONS_SELECTED
        }),
        currentConversationIsLoaded() {
            if (this.CURRENT_CONVERSATION_METADATA) {
                return this.CONVERSATION_IS_LOADED(this.CURRENT_CONVERSATION_METADATA);
            }
            return false;
        }
    },
    methods: {
        ...mapMutations("mail", { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION }),
        back() {
            if (this.SELECTION_IS_EMPTY) {
                this.UNSET_CURRENT_CONVERSATION();
            } else {
                this.UNSELECT_ALL_CONVERSATIONS();
            }
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

.mail-toolbar .btn svg.fa-2x {
    font-size: 1.5em;
}
</style>
