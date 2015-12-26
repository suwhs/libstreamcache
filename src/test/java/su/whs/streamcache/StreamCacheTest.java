package su.whs.streamcache;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;

/**
 * Created by igor n. boulliev on 26.12.15.
 */

public class StreamCacheTest {

    @Test
    public void testCaching() throws IOException {
        byte[] source_data = new byte[1024*1024*10];
        /* fill source data with random */
        Random r = new Random();
        r.nextBytes(source_data);

        final ByteArrayInputStream bais = new ByteArrayInputStream(source_data);
        ByteArrayOutputStream taos = new ByteArrayOutputStream();
        final ByteArrayOutputStream caos = new ByteArrayOutputStream();

        StreamCache sc = new StreamCache(new StreamCache.StreamsProvider() {
            @Override
            public InputStream getSourceInputStream() {
                return bais;
            }

            @Override
            public InputStream getCacheInputStream() {
                return null;
            }

            @Override
            public OutputStream getCacheOutputStream() {
                return caos;
            }
        });

        copy(sc, taos);

        final byte[] first_stage = taos.toByteArray();

        assertArrayEquals(source_data, first_stage);

        sc = new StreamCache(new StreamCache.StreamsProvider() {
            @Override
            public InputStream getSourceInputStream() {
                return null;
            }

            @Override
            public InputStream getCacheInputStream() {
                return new ByteArrayInputStream(first_stage);
            }

            @Override
            public OutputStream getCacheOutputStream() {
                return null;
            }
        });

        taos = new ByteArrayOutputStream();

        copy(sc, taos);

        final byte[] second_stage = taos.toByteArray();

        assertArrayEquals(source_data,second_stage);

    }

    private void copy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
    }
}
