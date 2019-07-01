/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.eas.serdes.resolverecipients;

import net.bluemind.eas.dto.NamespaceMapping;
import net.bluemind.eas.dto.base.Callback;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response;
import net.bluemind.eas.dto.resolverecipients.ResolveRecipientsResponse.Response.Recipient;
import net.bluemind.eas.serdes.IEasResponseFormatter;
import net.bluemind.eas.serdes.IResponseBuilder;

public class ResolveRecipientsResponseFormatter implements IEasResponseFormatter<ResolveRecipientsResponse> {

	/**
	 * Exchange 2010 response:
	 * 
	 * <pre>
	 * <?xml version="1.0" encoding="UTF-8"?>
	 * <ResolveRecipients xmlns="ResolveRecipients">
	 *   <Status>1</Status>
	 *   <Response>
	 *     <To>tom@ex2k10.wmv</To>
	 *     <Status>1</Status>
	 *     <RecipientCount>1</RecipientCount>
	 *     <Recipient>
	 *       <Type>1</Type>
	 *       <DisplayName>Thomas Cataldo</DisplayName>
	 *       <EmailAddress>tom@ex2k10.wmv</EmailAddress>
	 *       <Availability>
	 *         <Status>1</Status>
	 *         <MergedFreeBusy>000</MergedFreeBusy>
	 *       </Availability>
	 *     </Recipient>
	 *   </Response>
	 *   <Response>
	 *     <To>david@ex2k10.wmv</To>
	 *     <Status>1</Status>
	 *     <RecipientCount>1</RecipientCount>
	 *     <Recipient>
	 *       <Type>1</Type>
	 *       <DisplayName>david david</DisplayName>
	 *       <EmailAddress>david@ex2k10.wmv</EmailAddress>
	 *       <Availability>
	 *         <Status>1</Status>
	 *         <MergedFreeBusy>220</MergedFreeBusy>
	 *       </Availability>
	 *     </Recipient>
	 *   </Response>
	 * </ResolveRecipients>
	 * </pre>
	 * 
	 * @see net.bluemind.eas.serdes.IEasResponseFormatter#format(double,
	 *      java.lang.Object)
	 */
	@Override
	public void format(IResponseBuilder builder, double protocolVersion, ResolveRecipientsResponse response,
			Callback<Void> completion) {
		builder.start(NamespaceMapping.ResolveRecipients);

		builder.text("Status", response.status.xmlValue());

		if (response.responses != null && !response.responses.isEmpty()) {
			for (Response resp : response.responses) {
				builder.container("Response");
				builder.text("To", resp.to);
				builder.text("Status", resp.status.xmlValue());
				builder.text("RecipientCount", resp.recipientCount.toString());

				for (Recipient recip : resp.recipients) {
					builder.container("Recipient");
					builder.text("Type", recip.type.xmlValue());
					builder.text("DisplayName", recip.displayName);
					builder.text("EmailAddress", recip.emailAddress);

					if (recip.availability != null) {
						builder.container("Availability");
						builder.text("Status", recip.availability.status.xmlValue());
						builder.text("MergedFreeBusy", recip.availability.mergedFreeBusy);
						builder.endContainer();
					}

					if (recip.certificate != null) {
						builder.container("Certificates");
						builder.text("Status", recip.certificate.status.xmlValue());
						builder.text("CertificateCount", recip.certificate.certificateCount.toString());
						builder.text("RecipientCount", recip.certificate.recipientCount.toString());
						builder.text("Certificate", recip.certificate.certificate);
						builder.text("MiniCertificate", recip.certificate.miniCertificate);
						builder.endContainer();
					}

					// FIXME iPad complains about missing picture even when it
					// didn't asked for one. Adding the picture results in a
					// worse error.
					// 0x14e34010|EAS|Error|ASResolveRecipientsSingleRecipientItem:
					// Parse Rule Constraint Violation. Received 0 counts of
					// code page 10 / token 0x1a, but the parse rule says we
					// have a range of 1 - 1
					//

					if (recip.picture != null) {
						builder.container("Picture");
						builder.text("Status", Recipient.Picture.Status.NoPhoto.xmlValue());
						builder.endContainer();
					}
					builder.endContainer(); // Recipient
				}
				builder.endContainer(); // Response
			}
		}
		builder.end(completion);
	}

}
