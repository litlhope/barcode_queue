package team.milkyway.study.bqueue;

import org.usb4java.*;
import team.milkyway.study.bqueue.model.USBInfo;
import team.milkyway.study.bqueue.util.AOAUtil;
import team.milkyway.study.bqueue.util.ByteUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

public class Main {
    private static final short VENDOR_ID_ACCESSORY = (short) 0x18D1;    // Google
    private static final short PRODUCT_ID_ACCESSORY = (short) 0x2D01;    // Accessory mode

    private static List<USBInfo> knownUsbList;
    private static List<USBInfo> connUsbList;
    private static USBInfo selectedUsb;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AOAUtil.setShutdown();
            }
        });

        Main main = new Main();
        main.start();
    }

    public Main() {
        knownUsbList = new ArrayList<>();
        USBInfo usb;

        // 04e8:6860 Samsung Electronics Co., Ltd. SAMSUNG_Android  Serial: 1c5cdba03f037ece
        usb = new USBInfo();
        usb.setVendorId((short) 0x04E8);
        usb.setProductId((short) 0x6860);
        usb.setVendorNm("Samsung Electronics Co., Ltd.");
        usb.setProductNm("SAMSUNG_Android");
        knownUsbList.add(usb);

        // 18d1:4ee7 Google Inc. Nexus 6P  Serial: ENU7N15B05001013
        usb = new USBInfo();
        usb.setVendorId((short) 0x18D1);
        usb.setProductId((short) 0x4EE7);
        usb.setVendorNm("Google Inc.");
        usb.setProductNm("Nexus 6P");
        knownUsbList.add(usb);
    }

    public void start() {
        try {
            getConnectedUsbList();

            if (!isAccessoryConnected()) {
                USBInfo usb = getKnownAndroidDevice();
                if (usb == null) {
                    infoNotExistKnownUsb();
                } else {
                    infoExistKnownUsb(usb);
                }
            } else {
                System.out.println("기존에 연결했던 장비를 찾았습니다. 통신을 시작합니다.");
                startAOACommunication();
            }
        } finally {
            AOAUtil.close();
        }
    }

    private void getConnectedUsbList() {
        connUsbList = new ArrayList<>();
        Context context = null;
        try {
            context = new Context();
            int result = LibUsb.init(context);
            if (result != LibUsb.SUCCESS) {
                throw new LibUsbException("Unable to initialize libusb.", result);
            }

            DeviceList list = new DeviceList();
            result = LibUsb.getDeviceList(context, list);
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

                    String vendorNm;
                    String productNm;
                    DeviceHandle handle = new DeviceHandle();
                    if (LibUsb.open(device, handle) == LibUsb.SUCCESS) {
                        vendorNm = LibUsb.getStringDescriptor(handle, descriptor.iManufacturer());
                        productNm = LibUsb.getStringDescriptor(handle, descriptor.iProduct());
                        LibUsb.close(handle);

                        // USB 정보를 확인 할 수 없으면, 연결목록에서 제외 한다.
                        // 안드로이드 단말기 중 정보 확인이 되지 않는 경우가 있을경우 수정 필요.
                        if (vendorNm != null && productNm != null) {
                            USBInfo usb = new USBInfo();
                            usb.setVendorId(descriptor.idVendor());
                            usb.setProductId(descriptor.idProduct());
                            usb.setVendorNm(vendorNm.trim());
                            usb.setProductNm(productNm.trim());
                            System.out.println("usb: " + usb);
                            connUsbList.add(usb);
                        }
                    }
                }
            } finally {
                LibUsb.freeDeviceList(list, true);
            }

        } catch (LibUsbException ex) {
            ex.printStackTrace();
        } finally {
            if (context != null) {
                LibUsb.exit(context);
            }
        }
    }

    private boolean isAccessoryConnected() {
        if (connUsbList != null && connUsbList.size() > 0) {
            for (USBInfo usb : connUsbList) {
                if (usb.getVendorId() == VENDOR_ID_ACCESSORY && usb.getProductId() == PRODUCT_ID_ACCESSORY) {
                    return true;
                }
            }
        }
        return false;
    }

    private USBInfo getKnownAndroidDevice() {
        if (knownUsbList != null && knownUsbList.size() > 0 && connUsbList != null && connUsbList.size() > 0) {
            for (USBInfo knownUsb : knownUsbList) {
                for (USBInfo usb : connUsbList) {
                    if (knownUsb.getVendorId() == usb.getVendorId() && knownUsb.getProductId() == usb.getProductId()) {
                        return usb;
                    }
                }
            }
        }
        return null;
    }

    private void sendAOAProtocol() {
        try {
            if (selectedUsb != null) {
                if (AOAUtil.init(selectedUsb.getVendorId(), selectedUsb.getProductId()) == LibUsb.SUCCESS) {
                    AOAUtil.setupAccessory(
                            "Milkyway company",
                            "Barcode Queue",
                            "Milkyway AOA Study project",
                            "1.0",
                            "http://www.milkyway.team",
                            "000000001234"
                    );
                }
                sleep(1000);
                startAOACommunication();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            AOAUtil.close();
        }
    }

    private void startAOACommunication() {
        try {
            AOAUtil.init(VENDOR_ID_ACCESSORY, PRODUCT_ID_ACCESSORY);
            AOAUtil.getEndPointAddress();
            AOAUtil.write();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            AOAUtil.close();
        }
    }

    private void infoSelectAllDevice() {
        try {
            System.out.println("USB 장치 목록 중 연결을 시도 할 장치를 선택해 주세요.");
            for (int inx = 0; inx < connUsbList.size(); inx++) {
                USBInfo usb = connUsbList.get(inx);
                if (usb.getVendorNm() != null && usb.getProductNm() != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(String.format("%d", (inx + 1)))
                            .append(" . ")
                            .append(usb.getVendorNm())
                            .append(" / ")
                            .append(usb.getProductNm());
                    System.out.println(sb.toString());
                }
            }
            System.out.print("연결 할 장치의 번호를 입력해 주세요.: ");
            char usbNo = (char) System.in.read();
            clearSystemIn();

            int usbInx = Integer.parseInt(usbNo + "") - 1;
            System.out.println("usbInx: " + usbInx);
            selectedUsb = connUsbList.get(usbInx);
            sendAOAProtocol();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void infoNotExistKnownUsb() {
        try {
            System.out.print("알려진 안드로이드 장비를 찾을 수 없습니다. 연결된 전체 USB 장비 중 선택하시겠습니까? (y/N): ");
            char yN = (char) System.in.read();
            clearSystemIn();
            if (yN == 'n' || yN == 'N') {
                System.exit(-1);
            } else {
                infoSelectAllDevice();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void infoExistKnownUsb(USBInfo usb) {
        try {
            System.out.println("안드로이드 장비를 찾았습니다.");
            System.out.println("  - " + usb.getVendorNm() + " / " + usb.getProductNm());
            System.out.print("위 장비와의 연결을 시도하시겠습니까? N을 입력하시면, 전체 USB 목록을 볼 수 있습니다. (y/N): ");
            char yN = (char) System.in.read();
            clearSystemIn();
            if (yN == 'n' || yN == 'N') {
                infoSelectAllDevice();
            } else {
                selectedUsb = usb;
                sendAOAProtocol();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * System.in.read()에서 사용한 입력값 이후에 이용된 개행문자를 제거하는 유틸리티
     *   - 시스템별로 개행문자가 "\n" 이거나 "\r\n" 이므로, 개행문자 길이에 맞게 읽어와서 버린다.
     * @throws IOException
     */
    private void clearSystemIn() throws IOException {
        String newLine = System.lineSeparator();
        for (int inx = 0; inx < newLine.length(); inx++) {
            System.in.read();
        }
    }
}
