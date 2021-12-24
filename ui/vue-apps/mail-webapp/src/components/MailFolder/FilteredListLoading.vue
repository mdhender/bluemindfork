<template>
    <div class="filtered-list-loading d-flex flex-column align-items-center justify-content-around h-50">
        <div class="h-50 text-center">
            <bm-spinner />
        </div>
        <bm-button variant="inline-secondary" @click="$emit('clearFilter')">
            {{ $t("mail.folder.filter.remove") }}
        </bm-button>
    </div>
</template>

<script>
import { BmButton, BmSpinner } from "@bluemind/styleguide";

export default {
    name: "FilteredListLoading",
    components: { BmButton, BmSpinner }
};
</script>
