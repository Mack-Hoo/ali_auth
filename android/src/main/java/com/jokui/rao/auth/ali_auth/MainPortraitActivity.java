package com.jokui.rao.auth.ali_auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mobile.auth.gatewayauth.AuthRegisterViewConfig;
import com.mobile.auth.gatewayauth.AuthRegisterXmlConfig;
import com.mobile.auth.gatewayauth.AuthUIConfig;
import com.mobile.auth.gatewayauth.AuthUIControlClickListener;
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper;
import com.mobile.auth.gatewayauth.PreLoginResultListener;
import com.mobile.auth.gatewayauth.TokenResultListener;
import com.mobile.auth.gatewayauth.model.TokenRet;
import com.mobile.auth.gatewayauth.ui.AbstractPnsViewDelegate;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

import static com.jokui.rao.auth.ali_auth.AppUtils.dp2px;
import static com.mobile.auth.gatewayauth.PhoneNumberAuthHelper.SERVICE_TYPE_LOGIN;

public class MainPortraitActivity implements PluginRegistry.ActivityResultListener {
    private final String TAG = "MainPortraitActivity";

    private final Activity activity;
    private final Context context;
    private PhoneNumberAuthHelper mAlicomAuthHelper;
    private TokenResultListener mTokenListener;
    private String token;
    private View switchTV;
    private int mScreenWidthDp;
    private int mScreenHeightDp;

    private final int SUCCESS = 0;
    private final int ERROR = -1;


    public MainPortraitActivity(Activity activity, Context context) {
        this.activity = activity;
        this.context = context;
    }
    
    @Override
    public boolean onActivityResult(int i, int i1, Intent intent) {
        return false;
    }

    private void updateScreenSize(int authPageScreenOrientation) {
        int screenHeightDp = AppUtils.px2dp(context, AppUtils.getPhoneHeightPixels(context));
        int screenWidthDp = AppUtils.px2dp(context, AppUtils.getPhoneWidthPixels(context));
        mScreenWidthDp = screenWidthDp;
        mScreenHeightDp = screenHeightDp;
    }

