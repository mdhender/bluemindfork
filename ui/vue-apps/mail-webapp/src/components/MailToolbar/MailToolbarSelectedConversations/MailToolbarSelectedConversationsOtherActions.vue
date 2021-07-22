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
            v-if="showMarkAsRead && !isTemplate"
            icon="read"
            :title="markAsReadAriaText()"
            :aria-label="markAsReadAriaText()"
            @click="markAsRead()"
        >
            {{ markAsReadText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            v-else-if="!isTemplate"
            icon="unread"
            :title="markAsUnreadAriaText()"
            :aria-label="markAsUnreadAriaText()"
            @click="markAsUnread()"
        >
            {{ markAsUnreadText }}
        </bm-dropdown-item>
        <bm-dropdown-item
            class="shadow-sm"
            :shortcut="$t('mail.shortcuts.purge')"
            :title="removeAriaText()"
            :aria-label="removeAriaText()"
            @click="remove()"
        >
            {{ $t("mail.actions.purge") }}
        </bm-dropdown-item>
    </bm-dropdown>
</template>

<script>
import { mapGetters } from "vuex";
import { BmDropdown, BmDropdownItem, BmIcon } from "@bluemind/styleguide";
import { ActionTextMixin, RemoveMixin, FlagMixin } from "~/mixins";
import { CURRENT_CONVERSATION_METADATA, MY_TEMPLATES } from "~/getters";

export default {
    name: "MailToolbarConsultMessageOtherActions",
    components: { BmDropdown, BmDropdownItem, BmIcon },
    mixins: [ActionTextMixin, RemoveMixin, FlagMixin],
    props: {
        subject: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { CURRENT_CONVERSATION_METADATA, MY_TEMPLATES }),

        isTemplate() {
            return this.selectionLength === 1 && this.selected[0]?.folderRef.key === this.MY_TEMPLATES.key;
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
