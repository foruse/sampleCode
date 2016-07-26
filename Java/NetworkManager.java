package com.foruse.samplecode;

import android.content.Context;

import com.foruse.samplecode.authentification.LoginResult;
import com.foruse.samplecode.authentification.PerformForgotPasswordConfirm;
import com.foruse.samplecode.authentification.RegistrationResult;
import com.foruse.samplecode.authentification.session.Session;
import com.foruse.samplecode.authentification.session.SessionManager;
import com.foruse.samplecode.firebase.IDListenerService;
import com.foruse.samplecode.main.LoadAlertsResult;
import com.foruse.samplecode.main.LoadVehicleInfo;
import com.foruse.samplecode.main.ProfileInfoResult;
import com.foruse.samplecode.main.QuestionListLoaded;
import com.foruse.samplecode.main.SubjectListLoaded;
import com.foruse.samplecode.main.UpdateProfileResult;
import com.foruse.samplecode.main.UpdateVehicleResult;
import com.foruse.samplecode.messages.NotificationsLoaded;
import com.foruse.samplecode.messages.PerformAlertDialog;
import com.foruse.samplecode.model.Alerts;
import com.foruse.samplecode.model.Notification;
import com.foruse.samplecode.model.ProfileInfo;
import com.foruse.samplecode.model.VehicleInfo;
import com.foruse.samplecode.network.parsers.ChangePasswordParser;
import com.foruse.samplecode.network.parsers.ForgotPasswordParser;
import com.foruse.samplecode.network.parsers.LoadAlertsParser;
import com.foruse.samplecode.network.parsers.LoadProfileDataParser;
import com.foruse.samplecode.network.parsers.LoadQuestionParser;
import com.foruse.samplecode.network.parsers.LoadSubjectsParser;
import com.foruse.samplecode.network.parsers.LoadVehicleInfoParser;
import com.foruse.samplecode.network.parsers.LoginParser;
import com.foruse.samplecode.network.parsers.NotificationParser;
import com.foruse.samplecode.network.parsers.RegistrationParser;
import com.foruse.samplecode.network.parsers.UpdateProfileDataParser;
import com.foruse.samplecode.network.parsers.UpdateVehicleParser;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.greenrobot.eventbus.EventBus;
import java.util.List;
import cz.msebera.android.httpclient.Header;

public class NetworkManager {

    /** Singleton instance */
    private static volatile NetworkManager instance;

    /** Link to web API */
    private final String API_URL = BuildConfig.api_url;

    /**
     * NetworkManager will be singleton
     */
    private NetworkManager() {
    }

    /**
     * Method that will return single NetworkManager instance
     */
    public static NetworkManager getInstance() {
        NetworkManager instance = NetworkManager.instance;
        if (instance == null) {
            synchronized (NetworkManager.class) {
                instance = NetworkManager.instance;
                if (instance == null) {
                    NetworkManager.instance = instance = new NetworkManager();
                }
            }
        }
        return instance;
    }

