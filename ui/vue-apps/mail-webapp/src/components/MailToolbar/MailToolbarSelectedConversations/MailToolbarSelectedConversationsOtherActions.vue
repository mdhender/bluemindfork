<template>
    <bm-dropdown
        :no-caret="true"
        variant="inline-light"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        toggle-class="btn-lg-simple-dark"
        class="mail-toolbar-consult-message-other-actions h-100"
        right
    >
        <template slot="button-content">
            <bm-icon icon="3dots" size="2x" />
            <span class="d-none d-lg-block">{{ $tc("mail.toolbar.more") }}</span>
        </template>
        <bm-dropdown-item
            class="shadow-sm"
            :shortcut="$t('mail.shortcuts.purge')"
            :title="removeAriaText"
            :aria-label="removeAriaText"
            @click="remove()"
        >
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import { BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { mapGetters, mapState } from "vuex";
import { ActionTextMixin, RemoveMixin } from "~/mixins";
import { CONVERSATION_METADATA, SELECTION } from "~/getters";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: {
        BmDropdown,
        BmDropdownItem,
        BmIcon
    },
    mixins: [ActionTextMixin, RemoveMixin],
    computed: {
        ...mapGetters("mail", { CONVERSATION_METADATA, SELECTION }),
        ...mapState("mail", {
            conversations: ({ conversations }) => conversations.conversationByKey,
            currentConversation: ({ conversations }) => conversations.currentConversation
        }),
        selected() {
            return this.SELECTION.length
                ? this.SELECTION.map(s => this.CONVERSATION_METADATA(s.key))
                : [this.CONVERSATION_METADATA(this.currentConversation.key)];
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.mail-toolbar-consult-message-other-actions .dropdown-menu {
    border: none !important;
    margin-top: $sp-1 !important;
    padding: 0 !important;
}
</style>
