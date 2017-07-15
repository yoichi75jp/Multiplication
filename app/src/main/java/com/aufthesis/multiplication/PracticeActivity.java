package com.aufthesis.multiplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.os.Handler;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;

/**
// Created by yoichi75jp2 on 2016/08/07.
 */
public class PracticeActivity extends Activity implements View.OnClickListener, Runnable {

    private Context m_context;

    private String m_entryMode;
    private int m_targetRow;
    private boolean m_isRandom = false;
    private List<Pair<Integer,Integer>> m_listID = new ArrayList<>();
    private List<Pair<Integer,Integer>> m_listFormula = new ArrayList<>();
    private List<Pair<Pair<Integer,Integer>,Boolean>> m_listResult = new ArrayList<>();
    private TextView m_num1Text = null;
    private TextView m_num2Text = null;
    private TextView m_answerText = null;
    private TextView m_timerText = null;
    private TextView m_correctCountText = null;
    private TextView m_wrongCountText = null;
    private TextView m_remCountText = null;

    private LinearLayout m_correctLayout = null;
    private LinearLayout m_wrongLayout = null;

    private int m_count = 0;
    private int m_correctSize = 0;

    private long m_startTime;
    private long m_pauseTime;
    private long m_diffTime;
    private SimpleDateFormat m_dataFormat = new SimpleDateFormat("mm:ss.SSS", Locale.getDefault());

    private Thread m_thread = null;
    private final Handler m_handler = new Handler();
    private volatile boolean m_stopRun = false;

    // 効果音用
    final int SOUND_POOL_MAX = 6;
    private SoundPool m_soundPool;
    private int m_correctSound;
    private int m_incorrectSound;
    private int m_clearSoundID;
    private int m_failedSoundID;
    private int m_laughSoundID;
    private int m_cheer2SoundID;

    // 10 m_sec order
    private int m_period = 10;

    static public boolean m_isShortMode = false;

    private SQLiteDatabase m_db;

    private static InterstitialAd m_InterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        m_context = this;

        DBOpenHelper dbHelper = new DBOpenHelper(this);
        m_db = dbHelper.getDataBase();

        Intent intent = getIntent();
        m_entryMode = intent.getExtras().getString(getString(R.string.keyword1));
        m_targetRow = intent.getExtras().getInt(getString(R.string.keyword2));
        m_isRandom = intent.getExtras().getBoolean(getString(R.string.keyword3));

        // Masteryのときの処理
        if(m_entryMode.equals(getString(R.string.mastery)))
        {
            ImageView image = (ImageView)findViewById(R.id.mode_image);
            image.setImageResource(R.drawable.mastery_img);
        }

