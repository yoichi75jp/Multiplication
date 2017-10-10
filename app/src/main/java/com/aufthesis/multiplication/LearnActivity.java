package com.aufthesis.multiplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

/**
 // Created by yoichi75jp2 on 2016/08/13.
 */
public class LearnActivity extends Activity implements View.OnClickListener {

    private Context m_context;

    private final int MP = LinearLayout.LayoutParams.MATCH_PARENT;
    private final int WC = LinearLayout.LayoutParams.WRAP_CONTENT;

    private int m_maxRow = 9;

    private int m_targetRow;
    private int m_rowSize;
    private int m_columnSize;

    private final int m_defaultColor = Color.parseColor("#D3D3D3"); // LightGrey
    private final int m_onClickColor = Color.parseColor("#FFFFE0"); // LightYellow

    private TextView m_answerText;

    // 効果音用
    private SoundPool m_soundPool;
    private int m_SoundID;

    private AdView m_AdView;
    private static InterstitialAd m_InterstitialAd;

    private SharedPreferences m_prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);

        m_context = this;

        Intent intent = getIntent();
        m_targetRow = intent.getExtras().getInt(getString(R.string.keyword2));

        m_answerText = findViewById(R.id.answer);
        m_answerText.setTypeface(Typeface.DEFAULT_BOLD);

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        m_columnSize = m_maxRow == 20 ? 4 : 3;
        m_rowSize = m_maxRow == 9 ? 3 : m_maxRow == 12 ? 4 : 5;
        int count = 0;
        LinearLayout layout = findViewById(R.id.button_placement);
        for(int row = 1; row <= m_rowSize; row++)
        {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(MP, WC));
            for(int column = 1; column <= m_columnSize; column++)
            {
                count++;
                Button button = new Button(this);
                LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(WC, WC);
                param.weight = 1.0f;
                button.setLayoutParams(param);
                button.setText(getString(R.string.times, m_targetRow, count));
                button.setTextSize(40);

                ViewGroup.LayoutParams lp = button.getLayoutParams();
                ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams)lp;
                mlp.setMargins(10, 10, 10, 10);
                button.setLayoutParams(mlp);

                button.setId(count);
                rowLayout.addView(button);
                button.setOnClickListener(this);
                button.setBackgroundColor(m_defaultColor);
            }
            layout.addView(rowLayout);
        }

        ImageView backImage = findViewById(R.id.back);
        backImage.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        if(m_targetRow > 1)
                        {
                            m_targetRow--;
                            setFormula();
                            m_answerText.setText("");
                        }
                    }
                }
        );
        ImageView forwardImage = findViewById(R.id.forward);
        forwardImage.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        if(m_targetRow < m_maxRow)
                        {
                            m_targetRow++;
                            setFormula();
                            m_answerText.setText("");
                        }
                    }
                }
        );
        ImageView mouthImage = findViewById(R.id.mouth);
        mouthImage.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        m_answerText.setText("");
                        m_soundPool.play(m_SoundID, 1.0F, 1.0F, 0, 0, 1.0F);
                        clearButtonColor();
                    }
                }
        );

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~5867720659");

        //バナー広告
        m_AdView = (AdView) findViewById(R.id.adView4);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_AdView.loadAd(adRequest);

        //m_AdView.setVisibility(View.INVISIBLE);

        // AdMobインターステイシャル
        m_InterstitialAd = new InterstitialAd(this);
        m_InterstitialAd.setAdUnitId(getString(R.string.adUnitInterId));
        m_InterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (m_InterstitialAd.isLoaded()) {
                    m_InterstitialAd.show();
                }
            }
        });

    }

    private void setFormula()
    {
        int count = 0;
        for(int row = 1; row <= m_rowSize; row++)
        {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(MP, WC));
            for(int column = 1; column <= m_columnSize; column++)
            {
                count++;
                Button button = findViewById(count);
                if(button != null)
                {
                    button.setText(getString(R.string.times, m_targetRow, count));
                    button.setBackgroundColor(m_defaultColor);
                }
            }
        }

    }

    //Button押下時処理
    public void onClick(View view)
    {
        this.clearButtonColor();
        Button button = (Button)view;
        if(button != null)
        {
            String formula = button.getText().toString();
            String[] vals = formula.split(getString(R.string.times_sign));
            try
            {
                int num1 = Integer.parseInt(vals[0]);
                int num2 = Integer.parseInt(vals[1]);
                int answer = num1 * num2;
                m_answerText.setText(String.valueOf(answer));
                button.setBackgroundColor(m_onClickColor);
            }
            catch(Exception ex){}
        }
    }

    private void clearButtonColor()
    {
        int count = 0;
        for(int row = 1; row <= m_rowSize; row++)
        {
            for(int column = 1; column <= m_columnSize; column++)
            {
                count++;
                Button button = findViewById(count);
                if(button != null)
                    button.setBackgroundColor(m_defaultColor);
            }
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

        // 予め音声データを読み込む
        m_soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        m_SoundID = m_soundPool.load(getApplicationContext(), R.raw.hito_ge_paku01, 0);

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            int count = m_prefs.getInt(getString(R.string.count_learn), 0);
            count++;
            if(count >= 3)
            {
                count = 0;
                AdRequest adRequest = new AdRequest.Builder().build();
                m_InterstitialAd.loadAd(adRequest);
                if (m_InterstitialAd.isLoaded()) {
                    m_InterstitialAd.show();
                }
            }
            SharedPreferences.Editor editor = m_prefs.edit();
            editor.putInt(getString(R.string.count_learn), count);
            editor.apply();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