    public void init(final MethodCall call, final MethodChannel.Result methodResult) {
        mTokenListener = new TokenResultListener() {
            @Override
            public void onTokenSuccess(final String ret) {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.e("xxxxxx", "onTokenSuccess:" + ret);
                        /*
                         *   setText just show the result for get token???
                         *   use ret to verfiy number???
                         */
                        TokenRet tokenRet = null;
                        try {
                            tokenRet = JSON.parseObject(ret, TokenRet.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        JSONObject jsonObject = new JSONObject();

                        if (tokenRet != null && ("600024").equals(tokenRet.getCode())) {
                            jsonObject.put("code", tokenRet.getCode());
                            jsonObject.put("msg", "?????????????????????");
                        }

                        if (tokenRet != null && ("600001").equals(tokenRet.getCode())) {
                            jsonObject.put("code", tokenRet.getCode());
                            jsonObject.put("msg", "????????????????????????");
                        }

                        if (tokenRet != null && ("600000").equals(tokenRet.getCode())) {
                            token = tokenRet.getToken();
                            mAlicomAuthHelper.quitLoginPage();
                            jsonObject.put("code", tokenRet.getCode());
                            jsonObject.put("msg", "??????token?????????");
                        }
                        methodResult.success(jsonObject);
                    }
                });
            }

            @Override
            public void onTokenFailed(final String ret) {
                Log.e("xxxxxx", "onTokenFailed:" + ret);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /*
                         *  setText just show the result for get token
                         *  do something when getToken failed, such as use sms verify code.
                         */
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code", 5000);
                        jsonObject.put("msg", "?????????" + ret);
                        methodResult.success(jsonObject);
                    }
                });
            }
        };

        mAlicomAuthHelper = PhoneNumberAuthHelper.getInstance(context, mTokenListener);

        // ?????????SDK
        String SK = call.argument("sk");
        mAlicomAuthHelper.setAuthSDKInfo(SK);

        // ?????????????????????????????????????????????????????????????????????
        /*
         *   ???????????????????????? SERVICE_TYPE_LOGIN
         *   ???????????????????????? SERVICE_TYPE_AUTH
         */
        int checkEnvAvailable = (int) call.argument("checkEnvAvailable");
        mAlicomAuthHelper.checkEnvAvailable(checkEnvAvailable);

        // ??????????????????
        boolean isDebug = (boolean) call.argument("debug");
        mAlicomAuthHelper.getReporter().setLoggerEnable(isDebug);

        // ??????UI????????????
        mAlicomAuthHelper.setUIClickListener(new AuthUIControlClickListener() {
            @Override
            public void onClick(String code, Context context, String jsonObj) {
                Log.e("xxxxxx", "OnUIControlClick:code=" + code + ", jsonObj=" + (jsonObj == null ? "" : jsonObj));
            }
        });

        // ??????????????????dialog??????
        boolean type = (boolean) call.argument("type");
        Log.i(TAG, "configLoginTokenPort: "+type);
        if(type){
            configLoginTokenPort(call, methodResult);
        } else {
            configLoginTokenPortDialog(call, methodResult);
        }

        preLogin(call, methodResult);
    }


    /** SDK ?????????????????????????????? */
    /**
     * @deprecated
     */
    public boolean checkVerifyEnable(MethodCall call, MethodChannel.Result result) {
        // ????????????????????????
        boolean checkRet = mAlicomAuthHelper.checkEnvAvailable();
        if (!checkRet){
            Log.d(TAG, ("??????????????????????????????????????????????????????"));
        }

        result.success(checkRet);
        return checkRet;
    }

    /** SDK??????debug?????? */
    public void setDebugMode(MethodCall call, MethodChannel.Result result) {
        Object enable = getValueByKey(call, "debug");
        if (enable != null) {
            mAlicomAuthHelper.getReporter().setLoggerEnable((Boolean) enable);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", enable);
        result.success(jsonObject);
    }

    /** SDK ????????????????????? */
    public void preLogin(MethodCall call, final MethodChannel.Result result) {
        int timeOut = 5000;
        if (call.hasArgument("timeOut")) {
        Integer value = call.argument("timeOut");
            timeOut = value;
        }

        mAlicomAuthHelper.accelerateLoginPage(timeOut, new PreLoginResultListener() {
            @Override
            public void onTokenSuccess(final String vendor) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, vendor + "??????????????????");
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code", vendor);
                        jsonObject.put("msg", "??????????????????");
                        result.success(jsonObject);
                    }
                });
            }

            @Override
            public void onTokenFailed(final String vendor, final String ret) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, vendor + "???????????????:\n" + ret);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("code", ret);
                        jsonObject.put("msg", "???????????????");
                        result.success(jsonObject);
                    }
                });
            }
        });
    }

    // ????????????
    public void login(final MethodCall call, final MethodChannel.Result methodResult){
        getAuthListener(call, methodResult);
        mAlicomAuthHelper.getLoginToken(context, 5000);
    }

    // dialog??????
    public void loginDialog(final MethodCall call, final MethodChannel.Result methodResult){
        getAuthListener(call, methodResult);
        mAlicomAuthHelper.getLoginToken(context, 5000);
    }

    // ????????????token
    public void getToken(final MethodCall call, final MethodChannel.Result methodResult){
        getAuthListener(call, methodResult);
        mAlicomAuthHelper.getVerifyToken(5000);
    }

    // ??????????????????
    private void getAuthListener(final MethodCall call, final MethodChannel.Result methodResult){
        mAlicomAuthHelper.setAuthListener(new TokenResultListener() {
            @Override
            public void onTokenSuccess(final String ret) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TokenRet tokenRet = null;
                        try {
                            tokenRet = JSON.parseObject(ret, TokenRet.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (tokenRet != null && !("600001").equals(tokenRet.getCode())) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("returnCode", tokenRet.getCode());
                            jsonObject.put("returnMsg", tokenRet.getMsg());
                            jsonObject.put("returnData", tokenRet.getToken());
                            //?????????json?????????
                            methodResult.success(jsonObject);
                            mAlicomAuthHelper.quitLoginPage();
                        }
                        Log.d(TAG, ("??????:\n" + ret));
                    }
                });
            }
            @Override
            public void onTokenFailed(final String ret) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TokenRet tokenRet = null;
                        try {
                            tokenRet = JSON.parseObject(ret, TokenRet.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("returnCode", tokenRet.getCode());
                        jsonObject.put("returnMsg", tokenRet.getMsg());
                        //?????????json?????????
                        methodResult.success(jsonObject);

                        Log.d(TAG, ("??????:\n" + ret));
                    }
                });
            }
        });
    }

    // ?????????UI
    private void initDynamicView() {
        switchTV = LayoutInflater.from(context).inflate(R.layout.custom_switch_other, new RelativeLayout(context), false);
        RelativeLayout.LayoutParams mLayoutParams2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, dp2px(activity, 150));
        mLayoutParams2.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        mLayoutParams2.setMargins(0, dp2px(context, 450), 0, 0);