        if(m_listID.size() == 0)
        {
            m_listID.add(new Pair<>(R.id.one,1));
            m_listID.add(new Pair<>(R.id.two,2));
            m_listID.add(new Pair<>(R.id.three,3));
            m_listID.add(new Pair<>(R.id.four,4));
            m_listID.add(new Pair<>(R.id.five,5));
            m_listID.add(new Pair<>(R.id.six,6));
            m_listID.add(new Pair<>(R.id.seven,7));
            m_listID.add(new Pair<>(R.id.eight,8));
            m_listID.add(new Pair<>(R.id.nine,9));
            m_listID.add(new Pair<>(R.id.zero,0));
            for(int i = 0; i < m_listID.size(); i++)
            {
                Button button = (Button)findViewById(m_listID.get(i).first);
                ColorStateList colorList = button.getTextColors();
                final int defaultColor = colorList.getDefaultColor();
                button.setOnClickListener(this);
                button.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        TextView textView = (TextView)view;
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            textView.setTextColor(Color.parseColor("#0000FF"));
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            //textView.setTextColor(Color.parseColor("#000000"));
                            textView.setTextColor(defaultColor);
                        }
                        return false;
                    }
                });
            }
        }
        Button button = (Button)findViewById(R.id.back);
        button.setOnClickListener(this);
        button = (Button)findViewById(R.id.forward);
        button.setOnClickListener(this);
        button.setVisibility(View.INVISIBLE);   // Hardモードで解禁?
        button = (Button)findViewById(R.id.ok);
        button.setOnClickListener(this);
        button.setTypeface(Typeface.DEFAULT_BOLD);

        m_answerText = (TextView)findViewById(R.id.answer);
        m_num1Text = (TextView)findViewById(R.id.num1);
        m_num2Text = (TextView)findViewById(R.id.num2);
        m_timerText = (TextView)findViewById(R.id.count_time);
        m_timerText.setText(m_dataFormat.format(0));

        m_correctCountText = (TextView)findViewById(R.id.correct_count);
        m_correctCountText.setText(String.valueOf(0));
        m_wrongCountText = (TextView)findViewById(R.id.wrong_count);
        m_wrongCountText.setText(String.valueOf(0));

        m_correctLayout = (LinearLayout)findViewById(R.id.correct_layout);
        m_wrongLayout = (LinearLayout)findViewById(R.id.wrong_layout);
        m_correctLayout.setVisibility(View.INVISIBLE);
        m_wrongLayout.setVisibility(View.INVISIBLE);

        m_remCountText = (TextView)findViewById(R.id.count_remaining);

        //計算式リスト作成
        this.createFormulaList();

        // Challenge10のときの処理
        if(m_entryMode.equals(getString(R.string.challenge)))
            this.setChallengeMode();

        m_stopRun = false;
        m_thread = new Thread(this);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~5867720659");

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

        /**/
        // 体得のときの処理
        if(m_entryMode.equals(getString(R.string.mastery)))
        {
            final String item_list[] = new String[] {getString(R.string.full_mode), getString(R.string.short_mode)};

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.select_title));
            dialog.setIcon(android.R.drawable.ic_dialog_info);
            dialog.setSingleChoiceItems(item_list, 0, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    //⇒アイテムを選択した時のイベント処理
                    Toast.makeText(m_context,
                            getString(R.string.selected, item_list[whichButton]),
                            Toast.LENGTH_SHORT).show();
                    if(whichButton == 0)
                        createFormulaList();
                    else if(whichButton == 1)
                        setShortMode();
                }
            });
            dialog.setPositiveButton(getString(R.string.select_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    startDialog();
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }/**/
        else
            startDialog();

    }

    //開始時のダイアログ
    private void startDialog()
    {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.start_title));
        dialog.setMessage(getString(R.string.start_message, m_entryMode, m_listFormula.size()));
        dialog.setPositiveButton(getString(R.string.start_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                m_thread.start();
                m_startTime = System.currentTimeMillis();
            }
        });
        dialog.setNegativeButton(getString(R.string.start_cancel),new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    // Challenge10のときの設定
    private void setChallengeMode()
    {
        // Imageを切り替える
        ImageView image = (ImageView)findViewById(R.id.mode_image);
        image.setImageResource(R.drawable.muscles_image);

        // 計算式を10問にする
        List<Pair<Integer,Integer>> tmp_listFormula = new ArrayList<>();
        for(int i = 0; i < m_listFormula.size(); i++)
        {
            if(m_listFormula.get(i).first*m_listFormula.get(i).second >= 10)
                tmp_listFormula.add(m_listFormula.get(i));

            if(tmp_listFormula.size() == 10)
            {
                m_listFormula = tmp_listFormula;
                break;
            }
        }

        // TextのBackgroundの切替
        int numColor = m_num1Text.getDrawingCacheBackgroundColor();
        m_answerText.setBackgroundColor(numColor);
        m_num1Text.setBackgroundColor(Color.parseColor("#FFE4E1"));
        m_num2Text.setBackgroundColor(Color.parseColor("#FFE4E1"));

        // Button制御
        Button button = (Button)findViewById(R.id.ok);
        button.setVisibility(View.INVISIBLE);
    }

    //ShortMode時の計算式の設定
    private void setShortMode()
    {
        // 計算式から1の段と重複を除く
        List<Pair<Integer,Integer>> tmp_listFormula = new ArrayList<>();
        for(int i = 0; i < m_listFormula.size(); i++)
        {
            int first = m_listFormula.get(i).first;
            int second = m_listFormula.get(i).second;
            //1の段除外
            if(first == 1 || second == 1)
                continue;
            //重複除外
            if(tmp_listFormula.contains(new Pair<>(second, first)))
                continue;

            tmp_listFormula.add(m_listFormula.get(i));
        }
        m_listFormula = tmp_listFormula;
        m_remCountText.setText(getString(R.string.remaining, m_listFormula.size()));

        //計算式リスト作成
        createFormula();
        m_isShortMode = true;
    }

    //Button押下時処理
    public void onClick(View view)
    {
        int id = view.getId();

        String value = m_answerText.getText().toString();
        String num1 = m_num1Text.getText().toString();
        String num2 = m_num2Text.getText().toString();
        if(m_listFormula.size() <= m_count)
        {
            this.endAlert();
            return;
        }

        if(id == R.id.ok)
        {
            this.checkAnswer(num1, num2, value);
        }
        else if(id == R.id.back)
        {
            // Challenge10のときの処理
            if(m_entryMode.equals(getString(R.string.challenge)))
            {
                if(!num1.equals(""))
                    m_num1Text.setText("");
            }
            else if(value.length() != 0)
                m_answerText.setText(value.substring(0, value.length()-1));
        }
        else if(id == R.id.forward){}   // Hardモードで解禁?
        else
        {
            // Challenge10のときの処理
            if(m_entryMode.equals(getString(R.string.challenge)))
            {
                for(int i = 0; i < m_listID.size(); i++)
                {
                    Pair<Integer,Integer> pairID = m_listID.get(i);
                    if(id == pairID.first)
                    {
                        String val = String.valueOf(pairID.second);
                        if(num1.equals(""))
                            m_num1Text.setText(val);
                        else if(num2.equals(""))
                        {
                            m_num2Text.setText(val);
                            this.checkAnswer(num1, val, value);
                        }
                        break;
                    }
                }
            }
            else
            {
                // 3桁より大きい数は対象外とする
                if(value.length() == 3) return;

                for(int i = 0; i < m_listID.size(); i++)
                {
                    Pair<Integer,Integer> pairID = m_listID.get(i);
                    if(id == pairID.first)
                    {
                        String answer = value + String.valueOf(pairID.second);
                        m_answerText.setText(answer);
                        break;
                    }
                }
            }
        }
    }

    // 計算式の答えを確認する
    private void checkAnswer(String num1, String num2, String value)
    {
        try
        {
            m_count++;
            m_remCountText.setText(getString(R.string.remaining, m_listFormula.size() - m_count));

            int val1 = Integer.parseInt(num1);
            int val2 = Integer.parseInt(num2);
            int answer = Integer.parseInt(value);
            Pair<Integer,Integer> pair = new Pair<>(val1, val2);
            boolean result = answer == val1 * val2;

            String countVal;
            int countNum;
            if(result)// 正解
            {
                m_correctLayout.setVisibility(View.VISIBLE);
                countVal = m_correctCountText.getText().toString();
                try
                {
                    countNum = Integer.parseInt(countVal);
                    countNum++;
                    m_correctCountText.setText(String.valueOf(countNum));
                    m_soundPool.play(m_correctSound, 1.0F, 1.0F, 0, 0, 1.0F);
                }
                catch (Exception ex){}
            }
            else// 間違い
            {
                m_wrongLayout.setVisibility(View.VISIBLE);
                countVal = m_wrongCountText.getText().toString();
                try
                {
                    countNum = Integer.parseInt(countVal);
                    countNum++;
                    m_wrongCountText.setText(String.valueOf(countNum));
                    m_soundPool.play(m_incorrectSound, 1.0F, 1.0F, 0, 0, 1.0F);
                }
                catch (Exception ex){}
            }
            m_listResult.add(new Pair<>(pair, result));
            if(m_listFormula.size() > m_count)
                createFormula();
            else
            {
                // 終わり
                Thread.sleep(500);
                m_stopRun = true;

                m_answerText.setText("");
                //int correctSize = 0;
                for(int i = 0; i < m_listResult.size(); i++)
                {
                    if(m_listResult.get(i).second) m_correctSize++;
                }

                int resultTime = this.result(m_correctSize, m_count);

                this.endAlert();
            }
        }
        catch(Exception ex){}
    }

    //最後のダイアログ
    private void endAlert()
    {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.end_title));
        dialog.setMessage(getString(R.string.end_message));
        dialog.setPositiveButton(getString(R.string.end_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                    /**/
                Intent intent = new Intent(m_context, ResultActivity.class);
                int requestCode = 1;
                if(m_entryMode.equals(getString(R.string.challenge)))
                    requestCode = 2;

                intent.putExtra(getString(R.string.keyword1), m_entryMode);
                intent.putExtra(getString(R.string.keyword4), m_correctSize);
                intent.putExtra(getString(R.string.keyword5), m_count);
                intent.putExtra(getString(R.string.keyword6), m_diffTime);
                intent.putExtra(getString(R.string.keyword7), getCheckList());
                startActivityForResult(intent, requestCode);
                // アニメーションの設定
                overridePendingTransition(R.animator.slide_in_right, R.animator.slide_out_left);
                     /**/
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    // 間違った計算のリストを返す
    private ArrayList<String> getCheckList()
    {
        ArrayList<String> checkList = new ArrayList<>();
        for(int i = 0; i < m_listResult.size(); i++)
        {
            Pair<Pair<Integer,Integer>,Boolean> item = m_listResult.get(i);
            if(!item.second)
                checkList.add(getString(R.string.times, item.first.first, item.first.second));
        }
        return checkList;
    }

    private int result(int correctSize, int questionSize)
    {
        int returnCode = 0;
        long lAnswerTime = m_diffTime/(long)questionSize;

        double per_sec = (double)lAnswerTime/1000d;
        String sql;
        /**/
        if(correctSize == questionSize)
        {
            sql = "select Min(TimeLevel) from time_level where per_second < " + String.valueOf(per_sec);
            Cursor cursor = m_db.rawQuery(sql, null);
            cursor.moveToFirst();
            if (cursor.getCount() != 0)
            {
                int timeLevel = (cursor.getInt(0));
                if(timeLevel > 10)
                {
                    m_soundPool.play(m_cheer2SoundID, 1.0F, 1.0F, 0, 0, 1.0F);
                    returnCode = 1;
                }
                else
                {
                    m_soundPool.play(m_clearSoundID, 1.0F, 1.0F, 0, 0, 1.0F);
                    returnCode = 2;
                }
            }
            cursor.close();
        }
        //if(m_entryMode != getString(R.string.mastery))
        //{
        //}
        else if(correctSize == 0)
        {
            m_soundPool.play(m_laughSoundID, 1.0F, 1.0F, 0, 0, 1.0F);
            returnCode = 3;
        }
        else
        {
            m_soundPool.play(m_failedSoundID, 1.0F, 1.0F, 0, 0, 1.0F);
            returnCode = 4;
        }
        return returnCode;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // 予め音声データを読み込む
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            m_soundPool = new SoundPool(SOUND_POOL_MAX, AudioManager.STREAM_MUSIC, 0);
        }
        else
        {
            AudioAttributes attr = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            m_soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attr)
                    .setMaxStreams(SOUND_POOL_MAX)
                    .build();
        }
        m_correctSound = m_soundPool.load(getApplicationContext(), R.raw.correct2, 0);
        m_incorrectSound = m_soundPool.load(getApplicationContext(), R.raw.incorrect1, 0);
        m_cheer2SoundID = m_soundPool.load(getApplicationContext(), R.raw.cheer_long, 0);
        m_clearSoundID = m_soundPool.load(getApplicationContext(), R.raw.cheer, 0);
        m_failedSoundID = m_soundPool.load(getApplicationContext(), R.raw.tin, 0);
        m_laughSoundID = m_soundPool.load(getApplicationContext(), R.raw.laugh, 0);
    }

    @Override
    public void onPause() {
        super.onPause();
        m_pauseTime = System.currentTimeMillis();
        m_soundPool.release();
    }
    @Override
    protected void onRestart() {
        super.onRestart();

        long endTime = System.currentTimeMillis();
        m_startTime = m_startTime - (endTime - m_pauseTime);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        setResult(RESULT_OK);
    }

    //計算式のリストを生成する
    private void createFormulaList()
    {
        int maxRow = 9;
        m_listFormula.clear();
        for(int row = 1; row <= maxRow; row++)
        {
            if(m_targetRow == 0 || m_targetRow == row)
            {
                for(int column = 1; column <= maxRow; column++)
                {
                    Pair<Integer,Integer> pair = new Pair<>(row,column);
                    m_listFormula.add(pair);
                }
            }
        }
        m_remCountText.setText(getString(R.string.remaining, m_listFormula.size()));
        if(m_isRandom)
            Collections.shuffle(m_listFormula);

        //計算式リスト作成
        createFormula();
    }

    // 計算式を生成する
    private void createFormula()
    {
        Pair<Integer,Integer> pair = m_listFormula.get(m_count);

        // Challenge10のときの処理
        if(m_entryMode.equals(getString(R.string.challenge)))
        {
            m_num1Text.setText("");
            m_num2Text.setText("");
            m_answerText.setText(String.valueOf(pair.first*pair.second));
        }
        else
        {
            m_num1Text.setText(String.valueOf(pair.first));
            m_num2Text.setText(String.valueOf(pair.second));
            m_answerText.setText("");
        }
    }


    public void run() {

        while (!m_stopRun) {
            // sleep: period m_sec
            try {
                Thread.sleep(m_period);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
                m_stopRun = true;
            }

            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    long endTime = System.currentTimeMillis();
                    // カウント時間 = 経過時間 - 開始時間
                    m_diffTime = (endTime - m_startTime);
                    m_timerText.setText(m_dataFormat.format(m_diffTime));
                }
            });
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            m_stopRun = true;
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.pause_title));
            dialog.setMessage(getString(R.string.pause_message, m_entryMode));
            dialog.setPositiveButton(getString(R.string.pause_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AdRequest adRequest = new AdRequest.Builder().build();
                    m_InterstitialAd.loadAd(adRequest);
                    if (m_InterstitialAd.isLoaded()) {
                        m_InterstitialAd.show();
                    }
                    finish();
                }
            });
            dialog.setNegativeButton(getString(R.string.pause_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    m_stopRun = false;
                }
            });
            dialog.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
        switch (requestCode) {
            case 1:
                break;
            case 2:
                break;
            default:break;
        }
    }

}
