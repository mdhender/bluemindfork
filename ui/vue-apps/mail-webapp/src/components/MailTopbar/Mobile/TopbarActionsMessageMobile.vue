<template>
    <contextual-bar class="topbar-actions-message-mobile justify-content-between">
        <mail-toolbar />
    </contextual-bar>
</template>

<script>
import MailToolbar from "../../MailToolbar/MailToolbar";
import ContextualBar from "./ContextualBar";

export default {
    name: "TopbarActionsMessageMobile",
    components: {
        MailToolbar,
        ContextualBar
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.topbar-actions-message-mobile {
    background-color: $fill-primary-bg;
    flex: 1 1 auto;
    & > .mail-toolbar-selected-conversations,
    & > .mail-toolbar-compose-message {
        gap: base-px-to-rem(16) !important;
    }
}
</style>
