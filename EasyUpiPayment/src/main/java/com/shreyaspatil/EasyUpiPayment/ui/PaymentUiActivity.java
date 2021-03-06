package com.shreyaspatil.EasyUpiPayment.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.shreyaspatil.EasyUpiPayment.R;
import com.shreyaspatil.EasyUpiPayment.Singleton;
import com.shreyaspatil.EasyUpiPayment.model.Payment;
import com.shreyaspatil.EasyUpiPayment.model.TransactionDetails;
import java.util.HashMap;
import java.util.Map;

public final class PaymentUiActivity extends AppCompatActivity {
    private static final String TAG = "PaymentUiActivity";
    private static final int PAYMENT_REQUEST = 4400;
    private Singleton singleton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upipay);

        singleton = Singleton.getInstance();

        //Get Payment Information
        Intent intent = getIntent();
        Payment payment = (Payment) intent.getSerializableExtra("payment");

        // Set Parameters for UPI
        Uri.Builder payUri = new Uri.Builder();

        payUri.scheme("upi").authority("pay");
        payUri.appendQueryParameter("pa", payment.getVpa());
        payUri.appendQueryParameter("pn", payment.getName());

        if(payment.getPayeeMerchantCode() != null) {
            payUri.appendQueryParameter("mc", payment.getPayeeMerchantCode());
        }
        if(payment.getTxnId() != null) {
            payUri.appendQueryParameter("tid", payment.getTxnId());
        }
        if(payment.getTxnRefId() != null) {
            payUri.appendQueryParameter("tr", payment.getTxnRefId());
        }

        payUri.appendQueryParameter("tn", payment.getDescription());
        payUri.appendQueryParameter("am", payment.getAmount());
        payUri.appendQueryParameter("cu", "INR");

        Uri uri = payUri.build();

        // Set Data Intent
        Intent paymentIntent = new Intent(Intent.ACTION_VIEW);
        paymentIntent.setData(uri);

        // Show Dialog to user
        Intent appChooser = Intent.createChooser(paymentIntent, "Pay");

        // Check if app is installed or not
        if(appChooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(appChooser, PAYMENT_REQUEST);
        } else {
            Toast.makeText(this,"No UPI app found! Please Install to Proceed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PAYMENT_REQUEST) {
            if ((resultCode == RESULT_OK) || (resultCode == 11)) {
                if (data != null) {
                    String response = data.getStringExtra("response");
                    TransactionDetails transactionDetails = getTransactionDetails(response);

                    //Update Listener onTransactionCompleted()
                    callbackTransactionComplete(transactionDetails);

                    //Check if success or failed
                    if (transactionDetails.getStatus().toLowerCase().equals("success")) {
                        callbackTransactionSuccess();
                    } else {
                        callbackTransactionFailed();
                    }
                } else {
                    Log.e(TAG, "Data is null");
                    callbackTransactionFailed();
                }
            } else {
                Log.e(TAG, "Transaction Cancelled by User");
                callbackTransactionCancelled();
            }
            finish();
        }
    }

    private Map<String, String> getQueryString(String url) {
        String[] params = url.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    //Make TransactionDetails object from response string
    private TransactionDetails getTransactionDetails(String response) {
        Map<String, String> map = getQueryString(response);

        String transactionId = map.get("txnId");
        String responseCode = map.get("responseCode");
        String approvalRefNo = map.get("ApprovalRefNo");
        String status = map.get("Status");
        String transactionRefId = map.get("txnRef");

        return new TransactionDetails(transactionId, responseCode, approvalRefNo, status, transactionRefId);
    }

    private boolean isListenerRegistered() {
        return (Singleton.getInstance().isListenerRegistered());
    }

    private void callbackTransactionSuccess() {
        if (isListenerRegistered()) {
            singleton.getListener().onTransactionSuccess();
        }
    }

    private void callbackTransactionFailed() {
        if (isListenerRegistered()) {
            singleton.getListener().onTransactionFailed();
        }
    }

    private void callbackTransactionCancelled() {
        if (isListenerRegistered()) {
            singleton.getListener().onTransactionCancelled();
        }
    }

    private void callbackTransactionComplete(TransactionDetails transactionDetails) {
        if (isListenerRegistered()) {
            singleton.getListener().onTransactionCompleted(transactionDetails);
        }
    }

}
