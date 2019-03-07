package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import jp.co.osstech.libjeid.JPKIAP;

public class INMenuActivity
    extends BaseActivity
    implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_in_menu);
        super.onCreate(savedInstanceState);

        findViewById(R.id.cardinfo_button).setOnClickListener(this);
        findViewById(R.id.selectcert_button).setOnClickListener(this);
        findViewById(R.id.sign_jpki_auth_button).setOnClickListener(this);
        findViewById(R.id.sign_jpki_sign_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
        case R.id.cardinfo_button:
            intent = new Intent(getApplication(), INReaderActivity.class);
            startActivity(intent);
            break;
        case R.id.selectcert_button:
            intent = new Intent(getApplication(), SelectCertActivity.class);
            startActivity(intent);
            break;
        case R.id.sign_jpki_auth_button:
            intent = new Intent(getApplication(), SignActivity.class);
            intent.putExtra("TYPE", JPKIAP.TYPE_AUTH);
            startActivity(intent);
            break;
        case R.id.sign_jpki_sign_button:
            intent = new Intent(getApplication(), SignActivity.class);
            intent.putExtra("TYPE", JPKIAP.TYPE_SIGN);
            startActivity(intent);
            break;
        }
    }
}
