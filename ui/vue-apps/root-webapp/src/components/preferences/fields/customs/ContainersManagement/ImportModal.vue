<template>
    <bm-modal v-model="show" centered hide-footer :title="$t('common.import')" body-class="import-modal-body">
        <bm-file-drop-zone
            v-if="uploadStatus === 'IDLE'"
            class="mt-1"
            :file-type-regex="allowedFileTypes"
            always-show-dropzone
            @drop-files="uploadFile($event)"
        >
            <template #dropZone>
                <div class="text-center my-4">
                    <bm-icon :icon="fileTypeIcon" size="lg" />
                    <h2 class="mt-2 mb-4">{{ $t("preferences.display_containers.import_file." + container.type) }}</h2>
                    <div class="mb-2">{{ $t("common.or") }}</div>
                    <bm-button variant="primary" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                </div>
            </template>
        </bm-file-drop-zone>
        <template v-else>
            <bm-progress :value="uploaded" :max="uploadSize" class="mt-4 mb-2" />
            <div class="d-flex">
                <bm-icon :icon="fileTypeIcon" size="lg" class="mr-2 mt-1 align-top" />
                <div>
                    <h2>{{ filename }}</h2>
                    <span class="text-secondary">{{ uploadedWithUnit }} / {{ uploadSizeWithUnit }}</span>
                </div>
            </div>
            <div class="float-right">
                <bm-button-close v-if="uploadStatus === 'IN_PROGRESS'" @click="cancelUpload" />
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
        <input
            ref="fileChooserRef"
            :accept="allowedFileTypes"
            tabindex="-1"
            aria-hidden="true"
            type="file"
            hidden
            @change="uploadFile($event.target.files)"
        />
    </bm-modal>
</template>

<script>
import { allowedFileTypes, importFileRequest, matchingFileTypeIcon } from "./container";
import { computeUnit } from "@bluemind/file-utils";
import { inject } from "@bluemind/inject";
import { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmModal, BmProgress } from "@bluemind/styleguide";
import { retrieveTaskResult } from "@bluemind/task";

export default {
    name: "ImportModal",
    components: { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmModal, BmProgress },
    data() {
        return {
            show: false,
            container: {},

            uploadStatus: "IDLE",
            filename: "",
            uploaded: 0,
            uploadSize: 0,
            uploadCanceller: { cancel: undefined }
        };
    },
    computed: {
        allowedFileTypes() {
            return allowedFileTypes(this.container.type);
        },
        fileTypeIcon() {
            return matchingFileTypeIcon(this.container.type);
        },
        uploadedWithUnit() {
            return computeUnit(this.uploaded, inject("i18n"));
        },
        uploadSizeWithUnit() {
            return computeUnit(this.uploadSize, inject("i18n"));
        }
    },
    methods: {
        open(container) {
            this.show = true;
            this.container = container;
            this.initUploadInfos();
        },
        initUploadInfos() {
            this.uploadStatus = "IDLE";
            this.filename = "";
            this.uploaded = 0;
            this.uploadSize = 0;
            this.uploadCanceller = { cancel: undefined };
        },
        openFilePicker() {
            this.$refs.fileChooserRef.click();
        },
        async uploadFile(files) {
            const file = files[0];
            // console.log(file);

            this.uploadStatus = "IN_PROGRESS";
            this.filename = file.name;
            this.uploadSize = file.size;

            // const onUploadProgress = taskStatus => {
            // FIXME: choose between displaying progress with file size or "number of elements":
            // - pass an onUploadProgress fn to axios (need to modify codegen to add this possibility when request returns a taskRef)
            // - display progression thanks to taskStatus results
            // console.log("coucou onUploadProgress ! ");
            // console.log(taskStatus);
            // // this.uploaded = progress.loaded;
            // // this.uploadSize = progress.total;
            // if (this.uploaded === this.uploadSize) {
            //     this.uploadStatus = "SUCCESS";
            // }
            // };
            const onUploadProgress = () => {};
            const taskRef = await importFileRequest(this.container, file, this.uploadCanceller);
            const taskService = inject("TaskService", taskRef.id);
            retrieveTaskResult(taskService, onUploadProgress)
                .then(() => {
                    this.uploadStatus = "SUCCESS";
                })
                .catch(() => {
                    this.uploadStatus = "ERROR";
                });
        },
        cancelUpload() {
            this.uploadCanceller.cancel();
            this.initUploadInfos();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.import-modal-body {
    .progress {
        height: 0.125rem;
    }
}
</style>
