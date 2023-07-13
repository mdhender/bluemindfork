<template>
    <contextual-bar class="topbar-actions-message-mobile justify-content-between" @back="back">
        <mail-toolbar />
    </contextual-bar>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import MailToolbar from "../../MailToolbar/MailToolbar";
import ContextualBar from "./ContextualBar";
import { SELECTION_IS_EMPTY } from "~/getters";
import { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";

export default {
    name: "TopbarActionsMessageMobile",
    components: {
        MailToolbar,
        ContextualBar
    },
    computed: {
        ...mapGetters("mail", { SELECTION_IS_EMPTY })
    },
    methods: {
        ...mapMutations("mail", {
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION
        }),
        back() {
            if (this.SELECTION_IS_EMPTY) {
                this.UNSET_CURRENT_CONVERSATION();
            } else {
                this.UNSELECT_ALL_CONVERSATIONS();
            }
            this.$router.navigate({ name: "v:mail:home" });
        }
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
