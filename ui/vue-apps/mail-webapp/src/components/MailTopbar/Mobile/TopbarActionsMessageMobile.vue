<template>
    <bm-navbar class="topbar-actions-message-mobile justify-content-between" @back="back">
        <bm-navbar-back @click="back" />
        <mail-toolbar />
    </bm-navbar>
</template>

<script>
import { mapGetters, mapMutations } from "vuex";
import MailToolbar from "../../MailToolbar/MailToolbar";
import { SELECTION_IS_EMPTY } from "~/getters";
import { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";
import { BmNavbar, BmNavbarBack } from "@bluemind/ui-components";

export default {
    name: "TopbarActionsMessageMobile",
    components: {
        MailToolbar,
        BmNavbar,
        BmNavbarBack
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
    .mail-toolbar {
        margin-left: $sp-5;
    }
}
</style>
