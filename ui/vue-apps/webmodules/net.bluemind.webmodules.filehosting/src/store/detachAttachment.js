import { inject } from "@bluemind/inject";
import { ADD_FH_FILE } from "./types/actions";
import { partUtils, messageUtils } from "@bluemind/mail";
import { renderLinksComponent } from "../helpers/renderers";
import { html2text } from "@bluemind/html-utils";
import { PartsBuilder } from "@bluemind/email";

const { MessageAdaptor, MessageHeader } = messageUtils;
const { sanitizeTextPartForCyrus } = partUtils;

export default async function extractAttachment({ commit, dispatch }, { file, message, vm }) {
    const service = inject("MailboxItemsPersistence", message.folderRef.uid);
    let textPartAddress, htmlPartAddress;
    try {
        const existingMessage = await service.getForUpdate(message.remoteRef.internalId);

        const content = await getContent(file);
        const newFakeFhFile = await dispatch(ADD_FH_FILE, { file, message, content }); // Overwrite file in store

        const { htmlContent, textContent } = getPartsContent(vm, { ...newFakeFhFile, size: file.size });
        const textAddressPromise = service.uploadPart(textContent);
        const htmlAddressPromise = service.uploadPart(htmlContent);
        [textPartAddress, htmlPartAddress] = await Promise.all([textAddressPromise, htmlAddressPromise]);
        const textPart = PartsBuilder.createTextPart(textPartAddress);
        const htmlPart = PartsBuilder.createHtmlPart(htmlPartAddress);
        newFakeFhFile.headers.push({ name: "X-BM-Prefered-Part", values: [true] });
        const alernativePart = PartsBuilder.createAlternativePart(textPart, newFakeFhFile, htmlPart);
        const structureWithReplacedPart = getStructureWithReplacedPart(
            existingMessage.value.body.structure,
            file.address,
            alernativePart
        );
        const updatedMessage = MessageAdaptor.toMailboxItem(message, structureWithReplacedPart);
        updatedMessage.body.headers = [{ name: MessageHeader.X_BM_REWRITE, values: [Date.now()] }];
        await service.updateById(message.remoteRef.internalId, updatedMessage);

        const oldAddress = file.address;
        await service.removePart(oldAddress);
    } catch (err) {
        commit("REMOVE_FILE", file);
        commit("ADD_FILE", { file });
        await service.removePart(textPartAddress);
        await service.removePart(htmlPartAddress);
        throw err;
    }
}

function getStructureWithReplacedPart(existingBodyStructure, address, newPart) {
    const newStructure = { ...existingBodyStructure };
    const AddressLevels = address.split(".");

    let currentPart = newStructure;
    for (let idx = 0; idx < AddressLevels.length; idx++) {
        const addressLevel = AddressLevels[idx];
        if (idx === AddressLevels.length - 1) {
            currentPart.children[addressLevel - 1] = newPart;
            break;
        } else {
            currentPart = currentPart.children[addressLevel - 1];
        }
    }
    return newStructure;
}

function getPartsContent(vm, file) {
    const links = renderLinksComponent(vm, { files: [file], simplified: true });
    links.$mount();
    const htmlContent = sanitizeTextPartForCyrus(links.$el.outerHTML);
    const textContent = sanitizeTextPartForCyrus(html2text(htmlContent));
    return { htmlContent, textContent };
}

async function getContent({ url }) {
    const content = await fetch(url);
    return await content.blob();
}
