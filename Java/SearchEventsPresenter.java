package com.example.events.list.presenter;


import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.spot101.industryevents.R;
import com.spot101.industryevents.base.mvp.BaseMVPPresenter;
import com.spot101.industryevents.events.general.object.EventObject;
import com.spot101.industryevents.events.list.presenter.model.Search;
import com.spot101.industryevents.filters.base.object.Industry;
import com.spot101.industryevents.utils.Constants;
import com.spot101.industryevents.utils.ExtraUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import rx.Observable;

public class SearchEventsPresenter extends BaseMVPPresenter<SearchEventsCallback.ListEventsView> implements SearchEventsCallback.ListAction {
    private final static int EVENTS_PER_PAGE = 30;
    private final static int TRAINING_PER_PAGE = EVENTS_PER_PAGE;
    private final static int NEWS_PER_PAGE = EVENTS_PER_PAGE;///2;

    @Override
    public void defaultSearch(Context context, int page) {
        getMvpView().showDialog();
        checkPageNumber(context, page);

        HashMap<String, String> eventOptions = new HashMap<>();
        eventOptions.put(Constants.API_PAGE, String.valueOf(page));
        eventOptions.put(Constants.API_START_DATE, ExtraUtils.getFormatedNowDate());

        HashMap<String, String> newsOptions = new HashMap<>(eventOptions);
        HashMap<String, String> trainingOptions = new HashMap<>(eventOptions);
        trainingOptions.remove(Constants.API_START_DATE);

        HashMap<String, String[]> arrayEventOptions = new HashMap<>();
        List<String> indList = new ArrayList<>();
        List<Industry> tempIndustryList = Industry.listAll(Industry.class);

        Observable.from(tempIndustryList)
                .filter(Industry::isSelected)
                .subscribe(industry -> indList.add(industry.getPermalink()));
        String[] industries = new String[indList.size()];
        industries = indList.toArray(industries);
        arrayEventOptions.put(Constants.API_INDUSTRIES, industries);

        HashMap<String, String[]> arrayNewsOptions = new HashMap<>(arrayEventOptions);

        eventOptions.put(Constants.API_PER_PAGE, String.valueOf(EVENTS_PER_PAGE));
        newsOptions.put(Constants.API_PER_PAGE, String.valueOf(NEWS_PER_PAGE));
        trainingOptions.put(Constants.API_PER_PAGE, String.valueOf(TRAINING_PER_PAGE));

        String state = ExtraUtils.getStringPreference(context, Constants.PREF_REGION_NAME_PERMALINK).toLowerCase();
        if (!TextUtils.isEmpty(state)) {
            eventOptions.put(Constants.API_STATE, state);
            startDefaultStateSearch(context, eventOptions, newsOptions, trainingOptions, arrayEventOptions, arrayNewsOptions, arrayNewsOptions);
        } else {
            String[] countryCodes = new String[1];
            countryCodes[0] = ExtraUtils.getStringPreference(context, Constants.PREF_CURRENT_COUNTRY_CODE).toLowerCase();
            arrayEventOptions.put(Constants.API_COUNTRY_CODES, countryCodes);

            startDefaultSearch(context, eventOptions, newsOptions, trainingOptions, arrayEventOptions, arrayNewsOptions, arrayNewsOptions);
        }
    }

