package com.skyflow.vault;

import com.skyflow.vault.audit.ListEventRequest;
import com.skyflow.vault.audit.ListEventResponse;
import com.skyflow.vault.bin.GetBinRequest;
import com.skyflow.vault.bin.GetBinResponse;
import org.junit.Assert;
import org.junit.Test;

public class BinAuditTests {
    @Test
    public void testGetBinRequestConstructor() {
        GetBinRequest req = new GetBinRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testGetBinResponseConstructor() {
        GetBinResponse resp = new GetBinResponse();
        Assert.assertNotNull(resp);
    }

    @Test
    public void testListEventRequestConstructor() {
        ListEventRequest req = new ListEventRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testListEventResponseConstructor() {
        ListEventResponse resp = new ListEventResponse();
        Assert.assertNotNull(resp);
    }
}
