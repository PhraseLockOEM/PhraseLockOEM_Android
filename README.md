## PhraseLockOEM FIDO2 Implementation
PhraseLock Security Integration example for Android


This sample project is the starting point to build your own FIDO2 token. All you need is this sample code and a [Phrase-Lock FIDO2 USB-Key](https://ipoxo.com/?page_id=736). The USB-Key is nothing but hardware interface to the computer (host) where you want to use it. 
 
 *Just if you are not familiar with Phrase-Lock: Phrase-Lock USB-Keys implement functionality via HID interfaces where all the business logic is on your smartphone. Uses cases are automatically typing in a password or login without password where FIDO2 is supported.*

 The library in **/app/libs/PhraseLockOEM.aar** has all the necessary interface to implement the use cases you like to implement. The picture below gives you an overview of what the USB-Key and the library are providing.


<img style="margin:0px auto" src="https://ipoxo.com/wp_ipx/postimg/oem/OEM_Blockdiagramm.png" width="600">



<br/><br/>
###Login with the Phrase-Lock Demo App into a Microsoft account
This video shows how to login into a Microsoft account with the FIDO2 compatible Phrase-Lock Demo app and the USB-Key that does the communication to the operating system.
You can use your own ID (AAGUID), your own root certificate and your own client certificate. And you can choose a PIN-code length as long as you want because it is entered by your app. You don't have to do it manually. This is how security and freedom sounds an smells like! 
<br/>
<br/>
[![Watch the video](https://ipoxo.com/video/PhraseLock2.jpg)](https://ipoxo.com/video/PhraseLock2.mp4)


<br/><br/>
###Login with Phrase-Lock Demo App into Windows 11 via AzureAD and Windows Hello
This is for all CIO: If you want to get rid of passwords, the Phrase-Lock library is for you. Usually hardware token must be carefully protected against misuse. No Problem with Phrase-Lock, just let the USB-Key plugged in on the PC or Laptop, because there are absolutely no interesting data on it. So it can't get lost easely - which minimises the replacement logistics tremendously. 
Every ID, certificate or identification is stored in your app. Or you prefer to keep them save in the backend and to provide credentials only if they are required, limited by time and location if you like. You have any freedom to build a secure __*user-authentication-infrastructure*__ the way you want, the way you need to protect your business.
Microsoft AzureAD supports filtering of FIDO2-token via there unique ID (AAGUID). So just go ahead and limit the type of token you like to accept. 
You also have the freedom to use as many token-IDs as you want. Its up to you to assign just a single identity to a user/smartphone for all purposes, or to assign different token-IDs for different accounts or use-cases. You will not find any competitive product that supports such a wide range of possibilities to tailer a lean solution.
<br/>
<br/>

[![Watch the video](https://ipoxo.com/video/PhraseLock1.jpg)](https://ipoxo.com/video/PhraseLock1.mp4)