    private void startDefaultSearch(Context context,
                                    HashMap<String, String> eventOptions,
                                    HashMap<String, String> newsOptions,
                                    HashMap<String, String> trainingsOptions,
                                    HashMap<String, String[]> arrayEventOptions,
                                    HashMap<String, String[]> arrayNewsOptions,
                                    HashMap<String, String[]> arrayTrainingsOptions) {
        Observable.zip(
                Search.Creator.defaultEventsSearch(context, arrayEventOptions, eventOptions),
                Search.Creator.defaultNewsSearch(context, arrayNewsOptions, newsOptions),
                Search.Creator.defaultTrainingSearch(context, arrayTrainingsOptions, trainingsOptions),
                this::getEventListByLists)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    private void startDefaultStateSearch(Context context,
                                    HashMap<String, String> eventOptions,
                                    HashMap<String, String> newsOptions,
                                    HashMap<String, String> trainingsOptions,
                                    HashMap<String, String[]> arrayEventOptions,
                                    HashMap<String, String[]> arrayNewsOptions,
                                    HashMap<String, String[]> arrayTrainingsOptions) {
        Observable.zip(
                Search.Creator.defaultEventsStateSearch(context, arrayEventOptions, eventOptions),
                Search.Creator.defaultNewsSearch(context, arrayNewsOptions, newsOptions),
                Search.Creator.defaultTrainingSearch(context, arrayTrainingsOptions, trainingsOptions),
                this::getEventListByLists)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    @Override
    public void keywordSearch(Context context, String query, int page) {
        getMvpView().showDialog();
        checkPageNumber(context, page);

        HashMap<String, String> eventOptions = new HashMap<>();
        eventOptions.put(Constants.API_QUERY, query);
        eventOptions.put(Constants.API_PAGE, String.valueOf(page));

        HashMap<String, String> newsOptions = new HashMap<>(eventOptions);

        eventOptions.put(Constants.API_PER_PAGE, String.valueOf(EVENTS_PER_PAGE));
        newsOptions.put(Constants.API_PER_PAGE, String.valueOf(NEWS_PER_PAGE));

        startKeywordSearch(context, eventOptions, newsOptions/*, null, arrayNewsOptions*/);
    }

    private void startKeywordSearch(Context context,
                                    HashMap<String, String> eventOptions,
                                    HashMap<String, String> newsOptions) {
        Observable.zip(
                Search.Creator.keywordEventsSearch(context, eventOptions),
                Search.Creator.keywordNewsSearch(context, newsOptions),
                (list1, list2) -> {
                    return getEventListByLists(list1, list2, null);
                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    @Override
    public void refineSearch(Context context, int page) {
        getMvpView().showDialog();
        checkPageNumber(context, page);

        HashMap<String, String> eventOptions = new HashMap<>();
        eventOptions.put(Constants.API_PAGE, String.valueOf(page));

        String startDate = ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_START_DATE);

        String state = ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_STATE_COUNTRY);
        if (state.equals("all"))
            state = "";

        if (!startDate.isEmpty())
            eventOptions.put(Constants.API_START_DATE, startDate);
        if (!state.isEmpty())
            eventOptions.put(Constants.API_STATE, state);

        HashMap<String, String> newsOptions = new HashMap<>(eventOptions);
        HashMap<String, String> trainingOptions = new HashMap<>(eventOptions);
        newsOptions.remove(Constants.API_STATE);
        trainingOptions.remove(Constants.API_START_DATE);
        trainingOptions.remove(Constants.API_STATE);

        eventOptions.put(Constants.API_PER_PAGE, String.valueOf(EVENTS_PER_PAGE));
        newsOptions.put(Constants.API_PER_PAGE, String.valueOf(NEWS_PER_PAGE));
        trainingOptions.put(Constants.API_PER_PAGE, String.valueOf(TRAINING_PER_PAGE));

        HashMap<String, String[]> arrayEventsOptions = new HashMap<>();

        if(!ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_COUNTRY_IDENTIFIER).isEmpty()) {
            String[] countryString = new String[1];
            int countryIdentifier = Integer.valueOf(ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_COUNTRY_IDENTIFIER).toLowerCase());
            if (countryIdentifier == 0) {
                String countryCode = ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_COUNTRY_CODE).toLowerCase();
                countryString[0] = countryCode;
                arrayEventsOptions.put(Constants.API_COUNTRY_CODES, countryString);
            } else {
                String statePermalink = ExtraUtils.getStringPreference(context, Constants.FILTER_REGION_PERMALINK_CODE).toLowerCase();
                countryString[0] = statePermalink;
                arrayEventsOptions.put(Constants.API_STATE, countryString);
            }
        }
        String[] regionName = new String[1];
        regionName[0] = ExtraUtils.getStringPreference(context, Constants.FILTER_VALUE_REGION);
        if (regionName[0].equals("all"))
            regionName[0] = "";
        if (!TextUtils.isEmpty(regionName[0])) {
            arrayEventsOptions.put(Constants.API_REGION_NAME, regionName);
            arrayEventsOptions.remove(Constants.API_COUNTRY_CODES);
            arrayEventsOptions.remove(Constants.API_STATE);
        }

        Set<String> setFilterHomeIndustry = ExtraUtils.getSetStringPreference(context, Constants.FILTER_VALUE_INDUSTRY);
        String[] industries = setFilterHomeIndustry.toArray(new String[setFilterHomeIndustry.size()]);
        arrayEventsOptions.put(Constants.API_INDUSTRIES, industries);

        Set<String> setEventType = ExtraUtils.getSetStringPreference(context, Constants.FILTER_VALUE_TYPE);
        String[] eventTypes = setEventType.toArray(new String[setEventType.size()]);

        HashMap<String, String[]> arrayNewsOptions = new HashMap<>(arrayEventsOptions);
        HashMap<String, String[]> arrayTrainingOptions = new HashMap<>(arrayEventsOptions);

        arrayNewsOptions.put(Constants.API_EVENT_TYPES, Search.NEWS_TYPE_SEARCH);
        arrayNewsOptions.remove(Constants.API_COUNTRY_CODES);
        arrayNewsOptions.remove(Constants.API_STATE);
        arrayNewsOptions.remove(Constants.API_REGION_NAME);

        arrayTrainingOptions.put(Constants.API_EVENT_TYPES, Search.TRAINING_TYPE_SEARCH);
        arrayTrainingOptions.remove(Constants.API_COUNTRY_CODES);
        arrayTrainingOptions.remove(Constants.API_STATE);
        arrayTrainingOptions.remove(Constants.API_REGION_NAME);

        if (eventTypes.length == 0 || eventTypes[0].equals("all")) { //3 API CALLS
            arrayEventsOptions.put(Constants.API_EVENT_TYPES, Search.EVENT_DEFAULT_TYPES);
            startRefineSearch(context, eventOptions, newsOptions, trainingOptions, arrayEventsOptions, arrayNewsOptions, arrayTrainingOptions);
        } else if (eventTypes[0].toLowerCase().equals(Search.TRAINING_TYPE_SEARCH[0])){//2 API CALLS (trainingonline, news)
            startTrainingOnlineRefineSearch(context, newsOptions, trainingOptions, arrayNewsOptions, arrayTrainingOptions);
        } else if (eventTypes[0].toLowerCase().equals(Search.NEWS_TYPE_SEARCH[0])) {//1 API CALL (news)
            startNewsRefineSearch(context, newsOptions, arrayNewsOptions);
        } else {//2 API CALLS(event/news)
            arrayEventsOptions.put(Constants.API_EVENT_TYPES, eventTypes);
            startRefineSearch(context, eventOptions, newsOptions, arrayEventsOptions, arrayNewsOptions);
        }
    }

