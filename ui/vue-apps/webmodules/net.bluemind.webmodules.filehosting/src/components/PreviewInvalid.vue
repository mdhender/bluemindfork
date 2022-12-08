<template>
    <div v-if="isFhExpiredFile" class="preview-invalid">
        <div><bm-icon icon="cloud-exclamation" size="3xl" /></div>
        <span class="text"> {{ $t("mail.preview.nopreview.invalid") }}</span>
    </div>
    <div v-else><slot /></div>
</template>

<script>
import { mapGetters } from "vuex";
import { BmIcon } from "@bluemind/ui-components";
import FilehostingL10N from "../l10n";
import { GET_FH_FILE } from "../store/types/getters";

export default {
    name: "PreviewInvalid",
    components: { BmIcon },
    componentI18N: { messages: FilehostingL10N },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", [GET_FH_FILE]),
        isFhExpiredFile() {
            const file = this.GET_FH_FILE(this.file);
            return file && file.expirationDate < Date.now();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.preview-invalid {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    color: $fill-neutral-fg-lo1;

    .text {
        color: $fill-neutral-fg;
    }
}
</style>
