<template>
    <bm-label-icon
        v-if="isFhFile && hasExpired"
        class="cloud-icon caption"
        icon-size="xs"
        icon="cloud-exclamation"
        size="xs"
    >
        <span class="caption">[{{ $t("filehosting.expired") }}]</span>
    </bm-label-icon>

    <bm-icon v-else-if="isFhFile" class="cloud-icon" icon="cloud" size="xs" />
</template>

<script>
import { mapMutations } from "vuex";
import { BmLabelIcon, BmIcon } from "@bluemind/ui-components";
import { getFhHeader } from "../helpers";

export default {
    name: "CloudIcon",
    components: { BmLabelIcon, BmIcon },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        isFhFile() {
            return !!getFhHeader(this.file.headers);
        },
        hasExpired() {
            return this.file.expirationDate && this.file.expirationDate < Date.now();
        }
    }
};
</script>
