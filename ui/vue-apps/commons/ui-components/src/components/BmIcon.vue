<template>
    <font-awesome-icon
        v-if="!stacked"
        :icon="icon"
        :fixed-width="fixedWidth"
        class="bm-icon"
        :class="[variantClass, `fa-${size}`]"
        :rotation="rotation"
        :flip="flip"
        :transform="transform"
    />
    <font-awesome-layers v-else class="bm-icon" :class="size ? `fa-${size}` : ''" :fixed-width="fixedWidth">
        <font-awesome-icon
            v-for="(stackedIcon, index) in stacked"
            :key="index"
            :icon="stackedIcon.icon || stackedIcon"
            :fixed-width="stackedIcon.fixedWidth"
            class="bm-icon"
            :class="[stackedIcon.class || variantClass, `fa-${stackedIcon.size || size}`]"
            :rotation="stackedIcon.rotation || rotation"
            :flip="stackedIcon.flip || flip"
            :transform="stackedIcon.transform || transform"
        />
    </font-awesome-layers>
</template>

<script>
import { bm3Dots } from "../icons/bm3dots";
import { bm3DotsCalendar } from "../icons/bm3dotsCalendar";
import { bm3DotsEnveloppe } from "../icons/bm3dotsEnveloppe";
import { bm3DotsV } from "../icons/bm3dotsV";
import { bm6DotsV } from "../icons/bm6dotsV";
import { bm9Dots } from "../icons/bm9dots";
import { bmAddressBook } from "../icons/bmAddressBook";
import { bmAddressBookShared } from "../icons/bmAddressBookShared";
import { bmArrowBack } from "../icons/bmArrowBack";
import { bmArrowDown } from "../icons/bmArrowDown";
import { bmArrowUp } from "../icons/bmArrowUp";
import { bmBlueMind } from "../icons/bmBlueMind";
import { bmBroom } from "../icons/bmBroom";
import { bmBurgerMenu } from "../icons/bmBurgerMenu";
import { bmCables } from "../icons/bmCables";
import { bmCalendar } from "../icons/bmCalendar";
import { bmCaretDown } from "../icons/bmCaretDown";
import { bmCaretFirst } from "../icons/bmCaretFirst";
import { bmCaretLast } from "../icons/bmCaretLast";
import { bmCaretRight } from "../icons/bmCaretRight";
import { bmCheck } from "../icons/bmCheck";
import { bmCheckCircle } from "../icons/bmCheckCircle";
import { bmCheckStamp } from "../icons/bmCheckStamp";
import { bmChevron } from "../icons/bmChevron";
import { bmChevronLeft } from "../icons/bmChevronLeft";
import { bmChevronRight } from "../icons/bmChevronRight";
import { bmChevronUp } from "../icons/bmChevronUp";
import { bmClock } from "../icons/bmClock";
import { bmCloud } from "../icons/bmCloud";
import { bmCloudExclamation } from "../icons/bmCloudExclamation";
import { bmCloudUp } from "../icons/bmCloudUp";
import { bmCode } from "../icons/bmCode";
import { bmCollapse } from "../icons/bmCollapse";
import { bmConnector } from "../icons/bmConnector";
import { bmCopy } from "../icons/bmCopy";
import { bmCross } from "../icons/bmCross";
import { bmCrossFramed } from "../icons/bmCrossFramed";
import { bmCursor } from "../icons/bmCursor";
import { bmDocuments } from "../icons/bmDocuments";
import { bmDownload } from "../icons/bmDownload";
import { bmEmoticon } from "../icons/bmEmoticon";
import { bmExclamation } from "../icons/bmExclamation";
import { bmExclamationCalendar } from "../icons/bmExclamationCalendar";
import { bmExclamationCircle } from "../icons/bmExclamationCircle";
import { bmExclamationCircleFill } from "../icons/bmExclamationCircleFill";
import { bmExclamationEnveloppe } from "../icons/bmExclamationEnveloppe";
import { bmExpand } from "../icons/bmExpand";
import { bmExtend } from "../icons/bmExtend";
import { bmEye } from "../icons/bmEye";
import { bmFile } from "../icons/bmFile";
import { bmFileTypeAudio } from "../icons/bmFileTypeAudio";
import { bmFileTypeCode } from "../icons/bmFileTypeCode";
import { bmFileTypeCompressed } from "../icons/bmFileTypeCompressed";
import { bmFileTypeData } from "../icons/bmFileTypeData";
import { bmFileTypeExcel } from "../icons/bmFileTypeExcel";
import { bmFileTypeIcs } from "../icons/bmFileTypeIcs";
import { bmFileTypeImage } from "../icons/bmFileTypeImage";
import { bmFileTypeMessage } from "../icons/bmFileTypeMessage";
import { bmFileTypePdf } from "../icons/bmFileTypePdf";
import { bmFileTypePresentation } from "../icons/bmFileTypePresentation";
import { bmFileTypeSystem } from "../icons/bmFileTypeSystem";
import { bmFileTypeUnknown } from "../icons/bmFileTypeUnknown";
import { bmFileTypeVCard } from "../icons/bmFileTypeVCard";
import { bmFileTypeVideo } from "../icons/bmFileTypeVideo";
import { bmFileTypeWord } from "../icons/bmFileTypeWord";
import { bmFilter } from "../icons/bmFilter";
import { bmFlagFill } from "../icons/bmFlagFill";
import { bmFlagOutline } from "../icons/bmFlagOutline";
import { bmFolder } from "../icons/bmFolder";
import { bmFolderFill } from "../icons/bmFolderFill";
import { bmFolderShared } from "../icons/bmFolderShared";
import { bmFolderUp } from "../icons/bmFolderUp";
import { bmForbidden } from "../icons/bmForbidden";
import { bmForbiddenShared } from "../icons/bmForbiddenShared";
import { bmForward } from "../icons/bmForward";
import { bmGroup } from "../icons/bmGroup";
import { bmHome } from "../icons/bmHome";
import { bmImage } from "../icons/bmImage";
import { bmInbox } from "../icons/bmInbox";
import { bmInfoCircle } from "../icons/bmInfoCircle";
import { bmInfoCirclePlain } from "../icons/bmInfoCirclePlain";
import { bmInterrogation } from "../icons/bmInterrogation";
import { bmKey } from "../icons/bmKey";
import { bmList } from "../icons/bmList";
import { bmListShared } from "../icons/bmListShared";
import { bmLock } from "../icons/bmLock";
import { bmLoop } from "../icons/bmLoop";
import { bmMessage } from "../icons/bmMessage";
import { bmMinus } from "../icons/bmMinus";
import { bmOrganization } from "../icons/bmOrganization";
import { bmPaperClip } from "../icons/bmPaperClip";
import { bmPaperPlane } from "../icons/bmPaperPlane";
import { bmPaperPlaneShared } from "../icons/bmPaperPlaneShared";
import { bmPen } from "../icons/bmPen";
import { bmPencil } from "../icons/bmPencil";
import { bmPencilShared } from "../icons/bmPencilShared";
import { bmPhone } from "../icons/bmPhone";
import { bmPlay } from "../icons/bmPlay";
import { bmPlus } from "../icons/bmPlus";
import { bmPlusDocument } from "../icons/bmPlusDocument";
import { bmPlusEnveloppe } from "../icons/bmPlusEnveloppe";
import { bmPlusFolder } from "../icons/bmPlusFolder";
import { bmPlusFolderShared } from "../icons/bmPlusFolderShared";
import { bmPopup } from "../icons/bmPopup";
import { bmPreferences } from "../icons/bmPreferences";
import { bmPrinter } from "../icons/bmPrinter";
import { bmQuote } from "../icons/bmQuote";
import { bmRead } from "../icons/bmRead";
import { bmRedo } from "../icons/bmRedo";
import { bmRename } from "../icons/bmRename";
import { bmReply } from "../icons/bmReply";
import { bmReplyAll } from "../icons/bmReplyAll";
import { bmRichEditorAddColumn } from "../icons/bmRichEditorAddColumn";
import { bmRichEditorAddRow } from "../icons/bmRichEditorAddRow";
import { bmRichEditorAlignCenter } from "../icons/bmRichEditorAlignCenter";
import { bmRichEditorAlignLeft } from "../icons/bmRichEditorAlignLeft";
import { bmRichEditorAlignRight } from "../icons/bmRichEditorAlignRight";
import { bmRichEditorBold } from "../icons/bmRichEditorBold";
import { bmRichEditorDeleteColumn } from "../icons/bmRichEditorDeleteColumn";
import { bmRichEditorDeleteRow } from "../icons/bmRichEditorDeleteRow";
import { bmRichEditorItalic } from "../icons/bmRichEditorItalic";
import { bmRichEditorLink } from "../icons/bmRichEditorLink";
import { bmRichEditorList } from "../icons/bmRichEditorList";
import { bmRichEditorNumberedList } from "../icons/bmRichEditorNumberedList";
import { bmRichEditorStrikethrough } from "../icons/bmRichEditorStrikethrough";
import { bmRichEditorTable } from "../icons/bmRichEditorTable";
import { bmRichEditorUnderline } from "../icons/bmRichEditorUnderline";
import { bmSave } from "../icons/bmSave";
import { bmSearch } from "../icons/bmSearch";
import { bmSend } from "../icons/bmSend";
import { bmServer } from "../icons/bmServer";
import { bmShare } from "../icons/bmShare";
import { bmSignOut } from "../icons/bmSignOut";
import { bmSpam } from "../icons/bmSpam";
import { bmStar } from "../icons/bmStar";
import { bmStarFill } from "../icons/bmStarFill";
import { bmTag } from "../icons/bmTag";
import { bmTextFormat } from "../icons/bmTextFormat";
import { bmTextHighlight } from "../icons/bmTextHighlight";
import { bmTrash } from "../icons/bmTrash";
import { bmTrashShared } from "../icons/bmTrashShared";
import { bmUndo } from "../icons/bmUndo";
import { bmUnlink } from "../icons/bmUnlink";
import { bmUnread } from "../icons/bmUnread";
import { bmUpload } from "../icons/bmUpload";
import { bmUser } from "../icons/bmUser";
import { bmUserAdd } from "../icons/bmUserAdd";
import { bmUserCalendar } from "../icons/bmUserCalendar";
import { bmUserEnveloppe } from "../icons/bmUserEnveloppe";
import { bmVideo } from "../icons/bmVideo";
import { bmVideoCircle } from "../icons/bmVideoCircle";
import { bmWand } from "../icons/bmWand";
import { bmWeight } from "../icons/bmWeight";
import { bmWithAttachment } from "../icons/bmWithAttachment";
import { bmWorld } from "../icons/bmWorld";
import { bmWrench } from "../icons/bmWrench";
import { FontAwesomeIcon, FontAwesomeLayers } from "@fortawesome/vue-fontawesome";
import { library } from "@fortawesome/fontawesome-svg-core";

