BASE64Encoder base64 = new BASE64Encoder();
PasswordDeriveBytes i_Pass = new PasswordDeriveBytes(passWord, saltWordAsBytes);
byte[] keyBytes = i_Pass.getBytes(24);
byte[] ivBytes = i_Pass.getBytes(8);
Cipher c3des = Cipher.getInstance("DESede/CBC/PKCS5Padding");
SecretKeySpec myKey = new SecretKeySpec(keyBytes, "DESede");
IvParameterSpec ivspec = new IvParameterSpec(ivBytes);
c3des.init (Cipher.ENCRYPT_MODE, myKey, ivspec);
encrytpedTextAsByte  = c3des.doFinal(plainTextAsBytes);
encryptedText  = base64.encode(encrytpedTextAsByte);