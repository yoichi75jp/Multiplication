package com.aufthesis.multiplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


/**
 // Created by yoichi75jp2 on 2016/08/07.
 */
public class PrimaryActivity extends Activity implements CompoundButton.OnCheckedChangeListener {

    private Context m_context;
    private String m_entryMode;
    private boolean m_isRandom = false;

    private AdView m_AdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_primary);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~5867720659");

        int maxRow = 9;
        m_context = this;

        Intent intent = getIntent();
        m_entryMode = intent.getExtras().getString(getString(R.string.keyword1));

        TextView title = findViewById(R.id.title);

        Switch randomSwitch = findViewById(R.id.isRandom);
        if(m_entryMode.equals(getString(R.string.primary)))
        {
            title.setText(getString(R.string.learn));
            randomSwitch.setVisibility(View.INVISIBLE);
        }
        else
        {
            title.setText(getString(R.string.practice));
            randomSwitch.setOnCheckedChangeListener(this);
            ImageView modeImage = findViewById(R.id.mode_image);
            modeImage.setImageResource(R.drawable.skills1);
        }

        // ListViewの設定
        String[] items = new String[maxRow];
        for(int i = 1; i <= maxRow; i++)
        {
            items[i-1] = getString(R.string.timestables, i);
        }

        ListView timesTablesListView =findViewById(R.id.list_TimesTables);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items)
                {
                    @NonNull
                    @Override
                     public View getView(int position, View convertView, ViewGroup parent) {
                        TextView view = (TextView)super.getView(position, convertView, parent);
                        view.setTextSize(45);
                        view.setTypeface(Typeface.DEFAULT_BOLD);
                        return view;
                    }
                };

        timesTablesListView.setAdapter(adapter);
        timesTablesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            //@SuppressWarnings("unchecked")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                if(m_entryMode.equals(getString(R.string.primary)))
                {
                    intent = new Intent(m_context, LearnActivity.class);
                    intent.putExtra(getString(R.string.keyword2), position+1);
                    int requestCode = 1;
                    startActivityForResult(intent, requestCode);
                    // アニメーションの設定
                    overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                }
                else
                {
                    intent = new Intent(m_context, PracticeActivity.class);
                    intent.putExtra(getString(R.string.keyword1), m_entryMode);
                    intent.putExtra(getString(R.string.keyword2), position+1);
                    intent.putExtra(getString(R.string.keyword3), m_isRandom);
                    int requestCode = 2;
                    startActivityForResult(intent, requestCode);
                    // アニメーションの設定
                    overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                }
            }
        });

        //バナー広告
        m_AdView = findViewById(R.id.adView3);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_AdView.loadAd(adRequest);

        //m_AdView.setVisibility(View.INVISIBLE);
    }

    //Switch切替時処理
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        m_isRandom = isChecked;
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
        }
    }

}
