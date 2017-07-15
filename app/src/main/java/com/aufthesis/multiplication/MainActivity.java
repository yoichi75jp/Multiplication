package com.aufthesis.multiplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Locale;


public class MainActivity extends Activity implements View.OnClickListener{

    private AdView m_AdView;
    private FirebaseAnalytics m_FirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the FirebaseAnalytics instance.
        m_FirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle fireLogBundle = new Bundle();
        fireLogBundle.putString("TEST", "MyApp MainActivity.onCreate() is called.");
        MyApp.getFirebaseAnalytics().logEvent(FirebaseAnalytics.Event.APP_OPEN, fireLogBundle);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~5867720659");

        TextView versionText = (TextView)findViewById(R.id.version_txt);
        try
        {
            String sPackageName = getPackageName();
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(sPackageName, 0);
            String sVersionName = packageInfo.versionName;
            versionText.setText(getString(R.string.version, sVersionName));
        }
        catch(Exception e)
        {
            versionText.setText("");
        }

        Button button1 = (Button)findViewById(R.id.primary);
        Button button2 = (Button)findViewById(R.id.training);
        Button button3 = (Button)findViewById(R.id.mastery);
        Button button4 = (Button)findViewById(R.id.challenge_questions);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
        button4.setOnClickListener(this);

        if(Locale.getDefault().toString().equals(Locale.JAPAN.toString()))
        {
            ImageView logoImage = (ImageView)findViewById(R.id.logo);
            logoImage.setImageResource(R.drawable.timestable_logo_ja);
        }

        //バナー広告
        m_AdView = (AdView) findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_AdView.loadAd(adRequest);

        //m_AdView.setVisibility(View.INVISIBLE);
    }

    //Button押下時処理
    public void onClick(View view)
    {
        int id = view.getId();
        Intent intent;
        int requestCode;
        switch(id)
        {
            case R.id.primary:
                intent = new Intent(this, PrimaryActivity.class);
                intent.putExtra(getString(R.string.keyword1), getString(R.string.primary));
                requestCode = 1;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.training:
                intent = new Intent(this, PrimaryActivity.class);
                intent.putExtra(getString(R.string.keyword1), getString(R.string.training));
                requestCode = 2;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.mastery:
                intent = new Intent(this, PracticeActivity.class);
                intent.putExtra(getString(R.string.keyword1), getString(R.string.mastery));
                intent.putExtra(getString(R.string.keyword2), 0);
                intent.putExtra(getString(R.string.keyword3), true);
                requestCode = 3;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            case R.id.challenge_questions:
                intent = new Intent(this, PracticeActivity.class);
                intent.putExtra(getString(R.string.keyword1), getString(R.string.challenge));
                intent.putExtra(getString(R.string.keyword2), 0);
                intent.putExtra(getString(R.string.keyword3), true);
                requestCode = 3;
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                break;
            default:break;
        }
    }

    @Override
    public void onPause() {
        if (m_AdView != null) {
            m_AdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (m_AdView != null) {
            m_AdView.resume();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_settings, menu);
        //m_menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                // アニメーションの設定
                //overridePendingTransition(R.animator.slide_in_left, R.animator.slide_out_right);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy()
    {
        if (m_AdView != null) {
            m_AdView.destroy();
        }
        super.onDestroy();
        setResult(RESULT_OK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.final_title));
            dialog.setMessage(getString(R.string.final_message));
            dialog.setPositiveButton(getString(R.string.final_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    finish();
                    //moveTaskToBack(true);
                }
            });
            dialog.setNegativeButton(getString(R.string.final_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
