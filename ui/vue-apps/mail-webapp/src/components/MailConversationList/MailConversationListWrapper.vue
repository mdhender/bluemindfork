<template>
    <conversation-list
        class="scroller-y scroller-visible-on-hover"
        :folder="activeFolder"
        :all-conversation-keys="CONVERSATION_LIST_ALL_KEYS"
        :conversation-keys="CONVERSATION_LIST_KEYS"
        :conversations-activated="CONVERSATIONS_ACTIVATED"
        :draggable="true"
        :selected="SELECTION_IS_EMPTY ? currentConversationKey : SELECTION_KEYS"
        @keyup.native.delete.exact.prevent="moveToTrash()"
        @keyup.native.shift.delete.exact.prevent="remove()"
        @next-page="CONVERSATION_LIST_NEXT_PAGE()"
        @set-selection="setSelection"
        @add-to-selection="addToSelection"
        @remove-from-selection="removeFromSelection"
    >
        <template v-slot:actions="{ conversation }">
            <conversation-list-item-quick-action-buttons :conversation="conversation" />
        </template>
    </conversation-list>
</template>
<script>
import { RemoveMixin } from "~/mixins";
import {
    CONVERSATIONS_ACTIVATED,
    CONVERSATION_LIST_KEYS,
    CONVERSATION_LIST_ALL_KEYS,
    SELECTION_IS_EMPTY,
    SELECTION_KEYS
} from "~/getters";
import { RESET_CONVERSATION_LIST_PAGE, SET_SELECTION, UNSELECT_CONVERSATION, SELECT_CONVERSATION } from "~/mutations";
import { CONVERSATION_LIST_NEXT_PAGE } from "~/actions";
import { mapActions, mapGetters, mapMutations, mapState } from "vuex";
import ConversationList from "~/components/ConversationList/ConversationList";
import ConversationListItemQuickActionButtons from "./ConversationListItemQuickActionButtons";

export default {
    name: "MailConversationListWrapper",
    components: { ConversationList, ConversationListItemQuickActionButtons },
    mixins: [RemoveMixin],
    computed: {
        ...mapGetters("mail", {
            CONVERSATIONS_ACTIVATED,
            CONVERSATION_LIST_ALL_KEYS,
            CONVERSATION_LIST_KEYS,
            SELECTION_KEYS,
            SELECTION_IS_EMPTY
        }),
        ...mapState("mail", { conversationByKey: ({ conversations }) => conversations.conversationByKey }),
        activeFolder() {
            return this.$store.state.mail.folders[this.$store.state.mail.activeFolder];
        },
        currentConversationKey() {
            return this.$store.state.mail.conversations.currentConversation;
        }
    },
    created() {
        this.RESET_CONVERSATION_LIST_PAGE();
        this.CONVERSATION_LIST_NEXT_PAGE();
    },
    methods: {
        ...mapActions("mail", { CONVERSATION_LIST_NEXT_PAGE }),
        ...mapMutations("mail", {
            RESET_CONVERSATION_LIST_PAGE,
            SET_SELECTION,
            SELECT_CONVERSATION,
            UNSELECT_CONVERSATION
        }),
        select(selection) {
            if (!Array.isArray(selection)) {
                const conversation = this.conversationByKey[selection];
                this.$router.navigate({ name: "v:mail:conversation", params: { conversation } });
            } else {
                this.SET_SELECTION(selection);
            }
        },
        setSelection(selection, mode) {
            if (mode === "MULTI") {
                this.SET_SELECTION(selection);
                this.$router.navigate({ name: "v:mail:home" });
            } else {
                const conversation = this.conversationByKey[selection];
                this.$router.navigate({ name: "v:mail:conversation", params: { conversation } });
            }
        },
        addToSelection(selection) {
            selection.forEach(key => this.SELECT_CONVERSATION(key));
            this.$router.navigate({ name: "v:mail:home" });
        },
        removeFromSelection(selection) {
            this.UNSELECT_CONVERSATION(selection[0]);
            this.$router.navigate({ name: "v:mail:home" });
        }
    }
};
</script>
