<template>
    <div class="pref-domain-filter-rules">
        <hr />
        <pref-filter-rules-subset
            :filters="domainFilters"
            :title="$t('preferences.mail.filters.subset.domain', { count: domainFilters.length })"
        />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { read as readRules } from "./filterRules";
import PrefFilterRulesSubset from "./PrefFilterRulesSubset";

export default {
    name: "PrefFilterRules",
    components: { PrefFilterRulesSubset },
    data() {
        return { domainFilters: [] };
    },
    async created() {
        this.domainFilters = await this.fetchDomainFilters();
    },
    methods: {
        async fetchDomainFilters() {
            const domainFilters = await inject("MailboxesPersistence")?.getDomainFilter();
            return readRules(domainFilters.rules);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-domain-filter-rules {
    hr {
        background-color: $neutral-bg-lo1;
    }
}
</style>
