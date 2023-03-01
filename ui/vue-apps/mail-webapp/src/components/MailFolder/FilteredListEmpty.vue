<template>
    <div class="filtered-list-empty d-flex flex-column align-items-center justify-content-around h-50">
        <bm-illustration value="filter" size="sm" :title="$t('mail.folder.filter.no_results')" />
        <bm-button variant="text" size="sm" icon="cross" @click="$emit('clearFilter')">
            {{ $t("mail.folder.filter.remove") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmIllustration } from "@bluemind/ui-components";

export default {
    name: "FilteredListEmpty",
    components: { BmButton, BmIllustration }
};
</script>
