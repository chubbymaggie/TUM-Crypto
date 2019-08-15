// The following code covered under the GNU Lesser General Public License v3.
static {
    String libName = System.getProperty("jnativehook.lib.name", "JNativeHook");

    try {
        // Try to load the native library assuming the java.library.path was
        // set correctly at launch.
        System.loadLibrary(libName);
    }
    catch (UnsatisfiedLinkError linkError) {
        // Get the package name for the GlobalScreen.
        String basePackage = GlobalScreen.class.getPackage().getName().replace('.', '/');

        // Compile the resource path for the
        StringBuilder libResourcePath = new StringBuilder("/");
        libResourcePath.append(basePackage).append("/lib/");
        libResourcePath.append(NativeSystem.getFamily()).append('/');
        libResourcePath.append(NativeSystem.getArchitecture()).append('/');


        // Get what the system "thinks" the library name should be.
        String libNativeName = System.mapLibraryName(libName);
        // Hack for OS X JRE 1.6 and earlier.
        libNativeName = libNativeName.replaceAll("\\.jnilib$", "\\.dylib");

        // Slice up the library name.
        int i = libNativeName.lastIndexOf('.');
        String libNativePrefix = libNativeName.substring(0, i) + '-';
        String libNativeSuffix = libNativeName.substring(i);
        String libNativeVersion = null;

        // This may return null in some circumstances.
        InputStream libInputStream = GlobalScreen.class.getResourceAsStream(libResourcePath.toString().toLowerCase() + libNativeName);
        if (libInputStream != null) {
            try {
                // Try and load the Jar manifest as a resource stream.
                URL jarFile = GlobalScreen.class.getProtectionDomain().getCodeSource().getLocation();
                JarInputStream jarInputStream = new JarInputStream(jarFile.openStream());

                // Try and extract a version string from the Manifest.
                Manifest manifest = jarInputStream.getManifest();
                if (manifest != null) {
                    Attributes attributes = manifest.getAttributes(basePackage);

                    if (attributes != null) {
                        String version = attributes.getValue("Specification-Version");
                        String revision = attributes.getValue("Implementation-Version");

                        libNativeVersion = version + '.' + revision;
                    }
                    else {
                        Logger.getLogger(GlobalScreen.class.getPackage().getName()).warning("Invalid library manifest!\n");
                    }
                }
                else {
                    Logger.getLogger(GlobalScreen.class.getPackage().getName()).warning("Cannot find library manifest!\n");
                }
            }
            catch (IOException e) {
                Logger.getLogger(GlobalScreen.class.getPackage().getName()).severe(e.getMessage());
            }


            try {
                // The temp file for this instance of the library.
                File libFile;

                // If we were unable to extract a library version from the manifest.
                if (libNativeVersion != null) {
                    libFile = new File(System.getProperty("java.io.tmpdir"), libNativePrefix + libNativeVersion + libNativeSuffix);
                }
                else {
                    libFile = File.createTempFile(libNativePrefix, libNativeSuffix);
                }

                byte[] buffer = new byte[4 * 1024];
                int size;

                // Check and see if a copy of the native lib already exists.
                FileOutputStream libOutputStream = new FileOutputStream(libFile);

                // Setup a digest...
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                DigestInputStream digestInputStream = new DigestInputStream(libInputStream, sha1);

                // Read from the digest stream and write to the file steam.
                while ((size = digestInputStream.read(buffer)) != -1) {
                    libOutputStream.write(buffer, 0, size);
                }

                // Close all the streams.
                digestInputStream.close();
                libInputStream.close();
                libOutputStream.close();

                // Convert the digest from byte[] to hex string.
                String sha1Sum = new BigInteger(1, sha1.digest()).toString(16).toUpperCase();
                if (libNativeVersion == null) {
                    // Use the sha1 sum as a version finger print.
                    libNativeVersion = sha1Sum;

                    // Better late than never.
                    File newFile = new File(System.getProperty("java.io.tmpdir"), libNativePrefix + libNativeVersion + libNativeSuffix);
                    if (libFile.renameTo(newFile)) {
                        libFile = newFile;
                    }
                }

                // Set the library version property.
                System.setProperty("jnativehook.lib.version", libNativeVersion);

                // Load the native library.
                System.load(libFile.getPath());

                Logger.getLogger(GlobalScreen.class.getPackage().getName())
                        .info("Library extracted successfully: " + libFile.getPath() + " (0x" + sha1Sum + ").\n");
            }
            catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        else {
            Logger.getLogger(GlobalScreen.class.getPackage().getName())
                    .severe("Unable to extract the native library " + libResourcePath.toString().toLowerCase() + libNativeName + "!\n");

            throw new UnsatisfiedLinkError();
        }
    }
}
