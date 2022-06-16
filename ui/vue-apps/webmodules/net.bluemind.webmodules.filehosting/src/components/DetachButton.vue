<template>
    <div class="detach-button">
        <bm-button variant="simple-neutral" :title="$tc('mail.filehosting.share.start')" @click="openFilePicker()">
            <bm-icon icon="cloud-up" size="lg" />
        </bm-button>
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
import { BmButton, BmIcon } from "@bluemind/styleguide";

export default {
    name: "DetachButton",
    components: { BmButton, BmIcon },
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
