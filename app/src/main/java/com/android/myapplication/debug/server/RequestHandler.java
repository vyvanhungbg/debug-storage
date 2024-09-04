/*
 *
 *  *    Copyright (C) 2019 Amit Shekhar
 *  *    Copyright (C) 2011 Android Open Source Project
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package com.android.myapplication.debug.server;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Pair;


import com.android.myapplication.debug.Constants;
import com.android.myapplication.debug.PrefHelper;
import com.android.myapplication.debug.Utils;
import com.android.myapplication.debug.model.Response;
import com.android.myapplication.debug.model.RowDataRequest;
import com.android.myapplication.debug.model.TableDataResponse;
import com.android.myapplication.debug.model.UpdateRowResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by amitshekhar on 06/02/17.
 */

public class RequestHandler {

    private final Context mContext;
    private final Gson mGson;
    private final AssetManager mAssets;
    private String mSelectedDatabase = null;

    public RequestHandler(Context context) {
        mContext = context;
        mAssets = context.getResources().getAssets();
        mGson = new GsonBuilder().serializeNulls().create();
    }

    public void handle(Socket socket) throws IOException {
        BufferedReader reader = null;
        PrintStream output = null;
        try {
            String route = null;

            // Read HTTP headers and parse out the route.
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line;
            while (!TextUtils.isEmpty(line = reader.readLine())) {
                if (line.startsWith("GET /")) {
                    int start = line.indexOf('/') + 1;
                    int end = line.indexOf(' ', start);
                    route = line.substring(start, end);
                    break;
                }
            }

            // Output stream that we send the response to
            output = new PrintStream(socket.getOutputStream());

            if (route == null || route.isEmpty()) {
                route = "index.html";
            }

            byte[] bytes;

            if (route.startsWith("getDbList")) {
                final String response = getDBListResponse();
                bytes = response.getBytes();
            } else if (route.startsWith("getAllDataFromTheTable")) {
                final String response = getAllDataFromTheTableResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("getTableList")) {
                final String response = getTableListResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("addTableData")) {
                final String response = addTableDataAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("updateTableData")) {
                final String response = updateTableDataAndGetResponse(route);
                bytes = response.getBytes();
            } else if (route.startsWith("deleteTableData")) {
                final String response = deleteTableDataAndGetResponse(route);
                bytes = response.getBytes();
            }
//            else if (route.startsWith("query")) {
//                final String response = executeQueryAndGetResponse(route);
//                bytes = response.getBytes();
//            } else if (route.startsWith("deleteDb")) {
//                final String response = deleteSelectedDatabaseAndGetResponse();
//                bytes = response.getBytes();
//            }
//            else if (route.startsWith("downloadDb")) {
//                bytes = Utils.getDatabase(mSelectedDatabase, mDatabaseFiles);
//            }
            else {
                bytes = Utils.loadContent(route, mAssets);
            }


            if (null == bytes) {
                writeServerError(output);
                return;
            }

            // Send out the content.
            output.println("HTTP/1.0 200 OK");
            output.println("Content-Type: " + Utils.detectMimeType(route));

            if (route.startsWith("downloadDb")) {
                output.println("Content-Disposition: attachment; filename=" + mSelectedDatabase);
            } else {
                output.println("Content-Length: " + bytes.length);
            }
            output.println();
            output.write(bytes);
            output.flush();
        } finally {
            try {
                if (null != output) {
                    output.close();
                }
                if (null != reader) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void writeServerError(PrintStream output) {
        output.println("HTTP/1.0 500 Internal Server Error");
        output.flush();
    }


    private String getTableListResponse(String route) {
        String database = null;
        if (route.contains("?database=")) {
            database = route.substring(route.indexOf("=") + 1, route.length());
        }

        Response response = null;

        if (Constants.APP_SHARED_PREFERENCES.equals(database)) {
            response = PrefHelper.getAllPrefTableName(mContext);
            mSelectedDatabase = Constants.APP_SHARED_PREFERENCES;
        } else {
////            openDatabase(database);
//            response = "";
//            mSelectedDatabase = database;
        }
        return mGson.toJson(response);
    }

    private String getDBListResponse() {

        Response response = new Response();

        response.rows.add(new String[]{Constants.APP_SHARED_PREFERENCES, "false", "false"});
        response.isSuccessful = true;
        return mGson.toJson(response);
    }

    private String getAllDataFromTheTableResponse(String route) {

        String tableName = null;

        if (route.contains("?tableName=")) {
            tableName = route.substring(route.indexOf("=") + 1, route.length());
        }

        TableDataResponse response;

        response = PrefHelper.getAllPrefData(mContext, tableName);

        return mGson.toJson(response);

    }

    private String addTableDataAndGetResponse(String route) {
        UpdateRowResponse response = null;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("addData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests);
            } else {
                //response = DatabaseHelper.addRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }

    private String updateTableDataAndGetResponse(String route) {
        UpdateRowResponse response = null;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("updatedData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.addOrUpdateRow(mContext, tableName, rowDataRequests);
            } else {
                // response = DatabaseHelper.updateRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }


    private String deleteTableDataAndGetResponse(String route) {
        UpdateRowResponse response = null;
        try {
            Uri uri = Uri.parse(URLDecoder.decode(route, "UTF-8"));
            String tableName = uri.getQueryParameter("tableName");
            String updatedData = uri.getQueryParameter("deleteData");
            List<RowDataRequest> rowDataRequests = mGson.fromJson(updatedData, new TypeToken<List<RowDataRequest>>() {
            }.getType());
            if (Constants.APP_SHARED_PREFERENCES.equals(mSelectedDatabase)) {
                response = PrefHelper.deleteRow(mContext, tableName, rowDataRequests);
            } else {
                //  response = DatabaseHelper.deleteRow(sqLiteDB, tableName, rowDataRequests);
            }
            return mGson.toJson(response);
        } catch (Exception e) {
            e.printStackTrace();
            response = new UpdateRowResponse();
            response.isSuccessful = false;
            return mGson.toJson(response);
        }
    }


}
