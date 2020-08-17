package ofs;

import org.junit.Assert;
import org.junit.Test;

public class FailTest {
    @Test
    public void fail1() throws InterruptedException {
        Thread.sleep(5000);
        Assert.fail();
    }
}
