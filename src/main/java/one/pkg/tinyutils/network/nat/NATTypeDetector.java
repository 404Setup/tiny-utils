package one.pkg.tinyutils.network.nat;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("all")
public class NATTypeDetector {

    // https://github.com/pradt2/always-online-stun
    public final String[] STUN_SERVERS = {
            "turn.cloudflare.com:3478",
            "stun.nextcloud.com:3478",
            "stun.sipnet.com:3478",
            "fwa.lifesizecloud.com:3478",
            "stun.healthtap.com:3478",
            "stun.threema.ch:3478",
            "stun.diallog.com:3478",
            "stun.genymotion.com:3478",
            "stun.alpirsbacher.de:3478",
            "stun.sipnet.net:3478",
            "stun.frozenmountain.com:3478",
            "stun.moonlight-stream.org:3478",
            "stun.finsterwalder.com:3478",
            "stun.zentauron.de:3478",
            "stun.geesthacht.de:3478",
            "stun.hot-chilli.net:3478",
            "stun.godatenow.com:3478",
            "stun.linuxtrent.it:3478",
            "stun.fitauto.ru:3478",
            "stun.bethesda.net:3478",
            "stun.1cbit.ru:3478",
            "stun.flashdance.cx:3478",
            "stun.bitburger.de:3478",
            "stun.annatel.net:3478",
            "stun.romaaeterna.nl:3478",
            "stun.graftlab.com:3478",
            "stun.uabrides.com:3478",
            "stun.lovense.com:3478",
            "stun.ttmath.org:3478",
            "stun.fmo.de:3478",
            "stun.freeswitch.org:3478",
            "stun.verbo.be:3478",
            "stun.vavadating.com:3478",
            "stun.voztovoice.org:3478",
            "stun.antisip.com:3478",
            "stun.lleida.net:3478",
            "stun.ncic.com:3478",
            "stun.3wayint.com:3478",
            "stun.peethultra.be:3478",
            "stun.acronis.com:3478",
            "stun.3deluxe.de:3478",
            "stun.poetamatusel.org:3478",
            "stun.tula.nu:3478",
            "stun.sonetel.com:3478",
            "stun.framasoft.org:3478",
            "stun.siptrunk.com:3478",
            "stun.m-online.net:3478",
            "stun.yesdates.com:3478",
            "stun.technosens.fr:3478",
            "stun.axialys.net:3478",
            "stun.bcs2005.net:3478",
            "stun.engineeredarts.co.uk:3478",
            "stun.ru-brides.com:3478",
            "stun.oncloud7.ch:3478",
            "stun.ipfire.org:3478",
            "stun.stochastix.de:3478",
            "stun.sipnet.ru:3478",
            "stun.sip.us:3478",
            "stun.meetwife.com:3478",
            "stun.mixvoip.com:3478",
            "stun.atagverwarming.nl:3478",
            "stun.siplogin.de:3478",
            "stun.voipgate.com:3478",
            "stun.cellmail.com:3478",
            "stun.signalwire.com:3478",
            "stun.ukh.de:3478",
            "stun.thinkrosystem.com:3478",
            "stun.skydrone.aero:3478",
            "stun.cope.es:3478",
            "stun.radiojar.com:3478",
            "stun.myspeciality.com:3478",
            "stun.nextcloud.com:443",
            "stun.business-isp.nl:3478",
            "stun.zepter.ru:3478",
            "stun.voip.blackberry.com:3478",
            "stun.dcalling.de:3478",
            "stun.kaseya.com:3478",
            "stun.bridesbay.com:3478",
            "stun.romancecompass.com:3478",
            "stun.ringostat.com:3478",
            "stun.kanojo.de:3478",
            "stun.f.haeder.net:3478",
            "stun.sipthor.net:3478",
            "stun.voipia.net:3478",
            "stun.files.fm:3478",
            "stun.vomessen.de:3478",
            "stun.telnyx.com:3478",
            "stun.sonetel.net:3478",
            "stun.baltmannsweiler.de:3478",
            "stun.pure-ip.com:3478"
    };

