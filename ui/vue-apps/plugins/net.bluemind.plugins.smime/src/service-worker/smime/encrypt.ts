import { MailboxItemsClient, MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email"; // FIXME: move MimeType into @bluemind/mime
import { MimeBuilder } from "@bluemind/mime";
import session from "@bluemind/session";
import { CRYPTO_HEADERS, SMIME_CERT_USAGE, SMIME_ENCRYPTION_ERROR_PREFIX } from "../../lib/constants";
import { SmimeErrors } from "../../lib/exceptions";
import { extractContentType, splitHeadersAndContent } from "./helpers/MimeEntityParserUtils";
import getRemoteContentFn from "./helpers/getRemoteContentFn";
import pkcs7 from "../pkcs7";
import { checkCertificate, getMyCertificate, getCertificate } from "../pki";

export default async function (item: MailboxItem, folderUid: string): Promise<MailboxItem> {
    try {
        const recipients = item.body.recipients || [];
        const promises = recipients.map(async ({ address, dn, kind }) => {
            if (!address) {
                throw "recipient " + dn + " has no address set";
            }
            const smimeUsage = SMIME_CERT_USAGE.ENCRYPT;
            if (kind === MessageBody.RecipientKind.Originator) {
                const myPem = await getMyCertificate();
                const myCertificate = await checkCertificate(myPem, { expectedAddress: address, smimeUsage });
                return myCertificate;
            }
            return getCertificate(address, smimeUsage);
        });
        const certificates = await Promise.all(promises);

        const client = new MailboxItemsClient(await session.sid, folderUid);
        let mimeTree: string;
        if (item.body.structure!.mime === MimeType.EML) {
            // mail has been signed just previously, and uploaded as an eml
            const eml = await client.fetch(item.imapUid!, item.body.structure!.address!).then(blob => blob.text());
            const { body, headers } = splitHeadersAndContent(eml);
            const contentType = "Content-Type: " + extractContentType(headers);
            mimeTree = contentType + "\r\n\r\n" + body;
        } else {
            mimeTree = await new MimeBuilder(getRemoteContentFn(item.imapUid!, folderUid)).build(item.body.structure!);
        }
        const encryptedPart = pkcs7.encrypt(mimeTree, certificates);
        const address = await client.uploadPart(encryptedPart);
        item.body.structure = { address, charset: "utf-8", encoding: "base64", mime: MimeType.PKCS_7 };
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        throw `[${SMIME_ENCRYPTION_ERROR_PREFIX}:${errorCode}]` + error;
    }
    return item;
}
