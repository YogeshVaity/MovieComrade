package com.compmaestros.moviecomrade;


import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Displays movies in GridView
 */
public class MainFragment extends Fragment {


    private static final String LOG_TAG = MainFragment.class.getSimpleName();
    public static final String MOVIE_PARCEL = "com.compmaestros.moviecomrade.MOVIE_PARCEL";
    // TODO: 10/2/16 this list is not used anywhere 
    private List<String> movieImageUrls;
    private MovieAdapter movieAdapter;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //setHasOptionsMenu(true);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) rootView.findViewById(R.id.grids_fragment_toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.setSupportActionBar(toolbar);

        GridView movieGrids = (GridView) rootView.findViewById(R.id.movie_grid_view);
        // Associate Adapter with GridView.
        // Adapter is backed by List of image urls of movies.
        movieAdapter = new MovieAdapter(getActivity(), new ArrayList<MovieInfo>());
        movieGrids.setAdapter(movieAdapter);
        movieGrids.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                showMovieDetails(position);
            }
        });

        // Inflate the layout for this fragment
        return rootView;
    }

    /** Launches Details Activity to show details about the selected movie.
     * @param position is the current position of the data item in ArrayAdapter on which the user
     *                 clicked.
     */
    private void showMovieDetails(int position) {
        MovieInfo movieInfo = movieAdapter.getItem(position);
        Intent detailsIntent = new Intent(getActivity(), DetailsActivity.class);
        detailsIntent.putExtra(MainFragment.MOVIE_PARCEL, movieInfo);
        startActivity(detailsIntent);
    }

    // TODO: 5/2/16 Can use ViewPager to display different pages instead of unlimited scroll.
    /** TODO: 9/2/16 Solve the following issue.
     * If this call is put in the onStart(), the call will be made everytime, the
     * activity is visible to the user, as a result there will be wasted calls to API.
     * Check if the user really want to refresh the list. Something can be taken from
     * savedInstanceState maybe. The current situation is right for if the user changes the
     * Sort By setting, when the back button is pressed, the activity get's refreshed. So keep this
     * in my while solving this issue.
     */
    @Override
    public void onStart() {
        super.onStart();
        // Call AsyncTask
        new FetchMoviesTask().execute("1");
    }

    // TODO: 5/2/16 Add code to check if the internet connection is enabled. 
    public class FetchMoviesTask extends AsyncTask<String, Void, List<MovieInfo>> {
        @Override
        protected List<MovieInfo> doInBackground(String... pageNumber) {
            List<MovieInfo> movieInfoObjects = null;
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonString = null;

            try {
                URL url = new URL(buildAPIUrl(pageNumber[0]));
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String.
                // If the HTTP response indicates that an error occurred, getInputStream() will
                // throw an IOException. Use getErrorStream() to read the error response.
                InputStream inputStream = urlConnection.getInputStream();
                if(inputStream == null) {
                    // Nothing to do, return
                    return null;
                }

                StringBuilder buffer = new StringBuilder();

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a lot easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line);
                    buffer.append("\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                moviesJsonString = buffer.toString();

            } catch (IOException e) {
                e.printStackTrace();
                // If the code didn't successfully get the weather data, there's no point in
                // attempting to parse it.
                return null;
            } finally {
                if(urlConnection != null) {
                    urlConnection.disconnect();
                }
                if(reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getMovieObjectsFromJson(moviesJsonString);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // If there is an error get Json or parsing the Json.
            return null;
        }

        @Override
        protected void onPostExecute(List<MovieInfo> movieInfoObjects) {
            super.onPostExecute(movieInfoObjects);
            if(movieInfoObjects != null) {
                movieAdapter.clear();
                movieAdapter.addAll(movieInfoObjects);
            }
        }

        /**
         * Extracts the Image url from Json String.
         *
         * @param moviesJsonString is String in Json format.
         * @return Returns complete image url ready to be used with Picasso.
         */
        private List<MovieInfo> getMovieObjectsFromJson(String moviesJsonString) throws JSONException {
            List<MovieInfo> movieObjectList = new ArrayList<>();
            JSONObject moviesJson = new JSONObject(moviesJsonString);
            // results is the name of an array in Json String
            // TODO: 10/2/16 Write constants for the following JSON attribute names.
            JSONArray results = moviesJson.getJSONArray("results");
            for (int i = 0; i < results.length(); i++) {
                JSONObject poster = results.getJSONObject(i);
                String relativeImageUrl = poster.getString("poster_path");
                String relativeBackdropUrl = poster.getString("backdrop_path");
                String movieId = poster.getString("id");
                String overview = poster.getString("overview");
                String releaseDate = poster.getString("release_date");
                String originalTitle = poster.getString("original_title");
                String voteAverage = poster.getString("vote_average");
                movieObjectList.add(new MovieInfo(
                        relativeImageUrl,
                        relativeBackdropUrl,
                        movieId,
                        overview,
                        releaseDate,
                        originalTitle,
                        voteAverage));
            }
            return movieObjectList;
        }

        /**
         * Constructs the url for API call.
         * Possible parameters are available at theMovieDB API page, at
         * http://docs.themoviedb.apiary.io/#reference/discover/discovermovie
         * Example API calls:
         * For popularity : http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&page=1&api_key={API_KEY}
         * For highest rating : http://api.themoviedb.org/3/discover/movie?sort_by=vote_average.desc&page=1&api_key={API_KEY}
         * @return Returns the string url.
         * @param pageNumber is page query parameter used in API call
         */
        private String buildAPIUrl(String pageNumber) {
            Uri.Builder apiUriBuilder = new Uri.Builder();
            apiUriBuilder.scheme("http");
            apiUriBuilder.authority("api.themoviedb.org");
            apiUriBuilder.appendPath("3");
            apiUriBuilder.appendPath("discover");
            apiUriBuilder.appendPath("movie");
            apiUriBuilder.appendQueryParameter("sort_by", getSortBySetting());
            apiUriBuilder.appendQueryParameter("page", pageNumber);
            // Default mode should be json
            //apiUriBuilder.appendQueryParameter("mode", "json");
            apiUriBuilder.appendQueryParameter("api_key", BuildConfig.TMDB_API_KEY);
            return apiUriBuilder.build().toString();
        }

        private String getSortBySetting() {
            SharedPreferences settings =
                    PreferenceManager.getDefaultSharedPreferences(getContext());
            return settings.getString(
                    getString(R.string.pref_sort_by_key),
                    getString(R.string.pref_sort_by_default));
        }
    }
}
