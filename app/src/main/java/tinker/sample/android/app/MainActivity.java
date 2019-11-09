/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tinker.sample.android.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.downloader.Status;
import com.tencent.tinker.lib.library.TinkerLoadLibrary;
import com.tencent.tinker.lib.listener.PatchListener;
import com.tencent.tinker.lib.service.AbstractResultService;
import com.tencent.tinker.lib.service.DefaultTinkerResultService;
import com.tencent.tinker.lib.tinker.Tinker;
import com.tencent.tinker.lib.tinker.TinkerInstaller;
import com.tencent.tinker.lib.util.TinkerLog;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import com.tencent.tinker.loader.shareutil.ShareTinkerInternals;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tinker.sample.android.R;
import tinker.sample.android.eventsmodel.MessageEvent;
import tinker.sample.android.models.ConfigResponseObject;
import tinker.sample.android.reporter.SamplePatchListener;
import tinker.sample.android.servercom.GetDataService;
import tinker.sample.android.servercom.RetrofitClientInstance;
import tinker.sample.android.util.AppConstant;
import tinker.sample.android.util.UserPrefs;
import tinker.sample.android.util.Utils;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Tinker.MainActivity";
    private static Class<? extends AbstractResultService> resultServiceClass = null;

    ProgressDialog progressDoalog;
    private TextView mTvMessage = null;
    int downloadIdOne;
    String patchUrl = "";

    ConfigResponseObject responseObject = null;

    Button bugTest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        boolean isARKHotRunning = ShareTinkerInternals.isArkHotRuning();
        Log.e(TAG, "ARK HOT Running status = " + isARKHotRunning);
        Log.e(TAG, "i am on onCreate classloader:" + MainActivity.class.getClassLoader().toString());
        //test resource change
//        Log.e(TAG, "i am on onCreate string:" + getResources().getString(R.string.test_resource));
        Log.e(TAG, "i am on patch onCreate");



        mTvMessage = findViewById(R.id.tv_message);

        progressDoalog = new ProgressDialog(MainActivity.this);
        progressDoalog.setMessage("Loading....");

        askForRequiredPermissions();

        Button loadPatchButton = (Button) findViewById(R.id.loadPatch);

        loadPatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk");
            }
        });

        Button loadLibraryButton = (Button) findViewById(R.id.loadLibrary);

        loadLibraryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // #method 1, hack classloader library path
                TinkerLoadLibrary.installNavitveLibraryABI(getApplicationContext(), "armeabi");
                System.loadLibrary("stlport_shared");

                // #method 2, for lib/armeabi, just use TinkerInstaller.loadLibrary
//                TinkerLoadLibrary.loadArmLibrary(getApplicationContext(), "stlport_shared");

                // #method 3, load tinker patch library directly
