KeyFactory keyFactory = KeyFactory.getInstance("RSA");
byte[] privKeyBytes = loadPriavteKeyFromFile(fileName, new String(txtPassword.getPassword()));
PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate(privSpec);
Cipher rsaCipher = Cipher.getInstance("RSA");
rsaCipher.init(Cipher.ENCRYPT_MODE, privKey);
byte[] ciphertext = null;
ciphertext = rsaCipher.doFinal(xmlToSign.getBytes());
String urlString = "http://localhost:3290/SignApplet.aspx";
String senddata  = Base64.encodeBase64String(ciphertext);
doHttpUrlConnectionAction(urlString,senddata.getBytes());
JOptionPane.showMessageDialog(this, "XML successfully signed and sent to server.");