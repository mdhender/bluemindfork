<?php

/*
 +-----------------------------------------------------------------------+
 | localization/fi_FI/messages.inc                                       |
 |                                                                       |
 | Language file of the Roundcube Webmail client                         |
 | Copyright (C) 2012, The Roundcube Dev Team                            |
 | Licensed under the GNU General Public License                         |
 |                                                                       |
 +-----------------------------------------------------------------------+
 | Author: Jiri Grönroos                                                |
 +-----------------------------------------------------------------------+
*/

$messages = array();
$messages['errortitle'] = 'Virhe havaittu.';
$messages['loginfailed'] = 'Sisäänkirjautuminen epäonnistui';
$messages['cookiesdisabled'] = 'Selaimesi ei hyväksy evästeitä';
$messages['sessionerror'] = 'Sessio ei kelpaa tai ei ole enää voimassa';
$messages['storageerror'] = 'Yhteys IMAP-palvelimeen epäonnistui';
$messages['servererror'] = 'Palvelinvirhe!';
$messages['servererrormsg'] = 'Palvelinvirhe: $msg';
$messages['dberror'] = 'Tietokantavirhe!';
$messages['requesttimedout'] = 'Pyyntö aikakatkaistiin';
$messages['errorreadonly'] = 'Toiminnon suoritus ei onnistu, koska hakemisto on vain lukutilassa.';
$messages['errornoperm'] = 'Toimintoa ei voitu suorittaa. Ei oikeuksia.';
$messages['invalidrequest'] = 'Virheellinen pyyntö! Tietoa ei tallennettu.';
$messages['nomessagesfound'] = 'Kansiossa ei ole sähköpostiviestejä';
$messages['loggedout'] = 'Sinut on kirjattu ulos järjestelmästä.';
$messages['mailboxempty'] = 'Kansio on tyhjä';
$messages['loading'] = 'Ladataan...';
$messages['uploading'] = 'Ladataan tiedostoa palvelimelle...';
$messages['uploadingmany'] = 'Ladataan tiedostoja...';
$messages['loadingdata'] = 'Ladataan tietoja...';
$messages['checkingmail'] = 'Tarkistetaan saapuneita viestejä...';
$messages['sendingmessage'] = 'Lähetetään viestiä...';
$messages['messagesent'] = 'Viesti lähetetty';
$messages['savingmessage'] = 'Tallennetaan viestiä...';
$messages['messagesaved'] = 'Viesti tallennettu "Luonnokset"-kansioon';
$messages['successfullysaved'] = 'Tallennus onnistui';
$messages['addedsuccessfully'] = 'Yhteystieto lisätty osoitekirjaan';
$messages['contactexists'] = 'Samalla sähköpostiosoitteella on jo olemassa yhteystieto';
$messages['contactnameexists'] = 'Yhteystieto samalla nimellä on jo olemassa';
$messages['blockedimages'] = 'Turvallisuussyistä viestin kuvia ei näytetty.';
$messages['encryptedmessage'] = 'Viesti on salattu, eikä sitä voida näyttää.';
$messages['nocontactsfound'] = 'Yhteystietoja ei löytynyt.';
$messages['contactnotfound'] = 'Pyydettyä yhteystietoa ei löytynyt';
$messages['contactsearchonly'] = 'Anna hakusanoja, joilla yhteystietoja haetaan';
$messages['sendingfailed'] = 'Viestin lähetys epäonnistui';
$messages['senttooquickly'] = 'Odota $sec sekunti(a) ennen viestin lähettämistä';
$messages['errorsavingsent'] = 'Lähetetyn viestin tallennuksessa tapahtui virhe';
$messages['errorsaving'] = 'Tallennuksessa tapahtui virhe';
$messages['errormoving'] = 'Viestiä ei voitu siirtää';
$messages['errormovingreadonly'] = 'Viestiä ei voitu siirtää, koska hakemisto on vain lukutilassa.';
$messages['errorcopying'] = 'Viestiä ei voitu kopioida';
$messages['errordeleting'] = 'Viestiä ei voitu poistaa';
$messages['errormarking'] = 'Viestiä ei voitu merkiä';
$messages['deletecontactconfirm'] = 'Haluatko varmasti poistaa valitut yhteystiedot?';
$messages['deletegroupconfirm'] = 'Haluatko varmasti poistaa valitun ryhmän?';
$messages['deletemessagesconfirm'] = 'Haluatko varmasti poistaa valitut viestit?';
$messages['deletefolderconfirm'] = 'Haluatko varmasti poistaa kansion?';
$messages['purgefolderconfirm'] = 'Haluatko varmasti poistaa kaikki viestit kansiosta?';
$messages['contactdeleting'] = 'Poistetaan yhteystietoja...';
$messages['groupdeleting'] = 'Poistetaan ryhmää...';
$messages['folderdeleting'] = 'Poistetaan kansiota...';
$messages['foldermoving'] = 'Siirretään kansiota...';
$messages['foldersubscribing'] = 'Tilataan kansio...';
$messages['folderunsubscribing'] = 'Perutaan kansio tilaus...';
$messages['formincomplete'] = 'Lomakkeen tiedot olivat puutteelliset';
$messages['noemailwarning'] = 'Anna sähköpostiosoite';
$messages['nonamewarning'] = 'Anna nimi';
$messages['nopagesizewarning'] = 'Anna sivukoko';
$messages['nosenderwarning'] = 'Anna lähettäjän sähköpostiosoite';
$messages['norecipientwarning'] = 'Anna ainakin yksi vastaanottaja';
$messages['nosubjectwarning'] = '"Otsikko"-kenttä on tyhjä. Haluatko kirjoittaa viestillesi otsikon?';
$messages['nobodywarning'] = 'Lähetetäänkö viesti ilman tekstiä?';
$messages['notsentwarning'] = 'Viestiä ei lähetetty. Haluatko poistaa viestin?';
$messages['noldapserver'] = 'Valitse LDAP-palvelin';
$messages['nosearchname'] = 'Anna yhteystiedon nimi tai sähköpostiosoite';
$messages['notuploadedwarning'] = 'Kaikkia liitteitä ei ole vielä ladattu palvelimelle. Odota tai peruuta lataus.';
$messages['searchsuccessful'] = 'Löydetty $nr viestiä';
$messages['contactsearchsuccessful'] = 'Löydetty $nr yhteystietoa';
$messages['searchnomatch'] = 'Haku ei tuottanut tuloksia';
$messages['searching'] = 'Etsitään...';
$messages['checking'] = 'Tarkistetaan...';
$messages['nospellerrors'] = 'Kirjoitusvirheitä ei löytynyt';
$messages['folderdeleted'] = 'Kansio poistettu onnistuneesti';
$messages['foldersubscribed'] = 'Kansio tilattu onnistuneesti.';
$messages['folderunsubscribed'] = 'Kansion tilaus poistettu onnistuneesti.';
$messages['folderpurged'] = 'Kansio on tyhjennetty onnistuneesti.';
$messages['folderexpunged'] = 'Kansio on pakattu onnistuneesti.';
$messages['deletedsuccessfully'] = 'Poistettu onnistuneesti';
$messages['converting'] = 'Poistetaan asettelu viestistä...';
$messages['messageopenerror'] = 'Virhe kopioitaessa viestiä palvelimelta';
$messages['fileuploaderror'] = 'Tiedoston lähettäminen epäonnistui';
$messages['filesizeerror'] = 'Lähetettävä tiedosto ylittää sallitun enimmäiskoon $size';
$messages['copysuccess'] = 'Kopioitu $nr osoitetta onnistuneesti';
$messages['copyerror'] = 'Yhtään osoitetta ei voitu kopioida';
$messages['sourceisreadonly'] = 'Tämän osoitteen lähde on kirjoitussuojattu';
$messages['errorsavingcontact'] = 'Yhteystietoa ei voitu tallentaa';
$messages['movingmessage'] = 'Siirretään viestiä...';
$messages['copyingmessage'] = 'Kopioidaan viestiä...';
$messages['copyingcontact'] = 'Kopioidaan yhteystietoja...';
$messages['deletingmessage'] = 'Poistetaan viestejä...';
$messages['markingmessage'] = 'Merkitään viestejä...';
$messages['addingmember'] = 'Lisätään yhteystietoja ryhmään...';
$messages['removingmember'] = 'Poistetaan yhteystietoja ryhmästä...';
$messages['receiptsent'] = 'Lukukuittaus lähetetty onnistuneesti';
$messages['errorsendingreceipt'] = 'Lukukuittausta ei voitu lähettää';
$messages['deleteidentityconfirm'] = 'Haluatko varmasti poistaa identiteetin?';
$messages['nodeletelastidentity'] = 'Et voi poistaa tätä identiteettiä, koska se on ainoa jäljelläoleva';
$messages['forbiddencharacter'] = 'Kansion nimessä on kiellettyjä merkkejä';
$messages['selectimportfile'] = 'Valitse lähetettävä tiedosto';
$messages['addresswriterror'] = 'Valittuun osoitekirjaan ei voi kirjoittaa';
$messages['contactaddedtogroup'] = 'Yhteystiedot lisätty ryhmään';
$messages['contactremovedfromgroup'] = 'Yhteystiedot poistettu ryhmästä';
$messages['importwait'] = 'Tuodaan, odota...';
$messages['importerror'] = 'Tuonti epäonnistui! Lähetetty tiedosto ei ole vCard-muodossa.';
$messages['importconfirm'] = '<b>$inserted yhteystietoa tuotu onnistuneesti</b>';
$messages['importconfirmskipped'] = '<b>Ohitettu $skipped  olemassa olevaa merkintää</b>';
$messages['opnotpermitted'] = 'Toiminto ei ole sallittu!';
$messages['nofromaddress'] = 'Valittu identiteetti ei sisällä sähköpostiosoitetta';
$messages['editorwarning'] = 'Vaihtaminen tekstieditoriin aiheuttaa viestin muotoilun katoamisen. Haluatko jatkaa?';
$messages['httpreceivedencrypterror'] = 'Vakava virhe asetuksissa. Ota välittömästi yhteys ylläpitoon. <b>Viestiäsi ei voida lähettää.</b>';
$messages['smtpconnerror'] = 'SMTP-virhe ($code): Palvelimelle yhdistäminen epäonnistui';
$messages['smtpautherror'] = 'SMTP-virhe ($code): Tunnistus epäonnistui';
$messages['smtpfromerror'] = 'SMTP-virhe ($code): Lähettäjän "$from" asettaminen epäonnistui ($msg)';
$messages['smtptoerror'] = 'SMTP-virhe ($code): Vastaanottajan "$to" lisääminen epäonnistui ($msg)';
$messages['smtprecipientserror'] = 'SMTP-virhe: Ei voida jäsentää vastaanottajien listaa';
$messages['smtperror'] = 'SMTP-virhe: $msg';
$messages['emailformaterror'] = 'Virheellinen sähköpostiosoite: $email';
$messages['toomanyrecipients'] = 'Liikaa vastaanottajia. Vähennä vastaanottajien määrä maksimiin $max.';
$messages['maxgroupmembersreached'] = 'Ryhmän jäsenten määrä ylittää maksimin $max';
$messages['internalerror'] = 'Ilmeni sisäinen virhe. Yritä uudelleen.';
$messages['contactdelerror'] = 'Yhteystietoja ei voitu poistaa.';
$messages['contactdeleted'] = 'Yhteystiedot poistettu onnistuneesti.';
$messages['contactrestoreerror'] = 'Poistettujen yhteystietojen palautus epäonnistui.';
$messages['contactrestored'] = 'Yhteystiedot palautettu onnistuneesti.';
$messages['groupdeleted'] = 'Ryhmä poistettu onnistuneesti.';
$messages['grouprenamed'] = 'Ryhmä nimetty uudelleen onnistuneesti.';
$messages['groupcreated'] = 'Ryhmä luotu onnistuneesti.';
$messages['savedsearchdeleted'] = 'Tallennettu haku poistettu onnistuneesti.';
$messages['savedsearchdeleteerror'] = 'Tallennetun haun poisto epäonnistui.';
$messages['savedsearchcreated'] = 'Tallennettu haku luotu onnistuneesti.';
$messages['savedsearchcreateerror'] = 'Tallennetun haun tallennus epäonnistui.';
$messages['messagedeleted'] = 'Viestit poistettu onnistuneesti.';
$messages['messagemoved'] = 'Viestit siirretty onnistuneesti.';
$messages['messagecopied'] = 'Viestit kopioitu onnistuneesti.';
$messages['messagemarked'] = 'Viestit merkitty onnistuneesti.';
$messages['autocompletechars'] = 'Kirjoita vähintään $min merkkiä  ennen automaattista täydennystä.';
$messages['autocompletemore'] = 'Täsmääviä kohteita löytyi enemmän. Syötä lisää merkkejä.';
$messages['namecannotbeempty'] = 'Nimi ei voi olla tyhjä.';
$messages['nametoolong'] = 'Nimi on liian pitkä.';
$messages['folderupdated'] = 'Kansio päivitetty onnistuneesti.';
$messages['foldercreated'] = 'Kansio luotu onnistuneesti.';
$messages['invalidimageformat'] = 'Virheellinen kuvamuoto.';
$messages['mispellingsfound'] = 'Viestissä havaittiin kielioppivirheitä.';
$messages['parentnotwritable'] = 'Kansiota ei voitu siirtää tai luoda valittuun yläkansioon. Ei käyttöoikeutta.';
$messages['messagetoobig'] = 'Viestiosa on liian suuri prosessoitavaksi.';

$messages['quotafulldeletemessagesconfirm'] = 'Quota limit reached. Do you really want to permanently delete the selected message(s)?';