//                TinkerInstaller.loadLibraryFromTinker(getApplicationContext(), "assets/x86", "stlport_shared");

            }
        });

        Button cleanPatchButton = (Button) findViewById(R.id.cleanPatch);

        cleanPatchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Tinker.with(getApplicationContext()).cleanPatch();
            }
        });

        Button killSelfButton = (Button) findViewById(R.id.killSelf);

        killSelfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareTinkerInternals.killAllOtherProcess(getApplicationContext());
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });

        Button buildInfoButton = (Button) findViewById(R.id.showInfo);

        buildInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfo(MainActivity.this);
            }
        });

        bugTest = (Button) findViewById(R.id.bug_test);

        //final Object object = null;

        bugTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // try to do crash
                // hotfix testing
                Toast.makeText(MainActivity.this, "Bug Fixed", Toast.LENGTH_SHORT).show(); // display toast
                bugTest.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light)); //turn green on bug fixed
            }
        });
    }

    private void askForRequiredPermissions() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= 16) {
            final int res = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return res == PackageManager.PERMISSION_GRANTED;
        } else {
            // When SDK_INT is below 16, READ_EXTERNAL_STORAGE will also be granted if WRITE_EXTERNAL_STORAGE is granted.
            final int res = ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return res == PackageManager.PERMISSION_GRANTED;
        }
    }

    public boolean showInfo(Context context) {
        // add more Build Info
        final StringBuilder sb = new StringBuilder();
        Tinker tinker = Tinker.with(getApplicationContext());
        if (tinker.isTinkerLoaded()) {
            sb.append(String.format("[patch is loaded] \n"));
            sb.append(String.format("[buildConfig TINKER_ID] %s \n", BuildInfo.TINKER_ID));
            sb.append(String.format("[buildConfig BASE_TINKER_ID] %s \n", BaseBuildInfo.BASE_TINKER_ID));

            sb.append(String.format("[buildConfig MESSSAGE] %s \n", BuildInfo.MESSAGE));
            sb.append(String.format("[TINKER_ID] %s \n", tinker.getTinkerLoadResultIfPresent().getPackageConfigByName(ShareConstants.TINKER_ID)));
            sb.append(String.format("[packageConfig patchMessage] %s \n", tinker.getTinkerLoadResultIfPresent().getPackageConfigByName("patchMessage")));
            sb.append(String.format("[TINKER_ID Rom Space] %d k \n", tinker.getTinkerRomSpace()));

        } else {
            sb.append(String.format("[patch is not loaded] \n"));
            sb.append(String.format("[buildConfig TINKER_ID] %s \n", BuildInfo.TINKER_ID));
            sb.append(String.format("[buildConfig BASE_TINKER_ID] %s \n", BaseBuildInfo.BASE_TINKER_ID));

            sb.append(String.format("[buildConfig MESSSAGE] %s \n", BuildInfo.MESSAGE));
            sb.append(String.format("[TINKER_ID] %s \n", ShareTinkerInternals.getManifestTinkerID(getApplicationContext())));
        }
        sb.append(String.format("[BaseBuildInfo Message] %s \n", BaseBuildInfo.TEST_MESSAGE));

        final TextView v = new TextView(context);
        v.setText(sb);
        v.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        v.setTextColor(0xFF000000);
        v.setTypeface(Typeface.MONOSPACE);
        final int padding = 16;
        v.setPadding(padding, padding, padding, padding);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setView(v);
        final AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    @Override
    protected void onResume() {
        Log.e(TAG, "i am on onResume");
//        Log.e(TAG, "i am on patch onResume");

        super.onResume();
        Utils.setBackground(false);

        if (hasRequiredPermissions()) {
            mTvMessage.setVisibility(View.GONE);

            fetchConfig();

        } else {
            mTvMessage.setText(R.string.msg_no_permissions);
            mTvMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            mTvMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.setBackground(true);
    }

    private void patchDownloader(final String patchUrl){

        if (Status.RUNNING == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.pause(downloadIdOne);
            return;
        }

        if (Status.PAUSED == PRDownloader.getStatus(downloadIdOne)) {
            PRDownloader.resume(downloadIdOne);
            return;
        }

        final ProgressDialog progressBarDialog= new ProgressDialog(MainActivity.this);


        progressBarDialog.setTitle("Patch Downloading...");

        progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressBarDialog.setCanceledOnTouchOutside(false);

        progressBarDialog.setIndeterminate(true);


        //setting the OK Button
        progressBarDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Background", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog,
                                int whichButton){

                showToast("Downloading continue in background");
            }
        });

        //set the Cancel button
        progressBarDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int whichButton){

                PRDownloader.cancel(downloadIdOne);
            }
        });

        //initialize the dialog..
        progressBarDialog.setProgress(0);

        //show the dialog
        progressBarDialog.show();

        downloadIdOne = PRDownloader.download(patchUrl, Utils.getRootDirPath(MainActivity.this), "patch_signed_7zip.apk")
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {

                        progressBarDialog.setIndeterminate(false);
                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {

                        downloadIdOne = 0;
                        progressBarDialog.setProgress(0);
                        progressBarDialog.dismiss();

                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {

                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        progressBarDialog.setProgress((int) progressPercent);
                        //showToast(""+(int)progressPercent);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {

                        showToast("Patch Download Complete");
                        progressBarDialog.dismiss();
                        // load patch
                        loadPatch();
                    }

                    @Override
                    public void onError(Error error) {

                        showToast("Download Error! something went wrong try again, try again.");

                        downloadIdOne = 0;
                        progressBarDialog.setProgress(0);
                        progressBarDialog.dismiss();
                        deleteErrorFile(patchUrl);
                    }

                });
    }

    private void deleteErrorFile(String path){

        File file = new File(getExternalFilesDirs(null)[0],getPatchFilePath(path)+".temp");

        if (file.exists()) {

            file.delete();

            if (file.exists()){

                showToast("Not clear");
            }else {

                showToast("Session clear");
            }
        }
    }

    private String getPatchFilePath(String videoURl){

        Uri uri = Uri.parse(videoURl);
        return uri.getLastPathSegment();
    }

    private void showToast(String message){

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void fetchConfig(){

        progressDoalog.show();

        GetDataService service = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
        Call<ConfigResponseObject> call = service.getConfig();
        call.enqueue(new Callback<ConfigResponseObject>() {
            @Override
            public void onResponse(Call<ConfigResponseObject> call, Response<ConfigResponseObject> response) {
                progressDoalog.dismiss();

                if (response.body()!=null){

                    responseObject = (ConfigResponseObject) response.body();

                    if (responseObject.isPatchAvailable()){

                        if (responseObject.getPatchId().equals(UserPrefs.getInstance(MainActivity.this).getString(AppConstant.PATCH_ID,""))){

                            showToast("Patch Already Applied!");
                        }
                        else {

                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            alertDialog.setTitle("Patch Available");
                            alertDialog.setMessage("Want to download patch?");
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Download",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            patchUrl = responseObject.getPatchUrl();
                                            patchDownloader(responseObject.getPatchUrl());
                                        }
                                    });

                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Later",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });

                            alertDialog.show();
                        }

                    }
                    else {

                        showToast("Patch not available!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ConfigResponseObject> call, Throwable t) {
                progressDoalog.dismiss();
                Toast.makeText(MainActivity.this, "Something went wrong...Please try later!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPatchExist(){

        File file = new File(getExternalFilesDirs(null)[0],"patch_signed_7zip.apk");
        if(file.exists()){

            return true;
        }
        else{
            return false;
        }
    }

    private void loadPatch(){

        if (checkPatchExist()){

            showToast("Patch applying...");
            File file = new File(getExternalFilesDirs(null)[0],"patch_signed_7zip.apk");
            TinkerInstaller.onReceiveUpgradePatch(getApplicationContext(),file.getAbsolutePath() ); //Environment.getExternalStorageDirectory().getAbsolutePath() + "/patch_signed_7zip.apk"
        }
        else {

            showToast("Something wrong, patch file not found!");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent patchEvent) {

        if (patchEvent.isPatchInstalled()){

            if(responseObject != null) {
                UserPrefs.getInstance(MainActivity.this).saveString(AppConstant.PATCH_ID, responseObject.getPatchId());
                showToast("Patch Applied - Restarting app");
                /*Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Do something after 2sec
                        showToast("Patch Applied! - Restart app");
                        //restartProcess();
                    }
                }, 2000);*/

            }
            else {

                showToast("Something went wrong!");
            }
        }
        else {

            showToast("Patch not applied!");
        }
    };

    private void restartProcess() {
        TinkerLog.i(TAG, "app is background now, i can kill quietly");
        //you can send service or broadcast intent to restart your process
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
