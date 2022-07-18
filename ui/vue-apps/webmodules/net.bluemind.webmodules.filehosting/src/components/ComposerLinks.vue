<template>
    <div
        v-if="files.length > 0"
        :class="`composer-links ${className}`"
        style="padding: 15px; background-color: #d9edff;"
        contenteditable="false"
    >
        <div style="margin-bottom: 15px;">
            {{ $tc("mail.filehosting.link", files.length) }}
        </div>
        <div style="background-color: #ffffff; padding: 15px;">
            <div
                v-for="file in files"
                :key="file.address"
                style="
                    border: 1px solid #cdcdcd;
                    border-radius: 5px;
                    margin-top: 10px;
                    margin-bottom: 10px;
                    padding: 15px;
                "
            >
                <img
                    style="margin-right: 5px; float: left; width: 24px; height: 24px;"
                    :src="`data:image/gif;${images.file}`"
                    alt="file"
                />
                <span style="float: right;">
                    <img
                        style="margin-right: 5px; vertical-align: middle;"
                        :src="`data:image/gif;${images.bm}`"
                        alt="BlueMind"
                    />
                    <a style="color: #0f7edb !important;" :href="bmUrl">BlueMind</a>
                </span>
                <a style="color: #0f7edb !important;" :href="file.url" :download="file.name">
                    {{ file.name }}
                </a>
                <span v-if="file.size" style="margin-left: 5px; font-size: small; color: grey;">
                    ({{ displaySize(file.size) }})
                </span>
                <span v-if="file.expirationDate" style="display: block; font-size: small; color: grey;">
                    {{ $t("mail.filehosting.expiration_date", { date: dateToString(file.expirationDate) }) }}
                </span>
            </div>
        </div>
    </div>
</template>

<script>
import { computeUnit } from "@bluemind/file-utils";
export default {
    name: "ComposerLinks",
    props: {
        className: {
            type: String,
            required: true
        },
        files: {
            type: Array,
            required: true
        }
    },
    data() {
        return {
            images: {
                file:
                    "base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAQAAABKfvVzAAAArUlEQVR4Ae3LMUoDQQCF4e8a4hbRNOJFBE+VIFpZRMtopzmAkhMErcRbLBZZBGVmGzGJNhmw2RnYKuBXvOr9+js214oeHZXdPy1NXGl8lCRzS/ug0niQ1ZrYuhZkbYxsjWyKgrS/QdqdCP6DlXNcWJUGzxqX3i3ywdoYQ0++LBxgbK3Dmzt/zdQ63Ar2SCrRVIeh1ktKKq+CgU6nouDemZkoOJF16EbtW21qoK8f5VpvkEyGyc0AAAAASUVORK5CYII=",
                bm:
                    "base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyRpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMC1jMDYxIDY0LjE0MDk0OSwgMjAxMC8xMi8wNy0xMDo1NzowMSAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNS4xIE1hY2ludG9zaCIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDo0OUVCRkE3RDcyMUQxMUUxODZERkUzOTYxN0JFNjVGNSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDo0OUVCRkE3RTcyMUQxMUUxODZERkUzOTYxN0JFNjVGNSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOjQ5RUJGQTdCNzIxRDExRTE4NkRGRTM5NjE3QkU2NUY1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjQ5RUJGQTdDNzIxRDExRTE4NkRGRTM5NjE3QkU2NUY1Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+HYY/cAAAA5ZJREFUeNpEU3ts01UYPff37GtrWQVU9kBXTFNCptlgojMVR3wszk1QUbPEiJII+o8KUUOi6IyayQwhGIWw0YA4MEbdzBYN1G6jTDNl3bIMZFhkcWVs7Srd6Lr+2v6uX9uoN/nuK/c7N98552Pb9+yFruswWSx45NkXoRpk6BxgwP20fAJgkvY7aT8uCkAsOoc/xkYgCHSgkZ+zz/8fqyjaJRk+kxl3Gk2oF0UM0d0eiqLsA875fyHkNzoYY2YmCB8Q8KjRjOfDodBxT+v+tZ2e4xsTN+YDdPcOYxgWRHGrohqYoqrIhqQlk7kSOBfKFFl8S9NSY18dO/ny2cGA2XbP5s9TsdmQ783du+oaG1a6N9YeNppMHy29ZcUxnfMUJUESRQkpbQGyLHJVBvznLk0MxswvrXm19WktGodkNFel1tc19HhP7iu+IzLlLL1JESWZ61oSsqKA9QRn0ek5jPMjQ7dt2H0kOMGNTM8AlzoPdZzvaG1WC4uWVWx99/0S94M1QgpYkQhdnun9sryu6QXY7ETJ2RhHf5iv3ue98O2W3gx/1DM8ZndWubNkUTKUAhtE1chK3ZuaNn0fiT7304L+zYVI15lZfldfhEhMafg0y3K5y9moKgJiE+N9BcWOvpsrN0C2WKEWFMHurOSLf09/EZ/6c8KgqrrVbq+n8n+jOJSVcQepeLXraNv2RDwBiai1ljlBP+bC7loHa5kLRvutkFVV/St4cfq7ts/cigGXKXebwAgilcRowO/rZnTQ0+lUfGYS08P9CA1043pwFDeuBpGci2QNoOuZjPSr74d++j1AspIPct7hkijLFol0tTnW3Be/dqV60t+FeDiExOwUZkYHYFpavLmw1FEmKKpCfskpyHNOpFk2MEFcnBsPtDefKCxZ5XrowKlf7t51sN2yrOT2JY6K6tqW7lO1LUe/TieT0siR9/YXFlpB6uvZXPSGOR+Y5+GDp8/VV9U9ieWVD9y7/u2O08/4OX+i5/r8Fm8y/ZR3MV2x4+N2Uqe85vEmnBi68rA/xif7KJcRQDPhvEKINk3Aj/3ewdcu/uzF6tqG7msWx0qjkMHy+eDvY4NnGh1r3aiudrWoOh4j88aJggPMN5NjoZTKekNgpIgEiDLSZBoxGl3wiKKg2ZYYtmVEsLRGLUMmo25tI4APCSD4LwBYviHXkaR7aS2g2JnRMl7yPMkn1SDf2tR5eJ1WKjDfw/8IMADrEovnH+QybQAAAABJRU5ErkJggg=="
            },
            bmUrl: "http://www.bluemind.net/"
        };
    },
    methods: {
        displaySize(size) {
            return computeUnit(size, this.$i18n);
        },
        dateToString(timestamp) {
            return this.$d(timestamp, "short_date");
        }
    }
};
</script>
