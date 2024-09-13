package com.strajag.fake_sms;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity
{
    private String default_sms_app;
    private EditText edit_text_sender;
    private EditText edit_text_message;
    private Button button_receive;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        default_sms_app = Telephony.Sms.getDefaultSmsPackage(this);
        edit_text_sender = findViewById(R.id.edit_text_sender);
        edit_text_message = findViewById(R.id.edit_text_message);
        button_receive = findViewById(R.id.button_receive);
        button_receive.setOnClickListener(view -> set_default_sms_app());
    }

    private void set_default_sms_app()
    {
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
        {
            // check if app have permission to be default sms app
            RoleManager roleManager = getApplicationContext().getSystemService(RoleManager.class);
            assert roleManager != null;
            boolean isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS);

            if(isRoleAvailable)
            {
                // check if app is already default sms app.
                boolean isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS);

                if(!isRoleHeld)
                {
                    Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    startActivityForResult(roleRequestIntent, 1);
                }
            }
        }
        else if(!Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName()))
        {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // get result from default sms dialog pop up and check if request was successful
        if(requestCode == 1)
            if(resultCode == RESULT_OK)
                if(Telephony.Sms.getDefaultSmsPackage(this).equals(getPackageName()))
                    receive_fake_sms(edit_text_sender.getText().toString(), edit_text_message.getText().toString());
    }

    private void receive_fake_sms(String sender, String message)
    {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
        values.put(Telephony.Sms.DATE, System.currentTimeMillis());
        values.put(Telephony.Sms.ADDRESS, sender);
        values.put(Telephony.Sms.BODY, message);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getContentResolver().insert(Telephony.Sms.Sent.CONTENT_URI, values);
        else
            getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
            //context.getContentResolver().insert(Uri.parse("content://sms/sent"), values);

        // change default sms app to last default sms app
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
        {
            Intent runIntent = getPackageManager().getLaunchIntentForPackage(default_sms_app);
            startActivity(runIntent);
        }
        else
        {
            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, default_sms_app);
            startActivity(intent);
        }
    }
}
