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
import { mapGetters } from "vuex";
import { BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import FilehostingL10N from "../l10n";
import { GET_FH_FILE } from "../store/types/getters";

export default {
    name: "CloudIcon",
    components: { BmLabelIcon, BmIcon },
    componentI18N: { messages: FilehostingL10N },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", [GET_FH_FILE]),
        isFhFile() {
            return !!this.GET_FH_FILE(this.file);
        },
        hasExpired() {
            return this.file.expirationDate && this.file.expirationDate < Date.now();
        }
    }
};
</script>
