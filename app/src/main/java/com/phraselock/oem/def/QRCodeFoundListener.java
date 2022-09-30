package com.phraselock.oem.def;

public interface QRCodeFoundListener {
	void onQRCodeFound(String qrCode);
	void qrCodeNotFound();
}