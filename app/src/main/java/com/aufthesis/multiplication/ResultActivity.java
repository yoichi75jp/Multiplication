package com.aufthesis.multiplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 // Created by yoichi75jp2 on 2016/08/15.
 */
public class ResultActivity extends Activity implements View.OnClickListener {

    Context m_context;
    private SimpleDateFormat m_dataFormat = new SimpleDateFormat("mm:ss.SSS", Locale.getDefault());
    SharedPreferences m_prefs;
    LinearLayout m_certificationLayout;
    private AdView m_AdView;
    private ArrayList<String> m_checkList;
    private MediaPlayer m_mediaPlayer;
    private String m_newRecord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        m_context = this;

        String sMastery = getString(R.string.mastery);

        // Initialize the Mobile Ads SDK.
        MobileAds.initialize(this, "ca-app-pub-1485554329820885~5867720659");

        DBOpenHelper DbHelper = new DBOpenHelper(this);
        SQLiteDatabase db = DbHelper.getDataBase();

        m_prefs = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);

        Intent intent = getIntent();
        String entryMode = intent.getExtras().getString(getString(R.string.keyword1));
        int correctSize = intent.getExtras().getInt(getString(R.string.keyword4));
        int questionSize = intent.getExtras().getInt(getString(R.string.keyword5));
        long timeResult = intent.getExtras().getLong(getString(R.string.keyword6));
        m_checkList = intent.getExtras().getStringArrayList(getString(R.string.keyword7));

        // SharedPreferencesから現在の最高称号を取得
        String preTitle = m_prefs.getString("title", "");
        float prePerSec = m_prefs.getFloat("per_sec", 10.f);

        //現在の最高称号の表示
        m_certificationLayout = (LinearLayout)findViewById(R.id.max_now_layout);
        TextView certificationText = (TextView)findViewById(R.id.max_now_text);
        certificationText.setText(getString(R.string.certification, preTitle));
        Button initializeButton = (Button)findViewById(R.id.initialize_button);
        initializeButton.setOnClickListener(this);
        if(!sMastery.equals(entryMode) || preTitle.equals(""))
            m_certificationLayout.setVisibility(View.INVISIBLE);

        TextView accuracyRateText = (TextView)findViewById(R.id.accuracy_rate);
        double rate = ((double)correctSize/(double)questionSize)*100d;
        accuracyRateText.setText(getString(R.string.rate1, correctSize, questionSize, String.valueOf((int)rate)));
        TextView answerTimeText = (TextView)findViewById(R.id.answer_time);
        long lAnswerTime = timeResult/(long)questionSize;
        String answerTime = m_dataFormat.format(lAnswerTime);
        answerTimeText.setText(answerTime);

        double per_sec = (double)lAnswerTime/1000d;
        Button backButton = (Button)findViewById(R.id.back_button);
        backButton.setOnClickListener(this);

        Button checkButton = (Button)findViewById(R.id.check_button);
        checkButton.setOnClickListener(this);

        String level = "Level";
        if(Locale.getDefault().toString().equals(Locale.JAPAN.toString()))
            level = "Level_ja";

        String sql;
        String message;
        if(correctSize == questionSize)
        {
            checkButton.setVisibility(View.INVISIBLE);
            sql = "select " + level + " from class_level where TimeLevel = " +
                    "(select Min(TimeLevel) from time_level where per_second < " + String.valueOf(per_sec) + ")";
            message = getString(R.string.result_all);
            //素晴らしい速さのとき
            if(per_sec <= 1.0d)
                message = getString(R.string.result_super_all);
        }
        else if(correctSize == 0)
        {
            sql = "select " + level + " from class_level where correct_size = 0";
            message = getString(R.string.nothing);
        }
        else
        {
            float correctSize2 = correctSize;
            if(PracticeActivity.m_isShortMode)
                correctSize2 *= 2.25;

            sql = "select Min(" + level + ") from class_level where correct_size < " + String.valueOf(correctSize2);
            message = getString(R.string.shame);
            // あと1問で全問正解だったとき
            if(correctSize == questionSize - 1)
                message = getString(R.string.one_more_push);
        }

        TextView resultText = (TextView)findViewById(R.id.result);
        if(sMastery.equals(entryMode))
        {
            Cursor cursor = db.rawQuery(sql, null);
            cursor.moveToFirst();
            if (cursor.getCount() != 0) {
                m_newRecord = (cursor.getString(0));
                resultText.setText(m_newRecord);
                resultText.setTextColor(Color.parseColor("#FF4444"));

                if(correctSize == questionSize && prePerSec > per_sec) // 全問正解且つ以前の成績を超えたら共有
                {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putString("title", m_newRecord);
                    editor.putFloat("per_sec", (float)per_sec);
                    editor.apply();

                    Bundle fireLogBundle = new Bundle();
                    fireLogBundle.putString("LevelUp", m_newRecord);
                    MyApp.getFirebaseAnalytics().logEvent(FirebaseAnalytics.Event.LEVEL_UP, fireLogBundle);

                    certificationText.setText(getString(R.string.certification, m_newRecord));
                    m_certificationLayout.setVisibility(View.VISIBLE);

                    if(!preTitle.equals(""))
                    {
                        m_mediaPlayer = MediaPlayer.create(this, R.raw.fanfare);
                        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                        dialog.setTitle(getString(R.string.update_record_title));
                        LayoutInflater inflater = getLayoutInflater();
                        View view = inflater.inflate(R.layout.update_record_layout, null);
                        TextView highScoreText = (TextView)view.findViewById(R.id.record);
                        highScoreText.setText(getString(R.string.record, m_newRecord));
                        ImageButton shareButton = (ImageButton)view.findViewById(R.id.twitter);
                        shareButton.setOnClickListener(this);
                        dialog.setView(view);
                        dialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                m_mediaPlayer.stop();   // 再生終了
                                m_mediaPlayer.reset();  // リセット
                                m_mediaPlayer.release();// リソースの解放
                                m_mediaPlayer = null;
                            }
                        });
                        dialog.setCancelable(false);
                        dialog.show();
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                        }
                        m_mediaPlayer.start();
                        this.induceReview();
                    }
                }
            }
            cursor.close();

            int count = m_prefs.getInt(getString(R.string.count_induce), 0);
            count++;
            SharedPreferences.Editor editor = m_prefs.edit();
            editor.putInt(getString(R.string.count_induce), count);
            editor.apply();
        }
        else
        {
            resultText.setText(message);
            resultText.setTextSize(35);
        }
       // }

        //バナー広告
        m_AdView = (AdView) findViewById(R.id.adView5);
        AdRequest adRequest = new AdRequest.Builder().build();
        m_AdView.loadAd(adRequest);

        db.close();

        //m_AdView.setVisibility(View.INVISIBLE);
    }

    //Button押下時処理
    public void onClick(View view)
    {
        int id = view.getId();
        if(id == R.id.back_button)
        {
            Intent data = new Intent();
            setResult(RESULT_OK, data);
            finish();
        }
        else if(id == R.id.check_button)
        {
            final CharSequence[] items = m_checkList.toArray(new CharSequence[m_checkList.size()]);
            AlertDialog.Builder listDlg = new AlertDialog.Builder(this);
            listDlg.setTitle(getString(R.string.check_title));
            listDlg.setItems(items , null);
            listDlg.setPositiveButton(getString(R.string.check_close), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which){}
            });
            final AlertDialog dialog = listDlg.show();
            ListView list = dialog.getListView();
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String[] items = m_checkList.get(position).split(getString(R.string.times_sign));
                    int answer = Integer.parseInt(items[0])*Integer.parseInt(items[1]);
                    Toast toast = Toast.makeText(m_context, String.valueOf(answer),Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }
        else if(id == R.id.twitter)
        {
            String shareMessage = Uri.encode(getString(R.string.share_message, m_newRecord, "MultiplicationApp"));
            String url = getString(R.string.twitter_url, shareMessage);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        }
        else if(id == R.id.initialize_button)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.initialize));
            dialog.setMessage(getString(R.string.qestion_initialize));
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which){
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putString("title", "");
                    editor.putFloat("per_sec", 10.f);
                    editor.apply();
                    m_certificationLayout.setVisibility(View.INVISIBLE);
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which){}
            });
            dialog.setCancelable(false);
            dialog.show();
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
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void induceReview()
    {
        int count = m_prefs.getInt(getString(R.string.count_induce), 0);
        if(count >= 25)
        {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(getString(R.string.induce_title));
            dialog.setMessage(getString(R.string.induce_message));
            dialog.setPositiveButton(getString(R.string.induce_ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = m_prefs.edit();
                    editor.putInt(getString(R.string.count_induce), 0);
                    editor.apply();
                    Intent googlePlayIntent = new Intent(Intent.ACTION_VIEW);
                    googlePlayIntent.setData(Uri.parse("market://details?id=com.aufthesis.multiplication"));
                    startActivity(googlePlayIntent);
                }
            });
            dialog.setNegativeButton(getString(R.string.induce_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            dialog.setCancelable(false);
            dialog.show();
        }
    }

}
