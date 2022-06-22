/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.Configuration;
import com.skyflow.entities.LogLevel;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

public class HelpersTest {

    @Test
    public void testFormatJsonToFormEncodedString(){
        Configuration.setLogLevel(LogLevel.DEBUG);
        JSONObject testJson = new JSONObject();
        testJson.put("key1","value1");
        JSONObject nestedObj = new JSONObject();
        nestedObj.put("key2","value2");
        testJson.put("nest",nestedObj);

        String testResponse = Helpers.formatJsonToFormEncodedString(testJson);
        System.out.println(testResponse);
        assert testResponse.contains("key1=value1");
        assert testResponse.contains("nest[key2]=value2");
    }

    @Test
    public void testFormatJsonToMultiPartFormDataString(){
        JSONObject testJson = new JSONObject();
        testJson.put("key1","value1");
        JSONObject nestedObj = new JSONObject();
        nestedObj.put("key2","value2");
        testJson.put("nest",nestedObj);
        String testBoundary = "123";
        String testResponse = Helpers.formatJsonToMultiPartFormDataString(testJson,testBoundary);
        assert testResponse.contains("--"+testBoundary);
        assert testResponse.contains("--"+testBoundary+"--");
        assert testResponse.contains("Content-Disposition: form-data; name=\"key1\"");
        assert testResponse.contains("value1");
        assert testResponse.contains("Content-Disposition: form-data; name=\"nest[key2]\"");
        assert testResponse.contains("value2");
    }
}
