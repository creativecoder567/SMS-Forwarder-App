package com.esecforte.smsforwarder.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.esecforte.smsforwarder.data.AppPref;
import com.esecforte.smsforwarder.model.SMSForwardEntry;
import com.esecforte.smsforwarder.services.SmsSenderService;
import com.esecforte.smsforwarder.utils.AppUtils;

import java.util.HashSet;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {


    private static final String TAG = SmsReceiver.class.getSimpleName();
    private String sender;
    private String message;

    public void onReceive(Context context, Intent intent) {
        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();
        try {
            if (bundle != null) {
                // get sms objects
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus.length == 0) {
                    return;
                }
                // large message might be broken into many
                SmsMessage[] messages = new SmsMessage[pdus.length];
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < pdus.length; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    sb.append(messages[i].getMessageBody());
                }
                sender = messages[0].getOriginatingAddress();
                message = sb.toString();
            }
        } catch (Exception e) {
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        if (!TextUtils.isEmpty(sender) && !TextUtils.isEmpty(message)) {
            AppPref appPref = AppPref.getInstance(context);
            List<SMSForwardEntry> datas = AppUtils.getFormattedSmsEntry(appPref.getString(AppPref.USER_PH_NO_DATA));
            HashSet<String> forWardNos = new HashSet<>();

            for (SMSForwardEntry entry : datas) {
                if (!entry.isEnabled())
                    break;
                List<String> smsNos = entry.getSmsNumbers();
                for (String sms : smsNos) {
                    if (sms.equalsIgnoreCase(sender)) {
                        forWardNos.addAll(entry.getForwardNumbers());
                        break;
                    }
                }
            }
            if (!forWardNos.isEmpty()) {
                Intent intent1 = new Intent(context, SmsSenderService.class);
                intent1.putExtra(SmsSenderService.SMS_FORWARD_NOS, forWardNos.toArray(new String[0]));
                intent1.putExtra(SmsSenderService.SMS_SENDER, sender);
                intent1.putExtra(SmsSenderService.SMS_BODY, message);
                context.startService(intent1);
            }

        }

    }

}