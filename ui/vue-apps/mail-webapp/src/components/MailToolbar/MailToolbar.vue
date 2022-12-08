<template>
    <bm-button-toolbar key-nav class="mail-toolbar flex-nowrap h-100">
        <bm-icon-button
            variant="compact-on-fill-primary"
            size="lg"
            class="d-lg-none mr-auto"
            icon="arrow-back"
            @click="back()"
        />
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

import { BmIconButton, BmButtonToolbar } from "@bluemind/ui-components";

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
        BmIconButton,
        BmButtonToolbar,
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
