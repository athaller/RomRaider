package enginuity.logger.innovate.io;

import enginuity.io.connection.ConnectionProperties;
import enginuity.io.connection.SerialConnection;
import enginuity.io.connection.SerialConnectionImpl;
import enginuity.logger.ecu.exception.SerialCommunicationException;
import static enginuity.util.HexUtil.asHex;
import static enginuity.util.ParamChecker.checkNotNull;
import static enginuity.util.ParamChecker.checkNotNullOrEmpty;
import static enginuity.util.ThreadUtil.sleep;
import org.apache.log4j.Logger;

public final class Lc1Connection implements InnovateConnection {
    private static final Logger LOGGER = Logger.getLogger(Lc1Connection.class);
    private final long sendTimeout;
    private final SerialConnection serialConnection;

    public Lc1Connection(ConnectionProperties connectionProperties, String portName) {
        checkNotNull(connectionProperties, "connectionProperties");
        checkNotNullOrEmpty(portName, "portName");
        this.sendTimeout = connectionProperties.getSendTimeout();
        serialConnection = new SerialConnectionImpl(connectionProperties, portName);
        LOGGER.info("LC-1 connected");
    }

    public byte[] read() {
        try {
            byte[] response = new byte[6];
            serialConnection.readStaleData();
            long timeout = sendTimeout;
            while (serialConnection.available() < response.length) {
                sleep(1);
                timeout -= 1;
                if (timeout <= 0) {
                    byte[] badBytes = new byte[serialConnection.available()];
                    serialConnection.read(badBytes);
                    LOGGER.debug("Bad response (read timeout): " + asHex(badBytes));
                    break;
                }
            }
            serialConnection.read(response);
            LOGGER.trace("LC-1 Response: " + asHex(response));
            return response;
        } catch (Exception e) {
            close();
            throw new SerialCommunicationException(e);
        }
    }

    public void close() {
        serialConnection.close();
        LOGGER.info("LC-1 disconnected");
    }
}