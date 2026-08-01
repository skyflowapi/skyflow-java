package com.skyflow.vault.data;

import org.junit.Assert;
import org.junit.Test;

public class BaseInsertRequestTests {

    @Test
    public void testBuilderSetsTable() {
        BaseInsertRequest.BaseInsertRequestBuilder builder = new BaseInsertRequest.BaseInsertRequestBuilder();
        builder.table("test_table");
        BaseInsertRequest request = new BaseInsertRequest(builder);

        Assert.assertEquals("test_table", request.getTable());
    }

    @Test
    public void testBuilderTableMethodReturnsSameBuilderInstance() {
        BaseInsertRequest.BaseInsertRequestBuilder builder = new BaseInsertRequest.BaseInsertRequestBuilder();
        BaseInsertRequest.BaseInsertRequestBuilder returned = builder.table("test_table");

        Assert.assertSame(builder, returned);
    }

    @Test
    public void testNullTableWhenNeverSet() {
        BaseInsertRequest.BaseInsertRequestBuilder builder = new BaseInsertRequest.BaseInsertRequestBuilder();
        BaseInsertRequest request = new BaseInsertRequest(builder);

        Assert.assertNull(request.getTable());
    }

    @Test
    public void testEmptyTable() {
        BaseInsertRequest.BaseInsertRequestBuilder builder = new BaseInsertRequest.BaseInsertRequestBuilder();
        builder.table("");
        BaseInsertRequest request = new BaseInsertRequest(builder);

        Assert.assertEquals("", request.getTable());
    }
}
