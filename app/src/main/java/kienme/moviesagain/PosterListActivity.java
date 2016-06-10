package kienme.moviesagain;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import kienme.moviesagain.dummy.DummyContent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * An activity representing a list of Posters. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link PosterDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class PosterListActivity extends AppCompatActivity {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */

    static boolean mTwoPane;

    String LINK = "https://api.themoviedb.org/3/discover/movie?";
    String SORT = "sort_by=popularity.desc";
    String KEY;

    ArrayList<PosterGridItem> gridData;
    PosterGridViewAdapter posterGridViewAdapter;
    GridView gridView;
    static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poster_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        KEY = "&api_key="+getResources().getString(R.string.api_key);

        context = this;
        gridData = new ArrayList<>();
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setColumnWidth(calcImageSize());

        if (findViewById(R.id.poster_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }

        new FetchData().execute();

        posterGridViewAdapter = new PosterGridViewAdapter(this, R.layout.grid_item, gridData, getSupportFragmentManager());
        posterGridViewAdapter.setGridData(gridData);
        gridView.setAdapter(posterGridViewAdapter);
    }

    static int calcImageSize() {
        int size = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch(size) {

            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                //return 1000;

            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                //return 500;

            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return 342;

        }

        return 185;
    }

    public class FetchData extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            //progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean success = false;
            StringBuilder data = new StringBuilder("");

            try {
                Log.d("DEBUG", "inside try");
                URL url = new URL(LINK+SORT+KEY);
                Log.d("DEBUG", "url: "+url.toString());
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                Log.d("DEBUG", "before connect()");
                httpURLConnection.connect();

                Log.d("DEBUG", "after connect()");

                InputStream inputStream = httpURLConnection.getInputStream();
                Log.d("DEBUG", "inside try");
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = bufferedReader.readLine()) != null) {
                    data.append(line);
                    Log.d("DEBUG", line);
                }

                if(inputStream != null) {
                    inputStream.close();
                    parseResult(data.toString());
                    Log.d("DEBUG", "input stream not null");
                    success = true;
                }

                else Log.d("DEBUG", "input stream nullllll");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if(success) {
                posterGridViewAdapter = new PosterGridViewAdapter(context, R.layout.grid_item, gridData, getSupportFragmentManager());
                posterGridViewAdapter.setGridData(gridData);
                gridView.setAdapter(posterGridViewAdapter);
                Log.d("DEBUG", "post execute success");
            }
            else {
                Toast.makeText(context, "Failed to load images", Toast.LENGTH_LONG).show();
                //progressBar.setVisibility(View.GONE);
                Log.d("DEBUG", "post execute fail");
            }
        }
    }

    void parseResult(String data) {
        String imageBase = "http://image.tmdb.org/t/p/w"+calcImageSize();
        try {
            JSONObject response = new JSONObject(data);
            JSONArray results = response.getJSONArray("results");

            for(int i = 0; i<results.length(); ++i) {
                JSONObject object= results.getJSONObject(i);
                String imagePath = object.get("poster_path").toString();
                PosterGridItem item = new PosterGridItem();

                item.setImage(imageBase+imagePath+"&api_key="+KEY);
                item.setName(object.get("title").toString());
                item.setRelease(object.get("release_date").toString());
                item.setRating(object.get("vote_average").toString());
                item.setOverview(object.get("overview").toString());
                item.setId(object.get("id").toString());
                Log.d("DEBUG", "Image path:" + imageBase+imagePath+"&api_key="+KEY);
                gridData.add(item);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //menu.setGroupCheckable(R.id.menu_group, true, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch(id) {
            case R.id.rating:
                item.setChecked(true);
                SORT = "sort_by=vote_average.desc";
                gridData.clear();
                new FetchData().execute();
                return true;

            case R.id.popularity:
                item.setChecked(true);
                SORT = "sort_by=popularity.desc";
                gridData.clear();
                new FetchData().execute();
                return true;

            case R.id.favourites:
                item.setChecked(true);
                gridData.clear();
                onFavSelect();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void onFavSelect() {
        SQLiteDatabase database = openOrCreateDatabase("FavDB", Context.MODE_PRIVATE, null);
        openOrCreateDatabase("FavDB", Context.MODE_PRIVATE, null);

        Cursor cursor = database.rawQuery("SELECT * FROM fav", null);
        if(cursor.getCount()==0) {
            Toast.makeText(PosterListActivity.this, "No favourites found", Toast.LENGTH_LONG).show();
        }
        else {
            while(cursor.moveToNext()) {
                PosterGridItem item = new PosterGridItem();

                item.setId(cursor.getString(0));
                item.setName(cursor.getString(1));
                item.setImage(cursor.getString(2));
                item.setRelease(cursor.getString(3));
                item.setRating(cursor.getString(4));
                item.setOverview(cursor.getString(5));

                gridData.add(item);
            }

            posterGridViewAdapter = new PosterGridViewAdapter(context, R.layout.grid_item, gridData, getSupportFragmentManager());
            posterGridViewAdapter.setGridData(gridData);
            gridView.setAdapter(posterGridViewAdapter);
        }
    }
}
