package com.qa.tlcask.network;

import android.content.Context;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.qa.tlcask.R;
import com.qa.tlcask.authentification.session.Session;
import com.qa.tlcask.authentification.session.SessionManager;
import com.qa.tlcask.main.model.Alerts;
import com.qa.tlcask.main.model.Notification;
import com.qa.tlcask.main.model.ProfileInfo;
import com.qa.tlcask.main.model.VehicleInfo;
import com.qa.tlcask.messages.NotificationsLoaded;
import com.qa.tlcask.messages.PerformAlertDialog;
import com.qa.tlcask.messages.authentification.LoginResult;
import com.qa.tlcask.messages.authentification.PerformForgotPasswordConfirm;
import com.qa.tlcask.messages.authentification.RegistrationResult;
import com.qa.tlcask.messages.main.LoadAlertsResult;
import com.qa.tlcask.messages.main.LoadVehicleInfo;
import com.qa.tlcask.messages.main.ProfileInfoResult;
import com.qa.tlcask.messages.main.QuestionListLoaded;
import com.qa.tlcask.messages.main.SubjectListLoaded;
import com.qa.tlcask.messages.main.UpdateProfileResult;
import com.qa.tlcask.messages.main.UpdateVehicleResult;
import com.qa.tlcask.network.parsers.ChangePasswordParser;
import com.qa.tlcask.network.parsers.ForgotPasswordParser;
import com.qa.tlcask.network.parsers.LoadAlertsParser;
import com.qa.tlcask.network.parsers.LoadProfileDataParser;
import com.qa.tlcask.network.parsers.LoadQuestionParser;
import com.qa.tlcask.network.parsers.LoadSubjectsParser;
import com.qa.tlcask.network.parsers.LoadVehicleInfoParser;
import com.qa.tlcask.network.parsers.LoginParser;
import com.qa.tlcask.network.parsers.NotificationParser;
import com.qa.tlcask.network.parsers.RegistrationParser;
import com.qa.tlcask.network.parsers.UpdateProfileDataParser;
import com.qa.tlcask.network.parsers.UpdateVehicleParser;

import org.apache.http.Header;

import java.io.IOException;
import java.util.List;

import de.greenrobot.event.EventBus;

public class NetworkManager {

    //links to web-api
    private static final String DEV_URL = "http://tlcask.dev.gbksoft.net/";
    private static final String PRODUCTION_URL = "http://tlcask.com/";
    private final String API_URL;

    public NetworkManager() {

        API_URL = DEV_URL;
    }

    /**
     * Method that will send login request to web api
     * Response will be re received by EventBus
     *
     * @param login    login of user
     * @param password password of user
     * @param context  app context.
     * @return nothing
     */

    public void doLoginRequest(String login, String password, final Context context) {

        String urlPart = "login";
        String url = API_URL + urlPart;
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

                    EventBus.getDefault().post(new LoginResult(LoginResult.RESULT_FAILED, session));
                }
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
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
     * @return nothing
     */

