<template>
    <div v-if="file">
        <div v-if="uploadStatus === 'IDLE'" class="d-flex align-items-center">
            <bm-icon :icon="fileTypeIcon" size="lg" />
            <h2 class="ml-2">{{ file.name }}</h2>
            <bm-button-close class="ml-2" @click="resetFile" />
        </div>
        <template v-else>
            <bm-progress :value="uploaded" :max="100" class="mt-4 mb-2" />
            <div class="d-flex align-items-center">
                <bm-icon :icon="fileTypeIcon" size="lg" />
                <h2 class="ml-2">{{ file.name }}</h2>
            </div>
            <div class="float-right">
                <bm-button-close v-if="uploadStatus === 'IN_PROGRESS' && autoUpload" @click="cancelUpload" />
                <div v-else-if="uploadStatus === 'SUCCESS'">
                    <bm-icon icon="check-circle" size="lg" class="text-success" />
                    {{ $t("common.import_successful") }}
                </div>
                <div v-else-if="uploadStatus === 'ERROR'">
                    <bm-icon icon="exclamation-circle" size="lg" class="text-danger" />
                    {{ $t("common.import_error") }}
                </div>
            </div>
        </template>
    </div>
    <div v-else>
        <bm-file-drop-zone :file-type-regex="allowedFileTypes" always-show-dropzone @drop-files="dropFile($event)">
            <template #dropZone>
                <div class="text-center my-4">
                    <bm-icon :icon="fileTypeIcon" size="lg" />
                    <h2 class="mt-2 mb-4">{{ $t("preferences.display_containers.import_file." + container.type) }}</h2>
                    <div class="mb-2">{{ $t("common.or") }}</div>
                    <bm-button variant="primary" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
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
import { allowedFileTypes, importFileRequest, matchingFileTypeIcon } from "./container";
import { WARNING } from "@bluemind/alert.store";
import { inject } from "@bluemind/inject";
import { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmProgress } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";
import { mapActions } from "vuex";

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
        allowedFileTypes() {
            return allowedFileTypes(this.container.type);
        },
        fileTypeIcon() {
            return matchingFileTypeIcon(this.container.type);
        }
    },
    methods: {
        ...mapActions("alert", { WARNING }),
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
                const IMPORT_DATA_ALERT = {
                    alert: { name: "preferences.containers.import_data", uid: "IMPORT_DATA_UID" },
                    options: { area: "pref-right-panel", renderer: "DefaultAlert" }
                };
                this.uploadStatus = "IN_PROGRESS";
                const onUploadProgress = () => {
                    if (this.uploaded < 90) {
                        this.uploaded += 10;
                    }
                };
                try {
                    const taskRef = await importFileRequest(
                        this.container.type,
                        containerUid,
                        this.file,
                        this.uploadCanceller
                    );
                    const taskService = inject("TaskService", taskRef.id);
                    await retrieveTaskResult(taskService, onUploadProgress);
                    this.uploaded = 100;
                    this.uploadStatus = "SUCCESS";
                } catch {
                    this.uploadStatus = "ERROR";
                    this.WARNING(IMPORT_DATA_ALERT);
                    throw new Error();
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
        }
    }
};
</script>
