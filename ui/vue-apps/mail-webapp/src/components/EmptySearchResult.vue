<template>
    <div class="empty-search-result">
        <div>
            <p>
                <i18n v-if="!searchQuery.folder" path="mail.list.search.no_result">
                    <template #pattern>
                        <br />
                        <span class="search-pattern">"{{ pattern }}"</span>
                        <br />
                    </template>
                </i18n>
                <i18n v-else path="mail.list.search.no_result.folder">
                    <template #pattern>
                        <br />
                        <span class="search-pattern">"{{ pattern }}"</span>
                        <br />
                    </template>
                    <template #folder> {{ folderName }} </template>
                    <template v-if="searchQuery.deep" #sub> {{ $t("mail.list.search.no_result.deep") }} </template>
                </i18n>
            </p>
            <p>{{ $t("common.search.try_otherwise") }}</p>
            <slot name="actions" />
        </div>
        <bm-illustration value="spider" size="md" over-background />
    </div>
</template>

<script>
import { mapState } from "vuex";
import { BmIllustration } from "@bluemind/ui-components";

export default {
    name: "EmptySearchResult",
    components: { BmIllustration },
    props: {
        pattern: {
            type: String,
            required: true
        }
    },
    computed: {
        ...mapState("mail", ["folders"]),
        ...mapState("mail", { searchQuery: state => state.conversationList.search.searchQuery }),
        folderName() {
            return this.folders[this.searchQuery.folder.key]?.name;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

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
        flex: none;
        max-width: 100%;
    }
}
</style>
