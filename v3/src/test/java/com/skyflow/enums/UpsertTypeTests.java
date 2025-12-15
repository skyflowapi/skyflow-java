
import org.junit.Test;
import static org.junit.Assert.*;
import com.skyflow.enums.UpsertType;

public class UpsertTypeTests {
    @Test
	public void testUpsertTypeValues() {
		UpsertType[] values = UpsertType.values();
		assertEquals(2, values.length);
		assertEquals(UpsertType.UPDATE, values[0]);
		assertEquals(UpsertType.REPLACE, values[1]);
	}

	@Test
	public void testUpsertTypeToString() {
		assertEquals("UPDATE", UpsertType.UPDATE.toString());
		assertEquals("REPLACE", UpsertType.REPLACE.toString());
	}

	@Test
	public void testUpsertTypeValueOf() {
		assertEquals(UpsertType.UPDATE, UpsertType.valueOf("UPDATE"));
		assertEquals(UpsertType.REPLACE, UpsertType.valueOf("REPLACE"));
	}

}
