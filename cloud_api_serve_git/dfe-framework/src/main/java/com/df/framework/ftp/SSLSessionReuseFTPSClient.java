package com.df.framework.ftp;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(SSLSessionReuseFTPSClient.class);

    public SSLSessionReuseFTPSClient() {
        super(true);
    }

    public SSLSessionReuseFTPSClient(boolean isImplicit) {
        super(isImplicit);
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
            if (session.isValid()) {
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
                } catch (NoSuchFieldException e) {
                    throw new IOException(e);
                } catch (Exception e) {
                    throw new IOException(e);
                }
            } else {
                throw new IOException("Invalid SSL Session");
            }
        }
    }
}



