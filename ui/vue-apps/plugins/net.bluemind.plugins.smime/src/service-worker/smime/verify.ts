import { MailboxItem, MessageBody } from "@bluemind/backend.mail.api";
import { ItemValue } from "@bluemind/core.container.api";
import { CRYPTO_HEADERS, SIGNED_HEADER_NAME } from "../../lib/constants";
import { SmimeErrors } from "../../lib/exceptions";
import extractSignedData from "./helpers/SMimeSignedDataParser";
import pkcs7 from "../pkcs7";
import { addHeaderValue, resetHeader } from "../../lib/helper";

export default async function (item: ItemValue<MailboxItem>, getEml: () => Promise<string>): Promise<MessageBody> {
    const body = item.value.body;
    try {
        body.headers = resetHeader(body.headers, SIGNED_HEADER_NAME);
        const eml = await getEml();
        const { toDigest, pkcs7Part } = extractSignedData(eml);
        await pkcs7.verify(pkcs7Part, toDigest, body);
        body.headers = addHeaderValue(body.headers, SIGNED_HEADER_NAME, CRYPTO_HEADERS.OK);
    } catch (error) {
        const errorCode = error instanceof SmimeErrors ? error.code : CRYPTO_HEADERS.UNKNOWN;
        body.headers = addHeaderValue(body.headers, SIGNED_HEADER_NAME, errorCode);
    }
    return body;
}
