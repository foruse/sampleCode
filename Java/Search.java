package com.example.events.list.presenter.model;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.spot101.industryevents.api.ResponseObject;
import com.spot101.industryevents.events.detail.api.EventApi;
import com.spot101.industryevents.events.detail.api.EventDetailResponseObject;
import com.spot101.industryevents.events.detail.api.IEventApi;
import com.spot101.industryevents.events.general.object.EventObject;
import com.spot101.industryevents.utils.Constants;
import com.spot101.industryevents.utils.ExtraUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by user on 8/9/16.
 */
public class Search extends DefaultBase {
    public static final int TYPE_SEARCH_NEW = 1, TYPE_SEARCH = 2;
    public final static String[] NEWS_TYPE_SEARCH = new String[] {"news"};
    public final static String[] TRAINING_TYPE_SEARCH = new String[] {"trainingonline"};
    public final static String[] EVENT_DEFAULT_TYPES = {"conference",
                                                        "convention",
                                                        "tradeshow",
                                                        "tour",
                                                        "meetup",
                                                        "trainingface"
    };

    public Search(Subscriber subscriber) {
        super(subscriber);
    }

    public void onDefaultEventsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx,
               getEventApi().searchDefaultEvent(getApiKey(ctx), arrayOptions.get(Constants.API_COUNTRY_CODES),
                        arrayOptions.get(Constants.API_INDUSTRIES), EVENT_DEFAULT_TYPES, options),
               Constants.PREF_EVENTS_COUNT);
    }

    public void onDefaultEventsStateSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx,
               getEventApi().searchEvent(getApiKey(ctx), arrayOptions.get(Constants.API_INDUSTRIES),
                       EVENT_DEFAULT_TYPES, options),
               Constants.PREF_EVENTS_COUNT);
    }

    public void onDefaultNewsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx,
               getEventApi().searchEvent(getApiKey(ctx), arrayOptions.get(Constants.API_INDUSTRIES),
                       NEWS_TYPE_SEARCH, options),
               Constants.PREF_EVENTS_COUNT);
    }

    public void onDefaultTrainingSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){ //without location
        search(ctx,
               getEventApi().searchEvent(getApiKey(ctx), arrayOptions.get(Constants.API_INDUSTRIES),
                       TRAINING_TYPE_SEARCH, options),
               Constants.PREF_EVENTS_COUNT);
    }


    public void onKeywordEventsSearch(Context ctx, HashMap<String, String> options) {
        search(ctx, getEventApi().searchEvent(getApiKey(ctx), options), Constants.PREF_EVENTS_COUNT);
    }

    public void onKeywordNewsSearch(Context ctx, HashMap<String, String> options) {
        search(ctx, getEventApi().searchEventByTypes(getApiKey(ctx), NEWS_TYPE_SEARCH, options), Constants.PREF_EVENTS_COUNT);
    }


    public void onRefineEventsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchEvent(getApiKey(ctx),
                arrayOptions.get(Constants.API_COUNTRY_CODES),
                arrayOptions.get(Constants.API_STATE),
                arrayOptions.get(Constants.API_INDUSTRIES),
                arrayOptions.get(Constants.API_REGION_NAME),
                arrayOptions.get(Constants.API_EVENT_TYPES),
                options),
            Constants.PREF_EVENTS_COUNT);
    }

    public void onRefineNewsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchRefineEvent(getApiKey(ctx),
                arrayOptions.get(Constants.API_INDUSTRIES),
                NEWS_TYPE_SEARCH,
                options),
            Constants.PREF_EVENTS_COUNT);
    }

    public void onRefineTrainingSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchRefineEvent(getApiKey(ctx),
                arrayOptions.get(Constants.API_INDUSTRIES),
                TRAINING_TYPE_SEARCH,
                options),
            Constants.PREF_EVENTS_COUNT);
    }

    public void onRelatedEventsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchRelated(getApiKey(ctx),
                arrayOptions.get(Constants.API_INDUSTRIES),
                EVENT_DEFAULT_TYPES,
                options),
            Constants.PREF_EVENTS_RELATED_COUNT);
    }

    public void onRelatedTrainingSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchRelated(getApiKey(ctx),
                arrayOptions.get(Constants.API_INDUSTRIES),
                TRAINING_TYPE_SEARCH,
                options),
                Constants.PREF_EVENTS_RELATED_COUNT);
    }

    public void onRelatedNewsSearch(Context ctx, HashMap<String, String[]> arrayOptions, HashMap<String, String> options){
        search(ctx, getEventApi().searchRelated(getApiKey(ctx),
                arrayOptions.get(Constants.API_INDUSTRIES),
                NEWS_TYPE_SEARCH,
                options),
            Constants.PREF_EVENTS_RELATED_COUNT);
    }


    @SuppressWarnings("unchecked")
    private void search(Context context, Call<ResponseObject> call, String preferenceCountName){
        call.enqueue(new Callback<ResponseObject>() {
            @Override
            public void onResponse(Call<ResponseObject> call, Response<ResponseObject> response) {
                SearchEventsListResponse.Body bodyResults = (new Gson().fromJson(response.body().getResponse(),
                        SearchEventsListResponse.class)).getBody();

                if (bodyResults != null) {
                    SearchEventsListResponse.Body.Results[] results = bodyResults.getResults();

                    if (results != null && results.length > 0) {

                        int eventsCountPref = ExtraUtils.getIntPreference(context, preferenceCountName);
                        if (eventsCountPref == -1)
                            eventsCountPref = bodyResults.getTotalMatched();
                        else
                            eventsCountPref += bodyResults.getTotalMatched();
                        ExtraUtils.putPreference(context, preferenceCountName, eventsCountPref);

                        LinkedList<EventObject> list = new LinkedList<>();

                        for (int i = 0; i < results.length; i++) {

                            String industStr = "";
                            String[] industArr = results[i].getIndustries();
                            if (industArr != null) {
                                for (int j = 0; j < industArr.length; j++)
                                    industStr += industArr[j] + ", ";
                                if (industStr.length() > 0)
                                    industStr = industStr.substring(0, industStr.length() - 2);
                            }


                            String stDate = "";
                            String endDate = "";
                            SimpleDateFormat format = new SimpleDateFormat();
                            try {
                                stDate = results[i].getStartDate();
                                if (!TextUtils.isEmpty(stDate)) {
                                    stDate = stDate.substring(0, 10);
                                    format.applyPattern("yyyy-MM-dd");
                                    Date startDate_ = format.parse(stDate);
                                    format.applyPattern("dd MMM");
                                    stDate = format.format(startDate_);
                                }

                                endDate = results[i].getEndDate();
                                if (!TextUtils.isEmpty(endDate)) {
                                    endDate = endDate.substring(0, 10);
                                    format.applyPattern("yyyy-MM-dd");
                                    Date endDate_ = format.parse(endDate);
                                    format.applyPattern("dd MMM yyyy");
                                    endDate = format.format(endDate_);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            String location = "";
                            if (!TextUtils.isEmpty(results[i].getCountryName()))
                                location += results[i].getCountryName() + ", ";
                            if (!TextUtils.isEmpty(results[i].getState()))
                                location += results[i].getState() + ", ";
                            if (!TextUtils.isEmpty(results[i].getCity()))
                                location += results[i].getCity() + ", ";
                            if (location.length() > 0)
                                location = location.substring(0, location.length() - 2);

                            String permalink = results[i].getPermalink();
                            String eventType = results[i].getEventType();
                            String logoUrl = results[i].getLogoUrl();
                            String title = results[i].getTitle();
                            String eventSource = results[i].getEventSource();
                            String registrationUrl = results[i].getRegistrationUrl();

                            list.add(new EventObject(
                                    title,
                                    stDate + " to " + endDate,
                                    location,
                                    industStr,
                                    logoUrl,
                                    permalink,
                                    eventType,
                                    eventSource,
                                    registrationUrl
                            ));

                        }
                        mSubscriber.onNext(list);

                    } else {
                        mSubscriber.onNext(null);
                    }
                } else
                    mSubscriber.onNext(null);

            }

            @Override
            public void onFailure(Call<ResponseObject> call, Throwable t) {
                t.printStackTrace();
                mSubscriber.onNext(null);
            }
        });
    }

    private IEventApi getEventApi() {
        return EventApi.event().create(IEventApi.class);
    }

    private String getApiKey(Context context) {
        return ExtraUtils.getStringPreference(context, Constants.PREF_API_KEY);
    }


    @SuppressWarnings("unchecked")
    public void onEventDetailSearch(Context context) {
        String permalink = ExtraUtils.getStringPreference(context, Constants.PREF_EVENT_DETAIL_PERMALINK); //
        String apiKey = ExtraUtils.getStringPreference(context, Constants.PREF_API_KEY); //appObjectList.get(0).getApiKey();

        IEventApi iEventApi = EventApi.event().create(IEventApi.class);
        Call<EventDetailResponseObject> call = iEventApi.getEventDetail(apiKey, permalink);
        call.enqueue(new Callback<EventDetailResponseObject>() {
            @Override
            public void onResponse(Call<EventDetailResponseObject> call, Response<EventDetailResponseObject> response) {
                SearchEventDetailsResponse.Body results = (new Gson().fromJson(response.body().getResponse(),
                        SearchEventDetailsResponse.class)).getBody();
                mSubscriber.onNext(results);
            }

            @Override
            public void onFailure(Call<EventDetailResponseObject> call, Throwable t) {
                mSubscriber.onError(t);
            }
        });
    }

    public static class Creator {
        public static Observable<List<EventObject>> defaultEventsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onDefaultEventsSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> defaultEventsStateSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onDefaultEventsStateSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> defaultNewsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.create(subscriber ->
                    new Search(subscriber).onDefaultNewsSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> defaultTrainingSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onDefaultTrainingSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> keywordEventsSearch(Context context, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onKeywordEventsSearch(context, options));
        }

        public static Observable<List<EventObject>> keywordNewsSearch(Context context, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onKeywordNewsSearch(context, options));
        }

        public static Observable<List<EventObject>> refineEventsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRefineEventsSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> refineNewsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRefineNewsSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> refineTrainingSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRefineTrainingSearch(context, arrayOptions, options));
        }

        public static Observable<SearchEventDetailsResponse.Body> eventDetailSearch(Context context) {
            return Observable.<SearchEventDetailsResponse.Body>create(subscriber ->
                    new Search(subscriber).onEventDetailSearch(context));
        }

        public static Observable<List<EventObject>> relatedEventsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRelatedEventsSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> relatedTrainingSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRelatedTrainingSearch(context, arrayOptions, options));
        }

        public static Observable<List<EventObject>> relatedNewsSearch(Context context, HashMap<String, String[]> arrayOptions, HashMap<String, String> options) {
            return Observable.<List<EventObject>>create(subscriber ->
                    new Search(subscriber).onRelatedNewsSearch(context, arrayOptions, options));
        }
    }
}
