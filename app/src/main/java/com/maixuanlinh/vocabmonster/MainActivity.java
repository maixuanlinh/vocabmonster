package com.maixuanlinh.vocabmonster;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private TextView resultTxv;
    private EditText editText;
    private ClipboardManager clipboardManager;
    private Button importBtn;
    private String resultString ="";
    private ClipboardManager.OnPrimaryClipChangedListener onPrimaryClipChangedListener;
    private ArrayList<String> wordList = new ArrayList<>();
    private Button reset;
    private Button cancelLast;

    public ArrayList<String> getWordList() {
        return wordList;
    }

    private Button getResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);
        resultTxv = findViewById(R.id.resultTxv);
        importBtn = findViewById(R.id.importBtn);
        getResult = findViewById(R.id.getResultBtn);
        reset = findViewById(R.id.reset);
        cancelLast = findViewById(R.id.cancelLastOne);
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textView.setText(editText.getText().toString());
            }
        });
        onPrimaryClipChangedListener = new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                ClipData clip = clipboardManager.getPrimaryClip();
                String word = clip.getItemAt(0).getText().toString();

                if (countWordsUsingSplit(word) == 1) {
                    new TakeLemmaTask(word, MainActivity.this).execute(inflections(word));
                } else {
                    word+=": ##.\n";
                    wordList.add(word);
                }


            }
        };
        clipboardManager.addPrimaryClipChangedListener(onPrimaryClipChangedListener);
        resultTxv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clipboardManager.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
                ClipData clipData = ClipData.newPlainText("Final result",resultTxv.getText().toString());
                clipboardManager.setPrimaryClip(clipData);
            }
        });

        getResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fuckingFinal = "";
                for (int i = 0; i < wordList.size(); i++) {

                       fuckingFinal += wordList.get(i);

                }
                resultTxv.setText(""+wordList.size());
                clipboardManager.removePrimaryClipChangedListener(onPrimaryClipChangedListener);
                ClipData clipData = ClipData.newPlainText("Final", fuckingFinal);
                clipboardManager.setPrimaryClip(clipData);
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wordList.removeAll(wordList);
                editText.setText("Paste");
                resultTxv.setText("");
                clipboardManager.clearPrimaryClip();
            }
        });
        cancelLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wordList.remove(wordList.size()-1);
                wordList.remove(wordList.size()-1);
            }
        });

    }

    public int countWordsUsingSplit(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        String[] words = input.split("\\s+");
        return words.length;
    }

    private String inflections(String rawWord) {
        final String language = "en";
        final String word = rawWord;
        final String word_id = word.toLowerCase();
        return "https://od-api.oxforddictionaries.com:443/api/v2/lemmas/" + language + "/" + word_id;
    }

    private String dictionaryEntries(String wordToTranslate) {
        final String language = "en-gb";
        final String word = wordToTranslate;
        /*   final String fields = "pronunciations";*/
        final String strictMatch = "false";
        final String word_id = word.toLowerCase();
        return "https://od-api.oxforddictionaries.com:443/api/v2/entries/" + language + "/" + word_id;
    }

    private class TakeEntriesTask extends AsyncTask<String, Integer, String> {
        private String trueWord;
        private boolean trueWordIsRaw;
        private String rawWord;
        private WeakReference<MainActivity> mainActivity;
        public TakeEntriesTask(String trueWord,boolean trueWordIsRaw, String rawWord, MainActivity mainActivity) {
            this.trueWord = trueWord;
            this.trueWordIsRaw = trueWordIsRaw;
            this.rawWord = rawWord;
            this.mainActivity = new WeakReference<>(mainActivity);
        }

        @Override
        protected String doInBackground(String... params) {

            //TODO: replace with your own app id and app key
            final String app_id = "8188144a";
            final String app_key = "83af78585b53adde682406e60e4e571c";

            try {
                URL url = new URL(params[0]);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept","application/json");
                urlConnection.setRequestProperty("app_id",app_id);
                urlConnection.setRequestProperty("app_key",app_key);

                // read the output from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                return stringBuilder.toString();

            }
            catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String r) {
            super.onPostExecute(r);

            try {

                JSONObject obj = new JSONObject(r);

                JSONArray resultsArr = obj.getJSONArray("results");
                JSONObject firstObject = resultsArr.getJSONObject(0);

                JSONArray lexicalEntries = firstObject.getJSONArray("lexicalEntries");
                JSONObject objectOfLexicalEntries = lexicalEntries.getJSONObject(0);

                JSONArray entriesArray = objectOfLexicalEntries.getJSONArray("entries");
                JSONArray pronunciationsArray = objectOfLexicalEntries.getJSONArray("pronunciations");
                JSONObject objectOfEntriesArray = entriesArray.getJSONObject(0);
                JSONObject objectOfPronunciationsArray = pronunciationsArray.getJSONObject(0);

                JSONArray sensesArray = objectOfEntriesArray.getJSONArray("senses");

                JSONObject ObjectOfSensesArray = sensesArray.getJSONObject(0);


                JSONArray definitionsArray = ObjectOfSensesArray.getJSONArray("definitions");
                final String def = definitionsArray.getString(0);
                final String audioFile = objectOfPronunciationsArray.getString("audioFile");
                if (trueWordIsRaw) {
                    String stringToReturn = rawWord+": "+def+".\n";

                    mainActivity.get().getWordList().add(stringToReturn);
                    mainActivity.get().resultTxv.setText(mainActivity.get().getWordList().get(mainActivity.get().getWordList().size()-1));
                    Log.i("wordListSize"," "+mainActivity.get().getWordList().size());


                } else {
                    String stringToReturn = rawWord+": "+trueWord+" ~ "+def+".\n";
                    mainActivity.get().getWordList().add(stringToReturn);
                    mainActivity.get().resultTxv.setText(mainActivity.get().getWordList().get(mainActivity.get().getWordList().size()-1));
                    Log.i("wordListSize"," "+mainActivity.get().getWordList().size());


                }








            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("Error",e.toString());
            }


        }
    }

    private class TakeLemmaTask extends AsyncTask<String, Integer, String> {
        private String rawWord;
        private WeakReference<MainActivity> mainActivity;
        public TakeLemmaTask(String rawWord, MainActivity mainActivity) {
            this.rawWord = rawWord;
            this.mainActivity = new WeakReference<>(mainActivity);
        }

        @Override
        protected String doInBackground(String... params) {

            //TODO: replace with your own app id and app key
            final String app_id = "8188144a";
            final String app_key = "83af78585b53adde682406e60e4e571c";
            try {
                URL url = new URL(params[0]);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept","application/json");
                urlConnection.setRequestProperty("app_id",app_id);
                urlConnection.setRequestProperty("app_key",app_key);

                // read the output from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                return stringBuilder.toString();

            }
            catch (Exception e) {
                e.printStackTrace();
                return e.toString();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            try {
                JSONObject biggestObject = new JSONObject(result);
                JSONArray resultsArray = biggestObject.getJSONArray("results");

                JSONObject sonOfResultsArray = resultsArray.getJSONObject(0);
                JSONArray lexicalEntriesArray = sonOfResultsArray.getJSONArray("lexicalEntries");

                JSONObject sonOflexicalEntries = lexicalEntriesArray.getJSONObject(0);
                JSONArray inflectionArray = sonOflexicalEntries.getJSONArray("inflectionOf");

                JSONObject inflectionObject = inflectionArray.getJSONObject(0);

                String trueWord = inflectionObject.getString("id");
                if (trueWord.toLowerCase().equals(rawWord.toLowerCase())) {
                    TextView resultTxv = mainActivity.get().findViewById(R.id.resultTxv);
                    new TakeEntriesTask(trueWord, true,rawWord, mainActivity.get()).execute(dictionaryEntries(trueWord));
                } else {
                    TextView resultTxv = mainActivity.get().findViewById(R.id.resultTxv);
                    new TakeEntriesTask(trueWord, false,rawWord, mainActivity.get()).execute(dictionaryEntries(trueWord));
                }










            } catch (JSONException e) {
                e.printStackTrace();


            }





        }
    }



}
