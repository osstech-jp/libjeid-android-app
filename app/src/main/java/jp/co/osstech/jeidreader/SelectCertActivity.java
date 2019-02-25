package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class SelectCertActivity
    extends BaseActivity
    implements View.OnClickListener
{

    public static final String TAG = "JeidReader";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, getClass().getSimpleName() +
              "#onCreate(" + savedInstanceState + ")");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_cert);

        findViewById(R.id.show_auth_cert_button).setOnClickListener(this);
        findViewById(R.id.show_sign_cert_button).setOnClickListener(this);
        findViewById(R.id.show_auth_ca_cert_button).setOnClickListener(this);
        findViewById(R.id.show_sign_ca_cert_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;

        switch (view.getId()) {
        case R.id.show_auth_cert_button:
            intent = new Intent(getApplication(), ShowCertActivity.class);
            intent.putExtra("TYPE", "AUTH");
            startActivity(intent);
            break;
        case R.id.show_sign_cert_button:
            intent = new Intent(getApplication(), ShowCertActivity.class);
            intent.putExtra("TYPE", "SIGN");
            startActivity(intent);
            break;
        case R.id.show_auth_ca_cert_button:
            intent = new Intent(getApplication(), ShowCertActivity.class);
            intent.putExtra("TYPE", "AUTH_CA");
            startActivity(intent);
            break;
        case R.id.show_sign_ca_cert_button:
            intent = new Intent(getApplication(), ShowCertActivity.class);
            intent.putExtra("TYPE", "SIGN_CA");
            startActivity(intent);
            break;
        }
    }
}
