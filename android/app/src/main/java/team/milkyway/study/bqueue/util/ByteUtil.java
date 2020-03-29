package team.milkyway.study.bqueue.util;

public class ByteUtil {
	public static String bytes2hex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b: bytes) {
			sb.append(String.format("%02X", b&0xff));
		}
		return sb.toString();
	}
}
