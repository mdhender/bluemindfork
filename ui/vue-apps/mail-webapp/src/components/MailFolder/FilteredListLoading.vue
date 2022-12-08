<template>
    <div class="filtered-list-loading d-flex flex-column align-items-center justify-content-around h-50">
        <div class="h-50 text-center">
            <bm-spinner />
        </div>
    </div>
</template>

<script>
import { BmSpinner } from "@bluemind/ui-components";

export default {
    name: "FilteredListLoading",
    components: { BmSpinner }
};
</script>
