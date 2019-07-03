<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
	<head>
		<title>Bienvenue dans BlueMind</title>
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
							<li>Utilisateur ou mot de passe invalide</li>
						</ul>
					</li>
				</ul>
				</#if>
				
				<fieldset id="fieldset-login-form">
					<legend>Identifiez-vous, svp</legend>

					<div>
						<label for="login" class="required">Utilisateur :</label>

						<#if defaultDomain??>
						<input id="login" type="text" name="login" class="leftPart" autocapitalize="none" autocorrect="off" placeholder="identifiant" />
						<div class="domainPart">@${defaultDomain}</div>
						<#else>
						    <input type="text" name="login" id="login" value="" autocapitalize="none" autocorrect="off" placeholder="identifiant@domaine.tld" />
						</#if>
					</div>
					<div>
						<label for="password" class="required">Mot de passe :</label>

						<input type="password" name="password" id="password" value="" placeholder="mot de passe" />
					</div>

					<div title="Sélectionnez cette option si vous êtes la seule personne qui utilise cet ordinateur.">
						<input type="radio" name="priv" value="priv" id="priv" <#if priv = "true">checked="checked"</#if>/>
						<label for="priv" style="display: inline-block;" >Ordinateur privé</label>
					</div>
					<div title="Sélectionnez cette option si vous utilisez BlueMind sur un ordinateur public. Assurez-vous de vous déconnecter lorsque vous avez fini d'utiliser BlueMind et fermez toutes les fenêtres pour mettre fin à votre session.">
						<input type="radio" name="priv" id="public" value="public" <#if priv = "false">checked="checked"</#if>/>
						<label for="public" style="display: inline-block;">Ordinateur public</label>
					</div>	
					

					<input type="hidden" name="askedUri" value="${askedUri}" />
					
					<input type="submit" name="submit" id="submit" value="Se connecter"/>
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
