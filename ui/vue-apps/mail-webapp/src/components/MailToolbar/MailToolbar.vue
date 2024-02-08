<template>
    <mail-toolbar-compose-message
        v-if="displayed && MESSAGE_IS_LOADED(ACTIVE_MESSAGE) && ACTIVE_MESSAGE.composing"
        :class="className"
        :message="ACTIVE_MESSAGE"
        :compact="compact"
    />
    <mail-toolbar-selected-conversations
        v-else-if="(displayed && currentConversationIsLoaded) || SEVERAL_CONVERSATIONS_SELECTED"
        :class="className"
        :compact="compact"
    />
</template>

<script>
import { mapGetters, mapState } from "vuex";

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
        MailToolbarComposeMessage,
        MailToolbarSelectedConversations
    },
    props: {
        compact: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            className: "mail-toolbar flex-nowrap h-100"
        };
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-toolbar {
    @include until-lg {
        justify-content: end;
    }
    & > .bm-icon-button,
    .mail-toolbar-item {
        margin-top: auto;
        margin-bottom: auto;
    }

    &.mail-toolbar-compose-message,
    &.mail-toolbar-selected-conversations {
        @include until-lg {
            gap: $sp-6;
        }
    }
}
</style>
