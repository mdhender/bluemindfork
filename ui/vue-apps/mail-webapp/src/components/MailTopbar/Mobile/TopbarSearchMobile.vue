<template>
    <contextual-bar class="topbar-search-mobile d-flex align-items-center shadow" @back="SET_SEARCH_MODE_MOBILE(false)">
        {{ $t("common.action.search") }}
    </contextual-bar>
</template>

<script>
import { mapMutations } from "vuex";
import { SET_SEARCH_MODE_MOBILE, UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION } from "~/mutations";
import ContextualBar from "./ContextualBar";

export default {
    components: { ContextualBar },
    methods: {
        ...mapMutations("mail", { UNSELECT_ALL_CONVERSATIONS, UNSET_CURRENT_CONVERSATION, SET_SEARCH_MODE_MOBILE }),
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
