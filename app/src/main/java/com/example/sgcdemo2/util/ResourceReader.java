package com.example.sgcdemo2.util;

import android.content.Context;
import android.content.res.Resources;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class ResourceReader {

    public static String readTextFileFromResource(Context context, int resourceId) {
        Resources res = context.getResources();
        InputStream in = res.openRawResource(resourceId);
        InputStreamReader inputReader = new InputStreamReader(in);
        BufferedReader bufferReader = new BufferedReader(inputReader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while ((line = bufferReader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            inputReader.close();
            bufferReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return text.toString();
    }
}
