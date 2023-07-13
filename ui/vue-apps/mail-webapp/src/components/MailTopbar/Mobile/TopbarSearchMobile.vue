<template>
    <contextual-bar class="topbar-search-mobile d-flex align-items-center flex-fill" @back="resetSearch">
        <div class="d-flex align-items-center justify-content-between">
            <div class="bold">{{ $t("common.action.search") }}</div>
            <mail-search-advanced-button variant="compact-on-fill-primary" size="lg" class="mx-3" />
        </div>
    </contextual-bar>
</template>

<script>
import { mapActions } from "vuex";
import { RESET_CURRENT_SEARCH } from "~/actions";
import MailSearchAdvancedButton from "../../MailSearch/MailSearchAdvancedButton";
import ContextualBar from "./ContextualBar";

export default {
    components: { ContextualBar, MailSearchAdvancedButton },
    methods: {
        ...mapActions("mail", { RESET_CURRENT_SEARCH }),
        resetSearch() {
            this.RESET_CURRENT_SEARCH();
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
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
