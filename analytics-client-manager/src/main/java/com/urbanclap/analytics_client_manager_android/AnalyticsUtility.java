package com.urbanclap.analytics_client_manager_android;

import java.io.IOException;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import android.content.res.Resources;

/**
 * Created by kanavarora on 10/06/16.
 */
public class AnalyticsUtility {
    private final static String KEYWORD_TRIGGER = "trigger";
    public static HashMap<String, CSVProperties> parseCSVFileIntoAnalyticsEvents(Resources r,
                                                                                 int csvFileId) {
        InputStream inputStream = null;
        ArrayList<String[]> resultList = new ArrayList();
        try {
            inputStream = r.openRawResource(csvFileId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String csvLine;
            while ((csvLine = reader.readLine()) != null) {
                String[] row = csvLine.split(",");
                resultList.add(row);
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            }
            catch (IOException e) {
                throw new RuntimeException("Error while closing input stream: "+e);
            }
        }

        //Read the CSV as List of Maps where each Map represents row data
        HashMap<String, CSVProperties> csvRows =  new HashMap<>();
        HashMap<String, String> row = null;
        if (resultList.size() <= 0) {
            return null;
        }
        String[] headers = resultList.get(0);
        for (int i=1; i< resultList.size(); i++) {
            String[] rowAsTokens = resultList.get(i);
            CSVProperties csvProperties = new CSVProperties();
            for (int j=0; j< headers.length && j < rowAsTokens.length; j++) {
                if (rowAsTokens[j] != null && rowAsTokens[j].length() > 0) {
                    csvProperties.put(headers[j], rowAsTokens[j]);
                }
            }
            if (csvProperties.containsKey(KEYWORD_TRIGGER)) {
                String trigger = csvProperties.get(KEYWORD_TRIGGER);
                csvProperties.remove(KEYWORD_TRIGGER);
                csvRows.put(trigger, csvProperties);
            }
        }
        return csvRows;
    }
}
