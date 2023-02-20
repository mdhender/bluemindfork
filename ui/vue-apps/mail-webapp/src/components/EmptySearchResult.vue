<template>
    <div class="empty-search-result">
        <div>
            <p>
                <i18n path="mail.list.search.no_result">
                    <template #pattern>
                        <br />
                        <span class="search-pattern">"{{ pattern }}"</span>
                        <br />
                    </template>
                </i18n>
            </p>
            <p>{{ $t("common.search.try_otherwise") }}</p>
            <slot name="actions" />
        </div>
        <bm-illustration value="spider" size="md" over-background />
    </div>
</template>

<script>
import { BmIllustration } from "@bluemind/ui-components";

export default {
    name: "EmptySearchResult",
    components: { BmIllustration },
    props: {
        pattern: {
            type: String,
            required: true
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.empty-search-result {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding-top: $sp-7;
    gap: $sp-6;

    .search-pattern {
        color: $primary-fg;
        font-weight: $font-weight-bold;
        word-break: break-all;
    }

    .bm-illustration {
        max-width: 100%;
    }
}
</style>
