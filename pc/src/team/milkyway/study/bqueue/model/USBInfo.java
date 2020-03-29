package team.milkyway.study.bqueue.model;

public class USBInfo {
    private short vendorId;
    private short productId;
    private String vendorNm;
    private String productNm;
    private byte inEndpoint;
    private byte outEndpoint;

    public short getVendorId() {
        return vendorId;
    }

    public void setVendorId(short vendorId) {
        this.vendorId = vendorId;
    }

    public short getProductId() {
        return productId;
    }

    public void setProductId(short productId) {
        this.productId = productId;
    }

    public String getVendorNm() {
        return vendorNm;
    }

    public void setVendorNm(String vendorNm) {
        this.vendorNm = vendorNm;
    }

    public String getProductNm() {
        return productNm;
    }

    public void setProductNm(String productNm) {
        this.productNm = productNm;
    }

    public byte getInEndpoint() {
        return inEndpoint;
    }

    public void setInEndpoint(byte inEndpoint) {
        this.inEndpoint = inEndpoint;
    }

    public byte getOutEndpoint() {
        return outEndpoint;
    }

    public void setOutEndpoint(byte outEndpoint) {
        this.outEndpoint = outEndpoint;
    }

    @Override
    public String toString() {
        return "USBInfo{" +
                "vendorId=" + vendorId +
                ", productId=" + productId +
                ", vendorNm='" + vendorNm + '\'' +
                ", productNm='" + productNm + '\'' +
                ", inEndpoint=" + inEndpoint +
                ", outEndpoint=" + outEndpoint +
                '}';
    }
}
