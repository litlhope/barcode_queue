package team.milkyway.study.bqueue.util;

public class ByteUtil {
    public static byte[] hex2Bytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int inx = 0; inx < len; inx += 2) {
            data[inx / 2] = (byte) ((Character.digit(hex.charAt(inx), 16) << 4)
                    + Character.digit(hex.charAt(inx + 1), 16));
        }
        return data;
    }

    public static String bytes2hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b: bytes) {
            sb.append(String.format("%02X", b&0xff));
        }
        return sb.toString();
    }
}