//        switchTV.setText("-----  ?????????view  -----");
//        switchTV.setTextColor(0xff999999);
//        switchTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.0F);
        switchTV.setLayoutParams(mLayoutParams2);
    }

    private ImageView createLandDialogPhoneNumberIcon( float rightMargin, float topMargin, float fontSize) {
        ImageView imageView = new ImageView(context);
        int size = AppUtils.dp2px(context, fontSize);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, RelativeLayout.TRUE);
        layoutParams.topMargin = AppUtils.px2dp(context, topMargin);
        layoutParams.rightMargin = AppUtils.px2dp(context, rightMargin);
        imageView.setLayoutParams(layoutParams);
        imageView.setBackgroundResource(R.drawable.slogan);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        return imageView;
    }

    private View createLandDialogCustomSwitchView(int layoutHeight, float leftMargin, float topMargin, float fontSize) {
        View v = LayoutInflater.from(context).inflate(R.layout.custom_slogan, new RelativeLayout(context), false);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
//        // ??????????????????
//        v.findViewById(R.id.login_left).setOnClickListener(new View.OnClickListener() {
//            @Override public void onClick(View v) {
//                Log.d(TAG, ("login_left ????????????"));
//            }
//        });
//
//        // ??????????????????
//        v.findViewById(R.id.login_right).setOnClickListener(new View.OnClickListener() {
//            @Override public void onClick(View v) {
//                Log.d(TAG, ("login_right ????????????"));
//            }
//        });
        TextView txv = v.findViewById(R.id.slogan_title);
        txv.setTextSize(fontSize);
        // int size = AppUtils.dp2px(context, 23);
        layoutParams.topMargin = AppUtils.px2dp(context, topMargin);
        layoutParams.leftMargin = AppUtils.px2dp(context, leftMargin);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        v.setLayoutParams(layoutParams);
        return v;
    }

    // ????????????????????????
    private void configLoginTokenPort(final MethodCall call, final MethodChannel.Result methodResult) {
        initDynamicView();
        Log.d(TAG, "configLoginTokenPort: "+call.arguments);
        mAlicomAuthHelper.removeAuthRegisterXmlConfig();
        mAlicomAuthHelper.removeAuthRegisterViewConfig();

        // ???????????????????????????
        mAlicomAuthHelper.addAuthRegistViewConfig("switch_acc_tv", new AuthRegisterViewConfig.Builder()
                .setView(switchTV)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_BODY)
                .build());

        // ???????????????????????????
//        mAlicomAuthHelper.addAuthRegisterXmlConfig(new AuthRegisterXmlConfig.Builder()
//            .setLayout(R.layout.custom_login_add, new AbstractPnsViewDelegate() {
//                @Override public void onViewCreated(View view) {
//                    // ??????????????????
//                    findViewById(R.id.login_left).setOnClickListener(new View.OnClickListener() {
//                        @Override public void onClick(View v) {
//                            Log.d(TAG, ("login_left ????????????"));
//                        }
//                    });
//
//                    // ??????????????????
//                    findViewById(R.id.login_right).setOnClickListener(new View.OnClickListener() {
//                        @Override public void onClick(View v) {
//                            Log.d(TAG, ("login_right ????????????"));
//                        }
//                    });
//                    //new AliAuthPlugin(call, methodResult)._events.success(message);
//                }
//            })
//            .build());


        // ????????????
