package misc;

import java.io.*;
import java.net.*;

public class NetworkCheck
{
    static final String OUTPUT_DIR = "c:/SAP";

    static final String[] SERVER_HOSTS =
    {
        "<HOST-NAME-TO-MONITOR-1>",
        "<HOST-NAME-TO-MONITOR-2>",
        "<HOST-NAME-TO-MONITOR-3>",
        "<HOST-NAME-TO-MONITOR-4>",
    };

    static final int CHECKER_PORT = 2203;

    static final int SOCKET_TIMEOUT = 10000; // 10000 milliseconds = 10 seconds

    static final String MODE_CONNECT = "connecting to";
    static final String MODE_READING = "reading from";
    static final String MODE_WRITING = "writing to";

    String localHost;
    FileWriter log;

    public static void main(String[] args) throws Exception
    {
        new NetworkCheck().run();
    }

    void run()
    {
        localHost = getLocalHost();
        startLog();
        boolean startedServer = false;
        for (String host : SERVER_HOSTS)
        {
            if (host.equalsIgnoreCase(localHost) || host.equals("localhost"))
            {
                new AcceptThread(host).start();
                startedServer = true;
            }
        }
        if (! startedServer)
        {
            new AcceptThread(localHost).start();
        }
        sleepFor(10000);
        for (String host : SERVER_HOSTS)
        {
            if (! host.equalsIgnoreCase(localHost))
            {
                new ClientThread(host, false).start();
                new ClientThread(host, true).start();
            }
        }
        sleepFor(Long.MAX_VALUE);
    }

    class ClientThread extends Thread
    {
        String serverHost;
        boolean dnsCheck;

        ClientThread(String host, boolean dns)
        {
            this.serverHost = host;
            this.dnsCheck = dns;
        }

        public void run()
        {
            boolean initial = ! dnsCheck;
            for (;;)
            {
                String mode = MODE_CONNECT;
                Socket server = null;
                try
                {
                    if (dnsCheck)
                    {
                        server = new Socket(serverHost, CHECKER_PORT);
                        server.close();
                        sleepFor(5000);
                    }
                    else
                    {
                        writeLog("Connecting to server " + serverHost
                            + (initial ? " (initial)" : ""));
                        server = new Socket(serverHost, CHECKER_PORT);
                        writeLog("Connected to server " + serverHost);
                        server.setSoTimeout(SOCKET_TIMEOUT);
                        initial = false;
                        OutputStream toServer = server.getOutputStream();
                        InputStream fromServer = server.getInputStream();
                        int c = 0;
                        long start = System.currentTimeMillis();
                        for (;;)
                        {
                            mode = MODE_WRITING;
                            toServer.write(c);
                            mode = MODE_READING;
                            int d = fromServer.read();
                            if (d == -1)
                            {
                                throw new IOException("read: unexpected end of input stream");
                            }
                            if (d != c)
                            {
                                throw new IOException("read: expected " + c + ", found " + d);
                            }
                            c = (c + 1) % 100;
                            long now = System.currentTimeMillis();
                            long diff = now - start;
                            if (diff >= 300000)
                            {
                                writeLog("Server " + serverHost + " is OK");
                                start = now;
                            }
                            sleepFor(1000);
                        }
                    }
                }
                catch (Exception ex)
                {
                    if (dnsCheck && ex.toString().indexOf("UnknownHostException") == -1)
                    {
                        // Don't log anything.
                    }
                    else if (initial)
                    {
                        writeLog("Initial connection to server " + serverHost + ": " + ex.toString());
                    }
                    else
                    {
                        writeLog("Exception while " + mode + " server " + serverHost
                            + (dnsCheck ? " (DNS check)" : "")
                            + ": " + myStackTrace(ex));
                    }
                    sleepFor(10000);
                    closeQuietly(server);
                }
            }
        }
    }

    class AcceptThread extends Thread
    {
        String serverHost;

        AcceptThread(String host)
        {
            this.serverHost = host;
        }

        public void run()
        {
            ServerSocket listener;
            try
            {
                writeLog("Starting listener on " + serverHost + ":" + CHECKER_PORT);
                listener = new ServerSocket(CHECKER_PORT, 10, InetAddress.getByName(serverHost));
            }
            catch (Exception ex)
            {
                writeLog("Exception while starting listener: " + myStackTrace(ex));
                return;
            }
            for (;;)
            {
                try
                {
                    Socket client = listener.accept();
                    new ServerThread(client).start();
                }
                catch (Exception ex)
                {
                    writeLog("Exception while accepting client: " + myStackTrace(ex));
                    sleepFor(10000);
                }
            }
        }
    }

    class ServerThread extends Thread
    {
        Socket client;
        String clientHost;

        ServerThread(Socket client)
        {
            this.client = client;
        }

        public void run()
        {
            String mode = MODE_READING;
            try
            {
                client.setSoTimeout(SOCKET_TIMEOUT);
                clientHost = client.getRemoteSocketAddress().toString();
                writeLog("Accepted connection from " + clientHost);
                InputStream fromClient = client.getInputStream();
                OutputStream toClient = client.getOutputStream();
                long start = System.currentTimeMillis();
                int c;
                while ((c = fromClient.read()) != -1)
                {
                    mode = MODE_WRITING;
                    toClient.write(c);
                    toClient.flush();
                    mode = MODE_READING;
                    long now = System.currentTimeMillis();
                    long diff = now - start;
                    if (diff >= 300000)
                    {
                        writeLog("Client " + clientHost + " is OK");
                        start = now;
                    }
                }
            }
            catch (Exception ex)
            {
                writeLog("Exception while " + mode + " client " + clientHost + ": " + myStackTrace(ex));
                closeQuietly(client);
            }
        }
    }

    void closeQuietly(Socket socket)
    {
        try
        {
            socket.close();
        }
        catch (Exception ignore)
        {
        }
    }

    void sleepFor(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    void startLog()
    {
        try
        {
            String file = new File(OUTPUT_DIR + "/NetworkCheck-" + localHost + ".log").getCanonicalPath();
            System.out.println("Opening " + file);
            log = new FileWriter(file, true);
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    void writeLog(String message)
    {
        try
        {
            String ts = new java.sql.Timestamp(System.currentTimeMillis()).toString().replace(' ', 'T');
            if (ts.length() < 20) ts += ".";
            while (ts.length() < 23) ts += "0";
            String line = ts + " " + message;
            synchronized (log)
            {
                System.out.println(line);
                log.write(line + "\n");
                log.flush();
            }
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    String getLocalHost()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    String myStackTrace(Exception ex)
    {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString().trim();
    }
}
