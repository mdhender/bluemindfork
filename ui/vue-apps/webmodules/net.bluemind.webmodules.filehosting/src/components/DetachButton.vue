<template>
    <div class="detach-button">
        <bm-dropdown-item-button variant="compact" size="lg" icon="cloud-arrow-up" @click="openFilePicker()">
            {{ $t("filehosting.share.upload") }}
        </bm-dropdown-item-button>
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
import { BmDropdownItemButton } from "@bluemind/ui-components";

export default {
    name: "DetachButton",
    components: { BmDropdownItemButton },
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
