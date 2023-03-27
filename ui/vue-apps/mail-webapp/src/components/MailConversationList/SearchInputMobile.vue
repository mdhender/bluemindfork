<template>
    <!-- TODO i18n -->
    <div class="search-input-mobile">
        <bm-form-input
            ref="search"
            class="search-input"
            variant="underline"
            placeholder="Rechercher un mot-clé"
            @focus="$emit('focus')"
            @blur="$emit('blur')"
        />
    </div>
</template>

<script>
import { BmFormInput } from "@bluemind/ui-components";
export default {
    name: "SearchInputMobile",
    components: { BmFormInput },
    mounted() {
        this.$refs.search.focus();
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.search-input-mobile {
    & > .search-input {
        background: $surface;
        & > input {
            border-color: $neutral-fg-lo3;
        }
    }
}
</style>