initIcons();

export { library };

export default {
    name: "BmIcon",
    components: {
        FontAwesomeIcon,
        FontAwesomeLayers
    },
    props: {
        size: {
            type: String,
            default: "md",
            validator: function validator(value) {
                return ["xs", "sm", "md", "lg", "xl", "2xl", "3xl"].indexOf(value) > -1;
            }
        },
        icon: {
            type: [Object, Array, String],
            required: false,
            default: undefined
        },
        stacked: {
            type: Array,
            required: false,
            default: undefined
        },
        fixedWidth: {
            type: Boolean,
            default: false
        },
        variant: {
            type: String,
            default: ""
        },
        rotation: {
            type: String,
            default: undefined
        },
        flip: {
            type: String,
            default: undefined
        },
        transform: {
            type: String,
            default: undefined
        }
    },
    computed: {
        variantClass() {
            if (this.variant) {
                return `text-${this.variant}`;
            }
            return "";
        }
    }
};

function initIcons() {
    library.add(bm3Dots);
    library.add(bm3DotsCalendar);
    library.add(bm3DotsEnveloppe);
    library.add(bm3DotsV);
    library.add(bm6DotsV);
    library.add(bm9Dots);
    library.add(bmAddressBook);
    library.add(bmAddressBookShared);
    library.add(bmArrowBack);
    library.add(bmArrowDown);
    library.add(bmArrowUp);
    library.add(bmBlueMind);
    library.add(bmBroom);
    library.add(bmBurgerMenu);
    library.add(bmCables);
    library.add(bmCalendar);
    library.add(bmCaretDown);
    library.add(bmCaretFirst);
    library.add(bmCaretLast);
    library.add(bmCaretRight);
    library.add(bmCheck);
    library.add(bmCheckCircle);
    library.add(bmCheckStamp);
    library.add(bmChevron);
    library.add(bmChevronLeft);
    library.add(bmChevronRight);
    library.add(bmChevronUp);
    library.add(bmClock);
    library.add(bmCloud);
    library.add(bmCloudExclamation);
    library.add(bmCloudUp);
    library.add(bmCode);
    library.add(bmCollapse);
    library.add(bmConnector);
    library.add(bmCopy);
    library.add(bmCross);
    library.add(bmCrossFramed);
    library.add(bmCursor);
    library.add(bmDocuments);
    library.add(bmDownload);
    library.add(bmEmoticon);
    library.add(bmExclamation);
    library.add(bmExclamationCalendar);
    library.add(bmExclamationCircle);
    library.add(bmExclamationCircleFill);
    library.add(bmExclamationEnveloppe);
    library.add(bmExpand);
    library.add(bmExtend);
    library.add(bmEye);
    library.add(bmFile);
    library.add(bmFileTypeAudio);
    library.add(bmFileTypeCode);
    library.add(bmFileTypeCompressed);
    library.add(bmFileTypeData);
    library.add(bmFileTypeExcel);
    library.add(bmFileTypeIcs);
    library.add(bmFileTypeImage);
    library.add(bmFileTypeMessage);
    library.add(bmFileTypePdf);
    library.add(bmFileTypePresentation);
    library.add(bmFileTypeSystem);
    library.add(bmFileTypeUnknown);
    library.add(bmFileTypeVCard);
    library.add(bmFileTypeVideo);
    library.add(bmFileTypeWord);
    library.add(bmFilter);
    library.add(bmFlagFill);
    library.add(bmFlagOutline);
    library.add(bmFolder);
    library.add(bmFolderFill);
    library.add(bmFolderShared);
    library.add(bmFolderUp);
    library.add(bmForbidden);
    library.add(bmForbiddenShared);
    library.add(bmForward);
    library.add(bmGroup);
    library.add(bmHome);
    library.add(bmImage);
    library.add(bmInbox);
    library.add(bmInfoCircle);
    library.add(bmInfoCirclePlain);
    library.add(bmInterrogation);
    library.add(bmKey);
    library.add(bmList);
    library.add(bmListShared);
    library.add(bmLock);
    library.add(bmLoop);
    library.add(bmMessage);
    library.add(bmMinus);
    library.add(bmOrganization);
    library.add(bmPaperClip);
    library.add(bmPaperPlane);
    library.add(bmPaperPlaneShared);
    library.add(bmPen);
    library.add(bmPencil);
    library.add(bmPencilShared);
    library.add(bmPhone);
    library.add(bmPlay);
    library.add(bmPlus);
    library.add(bmPlusDocument);
    library.add(bmPlusEnveloppe);
    library.add(bmPlusFolder);
    library.add(bmPlusFolderShared);
    library.add(bmPopup);
    library.add(bmPreferences);
    library.add(bmPrinter);
    library.add(bmQuote);
    library.add(bmRead);
    library.add(bmRedo);
    library.add(bmRename);
    library.add(bmReply);
    library.add(bmReplyAll);
    library.add(bmRichEditorAddColumn);
    library.add(bmRichEditorAddRow);
    library.add(bmRichEditorAlignCenter);
    library.add(bmRichEditorAlignLeft);
    library.add(bmRichEditorAlignRight);
    library.add(bmRichEditorBold);
    library.add(bmRichEditorDeleteColumn);
    library.add(bmRichEditorDeleteRow);
    library.add(bmRichEditorItalic);
    library.add(bmRichEditorLink);
    library.add(bmRichEditorList);
    library.add(bmRichEditorNumberedList);
    library.add(bmRichEditorStrikethrough);
    library.add(bmRichEditorTable);
    library.add(bmRichEditorUnderline);
    library.add(bmSave);
    library.add(bmSearch);
    library.add(bmSend);
    library.add(bmServer);
    library.add(bmShare);
    library.add(bmSignOut);
    library.add(bmSpam);
    library.add(bmStar);
    library.add(bmStarFill);
    library.add(bmTag);
    library.add(bmTextFormat);
    library.add(bmTextHighlight);
    library.add(bmTrash);
    library.add(bmTrashShared);
    library.add(bmUndo);
    library.add(bmUnlink);
    library.add(bmUnread);
    library.add(bmUpload);
    library.add(bmUser);
    library.add(bmUserAdd);
    library.add(bmUserCalendar);
    library.add(bmUserEnveloppe);
    library.add(bmVideo);
    library.add(bmVideoCircle);
    library.add(bmWand);
    library.add(bmWeight);
    library.add(bmWithAttachment);
    library.add(bmWorld);
    library.add(bmWrench);
}
</script>

<style lang="scss">
@import "../css/_variables.scss";

@each $name, $value in $icon-sizes {
    .bm-icon.fa-#{$name} {
        width: $value;
        height: $value;
        flex: none;
    }
}
</style>
