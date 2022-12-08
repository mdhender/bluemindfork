<template>
    <div class="detach-button">
        <bm-icon-button
            variant="compact"
            size="lg"
            icon="cloud-up"
            :title="$tc('filehosting.share.start')"
            @click="openFilePicker()"
        />
        <input
            ref="detachInputRef"
            tabindex="-1"
            aria-hidden="true"
            type="file"
            multiple
            hidden
            @change="doDetach"
            @click.stop="closeFilePicker()"
        />
    </div>
</template>

<script>
import { BmIconButton } from "@bluemind/ui-components";
import FilehostingL10N from "../l10n";

export default {
    name: "DetachButton",
    components: { BmIconButton },
    componentI18N: { messages: FilehostingL10N },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    methods: {
        openFilePicker() {
            this.$refs.detachInputRef.click();
        },
        closeFilePicker() {
            this.$refs.detachInputRef.value = "";
        },
        doDetach(event) {
            this.$execute(
                "add-attachments",
                {
                    files: event.target.files,
                    message: this.message
                },
                { forceFilehosting: true }
            );
        }
    }
};
</script>
