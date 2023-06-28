<template>
    <bm-modal-deprecated v-model="show" centered hide-footer :title="$t('common.import')">
        <import-file :container="container" auto-upload class="mt-2" />
    </bm-modal-deprecated>
</template>

<script>
import ImportFile from "./ImportFile";
import { BmModalDeprecated } from "@bluemind/ui-components";

export default {
    name: "ImportModal",
    components: { BmModalDeprecated, ImportFile },
    data() {
        return {
            show: false,
            container: {}
        };
    },
    methods: {
        open(container) {
            this.show = true;
            this.container = container;
        }
    }
};
</script>
