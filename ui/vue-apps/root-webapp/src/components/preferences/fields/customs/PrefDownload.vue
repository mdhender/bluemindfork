<template>
    <div class="pref-download">
        <h2 class="mb-2">{{ download.title }}</h2>
        <div class="d-flex justify-content-between p-2">
            <div class="d-flex flex-column justify-content-between">
                {{ download.desc }} {{ mimeType }}
                <div class="d-inline-flex pt-1">
                    <bm-button variant="outline-primary" :href="download.url" :aria-label="download.desc">{{
                        $t("common.download")
                    }}</bm-button>
                </div>
            </div>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <span class="icon" v-html="icon" />
        </div>
    </div>
</template>

<script>
import mime from "mime";
import { BmButton } from "@bluemind/styleguide";
import fileTypeAppIcon from "../../../../../assets/file-type-app-icon.svg";
import fileTypeDocIcon from "../../../../../assets/file-type-doc-icon.svg";
import fileTypeImgIcon from "../../../../../assets/file-type-img-icon.svg";
import fileTypeVideoIcon from "../../../../../assets/file-type-video-icon.svg";
import fileTypeDefaultIcon from "../../../../../assets/file-type-default-icon.svg";
import tbirdIcon from "../../../../../assets/tbird-icon.svg";

export default {
    name: "PrefDownload",
    components: { BmButton },
    props: {
        download: {
            type: Object,
            required: true
        }
    },
    computed: {
        mimeType() {
            return mime.getType(this.download.url);
        },
        icon() {
            if (!this.mimeType) {
                return fileTypeDefaultIcon;
            }
            const mimeTypeSplit = this.mimeType.split("/");
            const mimeType = mimeTypeSplit[0];
            const mimeSubType = mimeTypeSplit[1];
            switch (mimeType) {
                case "application":
                    if (mimeSubType === "octet-stream") {
                        return fileTypeAppIcon;
                    } else if (mimeSubType === "x-xpinstall") {
                        return tbirdIcon;
                    }
                    return fileTypeDocIcon;
                case "image":
                    return fileTypeImgIcon;
                case "video":
                    return fileTypeVideoIcon;
                case "text":
                    return fileTypeDocIcon;
                default:
                    return fileTypeDefaultIcon;
            }
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

.pref-download {
    div {
        background-color: $blue-100;
    }
    .icon svg {
        width: 60px;
        height: 60px;
    }
}
</style>
