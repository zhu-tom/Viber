package com.tomzhu.viber.ui.search;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tomzhu.viber.MainActivity;
import com.tomzhu.viber.R;
import com.tomzhu.viber.models.ProtectedResult;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class SearchResultsFragment extends Fragment {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<ProtectedResult> searchResults = new ArrayList<>();
    private FirebaseDatabase db;


    private final static String TAG = "SearchResultsFragment";

    public SearchResultsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.search_results_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.HORIZONTAL));

        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        mAdapter = new SearchResultsViewAdapter(searchResults.toArray());
        recyclerView.setAdapter(mAdapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        db = FirebaseDatabase.getInstance();

        return inflater.inflate(R.layout.fragment_search_results, container, false);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // TODO: Use the ViewModel
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.app_bar_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setBackgroundColor(getResources().getColor(R.color.common_google_signin_btn_text_light, getActivity().getTheme()));
        searchView.setIconifiedByDefault(false);
        searchItem.expandActionView();
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getFragmentManager().popBackStack();
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Log.i(TAG, "Query Changed");
                searchResults = new ArrayList<>();
                if (newText.equals("")) {
                    ((SearchResultsViewAdapter) mAdapter).setmDataset(new Object[0]);
                    mAdapter.notifyDataSetChanged();
                } else {
                    db.getReference().child("Users").orderByChild("username").startAt(newText).endAt(newText + "\uf8ff").limitToFirst(3).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                            Object url = dataSnapshot.child("photoUrl").getValue();
                            //if (url != null) Log.i(TAG, url.toString() + dataSnapshot.child("username").getValue().toString());
                            ProtectedResult user = new ProtectedResult(dataSnapshot.child("username").getValue().toString(), dataSnapshot.getKey(), url != null ? url.toString():null);
                            searchResults.add(user);
                            //Log.i(TAG, "Added");
                            ((SearchResultsViewAdapter) mAdapter).setmDataset(searchResults.toArray());
                            mAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
                return false;
            }
        });
    }
}
