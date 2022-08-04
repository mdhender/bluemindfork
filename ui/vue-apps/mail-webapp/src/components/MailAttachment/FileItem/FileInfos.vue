<template>
    <bm-row class="no-gutters align-items-center file-infos">
        <bm-col
            class="col-auto icon"
            :title="$t('mail.content.file-type', { fileType: $t('mail.content.' + matchingIcon) })"
        >
            <bm-icon :icon="matchingIcon" class="align-bottom" />
        </bm-col>
        <bm-col class="text-nowrap text-truncate flex-grow-1 px-2 file-text">
            <span :title="file.name" class="font-weight-bold">{{ file.name }} </span>
            <br />
            <div class="d-inline-flex">
                <file-tags :file="file" />
                {{ fileSize }}
            </div>
        </bm-col>
        <bm-col class="col-auto actions">
            <slot name="actions" />
        </bm-col>
    </bm-row>
</template>
<script>
import { BmCol, BmIcon, BmRow } from "@bluemind/styleguide";
import { computeUnit } from "@bluemind/file-utils";
import { MimeType } from "@bluemind/email";
import FileTags from "./FileTags";

export default {
    name: "FileInfos",
    components: {
        BmCol,
        BmIcon,
        BmRow,
        FileTags
    },
    props: {
        file: {
            type: Object,
            required: true
        }
    },
    computed: {
        fileSize() {
            return this.file.size > 0 ? computeUnit(this.file.size, this.$i18n) : "--";
        },
        matchingIcon() {
            return MimeType.matchingIcon(this.file.mime);
        }
    }
};
</script>

<style lang="scss">
@use "sass:math";
@import "@bluemind/styleguide/css/_variables.scss";

.file-infos {
    .icon,
    .file-text {
        padding-top: math.div($sp-1, 2);
        padding-bottom: math.div($sp-1, 2);
    }
}
</style>
