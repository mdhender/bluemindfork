<template>
    <bm-icon
        v-if="compact"
        class="mail-toolbar-menu-button dropdown b-dropdown bm-icon-dropdown btn-group compact dropdown-no-caret"
        icon="3dots-horizontal"
    />
    <div v-else>
        <div class="mail-toolbar-menu-button btn bm-captioned-icon-button desktop-only btn-captioned-icon">
            <bm-icon icon="3dots-horizontal" />
            <span class="caption">{{ $t("mail.toolbar.more") }}</span>
        </div>
        <bm-icon class="mail-toolbar-menu-button mobile-only btn-icon-compact-on-fill-primary" icon="3dots" />
    </div>
</template>

<script>
import { BmIcon } from "@bluemind/ui-components";

export default {
    name: "MailToolbarMenuButton",
    components: { BmIcon },
    props: {
        compact: {
            type: Boolean,
            default: false
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.mail-toolbar-menu-button {
    &.bm-icon {
        $icon-size: map-get($icon-sizes, "lg");
        width: $icon-size !important;
        height: $icon-size !important;
        margin: $sp-3;
    }
    &.mobile-only {
        width: $icon-btn-width-compact-lg !important;
        margin: 0;
    }
}
</style>
