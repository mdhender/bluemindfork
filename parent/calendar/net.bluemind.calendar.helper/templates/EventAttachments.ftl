<#if attachments??>
	<#list attachments as attachment>
		<div id="cloudAttachmentListRoot" style="padding: 15px; background-color: #d9edff;">
			<div id="cloudAttachmentList" style="background-color: #ffffff; padding: 15px;">
			<div class="cloudAttachmentItem" style="border: 1px  solid  #cdcdcd; border-radius: 5px; margin-top: 10px; margin-bottom: 10px; padding: 15px;"><a style="color: #0f7edb ;" href="${attachment.uri}" target="_blank">${attachment.name}</a><span style="margin-left: 5px; font-size: small; color: grey;">(146 ko)</span><span style="display: block; font-size: small; color: grey;"></span></div>
			</div>
		</div>
		</br>
	</#list>
</#if>