package com.vio.aitukuviobe.filter;

import cn.hutool.core.io.IoUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * 请求包装器，支持重复读取 Body
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public RequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        body = IoUtil.readBytes(request.getInputStream());
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
            @Override
            public boolean isFinished() { return false; }
            @Override
            public boolean isReady() { return true; }
            @Override
            public void setReadListener(ReadListener listener) {}
        };
    }
}
