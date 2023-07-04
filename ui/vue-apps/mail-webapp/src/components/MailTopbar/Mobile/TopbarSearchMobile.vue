<template>
    <bm-navbar>
        <bm-navbar-back @click="resetSearch" />
        <bm-navbar-title :title="$t('common.action.search')" />
        <mail-search-advanced-button variant="compact-on-fill-primary" size="lg" />
    </bm-navbar>
</template>

<script>
import { mapActions } from "vuex";
import { RESET_CURRENT_SEARCH } from "~/actions";
import { BmNavbar, BmNavbarBack, BmNavbarTitle } from "@bluemind/ui-components";
import MailSearchAdvancedButton from "../../MailSearch/MailSearchAdvancedButton";

export default {
    components: { BmNavbar, BmNavbarBack, BmNavbarTitle, MailSearchAdvancedButton },
    methods: {
        ...mapActions("mail", { RESET_CURRENT_SEARCH }),
        resetSearch() {
            this.RESET_CURRENT_SEARCH();
            this.$router.navigate({ name: "v:mail:home", params: { search: null } });
        }
    }
};
</script>
