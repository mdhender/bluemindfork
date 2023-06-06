<template>
    <bm-button-toolbar v-if="displayed" key-nav class="mail-toolbar flex-nowrap h-100">
        <mail-toolbar-compose-message
            v-if="MESSAGE_IS_LOADED(ACTIVE_MESSAGE) && ACTIVE_MESSAGE.composing"
            :message="ACTIVE_MESSAGE"
            :compact="compact"
        />
        <mail-toolbar-selected-conversations
            v-else-if="currentConversationIsLoaded || SEVERAL_CONVERSATIONS_SELECTED"
            :compact="compact"
        />
    </bm-button-toolbar>
</template>

<script>
import { mapGetters, mapState } from "vuex";

import { BmButtonToolbar } from "@bluemind/ui-components";

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

export default {
    name: "MailToolbar",
    components: {
        BmButtonToolbar,
        MailToolbarComposeMessage,
        MailToolbarSelectedConversations
    },
    props: {
        compact: {
            type: Boolean,
            default: false
        }
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
        },
        displayed() {
            return (
                (this.MESSAGE_IS_LOADED(this.ACTIVE_MESSAGE) && this.ACTIVE_MESSAGE.composing) ||
                this.currentConversationIsLoaded ||
                this.SEVERAL_CONVERSATIONS_SELECTED
            );
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.mail-toolbar {
    @include until-lg {
        justify-content: end;
    }
    & > .bm-icon-button,
    .mail-toolbar-item {
        margin-top: auto;
        margin-bottom: auto;
    }

    .mail-toolbar-compose-message,
    .mail-toolbar-selected-conversations {
        @include until-lg {
            gap: $sp-6;
        }
    }
}
</style>