    public void doRegistrationRequest(final String login, final String password, final String email, final Context context) {

        new Thread(new Runnable() {

            @Override
            public void run() {

                try {

                    String deviceId = GoogleCloudMessaging.getInstance(context).register(context.getResources().getString(R.string.apiKey));
                    String urlPart = "register";
                    String url = API_URL + urlPart;
                    RequestParams params = new RequestParams();
                    params.add("username", login);
                    params.add("pwd", password);
                    params.add("email", email);
                    params.add("device_token", deviceId);
                    params.add("device", "android");
                    SyncHttpClient client = new SyncHttpClient();
                    client.post(url, params, new AsyncHttpResponseHandler() {

                        @Override
                        public void onSuccess(int i, Header[] headers, byte[] bytes) {

                            Session session = new RegistrationParser().parse(bytes, context);
                            if (session != null) {

                                SessionManager.putSession(session, context);
                                EventBus.getDefault().post(new RegistrationResult(RegistrationResult.RESULT_OK));
                            } else {

                                EventBus.getDefault().post(new RegistrationResult(RegistrationResult.RESULT_FAILED));
                            }
                        }

                        @Override
                        public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                            EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
                        }
                    });
                } catch (IOException e) {

                    EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
                }
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
     * @return nothing
     */

    public void changePassword(final Context context, String newPass, String newPassConfirm, String oldPass) {

        String urlPart = "change_pwd";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("oldpwd", oldPass);
        params.add("newpwd", newPass);
        params.add("repwd", newPassConfirm);
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                if (new ChangePasswordParser().parse(bytes, context))
                    EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.changePasswordSuccess), context.getString(R.string.changePasswordTittle)));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load subjects of question
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */

    public void loadSubjects(final Context context) {

        SessionManager.getSession(context);
        String urlPart = "load_subjects";
        String url = API_URL + urlPart;
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                EventBus.getDefault().post(new SubjectListLoaded(new LoadSubjectsParser().parse(bytes, context)));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load question by subject
     * Response will be re received by EventBus
     *
     * @param subjectId id of subject
     * @param context   app context.
     * @return nothing
     */
    public void loadQuestions(Integer subjectId, final Context context) {

        String urlPart = "load_questions";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("sid", String.valueOf(subjectId));
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                EventBus.getDefault().post(new QuestionListLoaded(new LoadQuestionParser().parse(bytes, context)));

            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));

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
     * @return nothing
     */

    public void sendNewQuestion(Integer subjectID, String question, String email, final Context context) {

        String urlPart = "answer";
        String url = API_URL + urlPart;
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

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will send request for prompt
     * Response will be re received by EventBus
     *
     * @param email   email of user
     * @param context app context.
     * @return nothing
     */

    public void forgotPassword(String email, final Context context) {

        String urlPart = "forgot_pwd";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("email", email);
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                if (new ForgotPasswordParser().parse(bytes, context))
                    EventBus.getDefault().post(new PerformForgotPasswordConfirm());
                else
                    EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.forgotPasswordWrongEmail), context.getString(R.string.forgotDialogTitle)));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    public void updateProfileInfo(final Context context, ProfileInfo profile) {

        String urlPart = "update_vehicle";
        String url = API_URL + urlPart;
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

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load user profile data from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */

    public void loadProfileInfo(final Context context) {

        String urlPart = "load_profile";
        String url = API_URL + urlPart;
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

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load vehicle information from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */

    public void loadVehicleInfo(final Context context) {

        String urlPart = "load_vehicle";
        String url = API_URL + urlPart;
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

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will update vehicle information on server
     * Response will be re received by EventBus
     *
     * @param vehicle vehicle information
     * @param context app context.
     * @return nothing
     */
    public void updateVehicleInfoHttpPost(VehicleInfo vehicle, final Context context) {

        String urlPart = "update_vehicle";
        String url = API_URL + urlPart;
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
                EventBus.getDefault().post(new UpdateVehicleResult(result ? UpdateVehicleResult.SUCCESS :
                        UpdateVehicleResult.FAILED));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will remove user from server
     *
     * @param context app context.
     * @return nothing
     */

    public void logOut(final Context context) {

        String urlPart = "close_account";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will set notification settings on server
     *
     * @param alerts  alerts settings
     * @param context app context.
     * @return nothing
     */

    public void setAlertsState(final Context context, Alerts alerts) {

        String urlPart = "set_alert";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        params.add("individual", new StringBuilder().append(alerts.getIndividual() ? 1 : 0).toString());
        params.add("membership", new StringBuilder().append(alerts.getMembership() ? 1 : 0).toString());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load notification settings from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */
    public void loadAlertsState(final Context context) {

        String urlPart = "get_alert";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                EventBus.getDefault().post(new LoadAlertsResult(new LoadAlertsParser().parse(bytes, context)));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * Method that will load notifications from server. in firts step it will load industry notifications after that,
     * next step will be loading individual notifications and collect it to List<Notifications>
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */

    public void loadNotifications(final Context context) {

        loadIndustryNotifications(context);
    }

    /**
     * first step of loading notifications from server
     * Response will be send to second step
     *
     * @param context app context.
     * @return nothing
     */

    private void loadIndustryNotifications(final Context context) {

        String urlPart = "membership_alerts";
        String url = API_URL + urlPart;
        RequestParams params = new RequestParams();
        params.add("token", SessionManager.getSession(context).getToken());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {

                loadIndividualNotifications(context, new NotificationParser().parse(bytes, context));
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }

    /**
     * second step of loading notifications from server
     * Response will be re received by EventBus
     *
     * @param context app context.
     * @return nothing
     */

    private void loadIndividualNotifications(final Context context, final List<Notification> notifications) {

        String urlPart = "individual_alerts";
        String url = API_URL + urlPart;
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

                EventBus.getDefault().post(new PerformAlertDialog(context.getString(R.string.networkErrorMessage), context.getString(R.string.networkErrorTittle)));
            }
        });
    }
}
