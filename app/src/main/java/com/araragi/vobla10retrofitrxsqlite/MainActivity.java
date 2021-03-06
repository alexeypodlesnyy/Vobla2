package com.araragi.vobla10retrofitrxsqlite;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.widget.Toast;

import com.araragi.vobla10retrofitrxsqlite.adapter.RecyclerViewFlightAdapter;
import com.araragi.vobla10retrofitrxsqlite.background.RetrofitSQLiteIntentService;
import com.araragi.vobla10retrofitrxsqlite.database.Dao;
import com.araragi.vobla10retrofitrxsqlite.eventbus.DbUpdatedEvent;
import com.araragi.vobla10retrofitrxsqlite.eventbus.TicketClickEvent;
import com.araragi.vobla10retrofitrxsqlite.model.Flight;
import com.araragi.vobla10retrofitrxsqlite.networking.FlightsRetrofitApi;
import com.araragi.vobla10retrofitrxsqlite.networking.FlightsRetrofitService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerViewFlightAdapter recyclerViewFlightAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private Dao dao;
    private LinkedList<Flight> flightsDataSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        recyclerView = (RecyclerView)findViewById(R.id.recycler_container);

        EventBus.getDefault().register(this);

        dao = new Dao(this);
        dao.open();

        flightsDataSet = dao.getAllFlightsNotDeleted();

//        Intent intent = new Intent(this, RetrofitSQLiteIntentService.class);
//        startService(intent);
        setUpRecyclerView();


    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDbUpdatedEvent(DbUpdatedEvent resultStatus){

        String status = resultStatus.resultOfDbUpdate;

        switch(status) {
            case DbUpdatedEvent.DB_UPDATED:

                LinkedList<Flight> localListFlights = dao.getAllFlightsNotDeleted();
                for (Flight flight : localListFlights) {

                    if (flightsDataSet.contains(flight)) {
                        flightsDataSet.addLast(flight);
                        int size = flightsDataSet.size();
                        recyclerViewFlightAdapter.notifyItemInserted(size - 1);
                        Log.i("Main", "---Notify item inserted :" + flight.toString() + "---");
                    } else {
                        Log.i("Main", "---Element is already in dataset :" + flight.toString() + "---");
                    }
                }
                Toast.makeText(this, "Database updated", Toast.LENGTH_LONG).show();
                break;
            case DbUpdatedEvent.NO_INTERNET:
                Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();
                break;
            case DbUpdatedEvent.DB_EXCEPTION:
                Toast.makeText(this, "Database error", Toast.LENGTH_LONG).show();
                break;

        }

    }
    @Subscribe
    public void onTicketClicked(TicketClickEvent position){

        int positionInt = Integer.parseInt(position.positionCliked);
        Flight flight = flightsDataSet.get(positionInt);
        String ticketUrl = flight.getTicketServiceUrl();
        Log.i("main", "----- Ticket URL: " + ticketUrl);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://stockreleaser.com" + ticketUrl));
        startActivity(intent);
    }



    @Override
    protected void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        recyclerView.setAdapter(null);
        dao.close();
    }

    private void setUpRecyclerView() {

        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerViewFlightAdapter = new RecyclerViewFlightAdapter(flightsDataSet, R.id.row_model);
        recyclerView.setAdapter(recyclerViewFlightAdapter);
        recyclerView.setLayoutManager(mLinearLayoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        TouchHelperCallback touchHelperCallback = new TouchHelperCallback();
        ItemTouchHelper touchHelper = new ItemTouchHelper(touchHelperCallback);
        touchHelper.attachToRecyclerView(recyclerView);
    }
    private class TouchHelperCallback extends ItemTouchHelper.SimpleCallback {

        TouchHelperCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return true;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            Flight flight = flightsDataSet.get(position);
            flightsDataSet.remove(position);
            recyclerViewFlightAdapter.notifyItemRemoved(position);
            dao.markAsDeleted(flight.getTravelOptionId());

        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }
    }

    }