    private void startRefineSearch(Context context,
                                    HashMap<String, String> eventOptions,
                                    HashMap<String, String> newsOptions,
                                    HashMap<String, String> trainingsOptions,
                                    HashMap<String, String[]> arrayEventOptions,
                                    HashMap<String, String[]> arrayNewsOptions,
                                    HashMap<String, String[]> arrayTrainingsOptions) {
        Observable.zip(
                Search.Creator.refineEventsSearch(context, arrayEventOptions, eventOptions),
                Search.Creator.refineNewsSearch(context, arrayNewsOptions, newsOptions),
                Search.Creator.refineTrainingSearch(context, arrayTrainingsOptions, trainingsOptions),
                this::getEventListByLists)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    private void startTrainingOnlineRefineSearch(Context context,
                                   HashMap<String, String> newsOptions,
                                   HashMap<String, String> trainingsOptions,
                                   HashMap<String, String[]> arrayNewsOptions,
                                   HashMap<String, String[]> arrayTrainingsOptions) {
        Observable.zip(
                Search.Creator.refineNewsSearch(context, arrayNewsOptions, newsOptions),
                Search.Creator.refineTrainingSearch(context, arrayTrainingsOptions, trainingsOptions),
                (list1, list2) -> {
                    return getEventListByLists(list1, list2, null);
                })
                .subscribe(list -> listProcessing(context, list), this::listError);
    }


    private void startNewsRefineSearch(Context context, HashMap<String, String> newsOptions, HashMap<String, String[]> arrayNewsOptions) {
        Search.Creator.refineNewsSearch(context, arrayNewsOptions, newsOptions)
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    private void startRefineSearch(Context context,
                                   HashMap<String, String> eventOptions,
                                   HashMap<String, String> newsOptions,
                                   HashMap<String, String[]> arrayEventOptions,
                                   HashMap<String, String[]> arrayNewsOptions) {
        Observable.zip(
                Search.Creator.refineEventsSearch(context, arrayEventOptions, eventOptions),
                Search.Creator.refineNewsSearch(context, arrayNewsOptions, newsOptions),
                (list1, list2) -> {
                    return getEventListByLists(list1, list2, null);
                })
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    @Override
    public void relatedSearch(Context context, int page, String[] industries) {
        getMvpView().showDialog();
        if (page == 1)
            ExtraUtils.putPreference(context, Constants.PREF_EVENTS_RELATED_COUNT, -1);

        HashMap<String, String> eventOptions = new HashMap<>();
        HashMap<String, String[]> arrayEventOptions = new HashMap<>();
        eventOptions.put(Constants.API_PAGE, String.valueOf(page));
        arrayEventOptions.put(Constants.API_INDUSTRIES, industries);

        HashMap<String, String> trainingOptions = new HashMap<>(eventOptions);
        HashMap<String, String[]> arrayTrainingOptions = new HashMap<>(arrayEventOptions);

        HashMap<String, String> newsOptions = new HashMap<>(eventOptions);
        HashMap<String, String[]> arrayNewsOptions = new HashMap<>(arrayEventOptions);


        eventOptions.put(Constants.API_PER_PAGE, String.valueOf(EVENTS_PER_PAGE));
        trainingOptions.put(Constants.API_PER_PAGE, String.valueOf(TRAINING_PER_PAGE));
        newsOptions.put(Constants.API_PER_PAGE, String.valueOf(NEWS_PER_PAGE));

        startRelatedSearch(context,
                eventOptions, trainingOptions, newsOptions,
                arrayEventOptions, arrayTrainingOptions, arrayNewsOptions);
    }

    private void startRelatedSearch(Context context,
                                    HashMap<String, String> eventOptions,
                                    HashMap<String, String> trainingOptions,
                                    HashMap<String, String> newsOptions,
                                    HashMap<String, String[]> arrayEventOptions,
                                    HashMap<String, String[]> arrayTrainingOptions,
                                    HashMap<String, String[]> arrayNewsOptions) {
        Observable.zip(
                Search.Creator.relatedEventsSearch(context, arrayEventOptions, eventOptions),
                Search.Creator.relatedTrainingSearch(context, arrayTrainingOptions, trainingOptions),
                Search.Creator.relatedNewsSearch(context, arrayNewsOptions, newsOptions),
                (list1, list2, list3) -> {
                    return getEventListByLists(list1, list2, list3);
                })
                .subscribe(list -> listProcessing(context, list), this::listError);
    }

    private List<EventObject> getEventListByLists(List<EventObject> list1, List<EventObject> list2, List<EventObject> list3){
        List<EventObject> newList = new ArrayList<>();
        if (list1 != null && !list1.isEmpty())
            newList.addAll(list1);
        if (list2 != null && !list2.isEmpty())
            newList.addAll(list2);
        if (list3 != null && !list3.isEmpty())
            newList.addAll(list3);

        return newList;
    }


    private void checkPageNumber(Context context, int pageNumber){
        if (pageNumber == 1)
            ExtraUtils.putPreference(context, Constants.PREF_EVENTS_COUNT, -1);
    }

    private void listProcessing(Context ctx, List<EventObject> list) {
        if (list == null || list.isEmpty())
            getMvpView().showError(ctx.getString(R.string.events_not_found_error));
        ExtraUtils.putPreference(ctx, Constants.API_ES_JSON, new Gson().toJson(list));
        getMvpView().showEvents(list);
        getMvpView().hideDialog();
    }

    private void listError(Throwable error) {
        getMvpView().hideDialog();
        getMvpView().showError(error.getLocalizedMessage());
    }
}
