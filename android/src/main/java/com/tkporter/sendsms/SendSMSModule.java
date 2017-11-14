package com.tkporter.sendsms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.Callback;

public class SendSMSModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    private final ReactApplicationContext reactContext;
    private Callback callback = null;
    private static final int REQUEST_CODE = 5235;

    public SendSMSModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "SendSMS";
    }


    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //System.out.println("in module onActivityResult() request " + requestCode + " result " + resultCode);
        //canceled intent
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_CANCELED) {
            sendCallback(false, true, false);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {

    }

    public void sendCallback(Boolean completed, Boolean cancelled, Boolean error) {
        if (callback != null) {
            callback.invoke(completed, cancelled, error);
            callback = null;
        }
    }

    @ReactMethod
    public void send(ReadableMap options, final Callback callback) {
        try {
            this.callback = callback;
            //new SendSMSObserver(reactContext, this, options).start();

            String body = options.hasKey("body") ? options.getString("body") : "";
            ReadableArray recipients = options.hasKey("recipients") ? options.getArray("recipients") : null;

            Intent sendIntent;
            String recipientString = "";

            if (recipients != null) {
                //Samsung for some reason uses commas and not semicolons as a delimiter
                String separator = "; ";
                if(android.os.Build.MANUFACTURER.equalsIgnoreCase("Samsung")){
                    separator = ", ";
                }
                
                for (int i = 0; i < recipients.size(); i++) {
                    recipientString += recipients.getString(i);
                    recipientString += separator;
                }
            }
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(reactContext);

                Uri uri = Uri.parse(options.getString("attachment"));
                sendIntent = getDefaultShareIntent(uri.getLastPathSegment());
                if (defaultSmsPackageName != null) {
                    sendIntent.setPackage(defaultSmsPackageName);
                }
                if (recipientString != "") {
                    sendIntent.putExtra("address", recipientString);
                    sendIntent.putExtra("sms_body", body);

                } else {
                    sendIntent.putExtra(Intent.EXTRA_TEXT, body);

                }
                sendIntent.putExtra("exit_on_sent", true);
                reactContext.startActivityForResult(sendIntent, REQUEST_CODE, sendIntent.getExtras());
            }
        } catch (Exception e) {
            //error!
            sendCallback(false, false, true);
            throw e;
        }
    }

    private Intent getDefaultShareIntent(String fileName) {
        Uri uri = Uri.parse("content://com.mimi_stelladot.providers/img").buildUpon().appendPath(fileName).build();
        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType(reactContext.getContentResolver().getType(uri));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return shareIntent;
    }
}
