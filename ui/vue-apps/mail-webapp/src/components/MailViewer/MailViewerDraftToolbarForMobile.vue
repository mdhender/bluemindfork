<template>
    <bm-dropdown
        :no-caret="true"
        variant="simple-neutral"
        class="mail-viewer-draft-toolbar-for-mobile d-flex justify-content-end"
        :aria-label="$tc('mail.toolbar.more.aria')"
        :title="$tc('mail.toolbar.more.aria')"
        v-on="$listeners"
    >
        <template slot="button-content">
            <bm-icon class="text-secondary" icon="3dots" size="lg" />
        </template>
        <bm-dropdown-item-button @click="$emit('edit')">
            <bm-icon icon="pencil" />
            <span class="pl-1">{{ $t("mail.actions.edit") }}</span>
        </bm-dropdown-item-button>
        <bm-dropdown-divider />
        <bm-dropdown-item-button @click.exact.prevent.stop="REMOVE_DRAFT(conversation, message)">
            <bm-icon icon="trash" />
            <span class="pl-1">{{ $t("mail.actions.remove") }}</span>
        </bm-dropdown-item-button>
    </bm-dropdown>
</template>

<script>
import { BmDropdown, BmDropdownDivider, BmDropdownItemButton, BmIcon } from "@bluemind/styleguide";
import { RemoveMixin, ComposerInitMixin } from "~/mixins";

export default {
    name: "MailViewerDraftToolbarForMobile",
    components: {
        BmDropdown,
        BmDropdownDivider,
        BmDropdownItemButton,
        BmIcon
    },
    mixins: [RemoveMixin, ComposerInitMixin],
    props: {
        conversation: {
            type: Object,
            required: true
        },
        message: {
            type: Object,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

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