    public NATDetectionResult detectNATType() {
        NATDetectionResult result = new NATDetectionResult();

        try {
            String stunServer = findAvailableStunServer();
            if (stunServer == null) {
                result.natType = NATType.NO_CONNECTION;
                return result;
            }

            String[] parts = stunServer.split(":");
            String stunHost = parts[0];
            int stunPort = Integer.parseInt(parts[1]);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(3000);

                InetAddress localAddress = socket.getLocalAddress();
                result.localIP = localAddress.getHostAddress();
                result.localPort = socket.getLocalPort();

                STUNResponse test1 = sendSTUNRequest(socket, stunHost, stunPort, false, false);
                if (test1 == null) {
                    result.natType = NATType.NO_CONNECTION;
                    return result;
                }

                result.publicIP = test1.mappedAddress.getHostAddress();
                result.publicPort = test1.mappedPort;

                if (result.localIP.equals(result.publicIP) && result.localPort == result.publicPort) {
                    result.natType = NATType.PUBLIC_NETWORK;
                    return result;
                }

                STUNResponse test2 = sendSTUNRequest(socket, stunHost, stunPort, true, true);
                if (test2 != null) {
                    result.natType = NATType.FULL_CONE;
                    return result;
                }

                STUNResponse test3 = sendSTUNRequest(socket, stunHost, stunPort, false, true);
                if (test3 != null) {
                    result.natType = NATType.RESTRICTED_CONE;
                    return result;
                }

                try (DatagramSocket socket2 = new DatagramSocket()) {
                    socket2.setSoTimeout(3000);
                    STUNResponse test4 = sendSTUNRequest(socket2, stunHost, stunPort, false, false);

                    if (test4 != null) {
                        if (test4.mappedPort == result.publicPort) {
                            result.natType = NATType.PORT_RESTRICTED_CONE;
                            return result;
                        } else {
                            result.natType = NATType.SYMMETRIC;
                            return result;
                        }
                    }
                }

                result.natType = NATType.PORT_RESTRICTED_CONE;
            }

        } catch (Exception e) {
            result.natType = NATType.UNKNOWN;
            e.printStackTrace();
        }

