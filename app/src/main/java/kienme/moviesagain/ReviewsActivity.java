package kienme.moviesagain;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class ReviewsActivity extends AppCompatActivity {

    String LINK = "https://api.themoviedb.org/3/movie/";
    String KEY;
    String id;

    ArrayList<String> arrayList;
    ArrayAdapter<String> arrayAdapter;
    ListView listView;

    boolean reviewsFound = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reviews);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Reviews");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        KEY = "api_key="+getResources().getString(R.string.api_key);
        id = getIntent().getExtras().getString("id");

        arrayList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, arrayList);
        listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);

        new FetchTrailers().execute();
    }

    class FetchTrailers extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            arrayAdapter.notifyDataSetChanged();

            if(!reviewsFound) {
                Toast.makeText(ReviewsActivity.this, "No reviews available", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean success = false;
            StringBuilder data = new StringBuilder("");
            try {
                Log.d("DEBUG_R", "inside try");
                URL url = new URL(LINK+id+"/reviews?"+KEY);
                Log.d("DEBUG_R", "url: "+url.toString());
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = bufferedReader.readLine()) != null) {
                    data.append(line);
                    Log.d("DEBUG_R", line);
                }

                if(inputStream != null) {
                    inputStream.close();
                    parseResult(data.toString());
                    Log.d("DEBUG_R", "input stream not null");
                    success = true;
                }

                else Log.d("DEBUG_R", "input stream IS null");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return success;
        }
    }

    void parseResult(String data) {
        try {
            JSONObject response = new JSONObject(data);
            JSONArray results = response.getJSONArray("results");

            if(response.get("total_results").toString().equals("0"))
                reviewsFound = false;

            for(int i=0; i<results.length(); ++i) {
                JSONObject object= results.getJSONObject(i);
                String content = (String) object.get("content");
                content += ( "\n\nAuthor: " + object.get("author") );
                arrayList.add(content);
            }

            arrayAdapter.notifyDataSetChanged();

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
