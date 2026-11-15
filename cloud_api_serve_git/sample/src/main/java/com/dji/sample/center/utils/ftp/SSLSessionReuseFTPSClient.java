package com.dji.sample.center.utils.ftp;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.Util;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Locale;

public class SSLSessionReuseFTPSClient extends FTPSClient {

    public SSLSessionReuseFTPSClient() {
        this(true);
    }

    public SSLSessionReuseFTPSClient(boolean isImplicit) {
        super(isImplicit);
        // This cross-port JSSE cache strategy uses TLS 1.2 sessions, not TLS 1.3 tickets.
        setEnabledProtocols(new String[]{"TLSv1.2"});
    }

    /**
     * @param command the command to get
     * @param remote  the remote file name
     * @param local   the local file name
     * @return true if successful
     * @throws IOException on error
     * @since 3.1
     */
    @Override
    protected boolean _retrieveFile(String command, String remote, OutputStream local)
            throws IOException {
        Socket socket = _openDataConnection_(command, remote);

        if (socket == null) {
            return false;
        }

        final InputStream input;
        input = new BufferedInputStream(socket.getInputStream());

        // Treat everything else as binary for now
        try {
            Util.copyStream(input, local, getBufferSize(),
                    CopyStreamEvent.UNKNOWN_STREAM_SIZE, null,
                    false);
        } finally {
            Util.closeQuietly(input);
            Util.closeQuietly(socket);
        }

        // Get the transfer response
        boolean ok = completePendingCommand();
        return ok;
    }


    @Override
    protected void _prepareDataSocket_(final Socket socket) throws IOException {
        if (socket instanceof SSLSocket) {
            // Control socket is SSL
            final SSLSession session = ((SSLSocket) _socket_).getSession();
            if (session.isValid() && "TLSv1.2".equals(session.getProtocol())) {
                final SSLSessionContext context = session.getSessionContext();
                try {
                    final Field sessionHostPortCache = context.getClass().getDeclaredField("sessionHostPortCache");
                    sessionHostPortCache.setAccessible(true);
                    final Object cache = sessionHostPortCache.get(context);
                    final Method method = cache.getClass().getDeclaredMethod("put", Object.class, Object.class);
                    method.setAccessible(true);
                    method.invoke(cache, String
                            .format("%s:%s", socket.getInetAddress().getHostName(), String.valueOf(socket.getPort()))
                            .toLowerCase(Locale.ROOT), session);
                    method.invoke(cache, String
                            .format("%s:%s", socket.getInetAddress().getHostAddress(), String.valueOf(socket.getPort()))
                            .toLowerCase(Locale.ROOT), session);
                } catch (Exception e) {
                    try {
                        socket.close();
                    } catch (IOException closeError) {
                        e.addSuppressed(closeError);
                    }
                    throw new IOException("FTPS TLS session cache access failed; context="
                            + (context == null ? "null" : context.getClass().getName())
                            + ", Java=" + System.getProperty("java.version")
                            + ". This strategy requires SunJSSE. On Java 9+ add application JVM options: "
                            + "--add-opens java.base/sun.security.ssl=ALL-UNNAMED "
                            + "--add-opens java.base/sun.security.util=ALL-UNNAMED", e);
                }
            } else {
                socket.close();
                throw new IOException("FTPS requires a valid TLSv1.2 control session for data reuse; protocol=" + session.getProtocol());
            }
        }
    }
}