        return result;
    }

    public String findAvailableStunServer() {
        for (String server : STUN_SERVERS) {
            try {
                String[] parts = server.split(":");
                InetAddress address = InetAddress.getByName(parts[0]);
                if (address.isReachable(2000)) {
                    return server;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        return null;
    }

    STUNResponse sendSTUNRequest(
            DatagramSocket socket,
            String stunHost,
            int stunPort,
            boolean changeIP,
            boolean changePort) throws Exception {

        InetAddress stunAddress = InetAddress.getByName(stunHost);

        byte[] requestData = buildSTUNBindingRequest(changeIP, changePort);
        DatagramPacket request = new DatagramPacket(
                requestData,
                requestData.length,
                stunAddress,
                stunPort
        );

        socket.send(request);

        byte[] buffer = new byte[1024];
        DatagramPacket response = new DatagramPacket(buffer, buffer.length);

        try {
            socket.receive(response);
            return parseSTUNResponse(response.getData(), response.getLength());
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    byte[] buildSTUNBindingRequest(boolean changeIP, boolean changePort) {
        ByteBuffer buffer = ByteBuffer.allocate(28);

        // Header
        buffer.putShort((short) 0x0001); // Binding Request
        buffer.putShort((short) 8); // Message Length

        // Transaction ID (128 bits)
        int magicCookie = 0x2112A442;
        buffer.putInt(magicCookie);
        int r1 = ThreadLocalRandom.current().nextInt();
        int r2 = ThreadLocalRandom.current().nextInt();
        int r3 = ThreadLocalRandom.current().nextInt();
        buffer.putInt(r1);
        buffer.putInt(r2);
        buffer.putInt(r3);

        // CHANGE-REQUEST attribute
        if (changeIP || changePort) {
            buffer.putShort((short) 0x0003); // CHANGE-REQUEST
            buffer.putShort((short) 4); // Length
            int changeValue = 0;
            if (changeIP) changeValue |= 0x04;
            if (changePort) changeValue |= 0x02;
            buffer.putInt(changeValue);
        }

        return buffer.array();
    }

    private STUNResponse parseSTUNResponse(byte[] data, int length) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 0, length);

            /*short messageType = */
            buffer.getShort();
            /*short messageLength = */
            buffer.getShort();
            int magicCookie = buffer.getInt();

            buffer.position(buffer.position() + 12);

            STUNResponse response = new STUNResponse();

            while (buffer.remaining() >= 4) {
                short attrType = buffer.getShort();
                short attrLength = buffer.getShort();

                if (attrType == 0x0001 || attrType == 0x0020) { // MAPPED-ADDRESS or XOR-MAPPED-ADDRESS
                    buffer.get();
                    byte family = buffer.get();
                    short port = buffer.getShort();

                    byte[] addressBytes = new byte[family == 0x01 ? 4 : 16];
                    buffer.get(addressBytes);

                    if (attrType == 0x0020) { // XOR-MAPPED-ADDRESS
                        // XOR with magic cookie
                        port ^= (short) (magicCookie >> 16);
                        for (int i = 0; i < addressBytes.length; i++) {
                            addressBytes[i] ^= (byte) (magicCookie >> (24 - i * 8));
                        }
                    }

                    response.mappedAddress = InetAddress.getByAddress(addressBytes);
                    response.mappedPort = port & 0xFFFF;

                } else if (attrType == 0x0005) { // CHANGED-ADDRESS
                    buffer.get();
                    byte family = buffer.get();
                    short port = buffer.getShort();

                    byte[] addressBytes = new byte[family == 0x01 ? 4 : 16];
                    buffer.get(addressBytes);

                    response.changedAddress = InetAddress.getByAddress(addressBytes);
                    response.changedPort = port & 0xFFFF;

                } else {
                    buffer.position(buffer.position() + attrLength);
                }

                int padding = (4 - (attrLength % 4)) % 4;
                buffer.position(buffer.position() + padding);
            }

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum NATType {
        PUBLIC_NETWORK("Public Network"),
        NO_CONNECTION("No Connection"),
        FULL_CONE("Full Cone NAT"),
        RESTRICTED_CONE("Restricted Cone NAT"),
        PORT_RESTRICTED_CONE("Port Restricted Cone NAT"),
        SYMMETRIC("Symmetric NAT"),
        FULL_TO_RESTRICTED("Full Cone to Restricted Cone"),
        FULL_TO_PORT_RESTRICTED("Full Cone to Port Restricted Cone"),
        FULL_TO_SYMMETRIC("Full Cone to Symmetric"),
        RESTRICTED_TO_PORT_RESTRICTED("Restricted Cone to Port Restricted Cone"),
        RESTRICTED_TO_SYMMETRIC("Restricted Cone to Symmetric"),
        PORT_RESTRICTED_TO_SYMMETRIC("Port Restricted Cone to Symmetric"),
        UNKNOWN("Unknown");

        private final String description;

        NATType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class NATDetectionResult {
        public NATType natType;
        public String localIP;
        public int localPort;
        public String publicIP;
        public int publicPort;
        public boolean hairpinning = false;

        @Override
        public String toString() {
            return String.format("""
                            NATDetectionResult{
                                nat_type: %s,
                                local_address: %s:%d,
                                public_address: %s:%d,
                                supports_hairpinning: %s
                            }""",
                    natType.getDescription(), localIP, localPort, publicIP, publicPort, hairpinning
            );
        }
    }

    static class STUNResponse {
        InetAddress mappedAddress;
        int mappedPort;
        InetAddress changedAddress;
        int changedPort;
    }
}