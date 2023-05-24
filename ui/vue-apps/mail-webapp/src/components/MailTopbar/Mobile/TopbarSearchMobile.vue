<template>
    <contextual-bar
        class="topbar-search-mobile d-flex align-items-center flex-fill"
        @back="RESET_CURRENT_SEARCH_PATTERN()"
    >
        <div class="d-flex align-items-center justify-content-between">
            <div class="bold">{{ $t("common.action.search") }}</div>
            <mail-search-advanced-button variant="compact-on-fill-primary" size="lg" class="mx-3" />
        </div>
    </contextual-bar>
</template>

<script>
import { mapMutations } from "vuex";
import {
    RESET_CURRENT_SEARCH_PATTERN,
    SET_CURRENT_SEARCH_PATTERN,
    UNSELECT_ALL_CONVERSATIONS,
    UNSET_CURRENT_CONVERSATION
} from "~/mutations";
import MailSearchAdvancedButton from "../../MailSearch/MailSearchAdvancedButton";
import ContextualBar from "./ContextualBar";

export default {
    components: { ContextualBar, MailSearchAdvancedButton },
    methods: {
        ...mapMutations("mail", {
            RESET_CURRENT_SEARCH_PATTERN,
            UNSELECT_ALL_CONVERSATIONS,
            UNSET_CURRENT_CONVERSATION,
            SET_CURRENT_SEARCH_PATTERN
        }),
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
.topbar-search-mobile {
    & > .slot-wrapper {
        flex: 1 1 auto;
    }
}
</style>
