package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;
import org.junit.Assert;
import org.junit.Test;

public class RequestInterceptorTests {

    @Test
    public void testInterceptMutatesRequestContext() {
        RequestInterceptor interceptor = context -> context.addHeader(CustomHeaderKey.SkyflowAccountID, "account-id-value");
        RequestContext context = new RequestContext("INSERT");

        interceptor.intercept(context);

        Assert.assertEquals("account-id-value", context.getHeaders().get(CustomHeaderKey.SkyflowAccountID));
    }

    @Test
    public void testInterceptorIsFunctionalInterfaceUsableAsLambda() {
        final boolean[] invoked = {false};
        RequestInterceptor interceptor = context -> invoked[0] = true;

        interceptor.intercept(new RequestContext("DETOKENIZE"));

        Assert.assertTrue(invoked[0]);
    }
}
