package su.whs.streamcache;

/*
 * Copyright 2015 whs.su
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * StreamCache - wrapper for set of 'source stream' and 'cache stream'
 * if cache input stream == null (no cached data for input stream) - its read real input stream and write
 * copy to cache stream
 */

public class StreamCache extends InputStream {
    /**
     * interface to provide streams to wrapper
     */
    public interface StreamsProvider {
        /**
         *  must returns real input stream (for example: can return http stream on demand)
         *
         *  @return real input stream
         */
        InputStream getSourceInputStream();

        /**
         * must returns inputstream for cache data, if exists. NULL, if not
         * @return InputStream
         */
        InputStream getCacheInputStream();

        /**
         * must returns output stream for caching inputstream
         * @return OutputStream
         */
        OutputStream getCacheOutputStream();
    }

    private StreamsProvider mProvider;
    private boolean mCached = false;
    private InputStream mWrapped;
    private OutputStream mCache;
    private boolean mMarked = false;
    private int mMark = -1;

    public StreamCache(StreamsProvider provider) throws IOException {
        mProvider = provider;
        mWrapped = provider.getCacheInputStream();
        if (mWrapped==null) {
            mWrapped = provider.getSourceInputStream();
            if (mWrapped==null) throw new IOException("could not open source input stream");
            if (!mWrapped.markSupported()) {
                mWrapped = new BufferedInputStream(mWrapped);
            }
            mCache = provider.getCacheOutputStream();
            if (mCache==null) {
                System.err.println("StreamCache: StreamCache.StreamsProvider returns null Output stream");
                mCache = new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                    }
                };
            }
        } else {
            mCached = true;
        }
    }

    @Override
    public int read() throws IOException {
        /* */
        if (mCached) return mWrapped.read();
        int data = mWrapped.read();
        if (!mMarked)
            mCache.write(data);
        return data;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (mCached) return mWrapped.read(b);
        int len = mWrapped.read(b);
        if (len>0 && !mMarked)
            mCache.write(b,0,len);
        return len;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mCached)
            return mWrapped.read(b, off, len);
        int rlen = mWrapped.read(b,off,len);
        if (rlen>0 && !mMarked)
            mCache.write(b,off,rlen);
        return rlen;
    }

    @Override
    public int available() throws IOException {
        return mWrapped.available();
    }

    @Override
    public void close() throws IOException {
        super.close();
        mWrapped.close();
        if (mCache!=null) mCache.close();
    }

    @Override
    public void mark(int bytes) {
        mWrapped.mark(bytes);
        mMarked = true;
    }

    @Override
    public void reset() throws IOException {
        mWrapped.reset();
        mMarked = false;
    }
}
