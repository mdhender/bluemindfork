<template>
    <part class="mdn-template" mime="multipart/report" :subject="$t('template.eml.mdn.subject', parameters)">
        <part mime="multipart/alternative">
            <part mime="text/plain" encoding="quoted-printable" charset="utf-8">
                <pre><i18n path="template.eml.mdn.html" :tag="false">
<template #quoted>
<i18n path="template.eml.mdn.html.quoted" :tag="false">
<template #to>
    {{ $t("common.to") }} : {{ parameters.from }}</template>
<template #subject>
    {{ $t("common.subject") }} : {{ parameters.subject }}</template>
<template #date>
    {{ $t("common.date") }} : {{ parameters.date }}</template>
</i18n>
</template>
<template #notice>
{{ $t("template.eml.mdn.html.notice") }}</template>
                </i18n></pre>
            </part>
            <part mime="text/html" encoding="quoted-printable" charset="utf-8">
                <i18n path="template.eml.mdn.html" :tag="false">
                    <template #quoted>
                        <blockquote
                            style="
                                border-left: 1px solid var(--neutral-fg-lo2, rgba(83, 83, 83, 0.4));
                                padding-left: 10px;
                            "
                        >
                            <i18n path="template.eml.mdn.html.quoted" :tag="false">
                                <template #to>
                                    <strong>{{ $t("common.to") }} : </strong>{{ parameters.from }}<br />
                                </template>
                                <template #subject>
                                    <strong>{{ $t("common.subject") }} : </strong>{{ parameters.subject }}<br />
                                </template>
                                <template #date>
                                    <strong>{{ $t("common.date") }} : </strong>{{ parameters.date }}<br />
                                </template>
                            </i18n>
                        </blockquote>
                    </template>
                    <template #notice>
                        <em>{{ $t("template.eml.mdn.html.notice") }}</em>
                    </template>
                </i18n>
            </part>
        </part>
        <part mime="message/disposition-notification" encoding="7bit" file-name="MDN.txt" disposition-type="ATTACHMENT">
            <pre>
Reporting-UA: BlueMind Mail App\nOriginal-Recipient: {{ parameters.to }}
Final-Recipient: rfc822; {{ parameters.toAddress }}
Original-Message-ID: {{ parameters.messageId }}
Disposition: manual-action/MDN-sent-manually; displayed</pre
            >
        </part>
    </part>
</template>

<script>
import EmlTemplate from "../EmlTemplate";
export default { name: "MDNTemplate", extends: EmlTemplate };
</script>
