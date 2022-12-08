<template>
    <div v-if="file">
        <div v-if="uploadStatus === 'IDLE'" class="d-flex align-items-center">
            <bm-icon :icon="fileTypeIcon" />
            <div class="regular ml-4">{{ file.name }}</div>
            <bm-button-close class="ml-2" size="sm" @click="resetFile" />
        </div>
        <template v-else>
            <bm-progress :value="uploaded" :max="100" class="mt-4 mb-2" />
            <div class="d-flex align-items-center">
                <bm-icon :icon="fileTypeIcon" />
                <div class="regular ml-4">{{ file.name }}</div>
            </div>
            <div class="float-right">
                <bm-button-close v-if="uploadStatus === 'IN_PROGRESS' && autoUpload" size="sm" @click="cancelUpload" />
                <div v-else-if="uploadStatus === 'SUCCESS'">
                    <bm-icon icon="check-circle" class="text-success" />
                    {{ $t("common.import_successful") }}
                </div>
                <div v-else-if="uploadStatus === 'ERROR'">
                    <bm-icon icon="exclamation-circle" class="text-danger" />
                    {{ $t("common.import_error") }}
                </div>
            </div>
        </template>
    </div>
    <div v-else>
        <bm-file-drop-zone :should-activate-fn="shouldActivate" always-show-dropzone @drop-files="dropFile($event)">
            <template #dropZone>
                <div class="text-center my-4">
                    <bm-icon :icon="fileTypeIcon" size="lg" />
                    <div class="bold my-5">
                        {{ $t("preferences.display_containers.import_file." + container.type) }}
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
import { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmProgress } from "@bluemind/ui-components";
import { retrieveTaskResult } from "@bluemind/task";

export default {
    name: "ImportFile",
    components: { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmProgress },
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
