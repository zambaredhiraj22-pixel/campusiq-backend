package com.campusiq.hardware;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.campusiq.dto.AttendanceMarkRequest;
import com.campusiq.service.FingerprintAttendanceService;
import com.fazecast.jSerialComm.SerialPort;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class SerialAttendanceListener {

    private static final Logger logger =
            LoggerFactory.getLogger(
                    SerialAttendanceListener.class
            );

    private static final String ATTENDANCE_PREFIX =
            "ATTENDANCE:";

    private final FingerprintAttendanceService
            fingerprintAttendanceService;

    @Value("${campusiq.esp32.device-id:ESP32-LAB-01}")
    private String deviceId;

    @Value("${campusiq.esp32.serial-port:COM18}")
    private String serialPortName;

    @Value("${campusiq.esp32.serial-baud-rate:115200}")
    private int baudRate;

    private volatile boolean running;

    private Thread listenerThread;

    private SerialPort serialPort;

    private OutputStream outputStream;

    private final Object writeLock =
            new Object();

    private volatile Integer
            pendingEnrollmentId;

    private volatile CompletableFuture<Boolean>
            pendingEnrollmentFuture;

    public SerialAttendanceListener(
            FingerprintAttendanceService
                    fingerprintAttendanceService) {

        this.fingerprintAttendanceService =
                fingerprintAttendanceService;
    }

    // =================================================
    // START SERIAL LISTENER
    // =================================================

    @PostConstruct
    public void startSerialListener() {

        running = true;

        listenerThread =
                new Thread(
                        this::listenForMessages,
                        "campusiq-esp32-serial-listener"
                );

        listenerThread.setDaemon(true);

        listenerThread.start();
    }

    // =================================================
    // SERIAL LISTENING LOOP
    // =================================================

    private void listenForMessages() {

        while (running) {

            try {

                if (!openSerialPort()) {

                    sleepBeforeRetry();

                    continue;
                }

                logger.info(
                        "CAMPUS-IQ ESP32 serial listener connected to {} at {} baud",
                        serialPortName,
                        baudRate
                );

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        serialPort
                                                .getInputStream(),
                                        StandardCharsets.UTF_8
                                )
                        );

                String line;

                while (running
                        && serialPort.isOpen()
                        && (line = reader.readLine())
                                != null) {

                    processSerialMessage(
                            line.trim()
                    );
                }

            } catch (Exception exception) {

                if (running) {

                    logger.warn(
                            "ESP32 serial communication error: {}",
                            exception.getMessage()
                    );
                }

            } finally {

                closeSerialPort();
            }

            if (running) {

                sleepBeforeRetry();
            }
        }
    }

    // =================================================
    // OPEN COM PORT
    // =================================================

    private boolean openSerialPort() {

        serialPort =
                SerialPort.getCommPort(
                        serialPortName
                );

        serialPort.setComPortParameters(
                baudRate,
                8,
                SerialPort.ONE_STOP_BIT,
                SerialPort.NO_PARITY
        );

        serialPort.setComPortTimeouts(
                SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                0,
                0
        );

        if (!serialPort.openPort()) {

            logger.warn(
                    "Could not open {}. Close Arduino Serial Monitor if it is open. Retrying...",
                    serialPortName
            );

            return false;
        }

        outputStream =
                serialPort.getOutputStream();

        return true;
    }

    // =================================================
    // PROCESS MESSAGE FROM ESP32
    // =================================================

    private void processSerialMessage(
            String message) {

        if (message == null
                || message.isBlank()) {

            return;
        }

        logger.info(
                "ESP32 >> {}",
                message
        );

        // ---------------------------------------------
        // Attendance
        // Example: ATTENDANCE:7
        // ---------------------------------------------

        if (message.startsWith(
                ATTENDANCE_PREFIX)) {

            processAttendanceMessage(
                    message
            );

            return;
        }

        // ---------------------------------------------
        // Enrollment successful
        // Example: ENROLL_SUCCESS:9
        // ---------------------------------------------

        if (message.startsWith(
                "ENROLL_SUCCESS:")) {

            processEnrollmentSuccess(
                    message
            );

            return;
        }

        // ---------------------------------------------
        // Enrollment error
        // Example:
        // ENROLL_ERROR:9:FINGERPRINTS_DID_NOT_MATCH
        // ---------------------------------------------

        if (message.startsWith(
                "ENROLL_ERROR:")) {

            processEnrollmentError(
                    message
            );

            return;
        }

        // Other messages such as:
        // ENROLL_INFO:PLACE_FINGER
        // DEVICE_READY
        // DEVICE_MODE:ATTENDANCE
        // MATCHED_ID:7
        // CONFIDENCE:80
        // are only logged.
    }

    // =================================================
    // PROCESS ATTENDANCE
    // =================================================

    private void processAttendanceMessage(
            String message) {

        String fingerprintIdText =
                message
                        .substring(
                                ATTENDANCE_PREFIX
                                        .length()
                        )
                        .trim();

        try {

            int fingerprintTemplateId =
                    Integer.parseInt(
                            fingerprintIdText
                    );

            AttendanceMarkRequest request =
                    new AttendanceMarkRequest(
                            deviceId,
                            fingerprintTemplateId
                    );

            fingerprintAttendanceService
                    .markAttendance(
                            request
                    );

            logger.info(
                    "Attendance successfully processed from ESP32. Template ID = {}",
                    fingerprintTemplateId
            );

        } catch (NumberFormatException exception) {

            logger.warn(
                    "Invalid fingerprint template ID received from ESP32: {}",
                    fingerprintIdText
            );

        } catch (Exception exception) {

            logger.error(
                    "Attendance processing failed for message '{}': {}",
                    message,
                    exception.getMessage()
            );
        }
    }

    // =================================================
    // SEND ENROLLMENT COMMAND TO ESP32
    // =================================================

    public boolean enrollFingerprintOnDevice(
            Integer fingerprintTemplateId) {

        if (fingerprintTemplateId == null
                || fingerprintTemplateId <= 0) {

            throw new IllegalArgumentException(
                    "Fingerprint Template ID must be greater than 0"
            );
        }

        if (serialPort == null
                || !serialPort.isOpen()
                || outputStream == null) {

            throw new IllegalStateException(
                    "ESP32 serial device is not connected"
            );
        }

        synchronized (this) {

            if (pendingEnrollmentFuture != null
                    && !pendingEnrollmentFuture
                            .isDone()) {

                throw new IllegalStateException(
                        "Another fingerprint enrollment is already in progress"
                );
            }

            pendingEnrollmentId =
                    fingerprintTemplateId;

            pendingEnrollmentFuture =
                    new CompletableFuture<>();
        }

        String command =
                "ENROLL:"
                + fingerprintTemplateId
                + "\n";

        sendCommand(
                command
        );

        logger.info(
                "Spring Boot >> ESP32 ENROLL:{}",
                fingerprintTemplateId
        );

        try {

            return pendingEnrollmentFuture
                    .get(
                            70,
                            TimeUnit.SECONDS
                    );

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Fingerprint enrollment failed or timed out: "
                            + exception.getMessage(),
                    exception
            );

        } finally {

            synchronized (this) {

                pendingEnrollmentId =
                        null;

                pendingEnrollmentFuture =
                        null;
            }
        }
    }

    // =================================================
    // WRITE COMMAND TO ESP32
    // =================================================

    private void sendCommand(
            String command) {

        synchronized (writeLock) {

            try {

                outputStream.write(
                        command.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                outputStream.flush();

            } catch (Exception exception) {

                throw new IllegalStateException(
                        "Could not send command to ESP32",
                        exception
                );
            }
        }
    }

    // =================================================
    // ENROLLMENT SUCCESS RESPONSE
    // =================================================

    private void processEnrollmentSuccess(
            String message) {

        String idText =
                message
                        .substring(
                                "ENROLL_SUCCESS:"
                                        .length()
                        )
                        .trim();

        try {

            int receivedId =
                    Integer.parseInt(
                            idText
                    );

            CompletableFuture<Boolean>
                    future =
                    pendingEnrollmentFuture;

            Integer expectedId =
                    pendingEnrollmentId;

            if (future != null
                    && expectedId != null
                    && expectedId
                            .equals(receivedId)) {

                logger.info(
                        "Fingerprint enrollment successful on ESP32. Template ID = {}",
                        receivedId
                );

                future.complete(
                        true
                );
            }

        } catch (NumberFormatException exception) {

            logger.warn(
                    "Invalid enrollment success message: {}",
                    message
            );
        }
    }

    // =================================================
    // ENROLLMENT ERROR RESPONSE
    // =================================================

    private void processEnrollmentError(
            String message) {

        String[] parts =
                message.split(
                        ":",
                        3
                );

        if (parts.length < 3) {

            logger.warn(
                    "Invalid enrollment error message: {}",
                    message
            );

            return;
        }

        try {

            int receivedId =
                    Integer.parseInt(
                            parts[1]
                    );

            String reason =
                    parts[2];

            CompletableFuture<Boolean>
                    future =
                    pendingEnrollmentFuture;

            Integer expectedId =
                    pendingEnrollmentId;

            if (future != null
                    && expectedId != null
                    && expectedId
                            .equals(receivedId)) {

                logger.error(
                        "Fingerprint enrollment failed on ESP32. Template ID = {}, Reason = {}",
                        receivedId,
                        reason
                );

                future.completeExceptionally(
                        new IllegalStateException(
                                reason
                        )
                );
            }

        } catch (NumberFormatException exception) {

            logger.warn(
                    "Invalid fingerprint ID in enrollment error message: {}",
                    message
            );
        }
    }

    // =================================================
    // RETRY WAIT
    // =================================================

    private void sleepBeforeRetry() {

        try {

            Thread.sleep(
                    3000
            );

        } catch (InterruptedException exception) {

            Thread.currentThread()
                    .interrupt();
        }
    }

    // =================================================
    // CLOSE SERIAL PORT
    // =================================================

    private void closeSerialPort() {

        outputStream = null;

        if (serialPort != null
                && serialPort.isOpen()) {

            serialPort.closePort();

            logger.info(
                    "ESP32 serial port {} closed",
                    serialPortName
            );
        }
    }

    // =================================================
    // STOP LISTENER
    // =================================================

    @PreDestroy
    public void stopSerialListener() {

        running = false;

        closeSerialPort();

        if (listenerThread != null) {

            listenerThread.interrupt();
        }
    }
}