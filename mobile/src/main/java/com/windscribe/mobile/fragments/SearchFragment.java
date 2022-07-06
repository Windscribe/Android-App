/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.fragments;

import static android.content.Context.SEARCH_SERVICE;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup;
import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.ExpandedAdapter;
import com.windscribe.mobile.adapter.SearchRegionsAdapter;
import com.windscribe.mobile.windscribe.WindscribeActivity;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.ThemeUtils;
import com.windscribe.vpn.serverlist.entity.City;
import com.windscribe.vpn.serverlist.entity.ServerListData;
import com.windscribe.vpn.serverlist.entity.Group;
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;


@SuppressWarnings("rawtypes")
public class SearchFragment extends Fragment {

    private static final String EXPAND_STATE_MAP = "expandable_recyclerview_adapter_expand_state_map";

    @BindView(R.id.recycle_server_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.minimize_icon)
    ImageView minimizeBtn;

    @BindView(R.id.progress)
    ProgressBar progressBar;

    @BindView(R.id.searchView)
    SearchView searchView;

    private ServerListData serverListData;

    private ExpandedAdapter expandedAdapter;

    private List<? extends ExpandableGroup> groups;

    private int lastPositionSnapped = 0;

    private LinearLayoutManager linearLayoutManager;

    private ListViewClickListener listViewClickListener;

    private SearchRegionsAdapter regionsAdapter;

    public static SearchFragment newInstance(List<? extends ExpandableGroup> groups, ServerListData dataDetails,
            ListViewClickListener listViewClickListener) {
        return new SearchFragment(groups, dataDetails, listViewClickListener);
    }

    public SearchFragment(List<? extends ExpandableGroup> groups, ServerListData serverListData,
            ListViewClickListener listViewClickListener) {
        super();
        this.groups = groups;
        this.serverListData = serverListData;
        this.listViewClickListener = listViewClickListener;
    }

    public SearchFragment() {
        super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        if (getActivity() != null) {
            View view = inflater.inflate(R.layout.search_layout, container, false);
            ButterKnife.bind(this, view);
            return view;
        } else {
            return super.getView();
        }

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setUpCustomSearchBox(view);

        // Recycle view
        linearLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(linearLayoutManager);

        if (groups != null) {
            regionsAdapter = new SearchRegionsAdapter(groups, serverListData, listViewClickListener);
            mRecyclerView.setAdapter(regionsAdapter);
            expandedAdapter = new ExpandedAdapter(groups, serverListData, listViewClickListener);
        }

        setSearchView(true);
        setScrollHapticFeedback();

    }

    public void scrollTo(int scrollTo) {
        if (mRecyclerView.getAdapter() != null) {
            mRecyclerView.smoothScrollToPosition(scrollTo);
        }
    }

