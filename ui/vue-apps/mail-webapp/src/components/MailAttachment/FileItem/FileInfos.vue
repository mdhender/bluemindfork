<template>
    <bm-row class="no-gutters align-items-start file-infos">
        <bm-col
            class="icon-col"
            :title="$t('mail.content.file-type', { fileType: $t('mail.content.' + matchingIcon) })"
        >
            <bm-file-icon :file="file" />
        </bm-col>
        <bm-col class="text-nowrap text-truncate file-text">
            <div :title="file.name" class="text-truncate bold">{{ file.name }}</div>
            <div class="d-flex">
                <file-tags :file="file" />
                <div class="caption">{{ fileSize }}</div>
            </div>
        </bm-col>
        <div class="actions-wrapper">
            <slot name="actions" />
        </div>
    </bm-row>
</template>
<script>
import { BmCol, BmFileIcon, BmRow } from "@bluemind/ui-components";
import { computeUnit } from "@bluemind/file-utils";
import { MimeType } from "@bluemind/email";
import FileTags from "./FileTags";
export default {
    name: "FileInfos",
    components: {
        BmCol,
        BmFileIcon,
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
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.file-infos {
    position: relative;
    background-color: inherit;

    padding-top: $sp-2;
    padding-bottom: $sp-2;

    .icon-col {
        display: flex;
        flex: 0;
        margin-top: base-px-to-rem(2);
    }
    .file-text {
        margin-left: $sp-5;
    }
    .actions-wrapper {
        position: absolute;
        top: 0;
        bottom: 0;
        right: 0;
        display: flex;
        align-items: center;
        background-color: inherit;
        max-width: 100%;
        @include until-lg {
            gap: $sp-4;
        }
    }
}

@include from-lg {
    .file-item {
        &.disabled,
        &:not(:hover):not(:focus):not(:focus-within) {
            .file-infos .actions-wrapper {
                display: none;
            }
        }
    }
}
</style>
