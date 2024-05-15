<template>
    <div v-if="file" class="import-file">
        <div v-if="uploadStatus === 'IDLE'" class="d-flex align-items-end mt-6">
            <bm-icon :icon="fileTypeIcon" />
            <div class="regular text-truncate ml-4">{{ file.name }}</div>
            <bm-button-close class="ml-4" size="sm" @click="resetFile" />
        </div>
        <template v-else>
            <bm-progress :value="uploaded" :max="100" />
            <div class="d-flex align-items-end">
                <bm-icon :icon="fileTypeIcon" />
                <div class="regular text-truncate ml-4">{{ file.name }}</div>
            </div>
            <div class="align-self-end">
                <bm-button-close v-if="uploadStatus === 'IN_PROGRESS' && autoUpload" size="sm" @click="cancelUpload" />
                <bm-label-icon
                    v-else-if="uploadStatus === 'SUCCESS'"
                    icon="check-circle-fill"
                    class="import-successful"
                >
                    {{ $t("common.import_successful") }}
                </bm-label-icon>
                <bm-label-icon v-else-if="uploadStatus === 'ERROR'" icon="exclamation-circle" class="import-error">
                    {{ $t("common.import_error") }}
                </bm-label-icon>
            </div>
        </template>
    </div>
    <div v-else class="import-file">
        <bm-file-drop-zone :should-activate-fn="shouldActivate" always-show-dropzone @drop-files="dropFile($event)">
            <template #dropZone>
                <div class="drop-zone-content">
                    <div class="icon-and-text">
                        <bm-icon :icon="fileTypeIcon" size="lg" />
                        <div>{{ $t("preferences.display_containers.import_file." + container.type) }}</div>
                    </div>
                    <div class="mb-5">{{ $t("common.or") }}</div>
                    <bm-button variant="fill-accent" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                </div>
            </template>
        </bm-file-drop-zone>
        <input
            ref="fileChooserRef"
            :accept="allowedFileTypes"
            tabindex="-1"
            aria-hidden="true"
            type="file"
            hidden
            @change="dropFile($event.target.files)"
        />
    </div>
</template>

<script>
import { ContainerHelper } from "./container";
import { inject } from "@bluemind/inject";
import { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmLabelIcon, BmProgress } from "@bluemind/ui-components";
import { retrieveTaskResult } from "@bluemind/task";

export default {
    name: "ImportFile",
    components: { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmLabelIcon, BmProgress },
    props: {
        container: {
            type: Object,
            required: true
        },
        autoUpload: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            file: null,
            uploadStatus: "IDLE",
            uploaded: 0,
            uploadCanceller: { cancel: undefined }
        };
    },
    computed: {
        helper() {
            return ContainerHelper.use(this.container.type);
        },
        allowedFileTypes() {
            return this.helper.allowedFileTypes();
        },
        fileTypeIcon() {
            return this.helper.matchingFileTypeIcon();
        }
    },
    methods: {
        openFilePicker() {
            this.$refs.fileChooserRef.click();
        },
        dropFile(files) {
            this.file = files[0];
            if (this.autoUpload) {
                // containerUid must be defined if autoUpload prop is true
                this.uploadFile(this.container.uid);
            }
        },
        // is also called by parent component with
        async uploadFile(containerUid) {
            if (this.file) {
                this.uploadStatus = "IN_PROGRESS";
                const onUploadProgress = () => {
                    if (this.uploaded < 90) {
                        this.uploaded += 10;
                    }
                };
                try {
                    const taskRef = await this.helper.importFileRequest(containerUid, this.file, this.uploadCanceller);
                    const taskService = inject("TaskService", taskRef.id);
                    await retrieveTaskResult(taskService, onUploadProgress);
                    this.uploaded = 100;
                    this.uploadStatus = "SUCCESS";
                } catch (e) {
                    this.uploadStatus = "ERROR";
                    throw new Error(e);
                }
            }
        },
        cancelUpload() {
            this.uploadCanceller.cancel();
            this.initUploadInfos();
        },
        initUploadInfos() {
            this.file = null;
            this.uploadStatus = "IDLE";
            this.uploaded = 0;
            this.uploadCanceller = { cancel: undefined };
        },
        resetFile() {
            this.file = null;
        },
        shouldActivate(event) {
            const files = event.dataTransfer.items.length
                ? Object.keys(event.dataTransfer.items).map(key => event.dataTransfer.items[key])
                : [];
            const matchFunction = f => f.type.match(new RegExp(this.allowedFileTypes, "i"));
            return files.some(matchFunction);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.import-file {
    height: base-px-to-rem(160);
    display: flex;
    flex-direction: column;
    margin-top: $sp-2;
    gap: $sp-5;

    .bm-file-drop-zone {
        height: 100%;

        .drop-zone-content {
            flex: 1;

            &,
            .icon-and-text {
                display: flex;
                flex-direction: column;
                align-items: center;
                text-align: center;
            }

            justify-content: space-evenly;

            .icon-and-text {
                gap: $sp-3;
            }
        }
    }
    .bm-label-icon {
        &.import-successful .bm-icon {
            color: $success-fg;
        }
        &.import-error .bm-icon {
            color: $danger-fg;
        }
    }
}
</style>
