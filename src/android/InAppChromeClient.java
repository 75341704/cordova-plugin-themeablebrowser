/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package com.initialxy.cordova.themeablebrowser;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.webkit.JsPromptResult;
import android.webkit.WebChromeClient;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.GeolocationPermissions.Callback;

public class InAppChromeClient extends WebChromeClient {

    private CordovaWebView webView;
    private String LOG_TAG = "InAppChromeClient";
    private long MAX_QUOTA = 100 * 1024 * 1024;

    public InAppChromeClient(CordovaWebView webView) {
        super();
        this.webView = webView;
        cordovaPluginThis = webView.getPluginManager().getPlugin("InAppBrowser");
    }
    private boolean isMissingPermissions(int req_requestCode, ArrayList<String> permissions) {
        ArrayList<String> missingPermissions = new ArrayList<>();
        for (String permission: permissions) {
            if (!PermissionHelper.hasPermission(cordovaPluginThis, permission)) {
                missingPermissions.add(permission);
            }
        }

        boolean isMissingPermissions = missingPermissions.size() > 0;
        LOG.d(LOG_TAG, "KonPermissionRequest isMissingPermissions"+isMissingPermissions);

        if (isMissingPermissions) {
            String[] missing = missingPermissions.toArray(new String[missingPermissions.size()]);
            LOG.d(LOG_TAG, "KonPermissionRequest requestPermissions");
            PermissionHelper.requestPermissions(cordovaPluginThis, req_requestCode, missing);
        }
        return isMissingPermissions;
    }



    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onPermissionRequest(PermissionRequest request) {
//                        super.onPermissionRequest(request);
        LOG.d(LOG_TAG, "KonPermissionRequest");
        final String[] requestedResources = request.getResources();
        for (String r : requestedResources) {
            if (r.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                LOG.d(LOG_TAG, "KonPermissionRequest PermissionRequest.RESOURCE_AUDIO_CAPTURE:"+PermissionRequest.RESOURCE_AUDIO_CAPTURE);
                int req_requestCode = 0;//PermissionRequest.RESOURCE_AUDIO_CAPTURE;
                isMissingPermissions(req_requestCode,
                        new ArrayList<>(Arrays.asList( new String[] { PermissionRequest.RESOURCE_AUDIO_CAPTURE }))
                );
                break;
            }
        }
    }
    /**
     * Handle database quota exceeded notification.
     *
     * @param url
     * @param databaseIdentifier
     * @param currentQuota
     * @param estimatedSize
     * @param totalUsedQuota
     * @param quotaUpdater
     */
    @Override
    public void onExceededDatabaseQuota(String url, String databaseIdentifier, long currentQuota, long estimatedSize,
            long totalUsedQuota, WebStorage.QuotaUpdater quotaUpdater)
    {
        LOG.d(LOG_TAG, "onExceededDatabaseQuota estimatedSize: %d  currentQuota: %d  totalUsedQuota: %d", estimatedSize, currentQuota, totalUsedQuota);
        quotaUpdater.updateQuota(MAX_QUOTA);
    }

    /**
     * Instructs the client to show a prompt to ask the user to set the Geolocation permission state for the specified origin.
     *
     * @param origin
     * @param callback
     */
    @Override
    public void onGeolocationPermissionsShowPrompt(String origin, Callback callback) {
        super.onGeolocationPermissionsShowPrompt(origin, callback);
        callback.invoke(origin, true, false);
    }

    /**
     * Tell the client to display a prompt dialog to the user.
     * If the client returns true, WebView will assume that the client will
     * handle the prompt dialog and call the appropriate JsPromptResult method.
     *
     * The prompt bridge provided for the ThemeableBrowser is capable of executing any
     * oustanding callback belonging to the ThemeableBrowser plugin. Care has been
     * taken that other callbacks cannot be triggered, and that no other code
     * execution is possible.
     *
     * To trigger the bridge, the prompt default value should be of the form:
     *
     * gap-iab://<callbackId>
     *
     * where <callbackId> is the string id of the callback to trigger (something
     * like "ThemeableBrowser0123456789")
     *
     * If present, the prompt message is expected to be a JSON-encoded value to
     * pass to the callback. A JSON_EXCEPTION is returned if the JSON is invalid.
     *
     * @param view
     * @param url
     * @param message
     * @param defaultValue
     * @param result
     */
    @Override
    public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
        // See if the prompt string uses the 'gap-iab' protocol. If so, the remainder should be the id of a callback to execute.
        if (defaultValue != null && defaultValue.startsWith("gap")) {
            if(defaultValue.startsWith("gap-iab://")) {
                PluginResult scriptResult;
                String scriptCallbackId = defaultValue.substring(10);
                if (scriptCallbackId.startsWith("ThemeableBrowser")) {
                    if(message == null || message.length() == 0) {
                        scriptResult = new PluginResult(PluginResult.Status.OK, new JSONArray());
                    } else {
                        try {
                            scriptResult = new PluginResult(PluginResult.Status.OK, new JSONArray(message));
                        } catch(JSONException e) {
                            scriptResult = new PluginResult(PluginResult.Status.JSON_EXCEPTION, e.getMessage());
                        }
                    }
                    this.webView.sendPluginResult(scriptResult, scriptCallbackId);
                    result.confirm("");
                    return true;
                }
            }
            else
            {
                // Anything else with a gap: prefix should get this message
                LOG.w(LOG_TAG, "ThemeableBrowser does not support Cordova API calls: " + url + " " + defaultValue); 
                result.cancel();
                return true;
            }
        }
        return false;
    }

}
