package one.pkg.tinyutils.network.nat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("all")
public class NATProxy {

    private final NATTypeDetector detector = new NATTypeDetector();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final int bufferSize;

    private DatagramSocket udpSocket;
    private ServerSocket tcpServerSocket;
    private Thread udpForwardThread;
    private Thread tcpAcceptThread;

    public NATProxy() {
        this(4096);
    }

    public NATProxy(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Retrieves the STUN mapping result for the default port.
     * The method determines the public IP address and public port
     */
    public STUNMappingResult getSTUNMapping() throws Exception {
        return getSTUNMapping(0);
    }

    /**
     * Establishes a STUN (Session Traversal Utilities for NAT) mapping by communicating
     **/
    public STUNMappingResult getSTUNMapping(int localPort) throws Exception {
        String stunServer = detector.findAvailableStunServer();
        if (stunServer == null) {
            throw new IOException("No available STUN server found");
        }

        String[] parts = stunServer.split(":");
        String stunHost = parts[0];
        int stunPort = Integer.parseInt(parts[1]);

        DatagramSocket socket = localPort > 0 ? new DatagramSocket(localPort) : new DatagramSocket();
        socket.setSoTimeout(3000);

        try {
            NATTypeDetector.STUNResponse response = detector.sendSTUNRequest(socket, stunHost, stunPort, false, false);
            if (response == null) {
                socket.close();
                throw new IOException("STUN request failed");
            }

            STUNMappingResult result = new STUNMappingResult();
            result.localPort = socket.getLocalPort();
            result.publicIP = response.mappedAddress.getHostAddress();
            result.publicPort = response.mappedPort;
            result.stunServer = stunServer;
            result.socket = socket;
            return result;
        } catch (Exception e) {
            socket.close();
            throw e;
        }
    }

    /**
     * Starts UDP forwarding by automatically detecting STUN mapping and forwarding to the specified internal port.
     *
     * @param internalPort The port number of the internal destination to which packets are forwarded.
     * @throws Exception If an error occurs during STUN detection or UDP forwarding setup.
     */
    public void startUDPForward(int internalPort) throws Exception {
        STUNMappingResult mapping = getSTUNMapping();
        startUDPForward(mapping, "127.0.0.1", internalPort);
    }

    /**
     * Starts forwarding UDP packets between an external STUN-mapped socket and an internal host/port.
     * This method spawns a new thread for handling the UDP packet forwarding process.
     *
     * @param mappingResult The result of the STUN mapping, which contains the external socket to use for forwarding.
     * @param internalHost  The hostname or IP address of the internal destination to which packets are forwarded.
     * @param internalPort  The port number of the internal destination to which packets are forwarded.
     * @throws Exception             If an error occurs during the setup or execution of the UDP forwarding process.
     * @throws IllegalStateException If the proxy is already running.
     */
    public void startUDPForward(STUNMappingResult mappingResult, String internalHost, int internalPort) throws Exception {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Proxy is already running");
        }

        this.udpSocket = mappingResult.socket;
        InetAddress internalAddress = InetAddress.getByName(internalHost);

        udpForwardThread = new Thread(() -> {
            try (DatagramSocket internalSocket = new DatagramSocket()) {
                internalSocket.setSoTimeout(1000);
                byte[] buffer = new byte[bufferSize];

                InetAddress lastExternalAddr = null;
                int lastExternalPort = -1;

                while (running.get()) {
                    DatagramPacket externalPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        udpSocket.receive(externalPacket);
                        lastExternalAddr = externalPacket.getAddress();
                        lastExternalPort = externalPacket.getPort();

                        DatagramPacket toInternal = new DatagramPacket(
                                externalPacket.getData(),
                                externalPacket.getOffset(),
                                externalPacket.getLength(),
                                internalAddress,
                                internalPort
                        );
                        internalSocket.send(toInternal);
                    } catch (SocketTimeoutException ignored) {
                    }

                    DatagramPacket internalPacket = new DatagramPacket(buffer, buffer.length);
                    try {
                        internalSocket.receive(internalPacket);

                        if (lastExternalAddr != null) {
                            DatagramPacket toExternal = new DatagramPacket(
                                    internalPacket.getData(),
                                    internalPacket.getOffset(),
                                    internalPacket.getLength(),
                                    lastExternalAddr,
                                    lastExternalPort
                            );
                            udpSocket.send(toExternal);
                        }
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            } catch (Exception e) {
                if (running.get()) {
                    e.printStackTrace();
                }
            }
        }, "NATProxy-UDP-Forward");
        udpForwardThread.setDaemon(true);
        udpForwardThread.start();
    }

    /**
     * Starts TCP forwarding by automatically detecting STUN mapping and forwarding to the specified internal port.
     * The listen port is determined by the STUN mapping's local port.
     *
     * @param internalPort The port number of the internal destination to which connections are forwarded.
     * @throws Exception If an error occurs during STUN detection or TCP forwarding setup.
     */
    public void startTCPForward(int internalPort) throws Exception {
        STUNMappingResult mapping = getSTUNMapping();
        startTCPForward(mapping.localPort, "127.0.0.1", internalPort);
    }

    /**
     * Starts a TCP forwarding proxy that listens for incoming TCP connections on the specified port
     * and forwards them to the internal host/port.
     */
    public void startTCPForward(int listenPort, String internalHost, int internalPort) throws Exception {
        if (running.getAndSet(true)) {
            throw new IllegalStateException("Proxy is already running");
        }

        tcpServerSocket = new ServerSocket(listenPort);

        tcpAcceptThread = new Thread(() -> {
            while (running.get()) {
                try {
                    Socket clientSocket = tcpServerSocket.accept();
                    Socket internalSocket = new Socket(internalHost, internalPort);

                    Thread c2i = new Thread(() -> forwardStream(clientSocket, internalSocket),
                            "NATProxy-TCP-C2I-" + clientSocket.getPort());
                    Thread i2c = new Thread(() -> forwardStream(internalSocket, clientSocket),
                            "NATProxy-TCP-I2C-" + clientSocket.getPort());

                    c2i.setDaemon(true);
                    i2c.setDaemon(true);
                    c2i.start();
                    i2c.start();
                } catch (Exception e) {
                    if (running.get()) {
                        e.printStackTrace();
                    }
                }
            }
        }, "NATProxy-TCP-Accept");
        tcpAcceptThread.setDaemon(true);
        tcpAcceptThread.start();
    }

    /**
     * Forwards data between two sockets by continuously reading from the input stream of the source socket
     * and writing to the output stream of
     */
    private void forwardStream(Socket source, Socket destination) {
        try {
            InputStream in = source.getInputStream();
            OutputStream out = destination.getOutputStream();
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
        } catch (IOException e) {
            // connection closed
        } finally {
            try {
                source.close();
            } catch (IOException ignored) {
            }
            try {
                destination.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Stops the NATProxy service and performs cleanup operations.
     * <p>
     * This method halts all ongoing processes of the NATProxy, including
     * shutting down sockets used for UDP and TCP communication
     */
    public void stop() {
        running.set(false);

        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        if (tcpServerSocket != null) {
            try {
                tcpServerSocket.close();
            } catch (IOException ignored) {
            }
        }

        if (udpForwardThread != null) {
            udpForwardThread.interrupt();
        }
        if (tcpAcceptThread != null) {
            tcpAcceptThread.interrupt();
        }
    }

    /**
     * Indicates whether the NATProxy is currently running.
     *
     * @return true if the NATProxy is running, false otherwise.
     */
    public boolean isRunning() {
        return running.get();
    }

    public static class STUNMappingResult {
        public int localPort;
        public String publicIP;
        public int publicPort;
        public String stunServer;
        DatagramSocket socket;

        public DatagramSocket getSocket() {
            return socket;
        }

        @Override
        public String toString() {
            return String.format("""
                            STUNMappingResult{
                                local_port: %d,
                                public_address: %s:%d,
                                stun_server: %s
                            }""",
                    localPort, publicIP, publicPort, stunServer
            );
        }
    }
}
