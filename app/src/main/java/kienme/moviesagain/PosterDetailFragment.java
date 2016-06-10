package kienme.moviesagain;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import kienme.moviesagain.dummy.DummyContent;

/**
 * A fragment representing a single Poster detail screen.
 * This fragment is either contained in a {@link PosterListActivity}
 * in two-pane mode (on tablets) or a {@link PosterDetailActivity}
 * on handsets.
 */
public class PosterDetailFragment extends Fragment {

    String name, image, release, rating, overview, id;
    boolean starred = false;

    SQLiteDatabase database;

    String trailerLink = "https://youtube.com/watch?v=";

    String LINK = "https://api.themoviedb.org/3/movie/";
    String KEY;

    Context context;
    Activity activity;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PosterDetailFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        context = getContext();
        activity = getActivity();

        KEY = "api_key="+getResources().getString(R.string.api_key);

        database = context.openOrCreateDatabase("FavDB", Context.MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS fav(id TEXT, name TEXT, image TEXT, release TEXT, rating TEXT, overview TEXT);");

        getMovieData();
        setMovieData();

        Button readReviewsButton = (Button) activity.findViewById(R.id.review_btn);
        readReviewsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent reviewsIntent = new Intent(context, ReviewsActivity.class);
                reviewsIntent.putExtra("id", id);
                startActivity(reviewsIntent);
            }
        });

        Button watchTrailerButton = (Button) activity.findViewById(R.id.trailer_btn);
        watchTrailerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FetchTrailer().execute();
            }
        });

        final ImageButton starButton = (ImageButton) activity.findViewById(R.id.star_btn);
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(starred) {
                    starred = false;
                    starButton.setImageResource(android.R.drawable.btn_star_big_off);
                    database.execSQL("DELETE FROM fav WHERE id="+id);
                }
                else {
                    starred = true;
                    starButton.setImageResource(android.R.drawable.btn_star_big_on);
                    database.execSQL("INSERT INTO fav VALUES("+id+", \""+name+"\", \""+image+"\", \""+release+"\", \""+rating+"\", \""+overview+"\");");
                }

                starButton.invalidate();
            }
        });

        Cursor cursor = database.rawQuery("SELECT * FROM fav WHERE id="+id, null);
        if(cursor.getCount()>0) {
            starred = true;
            starButton.setImageResource(android.R.drawable.btn_star_big_on);
        }
        cursor.close();


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_details, container, false);

        return rootView;
    }

    private void getMovieData() {
        Bundle data = activity.getIntent().getExtras();
        name = data.getString("name");
        image = data.getString("image");
        release = data.getString("release");
        rating = data.getString("rating");
        overview = data.getString("overview");
        id = data.getString("id");
    }

    private void setMovieData() {
        final CollapsingToolbarLayout layout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        ImageView imageView = (ImageView) activity.findViewById(R.id.details_image);
        TextView releaseView = (TextView) activity.findViewById(R.id.details_release);
        TextView ratingView = (TextView) activity.findViewById(R.id.details_rating);
        TextView overviewView = (TextView) activity.findViewById(R.id.details_overview);

        layout.setTitle(name);
        Picasso.with(context).load(image).into(imageView);
        releaseView.setText("Release date: " + release);
        ratingView.setText("Rating: " + rating + "/10");
        overviewView.setText(overview);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, layout.getTitle(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public class FetchTrailer extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPostExecute(Boolean success) {
            if(success)
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trailerLink)));
            else
                Toast.makeText(context, "Failed to fetch trailer", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean success = false;
            StringBuilder data = new StringBuilder("");
            try {
                Log.d("DEBUG_T", "inside try");
                URL url = new URL(LINK+id+"/videos?"+KEY);
                Log.d("DEBUG_T", "url: "+url.toString());
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = bufferedReader.readLine()) != null) {
                    data.append(line);
                    Log.d("DEBUG_T", line);
                }

                if(inputStream != null) {
                    inputStream.close();
                    parseResult(data.toString());
                    Log.d("DEBUG_T", "input stream not null");
                    success = true;
                }

                else Log.d("DEBUG_T", "input stream IS null");

            } catch (Exception e) {
                e.printStackTrace();
            }

            return success;
        }
    }

    void parseResult(String data) {
        try {
            JSONObject object = (new JSONObject(data)).getJSONArray("results").getJSONObject(0);
            trailerLink+=(String)object.get("key");

            Log.d("DEBUG_T", trailerLink);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
