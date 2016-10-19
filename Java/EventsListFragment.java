package com.spot101.industryevents.events.list;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.spot101.industryevents.R;

import com.spot101.industryevents.base.fragment.BaseButterKnifeFragment;
import com.spot101.industryevents.events.EventsActivity;
import com.spot101.industryevents.events.general.EventsListView;
import com.spot101.industryevents.events.general.adapter.EventsAdapter;
import com.spot101.industryevents.events.general.object.EventObject;
import com.spot101.industryevents.events.list.presenter.SearchEventsCallback;
import com.spot101.industryevents.events.list.presenter.SearchEventsPresenter;
import com.spot101.industryevents.events.list.presenter.model.Search;
import com.spot101.industryevents.events.sign.SignDialogFragment;
import com.spot101.industryevents.filters.SearchActivity;
import com.spot101.industryevents.filters.base.object.Industry;
import com.spot101.industryevents.filters.base.presenter.EventsListFilterContact;
import com.spot101.industryevents.profile.main.ProfileActivity;
import com.spot101.industryevents.social.helper.SocialLoginObject;
import com.spot101.industryevents.social.helper.SocialSubscribePresenter;
import com.spot101.industryevents.social.helper.SocialType;
import com.spot101.industryevents.utils.Constants;
import com.spot101.industryevents.utils.ExtraUtils;
import com.spot101.industryevents.utils.StringUtils;
import com.spot101.industryevents.utils.ViewUtils;
import com.spot101.industryevents.utils.recycler.TopSpaceItemDecoration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import butterknife.Optional;

public class EventsListFragment extends BaseButterKnifeFragment implements Serializable, EventsListFilterContact.View, SearchEventsCallback.ListEventsView{

    public static final int IE_LOCATION_PERMISSION = 6234;
    private static final String LIST_PREF_KEY = "list_key";
    public static final String QUERY_PREF_KEY = "query_key";
    private static final String PAGE_NUMBER_KEY = "page_number_key";

    @BindView(R.id.event_list_like_txt)
    protected TextView mLikeTxt;

    @BindView(R.id.event_list_header)
    protected View mListHeader;

    @BindView(R.id.event_list_recycler)
    protected EventsListView mEventList;


    @BindView(R.id.toolbar_profile_cancel)
    protected View mCancel;
    @BindView(R.id.toolbar_profile_clear_img)
    protected View mClear;
    @BindView(R.id.toolbar_profile_search_field)
    protected EditText mSearchField;

    @BindView(R.id.event_list_location_img)
    protected View mLocationImg;

    @BindView(R.id.event_list_industries_img)
    protected View mIndustriesImg;

    @BindView(R.id.event_list_location_txt)
    protected TextView mLocationTxt;

    @BindView(R.id.event_list_industries_txt)
    protected TextView mIndustriesTxt;

    @BindView(R.id.event_list_found)
    protected TextView mEventListFound;

    private EventsAdapter mEventAdapter;
    private SearchEventsPresenter mSearchEventsPresenter;
    private SocialSubscribePresenter mSubscribePresenter;
    private ProgressDialog mProgressDialog;
    private TopSpaceItemDecoration mItemDecoration;

    private int overallYScroll;

    private static int typeSearch;
    private static int mPageNumber = 1;

    private boolean mIsloading = true;
    int mPastVisibleItems, mVisibleItemCount, mTotalItemCount;
    int mTotalEventsOnServer;

    public static EventsListFragment newInstance(int ts, String queryString) {
        typeSearch = ts;
        EventsListFragment fragment = new EventsListFragment();
        if (!TextUtils.isEmpty(queryString)) {
            Bundle bundle = new Bundle();
            bundle.putString(QUERY_PREF_KEY, queryString);
            fragment.setArguments(bundle);
        }
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){

        outState.putInt("type_search", typeSearch);
        outState.putCharArray("search_field", mSearchField.getText().toString().toCharArray());
        outState.putInt(PAGE_NUMBER_KEY, mPageNumber);
        if (mEventAdapter != null)
            outState.putParcelableArrayList(LIST_PREF_KEY, (ArrayList<? extends Parcelable>) mEventAdapter.getList());
        super.onSaveInstanceState(outState);
    }

