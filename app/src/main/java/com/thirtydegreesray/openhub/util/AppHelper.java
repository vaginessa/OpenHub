/*
 *    Copyright 2017 ThirtyDegreesRay
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.thirtydegreesray.openhub.util;

import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;

import com.thirtydegreesray.openhub.AppApplication;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.ui.activity.ProfileActivity;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.ui.activity.ViewerActivity;

import java.util.List;
import java.util.Locale;

import es.dmoral.toasty.Toasty;

/**
 * Created on 2017/8/3.
 *
 * @author ThirtyDegreesRay
 */

public class AppHelper {

    public static void updateAppLanguage(@NonNull Context context) {
        String lang = PrefHelper.getLanguage();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, lang);
        }
        updateResourcesLegacy(context, lang);
    }

    private static void updateResources(Context context, String language) {
        Locale locale = getLocale(language);
        Locale.setDefault(locale);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        context.createConfigurationContext(configuration);
    }

    @SuppressWarnings("deprecation")
    private static void updateResourcesLegacy(Context context, String language) {
        Locale locale = getLocale(language);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }

    @NonNull public static Locale getLocale(String language) {
        Locale locale = null;
        if (language.equalsIgnoreCase("zh-rCN")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else if (language.equalsIgnoreCase("zh-rTW")) {
            locale = Locale.TRADITIONAL_CHINESE;
        }
        if (locale != null) return locale;
        String[] split = language.split("-");
        if (split.length > 1) {
            locale = new Locale(split[0], split[1]);
        } else {
            locale = new Locale(language);
        }
        return locale;
    }

    public static boolean isNightMode(){
        int theme = PrefHelper.getTheme();
        return theme == PrefHelper.DARK ;
    }

    public static void copyToClipboard(@NonNull Context context, @NonNull String uri) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(context.getString(R.string.app_name), uri);
        clipboard.setPrimaryClip(clip);
        Toasty.success(AppApplication.get(), context.getString(R.string.success_copied)).show();
    }

    public static void openInBrowser(@NonNull Context context, @NonNull String url){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        try{
            context.startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toasty.warning(context, context.getString(R.string.no_share_clients)).show();
        }
    }

    public static void shareText(@NonNull Context context, @NonNull String text) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        shareIntent.setType("text/plain");
        try{
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_to)));
        }catch (ActivityNotFoundException e){
            Toasty.warning(context, context.getString(R.string.no_share_clients)).show();
        }
    }

    public static void launchEmail(@NonNull Context context, @NonNull String email){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        try{
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_email)));
        }catch (ActivityNotFoundException e){
            Toasty.warning(context, context.getString(R.string.no_email_clients)).show();
        }
    }


    public static void launchUrl(@NonNull Context context, @NonNull Uri uri){
        if(StringUtils.isBlank(uri.toString())) return;
        String loginId;
        if(GitHubHelper.isImage(uri.toString())){
            ViewerActivity.showImage(context, uri.toString());
        } else if((loginId = GitHubHelper.getUserFromUrl(uri.toString())) != null){
            ProfileActivity.show(context, loginId);
        } else if(GitHubHelper.isRepoUrl(uri.toString())){
            String fullName = GitHubHelper.getRepoFullNameFromUrl(uri.toString());
            if(StringUtils.isBlank(fullName)){
                openInBrowser(context, uri.toString());
            } else {
                RepositoryActivity.show(context, fullName.split("/")[0], fullName.split("/")[1]);
            }
        } else {
            openInBrowser(context, uri.toString());
        }
    }


    public static boolean isAppAlive(Context context, String packageName){
        ActivityManager activityManager =
                (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfos
                = activityManager.getRunningAppProcesses();
        for(int i = 0; i < processInfos.size(); i++){
            if(processInfos.get(i).processName.equals(packageName)){
                return true;
            }
        }
        return false;
    }

    private static final String DOWNLOAD_SERVICE_PACKAGE_NAME = "com.android.providers.downloads";

    public static boolean checkApplicationEnabledSetting(Context context, String packageName){
        int state = context.getPackageManager().getApplicationEnabledSetting(packageName);
        return state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT ||
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    public static boolean checkDownloadServiceEnabled(Context context){
        return checkApplicationEnabledSetting(context, DOWNLOAD_SERVICE_PACKAGE_NAME);
    }

    public static void showDownloadServiceSetting(Context context){
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + DOWNLOAD_SERVICE_PACKAGE_NAME));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e){
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

}
