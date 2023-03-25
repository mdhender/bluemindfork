import Builder from "@bluemind/emailjs-mime-builder";
import { MessageBody } from "@bluemind/backend.mail.api";
import { MimeType } from "@bluemind/email";
import { extractContentType, splitHeadersAndContent } from "./MimeEntityParserUtils";

export default function (unsignedPart: string, signedContent: string, messageBody: MessageBody) {
    const rootContentType = `${MimeType.MULTIPART_SIGNED}; protocol="${MimeType.PKCS_7_SIGNED_DATA}"; micalg=sha-256;`;
    const rootNode = new Builder(rootContentType);
    setRootHeaders(rootNode, messageBody);

    const { body: unsignedContent, headers: unsignedHeaders } = splitHeadersAndContent(unsignedPart);
    const contentType = extractContentType(unsignedHeaders);
    const unsignedNode = new Builder(contentType, { isEncoded: true });
    unsignedNode.setContent(unsignedContent);
    rootNode.appendChild(unsignedNode);

    const signedNode = new Builder(`${MimeType.PKCS_7_SIGNED_DATA}; name="smime.p7s"`, { isEncoded: true });
    signedNode.setHeader("Content-Transfer-Encoding", "base64");
    signedNode.setHeader("Content-Disposition", 'attachment; filename="smime.p7s"');
    signedNode.setContent(signedContent);
    rootNode.appendChild(signedNode);

    return rootNode.build();
}

function setRootHeaders(rootNode: Builder, body: MessageBody) {
    const date = body.date ? new Date(body.date) : new Date();
    rootNode.setHeader("Date", date.toUTCString().replace(/GMT/, "+0000"));
    rootNode.setHeader("MIME-Version", "MIME-Version: 1.0");
    rootNode.setHeader("Subject", body.subject || "");

    body.headers?.forEach(header => {
        rootNode.setHeader(header.name!, header.values!.join(","));
    });

    const from = body.recipients!.find(recipient => recipient.kind === MessageBody.RecipientKind.Originator);
    if (from) {
        rootNode.setHeader("From", formatAddress(from));
    }

    const toRecipients = body.recipients!.filter(recipient => recipient.kind === MessageBody.RecipientKind.Primary);
    if (toRecipients.length > 0) {
        rootNode.setHeader("To", toRecipients.map(formatAddress).join(", "));
    }

    const ccRecipients = body.recipients!.filter(recipient => recipient.kind === MessageBody.RecipientKind.CarbonCopy);
    if (ccRecipients.length > 0) {
        rootNode.setHeader("Cc", ccRecipients.map(formatAddress).join(", "));
    }

    const bccRecipients = body.recipients!.filter(
        recipient => recipient.kind === MessageBody.RecipientKind.BlindCarbonCopy
    );
    if (bccRecipients.length > 0) {
        rootNode.setHeader("Bcc", bccRecipients.map(formatAddress).join(", "));
    }
}

function formatAddress(recipient: MessageBody.Recipient): string {
    if (recipient.dn) {
        return recipient.dn + "<" + recipient.address + ">";
    }
    return recipient.address!;
}
