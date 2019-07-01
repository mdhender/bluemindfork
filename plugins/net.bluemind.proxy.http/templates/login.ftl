<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
	<head>
		<title>Welcome to BlueMind</title>
		<link rel="shortcut icon" type="image/x-icon" href="/templates/favicon.ico" />
		<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
		<link rel="stylesheet" href="/templates/screen.css" type="text/css" />
	</head>
	<body onload="document.getElementById('login').focus()">
		<div id='sso-body'>
			<img id='sso-logo' src='/templates/logo-bluemind.png' alt='BlueMind' />
			<form id="login-form" enctype="application/x-www-form-urlencoded"
				action="bluemind_sso_security"
				method="post">
				
				<#if authErrorMsg??>
				<ul class="form-errors">
					<li>
						<ul class="errors">
							<li>Invalid login or password</li>
						</ul>
					</li>
				</ul>
				</#if>
				
				<fieldset id="fieldset-login-form">
					<legend>Please log in</legend>

					<div>
						<label for="login" class="required">Username:</label>
						<#if defaultDomain??>
						<input id="login" type="text" name="login" class="leftPart" autocapitalize="none" autocorrect="off" placeholder="login" />
						<div class="domainPart">@${defaultDomain}</div>
						<#else>
						    <input type="text" name="login" id="login" value="" autocapitalize="none" autocorrect="off" placeholder="login@domain.tld" />
						</#if>
					</div>
					<div>
						<label for="password" class="required">Password:</label>

						<input type="password" name="password" id="password" value="" placeholder="password" />
					</div>
					<div title="Select this option if you are the only person who uses this computer.">
						<input type="radio" name="priv" value="priv" id="priv" <#if priv = "true">checked="checked"</#if>/>
						<label for="priv" style="display: inline-block;" >Private computer</label>
					</div>
					<div title="Select this option if you use BlueMind on a public computer. Be sure to log off when you have finished using BlueMind and close all windows to end your session.">
						<input type="radio" name="priv" id="public" value="public" <#if priv = "false">checked="checked"</#if>/>
						<label for="public" style="display: inline-block;">Public computer</label>
						
					</div>	
					
					<input type="hidden" name="askedUri" value="${askedUri}" />
					
					<input type="submit" name="submit" id="submit" value="Connect"/>
				</fieldset>
			</form>
			<div id="corner"></div>
			<span id="version">edge</span>
			<#if buildVersion??>
			   <span id="buildVersion">${buildVersion}</span>
			</#if>
		</div>
	</body>
</html>
