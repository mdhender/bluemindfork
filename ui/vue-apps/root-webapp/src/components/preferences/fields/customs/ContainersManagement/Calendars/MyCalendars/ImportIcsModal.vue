<template>
    <bm-modal v-model="show" centered hide-footer :title="$t('common.import')" body-class="import-ics-modal-body">
        <bm-file-drop-zone
            v-if="uploadStatus === 'IDLE'"
            class="mt-1"
            :file-type-regex="allowOnlyIcs"
            always-show-dropzone
            @drop-files="uploadIcsFile($event)"
        >
            <template #dropZone>
                <div class="text-center my-4">
                    <bm-icon icon="file-type-ics" size="lg" />
                    <h2 class="mt-2 mb-4">{{ $t("preferences.calendar.my_calendars.drop_ics_file") }}</h2>
                    <div class="mb-2">{{ $t("common.or") }}</div>
                    <bm-button variant="primary" @click="openFilePicker">{{ $t("common.browse") }}</bm-button>
                </div>
            </template>
        </bm-file-drop-zone>
        <template v-else>
            <bm-progress :value="uploaded" :max="uploadSize" class="mt-5 mb-2" />
            <bm-icon icon="file-type-ics" size="lg" class="mr-2 mt-1 align-top" />
            <div class="d-inline-block">
                <h2>{{ filename }}</h2>
                <span class="text-secondary">{{ uploadedWithUnit }} / {{ uploadSizeWithUnit }}</span>
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
            ref="chooseIcsFileInputRef"
            :accept="allowOnlyIcs"
            tabindex="-1"
            aria-hidden="true"
            type="file"
            hidden
            @change="uploadIcsFile($event.target.files)"
        />
    </bm-modal>
</template>

<script>
import { MimeType } from "@bluemind/email";
import { computeUnit } from "@bluemind/file-utils";
import { inject } from "@bluemind/inject";
import { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmModal, BmProgress } from "@bluemind/styleguide";

export default {
    name: "ImportIcsModal",
    components: { BmButton, BmButtonClose, BmFileDropZone, BmIcon, BmModal, BmProgress },
    data() {
        return {
            show: false,
            calendarContainerUid: "",

            uploadStatus: "IDLE",
            filename: "",
            uploaded: 0,
            uploadSize: 0,
            uploadCanceller: { cancel: undefined }
        };
    },
    computed: {
        allowOnlyIcs() {
            return MimeType.TEXT_CALENDAR || MimeType.ICS || MimeType.TEXT_PLAIN;
        },
        uploadedWithUnit() {
            return computeUnit(this.uploaded);
        },
        uploadSizeWithUnit() {
            return computeUnit(this.uploadSize);
        }
    },
    methods: {
        open(calendar) {
            this.show = true;
            this.calendarContainerUid = calendar.uid;
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
            this.$refs.chooseIcsFileInputRef.click();
        },
        async uploadIcsFile(files) {
            const file = files[0];

            this.uploadStatus = "IN_PROGRESS";
            this.filename = file.name;
            this.uploadSize = file.size;

            const onUploadProgress = progress => {
                this.uploaded = progress.loaded;
                this.uploadSize = progress.total;
                if (this.uploaded === this.uploadSize) {
                    this.uploadStatus = "SUCCESS";
                }
            };

            await inject("VEventPersistence", this.calendarContainerUid)
                .importIcs(file, this.uploadCanceller, onUploadProgress)
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

.import-ics-modal-body {
    .progress {
        height: 0.125rem;
    }
}
</style>
