package team.milkyway.study.bqueue.util;

import org.usb4java.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static java.lang.Thread.sleep;

/**
 * Android Open Accessory Protocol Utility
 */
public class AOAUtil {
    //control request type
    private static final short CTRL_TYPE_STANDARD = (0 << 5);
    private static final short CTRL_TYPE_CLASS = (1 << 5);
    private static final short CTRL_TYPE_VENDOR = (2 << 5);
    private static final short CTRL_TYPE_RESERVED = (3 << 5);

    //control request direction
    private static final short CTRL_OUT = 0x00;
    private static final short CTRL_IN = 0x80;

    private static Context mContext;
    private static Device mDevice;
    private static DeviceHandle mHandle;

    private static byte inEndpoint = 0x00;
    private static byte outEndpoint = 0x00;

    private static boolean isShutdown = false;

    public static int init(short vendorId, short productId) {
        mContext = new Context();
        int result = LibUsb.init(mContext);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to initialize libusb.", result);
        }

        mDevice = findDevice(vendorId, productId);
        if (mDevice == null) {
            System.out.println("Not found device!!");
            return -1;
        }

        mHandle = new DeviceHandle();
        result = LibUsb.open(mDevice, mHandle);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to open USB device", result);
        }

        result = LibUsb.claimInterface(mHandle, 0);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to claim interface", result);
        }

        return LibUsb.SUCCESS;
    }

    public static Device findDevice(short vendorId, short productId) {
        DeviceList list = new DeviceList();
        int result = LibUsb.getDeviceList(mContext, list);
        if (result < 0) {
            throw new LibUsbException("Unable to get device list", result);
        }

        try {
            for (Device device : list) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                if (result != LibUsb.SUCCESS) {
                    throw new LibUsbException("Unable to read device descriptor", result);
                }

                if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) {
                    return device;
                }
            }
        } finally {
            LibUsb.freeDeviceList(list, true);
        }

        // Device not found
        return null;
    }

    public static int setupAccessory(String manufacturer, String model, String description, String version,
                                     String url, String serial) throws LibUsbException {
        int result;

        // send token packet
        sendSetupPacket((byte) (CTRL_TYPE_VENDOR | CTRL_IN), (byte) 51);

        // send data packet
        sendAccessoryDataPacket(manufacturer, (short) 0);
        sendAccessoryDataPacket(model, (short) 1);
        sendAccessoryDataPacket(description, (short) 2);
        sendAccessoryDataPacket(version, (short) 3);
        sendAccessoryDataPacket(url, (short) 4);
        sendAccessoryDataPacket(serial, (short) 5);

        // send handshake packet
        result = sendSetupPacket((byte) (CTRL_TYPE_VENDOR | CTRL_OUT), (byte) 53);

        LibUsb.releaseInterface(mHandle, 0);

        return result;
    }

    private static int sendSetupPacket(byte requestType, byte request) throws LibUsbException {
        int result;
        byte[] buffer = new byte[request == (byte) 53 ? 0 : 2];
        ByteBuffer data = BufferUtils.allocateByteBuffer(buffer.length);
        data.put(buffer);

        final short value = 0;
        final short index = 0;
        final long timeout = 0;

        System.out.println(String.format("sendSetupPacket: 0x%02x / 0x%02x", requestType, request));
        result = LibUsb.controlTransfer(mHandle, requestType, request, value, index, data, timeout);
        if (result < 0) {
            throw new LibUsbException("Control transfer failed", result);
        }

        System.out.println("result: " + result);

        if (request == (byte) 51) {
            data.rewind();
            data.get(buffer, 0, 2);
            System.out.println("data: " + ByteUtil.bytes2hex(buffer));

            int supportVersion = buffer[1] << 8 | buffer[0];
            System.out.println("supportVersion: " + supportVersion);

            if (supportVersion <= 0) {
                throw new LibUsbException("Not support Android Open Accessory!!", result);
            } else {
                System.out.println("AOAv" + supportVersion + " support!");
            }
        }

        return result;
    }

    private static void sendAccessoryDataPacket(String val, short index) {
        int result;
        byte[] buffer = val.getBytes();
        ByteBuffer data = BufferUtils.allocateByteBuffer(buffer.length);
        data.put(buffer);

        final byte requestType = (byte) (CTRL_TYPE_VENDOR | CTRL_OUT);
        final byte request = (byte) 52;
        final short value = 0;
        final long timeout = 0;

        result = LibUsb.controlTransfer(mHandle, requestType, request, value, index, data, timeout);
        if (result < 0) {
            throw new LibUsbException("Control transfer failed - " + index + " [" + val + "]", result);
        }
    }

    public static void getEndPointAddress() {
        ConfigDescriptor configDescriptor = new ConfigDescriptor();
        LibUsb.getActiveConfigDescriptor(mDevice, configDescriptor);
        EndpointDescriptor[] endpoints = (configDescriptor.iface()[0]).altsetting()[0].endpoint();
        for (EndpointDescriptor endpoint : endpoints) {
            if (DescriptorUtils.getDirectionName(endpoint.bEndpointAddress()).equals("IN")) {
                inEndpoint = (byte) (endpoint.bEndpointAddress() & 0xFF);
                System.out.println("IN = " + String.format("0x%02x", inEndpoint));
            }

            if (DescriptorUtils.getDirectionName(endpoint.bEndpointAddress()).equals("OUT")) {
                outEndpoint = (byte) (endpoint.bEndpointAddress() & 0xFF);
                System.out.println("OUT = " + String.format("0x%02x", outEndpoint));
            }
        }
    }

    public static void write() {
        int data = 0;
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(1);
        while (!isShutdown) {
            buffer.rewind();
            buffer.put((byte) data);
            IntBuffer transferred = BufferUtils.allocateIntBuffer();
            int result = LibUsb.bulkTransfer(mHandle, outEndpoint, buffer, transferred, 5000);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to send data", result);
            }

            System.out.println(transferred.get() + " bytes sent to device");
            if (data < 255) {
                data++;
            } else {
                data = 0;
            }

            try {
                sleep(50);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static ByteBuffer read(int size) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(size).order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int result = LibUsb.bulkTransfer(mHandle, inEndpoint, buffer, transferred, 5000);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to read data", result);
        }

        System.out.println(transferred.get() + " bytes read from device");
        return buffer;
    }

    public static void setShutdown() {
        isShutdown = true;
    }

    public static void close() {
        System.out.println("close");
        if (mHandle != null) {
            System.out.println("handle close");
            LibUsb.releaseInterface(mHandle, 0);
            LibUsb.close(mHandle);
        }

        if (mContext != null) {
            System.out.println("context exit");
            LibUsb.exit(mContext);
        }
    }
}
