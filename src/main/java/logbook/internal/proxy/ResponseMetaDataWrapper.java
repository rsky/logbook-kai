package logbook.internal.proxy;

import logbook.proxy.ResponseMetaData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

class ResponseMetaDataWrapper implements ResponseMetaData, Cloneable {

    private int status;

    private String contentType;

    private Optional<InputStream> responseBody;

    @Override
    public int getStatus() {
        return this.status;
    }

    void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public Optional<InputStream> getResponseBody() {
        return this.responseBody;
    }

    void setResponseBody(Optional<InputStream> responseBody) {
        this.responseBody = responseBody;
    }

    void set(InputStream body) throws IOException {
        this.setResponseBody(Optional.of(ungzip(body)));
    }

    @Override
    public ResponseMetaDataWrapper clone() {
        ResponseMetaDataWrapper clone = new ResponseMetaDataWrapper();
        clone.setStatus(this.getStatus());
        clone.setContentType(this.getContentType());
        clone.setResponseBody(this.getResponseBody());
        return clone;
    }

    private static InputStream ungzip(InputStream body) throws IOException {
        body.mark(Short.BYTES);
        int magicbyte = body.read() << 8 ^ body.read();
        body.reset();
        if (magicbyte == 0x1f8b) {
            return new GZIPInputStream(body);
        }
        return body;
    }
}
