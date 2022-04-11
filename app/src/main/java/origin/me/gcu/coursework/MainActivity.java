package origin.me.gcu.coursework;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Name: Matthew J. Garioch
 * Student ID: S1713642
 */

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, BottomBarModel.BottomSheetListener
{
    private ArrayList<ParseClass> roadworksList = new ArrayList<>();
    private ArrayList<ParseClass> plannedRoadworksList = new ArrayList<>();
    private ArrayList<ParseClass> currentIncidentList = new ArrayList<>();

    private ListView listView;
    private RssAdapter adapter;
    private EditText searchBar;
    private TextView listDate;
    private TextView listHeading;

    private GoogleMap map;

    private FloatingActionButton dateFab;
    private FloatingActionButton contentFab;

    // The Traffic Scotland URLs
    private String currentIncidentsSource = "https://trafficscotland.org/rss/feeds/currentincidents.aspx";
    private String roadworksSource = "https://trafficscotland.org/rss/feeds/roadworks.aspx";
    private String plannedRoadworksSource = "https://trafficscotland.org/rss/feeds/plannedroadworks.aspx";

    private boolean mapReady = false;
    private boolean mapFocused = false;
    private RssItem activeItemType = RssItem.current_roadworks;

    private Date filterDate;
    private String filterString;

    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        searchBar = findViewById(R.id.searchBar);
        dateFab = findViewById(R.id.dateFab);
        contentFab = findViewById(R.id.contentFab);
        listDate = findViewById(R.id.listDate);
        listHeading = findViewById(R.id.listHeading);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MainActivity.this);

        dateFab.bringToFront();
        contentFab.bringToFront();


        //Unfocus the search bar when clicking elsewhere.
        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard(v);
                }
            }
        });

        //Submit/done on searchbar edittext
        searchBar.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    filterString = searchBar.getText().toString();
                    if(searchBar.getText().toString().isEmpty())
                        filterString = null;
                    filterData();
                }
                return false;
            }
        });

        //Calendar listener
        final Calendar myCalendar = Calendar.getInstance();
        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

            //Detect date set
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                Log.i("Selected date: ", ""+myCalendar.getTime().toString());

                filterDate = myCalendar.getTime();
                filterData();
                DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH);
                listDate.setText(format.format(myCalendar.getTime()));
            }

        };

        dateFab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                new DatePickerDialog(MainActivity.this, date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        contentFab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                BottomBarModel bsm = new BottomBarModel();
                bsm.show(getSupportFragmentManager(), "bottomSheetModal ");
            }
        });

        //Fix listview scrolling up and down within a bottom sheet
        listView.setOnTouchListener(new ListView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow NestedScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow NestedScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<ParseClass> oneItem = new ArrayList<>();
                oneItem.add(getActiveItems().get(position));
                updateMap(activeItemType, oneItem, map);
                mapFocused = true;
            }
        });
    }

    //When the map is ready to be used
    @Override
    public void onMapReady(final GoogleMap map){
        this.map = map;
        //Set map ready to true
        map.setMaxZoomPreference(17.0f);
        mapReady = true;

        //Load in the data from the XML.
        //Load Current incidents to its array list
        new XmlPullParserHandler(currentIncidentsSource, RssItem.current_incidents, new ParseComplete() {
            @Override
            public void onParseComplete(ArrayList<ParseClass> items) {
                Log.i("XMLHelper", "Loaded current incidents");
                currentIncidentList = items;
            }
        });

        //Load roadworks to its array list
        new XmlPullParserHandler(roadworksSource, RssItem.current_roadworks, new ParseComplete() {
            @Override
            public void onParseComplete(ArrayList<ParseClass> items) {
                Log.i("XMLHelper", "Loaded roadworks");
                roadworksList = items;
                //The default active type of items to display, so populate these on the bottom sheet as the onCreate runs
                updateMap(RssItem.current_incidents, items, map);
            }
        });

        //Load planned roadworks to its array list
        new XmlPullParserHandler(plannedRoadworksSource, RssItem.roadworks_planned, new ParseComplete() {
            @Override
            public void onParseComplete(ArrayList<ParseClass> items) {
                Log.i("XMLHelper", "Loaded planned roadworks");
                plannedRoadworksList = items;
            }
        });

        /****/
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(mapFocused == true){
                    updateMap(activeItemType, getActiveItems(), map);
                    mapFocused = false;
                }
            }
        });
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public ArrayList<ParseClass> getActiveItems(){
        ArrayList<ParseClass> list = new ArrayList<ParseClass>();
        switch(activeItemType){
            case current_incidents:
                list = currentIncidentList;
                break;
            case current_roadworks:
                list = roadworksList;
                break;
            case roadworks_planned:
                list = plannedRoadworksList;
                break;
        }
        return list;
    }

    public void updateMap(RssItem type, ArrayList<ParseClass> items, GoogleMap map){
        //Clear the map
        map.clear();

        //Set the list in the bottom sheet
        adapter = new RssAdapter(MainActivity.this, items);
        listView.setAdapter(adapter);

        //Create new bounds
        LatLngBounds.Builder b = new LatLngBounds.Builder();

        //Set the markers on the map
        for(ParseClass item : items){
            LatLng latlng = new LatLng(item.getLat(),item.getLng());
            map.addMarker(new MarkerOptions().position(latlng).title(item.getTitle()));
            b.include(latlng);
        }

        if(items.size() > 0){
            LatLngBounds bounds = b.build();
            //Zoom on the map to fit all the markers
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20));
        }
        String message = "Showing data for " + items.size() + " items";
        if(filterString != null)
            message += " '" + filterString + "'";

        if(filterDate != null){
            DateFormat format = new SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH);
            message += " during " + format.format(filterDate);

        }
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

    }

    //RSS type selected, on click of item in bottomsheetmodal
    @Override
    public void onClicked(RssItem rssItemType) {
        activeItemType = rssItemType;
        updateMap(rssItemType, getActiveItems(), map);

        listDate.setText("");
        switch(rssItemType){
            case roadworks_planned:
                listHeading.setText("Roadworks Planned");
                break;
            case current_roadworks:
                listHeading.setText("Current Roadworks");
                break;
            case current_incidents:
                listHeading.setText("Current Incidents");
                break;
        }

    }

    //Filter the map data, with the stored global variables of the search and date conditions
    public void filterData(){
        //Make new temp list
        ArrayList<ParseClass> filteredList = new ArrayList<ParseClass>();
        //Loop over every item in the active list/map and add it to the local list if it meets condition
        for(ParseClass loopItem : getActiveItems()){
            //If a search condition has been entered and matches loop item, or if no search condition is entered:
            if(loopItem
                    .getTitle()
                    .toLowerCase()
                    .contains(
                            searchBar.getText()
                                    .toString()
                                    .toLowerCase())
                    || filterString == null){

                if(filterDate != null &&
                        loopItem.getStartDate().before(filterDate) &&
                        loopItem.getEndDate().after(filterDate))
                {
                    //Add the item
                    filteredList.add(loopItem);
                }else if(filterDate == null){
                    //Also add the item
                    filteredList.add(loopItem);
                }

            }
        }
        //Update the map with the new list
        updateMap(activeItemType, filteredList, map);
    }


} /**End of MainActivity**/