    @Override
    public int onCreateLayoutId() {
        return R.layout.event_list_fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLikeTxt.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
        mSearchEventsPresenter = new SearchEventsPresenter();
        mSearchEventsPresenter.attachView(this);
        getSavedData(savedInstanceState);

        long countStartApp = ExtraUtils.getLongPreference(getContext(), Constants.PREF_COUNT_START_APP);

        openGPSDialog(countStartApp);
        openNotificationDialog(countStartApp);

        mSubscribePresenter = new SocialSubscribePresenter(this);

        mSearchField.setOnKeyListener((View v, int keyCode, KeyEvent event) -> {
            if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)) {
                ExtraUtils.hideKeyboard(getActivity());
                String text = ((EditText)v).getText().toString();
                mPageNumber = 1;
                startSearch(text);
                return true;
            }
            return false;
        });
    }

    private void getSavedData(@Nullable Bundle savedInstanceState) {
        if(savedInstanceState != null) {
            typeSearch = savedInstanceState.getInt("type_search");
            mEventAdapter = new EventsAdapter(new ArrayList<>(), false);
            mSearchField.setText(savedInstanceState.getCharArray("search_field").toString());
            if (savedInstanceState.containsKey(PAGE_NUMBER_KEY))
                mPageNumber = savedInstanceState.getInt(PAGE_NUMBER_KEY);
            if (savedInstanceState.containsKey(LIST_PREF_KEY)) {
                showEvents(savedInstanceState.getParcelableArrayList(LIST_PREF_KEY));
                return;
            }
        }

        mPageNumber = 1;
        if (getArguments() != null && getArguments().containsKey(QUERY_PREF_KEY)) {
            String queryString = getArguments().getString(QUERY_PREF_KEY);
            mSearchField.setText(queryString);
            startSearch(queryString);
        } else {
            startSearch(null);
        }
    }

    private void openGPSDialog(long countStartApp) {
        if(countStartApp % 3 == 0 &&
                !((LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            if (!ExtraUtils.getBooleanPreference(getContext(), Constants.PREF_IS_GPS_DIALOG_SHOWED)) {
                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) &&
                            ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                IE_LOCATION_PERMISSION);
                    } else {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                IE_LOCATION_PERMISSION);
                    }
                    ExtraUtils.putPreference(getContext(), Constants.PREF_IS_GPS_DIALOG_SHOWED, true);
                }
            }
        } else
            ExtraUtils.putPreference(getContext(), Constants.PREF_IS_GPS_DIALOG_SHOWED, false);
    }

    private void openNotificationDialog(long countStartApp) {
        if (!ExtraUtils.getBooleanPreference(getContext(), Constants.PREF_IS_NOTIFICATION_PERMANENT_DISABLED)) {
            if (countStartApp % 4 == 0 &&
                    !ExtraUtils.getBooleanPreference(getContext(), Constants.API_NOTIFY_RELAVENT)) {

                if (!ExtraUtils.getBooleanPreference(getContext(), Constants.PREF_IS_NOTIFICATION_DIALOG_SHOWED)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                    dialog.setTitle(getResources().getString(R.string.title_push))
                            .setMessage(getResources().getString(R.string.message_push))
                            .setCancelable(false)
                            .setPositiveButton("Ok", (dialogInterface, id) -> {
                                dialogInterface.cancel();
                                ExtraUtils.putPreference(getContext(), Constants.PREF_IS_NOTIFICATION_PERMANENT_DISABLED, true);
                                ProfileActivity.newInstance(getActivity());
                            })
                            .setNegativeButton("Cancel", (dialogInterface, id) ->
                                    dialogInterface.cancel());
                    dialog.show();
                    ExtraUtils.putPreference(getContext(), Constants.PREF_IS_NOTIFICATION_DIALOG_SHOWED, true);
                }
            } else
                ExtraUtils.putPreference(getContext(), Constants.PREF_IS_NOTIFICATION_DIALOG_SHOWED, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case IE_LOCATION_PERMISSION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                } else {
                    showError(getString(R.string.permissions_not_granted));
                }
                return;
            }
        }
    }

    @OnClick({R.id.event_list_search, R.id.event_refine})
    protected void onSearchClick() {
        SearchActivity.newInstance(getActivity());
    }

    @OnClick({R.id.toolbar_profile_txt, R.id.toolbar_profile_img})
    protected void onProfileClick() {
        ProfileActivity.newInstance(getActivity());
    }

    @OnClick(R.id.toolbar_profile_clear_img)
    protected void onClearClick() {
        mSearchField.setText("");
    }

    @OnClick(R.id.toolbar_profile_cancel)
    protected void onCancelClick() {
        ViewUtils.hideKeyboard(getActivity());
        mCancel.setVisibility(View.GONE);
        mSearchField.setText("");
        mSearchField.clearFocus();
        typeSearch = Search.TYPE_SEARCH_NEW;
        startSearch(null);
    }

    @OnTextChanged(R.id.toolbar_profile_search_field)
    protected void onSearch(CharSequence text) {
        String str = text.toString();
        if (str.isEmpty()) {
            mClear.setVisibility(View.GONE);
        } else {
            mClear.setVisibility(View.VISIBLE);
        }
    }

    @OnFocusChange(R.id.toolbar_profile_search_field)
    protected void showCancelBtn(boolean hasFocus) {

        if (hasFocus) {
            mCancel.setVisibility(View.VISIBLE);
//            mLocationImg.setVisibility(View.INVISIBLE);
//            mLocationTxt.setVisibility(View.INVISIBLE);
//            mIndustriesImg.setVisibility(View.INVISIBLE);
//            mIndustriesTxt.setVisibility(View.INVISIBLE);
        } else {
//            mLocationImg.setVisibility(View.VISIBLE);
//            mLocationTxt.setVisibility(View.VISIBLE);
//            mIndustriesImg.setVisibility(View.VISIBLE);
//            mIndustriesTxt.setVisibility(View.VISIBLE);
        }
    }

    @Optional
    @OnClick(R.id.event_list_like_txt)
    protected void onFollowClick() {
        if(ExtraUtils.isOnline(getContext())) {
            List<SocialLoginObject> login = SocialLoginObject.listAll(SocialLoginObject.class);
            if (login.size() > 0) {
                SocialType mType = login.get(0).getType();
                mSubscribePresenter.subscribe(mType);
            } else
                SignDialogFragment.newInstance(new SignDialogFragment.SignSuccess() {
                    @Override
                    public void onSignSuccess() {
                        onFollowClick();
                    }

                    @Override
                    public int describeContents() {
                        return 0;
                    }

                    @Override
                    public void writeToParcel(Parcel dest, int flags) {

                    }
                }).show(getFragmentManager());
        } else {
            ExtraUtils.showNoInternetDialog(getContext());
        }

    }

    @Override
    public void updateItem(EventObject object) {
        int index = mEventAdapter.getList().indexOf(object);
        updateItem(index);
    }

    @Override
    public void updateItem(int pos) {
        if (pos >= 0) {
            mEventAdapter.notifyItemChanged(pos);
        }
    }

    @Override
    public void maxEventsListSelected() {

    }

    @Override
    public void setFilterString(String filterString) {
        mEventAdapter.filter(filterString);
    }

    @Override
    public void showDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            WindowManager.LayoutParams layoutParams = mProgressDialog.getWindow().getAttributes();
            layoutParams.format = PixelFormat.TRANSPARENT;
            mProgressDialog.getWindow().setAttributes(layoutParams);
            mProgressDialog.setMessage("Loading...");
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(true);
        }
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    @Override
    public void hideDialog() {
        if(mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    @Override
    public void showEvents(List<EventObject> list) {

        if (mPageNumber == 1) {
            String currentLocation;
            String industry = "";

            if(ExtraUtils.getIntPreference(getContext(), Constants.FILTER_REFINE) == Constants.ON_REFINE){
                if(!ExtraUtils.getStringPreference(getContext(), Constants.FILTER_STATE_COUNTRY).equals("All"))
                    currentLocation = ExtraUtils.getStringPreference(getContext(), Constants.FILTER_STATE_COUNTRY);
                else if(!ExtraUtils.getStringPreference(getContext(), Constants.FILTER_REGION).equals("All"))
                    currentLocation = ExtraUtils.getStringPreference(getContext(), Constants.FILTER_REGION);
                else
                    currentLocation = getString(R.string.worldwide);

                Set<String> setFilterHomeIndustry = ExtraUtils.getSetStringPreference(getContext(), Constants.FILTER_INDUSTRY);
                String[] strIndustry = setFilterHomeIndustry.toArray(new String[setFilterHomeIndustry.size()]);
                for(int i = 0; i < strIndustry.length; i++)
                    industry += strIndustry[i] + ", ";
                if(industry.length() > 0)
                    industry = industry.substring(0, industry.length() - 2);
                if(industry.equals("All")){
                    industry += " Industries";
                }
            } else {
                currentLocation = ExtraUtils.getStringPreference(getContext(), Constants.PREF_CURRENT_COUNTRY) + ", " +
                        ExtraUtils.getStringPreference(getContext(), Constants.PREF_CURRENT_STATE);// + ", " +
//                    ExtraUtils.getStringPreference(getContext(), Constants.PREF_CURRENT_CITY);

                List<Industry> industryList = Industry.listAll(Industry.class);

                for (Industry ind : industryList)
                    if (ind.isSelected())
                        industry += ind.getName() + ", ";
                if (industry.length() > 0)
                    industry = industry.substring(0, industry.length() - 2);
            }

            mLocationTxt.setText(currentLocation);
            mIndustriesTxt.setText(industry);

            mEventAdapter = new EventsAdapter(ExtraUtils.sortList(list), false);

            mTotalEventsOnServer = ExtraUtils.getIntPreference(getActivity(), Constants.PREF_EVENTS_COUNT);
            if (mTotalEventsOnServer == -1)
                mTotalEventsOnServer = list.size();

            mEventListFound.setText(getString(R.string.results_found, String.format(Locale.US, "%,d", mTotalEventsOnServer)));//.valueOf(mTotalEventsOnServer)));//list.size())));
            ExtraUtils.putPreference(getActivity(), Constants.PREF_EVENTS_COUNT, -1);

            mEventAdapter.setItemClickListener((view1, position) -> {
                if(ExtraUtils.isOnline(getContext())) {
                    boolean onAuth = ExtraUtils.getBooleanPreference(getContext(), Constants.PREF_ON_AUTH);

                    int eventType;
                    if (mEventAdapter.getItemByPos(position).getType() == EventObject.TYPE_NEWS) {
                        ExtraUtils.putPreference(getActivity(), Constants.PREF_NEWS_URL,
                                StringUtils.getFormatedUrl(mEventAdapter.getItemByPos(position).getRegistrationUrl()));
                        eventType = EventsActivity.TYPE_NEWS;
                    } else
                        eventType = EventsActivity.TYPE_DETAIL;
                    if (!onAuth)
                        SignDialogFragment.newInstance(eventType).show(getFragmentManager());
                    else {
                        if (eventType == EventsActivity.TYPE_NEWS)
                            ExtraUtils.putPreference(getActivity(), Constants.API_ES_JSON, new Gson().toJson(list));
                        EventsActivity.newInstance(getActivity(), eventType, typeSearch, null);
                    }
                } else {
                    ExtraUtils.showNoInternetDialog(getContext());
                }


            });

            mEventList.setAdapter(mEventAdapter);

            if (mItemDecoration != null)
                mEventList.removeItemDecoration(mItemDecoration);

            //count column in EventDetail
            if (getResources().getBoolean(R.bool.is_tablet)) {
                int columns = getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ? 2 : 3;
                configEventListByDevice(columns, mListHeader);
            } else {
                int columns = 1;
                configEventListByDevice(columns, getView().findViewById(R.id.event_list_result_header));
            }
        } else if (list != null && list.size() != 0){
            mEventAdapter.addItemRanged(ExtraUtils.sortList(list));
        }
        mIsloading = true;
    }

    private void openEventsActivity(int eventType) {
        EventsActivity.newInstance(getActivity(), eventType, typeSearch, null);
    }

    private void configEventListByDevice(int columns, View headerView) {
        int margin = getResources().getDimensionPixelSize(R.dimen.event_list_header_margin);
        int pixelOffset = getResources().getDimensionPixelSize(R.dimen.event_list_header_height) + margin * 2;
        LinearLayoutManager layoutManager = (LinearLayoutManager) mEventList.getLayoutManager();

        mItemDecoration = new TopSpaceItemDecoration(pixelOffset, columns);
        mEventList.addItemDecoration(mItemDecoration);
        mEventList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if(dy > 0) {

                    mVisibleItemCount = layoutManager.getChildCount();
                    mTotalItemCount = layoutManager.getItemCount();
                    mPastVisibleItems = layoutManager.findFirstVisibleItemPosition();

                    if (mIsloading && (mVisibleItemCount + mPastVisibleItems) >= mTotalItemCount && mTotalItemCount < mTotalEventsOnServer) {
                        mIsloading = false;
                        mPageNumber++;
                        String query = mSearchField.getText().toString();
                        startSearch(query);
                    }
                }
                overallYScroll = overallYScroll + dy;

                if (headerView != null && (headerView.getAlpha() == 1 || headerView.getAlpha() == 0.75f))
                    headerView.animate().alpha(overallYScroll > margin ? 0.75f : 1).setStartDelay(100).start();
            }
        });
    }

    private void startSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            if (typeSearch == Search.TYPE_SEARCH)
                mSearchEventsPresenter.refineSearch(getActivity().getApplicationContext(), mPageNumber);
            else {
                mSearchEventsPresenter.defaultSearch(getActivity().getApplicationContext(), mPageNumber);
            }
        } else
            mSearchEventsPresenter.keywordSearch(getActivity().getApplicationContext(), query, mPageNumber);
    }


    @Override
    public void showError(String error) {
        showMessage(error, true);
    }

    @Override
    public void onDestroyView() {
        mSearchEventsPresenter.detachView();
        super.onDestroyView();
    }
}
