<template>
    <bm-icon-dropdown
        variant="regular-accent"
        size="sm"
        icon="3dots-vertical"
        no-caret
        class="mail-viewer-draft-toolbar-for-mobile d-flex justify-content-end"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        v-on="$listeners"
    >
        <bm-dropdown-item-button @click="openEditor">
            <bm-icon icon="pencil" />
            <span class="pl-1">{{ $t("mail.actions.edit") }}</span>
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button @click.exact.prevent.stop="REMOVE_DRAFT(message, conversation)">
            <bm-icon icon="trash" />
            <span class="pl-1">{{ $t("mail.actions.remove") }}</span>
        </bm-dropdown-item-button>
    </bm-icon-dropdown>
</template>

<script>
import { BmIconDropdown, BmDropdownDivider, BmDropdownItemButton, BmIcon } from "@bluemind/ui-components";
import { RemoveMixin, DraftMixin } from "~/mixins";
import { SET_MESSAGE_COMPOSING } from "~/mutations";
import { useComposerInit } from "~/composables/composer/ComposerInit";

export default {
    name: "MailViewerDraftToolbarForMobile",
    components: {
        BmIconDropdown,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmIcon
    },
    mixins: [RemoveMixin, DraftMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    },
    setup() {
        return useComposerInit();
    },
    methods: {
        async openEditor() {
            await this.saveAndCloseOpenDrafts(this.conversation);
            this.$store.commit(`mail/${SET_MESSAGE_COMPOSING}`, { messageKey: this.message.key, composing: true });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-viewer-draft-toolbar-for-mobile {
    .dropdown-divider {
        border-top: 1px solid $neutral-fg-lo2 !important;
        margin: 0.05rem 0;
    }
    .dropdown-menu {
        box-shadow: none;
        position: fixed !important;
        top: auto !important;
        bottom: 0px !important;
        transform: none !important;
        right: 0px !important;
        line-height: 2;
    }
}
</style>