//         mAlicomAuthHelper.addAuthRegistViewConfig("image_icon",
//                 new AuthRegisterViewConfig.Builder()
//                         .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_BODY)
//                         .setView(createLandDialogPhoneNumberIcon(0, 0, 200))
//                         .build());

        final View switchContainer = createLandDialogCustomSwitchView(250,0, 0, 24);

        // ??????????????????
        mAlicomAuthHelper.addAuthRegisterXmlConfig(new AuthRegisterXmlConfig.Builder()
            .setLayout(R.layout.custom_slogan, new AbstractPnsViewDelegate() {
                @Override public void onViewCreated(View view) {
                    findViewById(R.id.slogan_title).setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            Log.d(TAG, ("slogan ????????????"));
                        }
                    });
                }
            })
            .build());

        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        if (Build.VERSION.SDK_INT == 26) {
            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        }
        mAlicomAuthHelper.setAuthUIConfig(
            new AuthUIConfig.Builder()
            // ??????????????????
            .setStatusBarColor(Color.parseColor("#ffffff"))
            // .setStatusBarColor(Color.TRANSPARENT)
            .setLightColor(true)
            // ???????????????
            .setNavHidden(true)
            .setNavColor(Color.parseColor("#3971fe")) // ??????????????????
            .setNavText("????????????????????????") // ??????????????????
            .setAppPrivacyColor(Color.GRAY, Color.parseColor("#3971fe"))
            // logo??????
            .setLogoHidden(true)
            .setLogoImgPath("ic_launcher")
            // slogan ??????
            .setSloganHidden(true)
            // ????????????
            .setNumberColor(Color.parseColor("#3C4F5E"))
            // ????????????
            .setLogBtnBackgroundPath("button")
            .setLogBtnHeight(38)
            .setAuthPageActIn("in_activity", "out_activity")
            .setAuthPageActOut("in_activity", "out_activity")
            .setVendorPrivacyPrefix("???")
            .setVendorPrivacySuffix("???")
            // ???????????????????????????
            .setSwitchAccTextColor(Color.parseColor("#3A71FF"))
            .setSwitchAccText("?????????????????????")
            .setScreenOrientation(authPageOrientation)
            // ?????????
            .setCheckboxHidden(false)
            // ?????????????????????
            // .setPrivacyBefore("sadadasda")
            .setPrivacyState(false)
            // .setLogBtnBackgroundPath("slogan")
            //.setPrivacyBefore("????????????????????????")
            .setAppPrivacyOne("????????????????????????", "https://www.baidu.com")
            .setAppPrivacyTwo("????????????????????????", "https://www.baidu.com")
            //.setStatusBarUIFlag(View.SYSTEM_UI_FLAG_LOW_PROFILE) // ????????????????????????????????????????????????????????????????????????
            .setStatusBarUIFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) // ????????????????????????????????????????????????????????????????????????
            // ??????????????????????????????
            .setBottomNavColor(Color.parseColor("#ffffff"))
            .create()
        );
    }
    // ??????????????????
    private void configLoginTokenPortDialog(final MethodCall call, final MethodChannel.Result methodResult) {
        // initDynamicView();
        mAlicomAuthHelper.removeAuthRegisterXmlConfig();
        mAlicomAuthHelper.removeAuthRegisterViewConfig();
        int authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        if (Build.VERSION.SDK_INT == 26) {
            authPageOrientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        }
        updateScreenSize(authPageOrientation);
        int dialogWidth = (int) (mScreenWidthDp * 0.8f);
        int dialogHeight = (int) (mScreenHeightDp * 0.65f);
        // mAlicomAuthHelper.addAuthRegisterXmlConfig(
        //     new AuthRegisterXmlConfig.Builder().setLayout(R.layout.custom_port_dialog_action_bar, new AbstractPnsViewDelegate() {
        //         @Override
        //         public void onViewCreated(View view) {
        //             findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
        //                 @Override
        //                 public void onClick(View v) {
        //                     mAlicomAuthHelper.quitLoginPage();
        //                 }
        //             });
        //         }
        //     }).build()
        // );
        int logBtnOffset = dialogHeight / 2;
        mAlicomAuthHelper.setAuthUIConfig(
            new AuthUIConfig.Builder()
            // .setAppPrivacyOne("???????????????????????????", "https://www.baidu.com")
            .setAppPrivacyColor(Color.GRAY, Color.parseColor("#3971fe"))
            .setPrivacyState(false)
            .setCheckboxHidden(true)
//            .setNavHidden(false)
//            .setNavColor(Color.parseColor("#3971fe"))
//            .setNavReturnImgPath("icon_close")
            .setWebNavColor(Color.parseColor("#3971fe"))
            .setAuthPageActIn("in_activity", "out_activity")
            .setAuthPageActOut("in_activity", "out_activity")
            .setVendorPrivacyPrefix("???")
            .setVendorPrivacySuffix("???")
            .setLogoImgPath("ic_launcher")
            .setLogBtnWidth(dialogWidth - 30)
            .setLogBtnMarginLeftAndRight(15)
            .setLogBtnBackgroundPath("button")
            .setLogoOffsetY(48)
            .setLogoWidth(42)
            .setLogoHeight(42)
            .setLogBtnOffsetY(logBtnOffset)
            .setSloganText("????????????????????????????????????????????????")
            .setSloganOffsetY(logBtnOffset - 100)
            .setSloganTextSize(11)
            .setNumFieldOffsetY(logBtnOffset - 50)
            .setSwitchOffsetY(logBtnOffset + 50)
            .setSwitchAccTextSize(11)
//            .setPageBackgroundPath("dialog_background_color")
            .setNumberSize(17)
            .setLogBtnHeight(38)
            .setLogBtnTextSize(16)
            .setDialogWidth(dialogWidth)
            .setDialogHeight(dialogHeight)
            .setDialogBottom(false)
//            .setDialogAlpha(82)
            .setScreenOrientation(authPageOrientation)
            .create()
        );
    }

    // ??????key???
    private Object getValueByKey(MethodCall call, String key) {
        if (call != null && call.hasArgument(key)) {
            return call.argument(key);
        } else {
            return null;
        }
    }

    // ??????????????????
    public static String getNetworkClass(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if(info==null || !info.isConnected())
            return "-"; //not connected
        if(info.getType() == ConnectivityManager.TYPE_WIFI)
            return "WIFI";
        if(info.getType() == ConnectivityManager.TYPE_MOBILE){
            int networkType = info.getSubtype();
            switch (networkType) {
                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
                    return "2G";
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
                case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
                case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
                    return "3G";
                case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
                    return "4G";
                default:
                    return "?";
            }
        }
        return "?";
    }
}

