<template>
    <bm-modal v-model="show" centered hide-footer :title="$t('common.import')">
        <import-file :container="container" auto-upload class="mt-2" />
    </bm-modal>
</template>

<script>
import ImportFile from "./ImportFile";
import { BmModal } from "@bluemind/ui-components";

export default {
    name: "ImportModal",
    components: { BmModal, ImportFile },
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
