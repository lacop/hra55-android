package com.geewhizstuff.hra55;

import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    final static int TIME_FOR_QUESTION = 15;
    final static int NUM_ANSWERS = 5;

    int questionNumber = 0;
    int correctAnswers = 0;
    public String[] currentAnswers;

    CountDownTimer timer;

    ArrayList<MediaPlayer> correctPlayers = new ArrayList<MediaPlayer>();
    ArrayList<MediaPlayer> wrongPlayers = new ArrayList<MediaPlayer>();
    Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String projectToken = "889f8ea7b0077cf3ef1e0338e9914873"; // e.g.: "1ef7e30d2a58d27f4b90c42e31d6d7ad"
        MixpanelAPI mixpanel = MixpanelAPI.getInstance(this, projectToken);
        mixpanel.track("sapan", null);

        correctPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.bravo1));
        correctPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.bravo2));
        correctPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.gogo));

        wrongPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.wrong1));
        wrongPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.wrong2));
        wrongPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.wrong3));
        wrongPlayers.add(MediaPlayer.create(getApplicationContext(), R.raw.wrong4));

        nextQuestion();
    }

    private synchronized void nextQuestion() {
        if (timer != null) {
            timer.cancel();
        }

        questionNumber++;
        correctAnswers = 0;

        new LoadQuestion().execute(questionNumber);
    }

    class LoadQuestion extends AsyncTask<Integer, Void, String[]> {

        @Override
        protected String[] doInBackground(Integer... params) {
            try {
                URL url = new URL("https://hra55-1108.appspot.com/command?action=get_qa&n=" + params[0]);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder str = new StringBuilder(is.available());
                String line;
                while ((line = br.readLine()) != null) str.append(line);

                JSONObject json = new JSONObject(str.toString());

                String[] result = new String[NUM_ANSWERS + 1];
                result[0] = json.getString("question");
                JSONArray answers = json.getJSONArray("answers");
                for (int i = 0; i < NUM_ANSWERS; i++) {
                    result[i+1] = answers.getJSONArray(i).getString(0);
                }

                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return new String[] {"Network error", "", "", "", "", ""};
            }
        }

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);

            String question = strings[0];
            String[] answers = new String[NUM_ANSWERS];
            Log.d("TAG", question);
            for (int i = 0; i < NUM_ANSWERS; i++) {
                answers[i] = strings[i+1];
                Log.d("TAG", answers[i]);
            }
            setQuestion(question, answers);

            timer = new CountDownTimer(TIME_FOR_QUESTION * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    ((TextView)findViewById(R.id.timeLeft)).setText("" + millisUntilFinished/1000);
                }

                @Override
                public void onFinish() {
                    nextQuestion();
                }
            };
            timer.start();
        }
    }

    private void setQuestion(String question, String[] answers) {
        assert answers.length == NUM_ANSWERS;

        ((TextView)findViewById(R.id.question)).setText(question);
        currentAnswers = answers;

        for (int i = 1; i <= NUM_ANSWERS; i++) {
            int id;
            id = getResources().getIdentifier("answer"+i, "id", getPackageName());
            TextView answerView = (TextView) findViewById(id);
            answerView.setText("???");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public synchronized void submit(View view) {
        EditText responseText = (EditText) findViewById(R.id.response);
        String response = responseText.getText().toString().toLowerCase().trim();
        boolean found=false;
        int i;
        for(i=0;i<currentAnswers.length;i++) {
            if (currentAnswers[i].toLowerCase().trim().equals(response)) {
                found = true;
                break;
            }
        }

        if (found) {
            int id;
            id = getResources().getIdentifier("answer"+(i+1), "id", getPackageName());
            TextView answerView = (TextView) findViewById(id);
            if (!answerView.getText().toString().toLowerCase().trim().equals(response)) {
                correctAnswers++;
                correctPlayers.get(random.nextInt(correctPlayers.size())).start();
            }
            answerView.setText(response);
        } else {
            wrongPlayers.get(random.nextInt(wrongPlayers.size())).start();
        }

        if (correctAnswers == NUM_ANSWERS)
            nextQuestion();
    }
}
