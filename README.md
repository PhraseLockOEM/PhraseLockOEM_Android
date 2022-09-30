## PhraseLockOEM FIDO2 Implementation
PhraseLock Security Integration example for Android


This sample project is the starting point to build your own FIDO2 token. All you need is this sample code and a [Phrase-Lock FIDO2 USB-Key](https://ipoxo.com/?page_id=736). The USB-Key is nothing but hardware interface to the computer (host) where you want to use it. 
 
 *Just if you are not familiar with Phrase-Lock: Phrase-Lock USB-Keys implement functionality via HID interfaces where all the business logic is on your smartphone. Uses cases are automatically typing in a password or login without password where FIDO2 is supported.*

 The library in **/app/libs/PhraseLockOEM.aar** has all the necessary interface to implement the use cases you like to implement. The picture below gives you an overview of what the USB-Key and the library are providing.


<img style="margin:0px auto" src="https://ipoxo.com/wp_ipx/postimg/oem/OEM_Blockdiagramm.png" width="600">

