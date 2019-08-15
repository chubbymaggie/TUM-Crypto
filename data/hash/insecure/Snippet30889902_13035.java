public class UploadService extends IntentService {

    public static final String ARG_FILE_PATH = "file_path";
    public static final String UPLOAD_STATE_CHANGED_ACTION = "com.readystatesoftware.simpl3r.example.UPLOAD_STATE_CHANGED_ACTION";
    public static final String UPLOAD_CANCELLED_ACTION = "com.readystatesoftware.simpl3r.example.UPLOAD_CANCELLED_ACTION";
    public static final String S3KEY_EXTRA = "s3key";
    public static final String PERCENT_EXTRA = "percent";
    public static final String MSG_EXTRA = "msg";

    private static final int NOTIFY_ID_UPLOAD = 1337;

    private AmazonS3Client s3Client;
    private Uploader uploader;

    private NotificationManager nm;

    public UploadService() {
        super("simpl3r-example-upload");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(getString(R.string.s3_access_key), getString(R.string.s3_secret)));
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        IntentFilter f = new IntentFilter();
        f.addAction(UPLOAD_CANCELLED_ACTION);
        registerReceiver(uploadCancelReceiver, f);
    }   

    @Override
    protected void onHandleIntent(Intent intent) {


        String filePath = intent.getStringExtra(ARG_FILE_PATH);
        File fileToUpload = new File(filePath);
        final String s3ObjectKey = md5(filePath);
        String s3BucketName = getString(R.string.s3_bucket);

        final String msg = "Uploading " + s3ObjectKey + "...";

        // create a new uploader for this file
        uploader = new Uploader(this, s3Client, s3BucketName, s3ObjectKey, fileToUpload);

    /*  File theFile = new File(filePath);
         PutObjectRequest putObjectRequest = new PutObjectRequest(amazonFileUploadLocationOriginal, keyName, theFile);
         putObjectRequest.withMetadata(objectMetadata);*/


        // listen for progress updates and broadcast/notify them appropriately
        uploader.setProgressListener(new UploadProgressListener() {         
            @Override
            public void progressChanged(ProgressEvent progressEvent,
                    long bytesUploaded, int percentUploaded) {

                Notification notification = buildNotification(msg, percentUploaded);
                nm.notify(NOTIFY_ID_UPLOAD, notification);
                broadcastState(s3ObjectKey, percentUploaded, msg);
            }
        });

        // broadcast/notify that our upload is starting
        Notification notification = buildNotification(msg, 0);
        nm.notify(NOTIFY_ID_UPLOAD, notification);
        broadcastState(s3ObjectKey, 0, msg);

        try {
            String s3Location = uploader.start(); // initiate the upload
            broadcastState(s3ObjectKey, -1, "File successfully uploaded to " + s3Location);
        } catch (UploadIterruptedException uie) {
            broadcastState(s3ObjectKey, -1, "User interrupted");
        } catch (Exception e) {
            e.printStackTrace();
            broadcastState(s3ObjectKey, -1, "Error: " + e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        nm.cancel(NOTIFY_ID_UPLOAD);
        unregisterReceiver(uploadCancelReceiver);
        super.onDestroy();
    }

    private void broadcastState(String s3key, int percent, String msg) {
        Intent intent = new Intent(UPLOAD_STATE_CHANGED_ACTION);
        Bundle b = new Bundle();
        b.putString(S3KEY_EXTRA, s3key);
        b.putInt(PERCENT_EXTRA, percent);
        b.putString(MSG_EXTRA, msg);
        intent.putExtras(b);
        sendBroadcast(intent);
    }

    private Notification buildNotification(String msg, int progress) {  
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(msg);
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(msg);
        builder.setSmallIcon(R.drawable.ic_stat_uploading);
        builder.setOngoing(true);
        builder.setProgress(100, progress, false);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(contentIntent);

        return builder.build();
    }

    private BroadcastReceiver uploadCancelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (uploader != null) {
                uploader.interrupt();
            }
        }
    };

    private String md5(String s) {
        try {
            // create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i=0; i<messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

}