    /**
     * Method that will send login request to web api
     * Response will be re received by EventBus
     *
     * @param login    login of user
     * @param password password of user
     * @param context  app context.
     */
    public void doLoginRequest(String login, String password, final Context context) {
        String url = API_URL + "login";
        RequestParams params = new RequestParams();
        params.add("username", login);
        params.add("pwd", password);
        params.add("device_token", password);
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Session session = new LoginParser().parse(bytes, context);
                if (session != null) {
                    SessionManager.putSession(session, context);
                    EventBus.getDefault().post(new LoginResult(LoginResult.RESULT_OK, session));
                } else {
                    EventBus.getDefault().post(new LoginResult(LoginResult.RESULT_FAILED, null));
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will send registration request to web api
     * Response will be re received by EventBus
     *
     * @param login    login of user
     * @param password password of user
     * @param email    email of user
     * @param context  app context.
     */
    public void doRegistrationRequest(final String login, final String password,
                                      final String email, final Context context) {
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        String deviceToken = IDListenerService.getInstance().getToken();
                        String url = API_URL + "register";

                        RequestParams params = new RequestParams();
                        params.add("username", login);
                        params.add("pwd", password);
                        params.add("email", email);
                        params.add("device_token", deviceToken);
                        params.add("device", "android");

                        SyncHttpClient client = new SyncHttpClient();
                        client.post(url, params, new AsyncHttpResponseHandler() {

                            @Override
                            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                                Session session = new RegistrationParser().parse(bytes, context);
                                if (session != null) {
                                    SessionManager.putSession(session, context);
                                    EventBus.getDefault().post(
                                            new RegistrationResult(RegistrationResult.RESULT_OK)
                                    );
                                } else {
                                    EventBus.getDefault().post(
                                            new RegistrationResult(RegistrationResult.RESULT_FAILED)
                                    );
                                }
                            }

                            @Override
                            public void onFailure(int i, Header[] headers, byte[] bytes,
                                                  Throwable throwable) {
                                EventBus.getDefault().post(
                                        new PerformAlertDialog(
                                                context.getString(R.string.networkErrorMessage),
                                                context.getString(R.string.networkErrorTittle)
                                        )
                                );
                            }

                        });
                    }

        }).start();
    }

    /**
     * Method that will send new password to server api and catching result of operation
     * Response will be re received by EventBus
     *
     * @param newPass        new password
     * @param newPassConfirm confirm new password
     * @param oldPass        old password of user
     * @param context        app context.
     */
    public void changePassword(String newPass, String newPassConfirm,
                               String oldPass, final Context context) {
        String url = API_URL + "change_pwd";

        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("old_pwd", oldPass);
        params.add("new_pwd", newPass);
        params.add("re_pwd", newPassConfirm);

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                if (new ChangePasswordParser().parse(bytes, context))
                    EventBus.getDefault().post(
                            new PerformAlertDialog(
                                    context.getString(R.string.changePasswordSuccess),
                                    context.getString(R.string.changePasswordTittle)
                            )
                    );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will load subjects of question
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    public void loadSubjects(final Context context) {
        SessionManager.getSession(context);
        String url = API_URL + "load_subjects";
        AsyncHttpClient client = new AsyncHttpClient();

        client.post(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                EventBus.getDefault().post(
                        new SubjectListLoaded(new LoadSubjectsParser().parse(bytes, context))
                );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will load question by subject
     * Response will be re received by EventBus
     *
     * @param subjectId id of subject
     * @param context   app context
     */
    public void loadQuestions(Integer subjectId, final Context context) {
        String url = API_URL + "load_questions";
        RequestParams params = new RequestParams();
        params.add("sid", String.valueOf(subjectId));

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                EventBus.getDefault().post(
                        new QuestionListLoaded(new LoadQuestionParser().parse(bytes, context))
                );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will send new question to web api
     * Response will be re received by EventBus
     *
     * @param question  new question that user want to ask
     * @param subjectID subject of new question
     * @param email     email of user
     * @param context   app context.
     */
    public void sendNewQuestion(Integer subjectID, String question,
                                String email, final Context context) {

        String url = API_URL + "answer";
        RequestParams params = new RequestParams();
        params.add("sid", String.valueOf(subjectID));
        params.add("question", question);
        params.add("email", email);

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will send request for prompt
     * Response will be re received by EventBus
     *
     * @param email   email of user
     * @param context app context.
     */
    public void forgotPassword(String email, final Context context) {

        String url = API_URL + "forgot_pwd";
        RequestParams params = new RequestParams();
        params.add("email", email);
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                if (new ForgotPasswordParser().parse(bytes, context))
                    EventBus.getDefault().post(
                            new PerformForgotPasswordConfirm()
                    );
                else
                    EventBus.getDefault().post(
                            new PerformAlertDialog(
                                    context.getString(R.string.forgotPasswordWrongEmail),
                                    context.getString(R.string.forgotDialogTitle)
                            )
                    );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    public void updateProfileInfo(ProfileInfo profile, final Context context) {
        String url = API_URL + "update_vehicle";

        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("first_name", profile.getFirstName());
        params.add("middle_name", profile.getMiddleName());
        params.add("last_name", profile.getLastName());
        params.add("building_number", profile.getBuildingNumber());
        params.add("street_name", profile.getStreetName());
        params.add("state", profile.getState());
        params.add("zip_code", profile.getZipCode());
        params.add("social_security", profile.getSocialSecurity());
        params.add("dob", profile.getDob());
        params.add("cell_phone", profile.getCellPhone());
        params.add("email", profile.getEmail());
        params.add("drivers_license", profile.getDriverLicense());
        params.add("license_state", profile.getStateLicense());
        params.add("license_expiration", profile.getExpirationDateState());
        params.add("tlc_license", profile.getTlcLicense());
        params.add("tlc_expiration", profile.getExpirationTLCLicense());
        params.add("course_expiration", profile.getDefExpiration());
        params.add("tlc_permit", profile.getTlcPermit());
        params.add("permit_expiration", profile.getTlcPermitExpiration());

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                UpdateProfileResult result = new UpdateProfileResult(
                        new UpdateProfileDataParser().parse(bytes, context) ?
                                UpdateProfileResult.SUCCESS : UpdateProfileResult.FAILED);
                EventBus.getDefault().post(result);
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }
        });

    }

    /**
     * Method that will load user profile data from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    public void loadProfileInfo(final Context context) {
        String url = API_URL + "load_profile";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                ProfileInfo profile = new LoadProfileDataParser().parse(bytes, context);
                EventBus.getDefault().post(new ProfileInfoResult(profile));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }
        });
    }

    /**
     * Method that will load vehicle information from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    public void loadVehicleInfo(final Context context) {
        String url = API_URL + "load_vehicle";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                VehicleInfo vehicle = new LoadVehicleInfoParser().parse(bytes, context);
                EventBus.getDefault().post(new LoadVehicleInfo(vehicle));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will update vehicle information on server
     * Response will be re received by EventBus
     *
     * @param vehicle vehicle information
     * @param context app context.
     */
    public void updateVehicleInfoHttpPost(VehicleInfo vehicle, final Context context) {
        String url = API_URL + "update_vehicle";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("vehicle_vin", vehicle.getVin());
        params.add("vehicle_year", vehicle.getYear());
        params.add("vehicle_make", vehicle.getMake());
        params.add("vehicle_model", vehicle.getModel());
        params.add("plate_number", vehicle.getPlateNumber());
        params.add("vehicle_state", vehicle.getState());
        params.add("registration_expiration", vehicle.getRegistrationExpiration());
        params.add("inspection_expiration", vehicle.getInspectionExpiration());
        params.add("insurance_policy", vehicle.getInsurancePolicy());
        params.add("vehicle_expiration", vehicle.getExpirationDate());
        params.add("base_name", vehicle.getBaseName());
        params.add("base_number", vehicle.getBaseNumber());
        params.add("base_address", vehicle.getBaseAddress());

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Boolean result = new UpdateVehicleParser().parse(bytes, context);
                EventBus.getDefault().post(
                        new UpdateVehicleResult(
                                result ? UpdateVehicleResult.SUCCESS : UpdateVehicleResult.FAILED
                        )
                );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will remove user from server
     *
     * @param context app context.
     */
    public void logOut(final Context context) {
        String url = API_URL + "close_account";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will set notification settings on server
     *
     * @param alerts  alerts settings
     * @param context app context.
     */
    public void setAlertsState(Alerts alerts, final Context context) {
        String url = API_URL + "set_alert";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("individual", Integer.toString(alerts.getIndividual() ? 1 : 0));
        params.add("membership", Integer.toString(alerts.getMembership() ? 1 : 0));
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * Method that will load notification settings from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    public void loadAlertsState(final Context context) {
        String url = API_URL + "get_alert";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                EventBus.getDefault().post(
                        new LoadAlertsResult(new LoadAlertsParser().parse(bytes, context))
                );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }
        });
    }

    /**
     * Method that will load notifications from server.
     * In firsts step it will load industry notifications after that,
     * next step will be loading individual notifications and collect it to List<Notifications>
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    public void loadNotifications(final Context context) {
        loadIndustryNotifications(context);
    }

    /**
     * first step of loading notifications from server
     * Response will be send to second step
     *
     * @param context app context.
     */
    private void loadIndustryNotifications(final Context context) {
        String url = API_URL + "membership_alerts";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                loadIndividualNotifications(
                        context,
                        new NotificationParser().parse(bytes, context)
                );
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

    /**
     * second step of loading notifications from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     */
    private void loadIndividualNotifications(final Context context,
                                             final List<Notification> notifications) {
        String url = API_URL + "individual_alerts";
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                notifications.addAll(new NotificationParser().parse(bytes, context));
                EventBus.getDefault().post(new NotificationsLoaded(notifications));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                EventBus.getDefault().post(
                        new PerformAlertDialog(
                                context.getString(R.string.networkErrorMessage),
                                context.getString(R.string.networkErrorTittle)
                        )
                );
            }

        });
    }

}