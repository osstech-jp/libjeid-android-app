package jp.co.osstech.jeidreader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity
    extends BaseActivity
    implements View.OnClickListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);
        setTitle(getTitle() + " " + BuildConfig.VERSION_NAME);
        findViewById(R.id.in_menu_button).setOnClickListener(this);
        findViewById(R.id.dl_reader_button).setOnClickListener(this);
        findViewById(R.id.ep_reader_button).setOnClickListener(this);
        findViewById(R.id.rc_reader_button).setOnClickListener(this);
        findViewById(R.id.pinstatus_button).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch (view.getId()) {
        case R.id.dl_reader_button:
            intent = new Intent(getApplication(), DLReaderActivity.class);
            startActivity(intent);
            break;
        case R.id.in_menu_button:
            intent = new Intent(getApplication(), INMenuActivity.class);
            startActivity(intent);
            break;
        case R.id.ep_reader_button:
            intent = new Intent(getApplication(), EPReaderActivity.class);
            startActivity(intent);
            break;
        case R.id.rc_reader_button:
            intent = new Intent(getApplication(), RCReaderActivity.class);
            startActivity(intent);
            break;
        case R.id.pinstatus_button:
            intent = new Intent(getApplication(), PinStatusActivity.class);
            startActivity(intent);
            break;
        }
    }

}
