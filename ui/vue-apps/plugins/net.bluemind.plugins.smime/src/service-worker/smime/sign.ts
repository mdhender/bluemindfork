import { MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email"; // FIXME: move MimeType into @bluemind/mime
import { MimeBuilder } from "@bluemind/mime";
import session from "@bluemind/session";
import { CRYPTO_HEADERS, SMIME_CERT_USAGE, SMIME_SIGNATURE_ERROR_PREFIX } from "../../lib/constants";
import { SmimeErrors } from "../../lib/exceptions";
import buildSignedEml from "./helpers/SMimeSignedEmlBuilder";
import pkcs7 from "../pkcs7";
import { checkCertificate, getMyCertificate, getMyPrivateKey } from "../pki";
import getRemoteContentFn from "./helpers/getRemoteContentFn";

export default async function (item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        item.body.structure = removePreviousSignedPart(item.body.structure!);

        const client = new MailboxItemsClient(await session.sid, folderUid);
        const unsignedMimeEntity = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(
            item.body.structure!
        );
        const key = await getMyPrivateKey();
        const certificate = await getMyCertificate();
        await checkCertificate(certificate, { smimeUsage: SMIME_CERT_USAGE.SIGN });
        const signedContent = await pkcs7.sign(unsignedMimeEntity, key, certificate);
        const eml = buildSignedEml(unsignedMimeEntity, signedContent, item.body);
        const address = await client.uploadPart(eml);
        item.body.structure = { address, mime: "message/rfc822", children: [] };
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_SIGNATURE_ERROR_PREFIX}:${errorCode}]` + error;
    }
    return item;
}

function removePreviousSignedPart(structure: MessageBody.Part): MessageBody.Part {
    if (structure.mime === MimeType.MULTIPART_MIXED && structure.children) {
        const signedPartIndex = structure.children.findIndex(part => part.mime === MimeType.PKCS_7_SIGNED_DATA);
        if (signedPartIndex !== -1) {
            structure.children.splice(signedPartIndex, 1);
            if (structure.children.length === 1) {
                return structure.children[0];
            }
        }
    }
    return structure;
}