    public void setSearchView(boolean open) {
        if (getActivity() != null) {
            final SearchManager searchManager = (SearchManager) getActivity().getSystemService(SEARCH_SERVICE);
            if (open) {
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
                searchView.setIconifiedByDefault(false);
                searchView.setFocusable(true);
                searchView.setIconified(false);
                searchView.requestFocus();
            } else {
                if (getActivity() != null) {
                    searchView.clearFocus();
                    searchView.setQuery("", false);
                    getActivity().onBackPressed();
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateDataSet(ServerListData serverListData) {
        regionsAdapter.setServerListData(serverListData);
        expandedAdapter.setServerListData(serverListData);
        regionsAdapter.notifyDataSetChanged();
        expandedAdapter.notifyDataSetChanged();
    }

    private Group filterIfContains(Group group, String keyword) {
        List<City> cities = new ArrayList<>();
        if (group.getItems() != null) {
            for (Object item : group.getItems()) {
                City city = (City) item;
                if (city.getNickName().toLowerCase().contains(keyword.toLowerCase()) | city.getNodeName()
                        .toLowerCase().contains(keyword.toLowerCase())) {
                    cities.add(city);
                }
            }
        }

        if (cities.size() > 0) {
            return new Group(group.getTitle(), group.getRegion(), cities, group.getLatencyAverage());
        }

        boolean groupFound = false;
        if (group.getTitle().toLowerCase().contains(keyword.toLowerCase())) {
            groupFound = true;
        }
        if (cities.size() == 0 && groupFound) {
            return group;
        }
        return null;
    }

    private boolean filterIfStartWith(Group group, String keyword) {
        if (group.getTitle().toLowerCase().startsWith(keyword.toLowerCase())) {
            return true;
        }
        if (group.getItems() != null) {
            for (Object item : group.getItems()) {
                City city = (City) item;
                if (city.getNickName().toLowerCase().startsWith(keyword.toLowerCase()) | city.getNodeName()
                        .toLowerCase().startsWith(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Comparator<Group> getComparator(String part) {
        return (o1, o2) -> {
            boolean containsFirst = filterIfStartWith(o1, part);
            boolean containsSecond = filterIfStartWith(o2, part);

            if (containsFirst && !containsSecond) {
                return -1;
            }

            if (!containsFirst && containsSecond) {
                return 1;
            }

            return 0;
        };
    }

    private void setScrollHapticFeedback() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int centerView = linearLayoutManager.findFirstVisibleItemPosition();
                updateAdapterPosition(centerView);
            }
        });
    }

    private void setUpCustomSearchBox(View view) {
        // Search view
        searchView.setIconifiedByDefault(false);
        searchView.setQueryHint("Search");
        searchView.setFocusable(false);
        // Filter results on text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String s) {
                if (regionsAdapter != null) {
                    // Group list
                    List<Group> groupList = new ArrayList<>();
                    for (ExpandableGroup expandableGroup : groups) {
                        Group group = (Group) expandableGroup;
                        groupList.add(group);
                    }
                    // Group list sorted by keyword
                    Collections.sort(groupList, getComparator(s));

                    // Only keep item with keyword
                    List<Group> updatedList = new ArrayList<>();
                    for (Group group : groupList) {
                        Group filteredGroup = filterIfContains(group, s);
                        if (filteredGroup != null) {
                            updatedList.add(filteredGroup);
                        }
                    }
                    if (updatedList.size() < groups.size()) {
                        expandedAdapter = new ExpandedAdapter(updatedList,
                                serverListData, listViewClickListener);
                        Bundle bundle = new Bundle();
                        boolean[] states = new boolean[updatedList.size()];
                        for (int i = 0; i < updatedList.size(); i++) {
                            states[i] = true;
                        }
                        bundle.putBooleanArray(EXPAND_STATE_MAP, states);
                        expandedAdapter.onRestoreInstanceState(bundle);
                        mRecyclerView.setAdapter(expandedAdapter);
                    } else {
                        regionsAdapter = new SearchRegionsAdapter(groups,
                                serverListData, listViewClickListener);
                        mRecyclerView.setAdapter(regionsAdapter);
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String s) {
                searchView.clearFocus();
                return true;
            }
        });

        // Search text
        TextView searchText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        int color = ThemeUtils.getColor(searchView.getContext(), R.attr.nodeListGroupTextColor, R.color.colorWhite40);
        searchText.setTextColor(color);
        searchText.setHintTextColor(color);
        searchText.setTextSize(Dimension.SP, 12);
        Typeface typeface = ResourcesCompat.getFont(view.getContext(), R.font.ibm_plex_sans_bold);
        searchText.setTypeface(typeface);
        searchText.setPadding(0, 0, 0, 0);

        // Close button
        ImageView closeButton = this.searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setPadding(0, 0, 0, 0);
        closeButton.setOnClickListener(v -> {
            searchView.clearFocus();
            searchView.setQuery("", false);
            regionsAdapter = new SearchRegionsAdapter(groups, serverListData, listViewClickListener);
            mRecyclerView.setAdapter(regionsAdapter);
        });
        // Search icon
        ImageView searchIcon = this.searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchIcon.setPadding(0, 0, 0, 0);
        searchIcon.setScaleType(ImageView.ScaleType.FIT_START);
        searchIcon.setImageTintList(ColorStateList.valueOf(color));

        minimizeBtn.setImageTintList(ColorStateList.valueOf(color));
        minimizeBtn.setOnClickListener(v -> {
            if (getActivity() != null) {
                setSearchView(false);
            }
        });

    }

    private void updateAdapterPosition(int position) {
        boolean hapticFeedbackEnabled = Windscribe.getAppContext().getPreference().isHapticFeedbackEnabled();
        if (position == lastPositionSnapped | !hapticFeedbackEnabled) {
            return;
        }
        Vibrator vibrator = (Vibrator) Objects.requireNonNull(getActivity())
                .getSystemService(Context.VIBRATOR_SERVICE);
        if (getActivity() instanceof WindscribeActivity && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect vibrationEffect = VibrationEffect.createOneShot(8, 255);
                if (vibrationEffect != null) {
                    try {
                        vibrator.vibrate(vibrationEffect);
                    } catch (Exception ignored) {
                    }
                }

            } else {
                AudioAttributes audioAttributes = new AudioAttributes.Builder().build();
                vibrator.vibrate(8, audioAttributes);
            }
        }
        lastPositionSnapped = position;
    }
}